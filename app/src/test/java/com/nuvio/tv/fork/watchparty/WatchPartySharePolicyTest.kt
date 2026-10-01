package com.nuvio.tv.fork.watchparty

import java.net.InetAddress
import java.net.UnknownHostException
import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G11a: room codes, what a host may share (243, 244) and what a guest accepts; G11 corrections: non-public hosts, tickets. */
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
        WatchPartyReceivedStreams.register(WatchPartyMedia(url = "https://cdn.example/a"))
        assertTrue(WatchPartyReceivedStreams.contains("https://cdn.example/a"))
        (1..8).forEach { WatchPartyReceivedStreams.register(WatchPartyMedia(url = "https://cdn.example/$it")) }
        assertTrue(!WatchPartyReceivedStreams.contains("https://cdn.example/a"))
        assertTrue(WatchPartyReceivedStreams.contains("https://cdn.example/8"))
        assertTrue(!WatchPartyReceivedStreams.contains(null))
        WatchPartyReceivedStreams.clearForTest()
    }

    @Test
    fun theRouteCarriesATicketThatOpensOnceAndNeverTheLink() {
        WatchPartyReceivedStreams.clearForTest()
        val media = WatchPartyMedia(url = "https://cdn.example/signed?sig=abc", headers = mapOf("Referer" to "https://site.example/"))
        val ticket = WatchPartyReceivedStreams.register(media)
        assertTrue(ticket.startsWith(WatchPartyReceivedStreams.TICKET_PREFIX))
        assertTrue("cdn.example" !in ticket && "sig" !in ticket)
        assertNotEquals(ticket, WatchPartyReceivedStreams.register(media))
        // An ordinary link is not a ticket.
        assertNull(WatchPartyReceivedStreams.redeem("https://cdn.example/v.mkv"))
        assertEquals(WatchPartyReceivedStreams.Redeemed(media.url, media.headers), WatchPartyReceivedStreams.redeem(ticket))
        // Once only, and after leaving (or a recreated process) a ticket opens nothing.
        assertEquals("", WatchPartyReceivedStreams.redeem(ticket)!!.url)
        val other = WatchPartyReceivedStreams.register(media)
        WatchPartyReceivedStreams.clearTickets()
        assertEquals(WatchPartyReceivedStreams.Redeemed("", emptyMap()), WatchPartyReceivedStreams.redeem(other))
        // The no-reuse memory stays for links still playing.
        assertTrue(WatchPartyReceivedStreams.contains(media.url))
        assertTrue("sig" !in WatchPartyReceivedStreams.Redeemed(media.url, media.headers).toString())
        WatchPartyReceivedStreams.clearForTest()
    }

    @Test
    fun privateLinkLocalAndOtherNonPublicHostsAreNeverSharedOrOpened() {
        listOf(
            "10.0.0.2", "172.16.4.1", "172.31.255.255", "192.168.1.1", "169.254.169.254", "100.64.0.1",
            "0.0.0.0", "127.1.2.3", "224.0.0.1", "255.255.255.255", "198.18.0.1",
            "[fe80::1]", "[fd00::1]", "[fc12::1]", "[::]", "[::1]", "[ff02::1]", "[::ffff:192.168.0.1]", "[64:ff9b::a00:1]",
            "0x7f.1", "2130706433", "017.0.0.1", "127.1", "nas", "router.lan", "printer.local", "box.home.arpa", "svc.internal",
        ).forEach { host ->
            // LOCAL, or NOT_HTTP where java.net.URI already refuses the name as a host (0x7f.1).
            val decision = WatchPartySharePolicy.decide("http://$host/x.mkv", emptyMap(), isTorrent = false, isLiveTv = false)
            assertTrue(host, decision is WatchPartyShareDecision.NotShareable &&
                decision.reason in setOf(WatchPartyUnshareable.LOCAL, WatchPartyUnshareable.NOT_HTTP))
            assertTrue(host, WatchPartySharePolicy.isLocal(host.trim('[', ']')))
            assertNull(host, WatchPartySharePolicy.acceptReceived(WatchPartyMedia(url = "http://$host/x.mkv")))
        }
        listOf("8.8.8.8", "172.32.0.1", "100.128.0.1", "[2606:4700::1111]", "cdn.example", "1.example").forEach { host ->
            assertTrue(host, WatchPartySharePolicy.decide("https://$host/x.mkv", emptyMap(), false, false) is WatchPartyShareDecision.Shareable)
        }
    }

    @Test
    fun aGuestOpensANameOnlyWhenEveryAddressItResolvesToIsPublic() {
        fun lookup(vararg ips: String): (String) -> List<InetAddress> = { ips.map(InetAddress::getByName) }
        assertTrue(WatchPartySharePolicy.resolvesToPublic("cdn.example", lookup("93.184.216.34", "2606:2800:220:1::1")))
        assertFalse(WatchPartySharePolicy.resolvesToPublic("rebind.example", lookup("93.184.216.34", "192.168.1.1")))
        assertFalse(WatchPartySharePolicy.resolvesToPublic("meta.example", lookup("169.254.169.254")))
        assertFalse(WatchPartySharePolicy.resolvesToPublic("none.example") { emptyList() })
        assertFalse(WatchPartySharePolicy.resolvesToPublic("fail.example") { throw UnknownHostException() })
        // A local name is refused before any lookup.
        assertFalse(WatchPartySharePolicy.resolvesToPublic("localhost") { error("no lookup") })
    }
}
