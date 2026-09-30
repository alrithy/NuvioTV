package com.nuvio.tv.fork.livetv

import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val http: LiveTvFetcher,
    private val activeProfileId: StateFlow<Int>,
    val featureEnabled: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @Inject constructor(
        store: LiveTvStorage,
        profileManager: ProfileManager,
        registry: FeatureRegistry,
    ) : this(store, LiveTvHttp(), profileManager.activeProfileId, registry.mode(FeatureId.LIVE_TV) != FeatureMode.OFF)

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
            val sources = try {
                store.sources(profileId)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                LiveTvLog.warn("Live TV sources unreadable", null, error)
                emptyList()
            }
            if (loadedProfileId != profileId) return@launch
            loaded.clear()
            _state.value = LiveTvState(sources = sources, isLoading = sources.isNotEmpty(), isLoaded = sources.isEmpty())
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

    /** Runs on [serial]. */
    private fun releaseAll() {
        profileJob?.cancel()
        profileJob = null
        cancelAllLoads()
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
        channels.forEach { sourceCounts[it.sourceId] = (sourceCounts[it.sourceId] ?: 0) + 1 }
        _state.update {
            change(it).copy(
                sources = sources,
                channels = channels,
                sourceCounts = sourceCounts,
                epgUrls = parts.flatMap { part -> part.epgUrls }.distinct(),
                isLoaded = true,
            )
        }
    }

    // endregion

    internal companion object {
        /** Sources loaded at once: each holds a connection and a parse buffer. */
        const val PARALLEL_SOURCES = 2

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
                )
            }
            return tagged
        }
    }
}
