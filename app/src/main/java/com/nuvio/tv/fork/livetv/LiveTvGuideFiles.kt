package com.nuvio.tv.fork.livetv

import java.io.File
import java.security.MessageDigest

/** No URL/credentials on disk, and distinct providers cannot collide through String.hashCode. */
internal fun liveTvGuideFileName(url: String): String = "guide_${liveTvGuideDigest(listOf(url))}.xml.gz"

internal fun liveTvGuideDigest(parts: List<String>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    parts.forEach { part ->
        val bytes = part.toByteArray(Charsets.UTF_8)
        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.size).array())
        digest.update(bytes)
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

/** Where guides are downloaded to and read from (G10c); a fake replaces it in tests. */
interface LiveTvGuideFiles {
    /** The app-cache folder the guides and the kept-programme cache live in. */
    val dir: File

    /** Saves [url] to [target] (gzip); the old file stays until the new one is complete. */
    suspend fun download(url: String, headers: Map<String, String>, target: File)

    /** Reads a saved guide for [request]; cancelling stops the read. */
    suspend fun read(file: File, request: LiveTvGuideRequest, nowEpochMs: Long, window: LiveTvGuideWindow): LiveTvGuide
}
