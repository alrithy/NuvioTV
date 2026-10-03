package com.nuvio.tv.ui.screens.mynetflix

import androidx.compose.runtime.Immutable
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.model.WatchedItem

/*
 * NETFLIX_THEME My Netflix hub (docs/NETFLIX_TV_2026_PARITY_AUDIT.md §6). Every section is backed by
 * a data owner Nuvio already has and that works without Trakt/MDBList/SIMKL: the profile's library,
 * its local watch progress and its watched history. A section with no data is omitted; nothing is
 * invented (no reminders, ratings or downloads: Nuvio has no owner for those).
 */

internal const val MY_NETFLIX_CONTINUE_LIMIT = 20
internal const val MY_NETFLIX_LIST_LIMIT = 40
internal const val MY_NETFLIX_HISTORY_LIMIT = 20

@Immutable
internal data class MyNetflixCard(
    val key: String,
    val contentId: String,
    val contentType: String,
    val title: String,
    /** 16:9 artwork when known, poster otherwise; null draws the dark fallback, never a bright box. */
    val imageUrl: String?,
    val addonBaseUrl: String?,
    /** 0..1 for a resumable title; null when there is no real progress. */
    val progress: Float? = null,
    /** S1 E2 style episode tokens are formatted by the UI with bidi isolation. */
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val resume: WatchProgress? = null,
)

internal enum class MyNetflixSection { CONTINUE_WATCHING, MY_LIST, RECENTLY_WATCHED }

@Immutable
internal data class MyNetflixRow(val section: MyNetflixSection, val cards: List<MyNetflixCard>)

@Immutable
internal data class MyNetflixHubState(
    val loading: Boolean = true,
    val rows: List<MyNetflixRow> = emptyList(),
    val libraryCount: Int = 0,
) {
    val isEmpty: Boolean get() = !loading && rows.isEmpty()
}

internal fun buildMyNetflixHub(
    library: List<LibraryEntry>,
    continueWatching: List<WatchProgress>,
    watched: List<WatchedItem>,
    allProgress: List<WatchProgress>,
): MyNetflixHubState {
    val resumable = continueWatching.asSequence()
        .filter { it.contentId.isNotBlank() && it.name.isNotBlank() && it.isInProgress() }
        .sortedByDescending { it.lastWatched }
        .distinctBy { it.contentId }
        .take(MY_NETFLIX_CONTINUE_LIMIT)
        .map { progress ->
            MyNetflixCard(
                key = "continue:${progress.contentType}:${progress.contentId}",
                contentId = progress.contentId,
                contentType = progress.contentType,
                title = progress.name,
                imageUrl = progress.backdrop ?: progress.poster,
                addonBaseUrl = progress.addonBaseUrl,
                progress = progress.progressPercentage,
                season = progress.season,
                episode = progress.episode,
                episodeTitle = progress.episodeTitle,
                resume = progress,
            )
        }.toList()

    val myList = library.asSequence()
        .filter { it.id.isNotBlank() && it.name.isNotBlank() }
        .sortedByDescending { it.listedAt }
        .distinctBy { "${it.type}:${it.id}" }
        .take(MY_NETFLIX_LIST_LIMIT)
        .map { entry ->
            MyNetflixCard(
                key = "list:${entry.type}:${entry.id}",
                contentId = entry.id,
                contentType = entry.type,
                title = entry.name,
                imageUrl = entry.landscapePoster ?: entry.background ?: entry.poster,
                addonBaseUrl = entry.addonBaseUrl,
            )
        }.toList()

    // Finished titles: explicit watched history plus progress past its completion threshold. Titles
    // still resumable above are not repeated here.
    val resumingIds = resumable.mapTo(HashSet()) { it.contentId }
    val completedArtwork = allProgress.asSequence()
        .filter { it.isCompleted() }
        .associateBy({ it.contentId }, { it.backdrop ?: it.poster })
    val history = (
        watched.asSequence().map { Triple(it, it.watchedAt, completedArtwork[it.contentId] ?: it.poster) } +
            allProgress.asSequence().filter { it.isCompleted() }.map { progress ->
                Triple(
                    WatchedItem(progress.contentId, progress.contentType, progress.name, watchedAt = progress.lastWatched,
                        poster = progress.poster),
                    progress.lastWatched,
                    progress.backdrop ?: progress.poster,
                )
            }
        )
        .filter { (item, _, _) -> item.contentId.isNotBlank() && item.title.isNotBlank() && item.contentId !in resumingIds }
        .sortedByDescending { it.second }
        .distinctBy { it.first.contentId }
        .take(MY_NETFLIX_HISTORY_LIMIT)
        .map { (item, _, image) ->
            MyNetflixCard(
                key = "watched:${item.contentType}:${item.contentId}",
                contentId = item.contentId,
                contentType = item.contentType,
                title = item.title,
                imageUrl = image,
                addonBaseUrl = null,
            )
        }.toList()

    return MyNetflixHubState(
        loading = false,
        rows = buildList {
            if (resumable.isNotEmpty()) add(MyNetflixRow(MyNetflixSection.CONTINUE_WATCHING, resumable))
            if (myList.isNotEmpty()) add(MyNetflixRow(MyNetflixSection.MY_LIST, myList))
            if (history.isNotEmpty()) add(MyNetflixRow(MyNetflixSection.RECENTLY_WATCHED, history))
        },
        libraryCount = library.size,
    )
}
