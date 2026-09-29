package com.nuvio.tv.fork.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    fun everyDefaultPreservesOfficialBehavior() {
        val registry = FeatureRegistry()
        FeatureId.entries.forEach { id ->
            assertEquals("$id default", FeatureMode.OFF, registry.mode(id))
        }
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
    }

    @Test
    fun overridesApplyOnlyToTheirFeature() {
        val registry = FeatureRegistry(mapOf(FeatureId.UNIFIED_DIAGNOSTICS to FeatureMode.AUTO))
        assertEquals(FeatureMode.AUTO, registry.mode(FeatureId.UNIFIED_DIAGNOSTICS))
        FeatureId.entries.filter { it != FeatureId.UNIFIED_DIAGNOSTICS }.forEach {
            assertEquals(FeatureMode.OFF, registry.mode(it))
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
