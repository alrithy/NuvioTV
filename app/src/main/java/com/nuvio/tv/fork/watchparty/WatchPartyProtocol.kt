package com.nuvio.tv.fork.watchparty

import java.security.SecureRandom
import kotlinx.serialization.Serializable

/*
 * Watch Party wire format (G11a, D056). FILE_PORT of AntoninoScardina/NuvioTV
 * `watchparty/WatchPartyProtocol.kt` @ ff597b1. Field names, message types, the code alphabet and the
 * room / password derivation are kept exactly, so the Nuvio Party phone build can join (feature 240).
 * Adapted: the room code comes from SecureRandom. The phone side is pinned for compatibility at
 * AntoninoScardina/NuvioMobile `watchparty` @ ff7a16b (tag v2026.929.8): its protocol file is the same
 * as the TV source's, and it reads messages with `ignoreUnknownKeys`.
 */

/** The stream the host shares: guests open exactly this link with these (allow-listed) headers. */
@Serializable
data class WatchPartyMedia(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val title: String? = null,
    val subtitle: String? = null,
    val contentId: String? = null,
    val contentType: String? = null,
    val videoId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val streamName: String? = null,
) {
    /** Never prints the link or the headers. */
    override fun toString(): String = "WatchPartyMedia(contentId=$contentId, season=$season, episode=$episode)"
}

/**
 * The one message on the data channel:
 *  - HELLO: introduction (name, host)
 *  - MEDIA: host → guest, the stream to open and the playback state
 *  - STATE: host → guest, position / playing, periodic and on every change
 *  - REQUEST_STATE: guest → host, asks for MEDIA / STATE (just joined or player just opened)
 *  - CMD: guest → host, play / pause / seek done by a guest
 *  - BYE: leaving
 *
 * [rate] is a Superfork addition (G11 corrections): the host's chosen playback speed in MEDIA / STATE,
 * sent only when it is not 1×, so normal-speed messages stay exactly protocol v1. Older builds ignore
 * it (they read with `ignoreUnknownKeys`).
 */
@Serializable
data class WatchPartyWire(
    val type: String,
    val version: Int = WatchPartyProtocol.VERSION,
    val name: String? = null,
    val host: Boolean? = null,
    val media: WatchPartyMedia? = null,
    val positionMs: Long? = null,
    val playing: Boolean? = null,
    val action: String? = null,
    val rate: Float? = null,
)

object WatchPartyProtocol {
    const val VERSION = 1

    const val HELLO = "HELLO"
    const val MEDIA = "MEDIA"
    const val STATE = "STATE"
    const val REQUEST_STATE = "REQUEST_STATE"
    const val CMD = "CMD"
    const val BYE = "BYE"

    const val ACTION_PLAY = "play"
    const val ACTION_PAUSE = "pause"
    const val ACTION_SEEK = "seek"

    /** The player's speed range; a [WatchPartyWire.rate] outside it is ignored. */
    const val MIN_RATE = 0.25f
    const val MAX_RATE = 2f

    /** No 0/O, 1/I/L: easy to read off a TV and type on a remote. */
    private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 6

    private val random = SecureRandom()

    fun generateCode(source: SecureRandom = random): String = buildString {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[source.nextInt(CODE_ALPHABET.length)]) }
    }

    fun normalizeCode(input: String): String? {
        val code = input.uppercase().filter { it.isLetterOrDigit() }
        if (code.length != CODE_LENGTH || code.any { it !in CODE_ALPHABET }) return null
        return code
    }

    fun roomFor(code: String): String = "nuviowatchparty$code"

    fun passwordFor(code: String): String = "nuvio-wp-$code"
}
