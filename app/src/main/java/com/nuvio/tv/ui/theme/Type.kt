package com.nuvio.tv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.AppFont

/**
 * The one Nuvio UI font owner: Thmanyah Sans, the official typeface for every theme (Netflix included)
 * and both Arabic and English. Each declared weight is a real licensed file; there is no SemiBold,
 * so 600 requests resolve to the Bold file (never synthetic bold, see [nuvioFontSynthesis]).
 * Glyphs the family lacks (arrows, ★, Persian letters) fall back to the platform Sans per glyph.
 *
 * The files are private build input (docs/PRIVATE_FONTS.md). Builds without them, such as untrusted
 * fork PRs, use the platform Sans; [ThmanyahFontResources.EMBEDDED] and BuildConfig.THMANYAH_EMBEDDED
 * say which one shipped. Resource fonts are loaded once by Compose's font cache, not per composition.
 */
val NuvioFontFamily: FontFamily = if (ThmanyahFontResources.EMBEDDED) {
    FontFamily(
        Font(ThmanyahFontResources.light(), FontWeight.Light),
        Font(ThmanyahFontResources.regular(), FontWeight.Normal),
        Font(ThmanyahFontResources.medium(), FontWeight.Medium),
        Font(ThmanyahFontResources.bold(), FontWeight.Bold),
        Font(ThmanyahFontResources.black(), FontWeight.Black)
    )
} else {
    FontFamily.SansSerif
}

/**
 * Semantic weights for the five real Thmanyah Sans files. Light: de-emphasised secondary text;
 * Regular: body, descriptions, metadata; Medium: navigation, keys, labels, list items, secondary
 * buttons; Bold: section headings, primary buttons, title emphasis; Black: rare display headings.
 */
object NuvioFontWeights {
    val Secondary = FontWeight.Light
    val Body = FontWeight.Normal
    val Label = FontWeight.Medium
    val Heading = FontWeight.Bold
    val Display = FontWeight.Black
}

/** Thmanyah Sans ships real files for every weight the UI uses; never fake a heavier one. */
fun nuvioFontSynthesis(fontFamily: FontFamily): FontSynthesis? =
    if (fontFamily === NuvioFontFamily && ThmanyahFontResources.EMBEDDED) FontSynthesis.None else null

val DMSansFamily = FontFamily(
    Font(R.font.dm_sans_variable, FontWeight.Normal),
    Font(R.font.dm_sans_variable, FontWeight.Medium),
    Font(R.font.dm_sans_variable, FontWeight.SemiBold),
    Font(R.font.dm_sans_variable, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold)
)

val OpenSansFamily = FontFamily(
    Font(R.font.opensans_variable, FontWeight.Normal),
    Font(R.font.opensans_variable, FontWeight.Medium),
    Font(R.font.opensans_variable, FontWeight.SemiBold),
    Font(R.font.opensans_variable, FontWeight.Bold)
)

fun getFontFamily(appFont: AppFont): FontFamily = when (appFont) {
    AppFont.THMANYAH_SANS -> NuvioFontFamily
    AppFont.INTER -> InterFamily
    AppFont.DM_SANS -> DMSansFamily
    AppFont.OPEN_SANS -> OpenSansFamily
}

@Immutable
data class NuvioTextStyleTokens(
    val display: TextStyle,
    val displayCompact: TextStyle,
    val headline: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyCompact: TextStyle,
    val metadata: TextStyle,
    val badge: TextStyle,
    val button: TextStyle,
    val tab: TextStyle,
    val nav: TextStyle,
    val playerControl: TextStyle
)

@OptIn(ExperimentalTvMaterial3Api::class)
fun buildNuvioTypography(fontFamily: FontFamily): Typography = buildBaseTypography(fontFamily).let { base ->
    val synthesis = nuvioFontSynthesis(fontFamily) ?: return@let base
    base.copy(
        displayLarge = base.displayLarge.copy(fontSynthesis = synthesis),
        displayMedium = base.displayMedium.copy(fontSynthesis = synthesis),
        headlineLarge = base.headlineLarge.copy(fontSynthesis = synthesis),
        headlineMedium = base.headlineMedium.copy(fontSynthesis = synthesis),
        titleLarge = base.titleLarge.copy(fontSynthesis = synthesis),
        titleMedium = base.titleMedium.copy(fontSynthesis = synthesis),
        titleSmall = base.titleSmall.copy(fontSynthesis = synthesis),
        bodyLarge = base.bodyLarge.copy(fontSynthesis = synthesis),
        bodyMedium = base.bodyMedium.copy(fontSynthesis = synthesis),
        bodySmall = base.bodySmall.copy(fontSynthesis = synthesis),
        labelLarge = base.labelLarge.copy(fontSynthesis = synthesis),
        labelMedium = base.labelMedium.copy(fontSynthesis = synthesis),
        labelSmall = base.labelSmall.copy(fontSynthesis = synthesis)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
private fun buildBaseTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
val NuvioTypography = buildNuvioTypography(NuvioFontFamily)

@OptIn(ExperimentalTvMaterial3Api::class)
fun buildNetflixTypography(): Typography {
    val base = buildNuvioTypography(NetflixThemeTokens.fontFamily)
    return base.copy(
        displayLarge = base.displayLarge.copy(fontSize = NetflixThemeTokens.heroTitle, lineHeight = 42.sp),
        displayMedium = base.displayMedium.copy(fontSize = NetflixThemeTokens.heroTitle, lineHeight = 42.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = NetflixThemeTokens.rowHeader, lineHeight = 26.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = NetflixThemeTokens.description, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontSize = NetflixThemeTokens.metadata, letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(fontSize = NetflixThemeTokens.buttonText, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
fun buildNuvioTextStyles(typography: Typography): NuvioTextStyleTokens = NuvioTextStyleTokens(
    display = typography.displayLarge,
    displayCompact = typography.displayMedium,
    headline = typography.headlineLarge,
    sectionTitle = typography.headlineMedium,
    cardTitle = typography.titleMedium,
    body = typography.bodyLarge,
    bodyCompact = typography.bodyMedium,
    metadata = typography.labelMedium,
    badge = typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    ),
    button = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    tab = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    nav = typography.titleMedium,
    playerControl = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
)

@OptIn(ExperimentalTvMaterial3Api::class)
val NuvioTextStyles = buildNuvioTextStyles(NuvioTypography)
