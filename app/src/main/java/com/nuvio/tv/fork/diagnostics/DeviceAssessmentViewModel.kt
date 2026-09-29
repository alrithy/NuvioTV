package com.nuvio.tv.fork.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.ui.screens.player.NuvioExoPlayerPerformanceHelper
import com.nuvio.tv.ui.screens.settings.MemoryBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceAssessmentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applier: DeviceAssessmentApplier,
    registry: FeatureRegistry,
) : ViewModel() {

    val enabled: Boolean = registry.mode(FeatureId.UNIFIED_DIAGNOSTICS) != FeatureMode.OFF

    val canRevert: StateFlow<Boolean> =
        applier.canRevert.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Settings written by the last apply, or -1 after a revert; null before any action. */
    private val _lastAction = MutableStateFlow<Int?>(null)
    val lastAction: StateFlow<Int?> = _lastAction.asStateFlow()

    @androidx.annotation.OptIn(UnstableApi::class)
    fun assess(
        settings: PlayerSettings,
        displaySupportsFrameRateSwitching: Boolean?,
        singleConnectionMbps: Double?,
        parallelMbps: Double?,
    ): AssessmentResult {
        val safeLimitMb = NuvioExoPlayerPerformanceHelper.getSafeNativeMemoryLimitMb(context)
        val chunkMb = (settings.parallelChunkSizeKb + 1023) / 1024
        fun maxSafe(useParallel: Boolean, connections: Int): Int {
            val overhead = if (useParallel) MemoryBudget.parallelOverheadMb(connections, chunkMb) else 0
            return ((safeLimitMb - overhead) / MemoryBudget.BUFFER_STEP_MB * MemoryBudget.BUFFER_STEP_MB)
                .coerceAtLeast(MemoryBudget.MIN_BUFFER_MB)
        }
        fun inputs(maxSafeMb: Int) = AssessmentInputs(
            useParallelConnections = settings.useParallelConnections,
            parallelConnectionCount = settings.parallelConnectionCount,
            targetBufferSizeMb = settings.bufferSettings.targetBufferSizeMb,
            frameRateMatchingOn = settings.frameRateMatchingMode != FrameRateMatchingMode.OFF,
            maxSafeTargetBufferMb = maxSafeMb,
            displaySupportsFrameRateSwitching = displaySupportsFrameRateSwitching,
            singleConnectionMbps = singleConnectionMbps,
            parallelMbps = parallelMbps,
            testedConnections = TESTED_CONNECTIONS,
        )
        // Size the buffer cap for the parallel setup the plan would leave in place.
        val first = DeviceAssessment.assess(inputs(maxSafe(settings.useParallelConnections, settings.parallelConnectionCount)))
        val useParallel = first.plan.useParallelConnections ?: settings.useParallelConnections
        val connections = first.plan.parallelConnectionCount ?: settings.parallelConnectionCount
        return DeviceAssessment.assess(inputs(maxSafe(useParallel, connections)))
    }

    fun apply(plan: AssessmentPlan) {
        viewModelScope.launch { _lastAction.value = applier.apply(plan) }
    }

    fun revert() {
        viewModelScope.launch { if (applier.revert()) _lastAction.value = -1 }
    }

    companion object {
        /** The official stream test measures 4 connections, the largest count settable in every mode. */
        const val TESTED_CONNECTIONS = 4
    }
}
