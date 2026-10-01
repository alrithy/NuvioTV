package com.nuvio.tv.fork.livetv

import com.nuvio.tv.fork.resource.AdaptiveResourcePolicy
import com.nuvio.tv.fork.resource.MemoryTier
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G10f: the channel preview's per-profile settings, their device default and the preview rules. */
class LiveTvPreviewTest {
    private val http = FakeLiveTvFetcher { throw IOException("unexpected") }
    private val lowRam = AdaptiveResourcePolicy(MemoryTier.LOW_RAM).liveTvPreviewBudget
    private val standard = AdaptiveResourcePolicy(MemoryTier.STANDARD).liveTvPreviewBudget

    private fun repository(store: MemoryLiveTvStore, profile: MutableStateFlow<Int>, budget: () -> com.nuvio.tv.fork.resource.LiveTvPreviewBudget, enabled: Boolean = true) =
        LiveTvRepository(store, store, http, profile, enabled, previewBudget = budget)

    @Test
    fun previewsFollowTheDeviceUntilTheViewerChoosesPerProfile() = runBlocking {
        val store = MemoryLiveTvStore()
        val profile = MutableStateFlow(1)
        val onLowRam = repository(store, profile, { lowRam })
        assertEquals(LiveTvPreviewSettings(previews = false, sound = true), onLowRam.previewSettings.first())
        assertEquals(LiveTvPreviewSettings(previews = true, sound = true), repository(store, profile, { standard }).previewSettings.first())

        onLowRam.setPreviewsEnabled(true)
        assertTrue(withTimeout(5_000) { onLowRam.previewSettings.first { it.previews } }.sound)
        onLowRam.setPreviewSound(false)
        assertEquals(
            LiveTvPreviewSettings(previews = true, sound = false),
            withTimeout(5_000) { onLowRam.previewSettings.first { !it.sound } },
        )
        assertEquals(LiveTvPreviewChoice(previews = true, sound = false), store.previewChoices.getValue(1).value)

        // Another profile keeps the device default.
        profile.value = 2
        assertEquals(LiveTvPreviewSettings(previews = false, sound = true), withTimeout(5_000) { onLowRam.previewSettings.first { it.sound } })
    }

    @Test
    fun liveTvOffNeverPreviews() = runBlocking {
        val store = MemoryLiveTvStore()
        store.setPreviewChoice(1, LiveTvPreviewChoice(previews = true))
        assertEquals(LiveTvPreviewSettings.OFF, repository(store, MutableStateFlow(1), { standard }, enabled = false).previewSettings.first())
    }

    @Test
    fun stalkerChannelsAreNotPreviewedAndHlsIsRecognisedByItsLink() {
        assertTrue(LiveTvPreviewRules.canPreview(LiveTvChannel(id = "1", name = "One", streamUrl = "http://s/1.ts")))
        assertFalse(LiveTvPreviewRules.canPreview(LiveTvChannel(id = "2", name = "Two", streamUrl = "", stalkerCommand = "ffrt http://l/ch/2")))
        assertTrue(LiveTvPreviewRules.looksHls("http://s/live/u/p/1.M3U8?token=x"))
        assertFalse(LiveTvPreviewRules.looksHls("http://s/live/u/p/1.ts"))
    }
}
