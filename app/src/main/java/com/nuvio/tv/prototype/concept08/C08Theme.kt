package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ColorField
import com.nuvio.tv.prototype.shared.data.ArtPalette
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 08 — AMBIENT TV OS
 * OLED black is the default state. Light is the only decoration: soft fields of colour taken
 * from whatever has focus, drifting slowly and breathing. Everything is round and quiet.
 */
internal object C08 {
    val Black = Color(0xFF000000)
    val Ink = Color(0xFFF5F2EE)
    val Ink2 = Color(0x99F5F2EE)
    val Ink3 = Color(0x59F5F2EE)
    val Soft = Color(0x14FFFFFF)
    val SoftStrong = Color(0x26FFFFFF)
    val Margin = 56.dp
    const val LIGHT_MS = 1400
}

@Immutable
internal class C08Type(val clock: TextStyle, val hero: TextStyle, val title: TextStyle, val body: TextStyle, val label: TextStyle, val small: TextStyle)

@Composable
internal fun c08Type(): C08Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val fam = if (ar) f.tajawal else f.outfit
    return C08Type(
        clock = ts(f.outfit, 96.sp, FontWeight.ExtraLight, C08.Ink, (-0.03).em, 100.sp),
        hero = ts(fam, if (ar) 34.sp else 38.sp, FontWeight.Light, C08.Ink, if (ar) 0.em else (-0.01).em, if (ar) 48.sp else 44.sp),
        title = ts(fam, if (ar) 19.sp else 20.sp, FontWeight.Light, C08.Ink, lineHeight = if (ar) 30.sp else 26.sp),
        body = ts(fam, 14.sp, FontWeight.Light, C08.Ink2, lineHeight = if (ar) 24.sp else 21.sp),
        label = ts(fam, 13.sp, FontWeight.Medium, C08.Ink, lineHeight = if (ar) 20.sp else 17.sp),
        small = ts(fam, 11.sp, FontWeight.Normal, C08.Ink3, if (ar) 0.em else 0.04.em, if (ar) 18.sp else 15.sp),
    )
}

/** The room light: a drifting, breathing colour field that crossfades when focus changes. */
@Composable
internal fun AmbientLight(palette: ArtPalette, modifier: Modifier = Modifier, strength: Float = 0.75f) {
    val frozen = LocalProtoEnv.current.frozen
    val breath = if (frozen) 1f else rememberInfiniteTransition(label = "breath").animateFloat(0.82f, 1f, infiniteRepeatable(tween(7000, easing = ProtoEasing.Standard), RepeatMode.Reverse), label = "b").value
    Box(modifier.fillMaxSize().background(C08.Black)) {
        Crossfade(palette, animationSpec = tween(C08.LIGHT_MS, easing = ProtoEasing.Decelerate), label = "light") { p ->
            ColorField(p, Modifier.fillMaxSize().graphicsLayer { alpha = strength * breath }, drift = true, intensity = 0.9f)
        }
        // Keep the centre of the panel dark: light lives at the edges, like bias lighting.
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xCC000000), Color(0x33000000), Color.Transparent))))
    }
}

/** Soft halo under a focused element, in its own colour. */
internal fun Modifier.halo(color: Color, amount: Float): Modifier = drawBehind {
    if (amount > 0.01f) {
        val c = Offset(size.width / 2, size.height * 0.75f)
        val r = size.maxDimension * 0.9f
        scale(1.3f, 0.75f, pivot = c) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f * amount), Color.Transparent), center = c, radius = r), radius = r, center = c)
        }
    }
}

/** Fully rounded soft button: translucent at rest, lit white when focused. */
@Composable
internal fun C08Pill(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    glow: Color = C08.Ink,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c08Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(360, easing = ProtoEasing.Decelerate), label = "pill")
    Row(
        modifier
            .graphicsLayer { scaleX = 1f + 0.05f * f; scaleY = 1f + 0.05f * f }
            .halo(glow, f * 0.7f)
            .clip(RoundedCornerShape(50))
            .background(if (focused) C08.Ink.copy(alpha = 0.92f) else C08.Soft)
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 14.dp, color = if (focused) C08.Black else C08.Ink, stroke = 1.6.dp)
            if (label.isNotEmpty()) Spacer(Modifier.width(8.dp))
        }
        if (label.isNotEmpty()) Txt(label, type.label.copy(color = if (focused) C08.Black else C08.Ink), maxLines = 1)
    }
}

/**
 * Dock magnification: the focused tile grows the most and its neighbours a little, like a
 * dock under a cursor. Sizes animate in layout, so the whole dock breathes around focus.
 */
@Composable
internal fun dockSize(index: Int, focused: Int?, base: Dp, peak: Float = 1.34f): Dp {
    val factor = when (if (focused == null) Int.MAX_VALUE else kotlin.math.abs(index - focused)) {
        0 -> peak
        1 -> 1f + (peak - 1f) * 0.42f
        2 -> 1f + (peak - 1f) * 0.14f
        else -> 1f
    }
    val v by animateDpAsState(base * factor, tween(340, easing = ProtoEasing.Decelerate), label = "dock")
    return v
}

@Composable
internal fun C08Screen(palette: ArtPalette, strength: Float = 0.75f, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AmbientLight(palette, strength = strength)
        content()
    }
}

@Composable
internal fun C08Dot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(50)).background(color))
}

@Composable
internal fun C08Caption(title: String, detail: String, alpha: Float, modifier: Modifier = Modifier) {
    val type = c08Type()
    Column(modifier.graphicsLayer { this.alpha = alpha; transformOrigin = TransformOrigin.Center }, horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(title, type.label, maxLines = 1)
        Txt(detail, type.small, maxLines = 1)
        Spacer(Modifier.height(4.dp))
    }
}

/** A choice that shows its state with a small light, not a checkbox. */
@Composable
internal fun C08Choice(label: String, selected: Boolean, glow: Color, modifier: Modifier = Modifier, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c08Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(320, easing = ProtoEasing.Decelerate), label = "choice")
    Row(
        modifier.halo(glow, f * 0.5f).clip(RoundedCornerShape(50))
            .background(if (focused) C08.SoftStrong else Color.Transparent)
            .protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        C08Dot(if (selected) glow else C08.Ink3, 6.dp)
        Spacer(Modifier.width(8.dp))
        Txt(label, type.label.copy(color = if (selected || focused) C08.Ink else C08.Ink2), maxLines = 1)
    }
}
