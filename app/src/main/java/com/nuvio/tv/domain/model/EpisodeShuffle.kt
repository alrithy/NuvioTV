package com.nuvio.tv.domain.model

import com.nuvio.tv.fork.discovery.ShuffleRules
import javax.inject.Inject
import javax.inject.Singleton

data class EpisodeShuffleSettings(
    val enabled: Boolean = false,
    val includeWatched: Boolean = false,
    /** Superfork G9d (171): only this season; null = every season. */
    val season: Int? = null,
    /** Superfork G9d (174–177): the picked episode stays hidden until playback starts. */
    val mystery: Boolean = false,
    /** Superfork G9d (173): unwatched first, every episode once all are watched (opt-in). */
    val fallbackToWatched: Boolean = false
)

enum class ShuffleSurface { DETAIL, HOME, PLAYBACK }

@Singleton
class EpisodeShuffle @Inject constructor() {
    private data class Key(
        val profileId: Int,
        val contentId: String,
        val surface: ShuffleSurface,
        val includeWatched: Boolean,
        val season: Int? = null
    )

    private class Session {
        var picker: RandomEpisodePicker? = null
        var selected: Video? = null
        var current: Pair<Int, Int>? = null
        var visit: Long = 0
    }

    private val sessions = LinkedHashMap<Key, Session>(16, 0.75f, true)
    /** Superfork G9d: episodes the shuffle dialog handed to playback, per profile and show. */
    private val handedOff = LinkedHashMap<Pair<Int, String>, String>()

    @Synchronized
    fun select(
        profileId: Int,
        contentId: String,
        videos: List<Video>,
        includeWatched: Boolean,
        watched: Set<Pair<Int, Int>> = emptySet(),
        progress: Map<Pair<Int, Int>, WatchProgress> = emptyMap(),
        surface: ShuffleSurface,
        current: Pair<Int, Int>? = null,
        visit: Long = 0,
        preferredVideoId: String? = null,
        season: Int? = null,
        fallbackToWatched: Boolean = false
    ): Video? {
        val key = Key(profileId, contentId, surface, includeWatched, season)
        val session = sessions.getOrPut(key, ::Session)
        while (sessions.size > 96) sessions.remove(sessions.keys.first())
        // Superfork G9d: the season scope and the all-watched fallback (173).
        val scoped = ShuffleRules.scope(videos, season) { it.season }
        val picker = RandomEpisodePicker(contentId, scoped, watched, progress)
        // The episode playing now is never picked, so it does not count as one left to watch.
        val currentUnwatched = current != null &&
            scoped.any { it.season to it.episode == current && picker.find(it.id, includeWatched = false) != null }
        val unwatchedLeft = picker.count(false) - if (currentUnwatched) 1 else 0
        @Suppress("NAME_SHADOWING")
        val includeWatched = ShuffleRules.includeWatched(includeWatched, fallbackToWatched, unwatchedLeft)
        picker.inheritHistoryFrom(session.picker)
        if (current != null && (session.picker == null || session.current != current)) picker.recordPlayed(current)
        session.picker = picker
        if (session.current != current || session.visit != visit) session.selected = null
        session.current = current
        session.visit = visit
        session.selected = session.selected?.let { picker.find(it.id, includeWatched, current) }
            ?: preferredVideoId?.let { picker.find(it, includeWatched, current) }
            ?: picker.pick(includeWatched, current, consumeSelection = surface != ShuffleSurface.PLAYBACK || current == null)
        return session.selected
    }

    /** Superfork G9d: whether [videoId] is the episode a shuffle surface picked for [contentId]. */
    @Synchronized
    fun isSelected(profileId: Int, contentId: String, videoId: String): Boolean =
        handedOff[profileId to contentId] == videoId || sessions.any { (key, session) ->
            key.profileId == profileId && key.contentId == contentId && session.selected?.id == videoId
        }

    /** Superfork G9d: the shuffle dialog's pick, so later surfaces treat it as a shuffle pick. */
    @Synchronized
    fun markHandedOff(profileId: Int, contentId: String, videoId: String) {
        handedOff[profileId to contentId] = videoId
        while (handedOff.size > 32) handedOff.remove(handedOff.keys.first())
    }

    @Synchronized
    fun clearSelection(profileId: Int, contentId: String, surface: ShuffleSurface) {
        sessions.filterKeys {
            it.profileId == profileId && it.contentId == contentId && it.surface == surface
        }.values.forEach { it.selected = null }
    }
}
