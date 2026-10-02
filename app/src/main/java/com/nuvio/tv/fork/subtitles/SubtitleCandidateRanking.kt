package com.nuvio.tv.fork.subtitles

import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.ui.screens.player.autosync.SubtitleLanguageMatching
import java.net.URLDecoder

/**
 * Which add-on subtitle to pick, and which ones AutoSync may try, for the playing file (G14 device
 * finding, features 82, 84, 94; D067). Official takes the first subtitle in provider order whose
 * language code matches and hands AutoSync every subtitle in that order, so a subtitle for another
 * release, another episode, or labelled with a language name ("Arabic", "العربية") instead of a code
 * is picked or skipped by position alone.
 *
 * Here, in this order:
 * 1. language: the subtitle's language, codes and names alike, must be the target's; others are out;
 * 2. episode: a subtitle whose name says another season / episode than the one playing is out;
 * 3. same file: the stream's own subtitle (same release by definition) or one carrying the file's
 *    OpenSubtitles hash comes first;
 * 4. one whose name says this very episode before one that says none;
 * 5. release: the closer its name is to the playing file's (release group, source, resolution);
 * 6. provider order breaks ties, so with nothing to tell them apart official's order stands.
 *
 * Names come from the subtitle's id and the file name in its URL, the only places add-ons put them.
 * Nothing is downloaded here; timing fit and confidence stay with official AutoSync.
 */
object SubtitleCandidateRanking {

    val enabled: Boolean get() = SubtitleIntelligence.enabled

    /** What is playing. */
    data class Playing(
        val filename: String? = null,
        val streamName: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val videoHash: String? = null,
    )

    /** [candidates] in [targetLanguage] that fit [playing], best first. Never adds a subtitle. */
    fun rank(candidates: List<Subtitle>, targetLanguage: String, playing: Playing): List<Subtitle> {
        val target = canonicalLanguage(targetLanguage)
        if (target.isBlank()) return emptyList()
        val release = ReleaseTokens.of(listOfNotNull(playing.filename, playing.streamName).joinToString(" "))
        val hash = playing.videoHash?.trim()?.lowercase()?.takeIf { it.length >= 8 }
        return candidates.asSequence()
            .withIndex()
            .filter { (_, subtitle) -> subtitle.url.isNotBlank() && languageMatches(subtitle.lang, target, targetLanguage) }
            .map { (index, subtitle) -> Scored(subtitle, index, describe(subtitle)) }
            .map { scored -> scored.copy(episode = episodeOf(scored.text, playing)) }
            .filterNot { it.episode == EpisodeMatch.OTHER }
            .map { scored ->
                val sameFile = scored.subtitle.isStreamProvided || (hash != null && hash in scored.text)
                scored.copy(sameFile = sameFile, releaseScore = release.similarity(ReleaseTokens.of(scored.text)))
            }
            .sortedWith(
                compareByDescending<Scored> { it.sameFile }
                    .thenByDescending { it.episode == EpisodeMatch.THIS }
                    .thenByDescending { it.releaseScore }
                    .thenBy { it.index },
            )
            .map { it.subtitle }
            .toList()
    }

    /** Same language by code or name, or as official matches it (a regional target takes its variants). */
    private fun languageMatches(language: String, canonicalTarget: String, rawTarget: String): Boolean =
        canonicalLanguage(language) == canonicalTarget || SubtitleLanguageMatching.matchesLanguageCode(language, rawTarget)

    /** The subtitle to pick for [targetLanguage], or null when none fits. */
    fun best(candidates: List<Subtitle>, targetLanguage: String, playing: Playing): Subtitle? =
        rank(candidates, targetLanguage, playing).firstOrNull()

    /** A language code for codes and names alike: "ara", "ar-SA", "Arabic", "العربية" are all "ar". */
    fun canonicalLanguage(language: String?): String {
        val raw = language?.trim()?.lowercase().orEmpty()
        if (raw.isBlank()) return ""
        // Official's codes first (it also reads Portuguese / Spanish variants out of names).
        val code = SubtitleLanguageMatching.normalizeLanguageCode(raw)
        if (CODE.matches(code)) {
            // Regional tags: "ar-sa" and "ar-eg" are Arabic; pt-br and es-419 stay distinct, as official keeps them.
            return if (code.length > 3 && code[2] == '-' && code.take(2) !in REGIONAL_DISTINCT) code.take(2) else code
        }
        // A name: "Arabic", "العربية", or "Arabic (SDH)", "arabic.forced", where the first word names it.
        val firstWord = raw.split(' ', '(', '.', '-', '_', '[', ',').firstOrNull { it.isNotBlank() }.orEmpty()
        return NAMES[raw] ?: NAMES[firstWord] ?: code
    }

    private val CODE = Regex("[a-z]{2,3}(-[a-z0-9]{2,4})?")

    private data class Scored(
        val subtitle: Subtitle,
        val index: Int,
        val text: String,
        val sameFile: Boolean = false,
        val releaseScore: Int = 0,
        val episode: EpisodeMatch = EpisodeMatch.UNSTATED,
    )

    internal enum class EpisodeMatch { THIS, OTHER, UNSTATED }

    /** The id and the URL's file name, decoded and lowercased: what add-ons name a subtitle by. */
    internal fun describe(subtitle: Subtitle): String {
        val path = subtitle.url.substringBefore('?').substringBefore('#')
        val file = path.trimEnd('/').substringAfterLast('/')
        val decoded = runCatching { URLDecoder.decode(file, "UTF-8") }.getOrDefault(file)
        return "${subtitle.id} $decoded".lowercase()
    }

    /** Whether the name states the season / episode playing, another one, or none (films: always none). */
    internal fun episodeOf(text: String, playing: Playing): EpisodeMatch {
        val season = playing.season ?: return EpisodeMatch.UNSTATED
        val episode = playing.episode ?: return EpisodeMatch.UNSTATED
        val stated = EPISODE_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
        } ?: return EpisodeMatch.UNSTATED
        return if (stated == season to episode) EpisodeMatch.THIS else EpisodeMatch.OTHER
    }

    /** Release traits two names can share. */
    internal class ReleaseTokens(
        val group: String?,
        val source: String?,
        val resolution: String?,
        val words: Set<String>,
    ) {
        /** Higher is closer: group 6, source family 3 (a different one −2), resolution 1, shared tags 1 each up to 3. */
        fun similarity(other: ReleaseTokens): Int {
            var score = 0
            if (group != null && group == other.group) score += 6
            if (source != null && other.source != null) score += if (source == other.source) 3 else -2
            if (resolution != null && resolution == other.resolution) score += 1
            score += (words intersect other.words).size.coerceAtMost(3)
            return score
        }

        companion object {
            fun of(name: String): ReleaseTokens {
                val lower = name.lowercase()
                val stem = lower.substringBeforeLast('.', lower).takeIf { lower.substringAfterLast('.') in EXTENSIONS } ?: lower
                val tokens = stem.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
                val group = Regex("-([a-z0-9]{2,})\\s*$").find(stem)?.groupValues?.get(1)
                    ?.takeUnless { it in SOURCE_FAMILY || it in RESOLUTIONS || it in TAGS || it == "dl" }
                val source = tokens.firstNotNullOfOrNull { SOURCE_FAMILY[it] }
                    ?: SOURCE_FAMILY.entries.firstOrNull { (token, _) -> token.length > 3 && token in stem }?.value
                val resolution = tokens.firstNotNullOfOrNull { RESOLUTIONS[it] }
                return ReleaseTokens(group, source, resolution, tokens.filterTo(HashSet()) { it in TAGS })
            }
        }
    }

    private val EPISODE_PATTERNS = listOf(
        Regex("(?<![a-z0-9])s(\\d{1,2})[ ._-]?e(\\d{1,3})(?![0-9])"),
        Regex("(?<![a-z0-9])(\\d{1,2})x(\\d{2,3})(?![0-9])"),
    )

    private val EXTENSIONS = setOf("srt", "vtt", "ass", "ssa", "sub", "txt", "zip", "gz", "mkv", "mp4", "avi", "m4v", "ts")

    private val SOURCE_FAMILY = mapOf(
        "remux" to "bluray", "bluray" to "bluray", "blu" to "bluray", "bdrip" to "bluray", "brrip" to "bluray", "bdremux" to "bluray",
        "web" to "web", "webdl" to "web", "webrip" to "web", "web-dl" to "web", "amzn" to "web", "nf" to "web", "dsnp" to "web",
        "hmax" to "web", "atvp" to "web", "hulu" to "web", "max" to "web",
        "hdtv" to "tv", "pdtv" to "tv",
        "dvdrip" to "dvd", "dvd" to "dvd",
    )
    private val RESOLUTIONS = mapOf("2160p" to "2160", "4k" to "2160", "uhd" to "2160", "1080p" to "1080", "1080i" to "1080", "720p" to "720", "480p" to "480")
    private val TAGS = setOf("proper", "repack", "extended", "uncut", "directors", "imax", "hdr", "dv", "x264", "x265", "h264", "h265", "hevc", "10bit", "atmos")

    private val REGIONAL_DISTINCT = setOf("pt", "es", "zh")

    private val NAMES = mapOf(
        "arabic" to "ar", "العربية" to "ar", "عربي" to "ar", "عربى" to "ar", "arabe" to "ar", "arab" to "ar",
        "english" to "en", "anglais" to "en", "inglés" to "en", "ingles" to "en", "الإنجليزية" to "en", "انجليزي" to "en",
        "french" to "fr", "français" to "fr", "francais" to "fr", "الفرنسية" to "fr",
        "german" to "de", "deutsch" to "de", "spanish" to "es", "español" to "es", "espanol" to "es",
        "italian" to "it", "italiano" to "it", "turkish" to "tr", "türkçe" to "tr", "turkce" to "tr", "التركية" to "tr",
        "persian" to "fa", "farsi" to "fa", "فارسی" to "fa", "urdu" to "ur", "hindi" to "hi", "hebrew" to "he",
        "russian" to "ru", "dutch" to "nl", "polish" to "pl", "swedish" to "sv", "greek" to "el", "romanian" to "ro",
        "indonesian" to "id", "malay" to "ms", "japanese" to "ja", "korean" to "ko", "chinese" to "zh",
        "vietnamese" to "vi", "thai" to "th", "czech" to "cs", "hungarian" to "hu", "ukrainian" to "uk",
        "portuguese" to "pt", "danish" to "da", "norwegian" to "no", "finnish" to "fi", "bengali" to "bn",
    )
}
