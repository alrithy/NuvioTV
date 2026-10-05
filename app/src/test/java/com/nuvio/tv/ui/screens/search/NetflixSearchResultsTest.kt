package com.nuvio.tv.ui.screens.search

import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import org.junit.Assert.assertEquals
import org.junit.Test

class NetflixSearchResultsTest {
    @Test
    fun `duplicate addon results retain their first usable source while equal IDs of different types survive`() {
        val movie = preview("shared", ContentType.MOVIE)
        val series = preview("shared", ContentType.SERIES)
        val results = netflixSearchResults(
            listOf(
                row("first", listOf(movie, preview("__placeholder_loading", ContentType.MOVIE))),
                row("second", listOf(movie, series))
            )
        )
        assertEquals(listOf("movie:shared", "series:shared"), results.map { it.key })
        assertEquals(listOf("https://first.invalid/manifest.json", "https://second.invalid/manifest.json"), results.map { it.addonBaseUrl })
    }

    private fun row(addon: String, items: List<MetaPreview>) = CatalogRow(
        addonId = addon,
        addonName = addon,
        addonBaseUrl = "https://$addon.invalid/manifest.json",
        catalogId = "search",
        catalogName = "Search",
        type = ContentType.MOVIE,
        items = items
    )

    private fun preview(id: String, type: ContentType) = MetaPreview(
        id = id, type = type, name = id, poster = null, posterShape = PosterShape.LANDSCAPE,
        background = null, logo = null, description = null, releaseInfo = null,
        imdbRating = null, genres = emptyList()
    )
}
