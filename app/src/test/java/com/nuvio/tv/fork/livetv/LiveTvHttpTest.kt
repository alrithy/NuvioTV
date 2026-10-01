package com.nuvio.tv.fork.livetv

import com.nuvio.tv.fork.resource.LiveTvGuideBudget
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test

class LiveTvHttpTest {
    private val budget = LiveTvGuideBudget(1L shl 20, 4L shl 20)
    @Test
    fun `leaving cancels a playlist stalled before headers`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val cancelled = CountDownLatch(1)
            val client = client(cancelled)
            val operation = async { LiveTvHttp(client).stream(server.url("/").toString(), emptyMap()) { it.readUtf8() } }
            assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
            withTimeout(2_000) { operation.cancelAndJoin() }
            assertEquals(0L, cancelled.count)
        }
    }

    @Test
    fun `cancelled guide body keeps the old file and removes its partial download`() = runBlocking {
        val dir = Files.createTempDirectory("live-guide").toFile()
        try {
            MockWebServer().use { server ->
                val headers = CountDownLatch(1)
                val cancelled = CountDownLatch(1)
                server.enqueue(MockResponse().setBody("<tv/>").setBodyDelay(3, TimeUnit.SECONDS))
                val target = java.io.File(dir, "guide.gz").apply { writeText("old guide") }
                val operation = async { LiveTvHttp(client(cancelled, headers)).download(server.url("/").toString(), emptyMap(), target, budget) }
                assertTrue(withContext(Dispatchers.IO) { headers.await(5, TimeUnit.SECONDS) })
                withTimeout(2_000) { operation.cancelAndJoin() }
                assertEquals(0L, cancelled.count)
                assertEquals("old guide", target.readText())
                assertEquals(listOf(target.name), dir.listFiles()!!.map { it.name })
            }
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun `completed guide replaces the old file with readable gzip`() = runBlocking {
        val dir = Files.createTempDirectory("live-guide").toFile()
        try {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody("<tv>new guide</tv>"))
                val target = java.io.File(dir, "guide.gz").apply { writeText("old") }
                LiveTvHttp().download(server.url("/").toString(), emptyMap(), target, budget)
                assertEquals("<tv>new guide</tv>", GZIPInputStream(target.inputStream()).bufferedReader().use { it.readText() })
                assertEquals(listOf(target.name), dir.listFiles()!!.map { it.name })
            }
        } finally { dir.deleteRecursively() }
    }

    private fun client(cancelled: CountDownLatch, headers: CountDownLatch? = null) =
        OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun canceled(call: Call) { cancelled.countDown() }
            override fun responseHeadersEnd(call: Call, response: Response) { headers?.countDown() }
        }).build()

    @Test
    fun `oversized chunked guide leaves the saved guide intact`() = runBlocking {
        rejectedGuide(MockResponse().setChunkedBody("x".repeat(4_096), 32), LiveTvGuideBudget(1_024, 128))
    }

    @Test
    fun `small gzip exceeding expanded budget leaves the saved guide intact`() = runBlocking {
        val bytes = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(bytes).use { it.write("x".repeat(4_096).toByteArray()) }
        rejectedGuide(MockResponse().setBody(okio.Buffer().write(bytes.toByteArray())), LiveTvGuideBudget(1_024, 128))
    }

    @Test
    fun `total timeout bounds a guide server that never completes`() = runBlocking {
        withTimeout(3_000) {
            rejectedGuide(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE), LiveTvGuideBudget(1_024, 4_096, 200))
        }
    }

    private suspend fun rejectedGuide(response: MockResponse, limits: LiveTvGuideBudget) {
        val dir = Files.createTempDirectory("live-guide-limit").toFile()
        try {
            MockWebServer().use { server ->
                server.enqueue(response)
                val target = java.io.File(dir, "guide.gz").apply { writeText("old") }
                try {
                    LiveTvHttp().download(server.url("/").toString(), emptyMap(), target, limits)
                    fail("oversized or stalled guide was accepted")
                } catch (_: java.io.IOException) { }
                assertEquals("old", target.readText())
                assertEquals(listOf(target.name), dir.listFiles()!!.map { it.name })
            }
        } finally { dir.deleteRecursively() }
    }
}
