package com.nuvio.tv.fork.livetv

import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G10g: the phone setup page's token, Origin check, size caps, form reading and file import. */
class LiveTvSetupPolicyTest {

    @Test
    fun tokensAreLongRandomHexAndOtherSitesCannotPost() {
        val token = LiveTvSetupPolicy.newToken()
        assertTrue(Regex("^[0-9a-f]{32}$").matches(token))
        assertNotEquals(token, LiveTvSetupPolicy.newToken(SecureRandom()))
        assertTrue(LiveTvSetupPolicy.originAllowed(null, "192.168.1.5:8110"))
        assertTrue(LiveTvSetupPolicy.originAllowed("http://192.168.1.5:8110", "192.168.1.5:8110"))
        assertFalse(LiveTvSetupPolicy.originAllowed("https://evil.example", "192.168.1.5:8110"))
        assertFalse(LiveTvSetupPolicy.originAllowed("http://192.168.1.5:8110", null))
    }

    @Test
    fun formsAndPlaylistsAreCappedBeforeAnythingIsRead() {
        assertFalse(LiveTvSetupPolicy.formLengthAllowed(null))
        assertFalse(LiveTvSetupPolicy.formLengthAllowed(0))
        assertTrue(LiveTvSetupPolicy.formLengthAllowed(LiveTvSetupPolicy.MAX_FORM_BYTES))
        assertFalse(LiveTvSetupPolicy.formLengthAllowed(LiveTvSetupPolicy.MAX_FORM_BYTES + 1))
        assertEquals(LiveTvPlaylistRefusal.INVALID, LiveTvSetupPolicy.playlistRefusal(null))
        assertEquals(LiveTvPlaylistRefusal.INVALID, LiveTvSetupPolicy.playlistRefusal(0))
        assertNull(LiveTvSetupPolicy.playlistRefusal(LiveTvSetupPolicy.MAX_PLAYLIST_BYTES))
        assertEquals(LiveTvPlaylistRefusal.TOO_LARGE, LiveTvSetupPolicy.playlistRefusal(LiveTvSetupPolicy.MAX_PLAYLIST_BYTES + 1))
    }

    @Test
    fun theFormBecomesOneValidSourceOrNothing() {
        assertEquals(
            LiveTvSetupRequest.M3u("http://list.example/a.m3u"),
            LiveTvSetupPolicy.parseForm("""{"type":"m3u","url":" http://list.example/a.m3u "}"""),
        )
        assertEquals(
            LiveTvSetupRequest.Xtream(LiveTvXtreamSettings("http://x.example", "u", "p")),
            LiveTvSetupPolicy.parseForm("""{"type":"xtream","server":"http://x.example/player_api.php","username":"u","password":"p"}"""),
        )
        assertEquals(
            LiveTvSetupRequest.Stalker(LiveTvStalkerSettings("http://portal.example/c", "00:1A:79:AA:BB:CC")),
            LiveTvSetupPolicy.parseForm("""{"type":"stalker","portal":"http://portal.example/c/","mac":"00:1a:79:aa:bb:cc","extra":3}"""),
        )
        listOf(
            """{"type":"m3u","url":"file:///sdcard/a.m3u"}""",
            """{"type":"xtream","server":"http://x.example","username":"u"}""",
            """{"type":"stalker","portal":"ftp://p.example","mac":"00:1A"}""",
            """{"type":"other"}""",
            """["m3u"]""",
            """{"type":"m3u","url":""",
            "",
        ).forEach { assertNull(it, LiveTvSetupPolicy.parseForm(it)) }
    }

    @Test
    fun uploadedFileNamesLoseTheirFoldersAndControlCharacters() {
        assertEquals("my list.m3u", LiveTvSetupPolicy.playlistName("name=%2Fsdcard%2FDownload%2Fmy%20list.m3u"))
        assertEquals("a.m3u", LiveTvSetupPolicy.playlistName("x=1&name=C%3A%5Cusers%5Ca.m3u"))
        assertEquals(LiveTvSetupPolicy.DEFAULT_PLAYLIST_NAME, LiveTvSetupPolicy.playlistName(null))
        assertEquals(LiveTvSetupPolicy.DEFAULT_PLAYLIST_NAME, LiveTvSetupPolicy.playlistName("name=%0A"))
        assertEquals(80, LiveTvSetupPolicy.playlistName("name=" + "a".repeat(200)).length)
    }

    @Test
    fun anImportedFileIsLoadedAsANewSourceAndAnOversizedOneIsRefused() = runBlocking {
        val store = MemoryLiveTvStore()
        val http = FakeLiveTvFetcher { throw IOException("unexpected") }
        val repository = LiveTvRepository(store, store, http, MutableStateFlow(1), true)
        assertFalse(repository.importPlaylist("a.m3u", ByteArrayInputStream("x".toByteArray()), 10)) // not loaded yet
        repository.ensureLoaded()
        withTimeout(5_000) { repository.state.first { it.isLoaded } }
        val playlist = "#EXTM3U\n#EXTINF:-1 group-title=\"News\",One\nhttp://s/1\n"
        assertFalse(repository.importPlaylist("big.m3u", ByteArrayInputStream(playlist.toByteArray()), 8))
        assertTrue(repository.importPlaylist("my.m3u", ByteArrayInputStream(playlist.toByteArray()), 1024))
        val state = withTimeout(5_000) { repository.state.first { it.channels.isNotEmpty() } }
        assertEquals(listOf("One"), state.channels.map { it.name })
        assertEquals(listOf("my.m3u"), state.sources.map { it.url })
    }
}
