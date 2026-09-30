package com.nuvio.tv.fork.subtitles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicCinemaPresetTest {

    @Test
    fun presetStaysInsideOfficialSettingRanges() {
        val style = ArabicCinemaPreset.style
        // Official PlayerSettingsDataStore bounds: size 50-200, vertical offset -20..50, outline 1-5.
        assertTrue(style.size in 50..200)
        assertTrue(style.verticalOffset in -20..50)
        assertTrue(style.outlineWidth in 1..5)
        assertTrue(style.outlineEnabled)
    }

    @Test
    fun appliedOnlyWhenEveryOwnedFieldMatches() {
        assertTrue(ArabicCinemaPreset.isApplied(ArabicCinemaPreset.style))
        assertFalse(ArabicCinemaPreset.isApplied(ArabicCinemaPreset.style.copy(size = 120)))
        assertFalse(ArabicCinemaPreset.isApplied(ArabicCinemaPreset.style.copy(bold = false)))
    }

    @Test
    fun officialDefaultsAreNotThePreset() {
        // Official SubtitleStyleSettings defaults: 120 %, offset 5, regular, white, no box, black outline 2.
        val official = ArabicCinemaPreset.Style(120, 5, false, 0xFFFFFFFF.toInt(), 0, true, 0xFF000000.toInt(), 2)
        assertFalse(ArabicCinemaPreset.isApplied(official))
    }
}
