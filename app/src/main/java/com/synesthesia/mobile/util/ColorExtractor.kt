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

        // Mirror the website's 3-bit RGB histogram and saturation-weighted colour ranking.
        val buckets = linkedMapOf<String, Pair<FloatArray, Float>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = scaledBitmap.getPixel(x, y)
                val rgb = floatArrayOf(
                    ((pixel shr 16) and 0xFF).toFloat(),
                    ((pixel shr 8) and 0xFF).toFloat(),
                    (pixel and 0xFF).toFloat()
                )
                val max = rgb.maxOrNull() ?: 0f
                val min = rgb.minOrNull() ?: 0f
                if (max < 28f || min > 235f) continue
                val key = rgb.joinToString(",") { (it.toInt() shr 5).toString() }
                val weight = 1f + (max - min) / 80f
                val bin = buckets[key]
                if (bin == null) buckets[key] = Pair(FloatArray(3) { rgb[it] * weight }, weight)
                else {
                    for (i in 0..2) bin.first[i] += rgb[i] * weight
                    buckets[key] = Pair(bin.first, bin.second + weight)
                }
            }
        }
        if (buckets.isEmpty()) return Triple(defaultA, defaultB, defaultC)
        val ranked = buckets.values.sortedByDescending { it.second }
            .map { (sum, weight) -> FloatArray(3) { (sum[it] / weight).coerceIn(0f, 255f) } }
        val chosen = mutableListOf<FloatArray>()
        for (rgb in ranked) {
            if (chosen.all { old -> kotlin.math.sqrt(((old[0]-rgb[0])*(old[0]-rgb[0]) + (old[1]-rgb[1])*(old[1]-rgb[1]) + (old[2]-rgb[2])*(old[2]-rgb[2])).toDouble()) > 65.0 }) {
                chosen.add(rgb)
            }
            if (chosen.size == 3) break
        }
        fun toColor(index: Int, fallback: Color): Color {
            val rgb = chosen.getOrNull(index) ?: return liftForContrast(fallback)
            val extracted = Color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1f)
            return liftForContrast(extracted)
        }
        return Triple(toColor(0, defaultA), toColor(1, defaultB), toColor(2, defaultC))
    }

    /**
     * Artwork still determines the hue, but very dark colours are lifted before they are
     * used for controls. Near-black / low-saturation artwork becomes a soft silver-grey;
     * dark chromatic colours keep their hue (e.g. forest green becomes a clearer green).
     */
    private fun liftForContrast(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255f).toInt(),
            (color.green * 255f).toInt(),
            (color.blue * 255f).toInt(),
            hsv
        )
        val saturation = hsv[1]
        val value = hsv[2]
        if (saturation < 0.16f || value < 0.13f) {
            // Neutral artwork should not produce invisible black controls.
            val grey = value.coerceIn(0.62f, 0.78f)
            return Color(grey, grey, (grey * 1.035f).coerceAtMost(0.84f), 1f)
        }
        hsv[1] = saturation.coerceAtLeast(0.48f).coerceAtMost(0.78f)
        hsv[2] = value.coerceAtLeast(0.70f).coerceAtMost(0.94f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
