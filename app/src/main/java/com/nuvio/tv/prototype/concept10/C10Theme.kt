package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
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
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoTime
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.ArtPalette
import com.nuvio.tv.prototype.shared.data.Episode
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 10 — NUVIO SIGNATURE
 * A proposal, not a mood board. OLED black because most premium TVs are OLED and black is the
 * cinema. One accent, a muted gold, used for exactly one job: the light bar that marks focus.
 * Colour otherwise comes from the artwork, as a restrained ambient tint.
 */
internal object C10 {
    val Black = Color(0xFF050505)
    val Surface = Color(0xFF0F0F10)
    val Surface2 = Color(0xFF19191B)
    val Ink = Color(0xFFF4F1EA)
    val Ink2 = Color(0xB3F4F1EA)
    val Ink3 = Color(0x6BF4F1EA)
    val Hair = Color(0x1FF4F1EA)
    val Gold = Color(0xFFD8B26E)
    val GoldDim = Color(0x59D8B26E)
    val Margin = 56.dp
    val Radius = 10.dp
    const val FOCUS_MS = 240
}

@Immutable
internal class C10Type(
    val hero: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
)

@Composable
internal fun c10Type(): C10Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    return if (ar) {
        C10Type(
            hero = ts(f.alexandria, 36.sp, FontWeight.Medium, C10.Ink, lineHeight = 50.sp),
            title = ts(f.alexandria, 20.sp, FontWeight.Medium, C10.Ink, lineHeight = 32.sp),
            headline = ts(f.alexandria, 26.sp, FontWeight.Light, C10.Ink, lineHeight = 42.sp),
            body = ts(f.alexandria, 13.sp, FontWeight.Light, C10.Ink2, lineHeight = 21.sp),
            label = ts(f.alexandria, 13.sp, FontWeight.Medium, C10.Ink, lineHeight = 21.sp),
            caption = ts(f.alexandria, 11.sp, FontWeight.Light, C10.Ink3, lineHeight = 18.sp),
            overline = ts(f.alexandria, 11.sp, FontWeight.Medium, C10.Gold, lineHeight = 18.sp),
        )
    } else {
        C10Type(
            hero = ts(f.geist, 42.sp, FontWeight.SemiBold, C10.Ink, (-0.025).em, 46.sp),
            title = ts(f.geist, 20.sp, FontWeight.Medium, C10.Ink, (-0.01).em, 25.sp),
            headline = ts(f.geist, 27.sp, FontWeight.Light, C10.Ink, (-0.015).em, 33.sp),
            body = ts(f.geist, 13.sp, FontWeight.Normal, C10.Ink2, lineHeight = 20.sp),
            label = ts(f.geist, 13.sp, FontWeight.Medium, C10.Ink, lineHeight = 17.sp),
            caption = ts(f.geist, 11.sp, FontWeight.Normal, C10.Ink3, 0.01.em, 15.sp),
            overline = ts(f.geist, 10.sp, FontWeight.SemiBold, C10.Gold, 0.14.em, 14.sp),
        )
    }
}

/** Overlines are tracked capitals in English and plain in Arabic. */
@Composable
internal fun over(en: String, ar: String): String = if (isArabic()) ar else en.uppercase()

/** Focus progress with the signature timing: quick in, slightly slower out. */
@Composable
internal fun focusAnim(focused: Boolean): Float {
    val v by animateFloatAsState(if (focused) 1f else 0f, tween(if (focused) C10.FOCUS_MS else 200, easing = ProtoEasing.Decelerate), label = "focus")
    return v
}

/**
 * The light bar: a short gold line under the focused element that widens as focus lands, with a
 * soft glow of its own. It is the only place the accent appears.
 */
internal fun Modifier.lightBar(f: () -> Float, gap: Dp = 9.dp): Modifier = drawBehind {
    val p = f()
    if (p > 0.01f) {
        val w = size.width * (0.28f + 0.5f * p)
        val x = (size.width - w) / 2
        val y = size.height + gap.toPx()
        val h = 3.dp.toPx()
        drawRoundRect(C10.Gold.copy(alpha = 0.10f * p), Offset(x - 6.dp.toPx(), y - 5.dp.toPx()), Size(w + 12.dp.toPx(), h + 10.dp.toPx()), CornerRadius(8.dp.toPx()))
        drawRoundRect(C10.Gold.copy(alpha = 0.22f * p), Offset(x - 2.dp.toPx(), y - 2.dp.toPx()), Size(w + 4.dp.toPx(), h + 4.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawRoundRect(C10.Gold.copy(alpha = p), Offset(x, y), Size(w, h), CornerRadius(h / 2))
    }
}

/** Vertical variant for list rows: the bar sits on the reading-start edge. */
internal fun Modifier.sideBar(f: () -> Float): Modifier = drawBehind {
    val p = f()
    if (p > 0.01f) {
        val h = size.height * (0.3f + 0.4f * p)
        val y = (size.height - h) / 2
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 3.dp.toPx() else 0f
        drawRoundRect(C10.Gold.copy(alpha = 0.18f * p), Offset(x - 3.dp.toPx(), y - 3.dp.toPx()), Size(9.dp.toPx(), h + 6.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawRoundRect(C10.Gold.copy(alpha = p), Offset(x, y), Size(3.dp.toPx(), h), CornerRadius(2.dp.toPx()))
    }
}

/** Lift plus a glow in the artwork's own colour. */
internal fun Modifier.lift(f: () -> Float, glow: Color): Modifier = this
    .drawBehind {
        val p = f()
        if (p > 0.01f) {
            val c = Offset(size.width / 2, size.height * 0.7f)
            val r = size.width * 0.62f
            scale(1.25f, 0.62f, pivot = c) {
                drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.38f * p), Color.Transparent), center = c, radius = r), r, c)
            }
        }
    }
    .graphicsLayer {
        val p = f()
        scaleX = 1f + 0.04f * p
        scaleY = 1f + 0.04f * p
        translationY = -3.dp.toPx() * p
    }

/**
 * Title reveal: the title is uncovered by a soft edge travelling in the reading direction while
 * it settles into place. Runs once per title, within the motion budget.
 */
@Composable
internal fun TitleReveal(text: String, style: TextStyle, key: Any, modifier: Modifier = Modifier, maxLines: Int = 2) {
    val frozen = LocalProtoEnv.current.frozen
    val a = remember(key) { Animatable(if (frozen) 1f else 0f) }
    LaunchedEffect(key) { if (!frozen) a.animateTo(1f, tween(350, 40, ProtoEasing.Decelerate)) }
    Txt(
        text, style,
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; translationY = 10.dp.toPx() * (1f - a.value) }
            .drawWithContent {
                drawContent()
                val p = a.value
                if (p < 0.999f) {
                    val edge = size.width * 0.3f
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    val lead = -edge + (size.width + edge) * p
                    val brush = if (!rtl) Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = lead, endX = lead + edge)
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = size.width - lead - edge, endX = size.width - lead)
                    drawRect(brush, blendMode = BlendMode.DstIn)
                }
            },
        maxLines = maxLines,
    )
}

/** Restrained ambient tint from the focused artwork, drawn into the black from the top-end. */
@Composable
internal fun C10Ambient(palette: ArtPalette, modifier: Modifier = Modifier, strength: Float = 1f) {
    Box(modifier.fillMaxSize().background(C10.Black)) {
        Crossfade(palette, animationSpec = tween(900, easing = ProtoEasing.Decelerate), label = "tint") { p ->
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    val c = Offset(if (rtl) 0f else size.width, 0f)
                    drawRect(Brush.radialGradient(listOf(p.ui.copy(alpha = 0.20f * strength), p.mid.copy(alpha = 0.06f * strength), Color.Transparent), center = c, radius = size.width * 0.75f))
                    drawRect(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.06f * strength), Color.Transparent), center = Offset(if (rtl) size.width else 0f, size.height), radius = size.width * 0.5f))
                },
            )
        }
    }
}

/** Artwork card with lift, colour glow and the light bar. Content overlays sit inside the frame. */
@Composable
internal fun C10Card(
    t: ProtoTitle,
    kind: ArtKind,
    width: Dp,
    aspect: Float,
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null,
    episode: Episode? = null,
    onFocus: (Boolean) -> Unit = {},
    overlay: @Composable BoxScope.(Float) -> Unit = {},
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    // The focus target sits outside the lift so the card's focus rectangle never moves: a lifted
    // card must not make its row neighbours look "below" it to D-pad focus search.
    Box(modifier.width(width).protoFocusable(requester, onFocusChange = { focused = it; onFocus(it) }, onClick = onClick).lift({ f }, t.palette.ui).lightBar({ f })) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(C10.Radius)).background(C10.Surface)) {
            ProtoArtwork(t, kind, Modifier.fillMaxSize().graphicsLayer { alpha = 0.86f + 0.14f * f }, episode = episode)
            overlay(f)
        }
    }
}

/** Primary is warm white; secondary is a quiet surface. Both carry the light bar. */
@Composable
internal fun C10Button(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    val fg = if (primary) C10.Black else C10.Ink
    Row(
        modifier
            .graphicsLayer { scaleX = 1f + 0.03f * f; scaleY = 1f + 0.03f * f }
            .lightBar({ f }, 7.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) C10.Ink.copy(alpha = 0.9f + 0.1f * f) else if (focused) C10.Surface2 else C10.Ink.copy(alpha = 0.08f))
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = if (label.isEmpty()) 12.dp else 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 15.dp, color = fg, stroke = 1.7.dp)
            if (label.isNotEmpty()) Spacer(Modifier.width(9.dp))
        }
        if (label.isNotEmpty()) Txt(label, type.label.copy(color = fg, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

/** Text tab: selected is ink, focused gets the bar. */
@Composable
internal fun C10Tab(label: String, selected: Boolean, modifier: Modifier = Modifier, requester: FocusRequester? = null, onFocus: () -> Unit = {}, onClick: () -> Unit = onFocus) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Box(
        modifier.lightBar({ f }, 5.dp)
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) { Txt(label, type.label.copy(color = if (focused || selected) C10.Ink else C10.Ink3), maxLines = 1) }
}

@Composable
internal fun C10Chip(text: String, color: Color = C10.Ink2) {
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(C10.Ink.copy(alpha = 0.07f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Txt(text, c10Type().caption.copy(color = color), maxLines = 1)
    }
}

internal enum class C10Section { HOME, SEARCH, LIBRARY, PROFILE }

private val navItems = listOf(
    C10Section.HOME to Bi("Home", "الرئيسية"),
    C10Section.SEARCH to Bi("Search", "بحث"),
    C10Section.LIBRARY to Bi("Library", "المكتبة"),
)

/**
 * Auto-hiding navigation. At rest it is only the wordmark, the section name and the clock; it
 * unfolds when focus travels up into it and folds away as soon as focus leaves.
 */
@Composable
internal fun C10TopNav(current: C10Section, clock: ProtoTime, modifier: Modifier = Modifier, scrolled: Float = 0f, onSelect: (C10Section) -> Unit) {
    val type = c10Type()
    val lang = LocalProtoLang.current
    val fonts = LocalProtoFonts.current
    var inside by remember { mutableStateOf(false) }
    val open by animateFloatAsState(if (inside) 1f else 0f, tween(280, easing = ProtoEasing.Decelerate), label = "nav")
    Row(
        modifier.fillMaxWidth().background(Brush.verticalGradient(0f to C10.Black.copy(alpha = maxOf(0.75f * (0.5f + 0.5f * open), 0.96f * scrolled)), 0.6f to C10.Black.copy(alpha = 0.85f * scrolled), 1f to Color.Transparent))
            .onFocusChanged { inside = it.hasFocus }
            .padding(start = C10.Margin, end = C10.Margin, top = 22.dp, bottom = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt("nuvio", ts(fonts.geist, 18.sp, FontWeight.SemiBold, C10.Ink, (-0.03).em, 22.sp))
        Box(Modifier.padding(start = 2.dp, top = 8.dp).size(4.dp).clip(CircleShape).background(C10.Gold))
        Spacer(Modifier.width(22.dp))
        Box {
            // Folded: just where you are.
            Txt(navItems.firstOrNull { it.first == current }?.second?.of(lang) ?: "", type.label.copy(color = C10.Ink3), Modifier.graphicsLayer { alpha = 1f - open })
            Row(Modifier.graphicsLayer { alpha = open }, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                navItems.forEach { (s, name) ->
                    C10Tab(name.of(lang), selected = s == current, onClick = { onSelect(s) })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Txt(clock.clock24(), type.label.copy(color = C10.Ink2))
        Spacer(Modifier.width(16.dp))
        C10Avatar(if (lang == ProtoLang.AR) "ف" else "F", 26.dp, current == C10Section.PROFILE) { onSelect(C10Section.PROFILE) }
    }
}

@Composable
internal fun C10Avatar(letter: String, size: Dp, selected: Boolean, modifier: Modifier = Modifier, color: Color = C10.Gold, requester: FocusRequester? = null, onClick: () -> Unit) {
    val fonts = LocalProtoFonts.current
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Box(
        modifier.lightBar({ f }, 6.dp).size(size).graphicsLayer { scaleX = 1f + 0.08f * f; scaleY = 1f + 0.08f * f }
            .clip(CircleShape).background(Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.45f))))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(letter, ts(if (letter.any { it.code > 0x600 }) fonts.alexandria else fonts.geist, (size.value * 0.42f).sp, FontWeight.SemiBold, C10.Black, lineHeight = (size.value * 0.6f).sp))
        if (selected) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(3.dp).clip(CircleShape).background(C10.Black))
    }
}

/** Next-episode intelligence: what the Continue card should say about where you are. */
internal data class NextUp(val text: Bi, val highlight: Boolean)

internal fun nextUp(t: ProtoTitle): NextUp? {
    val remaining = t.remainingMin
    val ep = t.resume
    if (!t.isSeries) return remaining?.let { NextUp(Bi("$it min left", "متبقٍ $it دقيقة"), false) }
    if (ep == null) return null
    val all = t.seasons.flatMap { it.episodes }
    val idx = all.indexOf(ep)
    val following = all.getOrNull(idx + 1)
    val fresh = all.drop(idx + 1).firstOrNull { it.isNew }
    return when {
        (ep.progress ?: 0f) >= 0.9f && following != null ->
            NextUp(Bi("Up next: S${following.season} · E${following.number}", "التالي: الموسم ${following.season} · الحلقة ${following.number}"), false)
        fresh != null && fresh == following ->
            NextUp(Bi("S${ep.season} · E${ep.number} · then E${fresh.number} is new", "الموسم ${ep.season} · الحلقة ${ep.number} · ثم حلقة ${fresh.number} جديدة"), true)
        else -> NextUp(Bi("S${ep.season} · E${ep.number} · ${remaining ?: ep.runtimeMin} min left", "الموسم ${ep.season} · الحلقة ${ep.number} · متبقٍ ${remaining ?: ep.runtimeMin} د"), false)
    }
}

/** The primary action label follows the same intelligence. */
@Composable
internal fun playLabel(t: ProtoTitle): String {
    val ep = t.resume
    val lang = LocalProtoLang.current
    return when {
        t.isSeries && ep != null -> if (lang == ProtoLang.AR) "استئناف الحلقة ${ep.number}" else "Resume S${ep.season} · E${ep.number}"
        t.progress != null -> if (lang == ProtoLang.AR) "استئناف" else "Resume"
        else -> if (lang == ProtoLang.AR) "تشغيل" else "Play"
    }
}

@Composable
internal fun C10Progress(p: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x40FFFFFF))) {
        Box(Modifier.fillMaxWidth(p).height(3.dp).background(C10.Ink))
    }
}
