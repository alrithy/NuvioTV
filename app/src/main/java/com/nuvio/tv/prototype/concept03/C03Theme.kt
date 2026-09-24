package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.toArabicDigits
import com.nuvio.tv.prototype.shared.ts

/**
 * CONCEPT 03 — DESERT MONOLITH
 * Dark stone, basalt slabs, sand ink, bronze rules. Zero radius. Wide monumental type.
 * Motion is heavy and calm: 380–450ms, long decelerations, nothing bounces.
 */
internal object C03 {
    val Stone = Color(0xFF15120F)
    val Basalt = Color(0xFF211C18)
    val Slab = Color(0xFF2B2520)
    val Sand = Color(0xFFDCCBAE)
    val SandDim = Color(0xFF9D8E76)
    val SandFaint = Color(0xFF5B5247)
    val Bronze = Color(0xFFA67D4E)
    val BronzeLight = Color(0xFFCBA473)
    val Shadow = Color(0xFF0A0806)

    val Margin = 52.dp
    const val SLOW = 420
    const val FOCUS = 320
}

@Immutable
internal class C03Type(
    val monument: TextStyle,
    val listItem: TextStyle,
    val numeral: TextStyle,
    val heading: TextStyle,
    val label: TextStyle,
    val small: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
    val arabic: Boolean,
)

@Composable
internal fun c03Type(): C03Type {
    val f = LocalProtoFonts.current
    return if (!isArabic()) {
        C03Type(
            monument = ts(f.lexendExa, 34.sp, FontWeight.ExtraLight, C03.Sand, 0.02.em, 44.sp),
            listItem = ts(f.lexendExa, 17.sp, FontWeight.ExtraLight, C03.Sand, 0.04.em, 26.sp),
            numeral = ts(f.lexendExa, 58.sp, FontWeight.ExtraLight, C03.Sand, (-0.02).em, 62.sp),
            heading = ts(f.lexendExa, 22.sp, FontWeight.ExtraLight, C03.Sand, 0.04.em, 30.sp),
            label = ts(f.lexendExa, 10.sp, FontWeight.Normal, C03.SandDim, 0.18.em, 14.sp),
            small = ts(f.inter, 10.sp, FontWeight.Medium, C03.SandFaint, 0.16.em, 14.sp),
            body = ts(f.inter, 13.sp, FontWeight.Light, C03.SandDim, 0.01.em, 20.sp),
            button = ts(f.lexendExa, 11.sp, FontWeight.Normal, C03.Sand, 0.2.em, 16.sp),
            arabic = false,
        )
    } else {
        C03Type(
            monument = ts(f.notoKufi, 32.sp, FontWeight.Light, C03.Sand, 0.em, 54.sp),
            listItem = ts(f.notoKufi, 17.sp, FontWeight.Light, C03.Sand, 0.em, 30.sp),
            numeral = ts(f.notoKufi, 52.sp, FontWeight.Light, C03.Sand, 0.em, 74.sp),
            heading = ts(f.notoKufi, 21.sp, FontWeight.Light, C03.Sand, 0.em, 36.sp),
            label = ts(f.notoKufi, 11.sp, FontWeight.Medium, C03.SandDim, 0.em, 18.sp),
            small = ts(f.notoKufi, 10.sp, FontWeight.Medium, C03.SandFaint, 0.em, 16.sp),
            body = ts(f.notoKufi, 12.sp, FontWeight.Light, C03.SandDim, 0.em, 22.sp),
            button = ts(f.notoKufi, 12.sp, FontWeight.Medium, C03.Sand, 0.em, 18.sp),
            arabic = true,
        )
    }
}

@Composable
internal fun String.up(): String = if (isArabic()) this else uppercase()

/** Roman numerals in English; Arabic-Indic digits in Arabic — both read as carved numbers. */
@Composable
internal fun numeral(n: Int): String {
    if (isArabic()) return n.toString().toArabicDigits()
    val values = intArrayOf(10, 9, 5, 4, 1)
    val symbols = arrayOf("X", "IX", "V", "IV", "I")
    var x = n
    val sb = StringBuilder()
    for (i in values.indices) while (x >= values[i]) { sb.append(symbols[i]); x -= values[i] }
    return sb.toString()
}

@Composable
internal fun String.digits(): String = if (isArabic()) toArabicDigits() else this

@Composable
internal fun C03Screen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(C03.Stone, Color(0xFF110E0B))))
            .filmGrain(0.07f),
        content = content,
    )
}

@Composable
internal fun C03Rule(modifier: Modifier = Modifier, color: Color = C03.Bronze.copy(alpha = 0.45f)) {
    Box(modifier.height(1.dp).background(color))
}

@Composable
internal fun C03VRule(modifier: Modifier = Modifier, color: Color = C03.Bronze.copy(alpha = 0.35f)) {
    Box(modifier.width(1.dp).background(color))
}

/**
 * Monumental list entry. Focus doesn't draw a box: the name steps forward (scale from the start
 * edge), gains full sand and a bronze mark appears at its side, like light hitting carved stone.
 */
@Composable
internal fun C03ListItem(
    text: String,
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null,
    style: TextStyle,
    maxScale: Float = 1.55f,
    selected: Boolean = false,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C03.FOCUS, easing = ProtoEasing.Cinematic), label = "li")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier
            .fillMaxWidth()
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(26.dp).graphicsLayer { alpha = if (selected) 1f else f }.background(C03.Bronze))
        Spacer(Modifier.width(14.dp))
        Txt(
            text,
            style.copy(color = C03.Sand.copy(alpha = (0.26f + 0.74f * f + if (selected) 0.3f else 0f).coerceAtMost(1f))),
            Modifier.graphicsLayer {
                val s = 1f + (maxScale - 1f) * f
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            },
            maxLines = 1,
        )
    }
}

/** A stone slab button: outlined in bronze, fills with sand when focused and shifts forward. */
@Composable
internal fun C03SlabButton(
    label: String,
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null,
    primary: Boolean = false,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c03Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C03.FOCUS, easing = ProtoEasing.Cinematic), label = "sb")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .graphicsLayer { translationX = (if (rtl) -1 else 1) * 6.dp.toPx() * f }
            .background(if (focused) C03.Sand else if (primary) C03.Slab else Color.Transparent)
            .border(1.dp, if (focused) C03.Sand else C03.Bronze.copy(alpha = 0.55f))
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Txt(label, type.button.copy(color = if (focused) C03.Shadow else C03.Sand), maxLines = 1)
    }
}

/** Specification row: label column, value column, bronze hairline beneath. */
@Composable
internal fun C03Spec(label: String, value: String, labelWidth: Dp = 110.dp) {
    val type = c03Type()
    Column {
        Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(label.up(), type.label.copy(color = C03.SandFaint), Modifier.width(labelWidth), maxLines = 1)
            Txt(value, type.label.copy(color = C03.Sand), maxLines = 1)
        }
        C03Rule(Modifier.fillMaxWidth(), C03.Bronze.copy(alpha = 0.22f))
    }
}

/** Shade under the lintel and at the sill of an aperture, as if the opening were cut into a wall. */
internal val C03Lintel = Brush.verticalGradient(0f to Color(0xCC0A0806), 0.12f to Color.Transparent, 0.85f to Color.Transparent, 1f to Color(0x990A0806))

internal fun Modifier.stoneEdge(): Modifier = drawBehind {
    drawLine(C03.Bronze.copy(alpha = 0.35f), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
    drawLine(C03.Bronze.copy(alpha = 0.35f), Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
}
