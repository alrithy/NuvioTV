package com.nuvio.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nuvio.tv.domain.model.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetflixThemeContrastTest {
    @Test
    fun netflixUsesBlackCanvasAndReadableMonochromeFocusAcrossSharedSurfaces() {
        val palette = ThemeColors.getColorPalette(AppTheme.NETFLIX)
        assertEquals(Color.Black, palette.background)
        assertEquals(Color.White, palette.focusRing)
        assertTrue(contrast(palette.secondary, palette.onSecondary) >= 7f)
        listOf(palette.background, palette.backgroundCard, palette.surface, palette.menu, palette.modal).forEach {
            assertTrue("Primary text must remain readable on every Netflix surface", contrast(Color.White, it) >= 7f)
            assertTrue("Metadata must remain readable on every Netflix surface", contrast(NetflixThemeTokens.textSecondary, it) >= 4.5f)
        }
    }

    @Test
    fun addingNetflixPreservesExistingDefaultAndThemePalettes() {
        assertEquals(ThemeColors.White, ThemeColors.getColorPalette(AppTheme.WHITE))
        assertEquals(ThemeColors.Ocean, ThemeColors.getColorPalette(AppTheme.OCEAN))
        assertEquals(ThemeColors.Crimson, ThemeColors.getColorPalette(AppTheme.CRIMSON))
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
}
