package com.nuvio.tv.fork.security

import java.security.MessageDigest
import java.security.SecureRandom

/*
 * G14a (D064; features 303, 304): one owner for who may talk to the TV's local QR configuration
 * servers. Official's five servers (add-ons, repositories, custom posters, debrid formatter, stream
 * badges) answer anyone on the network at fixed paths; the fork's font and Live TV servers already
 * carry a random per-session token. This is that token rule, shared, plus a session cookie so
 * official's pages keep working unchanged.
 */

/** What a request to a local QR server may do. */
enum class LocalServerDecision {
    /** A session request: serve it. */
    ALLOW,

    /** The QR link's first load: answer with [LocalServerAccess.entryHeaders] and a redirect. */
    ENTER,

    /** No valid session: the page was not opened from this TV's QR code. */
    DENY_SESSION,

    /** A state-changing request sent by another site's page. */
    DENY_ORIGIN,
}

/**
 * One server's session. The QR code carries `/?k=<key>`; its first load is answered with a session
 * cookie (`HttpOnly`, `SameSite=Lax`) and a redirect to the same path without the key, so the key
 * leaves the address bar and every later request — official's same-origin `fetch` calls included —
 * carries the cookie. Lax rather than Strict: QR scanners open the link from outside the browser,
 * and cross-site POSTs are refused by their Origin anyway.
 */
class LocalServerAccess(val key: String = newKey()) {
    /** The cookie's name is per session, so two servers on the same address never overwrite it. */
    val cookieName: String = "nuvio_qr_" + key.take(COOKIE_SUFFIX_LENGTH)

    /** Path and query the QR code opens, appended to `http://<ip>:<port>`. */
    val entryPath: String get() = "/?$KEY_PARAM=$key"

    fun entryUrl(baseUrl: String): String = baseUrl.trimEnd('/') + entryPath

    fun decide(
        method: String,
        keyParam: String?,
        cookieHeader: String?,
        origin: String?,
        referer: String?,
        host: String?,
    ): LocalServerDecision {
        val read = method.equals("GET", ignoreCase = true) || method.equals("HEAD", ignoreCase = true)
        if (read && keyParam != null && sameSecret(keyParam, key)) return LocalServerDecision.ENTER
        val session = cookieValue(cookieHeader, cookieName)
        if (session == null || !sameSecret(session, key)) return LocalServerDecision.DENY_SESSION
        if (!read && !originAllowed(origin, referer, host)) return LocalServerDecision.DENY_ORIGIN
        return LocalServerDecision.ALLOW
    }

    /** Headers of the [LocalServerDecision.ENTER] redirect. */
    fun entryHeaders(path: String): Map<String, String> = mapOf(
        "Set-Cookie" to "$cookieName=$key; Path=/; HttpOnly; SameSite=Lax",
        "Location" to path.ifBlank { "/" },
        "Cache-Control" to "no-store",
        "Referrer-Policy" to "no-referrer",
    )

    companion object {
        const val KEY_PARAM = "k"
        private const val COOKIE_SUFFIX_LENGTH = 8

        fun newKey(random: SecureRandom = SecureRandom()): String {
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        }

        /**
         * A page from another site may post to the TV from the user's browser; its Origin (or, when a
         * browser omits Origin, its Referer) then differs from our Host. With neither header (curl, some
         * same-page requests) the request is allowed: the key already limits it to someone who can see
         * the TV screen.
         */
        fun originAllowed(origin: String?, referer: String?, host: String?): Boolean {
            if (origin != null) return host != null && origin.equals("http://$host", ignoreCase = true)
            if (referer != null) {
                return host != null && (
                    referer.equals("http://$host", ignoreCase = true) ||
                        referer.startsWith("http://$host/", ignoreCase = true)
                    )
            }
            return true
        }

        internal fun cookieValue(header: String?, name: String): String? = header
            ?.split(';')
            ?.asSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter('=')

        private fun sameSecret(candidate: String, secret: String): Boolean =
            MessageDigest.isEqual(candidate.toByteArray(Charsets.UTF_8), secret.toByteArray(Charsets.UTF_8))
    }
}
