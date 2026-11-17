package com.synesthesia.mobile.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity

val SynesthesiaDarkBg = Color(0xFF0B0B13)

// Midnight Aurora palette defaults.
val DefaultAmbA = Color(0xFF7655A8) // violet
val DefaultAmbB = Color(0xFF568D9A) // teal
val DefaultAmbC = Color(0xFFC4A8FF) // lavender
val BrightBaseBg = Color(0xFFF3F4FA)

@Composable
fun SynesthesiaAtmosphere(
    isDarkTheme: Boolean = true,
    ambA: Color = DefaultAmbA,
    ambB: Color = DefaultAmbB,
    ambC: Color = DefaultAmbC,
    scrollProgress: Float = 0f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedA by animateColorAsState(ambA, tween(1800), label = "ambA")
    val animatedB by animateColorAsState(ambB, tween(1800), label = "ambB")
    val animatedC by animateColorAsState(ambC, tween(1800), label = "ambC")

    // Slow, restrained movement keeps the atmosphere alive without animating every UI item.
    val drift = rememberInfiniteTransition(label = "auroraDrift")
    val driftA by drift.animateFloat(
        initialValue = -0.035f,
        targetValue = 0.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDriftA"
    )
    val driftB by drift.animateFloat(
        initialValue = 0.025f,
        targetValue = -0.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDriftB"
    )

    val scrollShift by animateFloatAsState(scrollProgress.coerceIn(0f, 1f), tween(650), label = "scrollAtmosphere")

    val alphaPrimary = if (isDarkTheme) 0.28f else 0.30f
    val alphaSecondary = if (isDarkTheme) 0.19f else 0.23f
    val baseColor = if (isDarkTheme) SynesthesiaDarkBg else BrightBaseBg

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Keep artwork colours gentler in dark mode and a little more luminous in light mode.
        val tintA = if (isDarkTheme) lerp(animatedA, SynesthesiaDarkBg, 0.22f)
                    else lerp(animatedA, Color.White, 0.10f)
        val tintB = if (isDarkTheme) lerp(animatedB, SynesthesiaDarkBg, 0.22f)
                    else lerp(animatedB, Color.White, 0.10f)
        val tintC = if (isDarkTheme) lerp(animatedC, SynesthesiaDarkBg, 0.22f)
                    else lerp(animatedC, Color.White, 0.10f)

        val centerA = Offset(widthPx * (0.18f + driftA + scrollShift * 0.16f), heightPx * (0.18f + scrollShift * 0.18f))
        val centerB = Offset(widthPx * (0.82f - scrollShift * 0.18f), heightPx * (0.78f - scrollShift * 0.12f + driftB))
        val centerC = Offset(widthPx * (0.52f + scrollShift * 0.08f), heightPx * (0.48f + scrollShift * 0.12f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor)
                .background(
                    Brush.radialGradient(
                        colors = listOf(tintA.copy(alpha = alphaPrimary), Color.Transparent),
                        center = centerA,
                        radius = widthPx * 0.95f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(tintC.copy(alpha = alphaSecondary), Color.Transparent),
                        center = centerB,
                        radius = widthPx * 1.05f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(tintB.copy(alpha = alphaSecondary * 0.9f), Color.Transparent),
                        center = centerC,
                        radius = widthPx * 0.82f
                    )
                ),
            content = content
        )
    }
}
