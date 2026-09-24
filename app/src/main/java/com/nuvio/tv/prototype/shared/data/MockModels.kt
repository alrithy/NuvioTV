package com.nuvio.tv.prototype.shared.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.prototype.shared.Bi
import java.util.Locale

/** Locale-stable number formatting (Arabic locales would otherwise switch digits mid-string). */
fun fmt(value: Double, decimals: Int): String = String.format(Locale.US, "%.${decimals}f", value)

enum class MediaType { MOVIE, SERIES }

/** Scene archetype used by the procedural illustrated artwork. */
enum class ArtMotif { DUNES, DESERT_NIGHT, SPACE, CITY_NIGHT, NEON_RAIN, FIRE, OCEAN, STORM, MOUNTAIN, FOREST, INTERIOR, CORRIDOR, SKY, ARCHES }

/**
 * Colors "extracted" from a title's key art. Real apps would compute this from the image;
 * the prototype ships hand-tuned palettes so adaptive-color concepts are deterministic.
 */
@Immutable
data class ArtPalette(
    val sky: Color,
    val horizon: Color,
    val mid: Color,
    val ground: Color,
    val accent: Color,
    /** Readable tint for UI accents derived from the art. */
    val ui: Color,
)

@Immutable
data class Tech(
    val uhd: Boolean = true,
    val dolbyVision: Boolean = false,
    val hdr10: Boolean = true,
    val atmos: Boolean = false,
    val imax: Boolean = false,
) {
    fun labels(): List<String> = buildList {
        if (uhd) add("4K")
        if (dolbyVision) add("Dolby Vision")
        if (hdr10) add("HDR10")
        if (atmos) add("Dolby Atmos")
        if (imax) add("IMAX Enhanced")
    }
}

@Immutable
data class CastMember(val name: Bi, val role: Bi)

@Immutable
data class Episode(
    val season: Int,
    val number: Int,
    val title: Bi,
    val synopsis: Bi,
    val runtimeMin: Int,
    val progress: Float? = null,
    val watched: Boolean = false,
    val isNew: Boolean = false,
    val airDate: Bi? = null,
)

@Immutable
data class Season(val number: Int, val year: Int, val episodes: List<Episode>)

@Immutable
data class Chapter(val title: Bi, val startFraction: Float)

@Immutable
data class ProtoTitle(
    val id: String,
    val imdb: String?,
    val type: MediaType,
    val title: Bi,
    val year: Int,
    val runtimeMin: Int,
    val genres: List<Bi>,
    val rating: Double,
    val cert: String,
    val tagline: Bi,
    val synopsis: Bi,
    val director: Bi,
    val cast: List<CastMember>,
    val tech: Tech,
    val palette: ArtPalette,
    val motif: ArtMotif,
    val seed: Int,
    val country: Bi,
    val progress: Float? = null,
    val remainingMin: Int? = null,
    val seasons: List<Season> = emptyList(),
    val resumeSeason: Int? = null,
    val resumeEpisode: Int? = null,
    val award: Bi? = null,
    val quote: Bi? = null,
    val quoteSource: String? = null,
    val chapters: List<Chapter> = emptyList(),
) {
    val isSeries: Boolean get() = type == MediaType.SERIES

    val posterUrl: String? get() = imdb?.let { "https://images.metahub.space/poster/medium/$it/img" }
    val backdropUrl: String? get() = imdb?.let { "https://images.metahub.space/background/medium/$it/img" }
    val logoUrl: String? get() = imdb?.let { "https://images.metahub.space/logo/medium/$it/img" }
    fun stillUrl(season: Int, episode: Int): String? =
        imdb?.let { "https://episodes.metahub.space/$it/$season/$episode/w780.jpg" }

    val resume: Episode?
        get() {
            val s = resumeSeason ?: return null
            val e = resumeEpisode ?: return null
            return seasons.firstOrNull { it.number == s }?.episodes?.firstOrNull { it.number == e }
        }

    val primaryGenre: Bi get() = genres.first()
    val totalEpisodes: Int get() = seasons.sumOf { it.episodes.size }
}

@Immutable
data class ProtoProfile(
    val id: String,
    val name: Bi,
    val color: Color,
    val initial: String,
    val kids: Boolean = false,
)

@Immutable
data class Collection(
    val id: String,
    val title: Bi,
    val subtitle: Bi,
    val items: List<ProtoTitle>,
)

// ------------------------------------------------------------------------------------------------
// Streams
// ------------------------------------------------------------------------------------------------

enum class Resolution(val label: String, val rank: Int) { R2160("2160p", 3), R1080("1080p", 2), R720("720p", 1) }

enum class SourceKind(val label: String, val rank: Int) {
    REMUX("REMUX", 5), BLURAY("BluRay", 4), WEB_DL("WEB-DL", 3), WEBRIP("WEBRip", 2)
}

enum class Hdr(val label: String) { DV("Dolby Vision"), HDR10P("HDR10+"), HDR10("HDR10"), SDR("SDR") }

enum class AudioFormat(val label: String, val short: String, val rank: Int) {
    TRUEHD_ATMOS("TrueHD Atmos", "Atmos", 6),
    DDP_ATMOS("Dolby Digital+ Atmos", "DD+ Atmos", 5),
    DTSHD_MA("DTS-HD MA", "DTS-HD", 4),
    DDP("Dolby Digital+", "DD+", 3),
    DD("Dolby Digital", "DD", 2),
    AAC("AAC", "AAC", 1),
}

enum class CacheState { CACHED, UNCACHED, DIRECT }

@Immutable
data class ProtoStream(
    val id: String,
    val filename: String,
    val addon: String,
    val service: String,
    val cache: CacheState,
    val resolution: Resolution,
    val source: SourceKind,
    val hdr: List<Hdr>,
    val codec: String,
    val audio: AudioFormat,
    val channels: String,
    val sizeGb: Double,
    val bitrateMbps: Double,
    val seeders: Int?,
    val audioLangs: List<String>,
    val subtitleLangs: List<String>,
    val group: String,
    val startSeconds: Double,
) {
    val isCached: Boolean get() = cache != CacheState.UNCACHED
    val hasDv: Boolean get() = Hdr.DV in hdr
    val hasHdr: Boolean get() = hdr.any { it != Hdr.SDR }
    val hasArabicSubs: Boolean get() = "AR" in subtitleLangs
    val hasArabicAudio: Boolean get() = "AR" in audioLangs
    val isAtmos: Boolean get() = audio == AudioFormat.TRUEHD_ATMOS || audio == AudioFormat.DDP_ATMOS

    val sizeLabel: String get() = if (sizeGb >= 10) "${sizeGb.toInt()} GB" else "${fmt(sizeGb, 1)} GB"
    val bitrateLabel: String get() = "${fmt(bitrateMbps, 0)} Mbps"
    val hdrLabel: String get() = hdr.filter { it != Hdr.SDR }.joinToString(" · ") { it.label }.ifEmpty { "SDR" }
    val audioLabel: String get() = "${audio.label} $channels"
    val startLabel: String get() = if (startSeconds < 60) "~${fmt(startSeconds, 0)}s" else "~${(startSeconds / 60).toInt().coerceAtLeast(1)} min"
}

@Immutable
data class SubtitleTrack(
    val id: String,
    val language: Bi,
    val code: String,
    val variant: Bi? = null,
    val source: String,
    val format: String,
    val isDefault: Boolean = false,
)

@Immutable
data class AudioTrack(
    val id: String,
    val language: Bi,
    val code: String,
    val format: String,
    val channels: String,
    val note: Bi? = null,
    val isDefault: Boolean = false,
)
