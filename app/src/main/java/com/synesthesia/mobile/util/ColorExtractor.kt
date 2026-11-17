package com.synesthesia.mobile.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {

    /**
     * Extracts dominant accent colors from an artwork URI using Coil and simple bitmap sampling.
     * Returns a Triple of colors (Primary Accent, Secondary Accent, Tertiary Accent) to feed into the atmosphere.
     */
    suspend fun extractPaletteFromUri(
        context: Context,
        data: Any?,
        defaultA: Color,
        defaultB: Color,
        defaultC: Color
    ): Triple<Color, Color, Color> = withContext(Dispatchers.IO) {
        if (data == null) return@withContext Triple(defaultA, defaultB, defaultC)

        try {
            val request = ImageRequest.Builder(context)
                .data(data)
                .allowHardware(false) // Required to obtain a software Bitmap
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                return@withContext analyzeBitmapColors(bitmap, defaultA, defaultB, defaultC)
            }
        } catch (_: Exception) {
            // Fallback gracefully if extraction fails
        }

        Triple(defaultA, defaultB, defaultC)
    }

    private fun analyzeBitmapColors(
        bitmap: Bitmap,
        defaultA: Color,
        defaultB: Color,
        defaultC: Color
    ): Triple<Color, Color, Color> {
        // A tiny sample grid is enough for atmosphere colours and keeps work inexpensive.
        val sampleBitmap = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val buckets = linkedMapOf<Int, MutableList<Int>>()
        val hsv = FloatArray(3)

        try {
            for (x in 0 until 32 step 3) {
                for (y in 0 until 32 step 3) {
                    val pixel = sampleBitmap.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    android.graphics.Color.RGBToHSV(r, g, b, hsv)

                    // Ignore near-black, near-white and almost colourless pixels.
                    if (hsv[2] < 0.10f || hsv[2] > 0.97f || hsv[1] < 0.14f) continue

                    // Quantize RGB into compact buckets so repeated artwork colours win.
                    val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                    buckets.getOrPut(key) { mutableListOf() }.add(pixel)
                }
            }
        } finally {
            if (sampleBitmap !== bitmap) sampleBitmap.recycle()
        }

        val dominant = buckets.values
            .sortedByDescending { it.size }
            .take(3)
            .map { pixels ->
                val red = pixels.sumOf { android.graphics.Color.red(it) } / pixels.size
                val green = pixels.sumOf { android.graphics.Color.green(it) } / pixels.size
                val blue = pixels.sumOf { android.graphics.Color.blue(it) } / pixels.size
                Color(android.graphics.Color.rgb(red, green, blue))
            }

        return Triple(
            dominant.getOrNull(0) ?: defaultA,
            dominant.getOrNull(1) ?: dominant.getOrNull(0) ?: defaultB,
            dominant.getOrNull(2) ?: dominant.getOrNull(1) ?: defaultC
        )
    }
}
