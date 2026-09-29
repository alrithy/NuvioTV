package com.nuvio.tv.fork.diagnostics

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.TestPreferencesStore
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.BufferSettings
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAssessmentApplierTest {

    private val before = PlayerSettings(
        useParallelConnections = false,
        parallelConnectionCount = 2,
        bufferSettings = BufferSettings(targetBufferSizeMb = 500),
        frameRateMatchingMode = FrameRateMatchingMode.OFF,
    )

    private val activeProfile = MutableStateFlow(1)
    private val stores = mutableMapOf<Pair<Int, String>, TestPreferencesStore>()
    private val factory = mockk<ProfileDataStoreFactory>().also { factory ->
        every { factory.get(any(), any()) } answers { stores.getOrPut(firstArg<Int>() to secondArg<String>()) { TestPreferencesStore() } }
    }
    private val profileManager = mockk<ProfileManager>().also { every { it.activeProfileId } returns activeProfile }
    private val settings = mockk<PlayerSettingsDataStore>(relaxed = true).also {
        every { it.playerSettings } returns MutableStateFlow(before)
    }
    private val applier = DeviceAssessmentApplier(settings, factory, profileManager)

    private val plan = AssessmentPlan(
        useParallelConnections = true,
        parallelConnectionCount = 4,
        targetBufferSizeMb = 300,
        frameRateMatchingOn = true,
    )

    @Test
    fun emptyPlanWritesNothingAndLeavesNoSnapshot() = runTest {
        assertEquals(0, applier.apply(AssessmentPlan()))
        assertFalse(applier.canRevert.first())
        coVerify(exactly = 0) { settings.setUseParallelConnections(any()) }
    }

    @Test
    fun applyWritesOnlyPlannedSettingsThroughOfficialSetters() = runTest {
        assertEquals(4, applier.apply(plan))
        coVerifyOrder {
            settings.setUseParallelConnections(true)
            settings.setParallelConnectionCount(4)
            settings.setBufferTargetSizeMb(300)
            settings.setFrameRateMatchingMode(FrameRateMatchingMode.START)
        }
        assertTrue(applier.canRevert.first())
    }

    @Test
    fun revertRestoresTheExactPreviousValuesAndClearsTheSnapshot() = runTest {
        applier.apply(plan)
        assertTrue(applier.revert())
        coVerify {
            settings.setUseParallelConnections(false)
            settings.setParallelConnectionCount(2)
            settings.setBufferTargetSizeMb(500)
            settings.setFrameRateMatchingMode(FrameRateMatchingMode.OFF)
        }
        assertFalse(applier.canRevert.first())
        assertFalse(applier.revert())
    }

    @Test
    fun freshInstallHasNothingToRevert() = runTest {
        assertFalse(applier.canRevert.first())
        assertFalse(applier.revert())
        coVerify(exactly = 0) { settings.setFrameRateMatchingMode(any()) }
    }

    @Test
    fun corruptSnapshotIsDiscardedWithoutWritingSettings() = runTest {
        stores.getOrPut(1 to "fork_device_assessment") { TestPreferencesStore() }
            .edit { it[stringPreferencesKey("snapshot_frame_rate_matching_mode")] = "NOT_A_MODE" }
        assertTrue(applier.canRevert.first())
        assertFalse(applier.revert())
        assertFalse(applier.canRevert.first())
        coVerify(exactly = 0) { settings.setFrameRateMatchingMode(any()) }
        coVerify(exactly = 0) { settings.setBufferTargetSizeMb(any()) }
    }

    @Test
    fun snapshotIsScopedToTheActiveProfile() = runTest {
        applier.apply(plan)
        activeProfile.value = 2
        assertFalse(applier.canRevert.first())
        assertFalse(applier.revert())
        activeProfile.value = 1
        assertTrue(applier.canRevert.first())
    }
}
