package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.core.player.externalSkipIntervals
import com.nuvio.tv.data.repository.SkipInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Superfork G9b: mute segments stay apart from skip, and never reach an external player. */
class SkipMuteRulesTest {

    private val intro = SkipInterval(60.0, 90.0, "intro", "introdb")
    private val mute = SkipInterval(200.0, 204.0, "profanity", "videoskip", action = "mute")
    private val warn = SkipInterval(600.0, 606.0, "jumpscare", "notscare", action = "warn")

    @Test
    fun `mute is active only inside its span`() {
        val intervals = listOf(intro, mute, warn)
        assertFalse(muteActiveAt(intervals, 199_999))
        assertTrue(muteActiveAt(intervals, 200_000))
        assertTrue(muteActiveAt(intervals, 203_999))
        assertFalse(muteActiveAt(intervals, 204_000))
        assertFalse("a skip segment never mutes", muteActiveAt(intervals, 70_000))
    }

    @Test
    fun `a mute segment never shows the skip button but a warning does`() {
        val intervals = listOf(intro, mute, warn)
        assertNull(findActiveSkipInterval(intervals, 201_000))
        assertEquals(warn, findActiveSkipInterval(intervals, 601_000))
        assertEquals(intro, findActiveSkipInterval(intervals, 61_000))
    }

    @Test
    fun `external players only get plain official skip segments`() {
        val credits = SkipInterval(6000.0, 6200.0, "movie-credits", "introdb")
        val preview = SkipInterval(10.0, 20.0, "preview", "skipme")
        val forwarded = externalSkipIntervals(listOf(intro, mute, warn, credits, preview))
        assertEquals(listOf("intro", "end-credits"), forwarded.map { it.type })
    }
}
