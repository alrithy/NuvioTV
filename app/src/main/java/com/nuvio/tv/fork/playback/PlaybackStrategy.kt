package com.nuvio.tv.fork.playback

import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy

/**
 * Playback strategies (D006, G3). Each one is a set of session-only overrides on official
 * buffer/network settings; none of them replaces the player. [OFFICIAL] changes nothing.
 */
enum class PlaybackStrategy(val key: String) {
    OFFICIAL("official"),
    REMUX_THROUGHPUT("remux"),
    SEEK_OPTIMIZED("seek"),
    LOW_MEMORY("low_memory"),
    AUTO("auto");

    companion object {
        /** Unknown or missing stored values fall back to [OFFICIAL]. */
        fun fromKey(key: String?): PlaybackStrategy = entries.firstOrNull { it.key == key } ?: OFFICIAL
    }
}

/** What is known about a playback when the ExoPlayer session is built. */
data class PlaybackFacts(
    val exoPlayerEngine: Boolean,
    /** http(s) progressive file: not HLS/DASH, not a torrent, not a loopback proxy. */
    val progressiveHttp: Boolean,
    val fileSizeBytes: Long?,
    val filename: String?,
    val lowRamDevice: Boolean,
)

enum class StrategyReason(val key: String) {
    SELECTED("selected"),
    FEATURE_OFF("feature off"),
    NOT_EXOPLAYER("engine"),
    NOT_PROGRESSIVE_HTTP("stream type"),
    AUTO_LOW_RAM("low RAM"),
    AUTO_LARGE_FILE("large file"),
    AUTO_DEFAULT("default"),
}

data class StrategyDecision(
    val selected: PlaybackStrategy,
    val effective: PlaybackStrategy,
    val reason: StrategyReason,
) {
    /** Diagnostics label: the effective strategy, plus the selection and reason when they differ. */
    val hudLabel: String
        get() = if (selected == effective) effective.key else "${selected.key} → ${effective.key} (${reason.key})"
}

/** Overrides on official settings; `null` keeps the user's stored value. */
data class StrategyKnobs(
    val parallelNetwork: Boolean? = null,
    val useParallelConnections: Boolean? = null,
    val parallelConnectionCount: Int? = null,
    val bufferEngine: Boolean? = null,
    val bufferBudgetManaged: Boolean? = null,
    val vodCache: Boolean? = null,
    val targetBufferSizeMb: Int? = null,
) {
    val isOfficial: Boolean get() = this == NONE

    companion object {
        val NONE = StrategyKnobs()
    }
}

object PlaybackStrategies {
    /** Official non-performance-mode maximum; throughput never asks for fewer. */
    const val THROUGHPUT_MIN_CONNECTIONS = 4

    /** Official `MemoryBudget.MIN_TARGET_BUFFER_MB`, the smallest buffer the UI offers. */
    const val LOW_MEMORY_TARGET_BUFFER_MB = 50

    /** Auto treats files at least this large as REMUX-class (1080p REMUX starts around 20 GB). */
    const val LARGE_FILE_BYTES = 20L * 1024 * 1024 * 1024

    private val remuxToken = Regex("(^|[^a-z0-9])remux([^a-z0-9]|$)", RegexOption.IGNORE_CASE)

    fun resolve(selected: PlaybackStrategy, facts: PlaybackFacts, mode: FeatureMode): StrategyDecision {
        fun official(reason: StrategyReason) = StrategyDecision(selected, PlaybackStrategy.OFFICIAL, reason)
        if (mode == FeatureMode.OFF) return official(StrategyReason.FEATURE_OFF)
        if (selected == PlaybackStrategy.OFFICIAL) return StrategyDecision(selected, selected, StrategyReason.SELECTED)
        if (!facts.exoPlayerEngine) return official(StrategyReason.NOT_EXOPLAYER)
        return when (selected) {
            PlaybackStrategy.AUTO -> resolveAuto(facts)
            PlaybackStrategy.LOW_MEMORY -> StrategyDecision(selected, selected, StrategyReason.SELECTED)
            else ->
                if (facts.progressiveHttp) StrategyDecision(selected, selected, StrategyReason.SELECTED)
                else official(StrategyReason.NOT_PROGRESSIVE_HTTP)
        }
    }

    private fun resolveAuto(facts: PlaybackFacts): StrategyDecision {
        val auto = PlaybackStrategy.AUTO
        return when {
            facts.lowRamDevice -> StrategyDecision(auto, PlaybackStrategy.LOW_MEMORY, StrategyReason.AUTO_LOW_RAM)
            facts.progressiveHttp && isLargeFile(facts) ->
                StrategyDecision(auto, PlaybackStrategy.REMUX_THROUGHPUT, StrategyReason.AUTO_LARGE_FILE)
            else -> StrategyDecision(auto, PlaybackStrategy.OFFICIAL, StrategyReason.AUTO_DEFAULT)
        }
    }

    /**
     * True for an http(s) progressive file: the only kind official parallel range reads and the
     * disk cache serve. HLS/DASH (by resolved MIME), torrents and loopback proxies are excluded.
     */
    fun isProgressiveHttp(url: String, mimeType: String?, isTorrent: Boolean, isLoopback: Boolean): Boolean {
        if (isTorrent || isLoopback) return false
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) return false
        val mime = mimeType?.lowercase() ?: return true
        return "mpegurl" !in mime && "m3u8" !in mime && "dash+xml" !in mime
    }

    internal fun isLargeFile(facts: PlaybackFacts): Boolean =
        (facts.fileSizeBytes ?: 0L) >= LARGE_FILE_BYTES || facts.filename?.let(remuxToken::containsMatchIn) == true

    /**
     * Overrides for [effective]. Stored values are inputs so each knob only ever moves toward the
     * strategy's goal; connection counts still pass through the G2 allocation-safety policy.
     */
    fun knobs(
        effective: PlaybackStrategy,
        storedConnections: Int,
        storedEffectiveBufferMb: Int,
        policy: AdaptiveResourcePolicy,
    ): StrategyKnobs = when (effective) {
        PlaybackStrategy.OFFICIAL, PlaybackStrategy.AUTO -> StrategyKnobs.NONE
        PlaybackStrategy.REMUX_THROUGHPUT -> StrategyKnobs(
            parallelNetwork = true,
            useParallelConnections = true,
            parallelConnectionCount = policy.parallelConnections(
                storedConnections.coerceAtLeast(THROUGHPUT_MIN_CONNECTIONS)
            ),
            bufferEngine = true,
            bufferBudgetManaged = true,
        )
        // Sequential reads fill the official disk cache; parallel REMUX is not stacked on top (D006).
        PlaybackStrategy.SEEK_OPTIMIZED -> StrategyKnobs(
            useParallelConnections = false,
            bufferEngine = true,
            bufferBudgetManaged = true,
            vodCache = true,
        )
        PlaybackStrategy.LOW_MEMORY -> StrategyKnobs(
            useParallelConnections = false,
            bufferEngine = true,
            bufferBudgetManaged = false,
            targetBufferSizeMb = storedEffectiveBufferMb.coerceAtMost(LOW_MEMORY_TARGET_BUFFER_MB),
        )
    }
}
