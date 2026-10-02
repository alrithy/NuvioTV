package com.nuvio.tv.ui.screens.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetflixDetailMetadataTest {
    @Test
    fun providerRuntimeFormsKeepTheSameDurationWhenLocalized() {
        listOf("105", "105 min", "105 minutes", "1h 45m", "1:45", "١٠٥ دقيقة").forEach { value ->
            assertEquals(value, 105, netflixDetailRuntimeMinutes(value))
        }
        assertEquals(120, netflixDetailRuntimeMinutes("2h"))
    }

    @Test
    fun missingUnknownAndInvalidRuntimeDoesNotCreateDurationMetadata() {
        listOf(null, "", "0", "0 min", "unknown", "01:99", "2h 80m", "105 seconds", "999999999999h").forEach { value ->
            assertNull(value, netflixDetailRuntimeMinutes(value))
        }
    }
}
