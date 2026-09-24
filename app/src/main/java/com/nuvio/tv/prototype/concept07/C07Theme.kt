package com.nuvio.tv.prototype.concept07

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 07 — EDITORIAL CINEMA
 * Paper-black pages, ivory ink, one vermilion accent. High-contrast serif for headlines,
 * a quiet grotesk for navigation, hairline rules for structure. Curated, asymmetric, finite.
 */
internal object C07 {
    val Paper = Color(0xFF121110)
    val PaperLight = Color(0xFF1B1A18)
    val Ink = Color(0xFFF1EBDF)
    val Ink2 = Color(0xFFB3AB9E)
    val Ink3 = Color(0xFF6F695F)
    val Red = Color(0xFFD9412B)
    val Rule = Color(0x33F1EBDF)

    val Margin = 56.dp
    val Gutter = 24.dp
    const val FOCUS = 300
}

@Immutable
internal class C07Type(
    val masthead: TextStyle,
    val headline: TextStyle,
    val headlineM: TextStyle,
    val headlineS: TextStyle,
    val deck: TextStyle,
    val quote: TextStyle,
    val numeral: TextStyle,
    val body: TextStyle,
    val kicker: TextStyle,
    val caption: TextStyle,
    val nav: TextStyle,
    val arabic: Boolean,
)

@Composable
internal fun c07Type(): C07Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val serif = if (ar) f.amiri else f.instrumentSerif
    val sans = if (ar) f.plexArabic else f.instrumentSans
    return C07Type(
        masthead = ts(serif, if (ar) 40.sp else 46.sp, FontWeight.Normal, C07.Ink, if (ar) 0.em else (-0.02).em, if (ar) 60.sp else 48.sp),
        headline = ts(serif, if (ar) 50.sp else 62.sp, if (ar) FontWeight.Bold else FontWeight.Normal, C07.Ink, if (ar) 0.em else (-0.025).em, if (ar) 76.sp else 62.sp),
        headlineM = ts(serif, if (ar) 30.sp else 34.sp, if (ar) FontWeight.Bold else FontWeight.Normal, C07.Ink, if (ar) 0.em else (-0.015).em, if (ar) 48.sp else 38.sp),
        headlineS = ts(serif, if (ar) 20.sp else 22.sp, FontWeight.Normal, C07.Ink, lineHeight = if (ar) 34.sp else 26.sp),
        deck = ts(serif, if (ar) 17.sp else 19.sp, FontWeight.Normal, C07.Ink2, lineHeight = if (ar) 30.sp else 26.sp, italic = !ar),
        quote = ts(serif, if (ar) 24.sp else 28.sp, FontWeight.Normal, C07.Ink, lineHeight = if (ar) 42.sp else 34.sp, italic = !ar),
        numeral = ts(serif, if (ar) 44.sp else 52.sp, FontWeight.Normal, C07.Red, lineHeight = if (ar) 60.sp else 52.sp),
        body = ts(sans, 13.sp, FontWeight.Normal, C07.Ink2, lineHeight = if (ar) 23.sp else 20.sp),
        kicker = ts(sans, 10.sp, FontWeight.SemiBold, C07.Red, if (ar) 0.em else 0.18.em, if (ar) 16.sp else 13.sp),
        caption = ts(sans, 10.sp, FontWeight.Normal, C07.Ink3, lineHeight = if (ar) 16.sp else 14.sp),
        nav = ts(sans, 11.sp, FontWeight.Medium, C07.Ink2, if (ar) 0.em else 0.08.em, if (ar) 18.sp else 15.sp),
        arabic = ar,
    )
}

@Composable
internal fun String.kick(): String = if (isArabic()) this else uppercase()

@Composable
internal fun C07Rule(modifier: Modifier = Modifier, color: Color = C07.Rule) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Editorial focus: a vermilion rule is set above the focused block, like a section marker. */
internal fun Modifier.redRule(focus: Float): Modifier = drawBehind {
    if (focus > 0.01f) {
        val w = size.width * focus
        drawLine(C07.Red, Offset(0f, -8.dp.toPx()), Offset(w, -8.dp.toPx()), 3.dp.toPx())
    }
}

@Composable
internal fun c07Anim(focused: Boolean): Float {
    val v by animateFloatAsState(if (focused) 1f else 0f, tween(C07.FOCUS, easing = ProtoEasing.Standard), label = "c07")
    return v
}

/** Link-style action: small caps, with a vermilion arrow that advances on focus. */
@Composable
internal fun C07Link(
    label: String,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .drawBehind {
                val y = size.height
                drawLine(C07.Red.copy(alpha = f), Offset(0f, y), Offset(size.width * f, y), 1.5.dp.toPx())
            }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label.kick(), type.nav.copy(color = if (focused) C07.Ink else C07.Ink2), maxLines = 1)
        Spacer(Modifier.width(6.dp))
        ProtoIcon(Glyph.CHEVRON_RIGHT, Modifier.graphicsLayer { translationX = (if (rtl) -1 else 1) * 4.dp.toPx() * f }, size = 10.dp, color = if (focused) C07.Red else C07.Ink3, stroke = 1.8.dp)
    }
}

/** Masthead with section navigation, like the top of a magazine page. */
@Composable
internal fun C07Masthead(selected: Int, onSection: (Int) -> Unit) {
    val type = c07Type()
    val sections = listOf("Cover" to "الغلاف", "Film" to "أفلام", "Series" to "مسلسلات", "Arabic" to "عربي", "Index" to "الفهرس", "Library" to "المكتبة", "Profile" to "الملف")
    Column(Modifier.fillMaxWidth().padding(horizontal = C07.Margin).padding(top = 22.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(if (type.arabic) "نوفيو" else "Nuvio", type.masthead)
            Spacer(Modifier.width(18.dp))
            Txt(if (type.arabic) "العدد ٣٨ · الخميس ١٨ سبتمبر · طبعة المساء" else "No. 38 · Thursday 18 September · The evening edition", type.caption, Modifier.padding(bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            Row(Modifier.padding(bottom = 8.dp)) {
                sections.forEachIndexed { i, (en, ar) ->
                    var focused by remember { mutableStateOf(false) }
                    Txt(
                        (if (type.arabic) ar else en).kick(),
                        type.nav.copy(color = if (focused) C07.Ink else if (i == selected) C07.Red else C07.Ink3),
                        Modifier
                            .protoFocusable(onFocusChange = { focused = it }, onClick = { onSection(i) })
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        C07Rule(color = C07.Ink.copy(alpha = 0.7f))
        Spacer(Modifier.height(2.dp))
        C07Rule()
    }
}
