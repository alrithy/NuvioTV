package com.nuvio.tv.prototype.shared.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.nuvio.tv.prototype.shared.data.ArtMotif
import com.nuvio.tv.prototype.shared.data.ArtPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Deterministic pseudo random generator so every render of a title looks identical. */
class Rng(seed: Int) {
    private var s: Long = (seed.toLong() * 6364136223846793005L + 1442695040888963407L) and 0x7fffffffffffL
    fun next(): Float {
        s = (s * 25214903917L + 11L) and 0xffffffffffffL
        return ((s ushr 17) and 0xffffff).toFloat() / 0xffffff.toFloat()
    }
    fun range(a: Float, b: Float) = a + (b - a) * next()
}

typealias DrawOp = DrawScope.() -> Unit

private fun Color.a(alpha: Float) = copy(alpha = alpha.coerceIn(0f, 1f))

/**
 * Builds the draw operations for a scene. Called from drawWithCache so geometry is computed once
 * per size and reused on every frame.
 */
fun buildScene(size: Size, motif: ArtMotif, pal: ArtPalette, seed: Int, variant: Int = 0): List<DrawOp> {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return emptyList()
    val rng = Rng(seed * 31 + variant * 977)
    val ops = ArrayList<DrawOp>(64)
    val shift = ((variant % 5) - 2) * 0.03f
    when (motif) {
        ArtMotif.DUNES -> dunes(ops, w, h, pal, rng, night = false, shift = shift)
        ArtMotif.DESERT_NIGHT -> dunes(ops, w, h, pal, rng, night = true, shift = shift)
        ArtMotif.SPACE -> space(ops, w, h, pal, rng, seed, shift)
        ArtMotif.CITY_NIGHT -> city(ops, w, h, pal, rng, shift)
        ArtMotif.NEON_RAIN -> neon(ops, w, h, pal, rng, shift)
        ArtMotif.FIRE -> fire(ops, w, h, pal, rng, shift)
        ArtMotif.OCEAN -> ocean(ops, w, h, pal, rng, storm = false, shift = shift)
        ArtMotif.STORM -> ocean(ops, w, h, pal, rng, storm = true, shift = shift)
        ArtMotif.MOUNTAIN -> mountain(ops, w, h, pal, rng, seed, shift)
        ArtMotif.FOREST -> forest(ops, w, h, pal, rng, shift)
        ArtMotif.INTERIOR -> interior(ops, w, h, pal, rng, shift)
        ArtMotif.CORRIDOR -> corridor(ops, w, h, pal, rng, shift)
        ArtMotif.SKY -> sky(ops, w, h, pal, rng, shift)
        ArtMotif.ARCHES -> arches(ops, w, h, pal, rng, seed, shift)
    }
    // Lens vignette shared by every scene.
    ops += {
        drawRect(
            Brush.radialGradient(
                0.55f to Color.Transparent,
                1f to Color.Black.a(0.55f),
                center = Offset(w / 2f, h / 2f),
                radius = max(w, h) * 0.75f,
            ),
        )
    }
    return ops
}

private fun glow(center: Offset, radius: Float, color: Color, alpha: Float): DrawOp = {
    drawCircle(
        Brush.radialGradient(
            0f to color.a(alpha),
            0.35f to color.a(alpha * 0.45f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun skyOp(w: Float, h: Float, top: Color, horizon: Color, horizonY: Float, below: Color): DrawOp = {
    drawRect(
        Brush.verticalGradient(
            0f to top,
            (horizonY / h).coerceIn(0.05f, 0.95f) to horizon,
            1f to below,
            startY = 0f,
            endY = h,
        ),
    )
}

/** Smooth ridge from left to right through random crests. */
private fun ridgePath(w: Float, h: Float, baseY: Float, amp: Float, points: Int, rng: Rng, sharp: Boolean): Path {
    val path = Path()
    path.moveTo(-10f, h + 10f)
    val ys = FloatArray(points + 1) { baseY - rng.next() * amp }
    path.lineTo(-10f, ys[0])
    for (i in 1..points) {
        val x0 = w * (i - 1) / points
        val x1 = w * i / points
        if (sharp) {
            val peakX = x0 + (x1 - x0) * rng.range(0.3f, 0.7f)
            path.lineTo(peakX, min(ys[i - 1], ys[i]) - amp * rng.range(0.1f, 0.6f))
            path.lineTo(x1, ys[i])
        } else {
            val cx = (x0 + x1) / 2f
            path.cubicTo(cx, ys[i - 1], cx, ys[i], x1, ys[i])
        }
    }
    path.lineTo(w + 10f, h + 10f)
    path.close()
    return path
}

private fun duneCrest(w: Float, h: Float, baseY: Float, amp: Float, rng: Rng): Path {
    val path = Path()
    path.moveTo(-20f, h + 20f)
    path.lineTo(-20f, baseY)
    val segments = 3
    var x = -20f
    var y = baseY
    for (i in 0 until segments) {
        val nx = w * (i + 1) / segments + rng.range(-w * 0.08f, w * 0.08f)
        val peakX = x + (nx - x) * rng.range(0.55f, 0.8f)
        val peakY = baseY - amp * rng.range(0.4f, 1f)
        // long windward slope, sharp crest, steep slip face
        path.cubicTo(x + (peakX - x) * 0.5f, y, peakX - (peakX - x) * 0.25f, peakY, peakX, peakY)
        val ny = baseY + amp * rng.range(-0.1f, 0.3f)
        path.cubicTo(peakX + (nx - peakX) * 0.15f, peakY + amp * 0.2f, peakX + (nx - peakX) * 0.4f, ny, nx, ny)
        x = nx
        y = ny
    }
    path.lineTo(w + 20f, y)
    path.lineTo(w + 20f, h + 20f)
    path.close()
    return path
}

private fun stars(ops: MutableList<DrawOp>, w: Float, h: Float, maxY: Float, count: Int, rng: Rng, tint: Color) {
    val pts = List(count) { Triple(rng.next() * w, rng.next() * maxY, rng.next()) }
    ops += {
        pts.forEach { (x, y, r) ->
            drawCircle(tint.a(0.25f + r * 0.6f), radius = (0.4f + r * 1.2f) * (w / 960f).coerceAtLeast(0.5f), center = Offset(x, y))
        }
    }
}

private fun dunes(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, night: Boolean, shift: Float) {
    val horizonY = h * (0.56f + shift)
    ops += skyOp(w, h, pal.sky, pal.horizon, horizonY, pal.ground)
    val sun = Offset(w * (0.66f - shift * 2), horizonY - h * 0.14f)
    if (night) {
        stars(ops, w, h, horizonY, 160, rng, Color.White)
        ops += glow(sun, min(w, h) * 0.35f, pal.accent, 0.18f)
        ops += { drawCircle(pal.accent.a(0.95f), radius = min(w, h) * 0.035f, center = sun) }
    } else {
        ops += glow(sun, max(w, h) * 0.55f, pal.horizon, 0.55f)
        ops += glow(sun, min(w, h) * 0.22f, pal.accent, 0.7f)
        ops += { drawCircle(pal.accent.a(0.96f), radius = min(w, h) * 0.05f, center = sun) }
    }
    // Horizon haze band
    ops += {
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to pal.horizon.a(if (night) 0.18f else 0.45f),
                1f to Color.Transparent,
                startY = horizonY - h * 0.12f,
                endY = horizonY + h * 0.1f,
            ),
        )
    }
    val layers = 4
    for (i in 0 until layers) {
        val t = i / (layers - 1f)
        val base = horizonY + h * (0.02f + t * 0.3f)
        val amp = h * (0.05f + t * 0.1f)
        val path = duneCrest(w, h, base, amp, rng)
        val light = lerp(lerp(pal.horizon, pal.mid, 0.35f + t * 0.4f), pal.ground, if (night) 0.55f + t * 0.3f else t * 0.35f)
        val dark = lerp(pal.mid, pal.ground, if (night) 0.8f else 0.35f + t * 0.55f)
        ops += {
            drawPath(path, Brush.verticalGradient(listOf(light, dark), startY = base - amp, endY = base + h * 0.25f))
            drawPath(path, pal.accent.a(if (night) 0.18f else 0.22f), style = Stroke(width = 1.2f))
        }
    }
    if (rng.next() > 0.35f) {
        // A lone figure on a distant crest: scale is what makes it cinematic.
        val fx = w * rng.range(0.25f, 0.45f)
        val fy = horizonY + h * 0.07f
        val fh = h * 0.028f
        ops += {
            drawRect(pal.ground.a(0.9f), topLeft = Offset(fx - fh * 0.12f, fy - fh), size = Size(fh * 0.24f, fh))
            drawCircle(pal.ground.a(0.9f), radius = fh * 0.14f, center = Offset(fx, fy - fh - fh * 0.1f))
        }
    }
}

private fun space(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, seed: Int, shift: Float) {
    ops += { drawRect(pal.ground) }
    ops += glow(Offset(w * 0.3f, h * 0.35f), max(w, h) * 0.7f, pal.horizon, 0.9f)
    ops += glow(Offset(w * 0.75f, h * 0.2f), max(w, h) * 0.45f, pal.mid, 0.35f)
    stars(ops, w, h, h, 220, rng, Color.White)
    val c = Offset(w * (0.62f + shift), h * 0.5f)
    val r = min(w, h) * 0.16f
    if (seed % 2 == 1) {
        // Black hole with a luminous accretion disc.
        ops += glow(c, r * 3.2f, pal.accent, 0.35f)
        ops += {
            drawOval(
                Brush.horizontalGradient(listOf(Color.Transparent, pal.accent.a(0.9f), Color.White.a(0.95f), pal.accent.a(0.9f), Color.Transparent), startX = c.x - r * 3f, endX = c.x + r * 3f),
                topLeft = Offset(c.x - r * 3f, c.y - r * 0.28f),
                size = Size(r * 6f, r * 0.56f),
            )
            drawCircle(pal.accent.a(0.7f), radius = r * 1.08f, center = c, style = Stroke(width = r * 0.14f))
            drawCircle(Color.Black, radius = r, center = c)
            drawArc(
                pal.accent.a(0.85f),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(c.x - r * 1.35f, c.y - r * 1.35f), size = Size(r * 2.7f, r * 2.7f),
                style = Stroke(width = r * 0.1f),
            )
        }
    } else {
        val pc = Offset(w * 0.7f, h * 1.05f)
        val pr = max(w, h) * 0.45f
        ops += {
            drawCircle(Brush.radialGradient(listOf(pal.mid, pal.ground), center = Offset(pc.x - pr * 0.3f, pc.y - pr * 0.4f), radius = pr * 1.2f), radius = pr, center = pc)
            drawCircle(pal.accent.a(0.6f), radius = pr, center = pc, style = Stroke(width = 2.5f))
        }
        ops += glow(Offset(pc.x, pc.y - pr), pr * 0.6f, pal.accent, 0.25f)
    }
}

private fun city(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    val horizonY = h * (0.64f + shift)
    ops += skyOp(w, h, pal.sky, pal.horizon, horizonY, pal.ground)
    ops += glow(Offset(w * 0.5f, horizonY), max(w, h) * 0.6f, pal.horizon, 0.5f)
    stars(ops, w, h, horizonY * 0.6f, 40, rng, Color.White.a(0.6f))
    for (layer in 0..1) {
        val near = layer == 1
        var x = -10f
        val color = if (near) pal.ground else lerp(pal.sky, pal.ground, 0.55f)
        val buildings = ArrayList<Pair<Offset, Size>>()
        while (x < w + 10f) {
            val bw = w * rng.range(0.035f, if (near) 0.09f else 0.06f)
            val bh = h * rng.range(if (near) 0.14f else 0.08f, if (near) 0.44f else 0.24f)
            buildings += Offset(x, horizonY - bh + (if (near) h * 0.04f else 0f)) to Size(bw, bh + h)
            x += bw + w * rng.range(0f, 0.012f)
        }
        val windows = ArrayList<Offset>()
        val ws = w * 0.0045f
        if (near) {
            buildings.forEach { (o, s) ->
                var yy = o.y + ws * 3
                while (yy < horizonY + h * 0.03f) {
                    var xx = o.x + ws * 2
                    while (xx < o.x + s.width - ws * 2) {
                        if (rng.next() > 0.72f) windows += Offset(xx, yy)
                        xx += ws * 2.6f
                    }
                    yy += ws * 3.2f
                }
            }
        }
        ops += {
            buildings.forEach { (o, s) -> drawRect(color, o, s) }
            windows.forEachIndexed { i, o -> drawRect(pal.accent.a(0.35f + (i % 5) * 0.12f), o, Size(ws, ws * 1.3f)) }
        }
    }
    ops += {
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, pal.ground), startY = horizonY, endY = h),
            topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY),
        )
    }
}

private fun neon(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    ops += { drawRect(Brush.verticalGradient(listOf(pal.sky, pal.horizon, pal.ground))) }
    val bokeh = List(22) {
        Triple(Offset(rng.next() * w, h * rng.range(0.15f, 0.7f)), min(w, h) * rng.range(0.02f, 0.09f), if (rng.next() > 0.5f) pal.accent else pal.ui)
    }
    ops += {
        bokeh.forEach { (c, r, col) ->
            drawCircle(Brush.radialGradient(listOf(col.a(0.45f), col.a(0.12f), Color.Transparent), center = c, radius = r), radius = r, center = c)
        }
    }
    // A vertical neon sign with layered strokes to fake bloom.
    val sx = w * (0.72f + shift)
    val top = h * 0.16f
    val bottom = h * 0.62f
    ops += {
        for (k in 5 downTo 1) {
            drawLine(pal.accent.a(0.06f * (6 - k)), Offset(sx, top), Offset(sx, bottom), strokeWidth = w * 0.004f * k * 2, cap = StrokeCap.Round)
        }
        drawLine(Color.White.a(0.9f), Offset(sx, top), Offset(sx, bottom), strokeWidth = w * 0.002f, cap = StrokeCap.Round)
    }
    // Reflection streaks on wet asphalt.
    ops += {
        bokeh.take(10).forEach { (c, r, col) ->
            drawRect(
                Brush.verticalGradient(listOf(col.a(0.18f), Color.Transparent), startY = h * 0.72f, endY = h),
                topLeft = Offset(c.x - r * 0.3f, h * 0.72f), size = Size(r * 0.6f, h * 0.28f),
            )
        }
    }
    // Silhouette
    val fx = w * (0.42f - shift)
    val fh = h * 0.3f
    ops += {
        drawRect(Color.Black.a(0.92f), Offset(fx - fh * 0.1f, h * 0.72f - fh * 0.78f), Size(fh * 0.2f, fh * 0.78f))
        drawCircle(Color.Black.a(0.92f), fh * 0.08f, Offset(fx, h * 0.72f - fh * 0.86f))
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, pal.ground), startY = h * 0.7f, endY = h),
            topLeft = Offset(0f, h * 0.7f), size = Size(w, h * 0.3f),
        )
    }
    val rain = List(150) { Offset(rng.next() * w, rng.next() * h) to rng.range(0.03f, 0.08f) }
    ops += {
        rain.forEach { (o, len) ->
            drawLine(Color.White.a(0.12f), o, Offset(o.x - h * len * 0.2f, o.y + h * len), strokeWidth = 1f)
        }
    }
}

private fun fire(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    ops += { drawRect(Brush.verticalGradient(listOf(pal.sky, pal.horizon, pal.ground))) }
    val c = Offset(w * (0.5f + shift), h * 0.46f)
    val m = min(w, h)
    ops += glow(c, max(w, h) * 0.75f, pal.horizon, 0.9f)
    ops += glow(c, m * 0.62f, pal.mid, 0.85f)
    ops += glow(c, m * 0.34f, pal.accent, 0.9f)
    ops += glow(c, m * 0.12f, Color.White, 0.95f)
    val sparks = List(90) { Offset(rng.next() * w, rng.next() * h * 0.85f) to rng.next() }
    ops += {
        sparks.forEach { (o, r) -> drawCircle(pal.accent.a(0.2f + r * 0.6f), radius = 0.6f + r * 2.2f, center = o) }
    }
    val groundY = h * 0.78f
    val path = ridgePath(w, h, groundY, h * 0.08f, 14, rng, sharp = true)
    ops += { drawPath(path, Color.Black.a(0.95f)) }
}

private fun ocean(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, storm: Boolean, shift: Float) {
    val horizonY = h * (0.6f + shift)
    ops += skyOp(w, h, pal.sky, pal.horizon, horizonY, pal.ground)
    val sun = Offset(w * 0.64f, horizonY - h * 0.08f)
    ops += glow(sun, max(w, h) * 0.5f, pal.horizon, 0.7f)
    ops += { drawCircle(pal.accent.a(0.9f), radius = min(w, h) * 0.045f, center = sun) }
    val clouds = List(9) { Offset(rng.next() * w, h * rng.range(0.1f, 0.45f)) to Size(w * rng.range(0.25f, 0.55f), h * rng.range(0.05f, 0.12f)) }
    ops += {
        clouds.forEach { (o, s) ->
            drawOval(Brush.radialGradient(listOf(pal.ground.a(if (storm) 0.8f else 0.4f), Color.Transparent), center = Offset(o.x, o.y), radius = s.width / 2f), topLeft = Offset(o.x - s.width / 2, o.y - s.height / 2), size = s)
        }
    }
    ops += {
        drawRect(Brush.verticalGradient(listOf(lerp(pal.horizon, pal.ground, 0.4f), pal.ground), startY = horizonY, endY = h), topLeft = Offset(0f, horizonY), size = Size(w, h - horizonY))
    }
    val lines = List(40) { i -> Triple(horizonY + (h - horizonY) * (i / 40f), w * rng.range(0.02f, 0.08f) * (1 + i / 20f), rng.range(-0.02f, 0.02f)) }
    ops += {
        lines.forEachIndexed { i, (y, len, jitter) ->
            val x = sun.x + w * jitter * (1 + i / 10f)
            drawLine(pal.accent.a(0.55f * (1f - i / 40f)), Offset(x - len / 2, y), Offset(x + len / 2, y), strokeWidth = 1.5f)
        }
    }
    if (storm) {
        val cliff = Path().apply {
            moveTo(-10f, h)
            lineTo(-10f, h * 0.38f)
            lineTo(w * 0.08f, h * 0.42f)
            lineTo(w * 0.16f, h * 0.55f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.3f, h)
            close()
        }
        // A ship riding the swell.
        val sx = w * 0.46f
        val sy = horizonY + h * 0.02f
        val sw = w * 0.07f
        val hull = Path().apply {
            moveTo(sx - sw / 2, sy)
            lineTo(sx + sw / 2, sy)
            lineTo(sx + sw * 0.38f, sy + sw * 0.18f)
            lineTo(sx - sw * 0.4f, sy + sw * 0.18f)
            close()
        }
        ops += {
            drawPath(cliff, Color.Black.a(0.9f))
            drawPath(hull, Color.Black.a(0.85f))
            drawLine(Color.Black.a(0.85f), Offset(sx - sw * 0.15f, sy), Offset(sx - sw * 0.15f, sy - sw * 0.7f), strokeWidth = 2f)
            drawLine(Color.Black.a(0.85f), Offset(sx + sw * 0.15f, sy), Offset(sx + sw * 0.15f, sy - sw * 0.55f), strokeWidth = 2f)
            rotate(-8f, pivot = Offset(w / 2, h / 2)) {
                repeat(90) { k ->
                    val x = (k * 97 % 1000) / 1000f * w
                    val y = (k * 57 % 1000) / 1000f * h
                    drawLine(Color.White.a(0.07f), Offset(x, y), Offset(x, y + h * 0.05f), strokeWidth = 1f)
                }
            }
        }
    }
}

private fun mountain(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, seed: Int, shift: Float) {
    val horizonY = h * (0.5f + shift)
    ops += skyOp(w, h, pal.sky, pal.horizon, horizonY, pal.ground)
    ops += glow(Offset(w * 0.3f, horizonY - h * 0.2f), max(w, h) * 0.5f, pal.accent, 0.35f)
    if (seed % 7 == 4) {
        // A vessel hovering in the fog.
        val c = Offset(w * (0.58f + shift), h * 0.36f)
        val lens = Path().apply {
            moveTo(c.x, c.y - h * 0.24f)
            cubicTo(c.x + w * 0.05f, c.y - h * 0.1f, c.x + w * 0.05f, c.y + h * 0.1f, c.x, c.y + h * 0.2f)
            cubicTo(c.x - w * 0.05f, c.y + h * 0.1f, c.x - w * 0.05f, c.y - h * 0.1f, c.x, c.y - h * 0.24f)
            close()
        }
        ops += { drawPath(lens, lerp(pal.ground, pal.mid, 0.2f).a(0.9f)) }
    }
    for (i in 0 until 4) {
        val t = i / 3f
        val base = horizonY + h * (0.02f + t * 0.24f)
        val path = ridgePath(w, h, base, h * (0.16f - t * 0.05f), 6 + i * 2, rng, sharp = true)
        val col = lerp(lerp(pal.horizon, pal.mid, 0.5f), pal.ground, 0.15f + t * 0.8f)
        ops += {
            drawPath(path, col)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, pal.accent.a(0.12f), Color.Transparent), startY = base - h * 0.02f, endY = base + h * 0.1f))
        }
    }
}

private fun forest(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    ops += skyOp(w, h, pal.horizon, pal.sky, h * 0.6f, pal.ground)
    val shafts = List(5) { Offset(w * rng.range(0.1f, 0.9f), 0f) to w * rng.range(0.03f, 0.08f) }
    ops += {
        shafts.forEach { (o, sw) ->
            val p = Path().apply {
                moveTo(o.x, 0f)
                lineTo(o.x + sw, 0f)
                lineTo(o.x + sw * 4 + w * 0.1f, h)
                lineTo(o.x + w * 0.1f, h)
                close()
            }
            drawPath(p, Brush.verticalGradient(listOf(pal.accent.a(0.22f), Color.Transparent)))
        }
    }
    for (layer in 0..2) {
        val t = layer / 2f
        val count = 14 - layer * 4
        val trunks = List(count) { Offset(w * rng.next(), 0f) to w * rng.range(0.006f, 0.02f) * (1 + t * 2.5f) }
        val col = lerp(lerp(pal.sky, pal.mid, 0.6f), pal.ground, 0.2f + t * 0.8f)
        ops += {
            trunks.forEach { (o, tw) -> drawRect(col, Offset(o.x, 0f), Size(tw, h)) }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, pal.horizon.a(0.25f * (1 - t)), Color.Transparent), startY = h * 0.45f, endY = h * 0.9f))
        }
    }
    ops += { drawRect(Brush.verticalGradient(listOf(Color.Transparent, pal.ground), startY = h * 0.7f, endY = h)) }
}

private fun interior(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    ops += { drawRect(Brush.verticalGradient(listOf(lerp(pal.mid, pal.ground, 0.4f), pal.ground))) }
    val wx = w * (0.52f + shift)
    val wy = h * 0.16f
    val ww = w * 0.34f
    val wh = h * 0.48f
    ops += glow(Offset(wx + ww / 2, wy + wh / 2), max(w, h) * 0.55f, pal.horizon, 0.55f)
    ops += {
        drawRect(Brush.verticalGradient(listOf(pal.accent.a(0.95f), pal.horizon)), Offset(wx, wy), Size(ww, wh))
        val mullion = pal.ground.a(0.85f)
        drawRect(mullion, Offset(wx + ww / 2 - 3f, wy), Size(6f, wh))
        drawRect(mullion, Offset(wx, wy + wh * 0.45f), Size(ww, 5f))
        // Light pool on the floor
        val pool = Path().apply {
            moveTo(wx, h * 0.8f)
            lineTo(wx + ww, h * 0.8f)
            lineTo(wx + ww * 1.5f, h)
            lineTo(wx - ww * 0.2f, h)
            close()
        }
        drawPath(pool, Brush.verticalGradient(listOf(pal.horizon.a(0.35f), pal.horizon.a(0.08f)), startY = h * 0.8f, endY = h))
        drawRect(pal.ground.a(0.5f), Offset(0f, h * 0.78f), Size(w, 2f))
    }
    // Pendant lamp and a table silhouette.
    val lx = w * (0.26f - shift)
    ops += {
        drawLine(Color.Black.a(0.7f), Offset(lx, 0f), Offset(lx, h * 0.3f), strokeWidth = 2f)
    }
    ops += glow(Offset(lx, h * 0.32f), min(w, h) * 0.18f, pal.accent, 0.6f)
    ops += {
        drawCircle(pal.accent, radius = min(w, h) * 0.018f, center = Offset(lx, h * 0.32f))
        drawRect(Color.Black.a(0.85f), Offset(w * 0.12f, h * 0.66f), Size(w * 0.3f, h * 0.025f))
        drawRect(Color.Black.a(0.85f), Offset(w * 0.14f, h * 0.68f), Size(w * 0.012f, h * 0.14f))
        drawRect(Color.Black.a(0.85f), Offset(w * 0.39f, h * 0.68f), Size(w * 0.012f, h * 0.14f))
    }
    if (rng.next() > 0.3f) {
        val fx = wx + ww * 0.3f
        val fh = h * 0.34f
        ops += {
            drawRect(Color.Black.a(0.8f), Offset(fx - fh * 0.09f, h * 0.8f - fh * 0.8f), Size(fh * 0.18f, fh * 0.8f))
            drawCircle(Color.Black.a(0.8f), fh * 0.075f, Offset(fx, h * 0.8f - fh * 0.87f))
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun corridor(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    val vp = Offset(w * (0.5f + shift), h * 0.46f)
    val bw = w * 0.07f
    val bh = h * 0.12f
    val back = Pair(Offset(vp.x - bw, vp.y - bh), Offset(vp.x + bw, vp.y + bh))
    fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }
    val ceiling = quad(Offset(0f, 0f), Offset(w, 0f), Offset(back.second.x, back.first.y), back.first)
    val floor = quad(Offset(0f, h), Offset(w, h), back.second, Offset(back.first.x, back.second.y))
    val left = quad(Offset(0f, 0f), back.first, Offset(back.first.x, back.second.y), Offset(0f, h))
    val right = quad(Offset(w, 0f), Offset(back.second.x, back.first.y), back.second, Offset(w, h))
    ops += {
        drawPath(ceiling, Brush.verticalGradient(listOf(lerp(pal.sky, Color.White, 0.3f), pal.horizon), startY = 0f, endY = back.first.y))
        drawPath(left, Brush.horizontalGradient(listOf(lerp(pal.sky, Color.Black, 0.08f), pal.horizon), startX = 0f, endX = back.first.x))
        drawPath(right, Brush.horizontalGradient(listOf(pal.horizon, lerp(pal.sky, Color.Black, 0.12f)), startX = back.second.x, endX = w))
        drawPath(floor, Brush.verticalGradient(listOf(lerp(pal.ground, pal.mid, 0.3f), pal.ground), startY = back.second.y, endY = h))
        drawRect(lerp(pal.horizon, pal.ground, 0.5f), back.first, Size(bw * 2, bh * 2))
    }
    // Ceiling light panels receding to the vanishing point.
    val panels = (1..7).map { k ->
        val t0 = 1f - 1f / (1f + k * 0.55f)
        val t1 = 1f - 1f / (1f + k * 0.55f + 0.22f)
        t0 to t1
    }
    ops += {
        panels.forEach { (t0, t1) ->
            val y0 = back.first.y * t0
            val y1 = back.first.y * t1
            val half0 = (w / 2) * (1 - t0) * 0.35f + bw * 0.3f * t0
            val half1 = (w / 2) * (1 - t1) * 0.35f + bw * 0.3f * t1
            val p = quad(Offset(vp.x - half0, y0), Offset(vp.x + half0, y0), Offset(vp.x + half1, y1), Offset(vp.x - half1, y1))
            drawPath(p, Color.White.a(0.9f))
        }
    }
    // Doors on the walls.
    ops += {
        listOf(0.25f, 0.55f).forEach { t ->
            val x0 = back.first.x * t
            val x1 = back.first.x * (t + 0.12f)
            val topY0 = back.first.y * t + h * 0.22f * (1 - t)
            val topY1 = back.first.y * (t + 0.12f) + h * 0.22f * (1 - t - 0.12f)
            val botY0 = h - (h - back.second.y) * t
            val botY1 = h - (h - back.second.y) * (t + 0.12f)
            drawPath(quad(Offset(x0, topY0), Offset(x1, topY1), Offset(x1, botY1), Offset(x0, botY0)), lerp(pal.horizon, pal.mid, 0.6f))
        }
    }
    val fh = bh * 1.5f
    ops += {
        drawRect(Color.Black.a(0.85f), Offset(vp.x - fh * 0.08f, back.second.y - fh * 0.82f + bh * 0.3f), Size(fh * 0.16f, fh * 0.82f))
        drawCircle(Color.Black.a(0.85f), fh * 0.07f, Offset(vp.x, back.second.y - fh * 0.9f + bh * 0.3f))
    }
}

private fun sky(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, shift: Float) {
    val horizonY = h * (0.72f + shift)
    ops += skyOp(w, h, pal.sky, pal.horizon, horizonY, pal.mid)
    val sun = Offset(w * 0.72f, horizonY - h * 0.03f)
    ops += glow(sun, max(w, h) * 0.6f, pal.horizon, 0.75f)
    ops += glow(sun, min(w, h) * 0.18f, pal.accent, 0.8f)
    ops += { drawCircle(pal.accent, radius = min(w, h) * 0.04f, center = sun) }
    val clouds = List(16) { Offset(w * rng.next(), horizonY + h * rng.range(-0.02f, 0.25f)) to Size(w * rng.range(0.2f, 0.5f), h * rng.range(0.05f, 0.1f)) }
    ops += {
        clouds.forEach { (o, s) ->
            drawOval(Brush.verticalGradient(listOf(lerp(pal.horizon, Color.White, 0.2f).a(0.85f), pal.mid.a(0.9f)), startY = o.y - s.height / 2, endY = o.y + s.height / 2), Offset(o.x - s.width / 2, o.y - s.height / 2), s)
        }
    }
    val a = Offset(w * 0.05f, h * 0.55f)
    val b = Offset(w * (0.45f + shift), h * 0.2f)
    ops += {
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.a(0.7f)), start = a, end = b), a, b, strokeWidth = 3f, cap = StrokeCap.Round)
        val jet = Path().apply {
            moveTo(b.x + 14f, b.y - 6f)
            lineTo(b.x - 10f, b.y + 2f)
            lineTo(b.x - 4f, b.y + 8f)
            close()
        }
        drawPath(jet, Color.Black.a(0.8f))
    }
}

private fun arches(ops: MutableList<DrawOp>, w: Float, h: Float, pal: ArtPalette, rng: Rng, seed: Int, shift: Float) {
    ops += { drawRect(Brush.verticalGradient(listOf(pal.horizon, pal.sky, pal.mid))) }
    ops += { drawRect(Brush.verticalGradient(listOf(lerp(pal.sky, Color(0xFF6FA8D8), 0.35f), pal.horizon)), Offset.Zero, Size(w, h * 0.14f)) }
    val count = 4
    val gap = w / (count + 0.5f)
    val aw = gap * 0.55f
    val top = h * 0.3f
    val bottom = h * 0.78f
    val archPaths = (0 until count).map { i ->
        val cx = gap * (i + 0.75f) + w * shift
        Path().apply {
            moveTo(cx - aw / 2, bottom)
            lineTo(cx - aw / 2, top + aw * 0.5f)
            // pointed arch
            cubicTo(cx - aw / 2, top + aw * 0.1f, cx - aw * 0.1f, top, cx, top - aw * 0.12f)
            cubicTo(cx + aw * 0.1f, top, cx + aw / 2, top + aw * 0.1f, cx + aw / 2, top + aw * 0.5f)
            lineTo(cx + aw / 2, bottom)
            close()
        }
    }
    ops += {
        archPaths.forEach { p ->
            drawPath(p, Brush.verticalGradient(listOf(lerp(pal.ground, pal.mid, 0.2f), pal.ground), startY = top, endY = bottom))
        }
        // Courtyard light at the base of each opening
        archPaths.forEachIndexed { i, _ ->
            val cx = gap * (i + 0.75f) + w * shift
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, pal.horizon.a(0.45f)), startY = bottom - h * 0.12f, endY = bottom), Offset(cx - aw / 2, bottom - h * 0.12f), Size(aw, h * 0.12f))
        }
        // Long diagonal shadow
        val shadow = Path().apply {
            moveTo(0f, h * 0.2f)
            lineTo(w * 0.35f, h * 0.2f)
            lineTo(w * 0.85f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(shadow, Color.Black.a(0.18f))
        drawRect(Brush.verticalGradient(listOf(pal.mid, pal.ground), startY = bottom, endY = h), Offset(0f, bottom), Size(w, h - bottom))
    }
    if (seed == 127) {
        // A green bicycle leaning in the light.
        val bx = w * 0.62f
        val by = h * 0.86f
        val r = min(w, h) * 0.045f
        ops += {
            val c = pal.accent
            drawCircle(c, r, Offset(bx - r * 1.4f, by), style = Stroke(width = r * 0.14f))
            drawCircle(c, r, Offset(bx + r * 1.4f, by), style = Stroke(width = r * 0.14f))
            drawLine(c, Offset(bx - r * 1.4f, by), Offset(bx, by - r * 0.1f), strokeWidth = r * 0.14f)
            drawLine(c, Offset(bx, by - r * 0.1f), Offset(bx + r * 1.4f, by), strokeWidth = r * 0.14f)
            drawLine(c, Offset(bx, by - r * 0.1f), Offset(bx - r * 0.4f, by - r * 1.2f), strokeWidth = r * 0.14f)
            drawLine(c, Offset(bx - r * 0.4f, by - r * 1.2f), Offset(bx + r * 1.0f, by - r * 1.1f), strokeWidth = r * 0.14f)
            drawLine(c, Offset(bx + r * 1.0f, by - r * 1.1f), Offset(bx + r * 1.4f, by), strokeWidth = r * 0.14f)
        }
    }
}

/**
 * An abstract "blurred art" field built from the palette. Looks like a heavily blurred still,
 * costs a handful of gradients, and works on devices without RenderEffect.
 * [phase] in 0..1 slowly drifts the light sources for ambient concepts.
 */
fun buildColorField(size: Size, pal: ArtPalette, phase: Float = 0f, intensity: Float = 1f): List<DrawOp> {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return emptyList()
    val a = (phase * 2 * PI).toFloat()
    val ops = ArrayList<DrawOp>()
    ops += { drawRect(lerp(pal.ground, Color.Black, 0.35f)) }
    ops += glow(Offset(w * (0.25f + 0.05f * cos(a)), h * (0.3f + 0.05f * sin(a))), max(w, h) * 0.75f, pal.mid, 0.85f * intensity)
    ops += glow(Offset(w * (0.78f + 0.04f * sin(a)), h * (0.35f + 0.06f * cos(a))), max(w, h) * 0.6f, pal.horizon, 0.65f * intensity)
    ops += glow(Offset(w * (0.55f + 0.06f * cos(a * 1.3f)), h * 1.0f), max(w, h) * 0.55f, pal.accent, 0.35f * intensity)
    ops += glow(Offset(w * 0.1f, h * 0.95f), max(w, h) * 0.45f, pal.sky, 0.45f * intensity)
    ops += {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.a(0.35f))))
    }
    return ops
}
