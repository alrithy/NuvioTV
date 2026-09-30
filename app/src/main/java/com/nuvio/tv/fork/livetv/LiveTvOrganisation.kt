package com.nuvio.tv.fork.livetv

/*
 * Categories, favorites, hiding and search (G10b, features 219–224). ALGORITHM_PORT of Reshaped
 * `LiveTvRepository.orderedGroups` / `shownChannels` and `LiveTvScreen.filterChannels` @ 0ccf049,
 * keyed by [LiveTvChannel.key] instead of the stream URL.
 */

/** The category column's filter keys; a plain category name is its own key. */
object LiveTvFilterKeys {
    const val ALL = "\u0000all"
    const val FAVORITES = "\u0000favorites"
    const val SOURCE_PREFIX = "\u0000source:"

    fun source(id: String) = SOURCE_PREFIX + id
}

internal object LiveTvOrganisation {

    /** [names] in the viewer's [order], then the rest A to Z (by the name shown), "Uncategorised" last. */
    fun orderedGroups(names: Set<String>, order: List<String>, renamed: Map<String, String>): List<String> {
        val ordered = order.filterTo(ArrayList()) { it in names }
        val placed = ordered.toHashSet()
        names.filterNot(placed::contains)
            .sortedWith(
                compareBy<String> { it == LIVE_TV_UNGROUPED && it !in renamed }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { renamed[it]?.trim() ?: it },
            )
            .forEach(ordered::add)
        return ordered
    }

    /** The channels All channels and zapping go through. */
    fun shownChannels(channels: List<LiveTvChannel>, hiddenGroups: Set<String>, hiddenKeys: Set<Long>): List<LiveTvChannel> =
        if (hiddenGroups.isEmpty() && hiddenKeys.isEmpty()) {
            channels
        } else {
            channels.filter { it.group !in hiddenGroups && it.key !in hiddenKeys }
        }

    /**
     * The channels a filter key shows: hidden categories and channels leave everything but
     * favorites, and a search matches names. Slow for big lists: call off the main thread.
     */
    fun filter(channels: List<LiveTvChannel>, library: LiveTvLibrary, key: String, query: String = ""): List<LiveTvChannel> {
        val needle = query.trim()
        val hidden = library.hiddenGroups
        val hiddenChannels = library.hiddenChannels
        return channels.filter { channel ->
            when {
                key == LiveTvFilterKeys.ALL -> channel.group !in hidden && channel.key !in hiddenChannels
                key == LiveTvFilterKeys.FAVORITES -> channel.key in library.favorites
                key.startsWith(LiveTvFilterKeys.SOURCE_PREFIX) ->
                    channel.sourceId == key.removePrefix(LiveTvFilterKeys.SOURCE_PREFIX) &&
                        channel.group !in hidden && channel.key !in hiddenChannels
                else -> channel.group == key && channel.key !in hiddenChannels
            } && (needle.isEmpty() || channel.name.contains(needle, ignoreCase = true))
        }
    }

    /** Whether [key] still names something the list shows (a hidden category or a removed source falls back to All). */
    fun isStale(key: String, state: LiveTvState): Boolean = when {
        key == LiveTvFilterKeys.ALL || key == LiveTvFilterKeys.FAVORITES -> false
        key.startsWith(LiveTvFilterKeys.SOURCE_PREFIX) ->
            state.sources.none { it.id == key.removePrefix(LiveTvFilterKeys.SOURCE_PREFIX) }
        else -> key in state.library.hiddenGroups
    }

    /** [groups] with [group] moved [step] places; unchanged when that leaves the list. */
    fun move(groups: List<String>, group: String, step: Int): List<String>? {
        val from = groups.indexOf(group)
        val to = from + step
        if (from < 0 || to !in groups.indices) return null
        return ArrayList(groups).apply { add(to, removeAt(from)) }
    }

    /** The name the viewer gave [group], or null. */
    fun customName(group: String, names: Map<String, String>): String? = names[group]?.trim()?.takeIf(String::isNotEmpty)
}
