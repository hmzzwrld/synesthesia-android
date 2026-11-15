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
val GlassBackgroundDark = Color(0x9912121E) // rgba(18, 18, 30, 0.60)
val GlassBackgroundBright = Color(0xCCFFFFFF)
val GlassBorderColor = Color(0x28FFFFFF)     // rgba(255, 255, 255, 0.16)
val SynesthesiaDarkBg = Color(0xFF05050D)   // --bg-dark from website

/**
 * Custom Compose Modifier that mimics the website's CSS liquid-glass surface:
 * - Translucent gradient fill
 * - Soft highlight edge borders
 * - Clean clipping to the target shape
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
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f),
                        Color(0xFF141222).copy(alpha = 0.38f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.65f)
                    )
                }
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.08f)
                )
            ),
            shape = shape
        )
}
