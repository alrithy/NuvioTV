package com.nuvio.tv.fork.foundation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** G13a (D063): experimental groups turn on only through the opt-in, and nothing else can. */
class ExperimentalOptInTest {
    @After
    fun reset() = ExperimentalOptIn.resetForTest()

    @Test
    fun nothingIsOptedInByDefault() {
        assertTrue(ExperimentalOptIn.enabled.isEmpty())
        assertEquals(FeatureMode.OFF, FeatureRegistry().mode(FeatureId.AI_MEDIA))
        assertEquals(FeatureMode.OFF, FeatureRegistry().mode(FeatureId.MAT_AUDIO))
    }

    @Test
    fun anOptedInExperimentalGroupIsOnAndTheOtherStaysOff() {
        ExperimentalOptIn.install(setOf("MAT_AUDIO"))
        assertEquals(FeatureMode.ON, FeatureRegistry().mode(FeatureId.MAT_AUDIO))
        assertEquals(FeatureMode.OFF, FeatureRegistry().mode(FeatureId.AI_MEDIA))
    }

    @Test
    fun storedValuesCanNeverSwitchAStableGroupOrInventOne() {
        ExperimentalOptIn.install(setOf("UI_STYLES", "WATCH_PARTY", "SOMETHING_ELSE", "ai_media"))
        assertTrue(ExperimentalOptIn.enabled.isEmpty())
        FeatureId.entries.forEach { assertEquals("$it", FeatureRegistry.DEFAULTS.getValue(it), FeatureRegistry().mode(it)) }
    }

    @Test
    fun stableDefaultsAreUntouchedByAnOptIn() {
        ExperimentalOptIn.install(setOf("AI_MEDIA", "MAT_AUDIO"))
        FeatureId.entries.filterNot { it.experimental }.forEach {
            assertEquals("$it", FeatureRegistry.DEFAULTS.getValue(it), FeatureRegistry().mode(it))
        }
        assertEquals(setOf(FeatureId.AI_MEDIA, FeatureId.MAT_AUDIO), ExperimentalOptIn.enabled)
    }
}
