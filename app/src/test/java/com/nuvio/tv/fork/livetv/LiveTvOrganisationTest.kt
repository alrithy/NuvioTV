package com.nuvio.tv.fork.livetv

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTvOrganisationTest {

    private fun channel(name: String, group: String, source: String = "a") =
        LiveTvRepository.tagChannels(source, listOf(LiveTvChannel(id = name, name = name, streamUrl = "http://s/$name", group = group))).first()

    private val news1 = channel("BBC News", "News")
    private val news2 = channel("Sky News", "News")
    private val sport = channel("Sport 1", "Sports")
    private val loose = channel("Loose", "")
    private val other = channel("Other", "News", source = "b")
    private val channels = listOf(news1, news2, sport, loose, other)

    @Test
    fun groupsFollowTheViewersOrderThenAToZWithUncategorisedLast() {
        val names = setOf("sports", "News", "", "Kids")
        assertEquals(listOf("Kids", "News", "sports", ""), LiveTvOrganisation.orderedGroups(names, emptyList(), emptyMap()))
        assertEquals(listOf("sports", "Kids", "News", ""), LiveTvOrganisation.orderedGroups(names, listOf("sports", "Gone"), emptyMap()))
        // Renamed categories sort by the name shown; a renamed "Uncategorised" sorts like any other.
        assertEquals(listOf("", "Kids", "News", "sports"), LiveTvOrganisation.orderedGroups(names, emptyList(), mapOf("" to "Assorted")))
    }

    @Test
    fun hiddenCategoriesAndChannelsLeaveEverythingButFavorites() {
        val library = LiveTvLibrary(favorites = setOf(sport.key), hiddenGroups = setOf("Sports"), hiddenChannels = setOf(news2.key))
        assertEquals(listOf(news1, loose, other), LiveTvOrganisation.filter(channels, library, LiveTvFilterKeys.ALL))
        assertEquals(listOf(sport), LiveTvOrganisation.filter(channels, library, LiveTvFilterKeys.FAVORITES))
        assertEquals(listOf(news1, other), LiveTvOrganisation.filter(channels, library, "News"))
        assertEquals(listOf(other), LiveTvOrganisation.filter(channels, library, LiveTvFilterKeys.source("b")))
        assertEquals(listOf(news1), LiveTvOrganisation.filter(channels, library, LiveTvFilterKeys.ALL, query = " bbc "))
        assertEquals(listOf(news1, loose, other), LiveTvOrganisation.shownChannels(channels, library.hiddenGroups, library.hiddenChannels))
        assertSame(channels, LiveTvOrganisation.shownChannels(channels, emptySet(), emptySet()))
    }

    @Test
    fun staleFiltersFallBack() {
        val state = LiveTvState(sources = listOf(LiveTvSource("a", LiveTvSourceType.M3u)), library = LiveTvLibrary(hiddenGroups = setOf("News")))
        assertTrue(LiveTvOrganisation.isStale("News", state))
        assertFalse(LiveTvOrganisation.isStale("Sports", state))
        assertTrue(LiveTvOrganisation.isStale(LiveTvFilterKeys.source("gone"), state))
        assertFalse(LiveTvOrganisation.isStale(LiveTvFilterKeys.source("a"), state))
        assertFalse(LiveTvOrganisation.isStale(LiveTvFilterKeys.FAVORITES, state))
    }

    @Test
    fun movingStaysInsideTheList() {
        assertEquals(listOf("b", "a", "c"), LiveTvOrganisation.move(listOf("a", "b", "c"), "b", -1))
        assertNull(LiveTvOrganisation.move(listOf("a", "b"), "a", -1))
        assertNull(LiveTvOrganisation.move(listOf("a", "b"), "x", 1))
    }

    @Test
    fun theLibraryRoundTripsWithoutLinks() {
        val library = LiveTvLibrary(
            favorites = setOf(news1.key, -1L),
            hiddenGroups = setOf("", "Sports"),
            groupOrder = listOf("Sports", ""),
            groupNames = mapOf("" to "Misc", "News" to "Nachrichten \"24\""),
            hiddenChannels = setOf(news2.key),
            recent = LiveTvRecentChannel(sport.key, "Sport 1", "http://logo/1.png", "Sports", "sport.uk"),
        )
        val text = LiveTvLibraryCodec.encode(library)
        assertEquals(library, LiveTvLibraryCodec.decode(text))
        assertFalse("http://s/" in text)
        assertEquals(LiveTvLibrary(), LiveTvLibraryCodec.decode("{broken"))
    }

    @Test
    fun choicesArePublishedAndSavedPerProfile() = runBlocking {
        val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list.example/a.m3u"))))
        val http = FakeLiveTvFetcher { url ->
            if ("list.example" !in url) throw IOException("unexpected")
            "#EXTM3U\n#EXTINF:-1 group-title=\"News\",One\nhttp://s/1\n#EXTINF:-1 group-title=\"Sports\",Two\nhttp://s/2"
        }
        val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true)
        repository.ensureLoaded()
        val loaded = withTimeout(5_000) { repository.state.first { it.channels.size == 2 } }
        assertEquals(listOf("News", "Sports"), loaded.groups)
        val one = loaded.channels.first()

        repository.toggleFavorite(one)
        repository.setGroupHidden("Sports", hidden = true)
        repository.moveGroup("Sports", -1)
        repository.renameGroup("News", "Headlines")
        repository.recordRecentChannel(one)
        // The library changes apply at once; the channels zapping goes through are refiltered off
        // the caller's thread after a hiding change, so wait for that too (it raced this check).
        val state = withTimeout(5_000) { repository.state.first { it.library.recent != null && it.shownChannels == listOf(one) } }
        assertSame(loaded.channels, state.channels)
        assertEquals(setOf(one.key), state.library.favorites)
        assertEquals(listOf("Sports", "News"), state.groups)
        assertEquals(listOf("News"), state.visibleGroups)
        assertEquals(listOf(one), state.shownChannels)
        assertEquals(one, repository.recentChannel(state))
        withTimeout(5_000) { while (store.libraries[1]?.recent == null) delay(10) }
        assertEquals("Headlines", store.libraries.getValue(1).groupNames["News"])

        repository.toggleFavorite(one)
        assertEquals(emptySet<Long>(), withTimeout(5_000) { repository.state.first { it.library.favorites.isEmpty() } }.library.favorites)
    }

    @Test
    fun theMenuStaysOffUntilTurnedOnAndLiveTvOffHidesIt() = runBlocking {
        val store = MemoryLiveTvStore()
        val http = FakeLiveTvFetcher { throw IOException("unexpected") }
        val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true)
        assertFalse(repository.menuEnabled.first())
        repository.setMenuEnabled(true)
        assertTrue(withTimeout(5_000) { repository.menuEnabled.first { it } })
        store.menus.getValue(1).value = true
        assertFalse(LiveTvRepository(store, store, http, MutableStateFlow(1), false).menuEnabled.first())
    }

    @Test
    fun zappingWrapsAroundInTheListTheChannelCameFrom() {
        val list = listOf(news1, news2, sport)
        assertEquals(sport, LiveTvOrganisation.neighbour(list, news1.key, -1))
        assertEquals(news1, LiveTvOrganisation.neighbour(list, sport.key, 1))
        assertEquals(news2, LiveTvOrganisation.neighbour(list, news1.key, 1))
        assertEquals(news1, LiveTvOrganisation.neighbour(list, other.key, 1))
        assertNull(LiveTvOrganisation.neighbour(emptyList(), news1.key, 1))
    }

    @Test
    fun theZapListFallsBackToShownChannels() = runBlocking {
        val store = MemoryLiveTvStore(mapOf(1 to listOf(LiveTvSource("a", LiveTvSourceType.M3u, "http://list.example/a.m3u"))))
        val http = FakeLiveTvFetcher { "#EXTM3U\n#EXTINF:-1 group-title=\"News\",One\nhttp://s/1\n#EXTINF:-1,Two\nhttp://s/2" }
        val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true)
        repository.ensureLoaded()
        val state = withTimeout(5_000) { repository.state.first { it.shownChannels.size == 2 } }
        val (one, two) = state.channels
        repository.setZapList(listOf(two), folderKey = "")
        assertEquals(listOf(two) to "", repository.zapTarget(two.key))
        assertEquals(state.shownChannels to LiveTvFilterKeys.ALL, repository.zapTarget(one.key))
    }
}
