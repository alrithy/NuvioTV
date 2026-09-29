package com.nuvio.tv.fork.diagnostics

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHudTest {

    @Test
    fun videoRowShowsCodecSizeAndSourceFps() {
        val row = PlaybackHud.video(PlaybackHudInput(videoMime = MimeTypes.VIDEO_H265, width = 3840, height = 2160, frameRate = 23.976f))
        assertEquals(HudRow("video", "HEVC 3840x2160 23.976 fps"), row)
        assertNull(PlaybackHud.video(PlaybackHudInput()))
    }

    @Test
    fun dolbyVisionProfileIsReadFromCodecs() {
        assertEquals(8, PlaybackHud.dolbyVisionProfile(MimeTypes.VIDEO_H265, "dvhe.08.06"))
        assertEquals(5, PlaybackHud.dolbyVisionProfile(MimeTypes.VIDEO_DOLBY_VISION, "dvh1.05.06"))
        assertEquals(0, PlaybackHud.dolbyVisionProfile(MimeTypes.VIDEO_DOLBY_VISION, null))
        assertNull(PlaybackHud.dolbyVisionProfile(MimeTypes.VIDEO_H265, "hvc1.2.4.L153"))
        assertEquals(HudRow("hdr", "Dolby Vision P8"), PlaybackHud.hdr(PlaybackHudInput(videoMime = MimeTypes.VIDEO_H265, videoCodecs = "dvhe.08.06", colorTransfer = C.COLOR_TRANSFER_ST2084)))
    }

    @Test
    fun hdrFallsBackToTransferFunctionAndNeverGuesses() {
        assertEquals("HDR10 (PQ)", PlaybackHud.hdr(PlaybackHudInput(colorTransfer = C.COLOR_TRANSFER_ST2084))?.value)
        assertEquals("HLG", PlaybackHud.hdr(PlaybackHudInput(colorTransfer = C.COLOR_TRANSFER_HLG))?.value)
        assertEquals("SDR", PlaybackHud.hdr(PlaybackHudInput(colorTransfer = C.COLOR_TRANSFER_SDR))?.value)
        assertNull(PlaybackHud.hdr(PlaybackHudInput(colorTransfer = null)))
    }

    @Test
    fun displayWarnsOnlyWhenRefreshIsNotAMultipleOfFps() {
        assertFalse(PlaybackHud.display(PlaybackHudInput(displayRefreshHz = 23.976f, frameRate = 23.976f))!!.warn)
        assertFalse(PlaybackHud.display(PlaybackHudInput(displayRefreshHz = 50f, frameRate = 25f))!!.warn)
        assertTrue(PlaybackHud.display(PlaybackHudInput(displayRefreshHz = 60f, frameRate = 23.976f))!!.warn)
        assertFalse(PlaybackHud.display(PlaybackHudInput(displayRefreshHz = 60f))!!.warn)
        assertNull(PlaybackHud.display(PlaybackHudInput()))
    }

    @Test
    fun audioRowMarksObjectAudioOnlyWhenTheStreamSaysSo() {
        assertEquals("E-AC3 Atmos 5.1 48kHz", PlaybackHud.audio(PlaybackHudInput(audioMime = MimeTypes.AUDIO_E_AC3_JOC, audioChannels = 6, audioSampleRate = 48_000))?.value)
        assertEquals("E-AC3 Atmos 7.1", PlaybackHud.audio(PlaybackHudInput(audioMime = MimeTypes.AUDIO_E_AC3, audioCodecs = "ec+3", audioChannels = 8))?.value)
        assertEquals("DTS-UHD DTS:X", PlaybackHud.audio(PlaybackHudInput(audioMime = MimeTypes.AUDIO_DTS_X))?.value)
        // TrueHD Atmos is not signalled in the format, so no Atmos claim is made.
        assertEquals("TrueHD 7.1", PlaybackHud.audio(PlaybackHudInput(audioMime = MimeTypes.AUDIO_TRUEHD, audioChannels = 8))?.value)
        assertNull(PlaybackHud.audio(PlaybackHudInput()))
    }

    @Test
    fun outputDistinguishesPassthroughFromDecodedPcm() {
        assertEquals("passthrough E-AC3 Atmos", PlaybackHud.output(C.ENCODING_E_AC3_JOC)?.value)
        assertEquals("passthrough TrueHD", PlaybackHud.output(C.ENCODING_DOLBY_TRUEHD)?.value)
        assertEquals("passthrough DTS:X", PlaybackHud.output(C.ENCODING_DTS_UHD_P2)?.value)
        assertEquals("PCM (decoded)", PlaybackHud.output(C.ENCODING_PCM_16BIT)?.value)
        assertEquals("PCM (decoded)", PlaybackHud.output(C.ENCODING_PCM_FLOAT)?.value)
        assertNull(PlaybackHud.output(null))
    }

    @Test
    fun requiredBitrateWarnsWithoutTwentyPercentHeadroom() {
        assertEquals(HudRow("need", "40.0 / 100.0 Mbps", warn = false), PlaybackHud.bitrateNeed(40_000_000, 100_000_000))
        assertTrue(PlaybackHud.bitrateNeed(40_000_000, 45_000_000)!!.warn)
        assertNull(PlaybackHud.bitrateNeed(40_000_000, null))
        assertNull(PlaybackHud.bitrateNeed(null, 100_000_000))
    }

    @Test
    fun connectionsShowCountAndChunk() {
        assertEquals("4 x 8 MB", PlaybackHud.connections(4, 8L * 1024 * 1024)?.value)
        assertEquals("2 x 512 KB", PlaybackHud.connections(2, 512L * 1024)?.value)
        assertNull(PlaybackHud.connections(0, 1024))
    }

    @Test
    fun rowsAlwaysIncludeCountersAndStrategyAndDropUnavailableValues() {
        val rows = PlaybackHud.rows(PlaybackHudInput(rebuffers = 2, audioUnderruns = 0))
        assertEquals(listOf("rebuffer", "underrun", "strategy"), rows.map { it.label })
        assertTrue(rows.first { it.label == "rebuffer" }.warn)
        assertEquals(PlaybackHud.OFFICIAL_STRATEGY, rows.last().value)
        assertTrue(PlaybackHud.rows(PlaybackHudInput(loadErrors = 3)).any { it.label == "load err" && it.warn })
    }

    @Test
    fun clockDriftNeedsTwoSteadySamples() {
        val meter = ClockDriftMeter()
        assertNull(meter.update(positionMs = 10_000, wallMs = 0, speed = 1f, playing = true))
        assertEquals(5L, meter.update(positionMs = 11_005, wallMs = 1_000, speed = 1f, playing = true))
        assertEquals(0L, meter.update(positionMs = 13_005, wallMs = 2_000, speed = 2f, playing = true))
    }

    @Test
    fun clockDriftIgnoresPausesSeeksAndLongGaps() {
        val meter = ClockDriftMeter()
        meter.update(0, 0, 1f, true)
        assertNull(meter.update(1_000, 1_000, 1f, playing = false))
        assertNull(meter.update(60_000, 2_000, 1f, true))
        assertNull(meter.update(61_000, 10_000, 1f, true))
        meter.reset()
        assertNull(meter.update(62_000, 11_000, 1f, true))
    }
}
