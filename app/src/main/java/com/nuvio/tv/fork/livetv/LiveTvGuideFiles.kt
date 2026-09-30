package com.nuvio.tv.fork.livetv

import java.io.File

/** Where guides are downloaded to and read from (G10c); a fake replaces it in tests. */
interface LiveTvGuideFiles {
    /** The app-cache folder the guides and the kept-programme cache live in. */
    val dir: File

    /** Saves [url] to [target] (gzip); the old file stays until the new one is complete. */
    suspend fun download(url: String, headers: Map<String, String>, target: File)

    /** Reads a saved guide for [request]; cancelling stops the read. */
    suspend fun read(file: File, request: LiveTvGuideRequest, nowEpochMs: Long, window: LiveTvGuideWindow): LiveTvGuide
}
