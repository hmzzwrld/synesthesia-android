package com.synesthesia.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Refractive Milky Glass mirroring the reference website aesthetic:
 * - High-diffusion translucent core
 * - Top-down prismatic specular gradient
 * - Luminous outer border rim
 */
fun Modifier.glassSurface(
    shape: Shape,
    isDarkTheme: Boolean = true,
    borderWidth: Dp = 1.dp
): Modifier = composed {
    this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        Color.White.copy(alpha = 0.28f), // Prismatic top sheen
                        Color.White.copy(alpha = 0.10f), // Milky translucent diffusion
                        Color(0xFF141226).copy(alpha = 0.65f) // Deep ambient sink
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.75f)
                    )
                }
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f), // Crisp rim highlight
                    Color.White.copy(alpha = 0.15f)  // Soft fade
                )
            ),
            shape = shape
        )
}
