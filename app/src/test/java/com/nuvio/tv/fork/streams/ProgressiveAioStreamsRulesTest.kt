package com.nuvio.tv.fork.streams

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveAioStreamsRulesTest {

    @Test
    fun onlyAddonsThatOptInUseTheProgressiveEndpoint() {
        assertTrue(ProgressiveAioStreamsRules.isProgressive("https://aio.example/abc/manifest?client=nuvio-progressive"))
        assertTrue(ProgressiveAioStreamsRules.isProgressive("https://aio.example/abc?x=1&CLIENT=Nuvio-Progressive/"))
        assertFalse(ProgressiveAioStreamsRules.isProgressive("https://aio.example/abc"))
        assertFalse(ProgressiveAioStreamsRules.isProgressive("https://aio.example/abc?client=stremio"))
        assertFalse(ProgressiveAioStreamsRules.isProgressive("https://aio.example/client=nuvio-progressive"))
    }

    @Test
    fun progressiveUrlSitsNextToTheOrdinaryStreamPathAndKeepsTheQuery() {
        assertEquals(
            "https://aio.example/cfg/stream-progressive/movie/tt123.ndjson?client=nuvio-progressive",
            ProgressiveAioStreamsRules.progressiveUrl("https://aio.example/cfg/?client=nuvio-progressive", "movie", "tt123"),
        )
        assertEquals(
            "https://aio.example/cfg/stream-progressive/series/tt1%3A1%3A2.ndjson",
            ProgressiveAioStreamsRules.progressiveUrl("https://aio.example/cfg/", "series", "tt1%3A1%3A2"),
        )
    }

    @Test
    fun keepAliveAndCommentLinesAreSkipped() {
        assertFalse(ProgressiveAioStreamsRules.isEventLine(""))
        assertFalse(ProgressiveAioStreamsRules.isEventLine("   "))
        assertFalse(ProgressiveAioStreamsRules.isEventLine(": keep-alive"))
        assertTrue(ProgressiveAioStreamsRules.isEventLine("""{"streams":[],"complete":true}"""))
    }

    @Test
    fun logsNeverCarryTheAddonConfiguration() {
        assertEquals("aio.example", ProgressiveAioStreamsRules.logHost("https://aio.example/eyJkZWJyaWQiOiJTRUNSRVQifQ/manifest?token=x"))
        assertEquals("?", ProgressiveAioStreamsRules.logHost("not a url"))
    }
}
