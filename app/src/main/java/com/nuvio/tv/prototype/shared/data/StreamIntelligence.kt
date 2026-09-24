package com.nuvio.tv.prototype.shared.data

import androidx.compose.runtime.Immutable
import com.nuvio.tv.prototype.shared.Bi
import kotlin.math.max
import kotlin.math.roundToInt

/** Presentation modes for the stream picker. Every concept visualises the same engine. */
enum class StreamMode(val label: Bi, val blurb: Bi) {
    BEST(Bi("Best Match", "الأنسب"), Bi("The finest picture that still starts instantly on this TV", "أفضل صورة تبدأ فوراً على هذه الشاشة")),
    MAX_QUALITY(Bi("Maximum Quality", "أعلى جودة"), Bi("Uncompromised picture and sound", "صورة وصوت بلا أي تنازل")),
    FASTEST(Bi("Fastest Start", "الأسرع تشغيلاً"), Bi("Cached sources that begin in about a second", "مصادر مخزّنة تبدأ خلال ثانية تقريباً")),
    BALANCED(Bi("Balanced", "متوازن"), Bi("4K quality at a sensible bitrate", "جودة 4K بمعدل بت معتدل")),
    SMALLER(Bi("Smaller File", "حجم أصغر"), Bi("Gentle on slower connections", "مناسب للاتصالات الأبطأ")),
    ALL(Bi("All Sources", "كل المصادر"), Bi("Every result, with raw technical detail", "كل النتائج مع التفاصيل التقنية الكاملة")),
}

@Immutable
data class RankedStream(
    val stream: ProtoStream,
    val score: Int,
    val tier: Bi,
    val reasons: List<Bi>,
)

/** Compact status for the Playback Quality HUD. */
@Immutable
data class PlaybackHud(
    val resolution: String,
    val hdr: List<String>,
    val audio: String,
    val cached: Boolean,
    val service: String,
    val subtitles: Bi?,
    val bitrate: String,
    val sourceKind: String,
    val size: String,
)

object StreamIntelligence {

    private fun baseQuality(s: ProtoStream): Double {
        var q = when (s.resolution) {
            Resolution.R2160 -> 40.0
            Resolution.R1080 -> 26.0
            Resolution.R720 -> 10.0
        }
        q += when {
            s.hasDv -> 14.0
            s.hasHdr -> 9.0
            else -> 0.0
        }
        q += s.audio.rank * 2.2
        q += s.source.rank * 2.0
        return q
    }

    private fun score(s: ProtoStream, mode: StreamMode): Double = when (mode) {
        StreamMode.BEST -> {
            var v = baseQuality(s)
            v += if (s.isCached) 22.0 else -40.0
            if (s.hasArabicSubs) v += 5.0
            if (s.startSeconds > 5) v -= s.startSeconds / 10.0
            v
        }
        StreamMode.MAX_QUALITY -> baseQuality(s) * 1.2 + s.bitrateMbps * 0.25 + (if (s.isCached) 3.0 else 0.0)
        StreamMode.FASTEST -> (if (s.isCached) 100.0 else 0.0) - s.startSeconds * 12.0 + baseQuality(s) * 0.15
        StreamMode.BALANCED -> baseQuality(s) + (if (s.isCached) 20.0 else -40.0) - max(0.0, s.sizeGb - 26.0) * 1.3
        StreamMode.SMALLER -> (if (s.isCached) 60.0 else 0.0) - s.sizeGb * 1.6 + (if (s.resolution != Resolution.R720) 8.0 else 0.0)
        StreamMode.ALL -> 0.0
    }

    fun rank(streams: List<ProtoStream>, mode: StreamMode): List<RankedStream> {
        val ordered = if (mode == StreamMode.ALL) {
            streams.sortedWith(compareBy<ProtoStream> { it.addon }.thenByDescending { it.seeders ?: 9999 })
        } else {
            streams.sortedByDescending { score(it, mode) }
        }
        val bestPossible = streams.maxOf { score(it, StreamMode.BEST) }
        return ordered.map { s ->
            val raw = score(s, StreamMode.BEST)
            val pct = ((raw / bestPossible) * 100).roundToInt().coerceIn(12, 99)
            RankedStream(s, pct, tier(s), reasons(s))
        }
    }

    fun best(streams: List<ProtoStream>, mode: StreamMode = StreamMode.BEST): RankedStream = rank(streams, mode).first()

    fun tier(s: ProtoStream): Bi = when {
        s.resolution == Resolution.R2160 && s.source == SourceKind.REMUX -> Bi("Reference", "مرجعية")
        s.resolution == Resolution.R2160 -> Bi("Excellent", "ممتازة")
        s.resolution == Resolution.R1080 && s.source.rank >= SourceKind.BLURAY.rank -> Bi("Great", "رائعة")
        s.resolution == Resolution.R1080 -> Bi("Good", "جيدة")
        else -> Bi("Basic", "أساسية")
    }

    fun reasons(s: ProtoStream): List<Bi> = buildList {
        when (s.cache) {
            CacheState.CACHED -> add(Bi("${s.service} cached", "مخزّن على ${s.service}"))
            CacheState.DIRECT -> add(Bi("Direct stream", "بث مباشر"))
            CacheState.UNCACHED -> add(Bi("Not cached · ${s.seeders ?: 0} peers", "غير مخزّن · ${s.seeders ?: 0} مصدر"))
        }
        if (s.hasArabicSubs) add(Bi("Arabic subtitles available", "ترجمة عربية متوفرة"))
        if (s.hasArabicAudio) add(Bi("Arabic audio", "صوت عربي"))
        if (s.hasDv) add(Bi("Dolby Vision on this TV", "دولبي فيجن على هذه الشاشة"))
        if (s.startSeconds <= 2.0) add(Bi("Starts instantly", "يبدأ فوراً"))
    }

    /** "4K · Dolby Vision · TrueHD Atmos 7.1" */
    fun headline(s: ProtoStream): String = buildList {
        add(if (s.resolution == Resolution.R2160) "4K" else s.resolution.label)
        s.hdr.firstOrNull { it != Hdr.SDR }?.let { add(it.label) }
        add(s.audioLabel)
    }.joinToString(" · ")

    fun hud(s: ProtoStream, subtitle: SubtitleTrack?): PlaybackHud = PlaybackHud(
        resolution = s.resolution.label,
        hdr = s.hdr.filter { it != Hdr.SDR }.map { it.label },
        audio = s.audioLabel,
        cached = s.isCached,
        service = s.service,
        subtitles = subtitle?.language,
        bitrate = s.bitrateLabel,
        sourceKind = s.source.label,
        size = s.sizeLabel,
    )

    // --- Mock stream generation -------------------------------------------------------------------

    private data class Template(
        val res: Resolution, val src: SourceKind, val hdr: List<Hdr>, val codec: String, val audio: AudioFormat,
        val ch: String, val gbPer100: Double, val mbps: Double, val cache: CacheState, val seeders: Int?,
        val addon: String, val service: String, val group: String, val start: Double,
        val audioLangs: List<String>, val subs: List<String>, val tag: String,
    )

    private val templates = listOf(
        Template(Resolution.R2160, SourceKind.REMUX, listOf(Hdr.DV, Hdr.HDR10), "HEVC", AudioFormat.TRUEHD_ATMOS, "7.1", 47.0, 64.0, CacheState.CACHED, 214, "Torrentio", "Real-Debrid", "FraMeSToR", 3.2, listOf("EN", "FR"), listOf("AR", "EN", "FR", "ES", "TR"), "UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.7.1.Atmos"),
        Template(Resolution.R2160, SourceKind.WEB_DL, listOf(Hdr.DV, Hdr.HDR10), "HEVC", AudioFormat.DDP_ATMOS, "5.1", 14.5, 19.0, CacheState.CACHED, 1320, "Torrentio", "Real-Debrid", "FLUX", 1.6, listOf("EN", "AR"), listOf("AR", "EN", "FR"), "WEB-DL.DDP5.1.Atmos.DV.HDR.H.265"),
        Template(Resolution.R2160, SourceKind.BLURAY, listOf(Hdr.HDR10), "HEVC", AudioFormat.TRUEHD_ATMOS, "7.1", 19.3, 26.0, CacheState.CACHED, 480, "Comet", "TorBox", "SWTYBLZ", 2.4, listOf("EN"), listOf("EN", "AR"), "BluRay.x265.10bit.HDR10.TrueHD.7.1.Atmos"),
        Template(Resolution.R2160, SourceKind.WEB_DL, listOf(Hdr.HDR10P), "HEVC", AudioFormat.DDP, "5.1", 10.8, 15.0, CacheState.UNCACHED, 96, "MediaFusion", "Real-Debrid", "NTb", 95.0, listOf("EN"), listOf("EN"), "WEB-DL.DDP5.1.HDR10Plus.H.265"),
        Template(Resolution.R2160, SourceKind.REMUX, listOf(Hdr.HDR10), "HEVC", AudioFormat.DTSHD_MA, "7.1", 41.0, 58.0, CacheState.UNCACHED, 38, "Torrentio", "Real-Debrid", "EPSiLON", 240.0, listOf("EN"), listOf("EN", "FR"), "UHD.BluRay.REMUX.HDR10.HEVC.DTS-HD.MA.7.1"),
        Template(Resolution.R1080, SourceKind.REMUX, listOf(Hdr.SDR), "AVC", AudioFormat.DTSHD_MA, "7.1", 20.0, 28.0, CacheState.CACHED, 142, "Torrentio", "Real-Debrid", "BLURANiUM", 2.9, listOf("EN"), listOf("EN", "AR", "FR"), "BluRay.REMUX.AVC.DTS-HD.MA.7.1"),
        Template(Resolution.R1080, SourceKind.BLURAY, listOf(Hdr.SDR), "x264", AudioFormat.DTSHD_MA, "7.1", 8.5, 11.0, CacheState.CACHED, 610, "Comet", "TorBox", "DON", 2.0, listOf("EN"), listOf("EN", "AR"), "BluRay.x264.DTS-HD.MA.7.1"),
        Template(Resolution.R1080, SourceKind.WEB_DL, listOf(Hdr.SDR), "H.264", AudioFormat.DDP_ATMOS, "5.1", 4.3, 6.0, CacheState.DIRECT, null, "Nuvio Cloud", "Direct", "NCLD", 1.1, listOf("EN", "AR"), listOf("AR", "EN"), "WEB-DL.DDP5.1.Atmos.H.264"),
        Template(Resolution.R1080, SourceKind.WEB_DL, listOf(Hdr.SDR), "AV1", AudioFormat.AAC, "5.1", 1.9, 2.6, CacheState.CACHED, 880, "Comet", "Real-Debrid", "Silence", 1.4, listOf("EN"), listOf("EN", "AR", "ES"), "WEB-DL.AV1.Opus.5.1"),
        Template(Resolution.R1080, SourceKind.WEBRIP, listOf(Hdr.SDR), "x265", AudioFormat.AAC, "5.1", 1.75, 2.4, CacheState.CACHED, 2400, "Torrentio", "Real-Debrid", "YTS.MX", 0.9, listOf("EN"), listOf("EN"), "WEBRip.x265.10bit.AAC5.1"),
        Template(Resolution.R720, SourceKind.WEBRIP, listOf(Hdr.SDR), "x264", AudioFormat.AAC, "2.0", 0.85, 1.1, CacheState.UNCACHED, 312, "Torrentio", "Real-Debrid", "GalaxyRG", 40.0, listOf("EN"), listOf("EN"), "WEBRip.x264.AAC2.0"),
    )

    private val cache = HashMap<String, List<ProtoStream>>()

    fun streamsFor(title: ProtoTitle, season: Int? = null, episode: Int? = null): List<ProtoStream> {
        val key = "${title.id}:$season:$episode"
        return cache.getOrPut(key) {
            val slug = title.title.en.replace(":", "").replace("'", "").replace(" ", ".")
            val stamp = if (title.isSeries) "S${(season ?: 1).toString().padStart(2, '0')}E${(episode ?: 1).toString().padStart(2, '0')}" else title.year.toString()
            val minutes = if (title.isSeries) title.runtimeMin else title.runtimeMin
            templates.mapIndexed { i, t ->
                val jitter = 1.0 + ((title.seed * (i + 3)) % 9 - 4) / 100.0
                ProtoStream(
                    id = "${title.id}-$i",
                    filename = "$slug.$stamp.${t.res.label}.${t.tag}-${t.group}.mkv",
                    addon = t.addon,
                    service = t.service,
                    cache = t.cache,
                    resolution = t.res,
                    source = t.src,
                    hdr = if (!title.tech.uhd && t.res == Resolution.R2160) listOf(Hdr.SDR) else t.hdr,
                    codec = t.codec,
                    audio = t.audio,
                    channels = t.ch,
                    sizeGb = t.gbPer100 * minutes / 100.0 * jitter,
                    bitrateMbps = t.mbps * jitter,
                    seeders = t.seeders?.let { (it * jitter).roundToInt() },
                    audioLangs = t.audioLangs,
                    subtitleLangs = t.subs,
                    group = t.group,
                    startSeconds = t.start * jitter,
                )
            }.filter { title.tech.uhd || it.resolution != Resolution.R2160 }
        }
    }
}
