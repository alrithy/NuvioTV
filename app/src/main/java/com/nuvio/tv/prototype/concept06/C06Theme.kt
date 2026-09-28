package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.tr
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 06 — LUXURY MEDIA CONSOLE
 * Graphite, anodised modules with a single-pixel top bevel, one ember signal colour.
 * Focus is a detent: the surface brightens and an ember bar slides in. No scale, no bounce.
 * All numbers use tabular figures so readouts never jitter.
 */
internal object C06 {
    val Base = Color(0xFF0B0C0E)
    val Panel = Color(0xFF14161A)
    val PanelHi = Color(0xFF1D2025)
    val PanelFocus = Color(0xFF262A30)
    val Bevel = Color(0x14FFFFFF)
    val Ink = Color(0xFFE9ECEF)
    val Ink2 = Color(0xFF9AA1A9)
    val Ink3 = Color(0xFF5E656D)
    val Ember = Color(0xFFE4572E)
    val EmberDim = Color(0x55E4572E)
    val Ok = Color(0xFF5FD39A)

    val Gap = 8.dp
    val Radius = 10.dp
    const val DETENT = 200
}

@Immutable
internal class C06Type(
    val display: TextStyle,
    val title: TextStyle,
    val readout: TextStyle,
    val readoutBig: TextStyle,
    val label: TextStyle,
    val body: TextStyle,
    val value: TextStyle,
    val caption: TextStyle,
)

@Composable
internal fun c06Type(): C06Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val fam = if (ar) f.cairo else f.barlow
    val tnum = "tnum"
    return C06Type(
        display = ts(fam, if (ar) 30.sp else 32.sp, if (ar) FontWeight.Medium else FontWeight.Normal, C06.Ink, if (ar) 0.em else (-0.01).em, if (ar) 48.sp else 38.sp),
        title = ts(fam, 16.sp, FontWeight.Medium, C06.Ink, lineHeight = if (ar) 26.sp else 20.sp),
        readout = ts(fam, 13.sp, FontWeight.Medium, C06.Ink, lineHeight = 18.sp).copy(fontFeatureSettings = tnum),
        readoutBig = ts(fam, 28.sp, FontWeight.Light, C06.Ink, lineHeight = 32.sp).copy(fontFeatureSettings = tnum),
        label = ts(fam, if (ar) 10.sp else 9.sp, FontWeight.SemiBold, C06.Ink3, if (ar) 0.em else 0.16.em, if (ar) 16.sp else 12.sp),
        body = ts(fam, 12.sp, FontWeight.Normal, C06.Ink2, lineHeight = if (ar) 22.sp else 18.sp),
        value = ts(fam, 12.sp, FontWeight.Medium, C06.Ink, lineHeight = if (ar) 20.sp else 16.sp).copy(fontFeatureSettings = tnum),
        caption = ts(fam, 10.sp, FontWeight.Normal, C06.Ink2, lineHeight = if (ar) 16.sp else 13.sp).copy(fontFeatureSettings = tnum),
    )
}

@Composable
internal fun String.lbl(): String = if (isArabic()) this else uppercase()

/** Detent focus animation used everywhere in this concept. */
@Composable
internal fun detent(focused: Boolean): Float {
    val v by animateFloatAsState(if (focused) 1f else 0f, tween(C06.DETENT, easing = ProtoEasing.Detent), label = "detent")
    return v
}

/** An anodised module: rounded panel with a one-pixel top bevel and an ember focus bar. */
internal fun Modifier.module(focus: Float = 0f, radius: Dp = C06.Radius): Modifier = this
    .clip(RoundedCornerShape(radius))
    .drawBehind {
        val base = if (focus > 0f) lerpColor(C06.PanelHi, C06.PanelFocus, focus) else C06.Panel
        drawRect(Brush.verticalGradient(listOf(base, lerpColor(base, C06.Base, 0.35f))))
        drawLine(C06.Bevel, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1f)
        if (focus > 0.01f) {
            val w = size.width * focus
            drawRect(C06.Ember, Offset((size.width - w) / 2, size.height - 2.dp.toPx()), Size(w, 2.dp.toPx()))
        }
    }

internal fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

/** A focusable module whose content can react to focus. */
@Composable
internal fun C06Module(
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null,
    focusable: Boolean = true,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.(focus: Float) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Column(
        modifier
            .module(f)
            .then(if (focusable) Modifier.protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick) else Modifier)
            .padding(padding),
    ) { content(f) }
}

@Composable
internal fun C06Label(text: String, modifier: Modifier = Modifier) {
    Txt(text.lbl(), c06Type().label, modifier, maxLines = 1)
}

/** Rectangular console button. Primary uses the ember signal. */
@Composable
internal fun C06Button(
    label: String,
    glyph: Glyph? = null,
    primary: Boolean = false,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    primary -> lerpColor(C06.Ember, Color(0xFFFF7A4F), f)
                    else -> lerpColor(C06.PanelHi, C06.Ink, f)
                },
            )
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (primary) Color.White else lerpColor(C06.Ink, C06.Base, f)
        if (glyph != null) {
            ProtoIcon(glyph, size = 13.dp, color = ink, stroke = 1.6.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label.lbl(), type.label.copy(color = ink, fontSize = 10.sp), maxLines = 1)
    }
}

/** Linear gauge: a thin track, a fill and an optional reference mark. */
@Composable
internal fun C06Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = C06.Ink, mark: Float? = null) {
    Canvas(modifier.height(4.dp)) {
        drawRect(Color(0x1FFFFFFF), size = size)
        drawRect(color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height))
        if (mark != null) drawRect(C06.Ember, Offset(size.width * mark - 1f, -3f), Size(2f, size.height + 6f))
    }
}

/** Arc gauge in the style of a tachometer: 240° sweep, ticks, needle as a filled arc. */
@Composable
internal fun C06Arc(fraction: Float, modifier: Modifier = Modifier, color: Color = C06.Ember) {
    Box(
        modifier.drawWithCache {
            val stroke = 5.dp.toPx()
            val r = size.minDimension / 2 - stroke
            val c = Offset(size.width / 2, size.height / 2)
            onDrawBehind {
                drawArc(Color(0x22FFFFFF), 150f, 240f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 150f, 240f * fraction.coerceIn(0f, 1f), false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                for (k in 0..12) {
                    val a = Math.toRadians((150 + k * 20).toDouble())
                    val o = Offset(c.x + (r - stroke * 1.6f) * kotlin.math.cos(a).toFloat(), c.y + (r - stroke * 1.6f) * kotlin.math.sin(a).toFloat())
                    val i = Offset(c.x + (r - stroke * 2.6f) * kotlin.math.cos(a).toFloat(), c.y + (r - stroke * 2.6f) * kotlin.math.sin(a).toFloat())
                    drawLine(Color(0x44FFFFFF), i, o, 1.2f)
                }
            }
        },
    )
}

internal val C06RailItems = listOf(
    Triple(Glyph.HOME, Bi("Home", "الرئيسية"), null as ProtoRoute?),
    Triple(Glyph.MOVIES, Bi("Movies", "أفلام"), null),
    Triple(Glyph.SERIES, Bi("Series", "مسلسلات"), null),
    Triple(Glyph.SEARCH, Bi("Search", "بحث"), ProtoRoute.Search),
    Triple(Glyph.LIBRARY, Bi("Library", "مكتبة"), ProtoRoute.Library),
    Triple(Glyph.PROFILE, Bi("Profile", "الملف"), ProtoRoute.Profile),
)

/**
 * Drive-mode rail. The ember indicator slides between modes like a gear selector;
 * the rail surface has a faint brushed-metal grain.
 */
@Composable
internal fun C06Rail(session: ProtoSession, selected: Int) {
    val type = c06Type()
    val density = LocalDensity.current
    val ys = remember { mutableStateMapOf<Int, Float>() }
    var hover by remember { mutableStateOf(selected) }
    val y by animateDpAsState(with(density) { (ys[hover] ?: 0f).toDp() }, tween(C06.DETENT + 60, easing = ProtoEasing.Detent), label = "rail")
    Box(
        Modifier
            .width(78.dp)
            .fillMaxHeight()
            .drawWithCache {
                val grain = Brush.verticalGradient(listOf(Color(0xFF17191D), Color(0xFF101114)))
                onDrawBehind {
                    drawRect(grain)
                    var yy = 0f
                    while (yy < size.height) {
                        drawLine(Color(0x06FFFFFF), Offset(0f, yy), Offset(size.width, yy), 1f)
                        yy += 3f
                    }
                    drawLine(C06.Bevel, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1f)
                }
            },
    ) {
        Box(Modifier.absoluteOffset(y = y).width(3.dp).height(46.dp).background(C06.Ember))
        Column(Modifier.fillMaxSize().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            C06RailItems.forEachIndexed { i, (g, label, route) ->
                var focused by remember { mutableStateOf(false) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .onGloballyPositioned { ys[i] = it.positionInParent().y + with(density) { 70.dp.toPx() } }
                        .protoFocusable(onFocusChange = { focused = it; if (it) hover = i else if (hover == i) hover = selected }, onClick = {
                            if (route != null) session.nav.push(route) else if (i == 0) session.nav.home()
                        }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    val on = focused || i == selected
                    ProtoIcon(g, size = 16.dp, color = if (on) C06.Ink else C06.Ink3, stroke = 1.5.dp)
                    Spacer(Modifier.height(4.dp))
                    Txt(label.get().lbl(), type.label.copy(color = if (on) C06.Ink2 else C06.Ink3, fontSize = 8.sp), maxLines = 1)
                }
            }
        }
    }
}

/** Status strip: the console's always-on readouts. */
@Composable
internal fun C06Status(title: String) {
    val type = c06Type()
    val clock by rememberProtoClock()
    Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(title.lbl(), type.label.copy(color = C06.Ink2, fontSize = 10.sp))
        Spacer(Modifier.weight(1f))
        C06StatusItem(tr("DISPLAY", "الشاشة"), "4K · DV")
        C06StatusItem(tr("AUDIO", "الصوت"), "Atmos")
        C06StatusItem(tr("NETWORK", "الشبكة"), "480 Mbps")
        C06StatusItem("RD", tr("211 days", "211 يوماً"), dot = C06.Ok)
        Spacer(Modifier.width(14.dp))
        Txt(clock.clock24(), type.readout.copy(fontSize = 15.sp))
    }
}

@Composable
private fun C06StatusItem(label: String, value: String, dot: Color? = null) {
    val type = c06Type()
    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(dot))
            Spacer(Modifier.width(5.dp))
        }
        Txt(label, type.label)
        Spacer(Modifier.width(6.dp))
        Txt(value, type.caption.copy(color = C06.Ink))
    }
}

@Composable
internal fun C06Screen(session: ProtoSession, rail: Int, title: String, content: @Composable BoxScope.() -> Unit) {
    Row(Modifier.fillMaxSize().background(C06.Base)) {
        C06Rail(session, rail)
        Column(Modifier.fillMaxSize()) {
            C06Status(title)
            Box(Modifier.fillMaxSize().padding(start = 14.dp, end = 18.dp, bottom = 16.dp), content = content)
        }
    }
}
