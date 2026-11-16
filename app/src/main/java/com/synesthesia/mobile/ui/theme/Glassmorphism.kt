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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal Apple-style liquid glass modifier with prismatic top highlights,
 * milky diffusion, physical depth shadows, and a glowing refractive rim.
 */
fun Modifier.glassSurface(
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp
): Modifier = this.composed {
    val shape = RoundedCornerShape(cornerRadius)
    
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.5f),
            spotColor = Color.Black.copy(alpha = 0.7f)
        )
        .clip(shape)
        // Translucent milk-white refractive gradient body (simulating real glass lens depth)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f), // High specular light capture at top
                    Color.White.copy(alpha = 0.08f), // Milky mid-tone diffusion
                    Color.Black.copy(alpha = 0.25f)  // Dark lower refraction anchor
                )
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
            shape = shape
        )
}
