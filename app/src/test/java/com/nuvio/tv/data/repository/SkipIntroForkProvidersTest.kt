package com.nuvio.tv.data.repository

import com.nuvio.tv.fork.skip.ForkSkipProviders
import com.nuvio.tv.fork.skip.SkipProviderParsers
import com.nuvio.tv.fork.skip.SkipReport
import com.squareup.moshi.Moshi
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Superfork G9a: fork skip provider parsing and the merge with official results. */
class SkipIntroForkProvidersTest {

    private val moshi = Moshi.Builder().build()
    private val parsers = SkipProviderParsers(moshi)

    @Test
    fun `SkipMe episode segments are picked by season and episode`() {
        val raw = """[{"segments":[
            {"season":1,"episode":2,"segment":"intro","start_ms":30000,"end_ms":85000,"submissions":3},
            {"season":1,"episode":3,"segment":"intro","start_ms":1000,"end_ms":2000},
            {"season":1,"episode":2,"segment":"credits","start_ms":2500000,"end_ms":2600000}
        ]}]"""
        val reports = parsers.parseSkipMe(raw, isMovie = false, season = 1, episode = 2)
        assertEquals(listOf("intro" to 30.0, "outro" to 2500.0), reports.map { it.type to it.startTime })
        assertEquals(SkipProviderParsers.skipMeConfidence(3), reports.first().confidence, 1e-9)
        assertTrue(SkipProviderParsers.skipMeConfidence(100) <= 0.99)
    }

    @Test
    fun `SkipMe movie lists become movie credits and drop preview until G9b`() {
        val raw = """[{"intro":[{"start_ms":0,"end_ms":60000}],"credits":[{"start_ms":6000000,"end_ms":6300000}],"preview":[{"start_ms":10,"end_ms":20}]}]"""
        val reports = parsers.parseSkipMe(raw, isMovie = true, season = null, episode = null)
        assertEquals(listOf("intro", "movie-credits"), reports.map { it.type })
    }

    @Test
    fun `TheIntroDB accepts milliseconds, seconds and clock times`() {
        val raw = """{"intro":[{"start_ms":5000,"end_ms":65000}],"recap":[{"start":"0:10","end":"1:30"}],"credits":[{"start":1200.5}]}"""
        val reports = parsers.parseTheIntroDb(raw, isMovie = false, durationMs = 1_320_000L)
        assertEquals(listOf(5.0 to 65.0, 10.0 to 90.0, 1200.5 to 1320.0), reports.map { it.startTime to it.endTime })
        assertTrue(reports.all { it.confidence == SkipProviderParsers.THE_INTRO_DB_CONFIDENCE })
    }

    @Test
    fun `PublicMetaDB mapping and skips parse, broken bodies give nothing`() {
        assertEquals(1396L, parsers.parsePublicMetaDbMapping("""{"results":[{"tmdb_id":1396}]}"""))
        val reports = parsers.parsePublicMetaDb(
            """{"items":[{"intro_start_ms":1000,"intro_end_ms":45000,"credits_start_ms":-1,"credits_end_ms":-1}]}""",
            isMovie = false,
        )
        assertEquals(listOf("intro"), reports.map { it.type })
        assertTrue(parsers.parseSkipMe("not json", isMovie = true, season = null, episode = null).isEmpty())
        assertTrue(parsers.parseTheIntroDb("{", isMovie = true, durationMs = null).isEmpty())
        assertEquals(null, parsers.parsePublicMetaDbMapping("[]"))
    }

    @Test
    fun `official results merge with fork reports into one segment per category`() {
        val providers = ForkSkipProviders(mockk<OkHttpClient>(relaxed = true), moshi, mockk(relaxed = true))
        val official = listOf(
            SkipInterval(60.0, 90.0, "intro", "introdb"),
            SkipInterval(0.0, 20.0, "recap", "aniskip"),
        )
        val fork = listOf(
            SkipReport(61.0, 91.0, "opening", "intro", "skipme", 0.8),
            SkipReport(2500.0, 2600.0, "ending", "outro", "theintrodb", 0.86),
        )
        val merged = providers.combine(official, fork, isMovie = false)
        assertEquals(listOf("recap", "intro", "outro"), merged.map { it.type })
        val intro = merged.first { it.type == "intro" }
        assertEquals("introdb", intro.provider)
        assertTrue(intro.startTime in 60.0..61.0)
    }

    @Test
    fun `movie credits from a fork provider never cover the official post-credits scene`() {
        val providers = ForkSkipProviders(mockk<OkHttpClient>(relaxed = true), moshi, mockk(relaxed = true))
        val official = listOf(SkipInterval(6200.0, 6260.0, "post-credits", "introdb"))
        val fork = listOf(SkipReport(6000.0, 6300.0, "movie-credits", "movie-credits", "skipme", 0.9))
        val merged = providers.combine(official, fork, isMovie = true)
        assertEquals(6200.0, merged.first { it.type == "movie-credits" }.endTime, 0.0)
        assertTrue(merged.any { it.type == "post-credits" })
    }
}
