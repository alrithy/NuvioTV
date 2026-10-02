package com.nuvio.tv.ui.navigation

import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePlaybackNavigationPolicyTest {
    @Test
    fun `Home Play requests playback while More Info remains browse only`() {
        val playRoute = homePlaybackRoute("tmdb:42", "movie", null)
        val infoRoute = Screen.Detail.createRoute("tmdb:42", "movie")

        assertEquals("true", query(playRoute)["playOnLoad"])
        assertEquals("false", query(infoRoute)["playOnLoad"])
        // Selecting Play continues to use the user's existing Stream autoplay preference.
        assertEquals("false", query(playRoute)["manualSelection"])
        assertFalse(playRoute.contains("autoPlayNav="))
    }

    @Test
    fun `series Play preserves title identity for current episode resolution`() {
        val route = homePlaybackRoute("addon:show/arabic title", "series", null)

        assertTrue(route.startsWith("detail/"))
        val path = route.substringBefore('?').split('/').drop(1).map(::decode)
        assertEquals(listOf("addon:show/arabic title", "series"), path)
        assertEquals("", query(route)["returnFocusSeason"])
        assertEquals("", query(route)["returnFocusEpisode"])
        assertFalse(route.contains("videoId="))
    }

    @Test
    fun `Play retains add-on and cinematic backdrop through encoded handoff`() {
        val addon = "https://addon.example/catalog?token=a+b&language=ar"
        val backdrop = "https://images.example/الخلفية.jpg?width=1920&lang=ar"
        val route = homePlaybackRoute("custom:id", "series", addon, backdrop)

        assertEquals(addon, query(route)["addonBaseUrl"])
        assertEquals(backdrop, query(route)["heroBackdropUrl"])
    }

    @Test
    fun `Back from Home playback handoff returns to Home`() {
        val route = homePlaybackRoute("tmdb:42", "movie", null)

        assertEquals("true", query(route)["returnToHomeOnBack"])
        assertEquals("", query(route)["addonBaseUrl"])
    }

    private fun query(route: String): Map<String, String> = route.substringAfter('?')
        .split('&')
        .associate { entry ->
            decode(entry.substringBefore('=')) to decode(entry.substringAfter('='))
        }

    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
