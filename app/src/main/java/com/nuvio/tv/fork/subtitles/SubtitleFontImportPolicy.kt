package com.nuvio.tv.fork.subtitles

import com.nuvio.tv.fork.security.LocalServerAccess
import java.net.URI
import java.security.SecureRandom

enum class SubtitleFontImportResult {
    IMPORTED,
    TOO_LARGE,
    INVALID,
    DOWNLOAD_FAILED,
}

/**
 * Request rules for importing a subtitle font (G6b, features 101, 102). Pure, so the security
 * decisions are unit-tested apart from NanoHTTPD and OkHttp.
 */
object SubtitleFontImportPolicy {
    /** 128-bit random path token for the LAN upload page; shown only in the TV's QR code. */
    fun newToken(random: SecureRandom = SecureRandom()): String = LocalServerAccess.newKey(random)

    /**
     * A page from another site may post to the TV from the user's browser; its Origin then differs
     * from our Host. No Origin (curl, a same-page fetch in some browsers) is allowed: the token
     * already limits the upload to someone who can see the TV screen.
     */
    fun originAllowed(origin: String?, host: String?): Boolean =
        LocalServerAccess.originAllowed(origin, referer = null, host = host)

    /** Upfront verdict from Content-Length; null means read the body. */
    fun uploadLengthVerdict(contentLength: Long?): SubtitleFontImportResult? = when {
        contentLength == null || contentLength <= 0L -> SubtitleFontImportResult.INVALID
        contentLength > SubtitleFontFile.MAX_FONT_BYTES -> SubtitleFontImportResult.TOO_LARGE
        else -> null
    }

    /** Only absolute HTTPS URLs with a host are downloaded (no cleartext, no local schemes). */
    fun downloadUrlAllowed(url: String): Boolean {
        val uri = try {
            URI(url.trim())
        } catch (_: Exception) {
            return false
        }
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    /** Host only, for logs: never the path or query (a download link may carry a token). */
    fun logHost(url: String): String =
        try {
            URI(url.trim()).host ?: "?"
        } catch (_: Exception) {
            "?"
        }
}
