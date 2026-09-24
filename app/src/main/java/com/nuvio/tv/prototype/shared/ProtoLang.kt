package com.nuvio.tv.prototype.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * Prototype language switch. Every concept is authored for both English (LTR) and Arabic (RTL);
 * the Prototype Hub flips this local and the layout direction together.
 */
enum class ProtoLang {
    EN, AR;

    val isRtl: Boolean get() = this == AR
}

val LocalProtoLang = compositionLocalOf { ProtoLang.EN }

/** Bilingual string used throughout the mock catalog and copy decks. */
@Immutable
data class Bi(val en: String, val ar: String) {
    fun of(lang: ProtoLang): String = if (lang == ProtoLang.AR) ar else en

    @Composable
    @ReadOnlyComposable
    fun get(): String = of(LocalProtoLang.current)

    companion object {
        /** Same text in both languages (brand names, technical terms). */
        fun same(text: String) = Bi(text, text)
    }
}

@Composable
@ReadOnlyComposable
fun tr(en: String, ar: String): String = if (LocalProtoLang.current == ProtoLang.AR) ar else en

@Composable
@ReadOnlyComposable
fun isArabic(): Boolean = LocalProtoLang.current == ProtoLang.AR

private const val EASTERN_DIGITS = "٠١٢٣٤٥٦٧٨٩"

/** Converts Western digits to Arabic-Indic digits. Used selectively by concepts that treat numerals as display type. */
fun String.toArabicDigits(): String = buildString(length) {
    for (c in this@toArabicDigits) append(if (c in '0'..'9') EASTERN_DIGITS[c - '0'] else c)
}

@Composable
@ReadOnlyComposable
fun String.localDigits(): String = if (isArabic()) toArabicDigits() else this

fun formatRuntime(minutes: Int, lang: ProtoLang): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (lang == ProtoLang.AR) {
        when {
            h == 0 -> "$m د"
            m == 0 -> "$h س"
            else -> "$h س $m د"
        }
    } else {
        when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

fun formatTimecode(totalSeconds: Int, forceHours: Boolean = false): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0 || forceHours) {
        "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
    } else {
        "$m:${sec.toString().padStart(2, '0')}"
    }
}
