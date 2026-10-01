package com.nuvio.tv.fork.watchparty

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G11a: room codes, what a host may share (243, 244) and what a guest accepts. */
class WatchPartySharePolicyTest {

    @Test
    fun codesAreSixReadableCharactersAndNormaliseFromTyping() {
        val code = WatchPartyProtocol.generateCode(SecureRandom())
        assertTrue(Regex("^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$").matches(code))
        assertNotEquals(code, WatchPartyProtocol.generateCode())
        assertEquals("ABC234", WatchPartyProtocol.normalizeCode(" abc 234 "))
        assertEquals("ABC234", WatchPartyProtocol.normalizeCode("abc-234"))
        assertNull(WatchPartyProtocol.normalizeCode("ABC23"))
        assertNull(WatchPartyProtocol.normalizeCode("ABC0O1")) // look-alikes are not in the alphabet
        // Kept exactly for the Nuvio Party phone build (240).
        assertEquals("nuviowatchpartyABC234", WatchPartyProtocol.roomFor("ABC234"))
        assertEquals("nuvio-wp-ABC234", WatchPartyProtocol.passwordFor("ABC234"))
    }

    @Test
    fun onlyPlayerHeadersAreSharedAndCredentialStreamsAreNotShareable() {
        val shared = WatchPartySharePolicy.decide(
            "https://cdn.example/v.mkv?exp=1",
            mapOf("User-Agent" to "UA", "Referer" to "https://site.example/", "X-Forwarded-For" to "1.2.3.4"),
            isTorrent = false,
            isLiveTv = false,
        )
        assertEquals(
            WatchPartyShareDecision.Shareable(mapOf("User-Agent" to "UA", "Referer" to "https://site.example/")),
            shared,
        )
        listOf("Authorization", "cookie", "X-Api-Key", "x-auth-token", "Proxy-Authorization", "X-Session-Id").forEach { header ->
            assertEquals(
                header,
                WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.CREDENTIALS),
                WatchPartySharePolicy.decide("https://cdn.example/v.mkv", mapOf(header to "secret"), isTorrent = false, isLiveTv = false),
            )
        }
        assertEquals(
            WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.CREDENTIALS),
            WatchPartySharePolicy.decide("https://user:pass@cdn.example/v.mkv", emptyMap(), isTorrent = false, isLiveTv = false),
        )
    }

    @Test
    fun torrentsLocalLinksLiveTvAndNonHttpAreNotShareable() {
        fun reason(url: String, torrent: Boolean = false, live: Boolean = false) =
            (WatchPartySharePolicy.decide(url, emptyMap(), torrent, live) as WatchPartyShareDecision.NotShareable).reason
        assertEquals(WatchPartyUnshareable.TORRENT, reason("magnet:?xt=urn:btih:abc", torrent = true))
        assertEquals(WatchPartyUnshareable.TORRENT, reason("http://127.0.0.1:8080/stream", torrent = true))
        assertEquals(WatchPartyUnshareable.LOCAL, reason("http://127.0.0.1:11470/x.mkv"))
        assertEquals(WatchPartyUnshareable.LOCAL, reason("http://localhost/x.mkv"))
        assertEquals(WatchPartyUnshareable.LOCAL, reason("http://[::1]/x.mkv"))
        assertEquals(WatchPartyUnshareable.LIVE_TV, reason("http://provider.example/live/u/p/1.ts", live = true))
        assertEquals(WatchPartyUnshareable.NOT_HTTP, reason("file:///sdcard/x.mkv"))
        assertEquals(WatchPartyUnshareable.NOT_HTTP, reason("rtmp://cdn.example/x"))
    }

    @Test
    fun aGuestOpensOnlyAPublicLinkWithPlayerHeadersAndBoundedText() {
        val received = WatchPartyMedia(
            url = "https://cdn.example/v.mkv",
            headers = mapOf("Referer" to "https://site.example/", "Cookie" to "sid=1", "User-Agent" to "UA\r\nX: y"),
            title = "Film\u0007" + "x".repeat(400),
            poster = "javascript:alert(1)",
            season = -1,
        )
        val accepted = WatchPartySharePolicy.acceptReceived(received)!!
        assertEquals(mapOf("Referer" to "https://site.example/"), accepted.headers)
        assertEquals(WatchPartySharePolicy.MAX_TEXT_CHARS, accepted.title!!.length)
        assertTrue(accepted.title!!.none { it < ' ' })
        assertNull(accepted.poster)
        assertNull(accepted.season)
        assertNull(WatchPartySharePolicy.acceptReceived(received.copy(url = "http://127.0.0.1/x")))
        assertNull(WatchPartySharePolicy.acceptReceived(received.copy(url = "https://u:p@cdn.example/x")))
        assertNull(WatchPartySharePolicy.acceptReceived(received.copy(url = "content://media/1")))
        assertNull(WatchPartySharePolicy.acceptReceived(received.copy(url = "https://cdn.example/" + "a".repeat(9_000))))
    }

    @Test
    fun mediaNeverPrintsItsLinkOrHeaders() {
        val text = WatchPartyMedia(url = "https://cdn.example/secret-token", headers = mapOf("Referer" to "r"), contentId = "tt1").toString()
        assertTrue("secret-token" !in text && "Referer" !in text)
        assertEquals("Ann", WatchPartySharePolicy.peerName("  Ann\n"))
        assertNull(WatchPartySharePolicy.peerName(" \u0001 "))
        assertEquals(WatchPartySharePolicy.MAX_NAME_CHARS, WatchPartySharePolicy.peerName("n".repeat(100))!!.length)
    }

    @Test
    fun receivedLinksAreRememberedInMemoryOnlyAndBounded() {
        WatchPartyReceivedStreams.clearForTest()
        assertTrue(!WatchPartyReceivedStreams.contains("https://cdn.example/a"))
        WatchPartyReceivedStreams.register("https://cdn.example/a")
        assertTrue(WatchPartyReceivedStreams.contains("https://cdn.example/a"))
        (1..8).forEach { WatchPartyReceivedStreams.register("https://cdn.example/$it") }
        assertTrue(!WatchPartyReceivedStreams.contains("https://cdn.example/a"))
        assertTrue(WatchPartyReceivedStreams.contains("https://cdn.example/8"))
        assertTrue(!WatchPartyReceivedStreams.contains(null))
        WatchPartyReceivedStreams.clearForTest()
    }
}
