package com.nuvio.tv.fork.skip

import com.squareup.moshi.Moshi
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test

class SkipHttpCallTest {
    @Test
    fun `timeout cancels a request waiting for headers`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val call = OkHttpClient().newCall(Request.Builder().url(server.url("/")).build())
            val operation = async(Dispatchers.IO) { call.readSkipBody(1024) }
            assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
            withTimeout(2000) { operation.cancelAndJoin() }
            assertTrue(call.isCanceled())
        }
    }

    @Test
    fun `cancellation also closes a stalled response body`() = runBlocking {
        MockWebServer().use { server ->
            val headers = CountDownLatch(1)
            server.enqueue(MockResponse().setBody("body").setBodyDelay(3, TimeUnit.SECONDS))
            val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                override fun responseHeadersEnd(call: Call, response: Response) { headers.countDown() }
            }).build()
            val call = client.newCall(Request.Builder().url(server.url("/")).build())
            val operation = async(Dispatchers.IO) { call.readSkipBody(1024) }
            assertTrue(withContext(Dispatchers.IO) { headers.await(5, TimeUnit.SECONDS) })
            withTimeout(2000) { operation.cancelAndJoin() }
            assertTrue(call.isCanceled())
        }
    }

    @Test
    fun `oversized chunked bodies and HTTP failures fail closed`() = runBlocking {
        MockWebServer().use { server ->
            val client = OkHttpClient()
            server.enqueue(MockResponse().setChunkedBody("12345", 2))
            server.enqueue(MockResponse().setResponseCode(403).setBody("secret error"))
            repeat(2) {
                assertNull(client.newCall(Request.Builder().url(server.url("/")).build()).readSkipBody(4))
            }
        }
    }

    @Test
    fun `stalled provider stops at six seconds while successful reports survive`() = runBlocking {
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = if (request.path == "/slow") {
                    MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
                } else {
                    MockResponse().setBody("""[{"intro":[{"start_ms":1000,"end_ms":3000}]}]""")
                }
            }
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                val path = if (request.url.host == "api.theintrodb.org") "/slow" else "/fast"
                chain.proceed(request.newBuilder().url(server.url(path)).build())
            }.build()
            val providers = ForkSkipProviders(client, Moshi.Builder().build(), mockk(relaxed = true))
            val config = SkipProviderConfig(enabled = setOf(ForkSkipProvider.SKIP_ME, ForkSkipProvider.THE_INTRO_DB))
            val reports = withTimeout(FORK_SKIP_PROVIDER_TIMEOUT_MS + 2000) {
                providers.fetch(config, "tt123", null, null, true, 60_000)
            }
            assertTrue(reports.any { it.provider == "skipme" && it.startTime == 1.0 })
            assertEquals(2, server.requestCount)
        }
    }
}
