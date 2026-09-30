package com.nuvio.tv.fork.livetv

/*
 * Live TV models (G10a, D055). FILE_PORT of Reshaped `reshaped/livetv/LiveTvModels.kt` @ 0ccf049,
 * adapted: a channel's persisted identity is [LiveTvChannel.key] (a hash of source, category and
 * name), never its stream URL, because Xtream and many M3U links carry the account's password.
 */

/** A channel as the list shows it. [streamUrl] is kept in memory only; [key] is what is saved. */
data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val logoUrl: String? = null,
    val group: String = "",
    val headers: Map<String, String> = emptyMap(),
    /** Stalker only: the command a playable link is created from, per play. */
    val stalkerCommand: String? = null,
    /** The [LiveTvSource.id] this channel was listed by. */
    val sourceId: String = "",
    /** What favorites, hiding and the last channel store: see [liveTvChannelKey]. */
    val key: Long = 0L,
)

/** The category key of channels the playlist gives no category; the screens call it "Uncategorised". */
const val LIVE_TV_UNGROUPED = ""

/**
 * A channel's saved identity: a 64-bit FNV-1a hash of its source, category and name (Reshaped's
 * hide key). A link can be long, carries account details and changes when a provider rotates tokens.
 */
fun liveTvChannelKey(sourceId: String, group: String, name: String): Long {
    var hash = -0x340d631b7bdddcdbL // FNV-1a 64 offset basis
    fun mix(text: String) {
        text.forEach { char -> hash = (hash xor char.code.toLong()) * 0x100000001b3L }
        hash = (hash xor 0x1fL) * 0x100000001b3L
    }
    mix(sourceId)
    mix(group)
    mix(name)
    return hash
}

enum class LiveTvSourceType { M3u, Stalker, Xtream }

data class LiveTvStalkerSettings(
    val portalUrl: String = "",
    val macAddress: String = "",
    val username: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean get() = portalUrl.isNotBlank() && macAddress.isNotBlank()

    fun normalized(): LiveTvStalkerSettings = copy(
        portalUrl = portalUrl.trim().trimEnd('/'),
        macAddress = macAddress.trim().uppercase(),
        username = username.trim(),
        password = password.trim(),
    )

    /** Never prints the MAC or the credentials. */
    override fun toString(): String = "LiveTvStalkerSettings(host=${liveTvHost(portalUrl)})"
}

data class LiveTvXtreamSettings(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    fun normalized(): LiveTvXtreamSettings = copy(
        serverUrl = serverUrl.trim().trimEnd('/').substringBefore("/player_api.php").trimEnd('/'),
        username = username.trim(),
        password = password.trim(),
    )

    /** Never prints the credentials. */
    override fun toString(): String = "LiveTvXtreamSettings(host=${liveTvHost(serverUrl)})"
}

/** One saved channel source. Several can be added; their channels show as one list. */
data class LiveTvSource(
    val id: String,
    val type: LiveTvSourceType,
    /** The M3U link or an imported file's name; the server or portal URL for the others. */
    val url: String = "",
    val stalker: LiveTvStalkerSettings = LiveTvStalkerSettings(),
    val xtream: LiveTvXtreamSettings = LiveTvXtreamSettings(),
) {
    /** A short name for lists: the host of a link, or the imported file's name (never a query or user). */
    val label: String get() = liveTvHost(url).ifBlank { url.substringBefore('?') }

    /** Two sources with the same identity are one: adding it again replaces it. */
    internal val identity: String
        get() = when (type) {
            LiveTvSourceType.M3u -> "m3u|${url.lowercase()}"
            LiveTvSourceType.Xtream -> "xtream|${xtream.serverUrl.lowercase()}|${xtream.username}"
            LiveTvSourceType.Stalker -> "stalker|${stalker.portalUrl.lowercase()}|${stalker.macAddress}"
        }

    /** Never prints the link (which may hold credentials). */
    override fun toString(): String = "LiveTvSource(id=$id, type=$type, host=${liveTvHost(url)})"
}

/** A load failure the screen shows as a translated message. */
enum class LiveTvError {
    InvalidUrl, NoChannels, LoadFailed, FileEmpty, FileNoChannels,
    StalkerRequired, StalkerInvalidUrl, StalkerNoChannels, StalkerFailed, StalkerToken,
    /** The list loaded, but some of the portal's pages did not: a notice, not a failed load. */
    StalkerIncomplete,
    XtreamRequired, XtreamInvalidUrl, XtreamNoChannels, XtreamFailed,
}

internal class LiveTvException(val error: LiveTvError) : Exception(error.name)

/** What the repository publishes for the active profile. */
data class LiveTvState(
    val sources: List<LiveTvSource> = emptyList(),
    /** Every source's channels, in the order the sources were added. */
    val channels: List<LiveTvChannel> = emptyList(),
    /** How many channels each source listed. */
    val sourceCounts: Map<String, Int> = emptyMap(),
    /** Sources whose last load failed (their earlier channels, if any, stay listed). */
    val sourceErrors: Map<String, LiveTvError> = emptyMap(),
    /** Guide links the sources announced (read in G10c). */
    val epgUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    /** The last failed attempt to add a source. */
    val error: LiveTvError? = null,
    /** Goes up each time a source is added, so the source dialog can tell an add went through. */
    val addedCount: Int = 0,
) {
    val hasSource: Boolean get() = sources.isNotEmpty()
}

/** The host of [url] (no scheme, user, port, path or query), for labels and logs. */
fun liveTvHost(url: String?): String {
    if (url.isNullOrBlank()) return ""
    val afterScheme = url.substringAfter("://", "")
    if (afterScheme.isEmpty()) return ""
    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    val host = authority.substringAfterLast('@')
    return if (host.startsWith("[")) host.substringBefore(']') + "]" else host.substringBefore(':')
}
