package com.nuvio.tv.fork.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleRulesTest {
    private val episodes = listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 2 to 3)

    @Test
    fun seasonScopeKeepsOnlyThatSeason() {
        assertEquals(listOf(2 to 1, 2 to 2, 2 to 3), ShuffleRules.scope(episodes, 2) { it.first })
        assertEquals(episodes, ShuffleRules.scope(episodes, null) { it.first })
    }

    @Test
    fun aSeasonTheAddonNoLongerListsNeverEmptiesThePool() {
        assertEquals(episodes, ShuffleRules.scope(episodes, 7) { it.first })
        assertTrue(ShuffleRules.scope(emptyList<Pair<Int, Int>>(), 1) { it.first }.isEmpty())
    }

    @Test
    fun watchedEpisodesJoinOnlyWhenAskedOrWhenTheOptInFallbackFindsNothingUnwatched() {
        assertTrue(ShuffleRules.includeWatched(configured = true, fallback = false, unwatchedCount = 3))
        assertFalse(ShuffleRules.includeWatched(configured = false, fallback = false, unwatchedCount = 0))
        assertFalse(ShuffleRules.includeWatched(configured = false, fallback = true, unwatchedCount = 2))
        assertTrue(ShuffleRules.includeWatched(configured = false, fallback = true, unwatchedCount = 0))
    }
}
