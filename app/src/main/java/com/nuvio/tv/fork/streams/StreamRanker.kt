package com.nuvio.tv.fork.streams

import com.nuvio.tv.core.debrid.DirectDebridStreamFilter
import com.nuvio.tv.domain.model.DebridSettings
import com.nuvio.tv.domain.model.DebridStreamAudioChannel
import com.nuvio.tv.domain.model.DebridStreamAudioTag
import com.nuvio.tv.domain.model.DebridStreamEncode
import com.nuvio.tv.domain.model.DebridStreamQuality
import com.nuvio.tv.domain.model.DebridStreamVisualTag
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.fork.diagnostics.AddonHealthState

/** Per-load inputs the ranker cannot read from a stream (G8b). Defaults change nothing. */
data class StreamRankContext(
    /** False only when the display is known not to render Dolby Vision (feature 162). */
    val displaySupportsDv: Boolean = true,
    /** Official add-on display name → G1 health (feature 166). */
    val addonHealth: Map<String, AddonHealthState> = emptyMap(),
    /** G8c (165): learned sustained throughput on the current network; null while still learning. */
    val connectionMbps: Double? = null,
    /** Runtime of the title, for average bitrate; null leaves connection fit off. */
    val runtimeMinutes: Int? = null,
    /** False only when the device is known to have no hardware AV1 decoder (G14 device finding). */
    val decodesAv1: Boolean = true,
) {
    companion object {
        val NONE = StreamRankContext()
    }
}

/**
 * The one stream ranker (D053) behind the "Best quality" autoplay mode and list order. Reads the
 * **official** `DirectDebridStreamFilter` facts (REUSE; no second parser) and orders them with
 * [StreamRankRules]. It uses fixed orders rather than the user's Direct Debrid sort criteria, which
 * keep driving the official Direct Debrid list: a size-ascending list sort must not make autoplay
 * pick the smallest file (Cxsmo `StreamQualityRank` rationale).
 */
object StreamRanker {

    private val FACTS_SETTINGS = DebridSettings()

    /** Lossless first (feature 163); Atmos sits with the lossless formats it usually rides on. */
    private val AUDIO_ORDER = listOf(
        DebridStreamAudioTag.TRUEHD, DebridStreamAudioTag.DTS_HD_MA, DebridStreamAudioTag.FLAC,
        DebridStreamAudioTag.DTS_X, DebridStreamAudioTag.ATMOS, DebridStreamAudioTag.DTS_HD,
        DebridStreamAudioTag.DD_PLUS, DebridStreamAudioTag.DTS_ES, DebridStreamAudioTag.DTS,
        DebridStreamAudioTag.DD, DebridStreamAudioTag.OPUS, DebridStreamAudioTag.AAC,
    )

    /** Feature 161: the codecs TVs decode in hardware first (Cxsmo order). */
    private val ENCODE_ORDER = listOf(
        DebridStreamEncode.HEVC, DebridStreamEncode.AVC, DebridStreamEncode.AV1,
        DebridStreamEncode.XVID, DebridStreamEncode.DIVX,
    )

    private val DV_TAGS = setOf(DebridStreamVisualTag.DV, DebridStreamVisualTag.DV_ONLY, DebridStreamVisualTag.HDR_DV)
    private val HDR_TAGS = setOf(
        DebridStreamVisualTag.HDR, DebridStreamVisualTag.HDR10, DebridStreamVisualTag.HDR10_PLUS,
        DebridStreamVisualTag.HLG, DebridStreamVisualTag.HDR_ONLY, DebridStreamVisualTag.HDR_DV,
    )

    fun rank(streams: List<Stream>, context: StreamRankContext = StreamRankContext.NONE): List<Stream> =
        StreamRankRules.rank(streams) { inputFor(it, context) }

    fun best(streams: List<Stream>, context: StreamRankContext = StreamRankContext.NONE): Stream? =
        rank(streams, context).firstOrNull()

    /**
     * One stream load: official fact extraction is regex-heavy, and a load re-emits the growing
     * list with every add-on answer, so each stream's input is computed once per load.
     */
    class Session(private val context: StreamRankContext) {
        // Read from the main thread and the badge job; a duplicate computation is harmless.
        private val inputs = java.util.concurrent.ConcurrentHashMap<String, StreamRankInput>()

        fun rank(streams: List<Stream>): List<Stream> =
            StreamRankRules.rank(streams) { stream -> inputs.getOrPut(stream.rankKey()) { inputFor(stream, context) } }
    }

    private fun Stream.rankKey(): String = listOf(
        addonName, url, externalUrl, infoHash, fileIdx, name, title, description?.hashCode(),
        clientResolve?.filename, debridCacheStatus?.state,
    ).joinToString("|")

    fun inputFor(stream: Stream, context: StreamRankContext): StreamRankInput {
        val facts = DirectDebridStreamFilter.facts(stream, FACTS_SETTINGS)
        return StreamRankInput(
            cacheTier = cacheTier(stream),
            resolution = facts.resolution.value,
            quality = orderIndex(facts.quality, DebridStreamQuality.defaultOrder),
            releaseGroupTier = TrashReleaseGroups.tier(facts.releaseGroup),
            visual = StreamRankRules.visual(
                dv = facts.visualTags.any { it in DV_TAGS },
                hdr = facts.visualTags.any { it in HDR_TAGS },
                hdr10Plus = DebridStreamVisualTag.HDR10_PLUS in facts.visualTags,
                displaySupportsDv = context.displaySupportsDv,
            ),
            audio = facts.audioTags.minOf { orderIndex(it, AUDIO_ORDER) },
            channels = facts.audioChannels.minOf { orderIndex(it, DebridStreamAudioChannel.defaultOrder) },
            encode = orderIndex(facts.encode, ENCODE_ORDER),
            sizeBytes = facts.size,
            reliability = StreamRankRules.reliability(context.addonHealth[stream.addonName]),
            connection = ConnectionFitRules.connectionTier(facts.size, context.runtimeMinutes, context.connectionMbps),
            compatibility = StreamRankRules.compatibility(
                dvOnly = facts.visualTags.any { it in DV_TAGS } && facts.visualTags.none { it in HDR_TAGS },
                displaySupportsDv = context.displaySupportsDv,
                av1 = facts.encode == DebridStreamEncode.AV1,
                decodesAv1 = context.decodesAv1,
            ),
        )
    }

    /** Feature 156/157: cached first, uncached kept below, links that open elsewhere last. */
    private fun cacheTier(stream: Stream): Int {
        if (stream.isExternal()) return 4
        return when (stream.debridCacheStatus?.state) {
            StreamDebridCacheState.NOT_CACHED -> 3
            StreamDebridCacheState.CHECKING, StreamDebridCacheState.UNKNOWN -> 2
            StreamDebridCacheState.CACHED -> 0
            null -> if (stream.isTorrent()) 1 else 0
        }
    }

    private fun <T> orderIndex(value: T, order: List<T>): Int =
        order.indexOf(value).let { if (it >= 0) it else order.size }
}
