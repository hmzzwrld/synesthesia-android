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

fun Modifier.appleGlassSurface(
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp
): Modifier = this.composed {
    // 1. Soft multi-layered shadow to lift the glass off the background like a physical lens
    val shape = RoundedCornerShape(cornerRadius)
    
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.5f),
            spotColor = Color.Black.copy(alpha = 0.7f)
        )
        .clip(shape)
        // 2. Translucent milk-white refractive gradient body
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f), // High specular light capture at top
                    Color.White.copy(alpha = 0.08f), // Milky mid-tone diffusion
                    Color.Black.copy(alpha = 0.25f)  // Dark lower refraction anchor
                )
            )
        )
        // 3. Crisp convex rim border (simulating the curved glass edge refracting light)
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
