package com.nuvio.tv.fork.livetv

import com.squareup.moshi.JsonReader
import java.net.URLDecoder
import java.security.SecureRandom
import okio.Buffer

/*
 * The phone setup page's rules (G10g, feature 213). The checks of Reshaped `LiveTvSetupServer`
 * @ 0ccf049 as plain functions: a random per-session token in every path, the Origin check, the
 * form and playlist size caps, and the form read into one source request. Credentials only ever
 * arrive in a POST body; nothing here logs.
 */

/** A source sent from the phone page. */
internal sealed interface LiveTvSetupRequest {
    data class M3u(val url: String) : LiveTvSetupRequest

    data class Xtream(val settings: LiveTvXtreamSettings) : LiveTvSetupRequest

    data class Stalker(val settings: LiveTvStalkerSettings) : LiveTvSetupRequest
}

/** Why a playlist upload is refused before it is read. */
internal enum class LiveTvPlaylistRefusal(val code: String) { INVALID("invalid"), TOO_LARGE("too_large") }

internal object LiveTvSetupPolicy {
    /** A form is a link or a login: a few hundred bytes. */
    const val MAX_FORM_BYTES = 16L * 1024
    /** Big provider playlists run to tens of megabytes. */
    const val MAX_PLAYLIST_BYTES = 64L * 1024 * 1024
    const val DEFAULT_PLAYLIST_NAME = "M3U playlist"
    private const val MAX_NAME_CHARS = 80

    /** 128 random bits as hex: the path only someone who can see the TV's QR code knows. */
    fun newToken(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    /** A page from another site may post here: its Origin then differs from our Host. */
    fun originAllowed(origin: String?, host: String?): Boolean =
        origin == null || (host != null && origin.equals("http://$host", ignoreCase = true))

    fun formLengthAllowed(contentLength: Long?): Boolean = contentLength != null && contentLength in 1..MAX_FORM_BYTES

    fun playlistRefusal(contentLength: Long?): LiveTvPlaylistRefusal? = when {
        contentLength == null || contentLength <= 0L -> LiveTvPlaylistRefusal.INVALID
        contentLength > MAX_PLAYLIST_BYTES -> LiveTvPlaylistRefusal.TOO_LARGE
        else -> null
    }

    /** The JSON form as one source, or null when it is not one the TV can load. */
    fun parseForm(body: String): LiveTvSetupRequest? {
        val fields = readStringFields(body) ?: return null
        fun field(name: String) = fields[name].orEmpty().trim()
        return when (field("type")) {
            "m3u" -> field("url").takeIf(String::isHttpUrl)?.let(LiveTvSetupRequest::M3u)
            "xtream" -> LiveTvXtreamSettings(field("server"), field("username"), field("password")).normalized()
                .takeIf { it.isConfigured && it.serverUrl.isHttpUrl() }
                ?.let(LiveTvSetupRequest::Xtream)
            "stalker" -> LiveTvStalkerSettings(field("portal"), field("mac"), field("username"), field("password")).normalized()
                .takeIf { it.isConfigured && it.portalUrl.isHttpUrl() }
                ?.let(LiveTvSetupRequest::Stalker)
            else -> null
        }
    }

    /** The uploaded file's name from `?name=`, without any folders; what the source list shows. */
    fun playlistName(query: String?): String =
        query?.split('&')
            ?.firstOrNull { it.startsWith("name=") }
            ?.substringAfter('=')
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.filterNot { it < ' ' }
            ?.trim()
            ?.take(MAX_NAME_CHARS)
            ?.takeIf(String::isNotEmpty)
            ?: DEFAULT_PLAYLIST_NAME

    /** A flat JSON object's string values; null when the body is not one. */
    private fun readStringFields(body: String): Map<String, String>? = try {
        val reader = JsonReader.of(Buffer().writeUtf8(body))
        val fields = HashMap<String, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (reader.peek() == JsonReader.Token.STRING) fields[name] = reader.nextString() else reader.skipValue()
        }
        reader.endObject()
        fields
    } catch (_: Exception) {
        null
    }
}
