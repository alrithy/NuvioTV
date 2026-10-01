package com.nuvio.tv.fork.security

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G14a (303, 304): QR key → session cookie, no session → refused, foreign Origin → refused. */
class LocalServerAccessTest {
    private val access = LocalServerAccess("0123456789abcdef0123456789abcdef")
    private val host = "192.168.1.20:8080"
    private val cookie = "${access.cookieName}=${access.key}"

    @Test
    fun theQrLinkOpensASessionAndHidesTheKey() {
        assertEquals("http://$host/?k=${access.key}", access.entryUrl("http://$host"))
        assertEquals(LocalServerDecision.ENTER, decide("GET", keyParam = access.key))
        val headers = access.entryHeaders("/")
        assertEquals("/", headers["Location"])
        val setCookie = headers.getValue("Set-Cookie")
        assertTrue(setCookie.startsWith("$cookie;"))
        assertTrue(setCookie.contains("HttpOnly"))
        assertTrue(setCookie.contains("SameSite=Lax"))
        assertEquals("no-referrer", headers["Referrer-Policy"])
    }

    @Test
    fun withoutTheSessionNothingIsServed() {
        assertEquals(LocalServerDecision.DENY_SESSION, decide("GET"))
        assertEquals(LocalServerDecision.DENY_SESSION, decide("GET", keyParam = "0123456789abcdef0123456789abcdee"))
        assertEquals(LocalServerDecision.DENY_SESSION, decide("GET", cookie = "${access.cookieName}=wrong"))
        assertEquals(LocalServerDecision.DENY_SESSION, decide("GET", cookie = "other=${access.key}"))
        assertEquals("a key in the query never authorizes a write", LocalServerDecision.DENY_SESSION, decide("POST", keyParam = access.key))
    }

    @Test
    fun sessionRequestsAreServedAndCookiesAreParsed() {
        assertEquals(LocalServerDecision.ALLOW, decide("GET", cookie = cookie))
        assertEquals(LocalServerDecision.ALLOW, decide("GET", cookie = "theme=dark; $cookie; x=1"))
        assertEquals(LocalServerDecision.ALLOW, decide("POST", cookie = cookie, origin = "http://$host"))
        assertEquals("no Origin or Referer (curl)", LocalServerDecision.ALLOW, decide("POST", cookie = cookie))
    }

    @Test
    fun writesFromAnotherSiteAreRefused() {
        assertEquals(LocalServerDecision.DENY_ORIGIN, decide("POST", cookie = cookie, origin = "https://evil.example"))
        assertEquals(LocalServerDecision.DENY_ORIGIN, decide("POST", cookie = cookie, origin = "null"))
        assertEquals(LocalServerDecision.DENY_ORIGIN, decide("POST", cookie = cookie, referer = "https://evil.example/page"))
        assertEquals(LocalServerDecision.DENY_ORIGIN, decide("POST", cookie = cookie, referer = "http://$host.evil.example/"))
        assertEquals(LocalServerDecision.ALLOW, decide("POST", cookie = cookie, referer = "http://$host/"))
        assertEquals(LocalServerDecision.DENY_ORIGIN, decide("DELETE", cookie = cookie, origin = "http://other:8080", host = host))
    }

    @Test
    fun keysAreRandomAndCookiesDoNotCollideAcrossServers() {
        val a = LocalServerAccess()
        val b = LocalServerAccess()
        assertEquals(32, a.key.length)
        assertTrue(a.key.all { it in '0'..'9' || it in 'a'..'f' })
        assertNotEquals(a.key, b.key)
        assertNotEquals(a.cookieName, b.cookieName)
    }

    @Test
    fun theNanoHttpdGateAnswersOnlyWhatItMust() {
        val enter = access.gate(FakeSession("GET", "/", mapOf("host" to host), mapOf("k" to listOf(access.key))))!!
        assertEquals(NanoHTTPD.Response.Status.REDIRECT_SEE_OTHER, enter.status)
        assertEquals("/", enter.getHeader("Location"))
        assertTrue(enter.getHeader("Set-Cookie").startsWith("$cookie;"))

        val denied = access.gate(FakeSession("GET", "/api/addons", mapOf("host" to host)))!!
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN, denied.status)

        assertNull(access.gate(FakeSession("GET", "/api/addons", mapOf("host" to host, "cookie" to cookie))))
        val crossSite = access.gate(
            FakeSession("POST", "/api/addons", mapOf("host" to host, "cookie" to cookie, "origin" to "https://evil.example")),
        )!!
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN, crossSite.status)
    }

    @Test
    fun theForkServersKeepTheirRules() {
        assertTrue(com.nuvio.tv.fork.subtitles.SubtitleFontImportPolicy.originAllowed(null, host))
        assertTrue(com.nuvio.tv.fork.subtitles.SubtitleFontImportPolicy.originAllowed("http://$host", host))
        assertTrue(!com.nuvio.tv.fork.livetv.LiveTvSetupPolicy.originAllowed("https://evil.example", host))
        assertEquals(32, com.nuvio.tv.fork.livetv.LiveTvSetupPolicy.newToken().length)
    }

    private fun decide(
        method: String,
        keyParam: String? = null,
        cookie: String? = null,
        origin: String? = null,
        referer: String? = null,
        host: String? = this.host,
    ) = access.decide(method, keyParam, cookie, origin, referer, host)

    private class FakeSession(
        private val method: String,
        private val uri: String,
        private val headers: Map<String, String>,
        private val parameters: Map<String, List<String>> = emptyMap(),
    ) : NanoHTTPD.IHTTPSession {
        override fun execute() = Unit
        override fun getCookies(): NanoHTTPD.CookieHandler? = null
        override fun getHeaders(): Map<String, String> = headers
        override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun getMethod(): NanoHTTPD.Method = NanoHTTPD.Method.valueOf(method)
        @Deprecated("Deprecated in NanoHTTPD")
        override fun getParms(): Map<String, String> = parameters.mapValues { it.value.first() }
        override fun getParameters(): Map<String, List<String>> = parameters
        override fun getQueryParameterString(): String? = null
        override fun getUri(): String = uri
        @Deprecated("Deprecated in NanoHTTPD")
        override fun parseBody(files: MutableMap<String, String>) = Unit
        override fun getRemoteIpAddress(): String = "192.168.1.30"
        override fun getRemoteHostName(): String = "phone"
    }
}
