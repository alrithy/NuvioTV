package com.nuvio.tv.prototype.concept01

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.saturation
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 01 — CINEMATIC BLACK
 * Tokens: pure OLED black, one warm "projector white" ink at four opacities, zero radius,
 * cinemascope (2.39:1) geometry, focus expressed by light and a hairline, dissolves not slides.
 */
internal object C01 {
    val Black = Color(0xFF000000)
    val Ink = Color(0xFFF2EEE6)
    val Ink70 = Ink.copy(alpha = 0.72f)
    val Ink45 = Ink.copy(alpha = 0.46f)
    val Ink25 = Ink.copy(alpha = 0.24f)
    val Hair = Color(0x24F2EEE6)

    /** Letterbox bar height for a 2.39:1 picture on a 16:9 (960×540dp) screen. */
    val Bar: Dp = 69.dp
    val Margin: Dp = 64.dp
    const val SCOPE = 2.39f

    const val DISSOLVE = 700
    const val FOCUS = 280
    const val TITLE_SEQUENCE = 900
}

@Immutable
internal class C01Type(
    val display: TextStyle,
    val title: TextStyle,
    val overline: TextStyle,
    val meta: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val small: TextStyle,
    val numeral: TextStyle,
    val arabic: Boolean,
)

@Composable
internal fun c01Type(): C01Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    return if (!ar) {
        C01Type(
            display = ts(f.jost, 46.sp, FontWeight.Light, C01.Ink, 0.18.em, 52.sp),
            title = ts(f.jost, 26.sp, FontWeight.Light, C01.Ink, 0.16.em, 32.sp),
            overline = ts(f.inter, 10.sp, FontWeight.Medium, C01.Ink70, 0.32.em),
            meta = ts(f.inter, 11.sp, FontWeight.Normal, C01.Ink70, 0.14.em),
            body = ts(f.inter, 13.sp, FontWeight.Normal, C01.Ink70, 0.01.em, 20.sp),
            label = ts(f.jost, 12.sp, FontWeight.Normal, C01.Ink, 0.28.em),
            small = ts(f.inter, 9.sp, FontWeight.Medium, C01.Ink45, 0.26.em),
            numeral = ts(f.jost, 34.sp, FontWeight.Light, C01.Ink, 0.02.em, 36.sp),
            arabic = false,
        )
    } else {
        C01Type(
            display = ts(f.alexandria, 42.sp, FontWeight.ExtraLight, C01.Ink, 0.em, 64.sp),
            title = ts(f.alexandria, 26.sp, FontWeight.Light, C01.Ink, 0.em, 40.sp),
            overline = ts(f.alexandria, 11.sp, FontWeight.Medium, C01.Ink70, 0.em, 18.sp),
            meta = ts(f.alexandria, 11.sp, FontWeight.Light, C01.Ink70, 0.em, 18.sp),
            body = ts(f.alexandria, 13.sp, FontWeight.Light, C01.Ink70, 0.em, 23.sp),
            label = ts(f.alexandria, 13.sp, FontWeight.Light, C01.Ink, 0.em, 20.sp),
            small = ts(f.alexandria, 10.sp, FontWeight.Medium, C01.Ink45, 0.em, 16.sp),
            numeral = ts(f.alexandria, 32.sp, FontWeight.ExtraLight, C01.Ink, 0.em, 40.sp),
            arabic = true,
        )
    }
}

/** Uppercases Latin labels only; Arabic has no case. */
@Composable
internal fun String.cap(): String = if (isArabic()) this else uppercase()

/**
 * Title-sequence text: letters arrive with tracking tightening and light fading up, the way
 * opening credits resolve. Arabic joins letters, so it resolves by light and a slight rise instead.
 */
@Composable
internal fun TitleSequenceText(
    text: String,
    style: TextStyle,
    key: Any,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    maxLines: Int = 2,
    arabic: Boolean = isArabic(),
) {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(C01.TITLE_SEQUENCE, delayMillis = delayMs, easing = ProtoEasing.Cinematic))
    }
    val p = progress.value
    val tracked = if (arabic) style else style.copy(letterSpacing = (style.letterSpacing.value + 0.35f * (1f - p)).em)
    Txt(
        text,
        tracked,
        modifier.graphicsLayer {
            alpha = p
            translationY = if (arabic) (1f - p) * 10.dp.toPx() else 0f
        },
        maxLines = maxLines,
    )
}

/** A cinemascope frame. Focus is shown by light (brightness + saturation) and a hairline. */
@Composable
internal fun C01Frame(
    title: ProtoTitle,
    modifier: Modifier = Modifier,
    width: Dp,
    requester: FocusRequester? = null,
    progress: Float? = null,
    onFocus: () -> Unit = {},
    caption: String? = null,
    captionStyle: TextStyle? = null,
    variant: Int = 0,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val light by animateFloatAsState(if (focused) 1f else 0f, tween(C01.FOCUS, easing = ProtoEasing.Standard), label = "light")
    Column(modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(C01.SCOPE)
                .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick),
        ) {
            ProtoArtwork(
                title, ArtKind.BACKDROP,
                Modifier
                    .fillMaxSize()
                    .saturation(0.35f + 0.65f * light)
                    .graphicsLayer { alpha = 0.38f + 0.62f * light },
                variant = variant,
            )
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(C01.Black.copy(alpha = 0.6f)))
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(2.dp).background(C01.Ink.copy(alpha = 0.5f + 0.5f * light)))
            }
        }
        Spacer(Modifier.height(5.dp))
        // Hairline cursor grows from the centre.
        Box(Modifier.fillMaxWidth().height(1.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth(light).height(1.dp).background(C01.Ink))
        }
        if (caption != null && captionStyle != null) {
            Spacer(Modifier.height(6.dp))
            Txt(caption, captionStyle, Modifier.graphicsLayer { alpha = 0.5f + 0.5f * light }, maxLines = 1)
        }
    }
}

/** Text action: no pill, no box. Focus lights the label and draws an underline from the start edge. */
@Composable
internal fun C01Action(
    label: String,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    style: TextStyle,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val light by animateFloatAsState(if (focused) 1f else 0f, tween(C01.FOCUS), label = "a")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .drawBehind {
                val w = size.width * light
                val y = size.height - 1.dp.toPx() / 2
                val x0 = if (rtl) size.width - w else 0f
                drawLine(C01.Ink, Offset(x0, y), Offset(x0 + w, y), strokeWidth = 1.dp.toPx())
            }
            .padding(bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            ProtoIcon(glyph, size = 12.dp, color = C01.Ink.copy(alpha = 0.55f + 0.45f * light), stroke = 1.4.dp)
            Spacer(Modifier.width(10.dp))
        }
        Txt(label, style.copy(color = style.color.copy(alpha = style.color.alpha * (0.55f + 0.45f * light))), maxLines = 1)
    }
}

/** Black letterbox bar. Its height is animated by screens to open or close the frame. */
@Composable
internal fun C01Bar(height: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(C01.Black)
            .padding(horizontal = C01.Margin),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}
