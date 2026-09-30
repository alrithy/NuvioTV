package com.nuvio.tv.fork.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureRegistryTest {

    @Test
    fun featureModesAreExactlyOffOnAuto() {
        assertEquals(listOf("OFF", "ON", "AUTO"), FeatureMode.entries.map { it.name })
    }

    @Test
    fun everyFeatureIdHasADefault() {
        assertEquals(FeatureId.entries.toSet(), FeatureRegistry.DEFAULTS.keys)
    }

    @Test
    fun everyUndecidedDefaultPreservesOfficialBehavior() {
        val registry = FeatureRegistry()
        FeatureId.entries.filter { it !in FeatureRegistry.DECIDED_DEFAULTS }.forEach { id ->
            assertEquals("$id default", FeatureMode.OFF, registry.mode(id))
        }
    }

    @Test
    fun onlyRecordedDecisionsHaveADecidedDefault() {
        assertEquals(
            mapOf(
                FeatureId.UNIFIED_DIAGNOSTICS to FeatureMode.AUTO, // D041
                FeatureId.ADAPTIVE_RESOURCE_MANAGER to FeatureMode.AUTO, // D042
                FeatureId.PLAYBACK_STRATEGY_ENGINE to FeatureMode.AUTO, // D043
                FeatureId.REMUX_PERFORMANCE to FeatureMode.AUTO, // D044
                FeatureId.AUDIO_DV_AFR to FeatureMode.AUTO, // D048
                FeatureId.SUBTITLE_INTELLIGENCE to FeatureMode.AUTO, // D051
                FeatureId.SEEK_INTELLIGENCE to FeatureMode.AUTO, // D052
            ),
            FeatureRegistry.DECIDED_DEFAULTS,
        )
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.UNIFIED_DIAGNOSTICS))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.ADAPTIVE_RESOURCE_MANAGER))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.PLAYBACK_STRATEGY_ENGINE))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.REMUX_PERFORMANCE))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.AUDIO_DV_AFR))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.SUBTITLE_INTELLIGENCE))
        assertEquals(FeatureMode.AUTO, FeatureRegistry().mode(FeatureId.SEEK_INTELLIGENCE))
    }

    @Test
    fun defaultsAreDeterministic() {
        assertEquals(
            FeatureId.entries.map(FeatureRegistry()::mode),
            FeatureId.entries.map(FeatureRegistry()::mode),
        )
    }

    @Test
    fun experimentalAiAndMatAreOff() {
        assertEquals(
            setOf(FeatureId.AI_MEDIA, FeatureId.MAT_AUDIO),
            FeatureId.entries.filter { it.experimental }.toSet(),
        )
        val registry = FeatureRegistry()
        assertEquals(FeatureMode.OFF, registry.mode(FeatureId.AI_MEDIA))
        assertEquals(FeatureMode.OFF, registry.mode(FeatureId.MAT_AUDIO))
        assertTrue(FeatureRegistry.DECIDED_DEFAULTS.keys.none { it.experimental })
    }

    @Test
    fun overridesApplyOnlyToTheirFeature() {
        val registry = FeatureRegistry(mapOf(FeatureId.UNIFIED_DIAGNOSTICS to FeatureMode.OFF))
        assertEquals(FeatureMode.OFF, registry.mode(FeatureId.UNIFIED_DIAGNOSTICS))
        FeatureId.entries.filter { it != FeatureId.UNIFIED_DIAGNOSTICS }.forEach {
            assertEquals(FeatureRegistry.DEFAULTS.getValue(it), registry.mode(it))
        }
    }

    @Test
    fun defaultsCannotBeMutatedGlobally() {
        @Suppress("UNCHECKED_CAST")
        val mutableView = FeatureRegistry.DEFAULTS as MutableMap<FeatureId, FeatureMode>
        assertThrows(UnsupportedOperationException::class.java) {
            mutableView[FeatureId.AI_MEDIA] = FeatureMode.ON
        }
        assertEquals(FeatureMode.OFF, FeatureRegistry().mode(FeatureId.AI_MEDIA))
    }

    @Test
    fun registryIsIsolatedFromLaterChangesToOverrideSource() {
        val source = mutableMapOf(FeatureId.LIVE_TV to FeatureMode.ON)
        val registry = FeatureRegistry(source)
        source[FeatureId.LIVE_TV] = FeatureMode.OFF
        source[FeatureId.AI_MEDIA] = FeatureMode.ON
        assertEquals(FeatureMode.ON, registry.mode(FeatureId.LIVE_TV))
        assertEquals(FeatureMode.OFF, registry.mode(FeatureId.AI_MEDIA))
        assertEquals(FeatureMode.OFF, FeatureRegistry().mode(FeatureId.LIVE_TV))
    }
}
