package com.nuvio.tv.fork.dimmer

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDimmerRulesTest {
    @Test
    fun levelsAreClampedToCxsmosRange() {
        assertEquals(0, AppDimmerRules.clamp(-5))
        assertEquals(40, AppDimmerRules.clamp(40))
        assertEquals(90, AppDimmerRules.clamp(120))
        assertEquals(0f, AppDimmerRules.alpha(0), 0f)
        assertEquals(0.9f, AppDimmerRules.alpha(95), 1e-6f)
    }

    @Test
    fun presetsStepByTenFromOff() {
        assertEquals(listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90), AppDimmerRules.PRESETS)
    }
}
