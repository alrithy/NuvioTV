package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.theme.netflixIsolate
import com.nuvio.tv.ui.theme.netflixMetadataLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetflixCalloutTest {
    private val catalog = ModernPayload.Catalog("k", "id", "movie", "https://addon.invalid", "T", "2026", "movie")

    @Test fun `a callout is produced only from real progress, next-up or list membership`() {
        val progress = WatchProgress("id", "series", "T", null, null, null, "v", 2, 5, null, 100, 1000, 1)
        assertEquals(NetflixCallout(NetflixCalloutKind.CONTINUE, 2, 5),
            netflixCallout(ModernPayload.ContinueWatching(ContinueWatchingItem.InProgress(progress)), inLibrary = true))
        assertEquals(NetflixCallout(NetflixCalloutKind.IN_MY_LIST), netflixCallout(catalog, inLibrary = true))
        assertNull(netflixCallout(catalog, inLibrary = false))
        assertNull(netflixCallout(null, inLibrary = false))
    }

    @Test fun `mixed-direction tokens are isolated and missing facts vanish`() {
        assertEquals("⁨S1 E2⁩", netflixIsolate("S1 E2"))
        assertEquals("⁨2026⁩ · ⁨IMDb 8.1⁩", netflixMetadataLine(listOf("2026", null, " ", "IMDb 8.1")))
        assertEquals("", netflixIsolate(""))
    }
}
