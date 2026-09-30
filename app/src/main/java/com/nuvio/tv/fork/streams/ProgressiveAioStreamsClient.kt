package com.nuvio.tv.fork.streams

import android.util.Log
import com.nuvio.tv.data.mapper.toDomain
import com.nuvio.tv.data.remote.dto.StreamDto
import com.nuvio.tv.domain.model.Stream
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** One line from AIOStreams' opt-in Nuvio progressive stream endpoint. */
@JsonClass(generateAdapter = true)
data class ProgressiveStreamEnvelopeDto(
    @Json(name = "event") val event: String? = null,
    @Json(name = "complete") val complete: Boolean? = null,
    @Json(name = "streams") val streams: List<StreamDto>? = null,
)

/**
 * Reads AIOStreams' opt-in NDJSON endpoint line by line (G8a, features 147, 149, 151). FILE_PORT of
 * Cxsmo `StreamRepositoryImpl.fetchProgressiveStreams` @ 3e0d0fa. Every event carries the add-on's
 * cumulative list, handed to [fetch]'s callback as a snapshot that replaces the add-on's group.
 * Returns null whenever the endpoint is missing, fails, or ends without `complete`, so the caller
 * makes the official JSON request instead; a completed response (even empty) is authoritative.
 * Uses official's add-on HTTP client (`addonPermissive`); logs carry the host only.
 */
@Singleton
class ProgressiveAioStreamsClient @Inject constructor(
    @param:Named("addonPermissive") private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) {
    private val client: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            // Events arrive as slower sources finish; the overall call stays bounded.
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    private val adapter by lazy { moshi.adapter(ProgressiveStreamEnvelopeDto::class.java) }

    suspend fun fetch(
        addonBaseUrl: String,
        encodedType: String,
        encodedVideoId: String,
        addonName: String,
        addonLogo: String?,
        onSnapshot: suspend (List<Stream>) -> Unit,
    ): List<Stream>? = withContext(Dispatchers.IO) {
        val url = ProgressiveAioStreamsRules.progressiveUrl(addonBaseUrl, encodedType, encodedVideoId)
        val host = ProgressiveAioStreamsRules.logHost(url)
        val call = client.newCall(Request.Builder().url(url).header("Accept", "application/x-ndjson").build())
        // The blocking line read ignores coroutine cancellation; leaving the screen cancels the call.
        val cancelOnLeave = launch { try { awaitCancellation() } finally { call.cancel() } }
        try {
            readEvents(call, host, addonName, addonLogo, onSnapshot)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            ensureActive()
            Log.d(TAG, "progressive failed host=$host (${error.javaClass.simpleName}); official request")
            null
        } finally {
            cancelOnLeave.cancel()
        }
    }

    private suspend fun readEvents(
        call: Call,
        host: String,
        addonName: String,
        addonLogo: String?,
        onSnapshot: suspend (List<Stream>) -> Unit,
    ): List<Stream>? = call.execute().use { response ->
        if (!response.isSuccessful) {
            Log.d(TAG, "progressive unavailable host=$host status=${response.code}; official request")
            return null
        }
        val source = response.body?.source() ?: return null
        var latest = emptyList<Stream>()
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!ProgressiveAioStreamsRules.isEventLine(line)) continue
            val event = runCatching { adapter.fromJson(line.trim()) }.getOrNull() ?: continue
            val mapped = event.streams?.map { it.toDomain(addonName, addonLogo) }.orEmpty()
            if (mapped.isNotEmpty()) {
                latest = mapped
                onSnapshot(mapped)
            }
            if (event.complete == true) return latest
        }
        Log.d(TAG, "progressive ended without complete host=$host; official request")
        null
    }

    private companion object {
        const val TAG = "ProgressiveAioStreams"
        const val READ_TIMEOUT_S = 120L
        const val CALL_TIMEOUT_S = 180L
    }
}
