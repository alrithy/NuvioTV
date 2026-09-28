package com.nuvio.tv.prototype.shared.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import com.nuvio.tv.prototype.shared.LocalProtoEnv

/**
 * Desaturates everything drawn inside. Used for "focus by light": unfocused artwork loses
 * colour as well as brightness, so focus reads from across the room without borders.
 */
fun Modifier.saturation(amount: Float): Modifier =
    if (amount >= 0.995f) this else drawWithCache {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(amount.coerceIn(0f, 1f)) }) }
        onDrawWithContent {
            drawIntoCanvas { c ->
                c.saveLayer(Rect(Offset.Zero, size), paint)
                drawContent()
                c.restore()
            }
        }
    }

/** Slow push-in used as the mock "video" in players and hero art. Frozen for screenshots. */
@Composable
fun Modifier.kenBurns(durationMs: Int = 28_000, maxScale: Float = 1.08f): Modifier {
    if (LocalProtoEnv.current.frozen) return this
    val t = rememberInfiniteTransition(label = "kb")
    val s by t.animateFloat(1f, maxScale, infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Reverse), label = "kbs")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}
