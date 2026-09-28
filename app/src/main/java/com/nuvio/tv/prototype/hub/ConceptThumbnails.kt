package com.nuvio.tv.prototype.hub

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import com.nuvio.tv.prototype.shared.art.buildColorField
import com.nuvio.tv.prototype.shared.art.buildScene
import com.nuvio.tv.prototype.shared.data.MockCatalog

/** Miniature "signature" of each concept's layout, drawn procedurally for the hub tiles. */
@Composable
fun ConceptThumbnail(number: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (number) {
            1 -> cinematic()
            2 -> riyadh()
            3 -> monolith()
            4 -> liquid()
            5 -> zero()
            6 -> console()
            7 -> editorial()
            8 -> ambient()
            9 -> arabia()
            else -> signature()
        }
    }
}

private fun DrawScope.scene(id: String, variant: Int = 0, topLeft: Offset = Offset.Zero, sz: Size = size) {
    val t = MockCatalog.title(id)
    translate(topLeft.x, topLeft.y) {
        clipRect(0f, 0f, sz.width, sz.height) {
            buildScene(sz, t.motif, t.palette, t.seed, variant).forEach { it() }
        }
    }
}

private fun DrawScope.cinematic() {
    drawRect(Color.Black)
    val bar = size.height * 0.16f
    scene("dune2", topLeft = Offset(0f, bar), sz = Size(size.width, size.height - bar * 2))
    drawRect(Color.Black, Offset(0f, size.height - bar), Size(size.width, bar))
    val fw = size.width * 0.18f
    for (i in 0 until 5) {
        drawRect(Color.White.copy(alpha = if (i == 1) 0.9f else 0.25f), Offset(size.width * 0.08f + i * (fw + 4f), size.height - bar * 0.62f), Size(fw, bar * 0.28f))
    }
}

private fun DrawScope.riyadh() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF050811), Color(0xFF15131C), Color(0xFF3A2A1A))))
    var x = 0f
    var i = 0
    while (x < size.width) {
        val bw = size.width * (0.04f + (i * 37 % 5) * 0.012f)
        val bh = size.height * (0.12f + (i * 53 % 7) * 0.05f)
        drawRect(Color(0xFF0B0B10), Offset(x, size.height - bh), Size(bw, bh))
        if (i % 3 == 0) drawRect(Color(0xFFC9A45C).copy(alpha = 0.7f), Offset(x + bw * 0.4f, size.height - bh + 4f), Size(2f, 2f))
        x += bw + 2f
        i++
    }
    drawLine(Color(0xFFC9A45C), Offset(size.width * 0.08f, size.height * 0.3f), Offset(size.width * 0.45f, size.height * 0.3f), strokeWidth = 1.2f)
    drawRect(Color(0xFF0F5A47), Offset(size.width * 0.08f, size.height * 0.4f), Size(6f, 6f))
}

private fun DrawScope.monolith() {
    drawRect(Color(0xFF1C1916))
    drawRect(Color(0xFF2A2622), Offset(0f, 0f), Size(size.width * 0.42f, size.height))
    scene("theeb", topLeft = Offset(size.width * 0.58f, size.height * 0.1f), sz = Size(size.width * 0.34f, size.height * 0.8f))
    for (k in 0 until 4) {
        drawRect(Color(0xFFC8B49A).copy(alpha = if (k == 1) 1f else 0.25f), Offset(size.width * 0.06f, size.height * (0.2f + k * 0.18f)), Size(size.width * (0.3f - k * 0.03f), size.height * 0.08f))
    }
    drawLine(Color(0xFF8C6A43), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), strokeWidth = 1f)
}

private fun DrawScope.liquid() {
    val t = MockCatalog.dune2
    buildColorField(size, t.palette).forEach { it() }
    drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(size.width * 0.3f, size.height * 0.08f), Size(size.width * 0.4f, size.height * 0.1f), CornerRadius(20f))
    val cw = size.width * 0.2f
    for (i in 0 until 4) {
        val focused = i == 1
        val y = size.height * (if (focused) 0.42f else 0.48f)
        val h = size.height * (if (focused) 0.44f else 0.36f)
        drawRoundRect(Color.White.copy(alpha = if (focused) 0.9f else 0.28f), Offset(size.width * 0.06f + i * (cw + 8f) + (if (i > 1) 6f else 0f), y), Size(cw, h), CornerRadius(12f))
    }
}

private fun DrawScope.zero() {
    scene("madmax", variant = 1)
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = size.height * 0.6f))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(size.width * 0.07f, size.height * 0.8f), Size(size.width * 0.26f, 3f))
    drawRect(Color.White.copy(alpha = 0.4f), Offset(size.width * 0.07f, size.height * 0.86f), Size(size.width * 0.16f, 2f))
}

private fun DrawScope.console() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF16181B), Color(0xFF0B0C0E))))
    drawRect(Color(0xFF1D2024), Offset.Zero, Size(size.width * 0.12f, size.height))
    drawRect(Color(0xFFE4572E), Offset(size.width * 0.12f - 2f, size.height * 0.28f), Size(2f, size.height * 0.12f))
    val g = 5f
    val x0 = size.width * 0.16f
    val w = size.width - x0 - g * 2
    drawRoundRect(Color(0xFF22262B), Offset(x0, g * 2), Size(w * 0.62f, size.height * 0.52f), CornerRadius(6f))
    scene("br2049", topLeft = Offset(x0 + 3f, g * 2 + 3f), sz = Size(w * 0.62f - 6f, size.height * 0.52f - 6f))
    drawRoundRect(Color(0xFF22262B), Offset(x0 + w * 0.62f + g, g * 2), Size(w * 0.38f - g, size.height * 0.25f), CornerRadius(6f))
    drawRoundRect(Color(0xFF22262B), Offset(x0 + w * 0.62f + g, g * 3 + size.height * 0.25f), Size(w * 0.38f - g, size.height * 0.27f - g), CornerRadius(6f))
    for (i in 0 until 4) drawRoundRect(Color(0xFF22262B), Offset(x0 + i * (w / 4f + 1f), size.height * 0.62f + g), Size(w / 4f - g, size.height * 0.3f), CornerRadius(6f))
    drawRect(Color(0xFFE4572E), Offset(x0, size.height * 0.62f + g + size.height * 0.3f - 2f), Size(w / 4f - g, 2f))
}

private fun DrawScope.editorial() {
    drawRect(Color(0xFF121110))
    scene("wadjda", topLeft = Offset(size.width * 0.42f, 0f), sz = Size(size.width * 0.58f, size.height * 0.72f))
    drawRect(Color(0xFFF1EBDF), Offset(size.width * 0.06f, size.height * 0.2f), Size(size.width * 0.44f, size.height * 0.12f))
    drawRect(Color(0xFFF1EBDF), Offset(size.width * 0.06f, size.height * 0.36f), Size(size.width * 0.3f, size.height * 0.12f))
    drawRect(Color(0xFFD9412B), Offset(size.width * 0.06f, size.height * 0.1f), Size(size.width * 0.1f, 2f))
    for (i in 0 until 3) drawRect(Color(0x55F1EBDF), Offset(size.width * 0.06f + i * size.width * 0.3f, size.height * 0.8f), Size(size.width * 0.26f, 2f))
}

private fun DrawScope.ambient() {
    drawRect(Color.Black)
    drawCircle(Brush.radialGradient(listOf(Color(0xAA7A4FB0), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.4f), radius = size.width * 0.5f), radius = size.width * 0.5f, center = Offset(size.width * 0.3f, size.height * 0.4f))
    drawCircle(Brush.radialGradient(listOf(Color(0x88E8A080), Color.Transparent), center = Offset(size.width * 0.75f, size.height * 0.7f), radius = size.width * 0.4f), radius = size.width * 0.4f, center = Offset(size.width * 0.75f, size.height * 0.7f))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(size.width * 0.08f, size.height * 0.2f), Size(size.width * 0.22f, size.height * 0.14f))
    val cw = size.width * 0.1f
    for (i in 0 until 6) {
        val big = i == 2
        val s = if (big) cw * 1.35f else cw
        drawRoundRect(Color.White.copy(alpha = if (big) 0.9f else 0.35f), Offset(size.width * 0.12f + i * (cw + 6f) - (if (big) cw * 0.17f else 0f), size.height * 0.72f - s), Size(s, s), CornerRadius(8f))
    }
}

private fun DrawScope.arabia() {
    drawRect(Color(0xFF0A0A0C))
    val c = 10f
    fun chamfer(x: Float, y: Float, w: Float, h: Float) = Path().apply {
        moveTo(x + c, y); lineTo(x + w - c, y); lineTo(x + w, y + c); lineTo(x + w, y + h - c)
        lineTo(x + w - c, y + h); lineTo(x + c, y + h); lineTo(x, y + h - c); lineTo(x, y + c); close()
    }
    val cw = size.width * 0.18f
    for (i in 0 until 5) {
        val x = size.width * 0.06f + i * (cw + 6f)
        val focus = i == 2
        drawPath(chamfer(x, size.height * 0.36f, cw, size.height * 0.5f), if (focus) Color(0xFFEDE8E0).copy(alpha = 0.9f) else Color(0x33EDE8E0))
        if (focus) drawPath(chamfer(x - 3f, size.height * 0.36f - 3f, cw + 6f, size.height * 0.5f + 6f), Color(0xFFB8745A), style = Stroke(1.5f))
    }
    drawRect(Color(0xFFEDE8E0), Offset(size.width * 0.55f, size.height * 0.12f), Size(size.width * 0.38f, size.height * 0.1f))
}

private fun DrawScope.signature() {
    drawRect(Color(0xFF050505))
    scene("shogun", topLeft = Offset(size.width * 0.3f, 0f), sz = Size(size.width * 0.7f, size.height * 0.62f))
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF050505), Color.Transparent), startX = size.width * 0.3f, endX = size.width * 0.6f), Offset(size.width * 0.3f, 0f), Size(size.width * 0.3f, size.height * 0.62f))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050505)), startY = size.height * 0.4f, endY = size.height * 0.62f), Offset(0f, size.height * 0.4f), Size(size.width, size.height * 0.22f))
    drawRect(Color(0xFFF4F1EA), Offset(size.width * 0.07f, size.height * 0.24f), Size(size.width * 0.26f, size.height * 0.08f))
    val cw = size.width * 0.2f
    for (i in 0 until 4) {
        drawRoundRect(Color.White.copy(alpha = if (i == 0) 0.9f else 0.25f), Offset(size.width * 0.07f + i * (cw + 6f), size.height * 0.66f), Size(cw, size.height * 0.24f), CornerRadius(6f))
    }
    drawRect(Color(0xFFD8B26E), Offset(size.width * 0.07f, size.height * 0.93f), Size(cw, 2.5f))
}
