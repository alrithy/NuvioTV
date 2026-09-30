package com.nuvio.tv.fork.postplay

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostPlaySourcesTest {

    @Test
    fun officialStaysTheDefaultAndKeepsItsFourCards() {
        assertEquals(ForkPostPlaySource.OFFICIAL, ForkPostPlaySource.fromKey(null))
        assertEquals(ForkPostPlaySource.OFFICIAL, ForkPostPlaySource.fromKey("gone"))
        assertEquals(listOf(PostPlayStep.OFFICIAL), PostPlaySources.chain(ForkPostPlaySource.OFFICIAL))
        assertEquals(4, PostPlaySources.cardLimit(ForkPostPlaySource.OFFICIAL, 4))
        assertEquals(PostPlaySources.FORK_CARD_LIMIT, PostPlaySources.cardLimit(ForkPostPlaySource.AUTO, 4))
    }

    @Test
    fun everyForkSourceEndsInOfficialsChain() {
        assertEquals(
            listOf(PostPlayStep.KURATO_AI, PostPlayStep.BINGECAT_AI, PostPlayStep.OFFICIAL),
            PostPlaySources.chain(ForkPostPlaySource.AUTO),
        )
        ForkPostPlaySource.entries.forEach { source ->
            assertEquals(PostPlayStep.OFFICIAL, PostPlaySources.chain(source).last())
        }
        assertEquals(PostPlayStep.MDBLIST, PostPlaySources.chain(ForkPostPlaySource.MDBLIST).first())
    }

    @Test
    fun theChainSkipsMissingFailingAndFullyFilteredSteps() = runBlocking {
        val asked = mutableListOf<PostPlayStep>()
        val result = PostPlaySources.firstNonEmpty(
            steps = PostPlaySources.chain(ForkPostPlaySource.AUTO),
            load = { step ->
                asked += step
                when (step) {
                    PostPlayStep.KURATO_AI -> null
                    PostPlayStep.BINGECAT_AI -> error("add-on down")
                    PostPlayStep.OFFICIAL -> listOf("watched", "b")
                    PostPlayStep.MDBLIST -> listOf("never")
                }
            },
            usable = { items -> items.filterNot { it == "watched" } },
        )
        assertEquals(listOf("b"), result)
        assertEquals(listOf(PostPlayStep.KURATO_AI, PostPlayStep.BINGECAT_AI, PostPlayStep.OFFICIAL), asked)

        val allWatched = PostPlaySources.firstNonEmpty(
            steps = listOf(PostPlayStep.MDBLIST, PostPlayStep.OFFICIAL),
            load = { step -> if (step == PostPlayStep.MDBLIST) listOf("watched") else listOf("c") },
            usable = { items -> items.filterNot { it == "watched" } },
        )
        assertEquals(listOf("c"), allWatched)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNeverSwallowed() {
        runBlocking {
            PostPlaySources.firstNonEmpty<String>(
                steps = listOf(PostPlayStep.KURATO_AI, PostPlayStep.OFFICIAL),
                load = { throw CancellationException("left the player") },
            )
        }
    }

    @Test
    fun aiQueriesMatchCxsmo() {
        assertEquals(
            "Recommend movies similar to \"Alien\". Focus on similar tone and genres such as Horror, Sci-Fi. Return titles only, not episodes.",
            PostPlaySources.kuratoQuery(" Alien ", listOf("Horror", " Sci-Fi", "Horror", ""), isSeries = false),
        )
        assertEquals(
            "TV shows like \"this title\" with a similar tone and genres such as Drama, Crime, Thriller",
            PostPlaySources.bingeCatQuery("", listOf("Drama", "Crime", "Thriller", "Mystery"), isSeries = true),
        )
        assertEquals("movies like \"Up\"", PostPlaySources.bingeCatQuery("Up", emptyList(), isSeries = false))
    }

    @Test
    fun onlyYouTubeTrailersFromAddonsAreUsed() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", PostPlaySources.addonTrailerUrl(" dQw4w9WgXcQ "))
        assertEquals("https://youtu.be/abc", PostPlaySources.addonTrailerUrl("https://youtu.be/abc"))
        assertNull(PostPlaySources.addonTrailerUrl("https://example.com/trailer.mp4"))
        assertNull(PostPlaySources.addonTrailerUrl("javascript:alert(1)"))
        assertNull(PostPlaySources.addonTrailerUrl("short"))
        val urls = PostPlaySources.addonTrailerUrls(sequenceOf("dQw4w9WgXcQ", null, "", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "x"))
        assertEquals(listOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), urls)
        assertTrue(PostPlaySources.addonTrailerUrls(emptySequence()).isEmpty())
    }
}
