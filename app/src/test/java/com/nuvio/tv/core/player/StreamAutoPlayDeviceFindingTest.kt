package com.nuvio.tv.core.player

import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.data.local.StreamAutoPlaySource
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.domain.model.StreamDebridCacheStatus
import com.nuvio.tv.fork.streams.StreamRankContext
import com.nuvio.tv.fork.streams.StreamRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G14 device finding (D067): the Best-quality list and autoplay must pick the same best stream, and
 * neither may pick a file the TV cannot show as made. A realistic source list for one film.
 */
class StreamAutoPlayDeviceFindingTest {

    private fun stream(
        title: String,
        addonName: String = "Torrentio",
        cache: StreamDebridCacheState? = StreamDebridCacheState.CACHED,
        bingeGroup: String? = null,
    ) = Stream(
        name = addonName, title = title, description = null, url = null, ytId = null,
        infoHash = title.hashCode().toUInt().toString(16).padStart(40, '0'), fileIdx = null, externalUrl = null,
        behaviorHints = StreamBehaviorHints(notWebReady = null, bingeGroup = bingeGroup, countryWhitelist = null, proxyHeaders = null),
        addonName = addonName, addonLogo = null,
        debridCacheStatus = cache?.let { StreamDebridCacheStatus("rd", "Real-Debrid", it) },
    )

    private fun autoplay(streams: List<Stream>, context: StreamRankContext, bingeGroup: String? = null) =
        StreamAutoPlaySelector.selectAutoPlayStream(
            streams = streams,
            mode = StreamAutoPlayMode.BEST_QUALITY,
            regexPattern = "",
            source = StreamAutoPlaySource.ALL_SOURCES,
            installedAddonNames = setOf("Torrentio", "Comet"),
            selectedAddons = emptySet(),
            selectedPlugins = emptySet(),
            preferredBingeGroup = bingeGroup,
            preferBingeGroupInSelection = bingeGroup != null,
            rankContext = context,
        )

    private val remux4k = stream("Film 2023 2160p UHD BluRay REMUX DV HDR10 HEVC TrueHD Atmos 7.1-FraMeSToR 💾 62.1 GB")
    private val webDl4k = stream("Film 2023 2160p WEB-DL DDP5.1 Atmos HDR10 HEVC-FLUX 💾 81.4 GB")
    private val bluray1080 = stream("Film 2023 1080p BluRay DTS-HD MA 5.1 x264-CtrlHD 💾 18.2 GB")
    private val uncachedRemux4k = stream("Film 2023 2160p UHD BluRay REMUX HDR10 HEVC TrueHD 7.1-SURCODE 💾 70.0 GB", cache = StreamDebridCacheState.NOT_CACHED)
    private val dvOnlyRemux4k = stream("Film 2023 2160p UHD BluRay REMUX DV HEVC TrueHD Atmos 7.1-DVONLY 💾 64.0 GB")
    private val av1Web4k = stream("Film 2023 2160p WEB-DL DDP5.1 HDR10 AV1-AV1GRP 💾 9.0 GB")

    /** Add-on order as a provider might send it: the best files are not first. */
    private val sources = listOf(bluray1080, uncachedRemux4k, dvOnlyRemux4k, webDl4k, av1Web4k, remux4k)

    private val tclC6k = StreamRankContext(displaySupportsDv = true, decodesAv1 = true)
    private val noDvTv = StreamRankContext(displaySupportsDv = false, decodesAv1 = false)

    @Test
    fun `the ranked list and autoplay pick the same stream for any add-on order`() {
        listOf(sources, sources.reversed(), sources.shuffled(java.util.Random(7))).forEach { order ->
            for (context in listOf(tclC6k, noDvTv)) {
                val list = StreamRanker.Session(context).rank(order)
                assertEquals(list.first(), autoplay(order, context))
                assertEquals(remux4k, list.first())
            }
        }
    }

    @Test
    fun `cached REMUX beats a larger WEB-DL and the 1080p, uncached stays below every cached stream`() {
        val list = StreamRanker.rank(sources, tclC6k)
        assertEquals(remux4k, list.first())
        assertTrue(list.indexOf(remux4k) < list.indexOf(webDl4k))
        assertTrue(list.indexOf(webDl4k) < list.indexOf(bluray1080))
        assertEquals(uncachedRemux4k, list.last())
    }

    @Test
    fun `a Dolby Vision only file is never the pick on a TV without Dolby Vision`() {
        val list = StreamRanker.rank(sources, noDvTv)
        assertEquals(remux4k, autoplay(sources, noDvTv))
        // Below every cached file the TV shows as made, even the 1080p; above the uncached one.
        assertTrue(list.indexOf(dvOnlyRemux4k) > list.indexOf(bluray1080))
        assertTrue(list.indexOf(dvOnlyRemux4k) < list.indexOf(uncachedRemux4k))
        assertEquals(bluray1080, autoplay(listOf(dvOnlyRemux4k, bluray1080), noDvTv))
        // On the TCL C6K (Dolby Vision) the same file is a normal 4K REMUX.
        assertEquals(dvOnlyRemux4k, autoplay(listOf(dvOnlyRemux4k, bluray1080), tclC6k))
    }

    @Test
    fun `AV1 drops only where the TV cannot decode it`() {
        assertEquals(av1Web4k, autoplay(listOf(bluray1080, av1Web4k), tclC6k))
        assertEquals(bluray1080, autoplay(listOf(bluray1080, av1Web4k), noDvTv))
    }

    @Test
    fun `the next episode reuses the binge group with its best stream, not its first`() {
        val group = "torrentio|2160p|BluRay REMUX"
        val worse = stream("Show S01E02 2160p BluRay REMUX HEVC DTS-HD MA 5.1-OTHER", bingeGroup = group)
        val better = stream("Show S01E02 2160p BluRay REMUX DV HDR10 HEVC TrueHD Atmos 7.1-FraMeSToR", bingeGroup = group)
        val outside = stream("Show S01E02 2160p UHD BluRay REMUX DV HDR10 HEVC TrueHD Atmos 7.1-FraMeSToR", addonName = "Comet")
        assertEquals(better, autoplay(listOf(worse, outside, better), tclC6k, bingeGroup = group))
        // First stream mode keeps official behaviour: the group's first stream.
        assertEquals(
            worse,
            StreamAutoPlaySelector.selectAutoPlayStream(
                streams = listOf(worse, better), mode = StreamAutoPlayMode.FIRST_STREAM, regexPattern = "",
                source = StreamAutoPlaySource.ALL_SOURCES, installedAddonNames = setOf("Torrentio"),
                selectedAddons = emptySet(), selectedPlugins = emptySet(), preferredBingeGroup = group,
                preferBingeGroupInSelection = true,
            ),
        )
    }
}
