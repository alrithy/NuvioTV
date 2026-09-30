package com.nuvio.tv.fork.livetv

import android.util.Log
import java.io.File
import java.io.IOException
import java.util.zip.Deflater
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.GzipSink
import okio.GzipSource
import okio.buffer
import okio.sink

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

    /**
     * Saves [url] to [target] gzip-compressed (as sent when the server already gzipped it, else
     * compressed quickly on the way), so a 100+ MB guide takes a few MB on the TV's storage. The old
     * file stays until the new one is complete (G10c; Reshaped `LiveTvHttp.download` @ 0ccf049).
     * Some panels build their guide on request and send nothing for a minute or more.
     */
    suspend fun download(url: String, headers: Map<String, String>, target: File) {
        withContext(Dispatchers.IO) {
            val http = client.newBuilder().readTimeout(GUIDE_READ_TIMEOUT_S, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(url).apply {
                headers.forEach { (name, value) -> header(name, value) }
            }.build()
            target.parentFile?.mkdirs()
            val temp = File(target.path + ".part")
            val call = http.newCall(request)
            val cancelOnCompletion = coroutineContext.job.invokeOnCompletion { call.cancel() }
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val source = response.body?.source() ?: throw IOException("empty response")
                    if (source.startsWithGzipMagic()) {
                        temp.sink().buffer().use { it.writeAll(source) }
                    } else {
                        // Lowest compression: XML still shrinks about tenfold, at little CPU on a weak TV.
                        val gzip = GzipSink(temp.sink()).apply { deflater.setLevel(Deflater.BEST_SPEED) }
                        gzip.buffer().use { it.writeAll(source) }
                    }
                }
                if (!temp.renameTo(target) && !(target.delete() && temp.renameTo(target))) {
                    throw IOException("guide not saved")
                }
            } finally {
                cancelOnCompletion.dispose()
                temp.delete()
            }
        }
    }

    companion object {
        /** How long a guide download may wait for data. */
        const val GUIDE_READ_TIMEOUT_S = 120L

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
