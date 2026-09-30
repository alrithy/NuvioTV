package com.nuvio.tv.fork.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamSubtitleReferenceTest {

    private fun candidate(url: String, language: String = "eng") =
        StreamSubtitleReference.Candidate(url = url, language = language, headers = emptyMap())

    @Test
    fun selectedSubtitleIsNeverItsOwnReference() {
        val selected = "https://subs.example/ar.srt"
        val result = StreamSubtitleReference.select(
            listOf(candidate(selected, "ara"), candidate("https://subs.example/en.srt")),
            selectedUrl = selected,
        )
        assertEquals(listOf("https://subs.example/en.srt"), result.map { it.url })
    }

    @Test
    fun blankAndDuplicateUrlsAreDropped() {
        val result = StreamSubtitleReference.select(
            listOf(candidate(""), candidate("https://a/1.srt"), candidate("https://a/1.srt", "fre")),
            selectedUrl = "https://addon/target.srt",
        )
        assertEquals(listOf("https://a/1.srt"), result.map { it.url })
        assertEquals("eng", result.single().language)
    }

    @Test
    fun atMostMaxReferencesInStreamOrder() {
        val urls = (1..5).map { "https://a/$it.srt" }
        val result = StreamSubtitleReference.select(urls.map(::candidate), selectedUrl = "https://addon/t.srt")
        assertEquals(urls.take(StreamSubtitleReference.MAX_REFERENCES), result.map { it.url })
    }

    @Test
    fun noStreamSubtitlesMeansNoFallback() {
        assertTrue(StreamSubtitleReference.select(emptyList(), selectedUrl = "https://addon/t.srt").isEmpty())
    }

    @Test
    fun headersAreCarriedUnchanged() {
        val headers = mapOf("Referer" to "https://subs.example/")
        val result = StreamSubtitleReference.select(
            listOf(StreamSubtitleReference.Candidate("https://subs.example/en.srt", "eng", headers)),
            selectedUrl = "https://addon/t.srt",
        )
        assertEquals(headers, result.single().headers)
    }

    @Test
    fun enabledByDefault() {
        // D051: SUBTITLE_INTELLIGENCE is AUTO; official AutoSync itself stays off until the user enables it.
        assertTrue(StreamSubtitleReference.enabled)
    }
}
