package com.nuvio.tv.fork.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarRulesTest {
    private val today = LocalDate.of(2026, 9, 30)

    private fun episode(show: String, season: Int, number: Int, date: LocalDate, title: String = "$show $season.$number") =
        CalendarEpisode(show, title, "$show:$season:$number", season, number, "Ep $number", date, thumbnail = "still")

    @Test
    fun theWindowRuns30DaysBackAnd90Ahead() {
        assertTrue(CalendarRules.inWindow(today.minusDays(30), today))
        assertFalse(CalendarRules.inWindow(today.minusDays(31), today))
        assertTrue(CalendarRules.inWindow(today.plusDays(90), today))
        assertFalse(CalendarRules.inWindow(today.plusDays(91), today))
    }

    @Test
    fun anEpisodeIsASpoilerOnlyWhileTheOneBeforeItIsUnwatched() {
        val previous = CalendarRules.previousEpisodes(listOf(2 to 1, 1 to 2, 1 to 1, 0 to 1, 1 to 2))
        assertEquals(mapOf((1 to 2) to (1 to 1), (2 to 1) to (1 to 2)), previous)
        assertTrue(CalendarRules.spoilerHidden(false, previous[2 to 1], setOf(1 to 1)))
        assertFalse(CalendarRules.spoilerHidden(false, previous[2 to 1], setOf(1 to 2)))
        assertFalse("the first episode is never hidden", CalendarRules.spoilerHidden(false, previous[1 to 1], emptySet()))
        assertFalse("watched is never hidden", CalendarRules.spoilerHidden(true, previous[2 to 1], emptySet()))
    }

    @Test
    fun daysAreOrderedAndFilteredAroundToday() {
        val days = CalendarRules.groupDays(
            listOf(
                episode("b", 1, 2, today.plusDays(2)),
                episode("a", 1, 1, today.minusDays(3)),
                episode("a", 1, 3, today.plusDays(2), title = "Alpha"),
                episode("c", 1, 1, today),
            ),
        )
        assertEquals(listOf(today.minusDays(3), today, today.plusDays(2)), days.map { it.date })
        assertEquals(listOf("Alpha", "b 1.2"), days.last().episodes.map { it.showTitle })
        assertEquals(2, CalendarRules.filter(days, CalendarFilter.UPCOMING, today).size)
        assertEquals(1, CalendarRules.filter(days, CalendarFilter.PAST, today).size)
        assertEquals(1, CalendarRules.firstUpcomingIndex(days, today))
        assertEquals(0, CalendarRules.firstUpcomingIndex(days.take(1), today))
    }

    @Test
    fun watchedChangesUnlockTheNextEpisodeWithoutRefetching() {
        val days = listOf(CalendarDay(today, listOf(episode("a", 1, 2, today).copy(isSpoilerHidden = true))))
        val previous = mapOf((1 to 2) to (1 to 1))
        val unlocked = CalendarRules.rewatch(days, { setOf(1 to 1) }, { previous })
        assertFalse(unlocked.single().episodes.single().isSpoilerHidden)
        val watched = CalendarRules.rewatch(days, { setOf(1 to 1, 1 to 2) }, { previous })
        assertTrue(watched.single().episodes.single().isWatched)
    }

    @Test
    fun aSpoilerNeverOffersItsOwnStill() {
        val shown = episode("a", 1, 1, today).copy(showBackdrop = "backdrop", showPoster = " ")
        assertEquals(listOf("still", "backdrop"), shown.artworkCandidates())
        assertEquals(listOf("backdrop"), shown.copy(isSpoilerHidden = true).artworkCandidates())
    }
}
