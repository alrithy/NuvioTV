package com.nuvio.tv.prototype.concept02

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.Rng
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 02 — RIYADH AFTER DARK
 * Night navy, muted gold light, emerald only for "ready" states, warm-white ink.
 * Signature: every heading is a bilingual lockup; Najdi crenellations as a quiet structural motif.
 */
internal object C02 {
    val Night = Color(0xFF070A12)
    val Surface = Color(0xFF0D1220)
    val Raised = Color(0xFF141B2C)
    val Gold = Color(0xFFC9A45C)
    val GoldLight = Color(0xFFE8D2A0)
    val GoldDim = Color(0x80C9A45C)
    val Emerald = Color(0xFF1C7A5F)
    val EmeraldLight = Color(0xFF4DBB93)
    val Ink = Color(0xFFEDE6D6)
    val Ink2 = Color(0xFFA9A193)
    val Ink3 = Color(0xFF6E6A62)
    val Glass = Color(0x14FFFFFF)
    val Line = Color(0x26C9A45C)

    val Margin = 48.dp
    val Radius = 6.dp
    const val FOCUS_MS = 260
    const val FOCUS_SCALE = 1.06f
}

@Immutable
internal class C02Type(
    val hero: TextStyle,
    val heroSecond: TextStyle,
    val title: TextStyle,
    val eyebrow: TextStyle,
    val eyebrowSecond: TextStyle,
    val body: TextStyle,
    val meta: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val captionSecond: TextStyle,
    val button: TextStyle,
)

@Composable
internal fun c02Type(): C02Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val latin = f.sora
    val arabic = f.readex
    val primary = if (ar) arabic else latin
    val secondary = if (ar) latin else arabic
    return C02Type(
        hero = if (ar) ts(arabic, 38.sp, FontWeight.Normal, C02.Ink, lineHeight = 56.sp) else ts(latin, 40.sp, FontWeight.ExtraLight, C02.Ink, (-0.01).em, 48.sp),
        heroSecond = if (ar) ts(latin, 20.sp, FontWeight.ExtraLight, C02.GoldLight, 0.02.em, 28.sp) else ts(arabic, 22.sp, FontWeight.Light, C02.GoldLight, lineHeight = 34.sp),
        title = ts(primary, 17.sp, if (ar) FontWeight.Normal else FontWeight.Light, C02.Ink, lineHeight = if (ar) 28.sp else 22.sp),
        eyebrow = ts(primary, 10.sp, FontWeight.SemiBold, C02.Gold, if (ar) 0.em else 0.24.em, if (ar) 18.sp else 14.sp),
        eyebrowSecond = ts(secondary, 11.sp, FontWeight.Normal, C02.GoldDim, if (ar) 0.2.em else 0.em, 16.sp),
        body = ts(primary, 13.sp, FontWeight.Light, C02.Ink2, lineHeight = if (ar) 23.sp else 20.sp),
        meta = ts(primary, 11.sp, FontWeight.Normal, C02.Ink2, if (ar) 0.em else 0.04.em, if (ar) 18.sp else 15.sp),
        label = ts(primary, 12.sp, FontWeight.Normal, C02.Ink, lineHeight = if (ar) 20.sp else 16.sp),
        caption = ts(primary, 11.sp, FontWeight.Normal, C02.Ink, lineHeight = if (ar) 18.sp else 15.sp),
        captionSecond = ts(secondary, 10.sp, FontWeight.Light, C02.Ink3, lineHeight = 14.sp),
        button = ts(primary, 12.sp, FontWeight.SemiBold, C02.Night, if (ar) 0.em else 0.06.em, 16.sp),
    )
}

/** The other language of a bilingual pair. */
@Composable
internal fun Bi.other(): String = if (LocalProtoLang.current == ProtoLang.AR) en else ar

/**
 * Bilingual heading: the current language leads, the other follows after a gold hairline.
 * English is set in tracked caps; Arabic never takes tracking.
 */
@Composable
internal fun C02Heading(text: Bi, modifier: Modifier = Modifier, count: Int? = null) {
    val type = c02Type()
    val ar = isArabic()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Txt(if (ar) text.ar else text.en.uppercase(), type.eyebrow)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(28.dp).height(1.dp).background(C02.Line))
        Spacer(Modifier.width(12.dp))
        Txt(if (ar) text.en.uppercase() else text.ar, type.eyebrowSecond, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(12.dp))
            Txt(count.toString(), type.eyebrowSecond)
        }
    }
}

/** Najdi crenellation: a row of small stepped triangles, the silhouette of a Najdi parapet. */
@Composable
internal fun Crenellation(modifier: Modifier = Modifier, color: Color = C02.Line, tooth: Dp = 7.dp) {
    Canvas(modifier.height(tooth)) {
        val t = tooth.toPx()
        var x = 0f
        val path = Path()
        while (x < size.width) {
            path.moveTo(x, size.height)
            path.lineTo(x + t * 0.5f, 0f)
            path.lineTo(x + t, size.height)
            x += t * 1.6f
        }
        drawPath(path, color)
    }
}

/** Gold primary action. Focus deepens the gold and lifts the button. */
@Composable
internal fun C02GoldButton(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C02.FOCUS_MS), label = "gb")
    Row(
        modifier
            .graphicsLayer { scaleX = 1f + 0.04f * f; scaleY = 1f + 0.04f * f }
            .drawBehind {
                drawRoundRect(
                    Brush.radialGradient(listOf(C02.Gold.copy(alpha = 0.35f * f), Color.Transparent), center = Offset(size.width / 2, size.height), radius = size.width * 0.8f),
                    topLeft = Offset(-size.width * 0.2f, 0f), size = Size(size.width * 1.4f, size.height * 1.6f),
                )
            }
            .clip(RoundedCornerShape(C02.Radius))
            .background(Brush.horizontalGradient(listOf(if (focused) C02.GoldLight else C02.Gold, C02.Gold)))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 12.dp, color = C02.Night, stroke = 1.8.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label, type.button)
    }
}

/** Secondary action: hairline outline that fills with glass on focus. */
@Composable
internal fun C02GhostButton(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .clip(RoundedCornerShape(C02.Radius))
            .background(if (focused) C02.Ink else if (selected) C02.Glass else Color.Transparent)
            .border(1.dp, if (focused) Color.Transparent else if (selected) C02.Gold else C02.Line, RoundedCornerShape(C02.Radius))
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 12.dp, color = if (focused) C02.Night else C02.Ink, stroke = 1.6.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label, type.button.copy(color = if (focused) C02.Night else if (selected) C02.GoldLight else C02.Ink, fontWeight = FontWeight.Normal))
    }
}

/** Gold outline technical badge. */
@Composable
internal fun C02Badge(text: String, emerald: Boolean = false) {
    val f = LocalProtoFonts.current
    val c = if (emerald) C02.EmeraldLight else C02.Gold
    Box(
        Modifier
            .border(1.dp, c.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Txt(text, ts(f.sora, 9.sp, FontWeight.SemiBold, c, 0.12.em, 12.sp), maxLines = 1)
    }
}

@Composable
internal fun C02Pill(text: String, dot: Color? = null) {
    val type = c02Type()
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(C02.Glass).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        Txt(text, type.meta.copy(color = C02.Ink), maxLines = 1)
    }
}

/**
 * Riyadh at night: navy sky warming toward an amber horizon, a skyline with lit windows,
 * a tower with an arched sky-bridge void and a slim faceted spire. Drawn once and cached.
 */
@Composable
internal fun RiyadhSkyline(modifier: Modifier = Modifier, intensity: Float = 1f) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val rng = Rng(2035)
            val horizon = h * 0.8f
            val sky = Brush.verticalGradient(0f to C02.Night, 0.55f to Color(0xFF0E1426), 0.8f to Color(0xFF2A2230), 1f to Color(0xFF3A2A22))
            data class B(val x: Float, val w: Float, val h: Float)
            val back = ArrayList<B>()
            var x = -10f
            while (x < w) {
                val bw = w * rng.range(0.02f, 0.05f)
                back += B(x, bw, h * rng.range(0.06f, 0.16f))
                x += bw + 2f
            }
            val front = ArrayList<B>()
            x = -10f
            while (x < w) {
                val bw = w * rng.range(0.025f, 0.06f)
                front += B(x, bw, h * rng.range(0.05f, 0.12f))
                x += bw + w * rng.range(0.004f, 0.02f)
            }
            val windows = ArrayList<Offset>()
            front.forEach { b ->
                var yy = horizon - b.h + 6f
                while (yy < h) {
                    var xx = b.x + 4f
                    while (xx < b.x + b.w - 4f) {
                        if (rng.next() > 0.9f) windows += Offset(xx, yy)
                        xx += 7f
                    }
                    yy += 9f
                }
            }
            // Landmark 1: a tall tower with a parabolic void near the top.
            val lx = w * 0.72f
            val lw = w * 0.05f
            val lh = h * 0.42f
            val landmark = Path().apply {
                moveTo(lx - lw / 2, horizon)
                lineTo(lx - lw * 0.36f, horizon - lh)
                lineTo(lx + lw * 0.36f, horizon - lh)
                lineTo(lx + lw / 2, horizon)
                close()
            }
            val void = Path().apply {
                moveTo(lx - lw * 0.3f, horizon - lh)
                quadraticBezierTo(lx, horizon - lh * 0.55f, lx + lw * 0.3f, horizon - lh)
                close()
            }
            // Landmark 2: a slim faceted spire.
            val sx = w * 0.58f
            val spire = Path().apply {
                moveTo(sx - w * 0.018f, horizon)
                lineTo(sx - w * 0.01f, horizon - h * 0.34f)
                lineTo(sx, horizon - h * 0.4f)
                lineTo(sx + w * 0.012f, horizon - h * 0.32f)
                lineTo(sx + w * 0.02f, horizon)
                close()
            }
            val silhouette = Color(0xFF05070D)
            onDrawBehind {
                drawRect(sky)
                drawCircle(Brush.radialGradient(listOf(C02.Gold.copy(alpha = 0.22f * intensity), Color.Transparent), center = Offset(w * 0.65f, horizon), radius = w * 0.5f), radius = w * 0.5f, center = Offset(w * 0.65f, horizon))
                back.forEach { b -> drawRect(Color(0xFF0B0F1A), Offset(b.x, horizon - b.h), Size(b.w, b.h + h)) }
                drawPath(landmark, silhouette)
                drawPath(void, Color(0xFF2A2230))
                drawPath(spire, silhouette)
                drawLine(C02.Gold.copy(alpha = 0.7f * intensity), Offset(sx, horizon - h * 0.4f), Offset(sx, horizon - h * 0.43f), strokeWidth = 1.5f)
                front.forEach { b -> drawRect(silhouette, Offset(b.x, horizon - b.h), Size(b.w, b.h + h)) }
                windows.forEachIndexed { i, o -> drawRect(C02.GoldLight.copy(alpha = (0.25f + (i % 4) * 0.12f) * intensity), o, Size(2.5f, 3.5f)) }
                // Landmark window rhythm
                for (k in 0 until 40) {
                    val yy = horizon - k * (lh / 44f)
                    if (k % 3 != 0) drawRect(C02.GoldLight.copy(alpha = 0.25f * intensity), Offset(lx - lw * 0.2f, yy), Size(lw * 0.4f, 1.2f))
                }
            }
        },
    )
}

/** Bilingual caption under cards: title in the current language, the other beneath. */
@Composable
internal fun C02Caption(title: Bi, detail: String? = null, focused: Boolean) {
    val type = c02Type()
    Column {
        Txt(title.get(), type.caption.copy(color = if (focused) C02.Ink else C02.Ink2), maxLines = 1)
        Txt(title.other(), type.captionSecond.copy(color = if (focused) C02.GoldDim else C02.Ink3), maxLines = 1)
        if (detail != null) Txt(detail, type.captionSecond.copy(color = C02.Ink3), maxLines = 1)
    }
}

internal fun Modifier.c02Focus(focus: Float, radius: Dp = C02.Radius): Modifier = this
    .graphicsLayer {
        scaleX = 1f + (C02.FOCUS_SCALE - 1f) * focus
        scaleY = 1f + (C02.FOCUS_SCALE - 1f) * focus
    }
    .drawBehind {
        if (focus > 0.01f) {
            // Warm bloom, like a lit window reflecting on glass.
            drawRoundRect(
                Brush.radialGradient(listOf(C02.Gold.copy(alpha = 0.32f * focus), Color.Transparent), center = Offset(size.width / 2, size.height * 0.9f), radius = size.width * 0.75f),
                topLeft = Offset(-size.width * 0.25f, size.height * 0.2f), size = Size(size.width * 1.5f, size.height * 1.1f),
            )
        }
    }

@Composable
internal fun c02FocusAnim(focused: Boolean): Float {
    val v by animateFloatAsState(if (focused) 1f else 0f, tween(C02.FOCUS_MS, easing = ProtoEasing.Decelerate), label = "f02")
    return v
}

internal fun Modifier.goldBorder(focus: Float, radius: Dp = C02.Radius): Modifier =
    border(1.5.dp, C02.GoldLight.copy(alpha = focus), RoundedCornerShape(radius))

@Composable
internal fun C02Screen(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(C02.Night)) { content() }
}

internal val C02Nav = listOf(
    Bi("Home", "الرئيسية"), Bi("Movies", "أفلام"), Bi("Series", "مسلسلات"), Bi("Arabic", "عربي"), Bi("Search", "بحث"), Bi("Library", "مكتبتي"),
)

@Composable
internal fun C02Spacer(h: Dp) = Spacer(Modifier.height(h))

@Composable
internal fun C02Divider(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Crenellation(Modifier.fillMaxWidth(), C02.Line, 5.dp)
        Box(Modifier.fillMaxWidth().height(1.dp).background(C02.Line))
    }
}

internal val C02CardShape = RoundedCornerShape(C02.Radius)

@Composable
internal fun C02Row(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}
