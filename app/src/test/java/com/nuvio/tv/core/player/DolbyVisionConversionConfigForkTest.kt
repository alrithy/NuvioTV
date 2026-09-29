package com.nuvio.tv.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** G5d (feature 52): the fork DV fixes change only the preserve-mapping mode. */
class DolbyVisionConversionConfigForkTest {

    @Test
    fun officialPreserveMappingModeIsKeptWithoutForkFixes() {
        val config = DolbyVisionConversionConfig(active = true, preserveMapping = true, manualDv81 = true)
        assertEquals(5, config.conversionMode(7))
        assertEquals(5, config.conversionMode(null))
    }

    @Test
    fun preserveMappingUsesStandard81WithForkFixes() {
        // Bundled libdovi (dolby_vision 3.3.2): native 4 is To84, so official 5 -> 4 gave static 8.4.
        val config = DolbyVisionConversionConfig(
            active = true, preserveMapping = true, manualDv81 = true, forkDvFixes = true
        )
        assertEquals(2, config.conversionMode(7))
        assertEquals(2, config.conversionMode(null))
        assertEquals(true, config.allowMode2Fallback)
    }

    @Test
    fun otherModesAreUnchangedByForkFixes() {
        for (fixes in listOf(false, true)) {
            assertEquals(3, DolbyVisionConversionConfig(active = true, forkDvFixes = fixes).conversionMode(5))
            assertEquals(2, DolbyVisionConversionConfig(active = true, manualDv81 = true, forkDvFixes = fixes).conversionMode(7))
            assertEquals(1, DolbyVisionConversionConfig(active = true, forkDvFixes = fixes).conversionMode(7))
            assertEquals(4, DolbyVisionConversionConfig(active = true, forcedMode = 4, preserveMapping = true, forkDvFixes = fixes).conversionMode(7))
        }
    }
}
