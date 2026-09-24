package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.ts
import kotlinx.coroutines.delay

/**
 * CONCEPT 05 — ZERO CHROME
 * No permanent navigation, no panels, no cards on the main surface. Text sits directly on the
 * picture; metadata reads like a camera slate. Everything the UI adds disappears after idle.
 */
internal object C05 {
    val Ink = Color(0xFFFFFFFF)
    val Ink2 = Color(0xB3FFFFFF)
    val Ink3 = Color(0x66FFFFFF)
    val Signal = Color(0xFFFF4D2E)
    val Margin = 56.dp
    const val IDLE_MS = 3500L
}

@Immutable
internal class C05Type(
    val title: TextStyle,
    val big: TextStyle,
    val slate: TextStyle,
    val slateSmall: TextStyle,
    val body: TextStyle,
    val action: TextStyle,
)

@Composable
internal fun c05Type(): C05Type {
    val f = LocalProtoFonts.current
    val ar = isArabic()
    val sans = if (ar) f.plexArabic else f.inter
    val mono = if (ar) f.plexArabic else f.plexMono
    return C05Type(
        title = ts(sans, if (ar) 34.sp else 38.sp, FontWeight.SemiBold, C05.Ink, if (ar) 0.em else (-0.02).em, if (ar) 52.sp else 44.sp),
        big = ts(sans, if (ar) 46.sp else 54.sp, FontWeight.SemiBold, C05.Ink, if (ar) 0.em else (-0.03).em, if (ar) 70.sp else 60.sp),
        slate = ts(mono, 11.sp, FontWeight.Normal, C05.Ink2, if (ar) 0.em else 0.08.em, if (ar) 18.sp else 15.sp),
        slateSmall = ts(mono, 10.sp, FontWeight.Normal, C05.Ink3, if (ar) 0.em else 0.1.em, if (ar) 16.sp else 14.sp),
        body = ts(sans, 13.sp, FontWeight.Normal, C05.Ink2, lineHeight = if (ar) 23.sp else 20.sp),
        action = ts(mono, 12.sp, FontWeight.Medium, C05.Ink, if (ar) 0.em else 0.12.em, if (ar) 20.sp else 16.sp),
    )
}

@Composable
internal fun String.slate(): String = if (isArabic()) this else uppercase()

/** Tracks idle time; UI alpha falls to zero after [C05.IDLE_MS] without input. */
internal class C05Idle {
    var last by mutableLongStateOf(0L)
    fun poke() { last = System.nanoTime() }
}

@Composable
internal fun rememberIdleVisibility(idle: C05Idle): Float {
    val frozen = LocalProtoEnv.current.frozen
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(idle.last) {
        visible = true
        if (!frozen) {
            delay(C05.IDLE_MS)
            visible = false
        }
    }
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 220 else 900), label = "idle")
    return a
}

/** Mono text action with a signal-red cursor square when focused. No button shapes. */
@Composable
internal fun C05Action(
    label: String,
    glyph: Glyph? = null,
    requester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c05Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier.protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(6.dp).height(6.dp).graphicsLayer { alpha = if (focused) 1f else 0f }.background(C05.Signal))
        Spacer(Modifier.width(8.dp))
        if (glyph != null) {
            ProtoIcon(glyph, size = 12.dp, color = if (focused) C05.Ink else C05.Ink3, stroke = 1.5.dp)
            Spacer(Modifier.width(8.dp))
        }
        Txt(label.slate(), type.action.copy(color = if (focused) C05.Ink else C05.Ink3), maxLines = 1)
    }
}

/** Edge hint: a whisper of what lies in a direction ("↑ CONTINUE"). */
@Composable
internal fun C05Hint(glyph: Glyph, text: String, modifier: Modifier = Modifier) {
    val type = c05Type()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ProtoIcon(glyph, size = 12.dp, color = C05.Ink3, stroke = 1.5.dp)
        Txt(text.slate(), type.slateSmall, maxLines = 1)
    }
}
