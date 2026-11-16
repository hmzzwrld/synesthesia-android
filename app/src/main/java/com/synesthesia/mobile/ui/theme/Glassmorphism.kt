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

// Website Liquid Glass Color & Style Tokens
val GlassBackgroundDark = Color(0x9912121E)
val GlassBackgroundBright = Color(0xCCFFFFFF)
val GlassBorderColor = Color(0x40FFFFFF)     // Increased border luminance
val SynesthesiaDarkBg = Color(0xFF05050D)

/**
 * Enhanced Liquid Glass Modifier mirroring the website's lush frosted aesthetic:
 * - Bright top specular highlight gradient
 * - Rich translucent mid-tones
 * - Crisp, glowing rim border
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
                        Color.White.copy(alpha = 0.22f), // Brighter top specular highlight
                        Color.White.copy(alpha = 0.07f), // Smooth middle fade
                        Color(0xFF1E1A38).copy(alpha = 0.55f) // Deep atmospheric tint at bottom
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.90f),
                        Color.White.copy(alpha = 0.70f)
                    )
                }
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f), // Strong top rim light
                    Color.White.copy(alpha = 0.12f)  // Soft bottom edge
                )
            ),
            shape = shape
        )
}
