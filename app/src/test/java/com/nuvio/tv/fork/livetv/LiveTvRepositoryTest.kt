package com.nuvio.tv.fork.livetv

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MemoryLiveTvStore(initial: Map<Int, List<LiveTvSource>> = emptyMap()) : LiveTvSourceStore, LiveTvLibraryStore {
    val saved = HashMap(initial)
    val libraries = HashMap<Int, LiveTvLibrary>()
    val menus = HashMap<Int, MutableStateFlow<Boolean>>()
    private var next = 0

    override suspend fun library(profileId: Int) = synchronized(libraries) { libraries[profileId] } ?: LiveTvLibrary()

    override suspend fun saveLibrary(profileId: Int, library: LiveTvLibrary) {
        synchronized(libraries) { libraries[profileId] = library }
    }

    private fun menu(profileId: Int) = synchronized(menus) { menus.getOrPut(profileId) { MutableStateFlow(false) } }

    override fun menuEnabled(profileId: Int) = menu(profileId)

    override suspend fun setMenuEnabled(profileId: Int, enabled: Boolean) {
        menu(profileId).value = enabled
    }

    override suspend fun sources(profileId: Int) = saved[profileId].orEmpty()

    override suspend fun saveSources(profileId: Int, sources: List<LiveTvSource>) {
        synchronized(saved) { saved[profileId] = sources }
    }

    override fun newSourceId(): String = "s${next++}"

    override suspend fun <T> readPlaylist(profileId: Int, sourceId: String, block: (Sequence<String>) -> T): T? = null

    override suspend fun savePlaylist(profileId: Int, sourceId: String, input: InputStream, maxBytes: Long) = false

    override suspend fun deletePlaylist(profileId: Int, sourceId: String) = Unit
}

class LiveTvRepositoryTest {

    private val good = LiveTvSource("a", LiveTvSourceType.M3u, "http://good.example/list.m3u")
    private val bad = LiveTvSource("b", LiveTvSourceType.M3u, "http://bad.example/list.m3u")

    private var badFails = true
    private val http = FakeLiveTvFetcher { requested ->
        val url = requested.lowercase()
        when {
            "good.example" in url -> "#EXTM3U url-tvg=\"http://guide.example/g.xml\"\n#EXTINF:-1 group-title=\" News \",One\nhttp://s/1\n#EXTINF:-1,Two\nhttp://s/2"
            "bad.example" in url -> if (badFails) throw IOException("HTTP 503 for http://bad.example/list.m3u?password=x") else "#EXTM3U\n#EXTINF:-1,B\nhttp://s/b"
            else -> throw IOException("unexpected")
        }
    }

    private fun repository(store: MemoryLiveTvStore, profile: MutableStateFlow<Int> = MutableStateFlow(1), enabled: Boolean = true) =
        LiveTvRepository(store, store, http, profile, enabled)

    private suspend fun LiveTvRepository.settled(predicate: (LiveTvState) -> Boolean = { true }) =
        withTimeout(5_000) { state.first { it.isLoaded && !it.isLoading && predicate(it) } }

    @Test
    fun aFailingSourceIsIsolatedFromTheOthers() = runBlocking {
        val repository = repository(MemoryLiveTvStore(mapOf(1 to listOf(good, bad))))
        assertTrue(repository.ensureLoaded())
        val state = repository.settled { it.sourceErrors.isNotEmpty() }
        assertEquals(listOf("One", "Two"), state.channels.map { it.name })
        assertEquals(LiveTvError.LoadFailed, state.sourceErrors["b"])
        assertEquals(mapOf("a" to 2), state.sourceCounts)
        assertEquals(listOf("http://guide.example/g.xml"), state.epgUrls)
        assertFalse(repository.ensureLoaded())
    }

    @Test
    fun channelsAreTaggedWithHashedKeysNotLinks() = runBlocking {
        val repository = repository(MemoryLiveTvStore(mapOf(1 to listOf(good))))
        repository.ensureLoaded()
        val one = repository.settled { it.channels.isNotEmpty() }.channels.first()
        assertEquals("a/m0", one.id)
        assertEquals("a", one.sourceId)
        assertEquals("News", one.group)
        assertEquals(liveTvChannelKey("a", "News", "One"), one.key)
        assertEquals(one.key, LiveTvRepository.tagChannels("a", listOf(one.copy(streamUrl = "http://rotated/1", group = "News"))).first().key)
    }

    @Test
    fun aRefreshThatFailsKeepsTheChannelsTheSourceHad() = runBlocking {
        badFails = false
        val repository = repository(MemoryLiveTvStore(mapOf(1 to listOf(bad))))
        repository.ensureLoaded()
        assertEquals(listOf("B"), repository.settled { it.channels.isNotEmpty() }.channels.map { it.name })
        badFails = true
        repository.refresh()
        val state = repository.settled { it.sourceErrors.isNotEmpty() }
        assertEquals(listOf("B"), state.channels.map { it.name })
    }

    @Test
    fun addingSavesOnSuccessAndReplacesTheSameSource() = runBlocking {
        val store = MemoryLiveTvStore()
        val repository = repository(store)
        repository.ensureLoaded()
        repository.settled()
        repository.addM3uUrl(" http://good.example/list.m3u ")
        repository.settled { it.addedCount == 1 }
        repository.addM3uUrl("http://GOOD.example/list.m3u")
        val state = repository.settled { it.addedCount == 2 }
        assertEquals(1, state.sources.size)
        withTimeout(5_000) { while (store.saved[1]?.size != 1) kotlinx.coroutines.delay(10) }
        repository.addM3uUrl("http://bad.example/list.m3u")
        val failed = repository.settled { it.error != null }
        assertEquals(LiveTvError.LoadFailed, failed.error)
        assertEquals(1, failed.sources.size)
    }

    @Test
    fun invalidInputIsRejectedBeforeAnyRequest() = runBlocking {
        val repository = repository(MemoryLiveTvStore())
        repository.ensureLoaded()
        repository.settled()
        repository.addXtream(LiveTvXtreamSettings("panel.example", "u", "p"))
        assertEquals(LiveTvError.XtreamInvalidUrl, repository.state.value.error)
        repository.addStalker(LiveTvStalkerSettings("http://p.example", ""))
        assertEquals(LiveTvError.StalkerRequired, repository.state.value.error)
        repository.addM3uUrl("  ")
        assertEquals(LiveTvError.InvalidUrl, repository.state.value.error)
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun removingASourceDropsOnlyItsChannels() = runBlocking {
        badFails = false
        val store = MemoryLiveTvStore(mapOf(1 to listOf(good, bad)))
        val repository = repository(store)
        repository.ensureLoaded()
        repository.settled { it.channels.size == 3 }
        repository.removeSource("a")
        val state = repository.settled { it.sources.size == 1 }
        assertEquals(listOf("B"), state.channels.map { it.name })
        withTimeout(5_000) { while (store.saved[1]?.size != 1) kotlinx.coroutines.delay(10) }
    }

    @Test
    fun profilesKeepTheirOwnSources() = runBlocking {
        badFails = false
        val profile = MutableStateFlow(1)
        val repository = repository(MemoryLiveTvStore(mapOf(1 to listOf(good), 2 to listOf(bad))), profile)
        repository.ensureLoaded()
        assertEquals(2, repository.settled { it.channels.isNotEmpty() }.channels.size)
        profile.value = 2
        assertTrue(repository.ensureLoaded())
        assertEquals(listOf("B"), repository.settled { it.channels.isNotEmpty() }.channels.map { it.name })
    }

    @Test
    fun liveTvOffLoadsNothing() = runBlocking {
        val repository = repository(MemoryLiveTvStore(mapOf(1 to listOf(good))), enabled = false)
        assertFalse(repository.ensureLoaded())
        assertNull(repository.state.value.error)
        assertTrue(repository.state.value.channels.isEmpty())
        assertTrue(http.requests.isEmpty())
    }
}
