package com.nuvio.tv.fork.uistyle

import com.nuvio.tv.fork.resource.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G12a–G12c: navigation style choice and fallback, the Back contract, the clock, Glass effects. */
class UiStyleRulesTest {

    @Test
    fun officialSidebarIsTheDefaultAndTheOnlyStyleWhileTheFeatureIsOff() {
        assertEquals(NavigationStyle.SIDEBAR, NavigationStyle.fromStored(null))
        assertEquals(NavigationStyle.SIDEBAR, NavigationStyle.fromStored("GLASS_FROM_A_NEWER_BUILD"))
        assertEquals(NavigationStyle.PILL, NavigationStyle.fromStored("PILL"))
        assertEquals(NavigationStyle.TOP_BAR, UiStyleRules.navigationStyle(NavigationStyle.TOP_BAR, featureEnabled = true))
        NavigationStyle.entries.forEach {
            assertEquals(NavigationStyle.SIDEBAR, UiStyleRules.navigationStyle(it, featureEnabled = false))
        }
    }

    @Test
    fun settingsCycleThroughEveryStyle() {
        assertEquals(NavigationStyle.TOP_BAR, NavigationStyle.SIDEBAR.next())
        assertEquals(NavigationStyle.PILL, NavigationStyle.TOP_BAR.next())
        assertEquals(NavigationStyle.GLASS, NavigationStyle.PILL.next())
        assertEquals(NavigationStyle.CINEMATIC_GLASS, NavigationStyle.GLASS.next())
        assertEquals(NavigationStyle.SIDEBAR, NavigationStyle.CINEMATIC_GLASS.next())
    }

    @Test
    fun backFocusesTheMenuFirstAndThenLeavesLikeTheOfficialSidebar() {
        assertEquals(TopChromeBack.FOCUS_CHROME, UiStyleRules.back(onRootRoute = true, chromeFocused = false, longPressBackHeld = false))
        assertEquals(TopChromeBack.EXIT_APP, UiStyleRules.back(onRootRoute = true, chromeFocused = true, longPressBackHeld = false))
        assertEquals(TopChromeBack.IGNORE, UiStyleRules.back(onRootRoute = true, chromeFocused = true, longPressBackHeld = true))
        assertEquals(TopChromeBack.IGNORE, UiStyleRules.back(onRootRoute = false, chromeFocused = false, longPressBackHeld = false))
    }

    @Test
    fun theClockFollowsTheDeviceFormatAndTicksOnTheMinute() {
        assertEquals("HH:mm", UiStyleRules.clockPattern(is24Hour = true))
        assertEquals("h:mm a", UiStyleRules.clockPattern(is24Hour = false))
        assertEquals(60_000L, UiStyleRules.millisToNextMinute(120_000L))
        assertEquals(1L, UiStyleRules.millisToNextMinute(119_999L))
        assertEquals(30_000L, UiStyleRules.millisToNextMinute(30_000L))
    }

    @Test
    fun glassIsAModernReskinAndFallsBackToThePillElsewhere() {
        assertEquals(NavigationStyle.GLASS, UiStyleRules.effectiveStyle(NavigationStyle.GLASS, modernLayout = true, featureEnabled = true))
        assertEquals(NavigationStyle.PILL, UiStyleRules.effectiveStyle(NavigationStyle.GLASS, modernLayout = false, featureEnabled = true))
        assertEquals(NavigationStyle.TOP_BAR, UiStyleRules.effectiveStyle(NavigationStyle.TOP_BAR, modernLayout = false, featureEnabled = true))
        assertEquals(NavigationStyle.SIDEBAR, UiStyleRules.effectiveStyle(NavigationStyle.GLASS, modernLayout = true, featureEnabled = false))
        assertEquals(NavigationStyle.CINEMATIC_GLASS, UiStyleRules.effectiveStyle(NavigationStyle.CINEMATIC_GLASS, modernLayout = true, featureEnabled = true))
        assertEquals(NavigationStyle.PILL, UiStyleRules.effectiveStyle(NavigationStyle.CINEMATIC_GLASS, modernLayout = false, featureEnabled = true))
        assertEquals(NavigationStyle.SIDEBAR, UiStyleRules.effectiveStyle(NavigationStyle.CINEMATIC_GLASS, modernLayout = true, featureEnabled = false))
        assertTrue(NavigationStyle.CINEMATIC_GLASS.isGlass && NavigationStyle.GLASS.isGlass)
        assertFalse(NavigationStyle.PILL.isGlass || NavigationStyle.TOP_BAR.isGlass || NavigationStyle.SIDEBAR.isGlass)
    }

    @Test
    fun liveBlurOnlyWhereTheDeviceCanAffordItAndTheUserWantsIt() {
        assertEquals(GlassEffect.BLUR, UiStyleRules.glassEffect(31, MemoryTier.STANDARD, lightweight = false))
        assertEquals(GlassEffect.BLUR, UiStyleRules.glassEffect(32, MemoryTier.STANDARD, lightweight = false))
        assertEquals(GlassEffect.BLUR, UiStyleRules.glassEffect(34, MemoryTier.CONSTRAINED, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(30, MemoryTier.STANDARD, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(34, MemoryTier.LOW_RAM, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(34, null, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(34, MemoryTier.STANDARD, lightweight = true))
    }

    @Test
    fun liquidGlassOnlyOnAndroid13WithMemoryToSpare() {
        assertEquals(GlassEffect.LIQUID, UiStyleRules.glassEffect(33, MemoryTier.STANDARD, lightweight = false))
        assertEquals(GlassEffect.LIQUID, UiStyleRules.glassEffect(35, MemoryTier.STANDARD, lightweight = false))
        assertEquals(GlassEffect.BLUR, UiStyleRules.glassEffect(33, MemoryTier.CONSTRAINED, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(33, MemoryTier.LOW_RAM, lightweight = false))
        assertEquals(GlassEffect.FLAT, UiStyleRules.glassEffect(35, MemoryTier.STANDARD, lightweight = true))
    }

    @Test
    fun theGlassChromeLingersOnArrivalAndLeavesPromptlyAfterUse() {
        assertEquals(3_500L, UiStyleRules.glassChromeHideDelayMs(hasHeldFocus = false))
        assertEquals(250L, UiStyleRules.glassChromeHideDelayMs(hasHeldFocus = true))
    }
}
