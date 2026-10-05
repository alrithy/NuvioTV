package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.WatchProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCompletedProgressSnapshotTest {
    @Test fun `shared projection retains one newest completed playable fact and ignores newer started content`() {
        val newestCompleted = progress("movie", 200, 95)
        val snapshot = homeCompletedProgressSnapshot(1, listOf(progress("series", 100, 95),
            newestCompleted, progress("movie", 500, 15), progress("channel", 600, 100)))
        assertSame(newestCompleted, snapshot.latestCompleted)
        assertEquals(listOf(newestCompleted), snapshot.progressForProfile(1))
    }

    @Test fun `switching profiles hides retained projection before the next source emission`() {
        val retained = homeCompletedProgressSnapshot(1, listOf(progress("movie", 200, 95)))
        assertTrue(retained.progressForProfile(2).isEmpty())
        assertTrue((null as HomeCompletedProgressSnapshot?).progressForProfile(2).isEmpty())
        assertTrue(homeCompletedProgressSnapshot(2, emptyList()).progressForProfile(2).isEmpty())
    }

    private fun progress(type: String, watchedAt: Long, percent: Int) = WatchProgress("$type-$watchedAt", type,
        "Title", null, null, null, "video", null, null, null, percent.toLong(), 100, watchedAt)
}
