package com.nuvio.tv.fork.audio

import com.nuvio.tv.fork.audio.LosslessAudioDefault.TIER_DTS_HD_MA
import com.nuvio.tv.fork.audio.LosslessAudioDefault.TIER_FLAC
import com.nuvio.tv.fork.audio.LosslessAudioDefault.TIER_PCM
import com.nuvio.tv.fork.audio.LosslessAudioDefault.TIER_TRUEHD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LosslessAudioDefaultTest {

    private val sameLanguage: (String?, String) -> Boolean = { track, wanted -> track == wanted }

    private fun t(index: Int, codec: String?, name: String? = null, lang: String? = "en", channels: Int? = 6) =
        AudioTrackCandidate(index, codec, name, lang, channels)

    @Test
    fun onlyProvablyLosslessTracksAreRanked() {
        assertEquals(TIER_TRUEHD, LosslessAudioDefault.losslessTier("TrueHD", null))
        assertEquals(TIER_TRUEHD, LosslessAudioDefault.losslessTier("mlp", null))
        assertEquals(TIER_DTS_HD_MA, LosslessAudioDefault.losslessTier("DTS-HD", null))
        assertEquals(TIER_DTS_HD_MA, LosslessAudioDefault.losslessTier("dts", "English DTS-HD MA 7.1"))
        assertEquals(TIER_FLAC, LosslessAudioDefault.losslessTier("FLAC", null))
        assertEquals(TIER_PCM, LosslessAudioDefault.losslessTier("pcm_s24le", null))
        // mpv plain dts without an MA hint, and lossy codecs, are never promoted.
        listOf("dts" to "English 5.1", "E-AC-3" to null, "AC3" to null, "aac" to null).forEach { (c, n) ->
            assertNull("$c/$n", LosslessAudioDefault.losslessTier(c, n))
        }
    }

    @Test
    fun picksTheHighestTierWithinThePreferredLanguage() {
        val tracks = listOf(t(0, "E-AC-3"), t(1, "DTS-HD"), t(2, "TrueHD", "TrueHD Atmos"))
        assertEquals(2, LosslessAudioDefault.pickDefaultIndex(tracks, listOf("en"), sameLanguage))
    }

    @Test
    fun neverChoosesLosslessOverTheUsersLanguage() {
        val tracks = listOf(t(0, "E-AC-3", lang = "ar"), t(1, "TrueHD", lang = "en"))
        assertNull(LosslessAudioDefault.pickDefaultIndex(tracks, listOf("ar", "en"), sameLanguage))
    }

    @Test
    fun noPreferredLanguageMatchUsesTheWholeSet() {
        val tracks = listOf(t(0, "AC3", lang = "fr"), t(1, "FLAC", lang = "de"))
        assertEquals(1, LosslessAudioDefault.pickDefaultIndex(tracks, listOf("ar"), sameLanguage))
    }

    @Test
    fun commentaryIsNeverTheDefaultAndMoreChannelsWinATie() {
        val tracks = listOf(
            t(0, "TrueHD", "Director's Commentary", channels = 2),
            t(1, "DTS-HD", "DTS-HD MA 5.1", channels = 6),
            t(2, "DTS-HD", "DTS-HD MA 7.1", channels = 8),
        )
        assertEquals(2, LosslessAudioDefault.pickDefaultIndex(tracks, listOf("en"), sameLanguage))
        assertNull(LosslessAudioDefault.pickDefaultIndex(listOf(t(0, "TrueHD", "Commentary")), listOf("en"), sameLanguage))
    }

    @Test
    fun noLosslessTrackKeepsTheEnginePick() {
        assertNull(LosslessAudioDefault.pickDefaultIndex(listOf(t(0, "AC3"), t(1, "aac")), listOf("en"), sameLanguage))
        assertNull(LosslessAudioDefault.pickDefaultIndex(emptyList(), listOf("en"), sameLanguage))
    }
}
