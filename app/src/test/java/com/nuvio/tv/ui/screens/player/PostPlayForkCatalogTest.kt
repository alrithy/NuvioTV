package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.CatalogDescriptor
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.fork.postplay.PostPlayStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Superfork G9c: which installed add-on catalog answers for Kurato AI and BingeCat AI. */
class PostPlayForkCatalogTest {

    private fun addon(id: String, name: String, vararg catalogs: CatalogDescriptor, enabled: Boolean = true) = Addon(
        id = id, name = name, version = "1", description = null, logo = null,
        baseUrl = "https://$id.example", catalogs = catalogs.toList(), types = emptyList(),
        resources = emptyList(), enabled = enabled,
    )

    private fun catalog(id: String, type: ContentType) = CatalogDescriptor(type = type, id = id, name = id)

    @Test
    fun `the exact Kurato catalog for the content type wins`() {
        val other = addon("other", "Other", catalog("kurato-ai-discover-extra", ContentType.MOVIE))
        val kurato = addon(
            "org.aiostreams.kurato", "Kurato",
            catalog("kurato-ai-discover-series", ContentType.SERIES),
            catalog("kurato-ai-discover-movie", ContentType.MOVIE),
        )
        val (movieAddon, movieCatalog) = findAiCatalog(listOf(other, kurato), ContentType.MOVIE, PostPlayStep.KURATO_AI)!!
        assertEquals("org.aiostreams.kurato" to "kurato-ai-discover-movie", movieAddon.id to movieCatalog.id)
        val series = findAiCatalog(listOf(other, kurato), ContentType.SERIES, PostPlayStep.KURATO_AI)!!
        assertEquals("kurato-ai-discover-series", series.second.id)
    }

    @Test
    fun `BingeCat falls back to a partial catalog id and skips disabled add-ons`() {
        val disabled = addon("bingecat", "BingeCat", catalog("aicat_search_movie", ContentType.MOVIE), enabled = false)
        val partial = addon("mirror", "Mirror", catalog("aicat_search_movie_v2", ContentType.MOVIE))
        val found = findAiCatalog(listOf(disabled, partial), ContentType.MOVIE, PostPlayStep.BINGECAT_AI)!!
        assertEquals("mirror" to "aicat_search_movie_v2", found.first.id to found.second.id)
    }

    @Test
    fun `nothing installed or a non-catalog step gives nothing`() {
        val plain = addon("cinemeta", "Cinemeta", catalog("top", ContentType.MOVIE))
        assertNull(findAiCatalog(listOf(plain), ContentType.MOVIE, PostPlayStep.KURATO_AI))
        assertNull(findAiCatalog(listOf(plain), ContentType.SERIES, PostPlayStep.BINGECAT_AI))
        assertNull(findAiCatalog(listOf(plain), ContentType.MOVIE, PostPlayStep.OFFICIAL))
    }
}
