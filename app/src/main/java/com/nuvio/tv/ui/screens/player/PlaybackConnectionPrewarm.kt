package com.nuvio.tv.ui.screens.player

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.nuvio.tv.NuvioApplication
import com.nuvio.tv.fork.playback.ConnectionPrewarmPolicy
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * G4b (feature 18): press-time warm-up for the parallel REMUX path, ported from ysosrs 45e0984
 * `PlayerPlaybackNetworking.prewarmPlaybackConnection`. The caller decides whether to run it
 * ([ConnectionPrewarmPolicy.shouldPrewarm]).
 *
 * Fire-and-forget: nothing awaits it and every failure is swallowed, so a miss leaves the first
 * open to connect exactly as official does. Two requests run concurrently: the head window
 * (stored for `ParallelRangeDataSource`'s bootstrap read) and a suffix-range tail window; a
 * range-hostile tail falls back to an explicit tail range once the head gave the total. Both use
 * the official shared connection pool, so the sockets they open are the ones the player reuses.
 */
internal object PlaybackConnectionPrewarm {
    private const val TAG = "NuvioPrewarm"

    @Volatile private var lastUrl: String? = null
    @Volatile private var lastAtMs: Long = 0L

    /** Same base client, cookie jar and pool setup as `PlayerMediaSourceFactory`'s parallel path. */
    private val client: OkHttpClient by lazy {
        PlayerPlaybackNetworking.playbackHttpClient.newBuilder()
            .cookieJar(NuvioApplication.extensionCookieJar)
            .let { NuvioExoPlayerPerformanceHelper.applyNetworkOptimizations(it) }
            .build()
    }

    fun prewarm(url: String, headers: Map<String, String>?, warmTail: Boolean) {
        val target = url.trim()
        val nowMs = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (ConnectionPrewarmPolicy.isDuplicate(target, nowMs, lastUrl, lastAtMs)) return
            lastUrl = target
            lastAtMs = nowMs
        }
        Log.i(TAG, "PREWARM host=${Uri.parse(target).host} tail=$warmTail")
        if (warmTail) enqueueSuffixTail(target, headers)
        val headRequest = request(target, headers, "bytes=0-${ConnectionPrewarmPolicy.HEAD_WINDOW_BYTES - 1}") ?: return
        enqueue(headRequest) { response ->
            if (response.code != 206) return@enqueue
            val bytes = runCatching { response.body?.bytes() }.getOrNull() ?: return@enqueue
            val total = ConnectionPrewarmPolicy.contentRangeTotal(response.header("Content-Range"))
            if (!ConnectionPrewarmPolicy.isStorableHead(bytes.size, total)) return@enqueue
            PrefetchWindowStore.putHead(entry(target, response, 0L, total, total, bytes))
            if (warmTail) enqueueFallbackTail(target, headers, total)
        }
    }

    private fun enqueueSuffixTail(target: String, headers: Map<String, String>?) {
        val window = PrefetchWindowStore.TAIL_WINDOW_BYTES
        val tailRequest = request(target, headers, "bytes=-$window") ?: return
        enqueue(tailRequest) { response ->
            if (response.code != 206) return@enqueue
            val contentRange = response.header("Content-Range")
            val start = ConnectionPrewarmPolicy.contentRangeStart(contentRange)
            val total = ConnectionPrewarmPolicy.contentRangeTotal(contentRange)
            val bytes = runCatching { response.body?.bytes() }.getOrNull() ?: return@enqueue
            if (!ConnectionPrewarmPolicy.isStorableTail(start, bytes.size.toLong(), total, window)) return@enqueue
            PrefetchWindowStore.putTail(entry(target, response, start, window, total, bytes))
        }
    }

    private fun enqueueFallbackTail(target: String, headers: Map<String, String>?, total: Long) {
        if (PrefetchWindowStore.hasFreshTail(Uri.parse(target))) return
        val window = PrefetchWindowStore.TAIL_WINDOW_BYTES
        val start = ConnectionPrewarmPolicy.fallbackTailStart(total, window) ?: return
        val tailRequest = request(target, headers, "bytes=$start-${total - 1}") ?: return
        enqueue(tailRequest) { response ->
            if (response.code != 206) return@enqueue
            val bytes = runCatching { response.body?.bytes() }.getOrNull() ?: return@enqueue
            if (!ConnectionPrewarmPolicy.isStorableTail(start, bytes.size.toLong(), total, window)) return@enqueue
            PrefetchWindowStore.putTail(entry(target, response, start, window, total, bytes))
        }
    }

    /** Range is set last so a caller-supplied Range header can never widen the warm-up. */
    private fun request(target: String, headers: Map<String, String>?, range: String): Request? = runCatching {
        Request.Builder().url(target).apply {
            headers?.forEach { (name, value) -> header(name, value) }
            header("Range", range)
        }.build()
    }.getOrNull()

    private fun enqueue(request: Request, onResponse: (Response) -> Unit) {
        runCatching {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit

                override fun onResponse(call: Call, response: Response) {
                    response.use { runCatching { onResponse(it) } }
                }
            })
        }
    }

    private fun entry(
        target: String,
        response: Response,
        start: Long,
        openLength: Long,
        total: Long,
        bytes: ByteArray,
    ) = ParallelRangeDataSource.BootstrapCacheEntry(
        requestUri = Uri.parse(target),
        startPosition = start,
        resolvedUri = Uri.parse(response.request.url.toString()),
        openLength = openLength,
        totalFileLength = total,
        bootstrapData = bytes,
        bootstrapSize = bytes.size,
        createdAtUptimeMs = SystemClock.uptimeMillis()
    )
}
