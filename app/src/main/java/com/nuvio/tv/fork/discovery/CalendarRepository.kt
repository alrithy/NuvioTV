package com.nuvio.tv.fork.discovery

import android.util.Log
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.tracking.TrackingProgressProviderRegistry
import com.nuvio.tv.core.util.parseEpisodeReleaseLocalDate
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.repository.LibraryRepository
import com.nuvio.tv.domain.repository.MetaRepository
import com.nuvio.tv.domain.repository.WatchProgressRepository
import com.nuvio.tv.fork.resource.AdaptiveResources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G9e Calendar schedule (178–186). FILE_PORT of Cxsmo `CalendarRepositoryImpl` @ 3e0d0fa: series from
 * local / Nuvio Sync progress and watched items, the library and every signed-in tracker (Trakt, Simkl,
 * MDBList through official's provider registry); air dates from the metadata add-ons; watched and
 * spoiler flags refreshed from watched-state changes without refetching metadata.
 * Local bounds: the [CalendarRules.MAX_SHOWS] most recently active series, add-on concurrency from
 * [AdaptiveResources], a per-show timeout. Nothing loads until the Calendar is first opened.
 */
@Singleton
class CalendarRepository @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val libraryRepository: LibraryRepository,
    private val trackingProviders: TrackingProgressProviderRegistry,
    private val metaRepository: MetaRepository,
    private val profileManager: ProfileManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val watchMutex = Mutex()
    private val previousByShow = ConcurrentHashMap<String, Map<Pair<Int, Int>, Pair<Int, Int>>>()
    private val aliasesByShow = ConcurrentHashMap<String, Set<String>>()

    private val _days = MutableStateFlow<List<CalendarDay>>(emptyList())
    val days: StateFlow<List<CalendarDay>> = _days.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var started = false
    private var lastFetchMs = 0L
    private var profileId = -1

    /** Starts loading and watching on the first Calendar visit; later calls refresh when stale. */
    @Synchronized
    fun ensureStarted() {
        if (started) {
            scope.launch { load(force = false) }
            return
        }
        started = true
        _isRefreshing.value = true // the first visit shows loading, not an empty calendar
        scope.launch {
            profileManager.activeProfileId.collect { id ->
                if (id != profileId) {
                    profileId = id
                    lastFetchMs = 0L
                    previousByShow.clear()
                    aliasesByShow.clear()
                    _days.value = emptyList()
                    load(force = true)
                }
            }
        }
        scope.launch { watchProgressRepository.watchedItems.drop(1).collect { rewatch() } }
        trackingProviders.providers().forEach { provider ->
            scope.launch { provider.watchedItems.drop(1).collect { rewatch() } }
        }
    }

    fun refresh() {
        scope.launch { load(force = true) }
    }

    private fun fresh(): Boolean =
        _days.value.isNotEmpty() && System.currentTimeMillis() - lastFetchMs < CalendarRules.CACHE_TTL_MS

    private suspend fun load(force: Boolean) {
        if (!force && fresh()) return
        mutex.withLock {
            if (!force && fresh()) return
            _isRefreshing.value = true
            try {
                val today = LocalDate.now()
                val shows = trackedShows()
                val watched = watchedEpisodes()
                val artwork = savedArtwork()
                val limit = Semaphore(AdaptiveResources.policy.addonFetchConcurrency ?: CalendarRules.DEFAULT_CONCURRENCY)
                val episodes = coroutineScope {
                    shows.map { showId ->
                        async {
                            limit.withPermit {
                                withTimeoutOrNull(SHOW_TIMEOUT_MS) {
                                    episodesFor(showId, today, watched, artwork)
                                }.orEmpty()
                            }
                        }
                    }.awaitAll().flatten()
                }
                _days.value = CalendarRules.groupDays(episodes)
                lastFetchMs = System.currentTimeMillis()
                Log.d(TAG, "calendar: ${shows.size} series, ${episodes.size} episodes")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "calendar load failed: ${error.javaClass.simpleName}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun rewatch() {
        if (_days.value.isEmpty()) return
        watchMutex.withLock {
            val watched = watchedEpisodes()
            _days.value = CalendarRules.rewatch(
                _days.value,
                watchedByShow = { show -> aliasesOf(show).flatMap { watched[it].orEmpty() }.toSet() },
                previousByShow = { show -> previousByShow[show].orEmpty() },
            )
        }
    }

    private fun aliasesOf(showId: String): Set<String> = aliasesByShow[showId] ?: setOf(showId)

    /** Series ids, most recently active first, capped at [CalendarRules.MAX_SHOWS]. */
    private suspend fun trackedShows(): List<String> {
        val recency = HashMap<String, Long>()
        fun add(id: String, at: Long) {
            if (id.isNotBlank()) recency[id] = maxOf(recency[id] ?: Long.MIN_VALUE, at)
        }
        fun isSeries(type: String, season: Int?) = type.equals("series", true) || type.equals("tv", true) || season != null
        runCatching {
            watchProgressRepository.allProgress.firstOrNull().orEmpty()
                .forEach { if (isSeries(it.contentType, it.season)) add(it.contentId, it.lastWatched) }
        }
        runCatching {
            watchProgressRepository.watchedItems.firstOrNull().orEmpty()
                .forEach { if (isSeries(it.contentType, it.season)) add(it.contentId, it.watchedAt) }
        }
        runCatching {
            libraryRepository.libraryItems.firstOrNull().orEmpty()
                .forEach { if (it.type.equals("series", true)) add(it.id, it.listedAt) }
        }
        for (provider in trackingProviders.providers()) {
            if (!runCatching { provider.isAuthenticated.first() }.getOrDefault(false)) continue
            runCatching {
                provider.allProgress.firstOrNull().orEmpty()
                    .forEach { if (isSeries(it.contentType, it.season)) add(it.contentId, it.lastWatched) }
            }
            runCatching {
                provider.nextUpSeeds.firstOrNull().orEmpty()
                    .forEach { if (isSeries(it.contentType, it.season)) add(it.contentId, it.lastWatched) }
            }
            runCatching {
                provider.watchedItems.firstOrNull().orEmpty()
                    .forEach { if (isSeries(it.contentType, it.season)) add(it.contentId, it.watchedAt) }
            }
        }
        return recency.entries.sortedByDescending { it.value }.take(CalendarRules.MAX_SHOWS).map { it.key }
    }

    private suspend fun watchedEpisodes(): Map<String, Set<Pair<Int, Int>>> {
        val map = HashMap<String, MutableSet<Pair<Int, Int>>>()
        runCatching {
            watchProgressRepository.getWatchedShowEpisodes()
                .forEach { (show, episodes) -> map.getOrPut(show, ::HashSet).addAll(episodes) }
        }
        runCatching {
            watchProgressRepository.watchedItems.firstOrNull().orEmpty().forEach { item ->
                if (item.season != null && item.episode != null) {
                    map.getOrPut(item.contentId, ::HashSet).add(item.season to item.episode)
                }
            }
        }
        for (provider in trackingProviders.providers()) {
            if (!runCatching { provider.isAuthenticated.first() }.getOrDefault(false)) continue
            runCatching {
                provider.watchedShowEpisodes()
                    .forEach { (show, episodes) -> map.getOrPut(show, ::HashSet).addAll(episodes) }
            }
        }
        return map
    }

    private data class Artwork(val poster: String?, val backdrop: String?)

    /** Artwork already known from progress and the library, for add-ons that omit it. */
    private suspend fun savedArtwork(): Map<String, Artwork> {
        val map = HashMap<String, Artwork>()
        fun put(id: String, poster: String?, backdrop: String?) {
            val old = map[id]
            map[id] = Artwork(old?.poster.nonBlank() ?: poster.nonBlank(), old?.backdrop.nonBlank() ?: backdrop.nonBlank())
        }
        runCatching { watchProgressRepository.allProgress.firstOrNull().orEmpty().forEach { put(it.contentId, it.poster, it.backdrop) } }
        runCatching { libraryRepository.libraryItems.firstOrNull().orEmpty().forEach { put(it.id, it.poster, it.background) } }
        return map
    }

    private suspend fun episodesFor(
        showId: String,
        today: LocalDate,
        watchedByShow: Map<String, Set<Pair<Int, Int>>>,
        artwork: Map<String, Artwork>,
    ): List<CalendarEpisode> {
        val meta = fetchMeta(showId) ?: return emptyList()
        if (meta.videos.isEmpty()) return emptyList()
        val aliases = setOf(showId, meta.id)
        aliasesByShow[meta.id] = aliases
        val watched = aliases.flatMap { watchedByShow[it].orEmpty() }.toSet()
        val saved = artwork[showId] ?: artwork[meta.id]
        val poster = meta.poster.nonBlank() ?: saved?.poster
        val backdrop = meta.background.nonBlank() ?: meta.landscapePoster.nonBlank() ?: saved?.backdrop
        val previous = CalendarRules.previousEpisodes(meta.videos.mapNotNull { v -> v.season?.let { s -> v.episode?.let { s to it } } })
        previousByShow[meta.id] = previous
        return meta.videos.mapNotNull { video ->
            val season = video.season?.takeIf { it > 0 } ?: return@mapNotNull null
            val number = video.episode ?: return@mapNotNull null
            val airDate = parseEpisodeReleaseLocalDate(video.released) ?: return@mapNotNull null
            if (!CalendarRules.inWindow(airDate, today)) return@mapNotNull null
            val isWatched = (season to number) in watched
            CalendarEpisode(
                showId = meta.id,
                showTitle = meta.name,
                episodeId = video.id,
                seasonNumber = season,
                episodeNumber = number,
                episodeTitle = video.title.trim().takeIf { it.isNotEmpty() },
                airDate = airDate,
                thumbnail = video.thumbnail.nonBlank(),
                showPoster = poster,
                showBackdrop = backdrop,
                overview = video.overview,
                isWatched = isWatched,
                isSpoilerHidden = CalendarRules.spoilerHidden(isWatched, previous[season to number], watched),
            )
        }
    }

    /** Fresh add-on metadata (air dates move), official's cache when no add-on answers. */
    private suspend fun fetchMeta(showId: String): Meta? {
        val result = runCatching {
            metaRepository.getMetaFromAllAddons(type = "series", id = showId)
                .firstOrNull { it !is NetworkResult.Loading }
        }.getOrNull()
        val fetched = when (result) {
            is NetworkResult.Success -> result.data
            else -> null
        }
        return fetched ?: metaRepository.getCachedMeta("series", showId)
    }

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        const val TAG = "ForkCalendar"
        const val SHOW_TIMEOUT_MS = 15_000L
    }
}
