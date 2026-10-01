package com.nuvio.tv.fork.uistyle

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import com.nuvio.tv.fork.resource.MemoryTier

/*
 * G12a–G12c UI styles (D058, D060, D061; features 193, 194, 196–200, 204, 205): where the main menu sits
 * and how its glass is drawn. Pure, so the choices and the Back contract are JVM-tested. Official's
 * sidebars stay the default and the only option while UI_STYLES is OFF.
 */

/** Where the main menu sits for Classic / Grid / Modern. [SIDEBAR] is official's own (legacy or modern). */
enum class NavigationStyle {
    SIDEBAR,

    /** Cxsmo `TopNavigation` @ 3e0d0fa: a full-width bar across the top. */
    TOP_BAR,

    /** Reshaped pill menu @ 0ccf049 / NuvioGlass nav pill @ 84098b7: a compact floating pill. */
    PILL,

    /**
     * NuvioGlass Glass layout @ 84098b7 (D060): official's Modern home with frosted top chrome
     * floating over its full-bleed hero, hiding itself on Home until Up from the first row.
     */
    GLASS,

    /**
     * Glass with the focused title's artwork full screen behind everything (official's Modern
     * full-screen hero backdrop, without changing that setting); the Cinema View idea (D061).
     */
    CINEMATIC_GLASS;

    /** Both Glass styles float frosted chrome over official's Modern home. */
    val isGlass: Boolean get() = this == GLASS || this == CINEMATIC_GLASS

    /** The next choice in Settings (Sidebar → Top bar → Pill → Glass → Sidebar). */
    fun next(): NavigationStyle = entries[(ordinal + 1) % entries.size]

    companion object {
        /** An unknown or missing stored value is official's sidebar. */
        fun fromStored(name: String?): NavigationStyle = entries.firstOrNull { it.name == name } ?: SIDEBAR
    }
}

/** How the Glass chrome is drawn, from richest to cheapest. */
enum class GlassEffect {
    /** Reshaped's AGSL lens: the screen behind refracted at the rim, with dispersion and a specular edge. */
    LIQUID,

    /** A live blur of the screen behind. */
    BLUR,

    /** An opaque tint with the same edge. */
    FLAT,
}

/** What Back does on a root screen while a top menu is shown (mirrors official's sidebar). */
enum class TopChromeBack {
    /** First Back: focus the menu, as official opens its sidebar. */
    FOCUS_CHROME,

    /** Back while the menu has focus: leave the app (official's exit path). */
    EXIT_APP,

    /** Not a root screen, or a long press is still held: let the screen handle it. */
    IGNORE,
}

object UiStyleRules {
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.UI_STYLES) != FeatureMode.OFF

    /** The style in effect: always official's sidebar while UI_STYLES is OFF. */
    fun navigationStyle(stored: NavigationStyle, featureEnabled: Boolean = enabled): NavigationStyle =
        if (featureEnabled) stored else NavigationStyle.SIDEBAR

    /**
     * The style actually shown. Both Glass styles reskin official's Modern home, so with Classic or
     * Grid they fall back to the plain pill (the closest top menu without an overlay).
     */
    fun effectiveStyle(stored: NavigationStyle, modernLayout: Boolean, featureEnabled: Boolean = enabled): NavigationStyle {
        val style = navigationStyle(stored, featureEnabled)
        return if (style.isGlass && !modernLayout) NavigationStyle.PILL else style
    }

    /**
     * Live blur needs RenderEffect (Android 12, API 31) and memory to spare; 1–1.5 GB boxes, an
     * unknown tier and the user's "Lightweight effects" switch get the opaque tint (feature 200).
     * The liquid-glass lens needs AGSL (Android 13, API 33) and the STANDARD tier, above the 2 GB
     * class (Reshaped asks for about 3 GB); 2 GB boxes keep the blur (feature 199).
     */
    fun glassEffect(sdkInt: Int, tier: MemoryTier?, lightweight: Boolean): GlassEffect = when {
        lightweight || sdkInt < BLUR_MIN_SDK || tier == null || tier == MemoryTier.LOW_RAM -> GlassEffect.FLAT
        sdkInt >= LIQUID_MIN_SDK && tier == MemoryTier.STANDARD -> GlassEffect.LIQUID
        else -> GlassEffect.BLUR
    }

    /**
     * The Glass chrome slides away on Home when idle: it lingers on arrival (so you see where you
     * are) and leaves promptly once it has been used and focus went back to the rows (NuvioGlass).
     */
    fun glassChromeHideDelayMs(hasHeldFocus: Boolean): Long = if (hasHeldFocus) GLASS_DISMISS_MS else GLASS_AUTO_HIDE_MS

    fun back(onRootRoute: Boolean, chromeFocused: Boolean, longPressBackHeld: Boolean): TopChromeBack = when {
        !onRootRoute || longPressBackHeld -> TopChromeBack.IGNORE
        !chromeFocused -> TopChromeBack.FOCUS_CHROME
        else -> TopChromeBack.EXIT_APP
    }

    /** The clock follows the device's 12 / 24-hour setting. */
    fun clockPattern(is24Hour: Boolean): String = if (is24Hour) "HH:mm" else "h:mm a"

    /** The clock redraws on the minute, not every second (NuvioGlass `GlassClockPill`). */
    fun millisToNextMinute(nowEpochMs: Long): Long = MINUTE_MS - Math.floorMod(nowEpochMs, MINUTE_MS)

    private const val MINUTE_MS = 60_000L
    private const val BLUR_MIN_SDK = 31
    private const val LIQUID_MIN_SDK = 33
    const val GLASS_AUTO_HIDE_MS = 3_500L
    const val GLASS_DISMISS_MS = 250L
}
