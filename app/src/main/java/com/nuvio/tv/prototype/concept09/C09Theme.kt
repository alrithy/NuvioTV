package com.nuvio.tv.prototype.concept09

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import com.nuvio.tv.prototype.shared.ProtoTime
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.Episode
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.toArabicDigits
import com.nuvio.tv.prototype.shared.ts
import kotlin.math.abs
import kotlin.math.cos

/**
 * CONCEPT 09 — FUTURE ARABIA
 * Arabic is the primary script, not a translation. Obsidian surfaces, pearl type, copper light.
 * Every surface is chamfered on its reading-start corner and the opposite one; focus is a line of
 * light that traces the chamfered edge. Teal is used only for "ready" signals.
 */
internal object C09 {
    val Obsidian = Color(0xFF0A0A0C)
    val Obsidian2 = Color(0xFF131317)
    val Obsidian3 = Color(0xFF1C1C22)
    val Pearl = Color(0xFFEDE8E0)
    val Pearl2 = Color(0xB3EDE8E0)
    val Pearl3 = Color(0x6BEDE8E0)
    val Line = Color(0x26EDE8E0)
    val Copper = Color(0xFFB8745A)
    val CopperHi = Color(0xFFE3A283)
    val Teal = Color(0xFF55B7AC)
    val Margin = 44.dp
    val Rail = 108.dp
    val Cut = 12.dp
    const val TRACE_MS = 420
}

@Immutable
internal class C09Type(
    val mega: TextStyle,
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val tag: TextStyle,
    val ghost: TextStyle,
    val numeral: TextStyle,
)

@Composable
internal fun c09Type(): C09Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    return C09Type(
        // The mega word is always Arabic, in both languages. Copper light pools at its base.
        mega = ts(f.reemKufi, 120.sp, FontWeight.SemiBold, C09.Pearl, lineHeight = 136.sp)
            .copy(brush = Brush.verticalGradient(0.35f to C09.Pearl, 1f to C09.CopperHi)),
        display = ts(f.reemKufi, if (ar) 34.sp else 34.sp, FontWeight.SemiBold, C09.Pearl, lineHeight = if (ar) 50.sp else 40.sp),
        title = ts(f.reemKufi, if (ar) 21.sp else 20.sp, FontWeight.SemiBold, C09.Pearl, lineHeight = if (ar) 32.sp else 26.sp),
        body = ts(f.readex, 14.sp, FontWeight.Light, C09.Pearl2, lineHeight = if (ar) 25.sp else 21.sp),
        label = ts(f.readex, 13.sp, FontWeight.Normal, C09.Pearl, lineHeight = if (ar) 21.sp else 17.sp),
        tag = if (ar) ts(f.readex, 11.sp, FontWeight.SemiBold, C09.Pearl3, lineHeight = 17.sp)
        else ts(f.sora, 9.sp, FontWeight.SemiBold, C09.Pearl3, 0.22.em, 13.sp),
        ghost = ts(f.reemKufi, 15.sp, FontWeight.Normal, C09.Pearl3, lineHeight = 22.sp),
        numeral = if (ar) ts(f.readex, 40.sp, FontWeight.Light, C09.Pearl, lineHeight = 52.sp) else ts(f.sora, 40.sp, FontWeight.ExtraLight, C09.Pearl, lineHeight = 46.sp),
    )
}

/** Numbers are display type here: Arabic-Indic digits in Arabic, always. */
@Composable
internal fun num(s: Any): String = if (LocalProtoLang.current == ProtoLang.AR) s.toString().toArabicDigits() else s.toString()

/** Latin tags are tracked capitals; Arabic tags are never tracked or upper-cased. */
@Composable
internal fun tag(en: String, ar: String): String = if (isArabic()) ar else en.uppercase()

/** Cut on the reading-start top corner and the opposite bottom corner. */
internal fun chamfer(cut: Dp = C09.Cut) = CutCornerShape(topStart = cut, topEnd = 0.dp, bottomEnd = cut, bottomStart = 0.dp)

/** All four corners cut: used for profiles and the brand mark. */
internal fun octagon(cut: Dp) = CutCornerShape(cut)

private fun chamferPath(w: Float, h: Float, c: Float, rtl: Boolean): Path = Path().apply {
    // Starts at the reading-start cut and runs in reading direction, so the light follows the eye.
    if (!rtl) {
        moveTo(c, 0f); lineTo(w, 0f); lineTo(w, h - c); lineTo(w - c, h); lineTo(0f, h); lineTo(0f, c); close()
    } else {
        moveTo(w - c, 0f); lineTo(0f, 0f); lineTo(0f, h - c); lineTo(c, h); lineTo(w, h); lineTo(w, c); close()
    }
}

/**
 * Light-tracing focus. [progress] runs 0→1 as a copper line travels around the chamfered edge,
 * with a soft bloom behind it and a bright head. Read in the draw phase only.
 */
internal fun Modifier.c09Trace(progress: () -> Float, cut: Dp = C09.Cut, base: Color = C09.Line, bloom: Boolean = true): Modifier = drawWithCache {
    val c = cut.toPx()
    val inset = 0.75.dp.toPx()
    val path = chamferPath(size.width, size.height, c, layoutDirection == LayoutDirection.Rtl)
    val inner = chamferPath(size.width - inset * 2, size.height - inset * 2, (c - inset).coerceAtLeast(0f), layoutDirection == LayoutDirection.Rtl)
    inner.translate(androidx.compose.ui.geometry.Offset(inset, inset))
    val measure = PathMeasure().apply { setPath(inner, false) }
    val length = measure.length
    val seg = Path()
    val thin = Stroke(1.dp.toPx())
    val line = Stroke(1.6.dp.toPx(), join = StrokeJoin.Miter)
    val glow = Stroke(7.dp.toPx(), join = StrokeJoin.Round)
    onDrawWithContent {
        drawContent()
        drawPath(path, base, style = thin)
        val p = progress()
        if (p > 0.001f) {
            seg.reset()
            if (p >= 0.999f) seg.addPath(inner) else measure.getSegment(0f, length * p, seg, true)
            if (bloom) drawPath(seg, C09.Copper.copy(alpha = 0.28f), style = glow)
            drawPath(seg, C09.CopperHi, style = line)
            if (p < 0.999f) {
                val head = measure.getPosition(length * p)
                drawCircle(Brush.radialGradient(listOf(Color.White, C09.CopperHi.copy(alpha = 0f)), center = head, radius = 7.dp.toPx()), 7.dp.toPx(), head)
            }
        }
    }
}

/** Focus trace animation: runs the light around on focus, drops it instantly on blur. */
@Composable
internal fun rememberTrace(focused: Boolean): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(focused) {
        if (focused) a.animateTo(1f, tween(C09.TRACE_MS, easing = ProtoEasing.Decelerate)) else a.snapTo(0f)
    }
    return a
}

/**
 * Curved spatial gallery: items bend away from the focused one as if standing on the inside of a
 * wide cylinder, shrinking and dimming with distance. [offset] is index distance from focus.
 */
internal fun Modifier.curve(offset: Float, rtl: Boolean, angle: Float = 13f, pivotY: Float = 0.5f): Modifier = graphicsLayer {
    val d = offset.coerceIn(-5f, 5f)
    val a = abs(d)
    val dir = if (rtl) -1f else 1f
    rotationY = d * angle * dir
    cameraDistance = 9f * density
    val s = 1f - 0.06f * a.coerceAtMost(4f)
    scaleX = s
    scaleY = s
    alpha = 1f - 0.13f * a
    transformOrigin = TransformOrigin(if (d * dir > 0) 0f else 1f, pivotY)
    // Each bent card is visually narrower than its slot; pull the outer cards in so the gaps stay even.
    var pull = 0f
    for (j in 1..4) pull += (1f - (1f - 0.06f * j) * cos(Math.toRadians((angle * j).toDouble())).toFloat()) * (a - j).coerceIn(0f, 1f)
    translationX = -kotlin.math.sign(d) * dir * pull * size.width
}

/** Mirror of the artwork on the obsidian floor, fading into black. */
@Composable
internal fun C09Reflection(t: ProtoTitle, kind: ArtKind, width: Dp, height: Dp, fraction: Float = 0.3f, episode: Episode? = null, cut: Dp = C09.Cut) {
    Box(Modifier.width(width).height(height * fraction).clipToBounds()) {
        Box(
            Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).height(height)
                .graphicsLayer { scaleY = -1f; alpha = 0.32f }.clip(chamfer(cut)),
        ) { ProtoArtwork(t, kind, Modifier.fillMaxSize(), episode = episode) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(C09.Obsidian.copy(alpha = 0.45f), C09.Obsidian))))
    }
}

/** Chamfered action with the tracing light. Primary is filled pearl. */
@Composable
internal fun C09Button(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    val fg = if (primary) C09.Obsidian else C09.Pearl
    Row(
        modifier
            .clip(chamfer(10.dp))
            .background(if (primary) C09.Pearl else if (focused) C09.Obsidian3 else C09.Obsidian2)
            .c09Trace({ trace.value }, 10.dp, base = if (primary) Color.Transparent else C09.Line, bloom = false)
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 14.dp, color = fg, stroke = 1.7.dp)
            if (label.isNotEmpty()) Spacer(Modifier.width(9.dp))
        }
        if (label.isNotEmpty()) Txt(label, type.label.copy(color = fg, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

/** A dual-script label: the current language first, the other script ghosted beside it. */
@Composable
internal fun C09Dual(text: Bi, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val type = c09Type()
    val ar = isArabic()
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Txt(text.get(), style, color = color, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Txt(if (ar) text.en.uppercase() else text.ar, if (ar) type.tag.copy(fontFamily = LocalProtoFonts.current.sora, letterSpacing = 0.2.em, fontSize = 9.sp) else type.ghost, modifier = Modifier.padding(bottom = 3.dp), maxLines = 1)
    }
}

internal enum class C09Section { TONIGHT, SEARCH, LIBRARY, PROFILE }

private val sections = listOf(
    C09Section.TONIGHT to Bi("Tonight", "الليلة"),
    C09Section.SEARCH to Bi("Search", "ابحث"),
    C09Section.LIBRARY to Bi("Library", "مكتبتي"),
    C09Section.PROFILE to Bi("Account", "حسابي"),
)

/**
 * Start-side rail of Arabic words. Arabic stays primary even in English; the English word sits
 * beneath as a small tracked tag so the rail is readable either way.
 */
@Composable
internal fun C09NavRail(current: C09Section, clock: ProtoTime, modifier: Modifier = Modifier, onSelect: (C09Section) -> Unit) {
    val fonts = LocalProtoFonts.current
    val lang = LocalProtoLang.current
    val ar = lang == ProtoLang.AR
    Column(
        modifier.fillMaxHeight().width(C09.Rail).background(C09.Obsidian)
            .drawWithCache {
                val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width
                onDrawBehind { drawLine(C09.Line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.dp.toPx()) }
            }
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(38.dp).clip(octagon(10.dp)).background(C09.Copper), contentAlignment = Alignment.Center) {
            Txt("ن", ts(fonts.reemKufi, 20.sp, FontWeight.SemiBold, C09.Obsidian, lineHeight = 24.sp))
        }
        Spacer(Modifier.height(34.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            sections.forEach { (s, name) ->
                var focused by remember { mutableStateOf(false) }
                val trace = rememberTrace(focused)
                val on = s == current
                Column(
                    Modifier.width(84.dp).clip(chamfer(8.dp))
                        .background(if (focused) C09.Obsidian3 else Color.Transparent)
                        .c09Trace({ trace.value }, 8.dp, base = Color.Transparent, bloom = false)
                        .protoFocusable(onFocusChange = { focused = it }, onClick = { onSelect(s) })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Txt(name.ar, ts(fonts.reemKufi, 17.sp, FontWeight.SemiBold, if (focused || on) C09.Pearl else C09.Pearl3, lineHeight = 24.sp), maxLines = 1)
                    if (!ar) Txt(name.en.uppercase(), ts(fonts.sora, 7.5.sp, FontWeight.SemiBold, if (focused || on) C09.Pearl2 else C09.Pearl3, 0.2.em, 11.sp), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(if (on) 14.dp else 0.dp).height(2.dp).background(C09.Copper))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Txt(num(clock.clock24()), ts(if (ar) fonts.readex else fonts.sora, 18.sp, FontWeight.Light, C09.Pearl, lineHeight = 24.sp))
        Txt(clock.weekdayName(lang), ts(fonts.readex, 10.sp, FontWeight.Normal, C09.Pearl3, lineHeight = 16.sp))
        Spacer(Modifier.height(4.dp))
        Txt(clock.hijri(lang), ts(fonts.readex, 10.sp, FontWeight.Normal, C09.Copper, lineHeight = 16.sp), maxLines = 1)
    }
}

/**
 * Hijri date from the tabular Islamic calendar. It can differ by a day from Umm al-Qura; the
 * production build would use android.icu.util.IslamicCalendar with the UMALQURA calculation.
 */
internal fun ProtoTime.hijri(lang: ProtoLang): String {
    val m = month + 1
    val a = (14 - m) / 12
    val yy = year + 4800 - a
    val mm = m + 12 * a - 3
    val jdn = day + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    var l = jdn - 1948440 + 10632
    val n = (l - 1) / 10631
    l = l - 10631 * n + 354
    val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
    l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
    val hm = (24 * l) / 709
    val hd = l - (709 * hm) / 24
    val hy = 30 * n + j - 30
    val ar = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val en = listOf("Muharram", "Safar", "Rabiʿ I", "Rabiʿ II", "Jumada I", "Jumada II", "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhu al-Qaʿdah", "Dhu al-Hijjah")
    return if (lang == ProtoLang.AR) "$hd ${ar[hm - 1]}".toArabicDigits() else "$hd ${en[hm - 1]} $hy"
}

/** Obsidian stage with a faint copper horizon: the line everything stands on. */
@Composable
internal fun C09Stage(horizon: Float? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(C09.Obsidian).drawWithCache {
            val y = horizon?.let { it * size.height }
            onDrawBehind {
                if (y != null) {
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, C09.Copper.copy(alpha = 0.06f), Color.Transparent), startY = y - 80.dp.toPx(), endY = y + 80.dp.toPx()))
                    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, C09.Copper.copy(alpha = 0.55f), Color.Transparent)), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
                }
            }
        },
        content = content,
    )
}

/** Meta line with Arabic-Indic numerals in Arabic; certificates stay as printed. */
@Composable
internal fun c09Meta(t: ProtoTitle, withGenre: Boolean = true): String {
    val lang = LocalProtoLang.current
    val ar = lang == ProtoLang.AR
    val parts = buildList {
        add(num(t.year))
        add(
            if (t.isSeries) (if (ar) "${t.seasons.size} ${if (t.seasons.size == 1) "موسم" else "مواسم"}".toArabicDigits() else "${t.seasons.size} Season${if (t.seasons.size == 1) "" else "s"}")
            else formatRuntime(t.runtimeMin, lang).let { if (ar) it.toArabicDigits() else it },
        )
        add(t.cert)
        if (withGenre) add(t.primaryGenre.of(lang))
    }
    return parts.joinToString("  ·  ")
}
