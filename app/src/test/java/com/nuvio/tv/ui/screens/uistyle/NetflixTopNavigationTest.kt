package com.nuvio.tv.ui.screens.uistyle

import com.nuvio.tv.DrawerItem
import org.junit.Assert.assertEquals
import org.junit.Test

class NetflixTopNavigationTest {
    @Test fun `search leads, content tabs keep their order and settings trails as a secondary icon`() {
        val entries = netflixNavEntries(
            listOf(DrawerItem("search", "Search"), DrawerItem("home", "Home"), DrawerItem("discover?type=series", "Shows"),
                DrawerItem("discover?type=movie", "Movies"), DrawerItem("my_netflix", "My Netflix"),
                DrawerItem("live_tv", "Live TV"), DrawerItem("settings", "Settings")),
            searchRoute = "search", settingsRoute = "settings",
        )
        assertEquals(listOf("search", "home", "discover?type=series", "discover?type=movie", "my_netflix", "live_tv", "settings"),
            entries.map { it.route })
        assertEquals(NetflixNavEntry.Kind.SEARCH, entries.first().kind)
        assertEquals(NetflixNavEntry.Kind.SETTINGS, entries.last().kind)
        assertEquals(5, entries.count { it.kind == NetflixNavEntry.Kind.TAB })
    }

    @Test fun `missing optional destinations are not invented`() {
        val entries = netflixNavEntries(listOf(DrawerItem("home", "Home")), "search", "settings")
        assertEquals(listOf("home"), entries.map { it.route })
    }
}
