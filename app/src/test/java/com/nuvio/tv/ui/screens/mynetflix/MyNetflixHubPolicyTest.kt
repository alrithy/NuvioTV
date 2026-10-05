package com.nuvio.tv.ui.screens.mynetflix

import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.model.WatchedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyNetflixHubPolicyTest {
    private fun progress(id: String, position: Long, lastWatched: Long, type: String = "movie") =
        WatchProgress(id, type, "Title $id", "poster:$id", "backdrop:$id", null, "video:$id", null, null, null,
            position, 1_000L, lastWatched)

    private fun entry(id: String, listedAt: Long) = LibraryEntry(id, "movie", "Saved $id", "poster:$id",
        background = "backdrop:$id", logo = null, description = null, releaseInfo = null, imdbRating = null,
        genres = emptyList(), addonBaseUrl = "https://addon.invalid", listedAt = listedAt)

    @Test fun `hub works from local data alone and orders each section by recency`() {
        val hub = buildMyNetflixHub(
            library = listOf(entry("a", 1), entry("b", 3), entry("c", 2)),
            continueWatching = listOf(progress("x", 300, 10), progress("y", 500, 20)),
            watched = listOf(WatchedItem("w", "movie", "Watched w", watchedAt = 5, poster = "poster:w")),
            allProgress = emptyList(),
        )
        assertFalse(hub.loading)
        assertEquals(listOf(MyNetflixSection.CONTINUE_WATCHING, MyNetflixSection.MY_LIST, MyNetflixSection.RECENTLY_WATCHED),
            hub.rows.map { it.section })
        assertEquals(listOf("y", "x"), hub.rows[0].cards.map { it.contentId })
        assertEquals(.5f, hub.rows[0].cards[0].progress!!, 0.001f)
        assertEquals(listOf("b", "c", "a"), hub.rows[1].cards.map { it.contentId })
        // 16:9 artwork first.
        assertEquals("backdrop:b", hub.rows[1].cards[0].imageUrl)
    }

    @Test fun `sections without data are omitted and nothing is invented`() {
        val empty = buildMyNetflixHub(emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(empty.isEmpty)
        assertTrue(empty.rows.isEmpty())
        val onlyList = buildMyNetflixHub(listOf(entry("a", 1)), emptyList(), emptyList(), emptyList())
        assertEquals(listOf(MyNetflixSection.MY_LIST), onlyList.rows.map { it.section })
        assertTrue(onlyList.rows.single().cards.all { it.progress == null && it.resume == null })
    }

    @Test fun `completed titles move to history and are not repeated while still resumable`() {
        val completed = progress("done", 990, 30)
        val resuming = progress("x", 400, 40)
        val hub = buildMyNetflixHub(emptyList(), listOf(resuming, completed),
            watched = listOf(WatchedItem("x", "movie", "Title x", watchedAt = 50)), allProgress = listOf(completed, resuming))
        val continueIds = hub.rows.first { it.section == MyNetflixSection.CONTINUE_WATCHING }.cards.map { it.contentId }
        val historyIds = hub.rows.first { it.section == MyNetflixSection.RECENTLY_WATCHED }.cards.map { it.contentId }
        assertEquals(listOf("x"), continueIds)
        assertEquals(listOf("done"), historyIds)
        assertEquals("backdrop:done", hub.rows.last().cards.single().imageUrl)
    }

    @Test fun `sections stay bounded for large libraries`() {
        val hub = buildMyNetflixHub((1..500).map { entry("$it", it.toLong()) }, emptyList(), emptyList(), emptyList())
        assertEquals(MY_NETFLIX_LIST_LIMIT, hub.rows.single().cards.size)
        assertEquals(500, hub.libraryCount)
    }
}
