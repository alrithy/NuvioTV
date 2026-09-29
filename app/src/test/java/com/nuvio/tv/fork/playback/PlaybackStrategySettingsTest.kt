package com.nuvio.tv.fork.playback

import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy
import com.nuvio.tv.fork.resource.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Official parity and knob application on the real PlayerSettings (CI only: Android module). */
class PlaybackStrategySettingsTest {

    private val stored = PlayerSettings()
    private val standard = AdaptiveResourcePolicy(MemoryTier.STANDARD)

    @Test
    fun officialKnobsReturnTheStoredInstance() {
        assertSame(stored, stored.withKnobs(StrategyKnobs.NONE))
    }

    @Test
    fun throughputTurnsOnOfficialParallelAndManagedBuffer() {
        val knobs = PlaybackStrategies.knobs(PlaybackStrategy.REMUX_THROUGHPUT, stored.parallelConnectionCount, 50, standard)
        val session = stored.withKnobs(knobs)
        assertEquals(true, session.parallelNetworkEnabled)
        assertEquals(true, session.useParallelConnections)
        assertEquals(4, session.parallelConnectionCount)
        assertEquals(true, session.bufferEngineEnabled)
        assertEquals(true, session.bufferBudgetManaged)
        // Unrelated settings are untouched.
        assertEquals(stored.vodCacheEnabled, session.vodCacheEnabled)
        assertEquals(stored.nuvioPerformanceModeEnabled, session.nuvioPerformanceModeEnabled)
        assertEquals(stored.internalPlayerEngine, session.internalPlayerEngine)
    }

    @Test
    fun lowMemoryShrinksOnlyTheTargetBuffer() {
        val big = stored.copy(bufferSettings = stored.bufferSettings.copy(targetBufferSizeMb = 400))
        val session = big.withKnobs(PlaybackStrategies.knobs(PlaybackStrategy.LOW_MEMORY, 4, 400, standard))
        assertEquals(50, session.bufferSettings.targetBufferSizeMb)
        assertEquals(big.bufferSettings.maxBufferMs, session.bufferSettings.maxBufferMs)
        assertEquals(false, session.useParallelConnections)
    }

    @Test
    fun seekOptimizedEnablesTheOfficialDiskCache() {
        val session = stored.withKnobs(PlaybackStrategies.knobs(PlaybackStrategy.SEEK_OPTIMIZED, 2, 50, standard))
        assertEquals(true, session.vodCacheEnabled)
        assertEquals(true, session.bufferEngineEnabled)
        assertEquals(stored.vodCacheSizeMb, session.vodCacheSizeMb)
    }
}
