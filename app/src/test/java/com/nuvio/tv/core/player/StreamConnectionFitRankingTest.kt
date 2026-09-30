package com.nuvio.tv.core.player

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.domain.model.StreamDebridCacheStatus
import com.nuvio.tv.fork.streams.StreamRankContext
import com.nuvio.tv.fork.streams.StreamRanker
import org.junit.Assert.assertEquals
import org.junit.Test

/** Superfork G8c: connection fit through the ranker, on official size facts. */
class StreamConnectionFitRankingTest {

    private val gb = 1_000_000_000L

    private fun cached(title: String, sizeBytes: Long?) = Stream(
        name = "AddonA", title = title, description = null, url = "https://cdn.example/${title.hashCode()}.mkv", ytId = null,
        infoHash = null, fileIdx = null, externalUrl = null, behaviorHints = null, addonName = "AddonA", addonLogo = null,
        debridCacheStatus = StreamDebridCacheStatus("rd", "Real-Debrid", StreamDebridCacheState.CACHED, cachedSize = sizeBytes),
    )

    private val remux4k = cached("Movie 2020 2160p BluRay REMUX HEVC-FraMeSToR.mkv", 60 * gb) // ~67 Mbps over 2 h
    private val web4k = cached("Movie 2020 2160p WEB-DL HEVC-FLUX.mkv", 15 * gb)             // ~17 Mbps
    private val unknownSize = cached("Movie 2020 2160p BluRay REMUX HEVC-BLURANiUM.mkv", null)

    @Test
    fun `a stream the connection cannot sustain drops below ones it can`() {
        val slow = StreamRankContext(connectionMbps = 40.0, runtimeMinutes = 120)
        assertEquals(listOf(unknownSize, web4k, remux4k), StreamRanker.rank(listOf(remux4k, web4k, unknownSize), slow))
    }

    @Test
    fun `without an estimate or runtime the order is unchanged`() {
        val quality = StreamRanker.rank(listOf(web4k, remux4k, unknownSize))
        assertEquals(listOf(remux4k, unknownSize, web4k), quality)
        assertEquals(quality, StreamRanker.rank(listOf(web4k, remux4k, unknownSize), StreamRankContext(connectionMbps = 40.0)))
        assertEquals(quality, StreamRanker.rank(listOf(web4k, remux4k, unknownSize), StreamRankContext(runtimeMinutes = 120)))
        assertEquals(quality, StreamRanker.rank(listOf(web4k, remux4k, unknownSize), StreamRankContext(connectionMbps = 500.0, runtimeMinutes = 120)))
    }
}
