package com.synesthesia.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared SYNESTHESIA glass material, based on the website's --liquid-fill,
 * --glass-border and --liquid-edge tokens. Android's ordinary Compose layer
 * cannot sample arbitrary sibling content as a backdrop, so this deliberately
 * reproduces the website's visible material layers without claiming true refraction.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp,
    isDarkTheme: Boolean = true
): Modifier = this.composed {
    val resolvedShape = shape
    val fill = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.145f),
                Color.White.copy(alpha = 0.045f),
                Color(0xFF141222).copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.48f),
                Color(0xFFE9E7F2).copy(alpha = 0.36f)
            )
        )
    }
    this
        .shadow(
            elevation = elevation,
            shape = resolvedShape,
            ambientColor = Color.Black.copy(alpha = 0.38f),
            spotColor = Color.Black.copy(alpha = 0.48f)
        )
        .clip(resolvedShape)
        .background(fill)
        .background(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = if (isDarkTheme) 0.16f else 0.30f), Color.Transparent),
                radius = 420f
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.13f),
                    Color.White.copy(alpha = 0.055f)
                )
            ),
            shape = resolvedShape
        )
}
