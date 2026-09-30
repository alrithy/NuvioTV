package com.nuvio.tv.fork.livetv

import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.GzipSource
import okio.buffer

/** What Live TV reads playlists, provider APIs and guides through; fixtures replace it in tests. */
internal interface LiveTvFetcher {
    /** Opens [url] and hands [block] the body, un-gzipped when the server sent a gzip file. */
    suspend fun <T> stream(url: String, headers: Map<String, String>, block: (BufferedSource) -> T): T
}

/** A small response (provider API calls) as text, at most [LIVE_TV_MAX_TEXT_BYTES]. */
internal suspend fun LiveTvFetcher.text(url: String, headers: Map<String, String>): String =
    stream(url, headers) { source ->
        if (source.request(LIVE_TV_MAX_TEXT_BYTES + 1)) throw IOException("response too large")
        source.readUtf8()
    }

internal const val LIVE_TV_MAX_TEXT_BYTES = 1_000_000L

/**
 * Live TV's own HTTP client (Reshaped `LiveTvHttp` @ 0ccf049, adapted): playlists, provider APIs and
 * guides, never playback. It is deliberately not the app client: that one logs request URLs in debug
 * builds, and Xtream / Stalker / many M3U URLs carry the account's credentials (D055). Cancelling the
 * coroutine cancels the call, so a stalled server never holds a thread.
 */
internal class LiveTvHttp(private val client: OkHttpClient = defaultClient) : LiveTvFetcher {

    override suspend fun <T> stream(url: String, headers: Map<String, String>, block: (BufferedSource) -> T): T =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).apply {
                headers.forEach { (name, value) -> header(name, value) }
            }.build()
            val call = client.newCall(request)
            val cancelOnCompletion = coroutineContext.job.invokeOnCompletion { call.cancel() }
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val source = response.body?.source() ?: throw IOException("empty response")
                    block(if (source.startsWithGzipMagic()) GzipSource(source).buffer() else source)
                }
            } finally {
                cancelOnCompletion.dispose()
            }
        }

    companion object {
        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}

internal fun BufferedSource.startsWithGzipMagic(): Boolean =
    request(2) && buffer[0] == 0x1f.toByte() && buffer[1] == 0x8b.toByte()

/**
 * Live TV logging: what failed and the host, never a URL, a header or exception text (an OkHttp
 * message can hold the full request URL with the account's password).
 */
internal object LiveTvLog {
    private const val TAG = "LiveTv"

    fun warn(what: String, url: String?, error: Throwable? = null) {
        val cause = error?.let { " (${it.javaClass.simpleName})" }.orEmpty()
        runCatching { Log.w(TAG, "$what: ${liveTvHost(url).ifBlank { "local" }}$cause") }
    }
}
