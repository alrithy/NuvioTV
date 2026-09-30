package com.nuvio.tv.fork.discovery

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * G9e Calendar (178–187). FILE_PORT of Cxsmo `CalendarModels.kt` and the pure parts of
 * `CalendarRepositoryImpl` @ 3e0d0fa (window, day grouping, spoiler rule). Pure.
 */
data class CalendarEpisode(
    val showId: String,
    val showTitle: String,
    val episodeId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String?,
    val airDate: LocalDate,
    val thumbnail: String? = null,
    val showPoster: String? = null,
    val showBackdrop: String? = null,
    val overview: String? = null,
    val isWatched: Boolean = false,
    val isSpoilerHidden: Boolean = false,
) {
    val key: Pair<Int, Int> get() = seasonNumber to episodeNumber

    /** Artwork in preference order; the episode still is left out while it is a spoiler. */
    fun artworkCandidates(): List<String> =
        listOfNotNull(thumbnail.takeUnless { isSpoilerHidden }, showBackdrop, showPoster)
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
}

data class CalendarDay(val date: LocalDate, val episodes: List<CalendarEpisode>)

enum class CalendarFilter { ALL, UPCOMING, PAST }

object CalendarRules {
    /** The Calendar entry exists only while DISCOVERY_SKIP_RECOMMENDATIONS is on. */
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF

    const val PAST_DAYS = 30L
    const val FUTURE_DAYS = 90L

    /** Cxsmo fetches every tracked show; the fork caps it at the most recently active ones. */
    const val MAX_SHOWS = 80
    const val CACHE_TTL_MS = 30 * 60 * 1000L
    const val DEFAULT_CONCURRENCY = 6

    fun inWindow(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today.minusDays(PAST_DAYS)) && !date.isAfter(today.plusDays(FUTURE_DAYS))

    /** Each regular episode mapped to the one before it, specials (season 0) left out. */
    fun previousEpisodes(keys: Collection<Pair<Int, Int>>): Map<Pair<Int, Int>, Pair<Int, Int>> =
        keys.filter { it.first > 0 }
            .distinct()
            .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .zipWithNext { previous, current -> current to previous }
            .toMap()

    /** Cxsmo's spoiler rule (187): an unwatched episode whose previous episode is unwatched too. */
    fun spoilerHidden(isWatched: Boolean, previous: Pair<Int, Int>?, watched: Set<Pair<Int, Int>>): Boolean =
        !isWatched && previous != null && previous !in watched

    /** Days in date order, episodes by show then season and episode. */
    fun groupDays(episodes: List<CalendarEpisode>): List<CalendarDay> =
        episodes.groupBy { it.airDate }
            .map { (date, list) ->
                CalendarDay(
                    date,
                    list.sortedWith(
                        compareBy<CalendarEpisode> { it.showTitle.lowercase() }
                            .thenBy { it.seasonNumber }
                            .thenBy { it.episodeNumber },
                    ),
                )
            }
            .sortedBy { it.date }

    fun filter(days: List<CalendarDay>, filter: CalendarFilter, today: LocalDate): List<CalendarDay> = when (filter) {
        CalendarFilter.ALL -> days
        CalendarFilter.UPCOMING -> days.filter { !it.date.isBefore(today) }
        CalendarFilter.PAST -> days.filter { it.date.isBefore(today) }
    }

    /** Index of the first day from today on, for the initial scroll; 0 when every day is past. */
    fun firstUpcomingIndex(days: List<CalendarDay>, today: LocalDate): Int =
        days.indexOfFirst { !it.date.isBefore(today) }.coerceAtLeast(0)

    /** Re-applies watched and spoiler flags without refetching metadata (186). */
    fun rewatch(
        days: List<CalendarDay>,
        watchedByShow: (String) -> Set<Pair<Int, Int>>,
        previousByShow: (String) -> Map<Pair<Int, Int>, Pair<Int, Int>>,
    ): List<CalendarDay> = days.map { day ->
        day.copy(
            episodes = day.episodes.map { episode ->
                val watched = watchedByShow(episode.showId)
                val isWatched = episode.key in watched
                episode.copy(
                    isWatched = isWatched,
                    isSpoilerHidden = spoilerHidden(isWatched, previousByShow(episode.showId)[episode.key], watched),
                )
            },
        )
    }

    /** Days from [today]; negative for the past. */
    fun daysFrom(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)
}
