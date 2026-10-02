package com.nuvio.tv.fork.subtitles

import com.nuvio.tv.domain.model.Subtitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G14 device finding: Auto Sync picked an unsuitable subtitle (D067). */
class SubtitleCandidateRankingTest {

    private fun sub(id: String, lang: String, file: String = "$id.srt", streamProvided: Boolean = false) =
        Subtitle(id = id, url = "https://subs.example/download/$file", lang = lang, addonName = "Subs", addonLogo = null, isStreamProvided = streamProvided)

    private val episode = SubtitleCandidateRanking.Playing(
        filename = "The.Show.S02E05.1080p.WEB-DL.DDP5.1.H.264-NTb.mkv",
        season = 2,
        episode = 5,
    )
    private val movie = SubtitleCandidateRanking.Playing(filename = "Film.2023.2160p.UHD.BluRay.REMUX.HDR.HEVC.Atmos-FraMeSToR.mkv")

    @Test
    fun theReleaseMatchWinsAmongSeveralArabicFiles() {
        val candidates = listOf(
            sub("1", "ara", "Film.2023.720p.HDTV.x264-OTHER.srt"),
            sub("2", "ar", "Film.2023.2160p.WEB-DL.HDR.HEVC-WEBGRP.srt"),
            sub("3", "Arabic", "Film.2023.2160p.UHD.BluRay.REMUX.HDR.HEVC.Atmos-FraMeSToR.srt"),
        )
        val ranked = SubtitleCandidateRanking.rank(candidates, "ar", movie)
        assertEquals(listOf("3", "2", "1"), ranked.map { it.id })
    }

    @Test
    fun aWrongLanguageHigherInProviderOrderIsNeverPicked() {
        val candidates = listOf(
            sub("en", "eng", "Film.2023.2160p.UHD.BluRay.REMUX-FraMeSToR.srt"),
            sub("fa", "per", "Film.2023.2160p.UHD.BluRay.REMUX-FraMeSToR.srt"),
            sub("ar", "العربية", "Film.2023.1080p.WEB-DL-OTHER.srt"),
        )
        assertEquals("ar", SubtitleCandidateRanking.best(candidates, "ar", movie)?.id)
        assertEquals(listOf("ar"), SubtitleCandidateRanking.rank(candidates, "ar", movie).map { it.id })
    }

    @Test
    fun aSubtitleForAnotherEpisodeIsLeftOut() {
        val candidates = listOf(
            sub("e4", "ar", "The.Show.S02E04.1080p.WEB-DL.DDP5.1.H.264-NTb.srt"),
            sub("e15", "ar", "The.Show.2x15.WEB.srt"),
            sub("s1e5", "ar", "The.Show.S01E05.1080p.WEB-DL-NTb.srt"),
            sub("e5", "ar", "The.Show.S02E05.720p.HDTV.srt"),
            sub("noep", "ar", "the-show-arabic.srt"),
        )
        // The one naming this episode first, then the one naming none; the others are out.
        assertEquals(listOf("e5", "noep"), SubtitleCandidateRanking.rank(candidates, "ar", episode).map { it.id })
    }

    @Test
    fun nothingFitsMeansNothingIsPicked() {
        val candidates = listOf(sub("en", "en"), sub("fr", "French"), sub("e9", "ar", "The.Show.S02E09.srt"))
        assertNull(SubtitleCandidateRanking.best(candidates, "ar", episode))
        assertTrue(SubtitleCandidateRanking.rank(emptyList(), "ar", episode).isEmpty())
        assertTrue(SubtitleCandidateRanking.rank(candidates, "", episode).isEmpty())
    }

    @Test
    fun withNothingToTellThemApartProviderOrderStands() {
        val candidates = listOf(sub("a", "ar", "123.srt"), sub("b", "ara", "456.srt"), sub("c", "ar", "789.srt"))
        assertEquals(listOf("a", "b", "c"), SubtitleCandidateRanking.rank(candidates, "ar", movie).map { it.id })
    }

    @Test
    fun theStreamsOwnSubtitleOrTheFileHashComesFirst() {
        val candidates = listOf(
            sub("release", "ar", "Film.2023.2160p.UHD.BluRay.REMUX.HDR.HEVC.Atmos-FraMeSToR.srt"),
            sub("own", "ar", "track3.srt", streamProvided = true),
        )
        assertEquals("own", SubtitleCandidateRanking.best(candidates, "ar", movie)?.id)
        // Without the stream's own subtitle (no same-file reference) the release match decides.
        assertEquals("release", SubtitleCandidateRanking.best(candidates.take(1) + sub("other", "ar", "x.srt"), "ar", movie)?.id)
        val hashed = listOf(sub("plain", "ar", "Film.2023.REMUX-FraMeSToR.srt"), sub("hash", "ar", "8e245d9679d31e12.srt"))
        assertEquals("hash", SubtitleCandidateRanking.best(hashed, "ar", movie.copy(videoHash = "8E245D9679D31E12"))?.id)
    }

    @Test
    fun languageCodesAndNamesMeanTheSameLanguage() {
        listOf("ar", "ara", "ar-SA", "AR_EG", "Arabic", "arabic", "العربية", "عربي", "Arabic (SDH)", "arabic.forced").forEach {
            assertEquals(it, "ar", SubtitleCandidateRanking.canonicalLanguage(it))
        }
        assertEquals("en", SubtitleCandidateRanking.canonicalLanguage("English"))
        assertEquals("en", SubtitleCandidateRanking.canonicalLanguage("eng"))
        assertEquals("pt-br", SubtitleCandidateRanking.canonicalLanguage("pt-BR"))
        assertEquals("pt-br", SubtitleCandidateRanking.canonicalLanguage("Portuguese (Brazil)"))
        assertEquals("", SubtitleCandidateRanking.canonicalLanguage("  "))
    }

    @Test
    fun releaseTraitsAreReadFromTheName() {
        val tokens = SubtitleCandidateRanking.ReleaseTokens.of("The.Show.S02E05.1080p.WEB-DL.DDP5.1.H.264-NTb.mkv")
        assertEquals("ntb", tokens.group)
        assertEquals("web", tokens.source)
        assertEquals("1080", tokens.resolution)
        assertNull(SubtitleCandidateRanking.ReleaseTokens.of("Show.1080p.WEB-DL").group)
    }
}
