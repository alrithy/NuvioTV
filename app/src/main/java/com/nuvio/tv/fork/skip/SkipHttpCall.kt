package com.nuvio.tv.fork.skip

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

/** G9 closeout correction: one response header, with the same cancellation as [readSkipBody]. */
internal suspend fun Call.readHeaderCancellable(name: String): String? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use { if (continuation.isActive) continuation.resume(response.header(name)) }
        }
    })
}

/** G9 closeout correction: cancellation closes both a pending request and a stalled body read. */
internal suspend fun Call.readSkipBody(maxBytes: Long): String? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                try {
                    val text = if (!response.isSuccessful) null else response.body?.let { body ->
                        if (body.contentLength() > maxBytes) null else {
                            val source = body.source()
                            source.request(maxBytes + 1)
                            if (source.buffer.size > maxBytes) null else source.buffer.readUtf8()
                        }
                    }
                    if (continuation.isActive) continuation.resume(text)
                } catch (error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        }
    })
}
