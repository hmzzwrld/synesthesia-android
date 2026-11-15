package com.synesthesia.mobile.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
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
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(data)
                .allowHardware(false) // Required to obtain a software Bitmap
                .build()

            val result = loader.execute(request)
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
        // Downsample bitmap for fast sampling performance on reference devices
        val width = 32
        val height = 32
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var pixelCount = 0

        val samples = mutableListOf<Int>()

        for (x in 0 until width step 4) {
            for (y in 0 until height step 4) {
                val pixel = scaledBitmap.getPixel(x, y)
                samples.add(pixel)
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
                pixelCount++
            }
        }

        if (pixelCount == 0) return Triple(defaultA, defaultB, defaultC)

        // Pick distinct color samples from the image to build a rich dynamic triad
        val colorA = if (samples.isNotEmpty()) Color(samples[samples.size / 4] or 0xFF000000.toInt()) else defaultA
        val colorB = if (samples.isNotEmpty()) Color(samples[samples.size / 2] or 0xFF000000.toInt()) else defaultB
        val colorC = if (samples.isNotEmpty()) Color(samples[(samples.size * 3) / 4] or 0xFF000000.toInt()) else defaultC

        return Triple(colorA, colorB, colorC)
    }
}
