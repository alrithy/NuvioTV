package com.nuvio.tv.prototype.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Platform capabilities and evaluation switches for the prototype system.
 *
 * - [remoteArtwork]: load real artwork over the network (Coil). When false, only the procedural
 *   "illustrated" artwork is shown, which keeps every concept comparable offline.
 * - [supportsBlur]: RenderEffect blur is only available on Android 12+. Concepts fall back to
 *   color-field backgrounds when this is false.
 * - [frozen]: deterministic mode used for screenshot rendering. Clocks stop, controls never
 *   auto-hide and ambient loops hold still.
 */
@Immutable
data class ProtoEnv(
    val remoteArtwork: Boolean = true,
    val supportsBlur: Boolean = true,
    val frozen: Boolean = false,
)

val LocalProtoEnv = staticCompositionLocalOf { ProtoEnv() }

/** Font families used by the concepts. The Android activity supplies bundled resources. */
@Immutable
class ProtoFonts(
    val jost: FontFamily = FontFamily.SansSerif,
    val inter: FontFamily = FontFamily.SansSerif,
    val alexandria: FontFamily = FontFamily.SansSerif,
    val sora: FontFamily = FontFamily.SansSerif,
    val readex: FontFamily = FontFamily.SansSerif,
    val lexendExa: FontFamily = FontFamily.SansSerif,
    val notoKufi: FontFamily = FontFamily.SansSerif,
    val manrope: FontFamily = FontFamily.SansSerif,
    val plexArabic: FontFamily = FontFamily.SansSerif,
    val plexMono: FontFamily = FontFamily.Monospace,
    val barlow: FontFamily = FontFamily.SansSerif,
    val cairo: FontFamily = FontFamily.SansSerif,
    val instrumentSerif: FontFamily = FontFamily.Serif,
    val instrumentSans: FontFamily = FontFamily.SansSerif,
    val amiri: FontFamily = FontFamily.Serif,
    val outfit: FontFamily = FontFamily.SansSerif,
    val tajawal: FontFamily = FontFamily.SansSerif,
    val reemKufi: FontFamily = FontFamily.SansSerif,
    val geist: FontFamily = FontFamily.SansSerif,
)

val LocalProtoFonts = staticCompositionLocalOf { ProtoFonts() }

@Immutable
data class ProtoTime(val hour24: Int, val minute: Int, val weekday: Int, val day: Int, val month: Int, val year: Int = 2025) {
    val hour12: Int get() = ((hour24 + 11) % 12) + 1
    val isPm: Boolean get() = hour24 >= 12
    val minutePadded: String get() = minute.toString().padStart(2, '0')

    fun clock12(lang: ProtoLang): String =
        if (lang == ProtoLang.AR) "$hour12:$minutePadded ${if (isPm) "م" else "ص"}"
        else "$hour12:$minutePadded ${if (isPm) "PM" else "AM"}"

    fun clock24(): String = "${hour24.toString().padStart(2, '0')}:$minutePadded"

    fun weekdayName(lang: ProtoLang): String {
        val en = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val ar = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        return if (lang == ProtoLang.AR) ar[weekday] else en[weekday]
    }

    fun monthName(lang: ProtoLang): String {
        val en = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val ar = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        return if (lang == ProtoLang.AR) ar[month] else en[month]
    }

    fun greeting(lang: ProtoLang): String = when (hour24) {
        in 5..11 -> if (lang == ProtoLang.AR) "صباح الخير" else "Good morning"
        in 12..16 -> if (lang == ProtoLang.AR) "طاب يومك" else "Good afternoon"
        else -> if (lang == ProtoLang.AR) "مساء الخير" else "Good evening"
    }

    companion object {
        /** Fixed evening time used for screenshots: Thursday 18 September 2025, 9:41 PM. */
        val Demo = ProtoTime(hour24 = 21, minute = 41, weekday = 4, day = 18, month = 8, year = 2025)

        fun now(): ProtoTime {
            val c = Calendar.getInstance()
            return ProtoTime(
                hour24 = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                weekday = c.get(Calendar.DAY_OF_WEEK) - 1,
                day = c.get(Calendar.DAY_OF_MONTH),
                month = c.get(Calendar.MONTH),
                year = c.get(Calendar.YEAR),
            )
        }
    }
}

@Composable
fun rememberProtoClock(): State<ProtoTime> {
    val frozen = LocalProtoEnv.current.frozen
    val state = remember { mutableStateOf(if (frozen) ProtoTime.Demo else ProtoTime.now()) }
    if (!frozen) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(15_000)
                state.value = ProtoTime.now()
            }
        }
    }
    return state
}

/** Tracks the last D-pad interaction so concepts can auto-hide chrome after idle time. */
class IdleTracker {
    var lastInteraction by mutableStateOf(0L)
        private set

    fun touch() {
        lastInteraction = System.nanoTime()
    }
}
