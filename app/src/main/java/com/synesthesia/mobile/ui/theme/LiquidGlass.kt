package com.synesthesia.mobile.ui.theme

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/** Global glass switch. Snapshot state, so every surface that reads it recomposes when it flips. */
object GlassPrefs {
    var enabled by mutableStateOf(true)
}

/** Applies [block] only while glass is ON; in flat mode the modifier chain is left untouched. */
inline fun Modifier.glassOnly(block: Modifier.() -> Modifier): Modifier =
    if (GlassPrefs.enabled) block() else this

/** Haze backdrop blur when glass is ON, a plain opaque flat panel when OFF. */
fun Modifier.glassBackdrop(state: HazeState, blur: Dp, noise: Float): Modifier =
    if (!GlassPrefs.enabled) {
        this.background(Color(0xFF181624).copy(alpha = 0.96f))
    } else {
        this.hazeChild(state = state) {
            blurRadius = blur
            backgroundColor = Color.Transparent
            noiseFactor = noise
            inputScale = HazeInputScale.Fixed(0.66f)
        }
    }

private const val EDGE_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float strength;
half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float2 s = sign(p - c);
    float2 g = (q.x > 0.0 && q.y > 0.0) ? normalize(q) * s : (q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y));
    float depth = clamp(-d / 24.0, 0.0, 1.0);
    float bend = pow(1.0 - depth, 3.0);
    float2 off = g * bend * strength;
    half4 col;
    col.r = content.eval(p - off).r;
    col.g = content.eval(p - off * 1.07).g;
    col.b = content.eval(p - off * 1.14).b;
    col.a = content.eval(p - off).a;
    float rim = smoothstep(2.2, 0.0, abs(d + 1.2));
    float light = 0.45 + 0.55 * dot(g, normalize(float2(-0.6, -0.8)));
    col.rgb += half3(rim * light * 0.38) * col.a;
    return col;
}
"""

/**
 * Edge-only lens refraction (Android 13+): bends the already-composited glass (blurred backdrop + tint)
 * near the silhouette, adds slight colour fringing and a lit specular rim. The flat centre is untouched.
 * Place it OUTSIDE (before) the clip/backdrop modifiers. No-op below API 33 or when glass is OFF.
 */
fun Modifier.liquidEdge(cornerRadius: Dp? = null, strengthDp: Float = 14f): Modifier {
    if (!GlassPrefs.enabled || Build.VERSION.SDK_INT < 33) return this
    return composed {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val shader = remember { RuntimeShader(EDGE_AGSL) }
        this
            .onSizeChanged { size = it }
            .graphicsLayer {
                if (size.width > 0 && size.height > 0) {
                    val r = cornerRadius?.toPx() ?: (minOf(size.width, size.height) / 2f)
                    shader.setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
                    shader.setFloatUniform("radius", r.coerceAtMost(minOf(size.width, size.height) / 2f))
                    shader.setFloatUniform("strength", strengthDp * density.density)
                    renderEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
                }
            }
    }
}

/** Applies [block] only while glass is OFF (flat mode). */
inline fun Modifier.flatOnly(block: Modifier.() -> Modifier): Modifier =
    if (!GlassPrefs.enabled) block() else this
