package com.nuvio.tv.fork.audio

import androidx.media3.common.MimeTypes

/**
 * Per-format passthrough controls (G5c, feature 38). FILE_PORT of ysosrs `AudioPassthroughPolicy`
 * @ 45e0984 (user switches only; its learned-rejection and AC-3 transcode layers are not ported).
 *
 * Android bitstreams what the HDMI/eARC chain claims to decode, and some chains over-report (the
 * EDID lists DTS, the receiver plays silence). Each switch means "my receiver decodes this format":
 * on (the default) keeps the official behavior, off decodes that format to PCM in the app.
 *
 * Safety: [ALLOW_ALL] denies nothing, and only formats the bundled FFmpeg decoder handles (AC-3,
 * E-AC-3, TrueHD, DTS, DTS-HD) can be denied, and only while [softwareDecodersAvailable]; AC-4,
 * DTS Express and DTS:X P2 map to no group and always follow the platform report.
 */
/** The user-facing switches, in settings order; [key] is the DataStore key. */
enum class PassthroughFormat(val key: String) {
    AC3("passthrough_ac3"),
    EAC3("passthrough_eac3"),
    TRUEHD("passthrough_truehd"),
    DTS("passthrough_dts"),
    DTS_HD("passthrough_dtshd"),
}

data class AudioPassthroughPolicy(
    val allowAc3: Boolean = true,
    val allowEac3: Boolean = true,
    val allowTrueHd: Boolean = true,
    val allowDts: Boolean = true,
    val allowDtsHd: Boolean = true,
    val softwareDecodersAvailable: Boolean = true,
) {
    enum class Group { AC3, EAC3, TRUEHD, DTS, DTS_HD }

    /** True when [mimeType] must be decoded instead of bitstreamed. */
    fun deniesPassthrough(mimeType: String?): Boolean {
        if (!softwareDecodersAvailable) return false
        return when (groupOf(mimeType) ?: return false) {
            Group.AC3 -> !allowAc3
            Group.EAC3 -> !allowEac3
            Group.TRUEHD -> !allowTrueHd
            Group.DTS -> !allowDts
            Group.DTS_HD -> !allowDtsHd
        }
    }

    fun allowsEverything(): Boolean = allowAc3 && allowEac3 && allowTrueHd && allowDts && allowDtsHd

    /** Groups the user turned off, for diagnostics. */
    fun deniedGroups(): List<Group> = Group.entries.filter { group ->
        when (group) {
            Group.AC3 -> !allowAc3
            Group.EAC3 -> !allowEac3
            Group.TRUEHD -> !allowTrueHd
            Group.DTS -> !allowDts
            Group.DTS_HD -> !allowDtsHd
        }
    }

    companion object {
        val ALLOW_ALL = AudioPassthroughPolicy()

        /**
         * Exact match, never prefix: DTS Express ("audio/vnd.dts.hd;profile=lbr") starts with the
         * DTS-HD type but has no FFmpeg decoder. A null type delegates (codecs "dts" cannot tell
         * DTS from DTS-HD).
         */
        fun groupOf(mimeType: String?): Group? = when (mimeType) {
            MimeTypes.AUDIO_AC3 -> Group.AC3
            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> Group.EAC3
            MimeTypes.AUDIO_TRUEHD -> Group.TRUEHD
            MimeTypes.AUDIO_DTS -> Group.DTS
            MimeTypes.AUDIO_DTS_HD -> Group.DTS_HD
            else -> null
        }
    }
}
