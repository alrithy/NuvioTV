package com.nuvio.tv.fork.watchparty

/**
 * The links a guest opened from a host (G11b, D056; feature 244). Memory only and bounded: the
 * player asks it before saving a link for reuse, so a link someone else shared is never written to
 * this device.
 */
object WatchPartyReceivedStreams {
    private const val MAX_ENTRIES = 8
    private val urls = ArrayDeque<String>()

    @Synchronized
    fun register(url: String) {
        urls.remove(url)
        urls.addLast(url)
        while (urls.size > MAX_ENTRIES) urls.removeFirst()
    }

    @Synchronized
    fun contains(url: String?): Boolean = url != null && url in urls

    @Synchronized
    internal fun clearForTest() = urls.clear()
}
