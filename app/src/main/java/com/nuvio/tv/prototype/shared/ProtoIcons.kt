package com.nuvio.tv.prototype.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Hand-drawn line icons on a 24-unit grid. Stroke weight is a per-concept token. */
enum class Glyph(val mirrorInRtl: Boolean = false) {
    PLAY, PAUSE, SEARCH, HOME, LIBRARY, SETTINGS, PROFILE, SUBTITLES, AUDIO, NEXT, PREVIOUS,
    REPLAY10, FORWARD10, INFO, PLUS, CHECK, BACK(true), CHEVRON_LEFT(true), CHEVRON_RIGHT(true),
    CHEVRON_DOWN, CHEVRON_UP, MIC, FILM, TV, SPARKLE, BOLT, GEM, BALANCE, FEATHER, LIST, CLOUD,
    SIGNAL, STAR, CLOSE, EPISODES, HEART, GRID, SPEAKER, GLOBE, CLOCK, TRAILER, SOURCES, MOVIES, SERIES,
}

@Composable
fun ProtoIcon(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.White,
    stroke: Dp = 1.6.dp,
) {
    val mirror = glyph.mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { if (mirror) scaleX = -1f },
    ) {
        val s = this.size.minDimension / 24f
        val sw = stroke.toPx() / s
        scale(s, s, pivot = Offset.Zero) {
            drawGlyph(glyph, color, sw)
        }
    }
}

private fun path(block: Path.() -> Unit) = Path().apply(block)

private fun DrawScope.drawGlyph(g: Glyph, c: Color, sw: Float) {
    val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    fun circle(x: Float, y: Float, r: Float, fill: Boolean = false) = drawCircle(c, r, Offset(x, y), style = if (fill) Fill else st)
    fun p(fill: Boolean = false, block: Path.() -> Unit) = drawPath(path(block), c, style = if (fill) Fill else st)
    when (g) {
        Glyph.PLAY -> p(fill = true) { moveTo(7f, 4.5f); lineTo(19.5f, 12f); lineTo(7f, 19.5f); close() }
        Glyph.PAUSE -> {
            drawRoundRect(c, Offset(6.5f, 4.5f), Size(3.6f, 15f), androidx.compose.ui.geometry.CornerRadius(1f))
            drawRoundRect(c, Offset(13.9f, 4.5f), Size(3.6f, 15f), androidx.compose.ui.geometry.CornerRadius(1f))
        }
        Glyph.SEARCH -> { circle(10.5f, 10.5f, 6.5f); line(15.2f, 15.2f, 20f, 20f) }
        Glyph.HOME -> p { moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f); moveTo(6f, 9.5f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 9.5f) }
        Glyph.LIBRARY -> { line(5f, 4f, 5f, 20f); line(9.5f, 4f, 9.5f, 20f); p { moveTo(13f, 5f); lineTo(16.5f, 4f); lineTo(20.5f, 19f); lineTo(17f, 20f); close() } }
        Glyph.SETTINGS -> {
            circle(12f, 12f, 3f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                line(12f + 6f * kotlin.math.cos(a).toFloat(), 12f + 6f * kotlin.math.sin(a).toFloat(), 12f + 8.5f * kotlin.math.cos(a).toFloat(), 12f + 8.5f * kotlin.math.sin(a).toFloat())
            }
            circle(12f, 12f, 6f)
        }
        Glyph.PROFILE -> { circle(12f, 8.5f, 4f); p { moveTo(4.5f, 20f); cubicTo(5.5f, 15f, 18.5f, 15f, 19.5f, 20f) } }
        Glyph.SUBTITLES -> {
            drawRoundRect(c, Offset(3f, 5f), Size(18f, 14f), androidx.compose.ui.geometry.CornerRadius(2.5f), style = st)
            line(6.5f, 12.5f, 10f, 12.5f); line(12f, 12.5f, 17.5f, 12.5f); line(6.5f, 15.5f, 13f, 15.5f); line(15f, 15.5f, 17.5f, 15.5f)
        }
        Glyph.AUDIO -> { p { moveTo(4f, 9.5f); lineTo(7.5f, 9.5f); lineTo(12f, 5.5f); lineTo(12f, 18.5f); lineTo(7.5f, 14.5f); lineTo(4f, 14.5f); close() }; p { moveTo(15.5f, 9f); cubicTo(17f, 10.5f, 17f, 13.5f, 15.5f, 15f) }; p { moveTo(18f, 6.5f); cubicTo(21f, 9.5f, 21f, 14.5f, 18f, 17.5f) } }
        Glyph.NEXT -> { p(fill = true) { moveTo(5f, 5f); lineTo(15f, 12f); lineTo(5f, 19f); close() }; line(18.5f, 5f, 18.5f, 19f) }
        Glyph.PREVIOUS -> { p(fill = true) { moveTo(19f, 5f); lineTo(9f, 12f); lineTo(19f, 19f); close() }; line(5.5f, 5f, 5.5f, 19f) }
        Glyph.REPLAY10 -> { p { moveTo(5f, 12f); cubicTo(5f, 6f, 12f, 3.5f, 16.5f, 6.5f); cubicTo(21f, 9.5f, 20f, 18f, 13.5f, 19.5f) }; p { moveTo(3f, 9f); lineTo(5f, 12.2f); lineTo(8.2f, 10.2f) }; line(9f, 11f, 9f, 16f); circle(13f, 13.5f, 2.3f) }
        Glyph.FORWARD10 -> { p { moveTo(19f, 12f); cubicTo(19f, 6f, 12f, 3.5f, 7.5f, 6.5f); cubicTo(3f, 9.5f, 4f, 18f, 10.5f, 19.5f) }; p { moveTo(21f, 9f); lineTo(19f, 12.2f); lineTo(15.8f, 10.2f) }; line(9.5f, 11f, 9.5f, 16f); circle(13.5f, 13.5f, 2.3f) }
        Glyph.INFO -> { circle(12f, 12f, 8.5f); line(12f, 11f, 12f, 16.5f); circle(12f, 7.8f, 0.6f, fill = true) }
        Glyph.PLUS -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
        Glyph.CHECK -> p { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 6.5f) }
        Glyph.BACK -> { line(5f, 12f, 19f, 12f); p { moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f) } }
        Glyph.CHEVRON_LEFT -> p { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
        Glyph.CHEVRON_RIGHT -> p { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
        Glyph.CHEVRON_DOWN -> p { moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f) }
        Glyph.CHEVRON_UP -> p { moveTo(5f, 15f); lineTo(12f, 8f); lineTo(19f, 15f) }
        Glyph.MIC -> { drawRoundRect(c, Offset(9f, 3.5f), Size(6f, 11f), androidx.compose.ui.geometry.CornerRadius(3f), style = st); p { moveTo(5.5f, 11.5f); cubicTo(5.5f, 19.5f, 18.5f, 19.5f, 18.5f, 11.5f) }; line(12f, 17.5f, 12f, 21f) }
        Glyph.FILM -> { drawRoundRect(c, Offset(3.5f, 5f), Size(17f, 14f), androidx.compose.ui.geometry.CornerRadius(1.5f), style = st); line(7.5f, 5f, 7.5f, 19f); line(16.5f, 5f, 16.5f, 19f); line(3.5f, 12f, 7.5f, 12f); line(16.5f, 12f, 20.5f, 12f) }
        Glyph.TV, Glyph.SERIES -> { drawRoundRect(c, Offset(3f, 5.5f), Size(18f, 12f), androidx.compose.ui.geometry.CornerRadius(1.5f), style = st); line(8f, 20.5f, 16f, 20.5f) }
        Glyph.MOVIES -> { drawRoundRect(c, Offset(3.5f, 8f), Size(17f, 12f), androidx.compose.ui.geometry.CornerRadius(1.5f), style = st); p { moveTo(3.5f, 8f); lineTo(19f, 3.5f) }; line(8f, 6.7f, 10f, 8f); line(13f, 5.2f, 15f, 6.6f) }
        Glyph.SPARKLE -> p { moveTo(12f, 3f); cubicTo(12.8f, 9f, 15f, 11.2f, 21f, 12f); cubicTo(15f, 12.8f, 12.8f, 15f, 12f, 21f); cubicTo(11.2f, 15f, 9f, 12.8f, 3f, 12f); cubicTo(9f, 11.2f, 11.2f, 9f, 12f, 3f); close() }
        Glyph.BOLT -> p { moveTo(13.5f, 3f); lineTo(5.5f, 13.5f); lineTo(11.5f, 13.5f); lineTo(10.5f, 21f); lineTo(18.5f, 10.5f); lineTo(12.5f, 10.5f); close() }
        Glyph.GEM -> { p { moveTo(7f, 4.5f); lineTo(17f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 20f); lineTo(3f, 9.5f); close() }; line(3f, 9.5f, 21f, 9.5f); p { moveTo(9f, 9.5f); lineTo(12f, 20f); lineTo(15f, 9.5f) } }
        Glyph.BALANCE -> { line(12f, 4f, 12f, 20f); line(5f, 7f, 19f, 7f); line(8f, 20f, 16f, 20f); p { moveTo(2.5f, 14f); lineTo(5f, 7f); lineTo(7.5f, 14f); close() }; p { moveTo(16.5f, 14f); lineTo(19f, 7f); lineTo(21.5f, 14f); close() } }
        Glyph.FEATHER -> { p { moveTo(19f, 5f); cubicTo(11f, 5f, 6f, 10f, 6f, 18f); lineTo(14f, 18f); cubicTo(18f, 14f, 19.5f, 10f, 19f, 5f); close() }; line(4f, 20f, 15f, 9f) }
        Glyph.LIST -> { line(8f, 7f, 20f, 7f); line(8f, 12f, 20f, 12f); line(8f, 17f, 20f, 17f); circle(4.5f, 7f, 0.8f, true); circle(4.5f, 12f, 0.8f, true); circle(4.5f, 17f, 0.8f, true) }
        Glyph.CLOUD -> p { moveTo(7f, 18.5f); cubicTo(3f, 18.5f, 2.5f, 12.5f, 6.5f, 12f); cubicTo(7f, 7f, 14f, 5.5f, 16f, 10f); cubicTo(20.5f, 9.5f, 22f, 18.5f, 16.5f, 18.5f); close() }
        Glyph.SIGNAL -> { line(5f, 19f, 5f, 16f); line(9.5f, 19f, 9.5f, 13f); line(14f, 19f, 14f, 9.5f); line(18.5f, 19f, 18.5f, 5.5f) }
        Glyph.STAR -> p { moveTo(12f, 3.5f); lineTo(14.6f, 9f); lineTo(20.5f, 9.6f); lineTo(16f, 13.5f); lineTo(17.3f, 19.5f); lineTo(12f, 16.5f); lineTo(6.7f, 19.5f); lineTo(8f, 13.5f); lineTo(3.5f, 9.6f); lineTo(9.4f, 9f); close() }
        Glyph.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
        Glyph.EPISODES -> { drawRoundRect(c, Offset(3f, 8f), Size(14f, 12f), androidx.compose.ui.geometry.CornerRadius(1.5f), style = st); p { moveTo(6f, 5f); lineTo(20f, 5f); lineTo(20f, 16f) } }
        Glyph.HEART -> p { moveTo(12f, 19.5f); cubicTo(4f, 14f, 2.5f, 9.5f, 5f, 6.5f); cubicTo(7.5f, 3.8f, 11f, 5f, 12f, 7.5f); cubicTo(13f, 5f, 16.5f, 3.8f, 19f, 6.5f); cubicTo(21.5f, 9.5f, 20f, 14f, 12f, 19.5f); close() }
        Glyph.GRID -> { listOf(4f, 13.5f).forEach { x -> listOf(4f, 13.5f).forEach { y -> drawRoundRect(c, Offset(x, y), Size(6.5f, 6.5f), androidx.compose.ui.geometry.CornerRadius(1.2f), style = st) } } }
        Glyph.SPEAKER -> { drawRoundRect(c, Offset(6f, 3f), Size(12f, 18f), androidx.compose.ui.geometry.CornerRadius(2f), style = st); circle(12f, 14f, 3.2f); circle(12f, 7.5f, 1f, true) }
        Glyph.GLOBE -> { circle(12f, 12f, 8.5f); line(3.5f, 12f, 20.5f, 12f); p { moveTo(12f, 3.5f); cubicTo(7.5f, 8f, 7.5f, 16f, 12f, 20.5f) }; p { moveTo(12f, 3.5f); cubicTo(16.5f, 8f, 16.5f, 16f, 12f, 20.5f) } }
        Glyph.CLOCK -> { circle(12f, 12f, 8.5f); p { moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f) } }
        Glyph.TRAILER -> { drawRoundRect(c, Offset(3f, 5f), Size(18f, 14f), androidx.compose.ui.geometry.CornerRadius(2f), style = st); p(fill = true) { moveTo(10f, 9f); lineTo(15f, 12f); lineTo(10f, 15f); close() } }
        Glyph.SOURCES -> { line(4f, 7f, 20f, 7f); line(4f, 12f, 16f, 12f); line(4f, 17f, 12f, 17f); circle(19f, 15.5f, 2.5f) }
    }
}
