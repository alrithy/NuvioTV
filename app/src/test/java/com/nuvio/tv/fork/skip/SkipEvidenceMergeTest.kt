package com.nuvio.tv.fork.skip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipEvidenceMergeTest {

    private fun report(start: Double, end: Double, provider: String, confidence: Double, category: String = SkipCategories.OPENING, action: String = SkipReport.ACTION_SKIP) =
        SkipReport(start, end, category, SkipCategories.officialType(category), provider, confidence, action)

    @Test
    fun overlappingReportsMergeWithConfidenceWeightedTimes() {
        val merged = SkipEvidenceMerge.merge(
            listOf(report(60.0, 90.0, "introdb", 0.9), report(62.0, 92.0, "skipme", 0.3)),
        )
        assertEquals(1, merged.size)
        val only = merged.single()
        assertEquals(listOf("introdb", "skipme"), only.providers)
        assertEquals((60.0 * 0.9 + 62.0 * 0.3) / 1.2, only.report.startTime, 1e-9)
        assertEquals((90.0 * 0.9 + 92.0 * 0.3) / 1.2, only.report.endTime, 1e-9)
        // Mean confidence plus one extra provider's bonus.
        assertEquals((0.9 + 0.3) / 2 + 0.04, only.report.confidence, 1e-9)
        assertEquals("introdb", only.report.provider)
    }

    @Test
    fun reportsOnlyMergeWithinTheTolerance() {
        val apart = SkipEvidenceMerge.merge(listOf(report(10.0, 20.0, "a", 0.8), report(23.0, 30.0, "b", 0.8)))
        assertEquals(2, apart.size)
        val close = SkipEvidenceMerge.merge(listOf(report(10.0, 20.0, "a", 0.8), report(21.5, 30.0, "b", 0.8)))
        assertEquals(1, close.size)
    }

    @Test
    fun categoriesAndActionsNeverMerge() {
        val merged = SkipEvidenceMerge.merge(
            listOf(
                report(0.0, 30.0, "a", 0.9, SkipCategories.RECAP),
                report(0.0, 30.0, "b", 0.9, SkipCategories.OPENING),
                report(0.0, 30.0, "c", 0.9, SkipCategories.OPENING, action = "mute"),
            ),
        )
        assertEquals(3, merged.size)
    }

    @Test
    fun invalidReportsAreDropped() {
        val merged = SkipEvidenceMerge.merge(
            listOf(
                report(30.0, 10.0, "backwards", 0.9),
                report(-1.0, 10.0, "negative", 0.9),
                report(Double.NaN, 10.0, "nan", 0.9),
                report(5.0, 15.0, "ok", 0.9),
            ),
        )
        assertEquals(listOf("ok"), merged.map { it.report.provider })
    }

    @Test
    fun onePerCategoryPrefersConfidenceThenAgreement() {
        val merged = SkipEvidenceMerge.merge(
            listOf(
                report(60.0, 90.0, "introdb", 0.9),
                report(300.0, 330.0, "skipme", 0.5),
                report(1200.0, 1260.0, "theintrodb", 0.86, SkipCategories.ENDING),
            ),
        )
        val best = SkipEvidenceMerge.bestPerCategory(merged)
        assertEquals(listOf("introdb", "theintrodb"), best.map { it.report.provider })
    }

    @Test
    fun providerTypesMapOntoOfficialCategories() {
        assertEquals(SkipCategories.OPENING, SkipCategories.categoryOf("op", isMovie = false))
        assertEquals(SkipCategories.OPENING, SkipCategories.categoryOf("Intro", isMovie = true))
        assertEquals(SkipCategories.ENDING, SkipCategories.categoryOf("credits", isMovie = false))
        assertEquals(SkipCategories.MOVIE_CREDITS, SkipCategories.categoryOf("credits", isMovie = true))
        assertEquals(SkipCategories.POST_CREDITS, SkipCategories.categoryOf("post-credits", isMovie = true))
        assertEquals(SkipCategories.PREVIEW, SkipCategories.categoryOf("preview", isMovie = false))
        assertEquals(SkipCategories.JUMPSCARE, SkipCategories.categoryOf("jumpscare", isMovie = true))
        assertNull("unknown types stay out", SkipCategories.categoryOf("custom", isMovie = false))
        assertEquals("intro", SkipCategories.officialType(SkipCategories.OPENING))
        assertEquals("outro", SkipCategories.officialType(SkipCategories.ENDING))
        assertEquals("movie-credits", SkipCategories.officialType(SkipCategories.MOVIE_CREDITS))
    }

    @Test
    fun creditsNeverCoverAPostCreditsScene() {
        val credits = MergedSkip(report(6000.0, 6300.0, "skipme", 0.9, SkipCategories.MOVIE_CREDITS), listOf("skipme"))
        val scene = MergedSkip(report(6200.0, 6260.0, "introdb", 0.9, SkipCategories.POST_CREDITS), listOf("introdb"))
        val guarded = SkipCategories.guardPostCredits(listOf(credits, scene))
        assertEquals(6200.0, guarded.first { it.report.category == SkipCategories.MOVIE_CREDITS }.report.endTime, 0.0)

        val swallowed = MergedSkip(report(6210.0, 6250.0, "a", 0.9, SkipCategories.MOVIE_CREDITS), listOf("a"))
        assertTrue(SkipCategories.guardPostCredits(listOf(swallowed, scene)).none { it.report.category == SkipCategories.MOVIE_CREDITS })
    }

    @Test
    fun contentSegmentsAllStayWhileOfficialCategoriesKeepOne() {
        val merged = SkipEvidenceMerge.merge(
            listOf(
                report(600.0, 604.0, "notscare", 0.76, SkipCategories.JUMPSCARE, SkipReport.ACTION_WARN),
                report(1800.0, 1806.0, "notscare", 0.76, SkipCategories.JUMPSCARE, SkipReport.ACTION_WARN),
                report(60.0, 90.0, "introdb", 0.9),
                report(900.0, 930.0, "skipme", 0.5),
            ),
        )
        val best = SkipEvidenceMerge.bestPerCategory(merged)
        assertEquals(listOf(60.0, 600.0, 1800.0), best.map { it.report.startTime })
    }

    @Test
    fun optionalCategoriesNeedTheUsersSwitchAndStayOffExternalPlayers() {
        assertTrue(SkipCategories.allowed(SkipCategories.OPENING, emptySet()))
        assertTrue(!SkipCategories.allowed(SkipCategories.PREVIEW, emptySet()))
        assertTrue(SkipCategories.allowed(SkipCategories.PREVIEW, setOf(SkipCategories.PREVIEW)))
        assertTrue(!SkipCategories.allowed(SkipCategories.GORE, setOf(SkipCategories.PREVIEW)))
        assertTrue(SkipCategories.forwardableToExternalPlayer("intro", SkipReport.ACTION_SKIP))
        assertTrue(!SkipCategories.forwardableToExternalPlayer("intro", SkipReport.ACTION_MUTE))
        assertTrue(!SkipCategories.forwardableToExternalPlayer("jumpscare", SkipReport.ACTION_SKIP))
        assertTrue(!SkipCategories.forwardableToExternalPlayer("preview", SkipReport.ACTION_SKIP))
    }

    @Test
    fun labelsMapToCategories() {
        assertEquals(SkipCategories.JUMPSCARE, SkipCategories.categoryOfLabel("Jump Scare (major)", isMovie = true))
        assertEquals(SkipCategories.PROFANITY, SkipCategories.categoryOfLabel("strong language", isMovie = true))
        assertEquals(SkipCategories.MOVIE_CREDITS, SkipCategories.categoryOfLabel("End credits", isMovie = true))
        assertEquals(SkipCategories.ENDING, SkipCategories.categoryOfLabel("End credits", isMovie = false))
        assertNull(SkipCategories.categoryOfLabel("something else", isMovie = true))
    }
}
