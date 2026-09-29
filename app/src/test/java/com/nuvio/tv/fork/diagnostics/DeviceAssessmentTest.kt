package com.nuvio.tv.fork.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAssessmentTest {

    private val base = AssessmentInputs(
        useParallelConnections = false,
        parallelConnectionCount = 2,
        targetBufferSizeMb = 0,
        frameRateMatchingOn = false,
        maxSafeTargetBufferMb = 300,
        displaySupportsFrameRateSwitching = null,
        singleConnectionMbps = null,
        parallelMbps = null,
        testedConnections = 4,
    )

    private fun item(result: AssessmentResult, key: AssessmentKey) = result.items.firstOrNull { it.key == key }

    @Test
    fun parallelRowNeedsBothMeasurements() {
        assertNull(item(DeviceAssessment.assess(base), AssessmentKey.PARALLEL_CONNECTIONS))
        assertNull(item(DeviceAssessment.assess(base.copy(singleConnectionMbps = 50.0)), AssessmentKey.PARALLEL_CONNECTIONS))
    }

    @Test
    fun parallelIsRecommendedWhenItClearlyBeatsOneConnection() {
        val result = DeviceAssessment.assess(base.copy(singleConnectionMbps = 40.0, parallelMbps = 120.0))
        assertEquals(AssessmentItem(AssessmentKey.PARALLEL_CONNECTIONS, AssessmentTier.MEASURED, true), item(result, AssessmentKey.PARALLEL_CONNECTIONS))
        assertEquals(true, result.plan.useParallelConnections)
        assertEquals(4, result.plan.parallelConnectionCount)
    }

    @Test
    fun parallelIsTurnedOffWhenItDoesNotHelp() {
        val result = DeviceAssessment.assess(
            base.copy(useParallelConnections = true, parallelConnectionCount = 4, singleConnectionMbps = 100.0, parallelMbps = 110.0)
        )
        assertTrue(item(result, AssessmentKey.PARALLEL_CONNECTIONS)!!.changeNeeded)
        assertEquals(false, result.plan.useParallelConnections)
        assertNull(result.plan.parallelConnectionCount)
    }

    @Test
    fun matchingParallelSetupNeedsNoChange() {
        val result = DeviceAssessment.assess(
            base.copy(useParallelConnections = true, parallelConnectionCount = 4, singleConnectionMbps = 40.0, parallelMbps = 120.0)
        )
        assertFalse(item(result, AssessmentKey.PARALLEL_CONNECTIONS)!!.changeNeeded)
        assertTrue(result.plan.isEmpty)
    }

    @Test
    fun targetBufferIsCappedToTheSafeLimitButDefaultIsLeftAlone() {
        val over = DeviceAssessment.assess(base.copy(targetBufferSizeMb = 500))
        assertTrue(item(over, AssessmentKey.TARGET_BUFFER)!!.changeNeeded)
        assertEquals(300, over.plan.targetBufferSizeMb)

        val fits = DeviceAssessment.assess(base.copy(targetBufferSizeMb = 250))
        assertFalse(item(fits, AssessmentKey.TARGET_BUFFER)!!.changeNeeded)
        assertNull(fits.plan.targetBufferSizeMb)

        assertNull(DeviceAssessment.assess(base.copy(targetBufferSizeMb = 0)).plan.targetBufferSizeMb)
    }

    @Test
    fun unknownDisplayIsVerifyOnlyAndNeverApplied() {
        val result = DeviceAssessment.assess(base.copy(displaySupportsFrameRateSwitching = null))
        assertEquals(AssessmentTier.VERIFY, item(result, AssessmentKey.FRAME_RATE_MATCHING)!!.tier)
        assertNull(result.plan.frameRateMatchingOn)
    }

    @Test
    fun frameRateMatchingFollowsDisplayCapability() {
        assertEquals(true, DeviceAssessment.assess(base.copy(displaySupportsFrameRateSwitching = true)).plan.frameRateMatchingOn)
        assertEquals(false, DeviceAssessment.assess(base.copy(displaySupportsFrameRateSwitching = false, frameRateMatchingOn = true)).plan.frameRateMatchingOn)
        assertTrue(DeviceAssessment.assess(base.copy(displaySupportsFrameRateSwitching = true, frameRateMatchingOn = true)).plan.isEmpty)
    }
}
