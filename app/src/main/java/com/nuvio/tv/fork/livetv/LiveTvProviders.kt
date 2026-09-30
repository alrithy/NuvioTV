package com.nuvio.tv.fork.livetv

import com.squareup.moshi.JsonReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import okio.BufferedSource

/*
 * Xtream Codes and Stalker (MAG) providers (G10a, features 210, 211, 233). FILE_PORT of Reshaped
 * `reshaped/livetv/LiveTvProviders.kt` @ 0ccf049, adapted: Moshi's streaming reader replaces
 * `android.util.JsonReader` / `org.json` (the same one-entry-at-a-time reading, so a 20 000 channel
 * provider never becomes a JSON tree, and testable on the JVM), requests go through [LiveTvFetcher],
 * failures are logged by host only, and Stalker's session Cookie / Authorization are attached to a
 * channel only when its stream is on the portal's own host (D055).
 */

internal fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

// region Xtream

internal class LiveTvXtream(private val http: LiveTvFetcher) {

    suspend fun channels(settings: LiveTvXtreamSettings): List<LiveTvChannel> {
        val categories = http.stream(apiUrl(settings, "get_live_categories"), LIVE_TV_PLAYLIST_HEADERS) { source ->
            readObjects(source) { fields ->
                val id = fields["category_id"] ?: fields["id"] ?: return@readObjects null
                val name = fields["category_name"] ?: fields["name"] ?: return@readObjects null
                id to name
            }
        }.toMap()
        val extension = liveExtension(settings)
        return http.stream(apiUrl(settings, "get_live_streams"), LIVE_TV_PLAYLIST_HEADERS) { source ->
            readXtreamStreams(source, settings, categories, extension)
        }
    }

    /**
     * The live format this account may use: MPEG-TS, as IPTV players prefer, unless the account only
     * allows HLS ("allowed_output_formats" in the login reply); a TS link then fails on every channel.
     */
    private suspend fun liveExtension(settings: LiveTvXtreamSettings): String {
        val formats = try {
            readXtreamAllowedFormats(http.text(loginUrl(settings), LIVE_TV_PLAYLIST_HEADERS))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            LiveTvLog.warn("Xtream formats unreadable, using TS", settings.serverUrl, error)
            emptyList()
        }
        return xtreamLiveExtension(formats)
    }

    private fun loginUrl(settings: LiveTvXtreamSettings): String =
        "${settings.serverUrl}/player_api.php?username=${settings.username.urlEncoded()}" +
            "&password=${settings.password.urlEncoded()}"

    private fun apiUrl(settings: LiveTvXtreamSettings, action: String): String =
        loginUrl(settings) + "&action=${action.urlEncoded()}"

    companion object {
        /** Xtream providers publish their guide at `xmltv.php`. */
        fun guideUrl(settings: LiveTvXtreamSettings): String =
            "${settings.serverUrl}/xmltv.php?username=${settings.username.urlEncoded()}" +
                "&password=${settings.password.urlEncoded()}"
    }
}

internal fun xtreamLiveExtension(allowedFormats: List<String>): String =
    if (allowedFormats.isEmpty() || "ts" in allowedFormats || "m3u8" !in allowedFormats) "ts" else "m3u8"

internal fun readXtreamStreams(
    source: BufferedSource,
    settings: LiveTvXtreamSettings,
    categories: Map<String, String>,
    extension: String,
): List<LiveTvChannel> {
    val seen = HashSet<String>()
    var index = 0
    return readObjects(source) { fields ->
        val position = index++
        val name = fields["name"] ?: return@readObjects null
        val streamId = fields["stream_id"] ?: fields["id"] ?: return@readObjects null
        // Always the panel's own link, as IPTV players use: "direct_source" is often the panel's
        // upstream origin, which refuses clients, and the panel redirects to it when it is meant to be used.
        val streamUrl = "${settings.serverUrl}/live/${settings.username.urlEncoded()}/" +
            "${settings.password.urlEncoded()}/${streamId.urlEncoded()}.$extension"
        if (!seen.add(streamUrl)) return@readObjects null
        LiveTvChannel(
            id = "xtream-$streamId-$position",
            name = name,
            streamUrl = streamUrl,
            tvgId = fields["epg_channel_id"] ?: fields["tvg_id"],
            logoUrl = fields["stream_icon"] ?: fields["logo"],
            group = fields["category_id"]?.let(categories::get).orEmpty(),
            headers = LIVE_TV_STREAM_HEADERS,
        )
    }
}

/** `user_info.allowed_output_formats` of an Xtream login reply, lower-cased. */
internal fun readXtreamAllowedFormats(text: String): List<String> {
    val reader = JsonReader.of(Buffer().writeUtf8(text)).apply { isLenient = true }
    if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) return emptyList()
    var formats = emptyList<String>()
    reader.beginObject()
    while (reader.hasNext()) {
        if (reader.nextName() != "user_info" || reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            continue
        }
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "allowed_output_formats" && reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                val list = ArrayList<String>()
                reader.beginArray()
                while (reader.hasNext()) reader.nextScalar()?.lowercase()?.let(list::add)
                reader.endArray()
                formats = list
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
    }
    reader.endObject()
    return formats
}

// endregion

// region Stalker

internal data class StalkerChannels(val channels: List<LiveTvChannel>, val incomplete: Boolean)

private class StalkerSession(val settings: LiveTvStalkerSettings, val token: String) {
    /** Built once per session and shared by every channel, not copied into each. */
    var authHeaders: Map<String, String>? = null
}

internal class LiveTvStalker(private val http: LiveTvFetcher) {

    /** One session per portal login, so several Stalker sources do not keep renewing each other's. */
    private val sessions = ConcurrentHashMap<LiveTvStalkerSettings, StalkerSession>()

    fun clearSessions() {
        sessions.clear()
    }

    /** The portal's channels; [StalkerChannels.incomplete] when some pages still failed after a retry. */
    suspend fun channels(settings: LiveTvStalkerSettings): StalkerChannels = withSession(settings) { session ->
        val genres = dataObjects(session, "itv", "get_genres") { fields ->
            val id = fields["id"] ?: fields["alias"]
            val title = fields["title"] ?: fields["name"]
            if (id == null || title == null) null else id to title
        }.toMap()
        var index = 0
        val toChannel: (Map<String, String>) -> LiveTvChannel? = { fields -> fields.toChannel(session, genres, index++) }
        // One request where the portal supports it, otherwise every page of the ordered list.
        val all = try {
            dataObjects(session, "itv", "get_all_channels", toChannel)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
        val result = if (all.isNotEmpty()) StalkerChannels(all, incomplete = false) else orderedPages(session, toChannel)
        val seen = HashSet<String>()
        result.copy(channels = result.channels.filter { seen.add(it.id.ifBlank { it.streamUrl }) })
    }

    /** A playable link for a list entry: Stalker links are created per play and expire. */
    suspend fun resolve(settings: LiveTvStalkerSettings, channel: LiveTvChannel): LiveTvChannel {
        val command = channel.stalkerCommand ?: return channel
        return withSession(settings.normalized()) { session ->
            val fields = readJsFields(request(session.settings, session.token, "itv", "create_link", mapOf("cmd" to command)))
            // An expired session answers without a link: the failure renews it once (withSession).
            val link = (fields["cmd"] ?: fields["url"] ?: fields["stream_url"])
                ?.toStalkerPlayableUrl()?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("no link")
            channel.copy(streamUrl = link, headers = streamHeaders(session, link))
        }
    }

    /**
     * Runs [block] with a portal session. Portals expire sessions without notice, so a failure
     * with a cached session is retried once after a fresh handshake.
     */
    private suspend fun <T> withSession(settings: LiveTvStalkerSettings, block: suspend (StalkerSession) -> T): T {
        val cached = sessions[settings]
        if (cached != null) {
            try {
                return block(cached)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                sessions.remove(settings, cached)
            }
        }
        return block(handshake(settings))
    }

    private suspend fun handshake(settings: LiveTvStalkerSettings): StalkerSession {
        val token = readJsFields(request(settings, null, "stb", "handshake"))["token"]
            ?: throw LiveTvException(LiveTvError.StalkerToken)
        // Many portals only list channels after the device profile was requested with the token.
        try {
            request(settings, token, "stb", "get_profile")
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
        return StalkerSession(settings, token).also { sessions[settings] = it }
    }

    private suspend fun orderedPages(
        session: StalkerSession,
        toChannel: (Map<String, String>) -> LiveTvChannel?,
    ): StalkerChannels {
        // Pages load a few at a time, so the mapper is shared across threads.
        val lock = Any()
        val mapper: (Map<String, String>) -> LiveTvChannel? = { synchronized(lock) { toChannel(it) } }
        suspend fun page(number: Int): StalkerPage<LiveTvChannel> =
            http.stream(
                url(session.settings, session.token, "itv", "get_ordered_list", mapOf("p" to number.toString())),
                authHeaders(session),
            ) { source -> readStalkerPage(source, mapper) }

        val first = page(1)
        if (first.entries.isEmpty()) return StalkerChannels(emptyList(), incomplete = false)
        var failedPages = 0
        val entries = ArrayList(first.entries)
        val perPage = first.maxPageItems?.takeIf { it > 0 } ?: first.entries.size
        val total = first.totalItems
        if (total != null && perPage > 0) {
            val lastPage = ((total + perPage - 1) / perPage).coerceAtMost(MAX_PAGES)
            for (numbers in (2..lastPage).chunked(PARALLEL_PAGES)) {
                coroutineScope {
                    numbers.map { number ->
                        async {
                            // A portal that drops a page under load usually serves it on a second try.
                            loadPageTwice { page(number).entries } ?: run {
                                synchronized(lock) { failedPages++ }
                                emptyList()
                            }
                        }
                    }.awaitAll()
                }.forEach(entries::addAll)
                if (entries.size >= LIVE_TV_MAX_CHANNELS_PER_SOURCE) break
            }
        } else {
            for (number in 2..MAX_PAGES) {
                val data = page(number).entries
                if (data.isEmpty() || entries.size >= LIVE_TV_MAX_CHANNELS_PER_SOURCE) break
                entries += data
            }
        }
        if (failedPages > 0) LiveTvLog.warn("Stalker: $failedPages pages could not be loaded", session.settings.portalUrl)
        return StalkerChannels(entries.take(LIVE_TV_MAX_CHANNELS_PER_SOURCE), incomplete = failedPages > 0)
    }

    private fun Map<String, String>.toChannel(
        session: StalkerSession,
        genres: Map<String, String>,
        index: Int,
    ): LiveTvChannel? {
        val name = this["name"] ?: this["title"] ?: return null
        val command = this["cmd"] ?: this["mc_cmd"] ?: this["url"] ?: return null
        val streamUrl = command.toStalkerPlayableUrl().takeIf(String::isNotBlank) ?: return null
        return LiveTvChannel(
            id = this["id"] ?: "stalker-$index-${streamUrl.hashCode()}",
            name = name,
            streamUrl = streamUrl,
            tvgId = this["xmltv_id"] ?: this["tvg_id"],
            logoUrl = (this["logo"] ?: this["logo_url"])?.let { stalkerLogoUrl(session.settings.portalUrl, it) },
            group = (this["tv_genre_id"] ?: this["genre_id"])?.let(genres::get).orEmpty(),
            headers = streamHeaders(session, streamUrl),
            stalkerCommand = command,
        )
    }

    /** The `data` array of a `{js:{data:[...]}}` answer, each entry mapped from its plain fields. */
    private suspend fun <T : Any> dataObjects(
        session: StalkerSession,
        type: String,
        action: String,
        map: (Map<String, String>) -> T?,
    ): List<T> =
        http.stream(url(session.settings, session.token, type, action), authHeaders(session)) { source ->
            readStalkerPage(source, map).entries
        }

    private suspend fun request(
        settings: LiveTvStalkerSettings,
        token: String?,
        type: String,
        action: String,
        extra: Map<String, String> = emptyMap(),
    ): String = http.text(url(settings, token, type, action, extra), stalkerBaseHeaders(settings) + tokenHeader(token))

    private fun authHeaders(session: StalkerSession): Map<String, String> =
        session.authHeaders ?: (stalkerBaseHeaders(session.settings) + tokenHeader(session.token))
            .also { session.authHeaders = it }

    /** The session's Cookie / Authorization only for a stream on the portal's own host (D055). */
    private fun streamHeaders(session: StalkerSession, streamUrl: String): Map<String, String> =
        if (sameHost(streamUrl, session.settings.portalUrl)) authHeaders(session) else stalkerPublicHeaders(session.settings)

    private fun tokenHeader(token: String?): Map<String, String> =
        if (token.isNullOrBlank()) emptyMap() else mapOf("Authorization" to "Bearer $token")

    private companion object {
        const val MAX_PAGES = 500
        const val PARALLEL_PAGES = 4
    }
}

internal fun stalkerUrl(
    settings: LiveTvStalkerSettings,
    token: String?,
    type: String,
    action: String,
    extra: Map<String, String> = emptyMap(),
): String {
    val parameters = buildMap {
        put("type", type)
        put("action", action)
        put("JsHttpRequest", "1-xml")
        if (!token.isNullOrBlank()) put("token", token)
        if (settings.username.isNotBlank()) put("login", settings.username)
        if (settings.password.isNotBlank()) put("password", settings.password)
        putAll(extra)
    }
    val endpoint = stalkerEndpoint(settings.portalUrl)
    return endpoint + parameters.entries.joinToString("&", prefix = if ('?' in endpoint) "&" else "?") { (key, value) ->
        "${key.urlEncoded()}=${value.urlEncoded()}"
    }
}

private fun url(
    settings: LiveTvStalkerSettings,
    token: String?,
    type: String,
    action: String,
    extra: Map<String, String> = emptyMap(),
): String = stalkerUrl(settings, token, type, action, extra)

internal fun stalkerEndpoint(portalUrl: String): String {
    val normalized = portalUrl.trim().trimEnd('/')
    return when {
        normalized.endsWith("portal.php", ignoreCase = true) -> normalized
        normalized.contains("portal.php?", ignoreCase = true) -> normalized
        normalized.endsWith("/c", ignoreCase = true) -> normalized.dropLast(2) + "/portal.php"
        else -> "$normalized/portal.php"
    }
}

/** A MAG box's User-Agent and Referer: safe to send to any stream host. */
private fun stalkerPublicHeaders(settings: LiveTvStalkerSettings): Map<String, String> = mapOf(
    "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; MAG254; en) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 4 rev: 2721 Mobile Safari/533.3",
    "X-User-Agent" to "Model: MAG254; Link: Ethernet",
    "Referer" to settings.portalUrl.trim().substringBefore("/portal.php").trimEnd('/') + "/c/",
)

/** The public headers plus the MAC cookie the portal identifies the box by. */
private fun stalkerBaseHeaders(settings: LiveTvStalkerSettings): Map<String, String> =
    stalkerPublicHeaders(settings) +
        ("Cookie" to "mac=${settings.macAddress}; stb_lang=en; timezone=${java.util.TimeZone.getDefault().id.urlEncoded()}")

internal fun sameHost(a: String, b: String): Boolean {
    val hostA = liveTvHost(a)
    return hostA.isNotEmpty() && hostA.equals(liveTvHost(b), ignoreCase = true)
}

/**
 * A channel logo as a link. Many portals give only the file name ("1234.png"), served from the
 * portal's own logo folder.
 */
internal fun stalkerLogoUrl(portalUrl: String, value: String): String? {
    val logo = value.trim()
    return when {
        logo.isEmpty() -> null
        logo.isHttpUrl() -> logo
        logo.startsWith("//") -> "http:$logo"
        logo.startsWith("/") -> portalOrigin(portalUrl)?.let { it + logo }
        logo.contains("://") -> null
        else -> portalRoot(portalUrl)?.let { "$it/misc/logos/320/$logo" }
    }
}

/** `http://host:port` of the portal. */
private fun portalOrigin(portalUrl: String): String? {
    val url = portalUrl.trim()
    val scheme = url.indexOf("://").takeIf { it > 0 } ?: return null
    val pathStart = url.indexOf('/', scheme + 3)
    return if (pathStart < 0) url.trimEnd('/') else url.substring(0, pathStart)
}

/** The portal's folder ("…/stalker_portal"), which holds its logos. */
private fun portalRoot(portalUrl: String): String? {
    val url = portalUrl.trim().substringBefore('?')
    val marker = url.indexOf("/stalker_portal", ignoreCase = true)
    if (marker >= 0) return url.substring(0, marker + "/stalker_portal".length)
    return portalOrigin(portalUrl)?.let { "$it/stalker_portal" }
}

internal fun String.toStalkerPlayableUrl(): String =
    trim().removePrefix("ffmpeg ").removePrefix("auto ").substringBefore(' ').trim()

/** Runs [load] and, on a failure, once more; null when both failed. */
private suspend fun <T> loadPageTwice(load: suspend () -> T): T? {
    repeat(2) {
        try {
            return load()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
    }
    return null
}

internal class StalkerPage<T>(val entries: List<T>, val totalItems: Int?, val maxPageItems: Int?)

/** Streams a Stalker answer (`{"js":{"total_items":..,"data":[{..},..]}}`, `{"js":[{..}]}` or without `js`). */
internal fun <T : Any> readStalkerPage(source: BufferedSource, map: (Map<String, String>) -> T?): StalkerPage<T> {
    var entries: List<T> = emptyList()
    var total: Int? = null
    var perPage: Int? = null
    val reader = JsonReader.of(source).apply { isLenient = true }
    fun readBody() {
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "js" -> when (reader.peek()) {
                    JsonReader.Token.BEGIN_OBJECT -> readBody()
                    // get_genres answers `{"js":[...]}`: the array is the data (Reshaped skipped it).
                    JsonReader.Token.BEGIN_ARRAY -> entries = reader.readObjectArray(map)
                    else -> reader.skipValue()
                }
                "data" -> entries = if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                    reader.readObjectArray(map)
                } else {
                    reader.skipValue()
                    emptyList()
                }
                "total_items" -> total = reader.nextScalar()?.toIntOrNull()
                "max_page_items" -> perPage = reader.nextScalar()?.toIntOrNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }
    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) readBody()
    return StalkerPage(entries, total, perPage)
}

/** The plain fields of a Stalker answer's `js` object (or of the answer itself without one). */
internal fun readJsFields(text: String): Map<String, String> {
    val reader = JsonReader.of(Buffer().writeUtf8(text)).apply { isLenient = true }
    if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) return emptyMap()
    val outer = HashMap<String, String>()
    var inner: Map<String, String>? = null
    reader.beginObject()
    while (reader.hasNext()) {
        val name = reader.nextName()
        if (name == "js" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            val fields = HashMap<String, String>()
            reader.beginObject()
            while (reader.hasNext()) {
                val field = reader.nextName()
                reader.nextScalar()?.let { fields[field] = it }
            }
            reader.endObject()
            inner = fields
        } else {
            reader.nextScalar()?.let { outer[name] = it }
        }
    }
    reader.endObject()
    return inner ?: outer
}

// endregion

// region Streaming JSON helpers

/** Reads a top-level array of objects (or `{"data":[...]}`), mapping each object's plain fields. */
internal fun <T : Any> readObjects(source: BufferedSource, map: (Map<String, String>) -> T?): List<T> {
    val reader = JsonReader.of(source).apply { isLenient = true }
    return when (reader.peek()) {
        JsonReader.Token.BEGIN_ARRAY -> reader.readObjectArray(map)
        JsonReader.Token.BEGIN_OBJECT -> {
            var result: List<T> = emptyList()
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "data" && reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                    result = reader.readObjectArray(map)
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            result
        }
        else -> emptyList()
    }
}

private fun <T : Any> JsonReader.readObjectArray(map: (Map<String, String>) -> T?): List<T> {
    val result = ArrayList<T>()
    val fields = HashMap<String, String>(16)
    beginArray()
    while (hasNext()) {
        if (peek() != JsonReader.Token.BEGIN_OBJECT || result.size >= LIVE_TV_MAX_CHANNELS_PER_SOURCE) {
            skipValue()
            continue
        }
        fields.clear()
        beginObject()
        while (hasNext()) {
            val name = nextName()
            nextScalar()?.let { fields[name] = it }
        }
        endObject()
        // The map is reused for the next entry: [map] must not keep it.
        map(fields)?.let(result::add)
    }
    endArray()
    return result
}

/** A string, number or boolean as trimmed text; null (and skipped) for blanks, nulls and nested values. */
private fun JsonReader.nextScalar(): String? = when (peek()) {
    JsonReader.Token.STRING, JsonReader.Token.NUMBER -> nextString().trim().takeIf(String::isNotBlank)
    JsonReader.Token.BOOLEAN -> nextBoolean().toString()
    else -> {
        skipValue()
        null
    }
}

// endregion
