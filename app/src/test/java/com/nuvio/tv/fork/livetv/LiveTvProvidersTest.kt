package com.nuvio.tv.fork.livetv

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Answers requests from recorded bodies by the `action` (or path) they ask for. */
internal class FakeLiveTvFetcher(private val answer: (url: String) -> String) : LiveTvFetcher {
    val requests = mutableListOf<Pair<String, Map<String, String>>>()

    override suspend fun <T> stream(url: String, headers: Map<String, String>, block: (BufferedSource) -> T): T {
        synchronized(requests) { requests += url to headers }
        return block(Buffer().writeUtf8(answer(url)))
    }
}

class LiveTvProvidersTest {

    private val xtreamSettings = LiveTvXtreamSettings("http://panel.example:8080", "user", "p@ss").normalized()

    @Test
    fun xtreamListsPanelLinksWithCategories() = runBlocking {
        val http = FakeLiveTvFetcher { url ->
            when {
                "get_live_categories" in url -> """[{"category_id":"1","category_name":"News"}]"""
                "get_live_streams" in url ->
                    """[{"name":"One","stream_id":10,"category_id":"1","epg_channel_id":"one.uk","stream_icon":"http://l/1.png",
                        "direct_source":"http://origin/1"},{"name":"Dup","stream_id":10},{"stream_id":11},{"name":"Two","stream_id":"12"}]"""
                else -> """{"user_info":{"allowed_output_formats":["m3u8"]}}"""
            }
        }
        val channels = LiveTvXtream(http).channels(xtreamSettings)
        assertEquals(listOf("One", "Two"), channels.map { it.name })
        assertEquals("http://panel.example:8080/live/user/p%40ss/10.m3u8", channels[0].streamUrl)
        assertEquals("News", channels[0].group)
        assertEquals("one.uk", channels[0].tvgId)
        assertEquals("", channels[1].group)
    }

    @Test
    fun xtreamUsesTsUnlessOnlyHlsIsAllowed() {
        assertEquals("ts", xtreamLiveExtension(emptyList()))
        assertEquals("ts", xtreamLiveExtension(listOf("m3u8", "ts")))
        assertEquals("ts", xtreamLiveExtension(listOf("rtmp")))
        assertEquals("m3u8", xtreamLiveExtension(listOf("m3u8")))
        assertEquals(listOf("m3u8", "ts"), readXtreamAllowedFormats("""{"user_info":{"auth":1,"allowed_output_formats":["M3U8","ts"]}}"""))
        assertEquals(emptyList<String>(), readXtreamAllowedFormats("""{"user_info":{"auth":0}}"""))
    }

    @Test
    fun xtreamServerIsNormalisedAndSecretsStayOutOfToString() {
        val settings = LiveTvXtreamSettings(" http://h.example/player_api.php ", " u ", " secret ").normalized()
        assertEquals("http://h.example", settings.serverUrl)
        assertEquals("u", settings.username)
        assertFalse("secret" in settings.toString())
        assertFalse("secret" in LiveTvSource("a", LiveTvSourceType.Xtream, "http://u:secret@h/x", xtream = settings).toString())
    }

    @Test
    fun stalkerPagesAndJsFieldsAreStreamed() {
        val page = readStalkerPage(
            Buffer().writeUtf8("""{"js":{"total_items":"3","max_page_items":2,"data":[{"name":"A","cmd":"ffmpeg http://p/a"},{"x":{"nested":1}}]}}"""),
        ) { it["name"] }
        assertEquals(listOf("A"), page.entries)
        assertEquals(3, page.totalItems)
        assertEquals(2, page.maxPageItems)
        assertEquals("tok", readJsFields("""{"js":{"token":"tok","random":null}}""")["token"])
        assertEquals("tok", readJsFields("""{"token":"tok"}""")["token"])
        assertEquals(emptyMap<String, String>(), readJsFields("[]"))
        assertEquals("http://p/a", "ffmpeg http://p/a extra".toStalkerPlayableUrl())
    }

    @Test
    fun stalkerEndpointsAndLogos() {
        assertEquals("http://p.example/portal.php", stalkerEndpoint("http://p.example/c"))
        assertEquals("http://p.example/portal.php", stalkerEndpoint("http://p.example/c/"))
        assertEquals("http://p.example/portal.php", stalkerEndpoint("http://p.example/portal.php"))
        assertEquals("http://p.example/stalker_portal/portal.php", stalkerEndpoint("http://p.example/stalker_portal"))
        assertEquals("http://p.example:88/stalker_portal/misc/logos/320/1.png", stalkerLogoUrl("http://p.example:88/c/", "1.png"))
        assertEquals("http://p.example:88/img/1.png", stalkerLogoUrl("http://p.example:88/c/", "/img/1.png"))
        assertEquals("https://cdn/1.png", stalkerLogoUrl("http://p.example/c/", "https://cdn/1.png"))
        assertNull(stalkerLogoUrl("http://p.example/c/", "ftp://x/1.png"))
    }

    @Test
    fun stalkerSessionHeadersGoOnlyToThePortalHost() = runBlocking {
        val http = FakeLiveTvFetcher { url ->
            when {
                "action=handshake" in url -> """{"js":{"token":"TOKEN"}}"""
                "action=get_profile" in url -> """{"js":{}}"""
                "action=get_genres" in url -> """{"js":[{"id":"7","title":"Sport"}]}"""
                "action=get_all_channels" in url ->
                    """{"js":{"data":[{"id":"1","name":"Portal","cmd":"ffmpeg http://portal.example/ch/1","tv_genre_id":"7"},
                        {"id":"2","name":"Cdn","cmd":"ffmpeg http://cdn.example/ch/2"}]}}"""
                else -> throw IOException("unexpected")
            }
        }
        val settings = LiveTvStalkerSettings("http://portal.example/c", "00:1a:79:00:00:01").normalized()
        val (channels, incomplete) = LiveTvStalker(http).channels(settings)
        assertFalse(incomplete)
        assertEquals(listOf("Portal", "Cdn"), channels.map { it.name })
        assertEquals("Sport", channels[0].group)
        assertEquals("Bearer TOKEN", channels[0].headers["Authorization"])
        assertTrue(channels[0].headers.getValue("Cookie").startsWith("mac=00:1A:79:00:00:01"))
        assertNull(channels[1].headers["Authorization"])
        assertNull(channels[1].headers["Cookie"])
        assertTrue(channels[1].headers.containsKey("User-Agent"))
        assertFalse("00:1A:79" in settings.toString())
    }

    @Test
    fun stalkerFallsBackToOrderedPagesAndFlagsMissingOnes() = runBlocking {
        val http = FakeLiveTvFetcher { url ->
            when {
                "action=handshake" in url -> """{"js":{"token":"T"}}"""
                "action=get_profile" in url || "action=get_genres" in url -> """{"js":{}}"""
                "action=get_all_channels" in url -> throw IOException("unsupported")
                "p=1" in url -> """{"js":{"total_items":6,"max_page_items":2,"data":[{"name":"a","cmd":"http://p/a"},{"name":"b","cmd":"http://p/b"}]}}"""
                "p=2" in url -> """{"js":{"data":[{"name":"c","cmd":"http://p/c"}]}}"""
                else -> throw IOException("dropped")
            }
        }
        val (channels, incomplete) = LiveTvStalker(http).channels(LiveTvStalkerSettings("http://p", "M"))
        assertEquals(listOf("a", "b", "c"), channels.map { it.name })
        assertTrue(incomplete)
        // Page 3 was asked for twice (one retry).
        assertEquals(2, http.requests.count { "p=3" in it.first })
    }

    @Test
    fun hostsNeverIncludeUsersPortsOrQueries() {
        assertEquals("h.example", liveTvHost("http://user:pass@h.example:8080/get.php?password=x"))
        assertEquals("", liveTvHost("playlist.m3u"))
        assertTrue(sameHost("http://A.example/x", "https://a.example:81/c"))
        assertFalse(sameHost("http://a.example/x", "http://b.example/c"))
    }
}
