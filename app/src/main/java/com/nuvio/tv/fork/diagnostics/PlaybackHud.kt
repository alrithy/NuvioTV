package com.nuvio.tv.fork.diagnostics

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * G5c (39): the audio chain as the platform reports it, captured once per sink build.
 * [direct] lists the bitstream formats the platform accepts directly (same question media3 asks),
 * [surroundMode] is the Android TV surround setting (AUTO/MANUAL/...), [maxPcmChannels] the
 * highest PCM channel count the HDMI output negotiated. Null parts were not readable.
 */
data class AudioChainSnapshot(
    val direct: List<String>,
    val surroundMode: String? = null,
    val maxPcmChannels: Int? = null,
)

/** One HUD line. A null value means the device or stream does not expose it; the row is dropped. */
data class HudRow(val label: String, val value: String, val warn: Boolean = false)

/** Primitive inputs read from the player on the application thread once per HUD sample. */
data class PlaybackHudInput(
    val videoMime: String? = null,
    val videoCodecs: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val colorTransfer: Int? = null,
    val audioMime: String? = null,
    val audioCodecs: String? = null,
    val audioChannels: Int? = null,
    val audioSampleRate: Int? = null,
    val audioOutputEncoding: Int? = null,
    /** G5c (42): channel count the AudioTrack was configured with; meaningful for PCM only. */
    val audioOutputChannels: Int? = null,
    /** G5c (41): stream-reported audio bitrate (Format.bitrate); null or <= 0 = not reported. */
    val audioBitrateBps: Int? = null,
    /** G5c (40): decoded because the user's per-format receiver switch is off. */
    val audioPassthroughDenied: Boolean = false,
    /** G5c (39): what the platform claimed at sink build. */
    val audioChain: AudioChainSnapshot? = null,
    /** G5d (53): EL type / RPU mastering metadata / SEI outcome of the current DV stream. */
    val dvStreamInfo: String? = null,
    val displayRefreshHz: Float? = null,
    val requiredBps: Long? = null,
    val availableBps: Long? = null,
    val parallelConnections: Int? = null,
    val chunkBytes: Long? = null,
    val rebuffers: Int = 0,
    val audioUnderruns: Int = 0,
    val loadErrors: Int = 0,
    val clockDriftMsPerSecond: Long? = null,
    val socModel: String? = null,
    val strategy: String = PlaybackHud.OFFICIAL_STRATEGY,
)

/**
 * Formats the Superfork additions to the official stats HUD. Metric selection follows ysosrs
 * PlaybackStatsOverlay (45e0984); values are only ever read from the stream/device, never guessed.
 */
object PlaybackHud {

    fun rows(input: PlaybackHudInput): List<HudRow> = listOfNotNull(
        video(input),
        hdr(input),
        input.dvStreamInfo?.takeIf { it.isNotBlank() }?.let { HudRow("dv", it) },
        display(input),
        audio(input),
        output(input.audioOutputEncoding, input.audioOutputChannels, input.audioPassthroughDenied),
        chain(input.audioChain),
        bitrateNeed(input.requiredBps, input.availableBps),
        connections(input.parallelConnections, input.chunkBytes),
        HudRow("rebuffer", input.rebuffers.toString(), warn = input.rebuffers > 0),
        HudRow("underrun", input.audioUnderruns.toString(), warn = input.audioUnderruns > 0),
        input.loadErrors.takeIf { it > 0 }?.let { HudRow("load err", it.toString(), warn = true) },
        input.clockDriftMsPerSecond?.let { HudRow("a-clock", "drift $it ms/s", warn = it >= DRIFT_WARN_MS) },
        input.socModel?.takeIf { it.isNotBlank() }?.let { HudRow("soc", it) },
        HudRow("strategy", input.strategy),
    )

    internal fun video(input: PlaybackHudInput): HudRow? {
        val codec = videoCodecName(input.videoMime, input.videoCodecs) ?: return null
        val size = if ((input.width ?: 0) > 0 && (input.height ?: 0) > 0) " ${input.width}x${input.height}" else ""
        val fps = input.frameRate?.takeIf { it > 0f }?.let { String.format(Locale.US, " %.3f fps", it) } ?: ""
        return HudRow("video", codec + size + fps)
    }

    internal fun hdr(input: PlaybackHudInput): HudRow? {
        dolbyVisionProfile(input.videoMime, input.videoCodecs)?.let { profile ->
            return HudRow("hdr", if (profile > 0) "Dolby Vision P$profile" else "Dolby Vision")
        }
        return when (input.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> HudRow("hdr", "HDR10 (PQ)")
            C.COLOR_TRANSFER_HLG -> HudRow("hdr", "HLG")
            C.COLOR_TRANSFER_SDR, C.COLOR_TRANSFER_GAMMA_2_2 -> HudRow("hdr", "SDR")
            else -> null
        }
    }

    /** Warns when the display rate is not a whole multiple of the video rate (judder). */
    internal fun display(input: PlaybackHudInput): HudRow? {
        val hz = input.displayRefreshHz?.takeIf { it > 0f } ?: return null
        val fps = input.frameRate?.takeIf { it > 0f }
        val ratio = fps?.let { hz / it }
        val mismatch = ratio != null && abs(ratio - ratio.roundToInt()) > CADENCE_TOLERANCE
        return HudRow("display", String.format(Locale.US, "%.3f Hz", hz), warn = mismatch)
    }

    internal fun audio(input: PlaybackHudInput): HudRow? {
        val codec = audioCodecName(input.audioMime) ?: return null
        val objectAudio = when {
            input.audioMime == MimeTypes.AUDIO_E_AC3_JOC ||
                input.audioCodecs?.contains("ec+3", ignoreCase = true) == true -> " Atmos"
            input.audioMime == MimeTypes.AUDIO_DTS_X -> " DTS:X"
            else -> ""
        }
        val channels = input.audioChannels?.let(::channelLayout)?.let { " $it" } ?: ""
        val rate = input.audioSampleRate?.takeIf { it > 0 }?.let { " ${it / 1000}kHz" } ?: ""
        val bitrate = input.audioBitrateBps?.takeIf { it > 0 }?.let { " ${formatBitrate(it)}" } ?: ""
        return HudRow("audio", codec + objectAudio + channels + rate + bitrate)
    }

    /** Real output path as configured on the AudioTrack: bitstream passthrough or decoded PCM. */
    internal fun output(encoding: Int?, outputChannels: Int? = null, passthroughDenied: Boolean = false): HudRow? {
        encoding ?: return null
        val passthrough = when (encoding) {
            C.ENCODING_AC3 -> "AC3"
            C.ENCODING_E_AC3 -> "E-AC3"
            C.ENCODING_E_AC3_JOC -> "E-AC3 Atmos"
            C.ENCODING_AC4 -> "AC4"
            C.ENCODING_DTS -> "DTS"
            C.ENCODING_DTS_HD -> "DTS-HD"
            C.ENCODING_DTS_UHD_P2 -> "DTS:X"
            C.ENCODING_DOLBY_TRUEHD -> "TrueHD"
            else -> null
        }
        if (passthrough != null) return HudRow("output", "passthrough $passthrough")
        val reason = if (passthroughDenied) "decoded, receiver switch off" else "decoded"
        val channels = outputChannels?.let(::channelLayout)?.let { " $it" } ?: ""
        return HudRow("output", "PCM ($reason)$channels")
    }

    /** What the platform claimed the audio chain takes directly (ysosrs AudioCapabilityReport). */
    internal fun chain(snapshot: AudioChainSnapshot?): HudRow? {
        snapshot ?: return null
        val direct = if (snapshot.direct.isEmpty()) "PCM only" else snapshot.direct.joinToString(" ")
        val surround = snapshot.surroundMode?.let { " · surround $it" } ?: ""
        val pcm = snapshot.maxPcmChannels?.let(::channelLayout)?.let { " · PCM $it" } ?: ""
        return HudRow("chain", direct + surround + pcm)
    }

    internal fun formatBitrate(bps: Int): String = when {
        bps >= 1_000_000 -> String.format(Locale.US, "%.1f Mb/s", bps / 1_000_000.0)
        else -> "${bps / 1000} kb/s"
    }

    /** Required stream bitrate against the player's bandwidth estimate. */
    internal fun bitrateNeed(requiredBps: Long?, availableBps: Long?): HudRow? {
        val need = requiredBps?.takeIf { it > 0L } ?: return null
        val have = availableBps?.takeIf { it > 0L } ?: return null
        return HudRow(
            "need",
            String.format(Locale.US, "%.1f / %.1f Mbps", need / 1_000_000.0, have / 1_000_000.0),
            warn = have < need * HEADROOM,
        )
    }

    internal fun connections(connections: Int?, chunkBytes: Long?): HudRow? {
        val count = connections?.takeIf { it > 0 } ?: return null
        val chunk = chunkBytes?.takeIf { it > 0L }?.let { " x ${formatBytes(it)}" } ?: ""
        return HudRow("conn", "$count$chunk")
    }

    internal fun videoCodecName(mime: String?, codecs: String?): String? = when {
        dolbyVisionProfile(mime, codecs) != null -> "DV"
        mime == MimeTypes.VIDEO_H265 -> "HEVC"
        mime == MimeTypes.VIDEO_H264 -> "H.264"
        mime == MimeTypes.VIDEO_AV1 -> "AV1"
        mime == MimeTypes.VIDEO_VP9 -> "VP9"
        mime == MimeTypes.VIDEO_MPEG2 -> "MPEG-2"
        mime.isNullOrBlank() -> null
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }

    /** Dolby Vision profile from `dvhe.08.06` style codecs; 0 when DV but the profile is unknown. */
    internal fun dolbyVisionProfile(mime: String?, codecs: String?): Int? {
        val dvCodec = codecs?.split(',')?.map { it.trim() }
            ?.firstOrNull { it.startsWith("dvh1") || it.startsWith("dvhe") || it.startsWith("dav1") || it.startsWith("dvav") }
        if (dvCodec == null && mime != MimeTypes.VIDEO_DOLBY_VISION) return null
        return dvCodec?.split('.')?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    internal fun audioCodecName(mime: String?): String? = when (mime) {
        null, "" -> null
        MimeTypes.AUDIO_AC3 -> "AC3"
        MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
        MimeTypes.AUDIO_AC4 -> "AC4"
        MimeTypes.AUDIO_TRUEHD -> "TrueHD"
        MimeTypes.AUDIO_DTS -> "DTS"
        MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
        MimeTypes.AUDIO_DTS_EXPRESS -> "DTS Express"
        MimeTypes.AUDIO_DTS_X -> "DTS-UHD"
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_MPEG -> "MP3"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        MimeTypes.AUDIO_RAW -> "PCM"
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }

    internal fun channelLayout(channels: Int): String? = when {
        channels <= 0 -> null
        channels == 1 -> "mono"
        channels == 2 -> "stereo"
        channels == 6 -> "5.1"
        channels == 8 -> "7.1"
        else -> "${channels}ch"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= MIB -> "${bytes / MIB} MB"
        bytes >= KIB -> "${bytes / KIB} KB"
        else -> "$bytes B"
    }

    const val OFFICIAL_STRATEGY = "official"
    private const val CADENCE_TOLERANCE = 0.01f
    private const val HEADROOM = 1.2
    private const val DRIFT_WARN_MS = 20L
    private const val KIB = 1024L
    private const val MIB = 1024L * 1024L
}

/**
 * Audio-clock drift at HUD cadence. While playing at a steady speed, the player position follows
 * the audio clock, so its deviation from elapsed wall time is the clock drift per second.
 */
class ClockDriftMeter {
    private var lastPositionMs = -1L
    private var lastWallMs = -1L

    /** Returns |position delta - wall delta x speed| per second, or null when not measurable. */
    fun update(positionMs: Long, wallMs: Long, speed: Float, playing: Boolean): Long? {
        val previousPosition = lastPositionMs
        val previousWall = lastWallMs
        lastPositionMs = positionMs
        lastWallMs = wallMs
        if (!playing || speed <= 0f || previousPosition < 0L || previousWall < 0L) return null
        val wallDelta = wallMs - previousWall
        if (wallDelta !in MIN_WINDOW_MS..MAX_WINDOW_MS) return null
        val drift = abs((positionMs - previousPosition) - wallDelta * speed.toDouble())
        if (drift > MAX_PLAUSIBLE_MS) return null
        return (drift * 1000.0 / wallDelta).roundToInt().toLong()
    }

    fun reset() {
        lastPositionMs = -1L
        lastWallMs = -1L
    }

    private companion object {
        const val MIN_WINDOW_MS = 500L
        const val MAX_WINDOW_MS = 3_000L
        // A larger jump is a seek or discontinuity, not drift (ysosrs JITTER_MAX_PLAUSIBLE_MS).
        const val MAX_PLAUSIBLE_MS = 500.0
    }
}
