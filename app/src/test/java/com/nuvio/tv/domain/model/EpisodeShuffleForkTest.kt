package com.nuvio.tv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Superfork G9d: season scope, the opt-in all-watched fallback and shuffle picks on official shuffle. */
class EpisodeShuffleForkTest {
    private val shuffle = EpisodeShuffle()
    private val episodes = (1..2).flatMap { season -> (1..4).map { episode(season, it) } }

    private fun episode(season: Int, number: Int) = Video(
        id = "show:$season:$number", title = "S$season E$number", released = null, thumbnail = null,
        season = season, episode = number, overview = null
    )

    @Test
    fun `a season scope only ever picks that season`() {
        repeat(20) { visit ->
            val picked = shuffle.select(
                1, "show", episodes, includeWatched = true, surface = ShuffleSurface.DETAIL,
                visit = visit.toLong(), season = 2
            )
            assertEquals(2, picked?.season)
        }
    }

    @Test
    fun `the fallback reopens a fully watched pool only when switched on`() {
        val watched = episodes.map { it.season!! to it.episode!! }.toSet()
        assertNull(shuffle.select(1, "show", episodes, includeWatched = false, watched = watched, surface = ShuffleSurface.HOME))
        assertNotNull(
            shuffle.select(
                1, "show", episodes, includeWatched = false, watched = watched, surface = ShuffleSurface.HOME,
                fallbackToWatched = true
            )
        )
    }

    @Test
    fun `the fallback continues playback after the last unwatched episode`() {
        val watched = episodes.filterNot { it.id == "show:1:4" }.map { it.season!! to it.episode!! }.toSet()
        assertNull(shuffle.select(1, "show", episodes, false, watched, surface = ShuffleSurface.PLAYBACK, current = 1 to 4))
        val next = shuffle.select(
            1, "other-key", episodes, false, watched, surface = ShuffleSurface.PLAYBACK, current = 1 to 4,
            fallbackToWatched = true
        )
        assertNotNull(next)
        assertFalse(next!!.season == 1 && next.episode == 4)
    }

    @Test
    fun `the fallback still prefers unwatched episodes`() {
        val watched = episodes.filterNot { it.id == "show:2:3" }.map { it.season!! to it.episode!! }.toSet()
        val picked = shuffle.select(
            1, "show", episodes, includeWatched = false, watched = watched, surface = ShuffleSurface.DETAIL,
            fallbackToWatched = true
        )
        assertEquals("show:2:3", picked?.id)
    }

    @Test
    fun `surface picks and dialog hand-offs count as shuffle picks`() {
        val picked = shuffle.select(1, "show", episodes, includeWatched = true, surface = ShuffleSurface.DETAIL)!!
        assertTrue(shuffle.isSelected(1, "show", picked.id))
        assertFalse(shuffle.isSelected(2, "show", picked.id))
        val other = episodes.first { it.id != picked.id }
        assertFalse(shuffle.isSelected(1, "show", other.id))
        shuffle.markHandedOff(1, "show", other.id)
        assertTrue(shuffle.isSelected(1, "show", other.id))
        assertFalse(shuffle.isSelected(1, "other", other.id))
    }
}
