package com.nuvio.tv.fork.livetv

import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.fork.resource.AdaptiveResources
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Live TV's sources and channels for the active profile (G10a, features 208, 212; D055). FILE_PORT
 * of Reshaped `reshaped/livetv/LiveTvRepository.kt` @ 0ccf049, adapted from a process-wide `object`
 * to a Hilt singleton bound to [ProfileManager]'s active profile; the guide (G10c) and the category /
 * favorite state (G10b) join it in their slices.
 *
 * Several sources can be saved; their channels show as one list, in the order the sources were
 * added. Each source loads in its own job (two at once), so adding, removing or refreshing one
 * cancels only that source's load, and a source that fails keeps the channels it had. Everything is
 * let go once nothing has shown Live TV for [IDLE_RELEASE_MS]. With LIVE_TV OFF nothing loads.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvRepository internal constructor(
    private val store: LiveTvSourceStore,
    private val libraryStore: LiveTvLibraryStore,
    private val http: LiveTvFetcher,
    private val activeProfileId: StateFlow<Int>,
    val featureEnabled: Boolean,
    /** Guides (G10c); none in tests that do not read one. */
    private val guideFiles: LiveTvGuideFiles? = null,
    /** AdaptiveResources' constrained tier: a smaller guide window. */
    private val constrained: () -> Boolean = { AdaptiveResources.policy.isConstrained },
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @Inject constructor(
        store: LiveTvStorage,
        guides: LiveTvGuideStore,
        profileManager: ProfileManager,
        registry: FeatureRegistry,
    ) : this(
        store, store, LiveTvHttp(), profileManager.activeProfileId, registry.mode(FeatureId.LIVE_TV) != FeatureMode.OFF,
        guideFiles = guides,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val xtream = LiveTvXtream(http)
    private val stalker = LiveTvStalker(http)

    private val _state = MutableStateFlow(LiveTvState())
    val state: StateFlow<LiveTvState> = _state.asStateFlow()

    /** Results, state changes and [publish] run here one at a time, so no two of them race. */
    private val serial = dispatcher.limitedParallelism(1)
    /** Saves run in order on one thread: an older save can never land after a newer one. */
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val loadPermits = Semaphore(PARALLEL_SOURCES)
    private val sourceJobs = ConcurrentHashMap<String, Job>()

    @Volatile private var loadedProfileId: Int? = null
    private var profileJob: Job? = null
    private var idleJob: Job? = null

    private var epgJob: Job? = null
    @Volatile private var epgGeneration = 0
    /** The guide programmes kept for each [LiveTvChannel.guideKey], for "up next" and the guide. */
    @Volatile private var keptSchedule: LiveTvSchedule = emptyMap()
    private var epgKey: Pair<List<String>, Set<String>>? = null
    /** A guide read waiting for the sources still loading; set and started on [serial]. */
    @Volatile private var pendingEpg: Triple<List<String>, List<LiveTvChannel>, Set<String>>? = null
    /** Set by Refresh: the next guide read downloads every guide again, however recent its saved copy. */
    @Volatile private var forceGuideDownload = false

    /** What each source loaded last. Only touched on [serial]. */
    private class LoadedSource(val channels: List<LiveTvChannel>, val epgUrls: List<String>)
    private val loaded = HashMap<String, LoadedSource>()

    /**
     * Makes the state match the active profile: the first call (or a profile switch, or the first
     * after Live TV was let go while unused) reads the saved sources and loads their channels;
     * later calls for the same profile do nothing. Returns true when it started a load.
     */
    fun ensureLoaded(): Boolean {
        if (!featureEnabled) return false
        val profileId = activeProfileId.value
        if (loadedProfileId == profileId) return false
        loadedProfileId = profileId
        profileJob?.cancel()
        cancelAllLoads()
        stalker.clearSessions()
        _state.value = LiveTvState(isLoading = true)
        profileJob = scope.launch(serial) {
            stopEpg()
            epgKey = null
            pendingEpg = null
            val sources = readOrDefault("Live TV sources unreadable", emptyList()) { store.sources(profileId) }
            val library = readOrDefault("Live TV choices unreadable", LiveTvLibrary()) { libraryStore.library(profileId) }
            if (loadedProfileId != profileId) return@launch
            loaded.clear()
            _state.value = LiveTvState(
                sources = sources,
                library = library,
                isLoading = sources.isNotEmpty(),
                isLoaded = sources.isEmpty(),
            )
            sources.forEach { launchSourceLoad(profileId, it, adding = false) }
        }
        watchIdle()
        return true
    }

    /**
     * Lets go of everything Live TV holds once nothing has shown it for [IDLE_RELEASE_MS]: after
     * going back to Home only the saved sources remain. A collector of [state] counts as showing it.
     */
    private fun watchIdle() {
        if (idleJob?.isActive == true) return
        idleJob = scope.launch {
            while (isActive) {
                _state.subscriptionCount.first { it == 0 }
                val back = withTimeoutOrNull(IDLE_RELEASE_MS) { _state.subscriptionCount.first { it > 0 } }
                if (back != null) continue
                val released = withContext(serial) {
                    if (_state.subscriptionCount.value > 0) false else true.also { releaseAll() }
                }
                if (released) return@launch
            }
        }
    }

    private suspend fun <T> readOrDefault(what: String, default: T, read: suspend () -> T): T = try {
        read()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        LiveTvLog.warn(what, null, error)
        default
    }

    /** Runs on [serial]. */
    private fun releaseAll() {
        profileJob?.cancel()
        profileJob = null
        cancelAllLoads()
        stopEpg()
        epgKey = null
        pendingEpg = null
        loaded.clear()
        stalker.clearSessions()
        loadedProfileId = null
        _state.value = LiveTvState()
    }

    /** Loads every saved source again (the Refresh button). */
    fun refresh() {
        val profileId = loadedProfileId ?: return
        _state.update { it.copy(error = null) }
        scope.launch(serial) {
            // The guides are downloaded again too, once the channels are back.
            forceGuideDownload = true
            epgKey = null
            _state.value.sources.forEach { launchSourceLoad(profileId, it, adding = false) }
        }
    }

    fun addM3uUrl(url: String) {
        launchAdd(LiveTvSource("", LiveTvSourceType.M3u, url.trim()))
    }

    fun addXtream(settings: LiveTvXtreamSettings) {
        val normalized = settings.normalized()
        launchAdd(LiveTvSource("", LiveTvSourceType.Xtream, normalized.serverUrl, xtream = normalized))
    }

    fun addStalker(settings: LiveTvStalkerSettings) {
        val normalized = settings.normalized()
        launchAdd(LiveTvSource("", LiveTvSourceType.Stalker, normalized.portalUrl, stalker = normalized))
    }

    /** Removes one source and its channels; the other sources' loads carry on. */
    fun removeSource(sourceId: String) {
        val profileId = loadedProfileId ?: return
        sourceJobs.remove(sourceId)?.cancel()
        scope.launch(serial) {
            if (loadedProfileId != profileId) return@launch
            val sources = _state.value.sources.filterNot { it.id == sourceId }
            if (sources.size == _state.value.sources.size) return@launch
            stalker.clearSessions()
            loaded.remove(sourceId)
            save(profileId, sources, deletePlaylistOf = sourceId)
            publish(sources) { it.copy(sourceErrors = it.sourceErrors - sourceId, error = null) }
            updateLoading()
        }
    }

    // region Organisation (G10b)

    /** Whether Live TV shows in the menu for the active profile: off until the user turns it on. */
    val menuEnabled: Flow<Boolean> =
        if (!featureEnabled) flowOf(false) else activeProfileId.flatMapLatest { libraryStore.menuEnabled(it) }

    fun setMenuEnabled(enabled: Boolean) {
        val profileId = activeProfileId.value
        scope.launch(writer) {
            try {
                libraryStore.setMenuEnabled(profileId, enabled)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                LiveTvLog.warn("Live TV menu switch not saved", null, error)
            }
        }
    }

    fun toggleFavorite(channel: LiveTvChannel) = updateLibrary { state ->
        val favorites = state.library.favorites.toHashSet()
        if (!favorites.add(channel.key)) favorites.remove(channel.key)
        state.library.copy(favorites = favorites)
    }

    /** Shows or hides a category. */
    fun setGroupHidden(group: String, hidden: Boolean) = updateLibrary { state ->
        val groups = state.library.hiddenGroups
        state.library.copy(hiddenGroups = if (hidden) groups + group else groups - group)
    }

    /** Shows every category, or hides every one (to then pick the few that are wanted). */
    fun setAllGroupsHidden(hidden: Boolean) = updateLibrary { state ->
        state.library.copy(hiddenGroups = if (hidden) state.groups.toHashSet() else emptySet())
    }

    /** Shows or hides single channels (a whole category's at once for Show all / Hide all). */
    fun setChannelsHidden(channels: Collection<LiveTvChannel>, hidden: Boolean) = updateLibrary { state ->
        val keys = channels.map { it.key }
        val current = state.library.hiddenChannels
        state.library.copy(hiddenChannels = if (hidden) current + keys else current - keys.toSet())
    }

    /** Moves a category [step] places up (negative) or down; the order is kept for every list. */
    fun moveGroup(group: String, step: Int) = updateLibrary { state ->
        LiveTvOrganisation.move(state.groups, group, step)?.let { state.library.copy(groupOrder = it) } ?: state.library
    }

    /** Back to A to Z. */
    fun resetGroupOrder() = updateLibrary { it.library.copy(groupOrder = emptyList()) }

    /**
     * Gives a category a name of its own (blank goes back to the playlist's name). It keeps its
     * channels, hiding and place: everything still goes by the playlist's name.
     */
    fun renameGroup(group: String, name: String) = updateLibrary { state ->
        val names = state.library.groupNames
        state.library.copy(groupNames = if (name.isBlank()) names - group else names + (group to name))
    }

    /** Remembers [channel] (the list's own entry) as the last channel watched. */
    fun recordRecentChannel(channel: LiveTvChannel) = updateLibrary { state ->
        val recent = LiveTvRecentChannel(channel.key, channel.name, channel.logoUrl, channel.group, channel.tvgId)
        if (state.library.recent == recent) state.library else state.library.copy(recent = recent)
    }

    /** The list entry of the last channel watched, while a source still lists it. */
    fun recentChannel(state: LiveTvState = _state.value): LiveTvChannel? {
        val key = state.library.recent?.key ?: return null
        return state.channels.firstOrNull { it.key == key }
    }

    /**
     * Applies [change] to the profile's choices at once (a text field typing a category name reads
     * them straight back), reorders the categories in the same update, and saves them in order. The
     * channels zapping goes through are refiltered off the caller's thread when hiding changed. A
     * change that leaves the choices as they were does nothing.
     */
    private fun updateLibrary(change: (LiveTvState) -> LiveTvLibrary) {
        val profileId = loadedProfileId ?: return
        var saved: LiveTvLibrary? = null
        var hidingChanged = false
        _state.update { state ->
            val current = state.library
            val next = change(state)
            saved = next.takeIf { it != current }
            hidingChanged = next.hiddenGroups != current.hiddenGroups || next.hiddenChannels != current.hiddenChannels
            when {
                next == current -> state
                next.groupOrder != current.groupOrder || next.groupNames != current.groupNames -> state.copy(
                    library = next,
                    groups = LiveTvOrganisation.orderedGroups(state.groupCounts.keys, next.groupOrder, next.groupNames),
                )
                else -> state.copy(library = next)
            }
        }
        val next = saved ?: return
        if (hidingChanged) refreshShownChannels()
        scope.launch(writer) {
            try {
                libraryStore.saveLibrary(profileId, next)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                LiveTvLog.warn("Live TV choices not saved", null, error)
            }
        }
    }

    /** Recomputes the channels zapping goes through after a category or channel was hidden or shown. */
    private fun refreshShownChannels() {
        scope.launch(serial) {
            val state = _state.value
            val shown = LiveTvOrganisation.shownChannels(state.channels, state.library.hiddenGroups, state.library.hiddenChannels)
            _state.update { current ->
                // A newer list or newer hiding redoes this itself.
                if (current.channels === state.channels && current.library.hiddenGroups === state.library.hiddenGroups &&
                    current.library.hiddenChannels === state.library.hiddenChannels
                ) {
                    current.copy(shownChannels = shown)
                } else {
                    current
                }
            }
        }
    }

    // endregion

    /** The channel with a link that plays now: Stalker links are created per play; others are as listed. */
    suspend fun playableChannel(channel: LiveTvChannel): LiveTvChannel {
        val source = _state.value.sources.firstOrNull { it.id == channel.sourceId }
        if (source?.type != LiveTvSourceType.Stalker || channel.stalkerCommand == null) return channel
        return try {
            stalker.resolve(source.stalker, channel)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            LiveTvLog.warn("Stalker link failed, trying the listed link", source.stalker.portalUrl, error)
            channel
        }
    }

    // region Loads

    private fun cancelAllLoads() {
        sourceJobs.values.forEach(Job::cancel)
        sourceJobs.clear()
    }

    /** The screen shows "Loading" while any source is loading. */
    private fun updateLoading() {
        val busy = sourceJobs.values.any { it.isActive }
        _state.update { if (it.isLoading == busy) it else it.copy(isLoading = busy) }
        if (!busy && pendingEpg != null) {
            scope.launch(serial) {
                val pending = pendingEpg ?: return@launch
                if (sourceJobs.values.any { it.isActive }) return@launch
                pendingEpg = null
                startEpg(pending.first, pending.second, pending.third)
            }
        }
    }

    /** A source being added keeps the id of a saved one that is the same source, so it replaces it. */
    private fun withExistingId(candidate: LiveTvSource): LiveTvSource {
        val existing = _state.value.sources.firstOrNull { it.identity == candidate.identity }
        return candidate.copy(id = existing?.id ?: store.newSourceId())
    }

    /**
     * Loads a new source (or a saved one entered again) and adds it once it listed channels; a
     * failed attempt changes nothing but the error shown.
     */
    private fun launchAdd(input: LiveTvSource) {
        val profileId = loadedProfileId ?: return
        val validation = validate(input)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        _state.update { it.copy(error = null) }
        launchSourceLoad(profileId, if (input.id.isBlank()) withExistingId(input) else input, adding = true)
    }

    /** Loads one source in its own job, replacing only an earlier load of the same source. */
    private fun launchSourceLoad(profileId: Int, source: LiveTvSource, adding: Boolean) {
        sourceJobs.remove(source.id)?.cancel()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val outcome = loadPermits.withPermit {
                try {
                    Result.success(loadSource(profileId, source))
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    LiveTvLog.warn("Live TV ${source.type} source failed", source.url, error)
                    Result.failure(error)
                }
            }
            val self = coroutineContext[Job]
            withContext(serial) {
                // A newer load of this source, its removal or a profile switch wins over this result.
                if (sourceJobs[source.id] !== self || loadedProfileId != profileId) return@withContext
                val known = _state.value.sources.any { it.id == source.id }
                if (!adding && !known) return@withContext
                outcome.onSuccess { (result, notice) ->
                    loaded[source.id] = result
                    var sources = _state.value.sources
                    if (adding) {
                        sources = if (known) sources.map { if (it.id == source.id) source else it } else sources + source
                        val staleFile = source.type != LiveTvSourceType.M3u || source.url.isHttpUrl()
                        save(profileId, sources, deletePlaylistOf = source.id.takeIf { staleFile })
                    }
                    publish(sources) { state ->
                        val errors = state.sourceErrors - source.id
                        state.copy(
                            sourceErrors = if (notice != null) errors + (source.id to notice) else errors,
                            addedCount = if (adding) state.addedCount + 1 else state.addedCount,
                            error = if (adding) notice else state.error,
                        )
                    }
                }
                outcome.onFailure { error ->
                    val reason = (error as? LiveTvException)?.error ?: fallbackError(source.type)
                    if (adding) {
                        if (!known && source.type == LiveTvSourceType.M3u && !source.url.isHttpUrl()) {
                            scope.launch(writer) { runCatching { store.deletePlaylist(profileId, source.id) } }
                        }
                        _state.update { it.copy(error = reason, isLoaded = it.isLoaded || it.sources.isNotEmpty()) }
                    } else {
                        // The source keeps the channels it had.
                        _state.update { it.copy(sourceErrors = it.sourceErrors + (source.id to reason), isLoaded = true) }
                    }
                }
            }
        }
        sourceJobs[source.id] = job
        job.invokeOnCompletion {
            sourceJobs.remove(source.id, job)
            updateLoading()
        }
        _state.update { it.copy(isLoading = true) }
        job.start()
    }

    private fun save(profileId: Int, sources: List<LiveTvSource>, deletePlaylistOf: String? = null) {
        scope.launch(writer) {
            try {
                store.saveSources(profileId, sources)
                deletePlaylistOf?.let { store.deletePlaylist(profileId, it) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                LiveTvLog.warn("Live TV sources not saved", null, error)
            }
        }
    }

    /** One source's channels (tagged with the source) and guide links, plus a notice for a partial load. */
    private suspend fun loadSource(profileId: Int, source: LiveTvSource): Pair<LoadedSource, LiveTvError?> {
        var notice: LiveTvError? = null
        val (channels, epgUrls) = when (source.type) {
            LiveTvSourceType.M3u -> {
                val imported = store.readPlaylist(profileId, source.id) { parseM3uPlaylist(it) }
                val playlist = when {
                    imported != null -> imported
                    source.url.isHttpUrl() -> fetchM3u(source.url)
                    else -> throw LiveTvException(
                        if (source.url.startsWith("http", ignoreCase = true)) LiveTvError.InvalidUrl else LiveTvError.FileEmpty,
                    )
                }
                if (playlist.channels.isEmpty()) {
                    throw LiveTvException(if (imported != null) LiveTvError.FileNoChannels else LiveTvError.NoChannels)
                }
                playlist.channels to playlist.epgUrls
            }
            LiveTvSourceType.Xtream -> {
                val channels = xtream.channels(source.xtream)
                if (channels.isEmpty()) throw LiveTvException(LiveTvError.XtreamNoChannels)
                channels to listOf(LiveTvXtream.guideUrl(source.xtream))
            }
            LiveTvSourceType.Stalker -> {
                val (channels, incomplete) = stalker.channels(source.stalker)
                if (channels.isEmpty()) throw LiveTvException(LiveTvError.StalkerNoChannels)
                if (incomplete) notice = LiveTvError.StalkerIncomplete
                channels to emptyList()
            }
        }
        return LoadedSource(tagChannels(source.id, channels), epgUrls) to notice
    }

    private suspend fun fetchM3u(url: String): ParsedM3uPlaylist {
        if (url.looksLikeDirectVideoUrl()) return ParsedM3uPlaylist(listOf(directStreamChannel(url)), emptyList())
        val parsed = http.stream(url, LIVE_TV_PLAYLIST_HEADERS) { source ->
            parseM3uPlaylist(generateSequence { source.readUtf8Line() })
        }
        return if (parsed.isHlsStream) ParsedM3uPlaylist(listOf(directStreamChannel(url)), emptyList()) else parsed
    }

    /**
     * Shows the channels of [sources] as one list, applying [change] in the same update, so no
     * reader sees the sources and their channels disagree. Runs on [serial].
     */
    private fun publish(sources: List<LiveTvSource>, change: (LiveTvState) -> LiveTvState) {
        val parts = sources.mapNotNull { loaded[it.id] }
        val channels = ArrayList<LiveTvChannel>(parts.sumOf { it.channels.size })
        parts.forEach { channels.addAll(it.channels) }
        val sourceCounts = HashMap<String, Int>()
        val groupCounts = HashMap<String, Int>()
        channels.forEach {
            sourceCounts[it.sourceId] = (sourceCounts[it.sourceId] ?: 0) + 1
            // Channels without a category count under "Uncategorised", so they can be hidden too.
            groupCounts[it.group] = (groupCounts[it.group] ?: 0) + 1
        }
        val epgUrls = if (guideFiles == null) emptyList() else parts.flatMap { it.epgUrls }.distinct()
        val guideKeys = channels.mapTo(HashSet(channels.size * 2)) { it.guideKey }
        val hasGuide = epgUrls.isNotEmpty() && guideKeys.isNotEmpty()
        val guideChanged = epgKey != (epgUrls to guideKeys) || epgJob?.isActive != true
        _state.update { current ->
            val next = change(current)
            val library = next.library
            next.copy(
                // What is on now stays shown until the guide is read again.
                currentProgrammes = if (hasGuide) next.currentProgrammes else emptyMap(),
                guideLogos = if (hasGuide) next.guideLogos else emptyMap(),
                isEpgLoading = hasGuide && (guideChanged || next.isEpgLoading),
                sources = sources,
                channels = channels,
                groups = LiveTvOrganisation.orderedGroups(groupCounts.keys, library.groupOrder, library.groupNames),
                groupCounts = groupCounts,
                shownChannels = LiveTvOrganisation.shownChannels(channels, library.hiddenGroups, library.hiddenChannels),
                sourceCounts = sourceCounts,
                epgUrls = parts.flatMap { part -> part.epgUrls }.distinct(),
                isLoaded = true,
            )
        }
        if (!hasGuide) {
            stopEpg()
            epgKey = null
            pendingEpg = null
        } else if (guideChanged) {
            epgKey = epgUrls to guideKeys
            // Each source that finishes changes the channels: read the guide once, after the last.
            if (sourceJobs.values.any { it.isActive }) {
                pendingEpg = Triple(epgUrls, channels, guideKeys)
            } else {
                pendingEpg = null
                startEpg(epgUrls, channels, guideKeys)
            }
        }
    }

    // region Guide (G10c; Reshaped LiveTvRepository @ 0ccf049)

    /** The next programme after the one on now for [guideKey], from the kept guide; a map lookup. */
    fun nextProgramme(guideKey: String, nowEpochMs: Long = LiveTvClock.nowEpochMs()): LiveTvProgramme? =
        keptSchedule[guideKey]?.firstOrNull { it.startEpochMs > nowEpochMs }

    /** Every kept programme of [guideKey], earliest first: the last few hours and the next ones. */
    fun schedule(guideKey: String): List<LiveTvProgramme> = keptSchedule[guideKey].orEmpty()

    private fun stopEpg() {
        keptSchedule = emptyMap()
        epgGeneration++
        epgJob?.cancel()
        epgJob = null
    }

    /**
     * Reads the guide and moves each channel's "now playing" on every minute. Only runs while
     * something shows Live TV and catches up when it is opened again. The guide is saved compressed
     * in the cache, downloaded again every 10 hours (or on Refresh), and re-read from there whenever
     * channels run out of kept programmes. A guide that fails is tried again sooner, without holding
     * back the ones that loaded. Runs on [serial].
     */
    private fun startEpg(epgUrls: List<String>, channels: List<LiveTvChannel>, guideKeys: Set<String>) {
        val files = guideFiles ?: return
        val window = LiveTvGuideWindow.forDevice(constrained())
        // What is shown stays until the new read replaces it; constrained TVs let go of the old
        // programmes first, so two guides are never held at once.
        if (window === LiveTvGuideWindow.LowMemory) keptSchedule = emptyMap()
        epgGeneration++
        epgJob?.cancel()
        val generation = epgGeneration
        val dir = files.dir
        val guidePaths = epgUrls.map { File(dir, "guide_${Integer.toHexString(it.hashCode())}.xml.gz") }
        val cacheFile = File(dir, LiveTvGuideCache.FILE_NAME)
        val cacheKey = LiveTvGuideCache.key(epgUrls, guideKeys, window)
        epgJob = scope.launch {
            withContext(Dispatchers.IO) {
                // Guides of an earlier source.
                dir.listFiles()?.filter { it !in guidePaths && it != cacheFile }?.forEach(File::delete)
            }
            val request = LiveTvGuideRequest.from(channels)
            var schedule: LiveTvSchedule = keptSchedule
            var nextReadAtMs = 0L
            var firstRead = true
            val publishGuide = { kept: LiveTvSchedule, logos: Map<String, String>?, nowMs: Long ->
                if (epgGeneration == generation) keptSchedule = kept
                val current = currentProgrammes(kept, guideKeys, nowMs)
                _state.update { state ->
                    if (epgGeneration != generation) {
                        state
                    } else {
                        state.copy(
                            guideLogos = if (logos == null || state.guideLogos == logos) state.guideLogos else logos,
                            guideVersion = state.guideVersion + 1,
                            currentProgrammes = current,
                        )
                    }
                }
            }
            while (isActive) {
                _state.subscriptionCount.first { it > 0 }
                val nowMs = LiveTvClock.nowEpochMs()
                if (nowMs >= nextReadAtMs) {
                    val force = forceGuideDownload
                    // Opening again: the programmes kept by the last full read, while still good.
                    val saved = if (firstRead && !force) {
                        withContext(Dispatchers.IO) { LiveTvGuideCache.read(cacheFile, cacheKey, guidePaths, nowMs, EPG_DOWNLOAD_MS) }
                    } else {
                        null
                    }
                    firstRead = false
                    if (saved != null) {
                        schedule = saved.schedule
                        nextReadAtMs = saved.nextReadAtMs
                        publishGuide(schedule, saved.logos, nowMs)
                    } else {
                        val previous = schedule
                        val loadedGuides = HashMap<String, List<LiveTvProgramme>>()
                        val logos = HashMap<String, String>()
                        val truncated = HashSet<String>()
                        var failed = false
                        epgUrls.forEachIndexed { index, epgUrl ->
                            val guide = readGuide(files, epgUrl, guidePaths[index], request, nowMs, window, force)
                            if (guide == null) {
                                failed = true
                                return@forEachIndexed
                            }
                            guide.schedule.forEach { (key, list) ->
                                if (loadedGuides.putIfAbsent(key, list) == null && key in guide.truncated) truncated += key
                            }
                            guide.logos.forEach(logos::putIfAbsent)
                            // Each guide shows as soon as it is read; the slowest one holds back nothing.
                            if (index < epgUrls.lastIndex) {
                                publishGuide(HashMap(previous).apply { putAll(loadedGuides) }, HashMap(logos), nowMs)
                            }
                        }
                        if (force && epgGeneration == generation) forceGuideDownload = false
                        // A guide that failed keeps what it showed before.
                        schedule = if (failed && previous.isNotEmpty()) HashMap(previous).apply { putAll(loadedGuides) } else loadedGuides
                        val regular = if (loadedGuides.isEmpty()) {
                            nowMs + EPG_RETRY_MS
                        } else {
                            nextScheduleReadAt(loadedGuides, truncated, nowMs, EPG_MIN_READ_GAP_MS, EPG_DOWNLOAD_MS)
                        }
                        nextReadAtMs = if (failed) minOf(regular, nowMs + EPG_RETRY_MS) else regular
                        publishGuide(schedule, logos, nowMs)
                        if (!failed && loadedGuides.isNotEmpty() && epgGeneration == generation) {
                            val entry = LiveTvGuideCache.Entry(loadedGuides, logos, nextReadAtMs)
                            withContext(Dispatchers.IO) { LiveTvGuideCache.write(cacheFile, cacheKey, guidePaths, entry) }
                        }
                    }
                }
                val current = currentProgrammes(schedule, guideKeys, nowMs)
                _state.update { state ->
                    when {
                        epgGeneration != generation -> state
                        state.currentProgrammes != current || state.isEpgLoading ->
                            state.copy(currentProgrammes = current, isEpgLoading = false)
                        else -> state
                    }
                }
                if (epgGeneration != generation) return@launch
                delay(EPG_TICK_MS)
            }
        }
    }

    /**
     * One guide, downloaded when its saved copy is missing, old or [force]d; an old copy still
     * serves when the download fails. Null when there is no guide to read.
     */
    private suspend fun readGuide(
        files: LiveTvGuideFiles,
        url: String,
        file: File,
        request: LiveTvGuideRequest,
        nowMs: Long,
        window: LiveTvGuideWindow,
        force: Boolean,
    ): LiveTvGuide? {
        try {
            val saved = withContext(Dispatchers.IO) { file.lastModified() }
            if (force || saved == 0L || nowMs - saved !in 0 until EPG_DOWNLOAD_MS) {
                try {
                    files.download(url, LIVE_TV_STREAM_HEADERS, file)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    LiveTvLog.warn("Guide download failed", url, error)
                    if (saved == 0L) return null
                }
            }
            return files.read(file, request, nowMs, window)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            LiveTvLog.warn("Guide unreadable", url, error)
            return null
        }
    }

    // endregion

    // endregion

    internal companion object {
        /** Sources loaded at once: each holds a connection and a parse buffer. */
        const val PARALLEL_SOURCES = 2

        const val EPG_TICK_MS = 60_000L
        /** How long a downloaded guide is used before it is downloaded again. */
        const val EPG_DOWNLOAD_MS = 10L * 60 * 60 * 1000
        /** The saved guide is read again when channels run out of kept programmes, at most this often. */
        const val EPG_MIN_READ_GAP_MS = 60L * 60 * 1000
        /** A guide that could not be read is tried again sooner. */
        const val EPG_RETRY_MS = 30L * 60 * 1000

        /** How long Live TV may go unseen before its channels are let go (Reshaped: 5 min). */
        const val IDLE_RELEASE_MS = 5L * 60 * 1000

        fun validate(source: LiveTvSource): LiveTvError? = when (source.type) {
            LiveTvSourceType.M3u -> if (source.url.isBlank()) LiveTvError.InvalidUrl else null
            LiveTvSourceType.Xtream -> when {
                !source.xtream.isConfigured -> LiveTvError.XtreamRequired
                !source.xtream.serverUrl.isHttpUrl() -> LiveTvError.XtreamInvalidUrl
                else -> null
            }
            LiveTvSourceType.Stalker -> when {
                !source.stalker.isConfigured -> LiveTvError.StalkerRequired
                !source.stalker.portalUrl.isHttpUrl() -> LiveTvError.StalkerInvalidUrl
                else -> null
            }
        }

        fun fallbackError(type: LiveTvSourceType): LiveTvError = when (type) {
            LiveTvSourceType.M3u -> LiveTvError.LoadFailed
            LiveTvSourceType.Xtream -> LiveTvError.XtreamFailed
            LiveTvSourceType.Stalker -> LiveTvError.StalkerFailed
        }

        /** Ids only need to be unique within a source; the list keys on them across all of them. */
        fun tagChannels(sourceId: String, channels: List<LiveTvChannel>): List<LiveTvChannel> {
            val tagged = ArrayList<LiveTvChannel>(channels.size)
            channels.forEach { channel ->
                val group = channel.group.trim()
                tagged += channel.copy(
                    id = "$sourceId/${channel.id}",
                    sourceId = sourceId,
                    group = group,
                    key = liveTvChannelKey(sourceId, group, channel.name),
                    guideKey = liveTvGuideKey(channel.tvgId, channel.name),
                )
            }
            return tagged
        }
    }
}
