package com.nuvio.tv.fork.security

import com.nuvio.tv.core.logging.urlForLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** G14a (305): only scheme, host and port of a URL reach a log line. */
class LogRedactionTest {
    @Test
    fun pathQueryAndUserInfoAreDropped() {
        assertEquals(
            "https://torrentio.strem.fun",
            "https://torrentio.strem.fun/realdebrid=ABC123SECRET|sort=size/manifest.json".urlForLog(),
        )
        assertEquals("https://aio.example:8443", "https://aio.example:8443/stremio/eyJrZXkiOiJzZWNyZXQifQ/stream?x=1".urlForLog())
        assertEquals("http://cdn.example", "http://user:pass@CDN.example/video.mkv?token=abc#t=10".urlForLog())
        assertEquals("https://h.example", "https://h.example?apikey=1".urlForLog())
        assertEquals("https://h.example", "  HTTPS://h.example/path  ".urlForLog())
    }

    @Test
    fun nonUrlsAreHiddenWhole() {
        assertEquals("(null)", null.urlForLog())
        assertEquals("(blank)", " ".urlForLog())
        assertEquals("(redacted)", "AbC12xy".urlForLog())
        assertEquals("(redacted)", "/sdcard/movie.mkv".urlForLog())
        assertEquals("magnet:(redacted)", "magnet:?xt=urn:btih:abcdef&tr=udp://tracker".urlForLog())
        assertEquals("content:(redacted)", "content:/media/1".urlForLog())
        assertEquals("file://(redacted)", "file:///data/user/0/app/sub.srt".urlForLog())
        assertEquals("(redacted)", "1http://x".urlForLog())
    }

    @Test
    fun noSecretSurvives() {
        val secrets = listOf("ABC123SECRET", "token=abc", "pass", "eyJrZXki")
        val logged = listOf(
            "https://a.example/ABC123SECRET/manifest.json",
            "https://b.example/x?token=abc",
            "https://user:pass@c.example/",
            "https://d.example/eyJrZXki/stream",
        ).map { it.urlForLog() }
        logged.forEach { line -> secrets.forEach { assertFalse(line, line.contains(it)) } }
    }
}
