package com.nuvio.tv.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G5e: the implausible-rate floor applies only with the fork group on and only under 20 fps. */
class FrameRateUtilsForkTest {

    @Test
    fun refusesOnlyImplausibleRatesWithTheForkOn() {
        assertTrue(FrameRateUtils.shouldRefuseImplausibleRate(1f, forkEnabled = true))
        assertTrue(FrameRateUtils.shouldRefuseImplausibleRate(19.9f, forkEnabled = true))
        assertFalse(FrameRateUtils.shouldRefuseImplausibleRate(23.976f, forkEnabled = true))
        assertFalse(FrameRateUtils.shouldRefuseImplausibleRate(20f, forkEnabled = true))
        assertFalse(FrameRateUtils.shouldRefuseImplausibleRate(0f, forkEnabled = true))
    }

    @Test
    fun officialBehaviorWithTheForkOff() {
        assertFalse(FrameRateUtils.shouldRefuseImplausibleRate(1f, forkEnabled = false))
    }

    @Test
    fun forkDefaultIsOn() {
        // AUDIO_DV_AFR is AUTO (D048).
        assertTrue(FrameRateUtils.shouldRefuseImplausibleRate(1f))
    }
}
