package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.BlurredArt
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 04 — LIQUID CINEMA
 * The palette is not fixed: it is extracted from the focused title and flows into the room.
 * Soft radii (18–28dp), translucent white layers used only where hierarchy needs them,
 * colored glow instead of borders, neighbours that part around focus.
 */
internal object C04 {
    val Ink = Color(0xFFFFFFFF)
    val Ink2 = Color(0xCCFFFFFF)
    val Ink3 = Color(0x8CFFFFFF)
    val Glass = Color(0x1AFFFFFF)
    val GlassStrong = Color(0x2EFFFFFF)
    val GlassEdge = Color(0x29FFFFFF)
    val Dark = Color(0xFF0B0B10)

    val Margin = 44.dp
    val CardRadius = 18.dp
    val WindowRadius = 28.dp
    const val FOCUS_SCALE = 1.1f
    const val FOCUS_MS = 300
    const val FLOOD_MS = 900
}

/** The "room": blurred art of the focused title, re-tinting everything as focus moves. */
@Composable
internal fun C04Room(title: ProtoTitle, modifier: Modifier = Modifier, dim: Float = 0.35f, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(C04.Dark)) {
        Crossfade(title, animationSpec = tween(C04.FLOOD_MS, easing = ProtoEasing.Decelerate), label = "room") { t ->
            BlurredArt(t, Modifier.fillMaxSize(), radius = 70.dp, drift = true)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        content()
    }
}

/** Accent extracted from the focused artwork, animated as the room changes. */
@Composable
internal fun rememberAccent(title: ProtoTitle): Color {
    val c by animateColorAsState(title.palette.ui, tween(C04.FLOOD_MS), label = "accent")
    return c
}

@Immutable
internal class C04Type(
    val hero: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val meta: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

@Composable
internal fun c04Type(): C04Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val fam = if (ar) f.plexArabic else f.manrope
    val boldW = if (ar) FontWeight.SemiBold else FontWeight.ExtraBold
    return C04Type(
        hero = ts(fam, if (ar) 34.sp else 36.sp, boldW, C04.Ink, if (ar) 0.em else (-0.02).em, if (ar) 52.sp else 42.sp),
        title = ts(fam, 20.sp, boldW, C04.Ink, if (ar) 0.em else (-0.01).em, if (ar) 32.sp else 26.sp),
        section = ts(fam, 14.sp, FontWeight.SemiBold, C04.Ink, lineHeight = if (ar) 24.sp else 20.sp),
        body = ts(fam, 13.sp, FontWeight.Normal, C04.Ink2, lineHeight = if (ar) 23.sp else 20.sp),
        meta = ts(fam, 11.sp, FontWeight.SemiBold, C04.Ink3, lineHeight = if (ar) 18.sp else 15.sp),
        label = ts(fam, 12.sp, FontWeight.SemiBold, C04.Ink, lineHeight = if (ar) 20.sp else 16.sp),
        caption = ts(fam, 11.sp, FontWeight.Normal, C04.Ink2, lineHeight = if (ar) 18.sp else 15.sp),
    )
}

internal fun Modifier.glass(radius: Dp = 22.dp, strong: Boolean = false): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(if (strong) C04.GlassStrong else C04.Glass)
    .border(1.dp, C04.GlassEdge, RoundedCornerShape(radius))

/**
 * Coloured bloom behind a focused element, taken from its own artwork. Drawn as an elliptical
 * radial gradient that reaches full transparency at its edge, so it reads as light, not a panel.
 */
internal fun Modifier.bloom(color: Color, amount: Float, @Suppress("UNUSED_PARAMETER") radius: Dp = C04.CardRadius): Modifier = drawBehind {
    if (amount > 0.01f) {
        val w = size.width * (1f + 0.45f * amount)
        val h = size.height * (1f + 0.7f * amount)
        val c = Offset(size.width / 2, size.height * 0.58f)
        scale(scaleX = w / h, scaleY = 1f, pivot = c) {
            drawCircle(
                Brush.radialGradient(
                    0f to color.copy(alpha = 0.6f * amount),
                    0.55f to color.copy(alpha = 0.28f * amount),
                    1f to Color.Transparent,
                    center = c,
                    radius = h / 2,
                ),
                radius = h / 2,
                center = c,
            )
        }
    }
}

/**
 * Liquid displacement: cards next to the focused one drift a little away from it,
 * so the focused card appears to push the water aside.
 */
@Composable
internal fun displacement(index: Int, focusedIndex: Int?, rowFocused: Boolean): Float {
    val target = if (!rowFocused || focusedIndex == null || index == focusedIndex) 0f else {
        val d = index - focusedIndex
        val mag = when (kotlin.math.abs(d)) { 1 -> 14f; 2 -> 8f; else -> 4f }
        if (d > 0) mag else -mag
    }
    val v by animateFloatAsState(target, tween(C04.FOCUS_MS, easing = ProtoEasing.Decelerate), label = "disp")
    return v
}

/** Pill action. Primary is solid white; secondary is glass. */
@Composable
internal fun C04Pill(
    label: String,
    glyph: Glyph? = null,
    primary: Boolean = false,
    accent: Color = C04.Ink,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C04.FOCUS_MS, easing = ProtoEasing.Decelerate), label = "p")
    val bg = when {
        focused -> C04.Ink
        primary -> C04.Ink.copy(alpha = 0.88f)
        else -> C04.Glass
    }
    Row(
        modifier
            .graphicsLayer { scaleX = 1f + 0.06f * f; scaleY = 1f + 0.06f * f }
            .bloom(accent, f, 50.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (primary || focused) Color.Transparent else C04.GlassEdge, RoundedCornerShape(50))
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (focused || primary) C04.Dark else C04.Ink
        if (glyph != null) {
            ProtoIcon(glyph, size = 13.dp, color = ink, stroke = 1.8.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label, type.label.copy(color = ink), maxLines = 1)
    }
}

/**
 * Segmented control with a liquid indicator: the white pill slides and stretches to the newly
 * selected option instead of jumping. Uses absolute positions so it also works in RTL.
 */
@Composable
internal fun C04Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
    selectOnFocus: Boolean = true,
) {
    val type = c04Type()
    val density = LocalDensity.current
    val positions = remember { mutableStateMapOf<Int, Pair<Float, Int>>() }
    val target = positions[selected]
    val x by animateDpAsState(with(density) { (target?.first ?: 0f).toDp() }, tween(360, easing = ProtoEasing.Decelerate), label = "sx")
    val w by animateDpAsState(with(density) { (target?.second ?: 0).toDp() }, tween(420, easing = ProtoEasing.Decelerate), label = "sw")
    Box(modifier.glass(50.dp).padding(4.dp)) {
        if (target != null) {
            Box(Modifier.absoluteOffset(x = x).width(w).height(34.dp).clip(RoundedCornerShape(50)).background(C04.Ink))
        }
        Row {
            options.forEachIndexed { i, label ->
                var focused by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .height(34.dp)
                        .onGloballyPositioned { positions[i] = it.positionInParent().x to it.size.width }
                        .clip(RoundedCornerShape(50))
                        .background(if (focused && i != selected) C04.GlassStrong else Color.Transparent)
                        .protoFocusable(if (i == 0) firstRequester else null, onFocusChange = { focused = it; if (it && selectOnFocus) onSelect(i) }, onClick = { onSelect(i) })
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(label, type.label.copy(color = if (i == selected) C04.Dark else C04.Ink), maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun C04Chip(text: String) {
    val type = c04Type()
    Box(Modifier.glass(50.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Txt(text, type.meta.copy(color = C04.Ink), maxLines = 1)
    }
}
