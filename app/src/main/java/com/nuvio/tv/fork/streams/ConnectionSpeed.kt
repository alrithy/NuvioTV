package com.nuvio.tv.fork.streams

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.concurrent.atomic.AtomicLong

/**
 * Learns sustained download throughput from real playback, per [NetworkKind] (G8c; FILE_PORT of
 * Reshaped `ConnectionSpeedEstimator` @ 0ccf049). Samples come passively from
 * [PlaybackThroughput]; reading the estimate is a memory lookup. Device-level (the connection is
 * the TV's, not a profile's) in `fork_connection_speed`; only network kind, Mbps and time are kept.
 */
object ConnectionSpeedEstimator {
    private const val PREFS_NAME = "fork_connection_speed"
    private const val KEY_SAMPLES = "throughput_samples"

    private val lock = Any()
    @Volatile private var preferences: SharedPreferences? = null
    @Volatile private var cachedSamples: List<ConnectionSpeedSample>? = null

    fun ensureLoaded(context: Context) {
        if (preferences != null) return
        synchronized(lock) {
            if (preferences != null) return
            val appContext = context.applicationContext
            appContext.getSystemService(ConnectivityManager::class.java)?.let(DefaultNetworkObserver::start)
            preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /** The estimate for the current network, or null while still learning or offline. */
    fun estimateMbps(context: Context): Double? {
        ensureLoaded(context)
        val network = DefaultNetworkObserver.kind ?: return null
        return ConnectionFitRules.estimateMbps(loadedSamples(), network, System.currentTimeMillis())
    }

    /** Records a sample for the network it was measured on (not necessarily the current one). */
    fun record(context: Context, network: NetworkKind, mbps: Double) {
        if (!ConnectionFitRules.isValidThroughput(mbps)) return
        ensureLoaded(context)
        synchronized(lock) {
            val updated = ConnectionFitRules.appendSample(
                loadedSamples(),
                ConnectionSpeedSample(network, mbps, System.currentTimeMillis()),
            )
            cachedSamples = updated
            preferences?.edit()?.putString(KEY_SAMPLES, ConnectionFitRules.encode(updated))?.apply()
        }
    }

    private fun loadedSamples(): List<ConnectionSpeedSample> =
        cachedSamples ?: synchronized(lock) {
            cachedSamples ?: runCatching { ConnectionFitRules.decode(preferences?.getString(KEY_SAMPLES, null)) }
                .getOrDefault(emptyList())
                .also { cachedSamples = it }
        }
}

/**
 * Follows the default network through one system callback, so reading its kind on the stream
 * load and playback paths is a field read. [generation] changes whenever the default network
 * changes; a measurement that spans a change describes neither network and is dropped.
 */
object DefaultNetworkObserver : ConnectivityManager.NetworkCallback() {
    @Volatile var kind: NetworkKind? = null
        private set

    @Volatile var generation = 0
        private set

    private var network: Network? = null
    private var isStarted = false

    @Synchronized
    fun start(manager: ConnectivityManager) {
        if (isStarted) return
        isStarted = true
        runCatching { manager.activeNetwork?.let { active -> update(active, manager.getNetworkCapabilities(active)) } }
        runCatching { manager.registerDefaultNetworkCallback(this) }
    }

    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        update(network, capabilities)
    }

    /** A switch can report the new default before the old one is lost, so only the current one clears. */
    @Synchronized
    override fun onLost(network: Network) {
        if (network == this.network) update(null, null)
    }

    @Synchronized
    private fun update(network: Network?, capabilities: NetworkCapabilities?) {
        val newKind = capabilities?.let {
            when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.WIFI
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.CELLULAR
                else -> NetworkKind.OTHER
            }
        }
        if (network == this.network && newKind == kind) return
        this.network = network
        kind = newKind
        generation++
    }
}

/**
 * Player side of connection learning (FILE_PORT of Reshaped `PlaybackThroughput`). The player
 * wraps its network data source factories with [countingNetworkBytes] and calls [onExoTick] /
 * [onMpvTick] from its existing progress loop; one sampler per stream URL reports once. Nothing
 * runs while STREAM_INTELLIGENCE is OFF.
 */
object PlaybackThroughput {
    private val networkBytes = AtomicLong()
    private var sampler: PlaybackThroughputSampler? = null
    private var samplerUrl: String? = null

    @OptIn(UnstableApi::class)
    private val networkByteCounter: TransferListener = object : TransferListener {
        override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
        override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
        override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
            if (isNetwork) networkBytes.addAndGet(bytesTransferred.toLong())
        }
        override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    }

    /** Counts bytes received over the network by every data source [factory] creates. */
    @OptIn(UnstableApi::class)
    fun countingNetworkBytes(factory: DataSource.Factory): DataSource.Factory {
        if (!StreamIntelligence.enabled) return factory
        return DataSource.Factory { factory.createDataSource().apply { addTransferListener(networkByteCounter) } }
    }

    /** ExoPlayer tick: bytes since the last tick, gated on whether the player is loading. */
    @Synchronized
    fun onExoTick(context: Context, streamUrl: String?, isLoading: Boolean) {
        // G10d: a Live TV channel arrives at real time and would teach a false low speed.
        if (!StreamIntelligence.enabled || com.nuvio.tv.fork.livetv.LiveTvPlaybackRegistry.isLiveTv(streamUrl)) return
        samplerFor(context, streamUrl ?: return).onBytesTick(networkBytes.getAndSet(0L), isLoading)
    }

    /** mpv tick: its own download rate (`cache-speed`), gated on the demuxer still reading. */
    @Synchronized
    fun onMpvTick(context: Context, streamUrl: String?, bytesPerSecond: Long, isFetching: Boolean) {
        if (!StreamIntelligence.enabled || com.nuvio.tv.fork.livetv.LiveTvPlaybackRegistry.isLiveTv(streamUrl)) return
        samplerFor(context, streamUrl ?: return).onRateTick(bytesPerSecond, isFetching)
    }

    @Synchronized
    fun finish() {
        sampler?.finish()
        sampler = null
        samplerUrl = null
    }

    private fun samplerFor(context: Context, streamUrl: String): PlaybackThroughputSampler {
        sampler?.takeIf { samplerUrl == streamUrl }?.let { return it }
        finish()
        networkBytes.set(0L)
        val appContext = context.applicationContext
        ConnectionSpeedEstimator.ensureLoaded(appContext) // starts network tracking before the first tick
        return PlaybackThroughputSampler(
            sourceUrl = streamUrl,
            networkKind = { DefaultNetworkObserver.kind },
            networkGeneration = { DefaultNetworkObserver.generation },
        ) { network, mbps -> ConnectionSpeedEstimator.record(appContext, network, mbps) }.also {
            sampler = it
            samplerUrl = streamUrl
        }
    }
}
