package com.nuvio.tv.fork.discovery

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class MysteryStreamContextTest {
    @Test
    fun `restored route retains Mystery without the transient picker`() {
        val original = SavedStateHandle()
        MysteryStreamContext(original, selectedByShuffle = true, enabled = true).resolve(true)
        val restored = SavedStateHandle(mapOf(
            MysteryStreamContext.PICK to original.get<Boolean>(MysteryStreamContext.PICK),
            MysteryStreamContext.MYSTERY to original.get<Boolean>(MysteryStreamContext.MYSTERY),
        ))
        val context = MysteryStreamContext(restored, selectedByShuffle = false, enabled = true)
        assertTrue(context.shufflePick)
        assertTrue(context.mystery)
    }

    @Test
    fun `ordinary selection and flag OFF preserve official presentation`() {
        assertFalse(MysteryStreamContext(SavedStateHandle(), false, true).mystery)
        val saved = SavedStateHandle(mapOf(MysteryStreamContext.PICK to true, MysteryStreamContext.MYSTERY to true))
        val disabled = MysteryStreamContext(saved, true, false)
        assertFalse(disabled.shufflePick)
        assertFalse(disabled.mystery)
        assertTrue(saved.get<Boolean>(MysteryStreamContext.MYSTERY)!!)
    }

    @Test
    fun `turning Mystery off remains off after restoration`() {
        val state = SavedStateHandle()
        MysteryStreamContext(state, true, true).resolve(false)
        assertFalse(MysteryStreamContext(state, false, true).mystery)
    }

    @Test
    fun `manual fallback card carries no episode or source metadata`() {
        val display = mysteryStreamPresentation("Source 2")
        assertEquals("Source 2", display.getDisplayNameOrNull())
        assertNull(display.getDisplayDescription())
        assertNull(display.url)
        assertNull(display.behaviorHints)
        assertNull(display.addonLogo)
        assertEquals("", display.addonName)
        assertTrue(display.badges.isEmpty())
    }
}
