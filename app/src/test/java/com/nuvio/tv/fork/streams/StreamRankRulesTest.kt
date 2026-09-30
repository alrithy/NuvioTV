package com.nuvio.tv.fork.streams

import com.nuvio.tv.fork.diagnostics.AddonHealthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRankRulesTest {

    private fun input(
        cacheTier: Int = 0,
        resolution: Int = 1080,
        quality: Int = 2,
        group: Int = TrashReleaseGroups.UNKNOWN_TIER,
        visual: Int = 4,
        audio: Int = 12,
        channels: Int = 4,
        encode: Int = 5,
        size: Long? = null,
        reliability: Int = 1,
    ) = StreamRankInput(cacheTier, resolution, quality, group, visual, audio, channels, encode, size, reliability)

    private fun rankNames(vararg items: Pair<String, StreamRankInput>): List<String> =
        StreamRankRules.rank(items.toList()) { it.second }.map { it.first }

    @Test
    fun cachedStreamsComeFirstAndUncachedStayVisible() {
        val order = rankNames(
            "uncached-4k" to input(cacheTier = 3, resolution = 2160),
            "cached-1080" to input(cacheTier = 0, resolution = 1080),
            "checking" to input(cacheTier = 2, resolution = 2160),
        )
        assertEquals(listOf("cached-1080", "checking", "uncached-4k"), order)
    }

    @Test
    fun resolutionThenQualityThenReleaseGroup() {
        val order = rankNames(
            "1080-remux" to input(resolution = 1080, quality = 0),
            "2160-webdl" to input(resolution = 2160, quality = 2),
            "2160-remux-unknown-group" to input(resolution = 2160, quality = 0),
            "2160-remux-tier1" to input(resolution = 2160, quality = 0, group = 0),
        )
        assertEquals(listOf("2160-remux-tier1", "2160-remux-unknown-group", "2160-webdl", "1080-remux"), order)
    }

    @Test
    fun visualAudioChannelsCodecSizeAndReliabilityBreakTiesInThatOrder() {
        assertEquals(listOf("dv", "hdr"), rankNames("hdr" to input(visual = 3), "dv" to input(visual = 0)))
        assertEquals(listOf("truehd", "ddp"), rankNames("ddp" to input(audio = 6), "truehd" to input(audio = 0)))
        assertEquals(listOf("7.1", "5.1"), rankNames("5.1" to input(channels = 2), "7.1" to input(channels = 0)))
        assertEquals(listOf("hevc", "av1"), rankNames("av1" to input(encode = 2), "hevc" to input(encode = 0)))
        assertEquals(
            listOf("40gb", "20gb", "unknown-size"),
            rankNames("unknown-size" to input(size = null), "20gb" to input(size = 20L shl 30), "40gb" to input(size = 40L shl 30)),
        )
        assertEquals(listOf("healthy", "failing"), rankNames("failing" to input(reliability = 3), "healthy" to input(reliability = 0)))
        // An earlier key always wins over a later one.
        assertEquals(listOf("hdr-failing", "sdr-healthy"), rankNames("sdr-healthy" to input(visual = 4, reliability = 0), "hdr-failing" to input(visual = 3, reliability = 3)))
    }

    @Test
    fun tiesKeepTheIncomingAddonOrder() {
        val items = (1..20).map { "s$it" to input() }
        assertEquals(items.map { it.first }, StreamRankRules.rank(items) { it.second }.map { it.first })
    }

    @Test
    fun rankingIsDeterministicAndDropsNothing() {
        val items = listOf(
            "a" to input(resolution = 720), "b" to input(resolution = 2160, cacheTier = 3), "c" to input(quality = 0),
            "d" to input(), "e" to input(resolution = 0, quality = 12),
        )
        val first = StreamRankRules.rank(items) { it.second }
        val again = StreamRankRules.rank(items.reversed()) { it.second }
        assertEquals(items.size, first.size)
        assertEquals(items.toSet(), first.toSet())
        assertEquals("no two inputs tie, so the input order does not matter", first, again)
        assertEquals(listOf("c", "d", "a", "e", "b"), first.map { it.first })
    }

    @Test
    fun dolbyVisionRanksFirstOnlyOnADvDisplay() {
        val dvWithHdr = StreamRankRules.visual(dv = true, hdr = true, hdr10Plus = false, displaySupportsDv = true)
        val dvOnly = StreamRankRules.visual(dv = true, hdr = false, hdr10Plus = false, displaySupportsDv = true)
        val hdr10 = StreamRankRules.visual(dv = false, hdr = true, hdr10Plus = false, displaySupportsDv = true)
        assertTrue(dvWithHdr < dvOnly && dvOnly < hdr10)

        val sdr = StreamRankRules.visual(dv = false, hdr = false, hdr10Plus = false, displaySupportsDv = false)
        val dvOnlyNoDv = StreamRankRules.visual(dv = true, hdr = false, hdr10Plus = false, displaySupportsDv = false)
        val dvWithHdrNoDv = StreamRankRules.visual(dv = true, hdr = true, hdr10Plus = false, displaySupportsDv = false)
        val hdr10NoDv = StreamRankRules.visual(dv = false, hdr = true, hdr10Plus = false, displaySupportsDv = false)
        val hdr10PlusNoDv = StreamRankRules.visual(dv = false, hdr = true, hdr10Plus = true, displaySupportsDv = false)
        assertEquals("a DV release with an HDR base layer plays as HDR", hdr10NoDv, dvWithHdrNoDv)
        assertTrue(hdr10PlusNoDv < hdr10NoDv && hdr10NoDv < sdr && sdr < dvOnlyNoDv)
    }

    @Test
    fun reliabilityOrdersHealthyUnknownSlowFailing() {
        val healthy = StreamRankRules.reliability(AddonHealthState.HEALTHY)
        val unknown = StreamRankRules.reliability(null)
        val slow = StreamRankRules.reliability(AddonHealthState.SLOW)
        val failing = StreamRankRules.reliability(AddonHealthState.TIMEOUT)
        assertTrue(healthy < unknown && unknown < slow && slow < failing)
        assertEquals(unknown, StreamRankRules.reliability(AddonHealthState.UNKNOWN))
    }

    @Test
    fun releaseGroupTiersAreCaseInsensitiveWithLowQualityLast() {
        assertEquals(0, TrashReleaseGroups.tier("framestor"))
        assertEquals(0, TrashReleaseGroups.tier(" FraMeSToR "))
        assertTrue(TrashReleaseGroups.tier("FLUX") > TrashReleaseGroups.tier("CtrlHD"))
        assertEquals(TrashReleaseGroups.UNKNOWN_TIER, TrashReleaseGroups.tier("SomeNewGroup"))
        assertEquals(TrashReleaseGroups.UNKNOWN_TIER, TrashReleaseGroups.tier(""))
        assertEquals(TrashReleaseGroups.UNKNOWN_TIER, TrashReleaseGroups.tier(null))
        assertEquals(TrashReleaseGroups.LOW_QUALITY_TIER, TrashReleaseGroups.tier("yify"))
        assertEquals("a file extension from the parser is ignored", 0, TrashReleaseGroups.tier("framestor.mkv"))
        assertEquals("dotted group names still match", TrashReleaseGroups.LOW_QUALITY_TIER, TrashReleaseGroups.tier("YTS.MX"))
        assertEquals("Flights stays preferred (WEB Tier 02)", 10, TrashReleaseGroups.tier("Flights"))
    }
}
