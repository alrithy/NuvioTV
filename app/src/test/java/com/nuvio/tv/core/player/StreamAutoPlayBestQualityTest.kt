package com.nuvio.tv.core.player

import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.data.local.StreamAutoPlaySource
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.domain.model.StreamDebridCacheStatus
import com.nuvio.tv.fork.diagnostics.AddonHealthState
import com.nuvio.tv.fork.streams.StreamRankContext
import com.nuvio.tv.fork.streams.StreamRanker
import org.junit.Assert.assertEquals
import org.junit.Test

/** Superfork G8b: the ranker over official facts, and the Best-quality autoplay mode on top of it. */
class StreamAutoPlayBestQualityTest {

    private fun stream(
        title: String,
        addonName: String = "AddonA",
        url: String? = "https://cdn.example/${title.hashCode()}.mkv",
        infoHash: String? = null,
        cache: StreamDebridCacheState? = null,
    ) = Stream(
        name = addonName, title = title, description = null, url = url, ytId = null,
        infoHash = infoHash, fileIdx = null, externalUrl = null, behaviorHints = null,
        addonName = addonName, addonLogo = null,
        debridCacheStatus = cache?.let { StreamDebridCacheStatus("rd", "Real-Debrid", it) },
    )

    private fun select(
        streams: List<Stream>,
        mode: StreamAutoPlayMode = StreamAutoPlayMode.BEST_QUALITY,
        context: StreamRankContext = StreamRankContext.NONE,
    ) = StreamAutoPlaySelector.selectAutoPlayStream(
        streams = streams,
        mode = mode,
        regexPattern = "",
        source = StreamAutoPlaySource.ALL_SOURCES,
        installedAddonNames = setOf("AddonA", "AddonB"),
        selectedAddons = emptySet(),
        selectedPlugins = emptySet(),
        rankContext = context,
    )

    private val webDl1080 = stream("Movie 2020 1080p WEB-DL DDP5.1 H264-NTG.mkv")
    private val remux2160 = stream("Movie 2020 2160p BluRay REMUX HEVC TrueHD 7.1-FraMeSToR.mkv")
    private val remux2160Unknown = stream("Movie 2020 2160p BluRay REMUX HEVC TrueHD 7.1-SomeGroup.mkv")

    @Test
    fun `best quality picks the ranker's first stream while first stream keeps add-on order`() {
        val streams = listOf(webDl1080, remux2160Unknown, remux2160)
        assertEquals(remux2160, select(streams))
        assertEquals(webDl1080, select(streams, StreamAutoPlayMode.FIRST_STREAM))
    }

    @Test
    fun `autoplay and the ranked list agree`() {
        val streams = listOf(webDl1080, remux2160Unknown, remux2160)
        val ranked = StreamRanker.rank(streams)
        assertEquals(listOf(remux2160, remux2160Unknown, webDl1080), ranked)
        assertEquals(ranked.first(), select(streams))
    }

    @Test
    fun `cached streams win and uncached ones stay in the ranked list`() {
        val uncached4k = stream("Movie 2020 2160p BluRay REMUX HEVC-FraMeSToR.mkv", url = null, infoHash = "abc", cache = StreamDebridCacheState.NOT_CACHED)
        val cached1080 = stream("Movie 2020 1080p BluRay x264-CtrlHD.mkv", cache = StreamDebridCacheState.CACHED)
        assertEquals(cached1080, select(listOf(uncached4k, cached1080)))
        assertEquals(listOf(cached1080, uncached4k), StreamRanker.rank(listOf(uncached4k, cached1080)))
    }

    @Test
    fun `dolby vision only ranks first on a dolby vision display`() {
        val dvOnly = stream("Movie 2020 2160p WEB-DL DV HEVC-FLUX.mkv")
        val hdr10 = stream("Movie 2020 2160p WEB-DL HDR10 HEVC-FLUX.mkv")
        assertEquals(dvOnly, select(listOf(hdr10, dvOnly), context = StreamRankContext(displaySupportsDv = true)))
        assertEquals(hdr10, select(listOf(dvOnly, hdr10), context = StreamRankContext(displaySupportsDv = false)))
    }

    @Test
    fun `source reliability only breaks ties`() {
        val fromA = stream("Movie 2020 1080p WEB-DL H264-NTG.mkv", addonName = "AddonA")
        val fromB = stream("Movie 2020 1080p WEB-DL H264-NTG.mkv", addonName = "AddonB")
        val failingA = StreamRankContext(addonHealth = mapOf("AddonA" to AddonHealthState.TIMEOUT, "AddonB" to AddonHealthState.HEALTHY))
        assertEquals(fromA, select(listOf(fromA, fromB)))
        assertEquals(fromB, select(listOf(fromA, fromB), context = failingA))
        assertEquals(remux2160, select(listOf(remux2160, fromB), context = StreamRankContext(addonHealth = mapOf("AddonA" to AddonHealthState.TIMEOUT))))
    }

    @Test
    fun `a session caches inputs without changing the order`() {
        val streams = listOf(webDl1080, remux2160Unknown, remux2160)
        val session = StreamRanker.Session(StreamRankContext.NONE)
        assertEquals(StreamRanker.rank(streams), session.rank(streams))
        assertEquals(StreamRanker.rank(streams.reversed()), session.rank(streams.reversed()))
    }
}
