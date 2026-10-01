package com.nuvio.tv.fork.watchparty

import java.security.SecureRandom

/**
 * The links a guest opened from a host (G11b, D056; feature 244). Memory only and bounded: the
 * player asks it before saving a link for reuse, so a link someone else shared is never written to
 * this device.
 *
 * The player route carries a ticket instead of the link and its headers ([register] / [redeem]),
 * because route arguments live in the navigation saved state, which Android may write to disk. A
 * ticket is redeemed once; leaving the room drops the unredeemed ones; after the process is recreated
 * a ticket opens nothing.
 */
object WatchPartyReceivedStreams {
    const val TICKET_PREFIX = "nuvio-watchparty:"
    private const val MAX_ENTRIES = 8
    private val urls = ArrayDeque<String>()
    private val tickets = LinkedHashMap<String, WatchPartyMedia>()
    private val random = SecureRandom()

    /** What the player opens for a ticket: an empty link once the ticket is gone. */
    data class Redeemed(val url: String, val headers: Map<String, String>) {
        override fun toString(): String = "Redeemed(…)"
    }

    /** Remembers a link received from the host; returns the ticket the player route carries instead. */
    @Synchronized
    fun register(media: WatchPartyMedia): String {
        urls.remove(media.url)
        urls.addLast(media.url)
        while (urls.size > MAX_ENTRIES) urls.removeFirst()
        val ticket = TICKET_PREFIX + ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        tickets[ticket] = media
        while (tickets.size > MAX_ENTRIES) tickets.remove(tickets.keys.first())
        return ticket
    }

    /** For a route's stream value: the received link for a ticket (once), null for an ordinary link. */
    @Synchronized
    fun redeem(streamUrl: String): Redeemed? {
        if (!streamUrl.startsWith(TICKET_PREFIX)) return null
        val media = tickets.remove(streamUrl) ?: return Redeemed(url = "", headers = emptyMap())
        return Redeemed(url = media.url, headers = media.headers)
    }

    @Synchronized
    fun contains(url: String?): Boolean = url != null && url in urls

    /** Leaving the room: tickets not yet opened are dropped. */
    @Synchronized
    fun clearTickets() = tickets.clear()

    @Synchronized
    internal fun clearForTest() {
        urls.clear()
        tickets.clear()
    }
}
