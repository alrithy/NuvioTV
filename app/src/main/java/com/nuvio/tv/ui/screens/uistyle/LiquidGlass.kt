package com.nuvio.tv.ui.screens.uistyle

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay

/*
 * G12c liquid glass (D061; feature 199). FILE_PORT of DavidVamaiotu/NuvioTV-Reshaped @ 0ccf049
 * (`ui/reshaped/pillnav/PillGlassShader.kt`, `PillGlassBackdrop.kt` and the `LiquidPillGlass` draw in
 * `PillNavigationBar.kt`), itself a port of the NuvioMobile pill. Adapted: it draws the Glass chrome's
 * pills instead of Reshaped's own bar; whether a device gets it is [com.nuvio.tv.fork.uistyle.UiStyleRules.glassEffect]
 * (Android 13+, the AdaptiveResources STANDARD tier, Lightweight effects off) instead of a second RAM
 * probe and the sidebar blur preference; the moving item lens is not drawn, the Glass menu keeps its
 * own sliding indicator.
 */

/**
 * The screen behind the chrome, recorded once per content frame into a display list the pills replay
 * under the lens. No blur and no pixel copy: each pill only redraws its own small area.
 */
@Stable
internal class LiquidGlassBackdrop(val layer: GraphicsLayer) {
    var contentOrigin by mutableStateOf(Offset.Zero)
        internal set

    /** Bumped after every content recording, and by [refreshWhileShown], so the pills redraw with it. */
    var version by mutableIntStateOf(0)
        private set

    @Volatile
    private var pokeRequested = false

    internal fun onRecorded() {
        // Written from the content's draw pass; never read there, so it can't re-invalidate the content.
        Snapshot.withoutReadObservation { version += 1 }
    }

    /** A key was pressed: the screen is about to animate (focus moves, hero crossfades), so follow it closely. */
    fun poke() {
        pokeRequested = true
    }

    /**
     * Content animating in its own layer updates the recording without re-running the recorder's draw,
     * so while the chrome is shown the pills redraw every frame for a moment after each key press, and
     * once a second when idle (the rotating hero).
     */
    suspend fun refreshWhileShown() {
        var activeUntil = 0L
        while (true) {
            val idle = withFrameNanos { now ->
                if (pokeRequested) {
                    pokeRequested = false
                    activeUntil = now + ACTIVE_WINDOW_NANOS
                }
                version += 1
                now >= activeUntil
            }
            if (idle) delay(IDLE_REFRESH_MS)
        }
    }

    /** Drops the last recording (and the nodes and images it holds) while the chrome is hidden. */
    fun clear(density: Density, layoutDirection: LayoutDirection) {
        layer.record(density, layoutDirection, IntSize(1, 1)) {}
    }

    /** Draws the recorded content lined up with the node at [coordinates], including its current scale. */
    fun DrawScope.drawAligned(coordinates: LayoutCoordinates?) {
        val coords = coordinates?.takeIf { it.isAttached } ?: return
        val origin = coords.localToRoot(Offset.Zero)
        val right = coords.localToRoot(Offset(size.width, 0f))
        val rootScale = ((right.x - origin.x) / size.width).takeIf { it > 0.01f } ?: 1f
        val shift = origin - contentOrigin
        scale(1f / rootScale, pivot = Offset.Zero) {
            translate(-shift.x, -shift.y) { drawLayer(layer) }
        }
    }

    private companion object {
        const val ACTIVE_WINDOW_NANOS = 1_500_000_000L
        const val IDLE_REFRESH_MS = 1_000L
    }
}

/** The recorder, or null where the chrome uses blur or the flat tint instead. */
@Composable
internal fun rememberLiquidGlassBackdrop(enabled: Boolean): LiquidGlassBackdrop? {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { LiquidGlassBackdrop(layer) }
}

/** On the content the chrome floats over: records it for the lens and draws it as usual. */
internal fun Modifier.liquidGlassSource(backdrop: LiquidGlassBackdrop?): Modifier {
    if (backdrop == null) return this
    return onGloballyPositioned { backdrop.contentOrigin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
            backdrop.onRecorded()
        }
}

/** Under a capsule: the recorded screen bent by [LiquidGlassShader], then the capsule's own content. */
@Composable
internal fun Modifier.liquidGlassSurface(backdrop: LiquidGlassBackdrop, focus: Float, tint: Color): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return liquidGlassSurfaceApi33(backdrop, focus, tint)
}

/** Under holes such as video surfaces the glass reads as dark glass. */
private val LiquidGlassBase = Color(0xFF1C1C1E)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.liquidGlassSurfaceApi33(backdrop: LiquidGlassBackdrop, focus: Float, tint: Color): Modifier {
    val shader = remember { RuntimeShader(LiquidGlassShader) }
    val glass = rememberGraphicsLayer()
    // Kept out of state: read while drawing; a move redraws through the backdrop's version.
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onGloballyPositioned { coordinates[0] = it }
        .drawWithContent {
            backdrop.version // Redraw whenever the screen behind was re-recorded.
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("density", density)
            shader.setFloatUniform("outset", 0f)
            shader.setFloatUniform("tint", tint.red, tint.green, tint.blue)
            shader.setFloatUniform("lens", 0f, 0f, 0f, 0f)
            shader.setFloatUniform("focus", focus)
            glass.renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop").asComposeRenderEffect()
            glass.record {
                drawRect(LiquidGlassBase)
                with(backdrop) { drawAligned(coordinates[0]) }
            }
            drawLayer(glass)
            drawContent()
        }
}

// Reshaped's notes on the shader: the whole capsule is a lens with a circular edge profile that
// magnifies towards the rim; faint ordered dispersion near the rim, a vibrancy boost and a light tint;
// a thin specular line lit from the top left with a dimmer echo bottom right; an optional item lens
// (unused here, `lens` is empty). The backdrop is not blurred, so samples are softened in the shader.
internal const val LiquidGlassShader = """
uniform shader backdrop;
uniform float2 resolution;
uniform float density;
uniform float outset;
uniform half3 tint;
uniform float4 lens;
uniform float focus;

// TVs draw the pill over the sharp screen (no blur pass), so each sample is a small 4-tap soften:
// enough to keep the labels readable, far cheaper than a blur.
half4 soft(float2 p) {
    float2 a = float2(1.5, 0.75) * density;
    float2 b = float2(-0.75, 1.5) * density;
    return (backdrop.eval(p + a) + backdrop.eval(p - a) + backdrop.eval(p + b) + backdrop.eval(p - b)) * 0.25;
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

// Signed distance to a horizontal capsule and its outward normal, packed as (distance, normal.x, normal.y).
float3 capsule(float2 local, float2 halfSize) {
    float radius = halfSize.y;
    float2 q = float2(max(abs(local.x) - halfSize.x + radius, 0.0), local.y);
    float d = length(q);
    float2 n = float2(q.x * sign(local.x), q.y) / max(d, 0.001);
    return float3(d - radius, n);
}

half4 main(float2 position) {
    float2 halfSize = resolution * 0.5 - outset;
    float3 bar = capsule(position - resolution * 0.5, halfSize);
    float sd = bar.x;
    float coverage = 1.0 - smoothstep(-0.5, 0.5, sd);
    if (coverage <= 0.0) return half4(0.0);
    float2 normal = bar.yz;

    // 0 deep inside, 1 at the rim.
    float edge = clamp(1.0 + sd / (22.0 * density), 0.0, 1.0);
    float bend = circleMap(edge) * 20.0 * density;
    float2 samplePos = position - normal * bend;
    float2 spread = normal * bend * 0.07 * edge;

    float lensMix = 0.0;
    float lensRim = 0.0;
    float2 lensNormal = float2(0.0);
    if (lens.z > lens.x) {
        float2 lensCenter = (lens.xy + lens.zw) * 0.5;
        float2 lensHalf = (lens.zw - lens.xy) * 0.5;
        float3 l = capsule(position - lensCenter, lensHalf);
        lensMix = 1.0 - smoothstep(-0.5, 0.5, l.x);
        if (lensMix > 0.0) {
            float lensEdge = clamp(1.0 + l.x / lensHalf.y, 0.0, 1.0);
            float magnify = 0.9 - 0.04 * focus;
            float2 lensSamplePos = lensCenter + (samplePos - lensCenter) * magnify
                - l.yz * circleMap(lensEdge) * lensHalf.y * 0.45;
            samplePos = mix(samplePos, lensSamplePos, lensMix);
            spread += l.yz * circleMap(lensEdge) * 1.5 * density * lensMix;
            lensRim = smoothstep(1.5 * density, 0.0, -l.x) * lensMix;
            lensNormal = l.yz;
        }
    }

    half3 color = half3(
        soft(samplePos + spread).r,
        soft(samplePos).g,
        soft(samplePos - spread).b
    );
    half luminance = dot(color, half3(0.2126, 0.7152, 0.0722));
    color = clamp(mix(half3(luminance), color, 1.45), 0.0, 1.0);
    color = mix(color, mix(half3(28.0, 28.0, 30.0) / 255.0, tint, 0.08), 0.46);

    // Thickness: the glass darkens slightly towards its lower rim, and the selected lens is a touch brighter.
    color *= 1.0 - 0.14 * pow(edge, 3.0) * max(normal.y, 0.0);
    color = mix(color, half3(1.0), (0.07 + 0.05 * focus) * lensMix);
    color += tint * 0.05 * pow(edge, 3.0);

    float2 light = float2(-0.7071, -0.7071);
    float facing = dot(normal, light);
    float rimLine = smoothstep(1.6 * density, 0.0, -sd);
    float specular = rimLine * pow(abs(facing), 1.6) * (facing > 0.0 ? 0.55 : 0.2);
    specular += pow(edge, 4.0) * max(facing, 0.0) * 0.08;
    float lensFacing = dot(lensNormal, light);
    specular += lensRim * pow(abs(lensFacing), 1.4) * (lensFacing > 0.0 ? 0.42 : 0.16);
    color += half3(specular);

    return half4(clamp(color, 0.0, 1.0) * coverage, coverage);
}
"""
