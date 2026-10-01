package com.nuvio.tv.fork.watchparty

import java.net.URI

/*
 * What a Watch Party may send and accept (G11a, D056; features 243, 244, 249). Local WRITE: the
 * Antonino source shares the current link with every header. Here only headers a player needs to
 * reach a public link are shared, and a stream that needs credentials is not shareable at all
 * (rather than shared half-working). The same checks guard what a guest accepts.
 */

/** Why the open stream cannot be shared; the panel says so instead of offering a room. */
enum class WatchPartyUnshareable { NOT_HTTP, LOCAL, TORRENT, LIVE_TV, CREDENTIALS }

sealed interface WatchPartyShareDecision {
    data class Shareable(val headers: Map<String, String>) : WatchPartyShareDecision
    data class NotShareable(val reason: WatchPartyUnshareable) : WatchPartyShareDecision
}

object WatchPartySharePolicy {
    /** The only headers that ever leave the device: what a player needs to reach a public link. */
    private val SHARED_HEADERS = setOf("user-agent", "referer", "origin", "accept", "accept-language")

    /** Headers that carry an identity; their presence makes a stream unshareable. */
    private val CREDENTIAL_HEADERS = setOf("authorization", "proxy-authorization", "cookie", "set-cookie")
    private val CREDENTIAL_PARTS = listOf("key", "token", "secret", "auth", "session", "password", "signature")

    const val MAX_URL_CHARS = 8_192
    const val MAX_HEADER_VALUE_CHARS = 1_024
    const val MAX_TEXT_CHARS = 300
    const val MAX_NAME_CHARS = 40

    fun decide(url: String, headers: Map<String, String>, isTorrent: Boolean, isLiveTv: Boolean): WatchPartyShareDecision {
        val uri = parseHttp(url) ?: return WatchPartyShareDecision.NotShareable(
            if (isTorrent) WatchPartyUnshareable.TORRENT else WatchPartyUnshareable.NOT_HTTP,
        )
        return when {
            isTorrent -> WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.TORRENT)
            isLiveTv -> WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.LIVE_TV)
            isLocal(uri.host) -> WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.LOCAL)
            uri.rawUserInfo != null || headers.keys.any(::isCredentialHeader) ->
                WatchPartyShareDecision.NotShareable(WatchPartyUnshareable.CREDENTIALS)
            else -> WatchPartyShareDecision.Shareable(allowedHeaders(headers))
        }
    }

    /**
     * What a guest accepts from the host: a public HTTP(S) link, allow-listed headers only, bounded
     * text. Anything else is dropped (null), never opened.
     */
    fun acceptReceived(media: WatchPartyMedia): WatchPartyMedia? {
        if (media.url.length > MAX_URL_CHARS) return null
        val uri = parseHttp(media.url) ?: return null
        if (isLocal(uri.host) || uri.rawUserInfo != null) return null
        return media.copy(
            headers = allowedHeaders(media.headers),
            title = media.title.bounded(),
            subtitle = media.subtitle.bounded(),
            contentId = media.contentId.bounded(),
            contentType = media.contentType.bounded(),
            videoId = media.videoId.bounded(),
            poster = media.poster.httpOrNull(),
            backdrop = media.backdrop.httpOrNull(),
            logo = media.logo.httpOrNull(),
            streamName = media.streamName.bounded(),
            season = media.season?.takeIf { it in 0..10_000 },
            episode = media.episode?.takeIf { it in 0..100_000 },
        )
    }

    /** A peer's display name: short, printable. */
    fun peerName(raw: String?): String? =
        raw?.filterNot { it < ' ' }?.trim()?.take(MAX_NAME_CHARS)?.takeIf(String::isNotEmpty)

    private fun allowedHeaders(headers: Map<String, String>): Map<String, String> =
        headers.filter { (name, value) ->
            name.lowercase() in SHARED_HEADERS && value.length <= MAX_HEADER_VALUE_CHARS && value.none { it == '\r' || it == '\n' }
        }

    private fun isCredentialHeader(name: String): Boolean {
        val lower = name.lowercase()
        if (lower in SHARED_HEADERS) return false
        return lower in CREDENTIAL_HEADERS || CREDENTIAL_PARTS.any { it in lower }
    }

    private fun parseHttp(url: String): URI? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }

    private fun isLocal(host: String?): Boolean {
        val h = host?.lowercase()?.trim('[', ']') ?: return true
        return h == "localhost" || h.endsWith(".localhost") || h == "::1" || h.startsWith("127.") || h == "0.0.0.0"
    }

    private fun String?.bounded(): String? = this?.filterNot { it < ' ' }?.take(MAX_TEXT_CHARS)?.takeIf(String::isNotBlank)

    private fun String?.httpOrNull(): String? = this?.takeIf { it.length <= MAX_URL_CHARS && parseHttp(it) != null }
}
