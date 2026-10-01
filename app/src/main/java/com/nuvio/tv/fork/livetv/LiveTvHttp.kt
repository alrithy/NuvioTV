package com.nuvio.tv.fork.livetv

import android.util.Log
import java.io.File
import java.io.IOException
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import com.nuvio.tv.fork.resource.LiveTvGuideBudget
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.GzipSink
import okio.GzipSource
import okio.buffer
import okio.sink
import okio.Buffer
import okio.ForwardingSink
import okio.ForwardingSource
import okio.Source
import okio.Sink

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
            val cancelOnLeave = launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val source = response.body?.source() ?: throw IOException("empty response")
                    block(if (source.startsWithGzipMagic()) GzipSource(source).buffer() else source)
                }
            } catch (error: Exception) {
                // Closing a cancelled socket throws IOException; preserve coroutine cancellation
                // so callers do not treat leaving/profile changes as a provider failure.
                ensureActive()
                throw error
            } finally {
                cancelOnLeave.cancel()
            }
        }

    /**
     * Saves [url] to [target] gzip-compressed (as sent when the server already gzipped it, else
     * compressed quickly on the way), so a 100+ MB guide takes a few MB on the TV's storage. The old
     * file stays until the new one is complete (G10c; Reshaped `LiveTvHttp.download` @ 0ccf049).
     * Some panels build their guide on request and send nothing for a minute or more.
     */
    suspend fun download(url: String, headers: Map<String, String>, target: File, budget: LiveTvGuideBudget) {
        withContext(Dispatchers.IO) {
            val http = client.newBuilder().readTimeout(GUIDE_READ_TIMEOUT_S, TimeUnit.SECONDS)
                .callTimeout(budget.downloadTimeoutMs, TimeUnit.MILLISECONDS).build()
            val request = Request.Builder().url(url).apply {
                headers.forEach { (name, value) -> header(name, value) }
            }.build()
            target.parentFile?.mkdirs()
            // An interrupted old read must never delete a new generation's partial download.
            val temp = File.createTempFile(target.name + ".", ".part", target.parentFile)
            val call = http.newCall(request)
            // Completion handlers run after blocking IO returns. A cancelled child closes the
            // socket immediately, including while waiting for headers or reading the body.
            val cancelOnLeave = launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val source = response.body?.source() ?: throw IOException("empty response")
                    val sink = GuideBudgetSink(temp.sink(), budget.compressedBytes)
                    if (source.startsWithGzipMagic()) {
                        sink.buffer().use { it.writeAll(GuideBudgetSource(source, budget.compressedBytes)) }
                    } else {
                        // Lowest compression: XML still shrinks about tenfold, at little CPU on a weak TV.
                        val gzip = GzipSink(sink).apply { deflater.setLevel(Deflater.BEST_SPEED) }
                        gzip.buffer().use { it.writeAll(GuideBudgetSource(source, budget.expandedBytes)) }
                    }
                }
                // A small gzip file can expand far beyond its disk size. Validate before replacing
                // the old guide; cancellation is checked even while inflating a local file.
                GZIPInputStream(temp.inputStream().buffered()).use { input ->
                    val bytes = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        ensureActive()
                        val read = input.read(bytes)
                        if (read < 0) break
                        total += read
                        if (total > budget.expandedBytes) throw IOException("guide too large")
                    }
                }
                ensureActive()
                if (!temp.renameTo(target)) {
                    throw IOException("guide not saved")
                }
            } catch (error: Exception) {
                ensureActive()
                throw error
            } finally {
                cancelOnLeave.cancel()
                temp.delete()
            }
        }
    }

    companion object {
        /** How long a guide download may wait for data. */
        const val GUIDE_READ_TIMEOUT_S = 120L

        /**
         * Live TV's own client (no logging interceptor, unlike the app's in debug builds), shared with
         * the list's channel preview (G10f) so a preview never touches Nuvio's player networking.
         */
        internal val sharedClient: OkHttpClient get() = defaultClient

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

/** Bounded streaming IO: no whole guide allocation, including unknown-length/chunked bodies. */
internal class GuideBudgetSource(source: Source, private val maxBytes: Long) : ForwardingSource(source) {
    private var bytes = 0L
    override fun read(sink: Buffer, byteCount: Long): Long {
        val read = super.read(sink, minOf(byteCount, maxBytes - bytes + 1))
        if (read > 0) {
            bytes += read
            if (bytes > maxBytes) throw IOException("guide too large")
        }
        return read
    }
}

private class GuideBudgetSink(sink: Sink, private val maxBytes: Long) : ForwardingSink(sink) {
    private var bytes = 0L
    override fun write(source: Buffer, byteCount: Long) {
        if (byteCount > maxBytes - bytes) throw IOException("guide too large")
        super.write(source, byteCount)
        bytes += byteCount
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
