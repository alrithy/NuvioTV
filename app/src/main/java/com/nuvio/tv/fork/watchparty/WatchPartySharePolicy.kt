package com.nuvio.tv.fork.watchparty

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/*
 * What a Watch Party may send and accept (G11a, D056; features 243, 244, 249). Local WRITE: the
 * Antonino source shares the current link with every header. Here only headers a player needs to
 * reach a public link are shared, and a stream that needs credentials is not shareable at all
 * (rather than shared half-working). The same checks guard what a guest accepts. A link to a
 * loopback, private, link-local or otherwise non-public address is never shared or opened, so a host
 * cannot point a guest's TV at the guest's own network.
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

    /**
     * Guest, before opening a received link: every address [host] resolves to must be public, so a
     * name that points into the guest's own network is refused too. A failed lookup is a refusal.
     * Blocking; call off the main thread.
     */
    fun resolvesToPublic(host: String, lookup: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() }): Boolean {
        if (isLocal(host)) return false
        val addresses = runCatching { lookup(host.trim('[', ']')) }.getOrNull() ?: return false
        return addresses.isNotEmpty() && addresses.all(::isPublicAddress)
    }

    /**
     * Loopback, private, link-local and other non-public hosts, judged from the name alone (no DNS):
     * IP literals by range, LAN-only names, and numeric forms other than plain dotted IPv4 (such as
     * `0x7f.1` or `2130706433`, which some resolvers read as loopback).
     */
    internal fun isLocal(host: String?): Boolean {
        val h = host?.lowercase()?.trim('[', ']')?.trimEnd('.') ?: return true
        if (h.isEmpty()) return true
        if (':' in h) return ipv6Literal(h)?.let { !isPublicAddress(it) } ?: true
        if (NUMERIC_HOST.matches(h)) return ipv4Literal(h)?.let { !isPublicAddress(it) } ?: true
        if ('.' !in h) return true
        return LOCAL_NAMES.any { h == it || h.endsWith(".$it") }
    }

    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) return false
        val b = address.address.map { it.toInt() and 0xff }
        return when (address) {
            is Inet4Address -> !(
                b[0] == 0 ||
                    (b[0] == 100 && b[1] in 64..127) || // carrier-grade NAT
                    (b[0] == 192 && b[1] == 0 && b[2] == 0) ||
                    (b[0] == 198 && b[1] in 18..19) ||
                    b[0] >= 240
                )
            is Inet6Address -> when {
                (b[0] and 0xfe) == 0xfc -> false // unique local
                address.isIPv4CompatibleAddress || isNat64(b) ->
                    InetAddress.getByAddress(b.takeLast(4).map(Int::toByte).toByteArray()).let(::isPublicAddress)
                else -> true
            }
            else -> false
        }
    }

    private fun isNat64(b: List<Int>): Boolean =
        b[0] == 0x00 && b[1] == 0x64 && b[2] == 0xff && b[3] == 0x9b && b.subList(4, 12).all { it == 0 }

    /** Plain dotted IPv4 only: no leading zeros (octal in some resolvers), each part 0–255. */
    private fun ipv4Literal(h: String): InetAddress? {
        val parts = h.split('.')
        if (parts.size != 4 || parts.any { !IPV4_PART.matches(it) || it.toInt() > 255 }) return null
        return InetAddress.getByAddress(parts.map { it.toInt().toByte() }.toByteArray())
    }

    /** An IPv6 literal (a zone id is refused); Java parses literals without a DNS lookup. */
    private fun ipv6Literal(h: String): InetAddress? {
        if (!IPV6_CHARS.matches(h)) return null
        return runCatching { InetAddress.getByName(h) }.getOrNull()
    }

    private val NUMERIC_HOST = Regex("^(0x[0-9a-f]*|[0-9]+)(\\.(0x[0-9a-f]*|[0-9]+))*$")
    private val IPV4_PART = Regex("^(0|[1-9][0-9]{0,2})$")
    private val IPV6_CHARS = Regex("^[0-9a-f:.]+$")
    private val LOCAL_NAMES = listOf("localhost", "local", "lan", "home", "internal", "intranet", "localdomain", "home.arpa")

    private fun String?.bounded(): String? = this?.filterNot { it < ' ' }?.take(MAX_TEXT_CHARS)?.takeIf(String::isNotBlank)

    private fun String?.httpOrNull(): String? = this?.takeIf { it.length <= MAX_URL_CHARS && parseHttp(it) != null }
}
