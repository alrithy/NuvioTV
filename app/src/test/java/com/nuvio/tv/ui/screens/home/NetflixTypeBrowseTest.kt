package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.ui.util.asStable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetflixTypeBrowseTest {
    private fun card(id: String, type: String) = ModernCarouselItem(
        key = id, title = id, subtitle = null, imageUrl = null,
        heroPreview = HeroPreview(title = id, logo = null, description = null, contentTypeText = null, yearText = null,
            imdbText = null, genres = emptyList<String>().asStable(), poster = null, backdrop = null, imageUrl = null),
        payload = ModernPayload.Catalog(id, id, type, "", id, null, type),
    )

    private fun row(key: String, apiType: String?, vararg cards: ModernCarouselItem, loading: Boolean = false) =
        HeroCarouselRow(key = key, title = key, globalRowIndex = 0, items = cards.toList().asStable(), apiType = apiType, isLoading = loading)

    private val state = ModernHomePresentationState(rows = listOf(
        row("movies", "movie", card("m1", "movie"), card("m2", "movie")),
        row("shows", "series", card("s1", "series")),
        row("my_list", null, card("m3", "movie"), card("s2", "tv")),
        row("loading_movies", "movie", loading = true),
        row("loading_shows", "series", loading = true),
    ).asStable())

    @Test
    fun moviesKeepOnlyMovieCardsAndTheirLoadingRows() {
        val movies = state.filteredToType("movie")
        assertEquals(listOf("movies", "my_list", "loading_movies"), movies.rows.map { it.key })
        assertEquals(listOf("m3"), movies.rows.first { it.key == "my_list" }.items.map { it.key })
        assertTrue(movies.lookups.rowByKey.containsKey("my_list"))
        assertFalse(movies.lookups.rowByKey.containsKey("shows"))
    }

    @Test
    fun showsTreatTvAsSeries() {
        val shows = state.filteredToType("series")
        assertEquals(listOf("shows", "my_list", "loading_shows"), shows.rows.map { it.key })
        assertEquals(listOf("s2"), shows.rows.first { it.key == "my_list" }.items.map { it.key })
    }
}
