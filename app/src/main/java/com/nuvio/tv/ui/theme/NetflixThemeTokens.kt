package com.nuvio.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TV presentation tokens for the opt-in Netflix theme. Measurements use Android TV's canonical
 * 960 × 540 dp canvas (1920 × 1080 at 2×, 3840 × 2160 at 4×); artwork remains pixel-sized by Coil.
 * No Netflix artwork, font, logo, code or service data is included.
 */
object NetflixThemeTokens {
    val background = Color.Black
    val surface = Color(0xFF141414)
    val surfaceRaised = Color(0xFF202020)
    val surfaceMuted = Color(0xFF303030)
    val textPrimary = Color.White
    val textSecondary = Color(0xFFB3B3B3)
    val textMuted = Color(0xFF808080)
    val focus = Color.White
    val focusContent = Color.Black
    // Red conveys played progress/error rather than tinting every surface and focus state.
    val progress = Color(0xFFE50914)
    val overlay = Color(0xEF000000)

    // The one Nuvio UI font owner (Thmanyah Sans); never a Netflix-only family.
    val fontFamily: FontFamily get() = NuvioFontFamily
    val heroTitle = 42.sp
    val detailTitle = 46.sp
    val rowHeader = 19.sp
    val metadata = 15.sp
    val description = 17.sp
    val descriptionLineHeight = 25.sp
    val buttonText = 17.sp
    val profileHeadingSize = 38.sp
    val profileNameSize = 19.sp

    val safeMargin = 48.dp
    val safeVerticalMargin = 24.dp
    val heroHeightFraction = .66f
    val heroMetadataWidthFraction = .46f
    val detailHeroHeightFraction = .86f
    val detailContentWidthFraction = .50f
    val logoWidthFraction = .40f
    val logoHeight = 90.dp
    // About four 16:9 cards per 960 dp row: fewer, larger titles read better at 10 feet.
    val landscapeCardWidth = 212.dp
    val landscapeAspectRatio = 16f / 9f
    /** Portrait browse/search posters (reference: ~2:3). */
    const val posterAspectRatio = 2f / 3f
    val cardRadius = 3.dp
    val cardGap = 10.dp
    val rowGap = 18.dp
    val rowTitleGap = 8.dp
    val episodeCardWidth = 272.dp
    val episodeCardRadius = 4.dp
    val progressHeight = 3.dp
    val buttonRadius = 4.dp
    val buttonHeight = 44.dp
    val actionGap = 12.dp
    val metadataGap = 8.dp
    val focusElevation = 12.dp
    val focusedBorderWidth = 1.dp
    const val focusOutlineAlpha = .70f
    val previewPadding = 12.dp
    val focusEdgeReserve = 8.dp
    // The rows band rises into the artwork so the selected title and its row read as one surface.
    const val rowsViewportFraction = .45f
    const val descriptionMaxLines = 3
    const val previewDescriptionMaxLines = 2
    const val previewMetadataMaxItems = 4

    // Subtle: the focused card reads by scale + elevation + contrast + a thin outline together.
    const val focusScale = 1.06f
    const val secondaryActionAlpha = .55f
    const val secondaryActionFillAlpha = .22f
    const val episodeFocusScale = 1.04f
    const val expandedScale = 1.24f
    const val focusDurationMillis = 120
    const val focusDurationMs = focusDurationMillis
    const val heroCrossfadeMs = 280
    const val previewDelayMs = 700L
    const val screenTransitionMillis = 160

    // Icon size inside keyboard/action buttons (the theme has no side navigation rail).
    val navigationIconSize = 22.dp
    /** Measured reference (PR #100 maintainer packet): 6 × 31 dp keys + 2 dp gaps ≈ 196 dp. */
    val searchKeyboardWidth = 196.dp
    val searchKeySize = 31.dp
    val searchKeyGap = 2.dp
    val searchKeyGlyph = 15.sp
    val searchQuerySize = 17.sp
    /** Reference Search: 4 result columns of ~150×210 dp posters. */
    const val searchColumns = 4
    val searchPosterWidth = 150.dp
    val searchPosterHeight = 210.dp
    val searchResultGap = 12.dp
    val profileAvatarSize = 136.dp
    val profileAvatarCompactSize = 104.dp
    val profileCardWidth = 160.dp
    val profileCardCompactWidth = 128.dp
    val profileGap = 32.dp
    val profileRadius = 6.dp

    val cardShape = RoundedCornerShape(cardRadius)
    val buttonShape = RoundedCornerShape(buttonRadius)
    val heroBottomStops = arrayOf(
        0f to Color.Transparent,
        .42f to background.copy(alpha = .08f),
        .72f to background.copy(alpha = .70f),
        1f to background
    )
    /** Reads logical direction at the caller; artwork itself must never be mirrored. */
    fun heroSideGradient(rtl: Boolean): Brush {
        val colors = listOf(background, background.copy(alpha = .86f), background.copy(alpha = .20f), Color.Transparent)
        return Brush.horizontalGradient(if (rtl) colors.reversed() else colors)
    }

    /** Current-generation top navigation (parity audit §4); there is no Netflix-theme side rail. */
    object TopNav {
        val height = 64.dp
        val topInset = 6.dp
        val itemHeight = 36.dp
        val itemRadius = 18.dp
        val itemHorizontalPadding = 16.dp
        val itemGap = 4.dp
        val iconSize = 22.dp
        val avatarSize = 32.dp
        val labelSize = 17.sp
        val indicatorWidth = 20.dp
        val indicatorHeight = 3.dp
        val indicatorGap = 4.dp
        val focusOutline = 2.dp
        /** Focus is a soft translucent pill, never a solid white block. */
        const val focusFillAlpha = .20f
        /** Selected destination: a light grey pill (reference), distinct from the white focused pill. */
        const val selectedFillAlpha = .26f
        val brandHeight = 22.dp
        /** Compact square Nuvio mark in the bar (measured reference: small brand footprint). */
        val brandMarkSize = 30.dp
        const val idleLabelAlpha = .72f
        const val settingsAlpha = .55f
    }

    /** Home per the maintainer reference (audit §0): hero card, category strip, inline-expanding rows. */
    object Home {
        val heroHeight = 400.dp
        val heroRadius = 12.dp
        /** Content width of the hero card on the 960 dp canvas (safe margins excluded). */
        val heroRequestWidth = 880.dp
        val heroPadding = 28.dp
        val heroTitleSize = 34.sp
        val heroSynopsisSize = 15.sp
        /** Measured reference: synopsis up to ~2 lines when present, never dominant. */
        const val heroSynopsisLines = 2
        const val heroTextWidthFraction = .55f
        const val heroSideScrimAlpha = .85f
        const val heroBottomScrimAlpha = .80f
        /** Measured reference: hero card ≈ 880×400 dp (40 dp side inset on the 960 dp canvas). */
        val heroInset = 40.dp
        val heroLogoHeight = 88.dp
        val heroLogoMaxWidth = 320.dp
        val sectionGap = 20.dp
        val bottomPadding = 200.dp
        /** Category tiles size to their label, bounded like the reference (~120–185 dp). */
        val categoryMinWidth = 120.dp
        val categoryMaxWidth = 185.dp
        val categoryPadding = 20.dp
        val categoryHeight = 88.dp
        val categoryRadius = 12.dp
        val categoryGap = 6.dp
        val categoryTextSize = 17.sp
        /** Measured reference: idle posters ≈160×250 dp, focused card ≈440×250 dp (2.75×). */
        val rowCardHeight = 250.dp
        val rowCardIdleWidth = 160.dp
        val rowCardExpandedWidth = 440.dp
        val cardGap = 6.dp
        val cardRadius = 6.dp
        val cardTitleSize = 17.sp
        val focusOutline = 2.dp
        const val expandMillis = 200
        /** Neighbour peek kept on each side of the focus comfort zone, as a fraction of an idle poster. */
        const val comfortPeekFraction = 0.5f
        val factsMaxWidth = 560.dp
        val factsSynopsisSize = 15.sp
        val factsSynopsisLineHeight = 21.sp
        const val restoreFrames = 20
    }

    /** My Netflix hub (parity audit §6). */
    object Hub {
        val headerAvatarSize = 52.dp
        val headerGap = 16.dp
        val headerArtHeight = 220.dp
        val titleSize = 30.sp
        val subtitleSize = 15.sp
        val rowTitleSize = 19.sp
        val actionHeight = 32.dp
        val actionTextSize = 14.sp
        val sectionGap = 22.dp
        val emptyIconSize = 64.dp
        const val headerArtAlpha = .55f
    }

    /** Factual focus callout (parity audit §5 contextual callout model). */
    object Callout {
        val accentWidth = 3.dp
        val accentHeight = 16.dp
        val gap = 8.dp
        val textSize = 14.sp
        val iconSize = 16.dp
    }

    object Player {
        val safeMargin = NetflixThemeTokens.safeMargin
        val bottomMargin = 32.dp
        val controlSize = 48.dp
        val controlIconSize = 28.dp
        val controlsGap = 14.dp
        val titleSize = 30.sp
        val episodeSize = 17.sp
        val timeSize = 16.sp
        val topScrimHeight = 144.dp
        val bottomScrimHeight = 264.dp
        val titleGap = 8.dp
        val scrubHitHeight = 20.dp
        val scrubHeight = 4.dp
        val scrubFocusedHeight = 6.dp
        val scrubRadius = 2.dp
        val scrubThumb = 14.dp
        const val topScrimAlpha = .78f
        const val bottomScrimAlpha = .94f
        const val panelScrimAlpha = .93f
    }

    object Dialog {
        val radius = 6.dp
        val width = 600.dp
        val padding = 36.dp
        val gap = 20.dp
        val titleSize = 26.sp
        val bodySize = 17.sp
        val buttonMinWidth = 220.dp
        val buttonHeight = 44.dp
    }

    object State {
        val maxTextWidth = 560.dp
        val iconSize = 64.dp
        val titleSize = 28.sp
        val bodySize = 17.sp
        val gap = 16.dp
        val actionMinWidth = 200.dp
        val skeletonRadius = cardRadius
    }

    object Detail {
        val episodeInfoHeight = 104.dp
        val episodeContentPadding = 8.dp
        const val episodeTitleMaxLines = 1
        const val episodeDescriptionMaxLines = 3
        const val episodeDescriptionWithRatingMaxLines = 2
    }
}
