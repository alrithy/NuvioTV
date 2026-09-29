package com.nuvio.tv.fork.video

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Dolby Vision RPU static HDR metadata, as libdovi reports it (G5d, features 53/54). ALGORITHM_PORT
 * of ysosrs `DoviBridge.RpuStaticMetadata` @ 45e0984. source_*_pq are 12-bit PQ codes (always
 * present with DM data); the L6 fields are null when the RPU has no L6 block.
 */
data class RpuStaticMetadata(
    val sourceMinPq: Int,
    val sourceMaxPq: Int,
    val l6MinMasteringLuminance: Int? = null,
    val l6MaxMasteringLuminance: Int? = null,
    val maxCll: Int? = null,
    val maxFall: Int? = null,
) {
    /** "MaxCLL 1000 · MaxFALL 400 · MDL ~1000 nits"; a present-but-zero L6 value means unknown. */
    fun toDiagnosticLine(): String {
        val parts = ArrayList<String>(3)
        maxCll?.let { parts += "MaxCLL ${if (it > 0) it.toString() else "-"}" }
        maxFall?.let { parts += "MaxFALL ${if (it > 0) it.toString() else "-"}" }
        parts += "MDL ~${pqCodeToNits(sourceMaxPq).roundToInt()} nits"
        return parts.joinToString(" · ")
    }

    companion object {
        /** From the native int[6]; negative entries are "absent". */
        fun fromNative(values: IntArray?): RpuStaticMetadata? {
            if (values == null || values.size < 6) return null
            fun opt(i: Int): Int? = values[i].takeIf { it >= 0 }
            return RpuStaticMetadata(values[0], values[1], opt(2), opt(3), opt(4), opt(5))
        }
    }
}

/** SMPTE ST 2084 (PQ) EOTF applied to a 12-bit code value; returns nits. */
fun pqCodeToNits(pq12: Int): Double {
    val e = pq12.coerceIn(0, 4095) / 4095.0
    val m1 = 0.1593017578125
    val m2 = 78.84375
    val c1 = 0.8359375
    val c2 = 18.8515625
    val c3 = 18.6875
    val ep = e.pow(1.0 / m2)
    val num = (ep - c1).coerceAtLeast(0.0)
    val den = c2 - c3 * ep
    val l = if (den <= 0.0) 0.0 else (num / den).pow(1.0 / m1)
    return l * 10_000.0
}

/** Profile-7 enhancement-layer type from the first RPU (libdovi header `el_type`). */
enum class DvElType { FEL, MEL, NONE;
    companion object {
        /** Native codes: 2 FEL, 1 MEL, 0 parsed/not P7; negative = unknown. */
        fun fromNative(code: Int): DvElType? = when (code) {
            2 -> FEL
            1 -> MEL
            0 -> NONE
            else -> null
        }
    }
}

/**
 * What the current DV stream revealed, for the HUD `dv` row (feature 53). Written once per stream
 * by the extractor thread, read by the HUD; reset when a new stream's transformer is built.
 */
object DvStreamInfo {
    @Volatile var elType: DvElType? = null
    @Volatile var metadata: RpuStaticMetadata? = null
    @Volatile var hdr10SeiInjected: Boolean = false

    fun reset() {
        elType = null
        metadata = null
        hdr10SeiInjected = false
    }

    /** HUD value, or null when nothing is known. */
    fun hudLine(): String? {
        val parts = ArrayList<String>(3)
        elType?.takeIf { it != DvElType.NONE }?.let { parts += it.name }
        metadata?.let { parts += it.toDiagnosticLine() }
        if (hdr10SeiInjected) parts += "HDR10 SEI added"
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
