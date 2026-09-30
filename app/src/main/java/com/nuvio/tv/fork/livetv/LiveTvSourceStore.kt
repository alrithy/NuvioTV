package com.nuvio.tv.fork.livetv

import java.io.InputStream
import java.util.UUID

/** Where a profile's Live TV sources and imported playlists are kept; a fake replaces it in tests. */
interface LiveTvSourceStore {
    suspend fun sources(profileId: Int): List<LiveTvSource>

    suspend fun saveSources(profileId: Int, sources: List<LiveTvSource>)

    fun newSourceId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    /** Runs [block] over the lines of [sourceId]'s imported playlist; null when it has none. */
    suspend fun <T> readPlaylist(profileId: Int, sourceId: String, block: (Sequence<String>) -> T): T?

    /** Saves an imported playlist for [sourceId] from [input], at most [maxBytes]; false when it did not fit. */
    suspend fun savePlaylist(profileId: Int, sourceId: String, input: InputStream, maxBytes: Long): Boolean

    suspend fun deletePlaylist(profileId: Int, sourceId: String)
}
