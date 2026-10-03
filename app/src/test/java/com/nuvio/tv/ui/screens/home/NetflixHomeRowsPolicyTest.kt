package com.nuvio.tv.ui.screens.home

import android.content.Context
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.TmdbSettings
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.model.WatchedItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NetflixHomeRowsPolicyTest {
    @Test fun `because you watched uses the newest factual completed or watched item`() {
        val watched = listOf(WatchedItem("tt-old", "movie", "Older watched film", watchedAt = 100),
            WatchedItem("unsupported", "channel", "Live station", watchedAt = 900))
        val result = netflixRecommendationSeed(watched,
            listOf(progress("tt-complete", 200, 95), progress("tt-started", 800, 15)), enabledSettings, "ar")
        assertEquals("tt-complete", result?.contentId)
        assertEquals(ContentType.MOVIE, result?.contentType)
        assertEquals("ar", result?.language)
    }

    @Test fun `unwatched content and disabled recommendation preferences cannot fabricate a seed`() {
        assertNull(netflixRecommendationSeed(emptyList(), listOf(progress("started", 800, 10)), enabledSettings, "en"))
        val watched = listOf(WatchedItem("film", "movie", "Film", watchedAt = 100))
        assertNull(netflixRecommendationSeed(watched, emptyList(), enabledSettings.copy(enabled = false), "en"))
        assertNull(netflixRecommendationSeed(watched, emptyList(), enabledSettings.copy(useMoreLikeThis = false), "en"))
    }

    @Test fun `other themes subscribe to neither library nor recommendations and Netflix bounds genuine list data`() = runTest {
        val theme = MutableStateFlow(false)
        val library = MutableStateFlow((1..30).map { entry("$it") } + entry("1"))
        val seeds = MutableStateFlow<NetflixRecommendationSeed?>(seed("seed"))
        val states = mutableListOf<NetflixHomeSources>()
        var requests = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            netflixHomeSourcesFlow(theme, library, seeds) { requests++; listOf(preview("related")) }.collect { states += it }
        }
        runCurrent()
        assertEquals(0, library.subscriptionCount.value)
        assertEquals(0, seeds.subscriptionCount.value)
        assertFalse(states.last().enabled)
        theme.value = true
        advanceTimeBy(651)
        runCurrent()
        assertEquals(1, requests)
        assertEquals((1..24).map(Int::toString), states.last().libraryItems.map { it.id })
        assertEquals(listOf("related"), states.last().recommendations?.items?.map { it.id })
    }

    @Test fun `rapid seed changes and theme exit cancel in-flight recommendation work`() = runTest {
        val theme = MutableStateFlow(true)
        val seeds = MutableStateFlow<NetflixRecommendationSeed?>(seed("first"))
        val starts = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val states = mutableListOf<NetflixHomeSources>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            netflixHomeSourcesFlow(theme, MutableStateFlow(listOf(entry("library"))), seeds) { seed ->
                starts += seed.contentId
                try { awaitCancellation() } finally { cancelled += seed.contentId }
            }.collect { states += it }
        }
        advanceTimeBy(651)
        runCurrent()
        seeds.value = seed("second")
        runCurrent()
        advanceTimeBy(651)
        runCurrent()
        theme.value = false
        runCurrent()
        assertEquals(listOf("first", "second"), starts)
        assertEquals(starts, cancelled)
        assertEquals(NetflixHomeSources(), states.last())
    }

    @Test fun `same seed is memoized across leaving and re-entering the theme`() = runTest {
        val theme = MutableStateFlow(true)
        val seeds = MutableStateFlow<NetflixRecommendationSeed?>(seed("film"))
        var requests = 0
        val states = mutableListOf<NetflixHomeSources>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            netflixHomeSourcesFlow(theme, MutableStateFlow(emptyList()), seeds) {
                requests++; listOf(preview("related"), preview("related"))
            }.collect { states += it }
        }
        advanceTimeBy(651)
        runCurrent()
        theme.value = false
        runCurrent()
        theme.value = true
        runCurrent()
        assertEquals(1, requests)
        assertEquals(1, states.last().recommendations?.items?.size)
    }

    @Test fun `library adaptation retains real play origin without adding a synthetic addon loader`() {
        val context = mockk<Context> { every { getString(any()) } returns "List" }
        val rows = buildNetflixSourceRows(NetflixHomeSources(true, listOf(entry("same"), entry("same", "series"))),
            context, showFullReleaseDate = true, showImdbRatings = true)
        val row = rows.single()
        assertNull(row.addonId)
        assertNull(row.catalogId)
        assertFalse(row.hasMore)
        assertFalse(row.supportsSkip)
        assertEquals(listOf("movie", "series"), row.items.list.map { (it.payload as ModernPayload.Catalog).itemType })
        assertTrue(row.items.list.all { (it.payload as ModernPayload.Catalog).addonBaseUrl == "https://addon.invalid/manifest.json" })
        assertEquals(2, row.items.list.map { it.key }.toSet().size)
        assertTrue(buildNetflixSourceRows(NetflixHomeSources(), context, true, true).isEmpty())
    }

    private val enabledSettings = TmdbSettings(enabled = true, language = "ar", useMoreLikeThis = true)
    private fun seed(id: String) = NetflixRecommendationSeed(id, ContentType.MOVIE, id, "en")
    private fun preview(id: String) = MetaPreview(id, ContentType.MOVIE, name = id, poster = null,
        posterShape = PosterShape.LANDSCAPE, background = null, logo = null, description = null,
        releaseInfo = null, imdbRating = null, genres = emptyList())
    private fun entry(id: String, type: String = "movie") = LibraryEntry(id, type, id, null,
        background = null, logo = null, description = null, releaseInfo = null, imdbRating = null,
        genres = emptyList(), addonBaseUrl = "https://addon.invalid/manifest.json")
    private fun progress(id: String, watchedAt: Long, percent: Int) = WatchProgress(id, "movie", id,
        null, null, null, id, null, null, null, percent.toLong(), 100, watchedAt)
}
