package com.nuvio.tv.fork.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFontImportPolicyTest {

    @Test
    fun tokenIs128BitHexAndFreshEachTime() {
        val token = SubtitleFontImportPolicy.newToken()
        assertTrue(Regex("[0-9a-f]{32}").matches(token))
        assertNotEquals(token, SubtitleFontImportPolicy.newToken())
    }

    @Test
    fun crossSiteOriginIsRefused() {
        assertTrue(SubtitleFontImportPolicy.originAllowed(null, "192.168.1.5:8100"))
        assertTrue(SubtitleFontImportPolicy.originAllowed("http://192.168.1.5:8100", "192.168.1.5:8100"))
        assertFalse(SubtitleFontImportPolicy.originAllowed("https://evil.example", "192.168.1.5:8100"))
        assertFalse(SubtitleFontImportPolicy.originAllowed("http://192.168.1.5:8100", null))
    }

    @Test
    fun uploadLengthMustBeDeclaredAndBounded() {
        assertEquals(SubtitleFontImportResult.INVALID, SubtitleFontImportPolicy.uploadLengthVerdict(null))
        assertEquals(SubtitleFontImportResult.INVALID, SubtitleFontImportPolicy.uploadLengthVerdict(0))
        assertEquals(
            SubtitleFontImportResult.TOO_LARGE,
            SubtitleFontImportPolicy.uploadLengthVerdict(SubtitleFontFile.MAX_FONT_BYTES + 1),
        )
        assertNull(SubtitleFontImportPolicy.uploadLengthVerdict(SubtitleFontFile.MAX_FONT_BYTES))
    }

    @Test
    fun onlyHttpsDownloads() {
        assertTrue(SubtitleFontImportPolicy.downloadUrlAllowed(" https://fonts.example/a.ttf "))
        assertFalse(SubtitleFontImportPolicy.downloadUrlAllowed("http://fonts.example/a.ttf"))
        assertFalse(SubtitleFontImportPolicy.downloadUrlAllowed("file:///sdcard/a.ttf"))
        assertFalse(SubtitleFontImportPolicy.downloadUrlAllowed("https:///a.ttf"))
        assertFalse(SubtitleFontImportPolicy.downloadUrlAllowed("not a url"))
    }

    @Test
    fun logsCarryTheHostOnly() {
        assertEquals("fonts.example", SubtitleFontImportPolicy.logHost("https://fonts.example/a.ttf?token=secret"))
        assertEquals("?", SubtitleFontImportPolicy.logHost("::"))
    }
}
