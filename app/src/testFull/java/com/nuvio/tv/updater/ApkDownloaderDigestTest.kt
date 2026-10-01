package com.nuvio.tv.updater

import com.nuvio.tv.fork.distribution.ReleaseDigest
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G14b (306): a downloaded update reaches the installer only when its SHA-256 is the published one. */
class ApkDownloaderDigestTest {
    private val body = "not really an apk"
    private val published = ReleaseDigest.hex(ReleaseDigest.newDigest().digest(body.toByteArray()))

    @Test
    fun aMatchingDownloadIsKept() = withServer { url, target ->
        val result = ApkDownloader(OkHttpClient()).download(url, target, published) { _, _ -> }
        assertEquals(target, result.getOrThrow())
        assertEquals(body, target.readText())
    }

    @Test
    fun aMismatchedDownloadIsDeleted() = withServer { url, target ->
        val other = ReleaseDigest.hex(ReleaseDigest.newDigest().digest("tampered".toByteArray()))
        val result = ApkDownloader(OkHttpClient()).download(url, target, other) { _, _ -> }
        assertTrue(result.exceptionOrNull() is ReleaseDigest.UnverifiedUpdate)
        assertFalse(target.exists())
    }

    @Test
    fun aReleaseWithoutADigestIsRefused() = withServer { url, target ->
        val result = ApkDownloader(OkHttpClient()).download(url, target, null) { _, _ -> }
        assertTrue(result.exceptionOrNull() is ReleaseDigest.UnverifiedUpdate)
        assertFalse(target.exists())
    }

    private fun withServer(block: suspend (String, File) -> Unit) {
        val dir = Files.createTempDirectory("g14b").toFile()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(body))
            server.start()
            runBlocking { block(server.url("/app.apk").toString(), File(dir, "update.apk")) }
        }
        dir.deleteRecursively()
    }
}
