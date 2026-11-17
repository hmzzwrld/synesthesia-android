package com.synesthesia.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared translucent material for SYNESTHESIA.
 *
 * This builds layered lighting, tint, rim highlights and depth. It is a
 * lightweight glass treatment, not a true backdrop-refraction shader.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    @Suppress("UNUSED_PARAMETER") cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp,
    isDarkTheme: Boolean = true
): Modifier {
    val body = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFDCCEFF).copy(alpha = 0.13f),
                Color(0xFF8E9FE0).copy(alpha = 0.065f),
                Color(0xFF11101C).copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.48f),
                Color(0xFFE8E8F5).copy(alpha = 0.22f),
                Color(0xFFD8D9E8).copy(alpha = 0.12f)
            )
        )
    }

    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDarkTheme) 0.58f else 0.78f),
            Color(0xFFC4A8FF).copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.055f)
        )
    )

    val innerRim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.17f),
            Color.Transparent,
            Color.Black.copy(alpha = if (isDarkTheme) 0.12f else 0.025f)
        )
    )

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.42f else 0.12f),
            spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.52f else 0.16f)
        )
        .clip(shape)
        // Broad, soft reflected colour.
        .background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC4A8FF).copy(alpha = if (isDarkTheme) 0.105f else 0.16f),
                    Color.Transparent
                ),
                center = Offset(0f, 0f),
                radius = 720f
            )
        )
        // Translucent body lets the Aurora remain visible underneath.
        .background(body)
        // Fine outer and inner rims create a rounded, convex edge.
        .border(1.dp, rim, shape)
        .border(0.55.dp, innerRim, shape)
        // A restrained upper sheen gives the surface a curved light catch.
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkTheme) 0.075f else 0.12f),
                        Color.White.copy(alpha = 0.018f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.34f
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.34f),
                cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
            )
        }
}
