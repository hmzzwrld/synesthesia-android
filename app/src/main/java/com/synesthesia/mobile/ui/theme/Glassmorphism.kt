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
 * Universal Apple-style liquid glass modifier supporting custom shapes, 
 * theme states, prismatic top highlights, and physical depth shadows.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp,
    isDarkTheme: Boolean = true
): Modifier = this.composed {
    val resolvedShape = shape // uses custom shape if passed, else default rounded corners
    
    this
        .shadow(
            elevation = elevation,
            shape = resolvedShape,
            ambientColor = Color.Black.copy(alpha = 0.5f),
            spotColor = Color.Black.copy(alpha = 0.7f)
        )
        .clip(resolvedShape)
        // Translucent milk-white refractive gradient body with theme awareness
        .background(
            brush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        Color.White.copy(alpha = 0.18f), // High specular light capture at top
                        Color.White.copy(alpha = 0.08f), // Milky mid-tone diffusion
                        Color.Black.copy(alpha = 0.25f)  // Dark lower refraction anchor
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.10f)
                    )
                }
            )
        )
        // Crisp convex rim border simulating light bending along the curved glass edge
        .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f), // Bright top rim highlight
                    Color.White.copy(alpha = 0.05f) // Faded bottom edge
                )
            ),
            shape = resolvedShape
        )
}
