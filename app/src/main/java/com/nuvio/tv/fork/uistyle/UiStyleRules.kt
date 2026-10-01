package com.nuvio.tv.fork.uistyle

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/*
 * G12a UI styles (D058; features 196, 198, 204, 205): where the main menu sits. Pure, so the choices
 * and the Back contract are JVM-tested. Official's sidebars stay the default and the only option
 * while UI_STYLES is OFF.
 */

/** Where the main menu sits for Classic / Grid / Modern. [SIDEBAR] is official's own (legacy or modern). */
enum class NavigationStyle {
    SIDEBAR,

    /** Cxsmo `TopNavigation` @ 3e0d0fa: a full-width bar across the top. */
    TOP_BAR,

    /** Reshaped pill menu @ 0ccf049 / NuvioGlass nav pill @ 84098b7: a compact floating pill. */
    PILL;

    /** The next choice in Settings (Sidebar → Top bar → Pill → Sidebar). */
    fun next(): NavigationStyle = entries[(ordinal + 1) % entries.size]

    companion object {
        /** An unknown or missing stored value is official's sidebar. */
        fun fromStored(name: String?): NavigationStyle = entries.firstOrNull { it.name == name } ?: SIDEBAR
    }
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
}
