package com.nuvio.tv.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverThemeRouteTest {
    @Test
    fun `plain Discover routes remain compatible and Netflix categories use optional existing destination arguments`() {
        assertEquals("discover", Screen.Discover.createRoute())
        assertEquals("discover", Screen.Discover.createRoute("unsupported"))
        assertEquals("discover?type=movie", Screen.Discover.createRoute("movie"))
        assertEquals("discover?type=series", Screen.Discover.createRoute("series"))
    }
}
