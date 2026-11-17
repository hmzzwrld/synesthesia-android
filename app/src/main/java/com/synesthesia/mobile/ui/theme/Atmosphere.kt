package com.synesthesia.mobile.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val SynesthesiaDarkBg = Color(0xFF05050D)
// Website ambient palette defaults (--amb-a, --amb-b, --amb-c)
val DefaultAmbA = Color(0xFFFF3D8B) // website --amb-a
val DefaultAmbB = Color(0xFFFF9F40) // website --amb-b
val DefaultAmbC = Color(0xFFA855F7) // website --amb-c
val BrightBaseBg = Color(0xFFF3F4FA)

@Composable
fun SynesthesiaAtmosphere(
    isDarkTheme: Boolean = true,
    ambA: Color = DefaultAmbA,
    ambB: Color = DefaultAmbB,
    ambC: Color = DefaultAmbC,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Smooth color gliding matching CSS: transition: --amb-a 1.8s ease
    val animatedA by animateColorAsState(targetValue = ambA, animationSpec = tween(1800), label = "ambA")
    val animatedB by animateColorAsState(targetValue = ambB, animationSpec = tween(1800), label = "ambB")
    val animatedC by animateColorAsState(targetValue = ambC, animationSpec = tween(1800), label = "ambC")

    // Adjust intensity based on theme (controlled/dimmer in Dark, brighter in Bright)
    val alphaPrimary = if (isDarkTheme) 0.30f else 0.38f
    val alphaSecondary = if (isDarkTheme) 0.22f else 0.30f
    val baseColor = if (isDarkTheme) SynesthesiaDarkBg else BrightBaseBg

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedA.copy(alpha = alphaPrimary),
                        Color.Transparent
                    ),
                    center = Offset(150f, 200f),
                    radius = 900f
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedC.copy(alpha = alphaSecondary),
                        Color.Transparent
                    ),
                    center = Offset(900f, 750f),
                    radius = 1000f
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedB.copy(alpha = alphaSecondary * 0.85f),
                        Color.Transparent
                    ),
                    center = Offset(300f, 1500f),
                    radius = 950f
                )
            ),
        content = content
    )
}
