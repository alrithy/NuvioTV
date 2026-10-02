package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.fork.subtitles.SubtitleCandidateRanking

/**
 * G14 device finding hook (D067): official picks the first add-on subtitle in provider order whose
 * language matches; with SUBTITLE_INTELLIGENCE on the pick and AutoSync's candidates go through
 * [SubtitleCandidateRanking] (language names, episode, same file, release). OFF keeps official.
 */
internal fun PlayerRuntimeController.subtitleRankingPlaying() = SubtitleCandidateRanking.Playing(
    filename = currentFilename,
    streamName = currentStreamDescription,
    season = currentSeason,
    episode = currentEpisode,
    videoHash = currentVideoHash,
)

/** The add-on subtitle to auto-pick for [target] among those passing [allowed]. */
internal fun PlayerRuntimeController.pickAddonSubtitle(
    candidates: List<Subtitle>,
    target: String,
    allowed: (Subtitle) -> Boolean = { true },
): Subtitle? {
    if (!SubtitleCandidateRanking.enabled) {
        return candidates.firstOrNull { allowed(it) && PlayerSubtitleUtils.matchesLanguageCode(it.lang, target) }
    }
    return SubtitleCandidateRanking.best(candidates.filter(allowed), target, subtitleRankingPlaying())
}

/**
 * What AutoSync may try for [selected]: official hands it every add-on subtitle in provider order;
 * ranked, it gets only same-language, right-episode ones, best first. [selected] stays in.
 */
internal fun PlayerRuntimeController.autoSyncCandidates(candidates: List<Subtitle>, selected: Subtitle): List<Subtitle> {
    if (!SubtitleCandidateRanking.enabled) return candidates
    val ranked = SubtitleCandidateRanking.rank(candidates, selected.lang, subtitleRankingPlaying())
    return (listOf(selected) + ranked).distinctBy { it.url }
}

/**
 * The language AutoSync's matcher is given for [subtitle]: ranked candidates are already the
 * selected one's language, so they get its label (official's matcher does not read names, and a
 * subtitle labelled "Arabic" would otherwise not count as Arabic next to an "ar" one).
 */
internal fun autoSyncCandidateLanguage(subtitle: Subtitle, selected: Subtitle): String =
    if (SubtitleCandidateRanking.enabled) selected.lang else subtitle.lang
