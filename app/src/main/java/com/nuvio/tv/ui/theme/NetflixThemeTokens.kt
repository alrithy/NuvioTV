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

    // Platform sans includes Android's legally distributed Arabic fallback with shared metrics.
    val fontFamily = FontFamily.SansSerif
    val heroTitle = 36.sp
    val rowHeader = 20.sp
    val metadata = 13.sp
    val description = 16.sp
    val buttonText = 16.sp
    val profileHeadingSize = 36.sp
    val profileNameSize = 18.sp

    val safeMargin = 48.dp
    val safeVerticalMargin = 24.dp
    val heroHeightFraction = .66f
    val heroMetadataWidthFraction = .42f
    val detailHeroHeightFraction = .86f
    val detailContentWidthFraction = .50f
    val logoWidthFraction = .40f
    val logoHeight = 90.dp
    // About five 16:9 cards per 960 dp row: large enough for 10-foot reading, small enough to browse.
    val landscapeCardWidth = 184.dp
    val landscapeAspectRatio = 16f / 9f
    val cardRadius = 3.dp
    val cardGap = 8.dp
    val rowGap = 20.dp
    val rowTitleGap = 8.dp
    val episodeCardWidth = 256.dp
    val episodeCardRadius = 4.dp
    val progressHeight = 3.dp
    val buttonRadius = 4.dp
    val buttonHeight = 40.dp
    val actionGap = 12.dp
    val metadataGap = 8.dp
    val focusElevation = 12.dp
    val focusedBorderWidth = 1.5.dp
    val previewPadding = 12.dp
    val focusEdgeReserve = 8.dp
    const val rowsViewportFraction = .38f
    const val descriptionMaxLines = 3
    const val previewDescriptionMaxLines = 2
    const val previewMetadataMaxItems = 4

    // Subtle: the focused card reads by scale + elevation + contrast + a thin outline together.
    const val focusScale = 1.06f
    const val secondaryActionAlpha = .55f
    const val episodeFocusScale = 1.04f
    const val expandedScale = 1.24f
    const val focusDurationMillis = 120
    const val focusDurationMs = focusDurationMillis
    const val heroCrossfadeMs = 280
    const val previewDelayMs = 700L
    const val screenTransitionMillis = 160

    // Icon size inside keyboard/action buttons (the theme has no side navigation rail).
    val navigationIconSize = 22.dp
    val searchKeyboardWidth = 240.dp
    val searchKeySize = 32.dp
    const val searchColumns = 4
    val profileAvatarSize = 112.dp
    val profileAvatarCompactSize = 88.dp
    val profileCardWidth = 132.dp
    val profileCardCompactWidth = 108.dp
    val profileGap = 24.dp
    val profileRadius = 4.dp

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
        val height = 56.dp
        val itemHeight = 32.dp
        val itemRadius = 4.dp
        val itemHorizontalPadding = 14.dp
        val itemGap = 6.dp
        val iconSize = 20.dp
        val avatarSize = 28.dp
        val labelSize = 15.sp
        val indicatorWidth = 18.dp
        val indicatorHeight = 2.dp
        val indicatorGap = 3.dp
        val focusOutline = 2.dp
    }

    /** My Netflix hub (parity audit §6). */
    object Hub {
        val headerAvatarSize = 64.dp
        val headerGap = 16.dp
        val titleSize = 28.sp
        val subtitleSize = 14.sp
        val rowTitleSize = 18.sp
        val actionHeight = 36.dp
        val sectionGap = 24.dp
    }

    /** Factual focus callout (parity audit §5 contextual callout model). */
    object Callout {
        val height = 22.dp
        val horizontalPadding = 8.dp
        val radius = 2.dp
        val textSize = 12.sp
    }

    object Player {
        val safeMargin = NetflixThemeTokens.safeMargin
        val bottomMargin = 32.dp
        val controlSize = 44.dp
        val controlIconSize = 26.dp
        val controlsGap = 12.dp
        val topScrimHeight = 144.dp
        val bottomScrimHeight = 264.dp
        val titleGap = 8.dp
        val scrubHitHeight = 20.dp
        val scrubHeight = 3.dp
        val scrubFocusedHeight = 5.dp
        val scrubRadius = 1.dp
        val scrubThumb = 10.dp
        const val topScrimAlpha = .78f
        const val bottomScrimAlpha = .94f
        const val panelScrimAlpha = .93f
    }

    object Dialog {
        val radius = 4.dp
        val width = 560.dp
        val padding = 28.dp
        val gap = 16.dp
    }

    object State {
        val maxTextWidth = 560.dp
        val iconSize = 56.dp
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
