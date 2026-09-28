package com.nuvio.tv.prototype.shared.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.data.ArtPalette
import com.nuvio.tv.prototype.shared.data.Episode
import com.nuvio.tv.prototype.shared.data.ProtoTitle

enum class ArtKind { BACKDROP, POSTER, STILL }

/**
 * Title artwork. The procedural illustration is always drawn first (it doubles as the loading
 * state and the offline mode); when remote artwork is enabled the real image fades in on top.
 *
 * [variant] produces a different "shot" of the same scene — used for filmstrips and episode stills.
 * [overlay] lets posters draw a typographic title that only shows while no remote poster covers it.
 */
@Composable
fun ProtoArtwork(
    title: ProtoTitle,
    kind: ArtKind,
    modifier: Modifier = Modifier,
    variant: Int = 0,
    episode: Episode? = null,
    alignment: Alignment = Alignment.Center,
    remote: Boolean = true,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val env = LocalProtoEnv.current
    val url = when (kind) {
        ArtKind.BACKDROP -> title.backdropUrl
        ArtKind.POSTER -> title.posterUrl
        ArtKind.STILL -> episode?.let { title.stillUrl(it.season, it.number) } ?: title.backdropUrl
    }
    val seedVariant = when (kind) {
        ArtKind.STILL -> variant + (episode?.let { it.season * 17 + it.number } ?: 0)
        ArtKind.POSTER -> variant + 3
        ArtKind.BACKDROP -> variant
    }
    var loaded by remember(url) { mutableStateOf(false) }
    Box(modifier.clipToBounds()) {
        ProceduralScene(title, Modifier.fillMaxSize(), seedVariant)
        if (overlay != null && !loaded) overlay()
        if (remote && env.remoteArtwork && url != null) {
            RemoteImage(url, alignment, Modifier.fillMaxSize()) { loaded = true }
        }
    }
}

@Composable
fun ProceduralScene(title: ProtoTitle, modifier: Modifier = Modifier, variant: Int = 0) {
    Box(
        modifier.drawWithCache {
            val ops = buildScene(size, title.motif, title.palette, title.seed, variant)
            onDrawBehind { ops.forEach { it() } }
        },
    )
}

/**
 * Large soft "blurred artwork" background. Uses real blur on Android 12+ when remote art is
 * available, and a palette color-field everywhere else.
 */
@Composable
fun BlurredArt(
    title: ProtoTitle,
    modifier: Modifier = Modifier,
    radius: Dp = 60.dp,
    drift: Boolean = false,
    intensity: Float = 1f,
) {
    val env = LocalProtoEnv.current
    Box(modifier.clipToBounds()) {
        ColorField(title.palette, Modifier.fillMaxSize(), drift = drift, intensity = intensity)
        if (env.supportsBlur) {
            ProtoArtwork(
                title, ArtKind.BACKDROP,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.2f; scaleY = 1.2f; alpha = 0.85f }
                    .blur(radius),
            )
        }
    }
}

@Composable
fun ColorField(palette: ArtPalette, modifier: Modifier = Modifier, drift: Boolean = false, intensity: Float = 1f) {
    val frozen = LocalProtoEnv.current.frozen
    if (drift && !frozen) {
        val t = rememberInfiniteTransition(label = "field")
        val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
        Box(
            modifier.drawWithCache {
                onDrawBehind { buildColorField(size, palette, phase, intensity).forEach { it() } }
            },
        )
    } else {
        Box(
            modifier.drawWithCache {
                val ops = buildColorField(size, palette, 0f, intensity)
                onDrawBehind { ops.forEach { it() } }
            },
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Film grain: a small noise tile repeated across the surface. Adds analogue texture to dark UIs
// and hides banding in large gradients on 8-bit panels.
// ------------------------------------------------------------------------------------------------

private var grainTile: ImageBitmap? = null

private fun grain(): ImageBitmap {
    grainTile?.let { return it }
    val size = 96
    val bmp = ImageBitmap(size, size)
    val canvas = Canvas(bmp)
    val paint = Paint()
    val rng = Rng(7)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val v = rng.next()
            if (v > 0.5f) {
                paint.color = if (v > 0.75f) Color.White.copy(alpha = (v - 0.75f) * 1.6f) else Color.Black.copy(alpha = (v - 0.5f) * 1.6f)
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
    }
    grainTile = bmp
    return bmp
}

fun Modifier.filmGrain(alpha: Float = 0.08f): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(grain(), TileMode.Repeated, TileMode.Repeated))
    onDrawWithContent {
        drawContent()
        drawRect(brush, alpha = alpha)
    }
}

/** Solid scrim helper for legibility over art. */
fun Modifier.scrim(color: Color): Modifier = drawBehind { drawRect(color) }
