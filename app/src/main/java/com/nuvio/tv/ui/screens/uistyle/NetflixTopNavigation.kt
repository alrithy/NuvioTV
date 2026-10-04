package com.nuvio.tv.ui.screens.uistyle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.R
import com.nuvio.tv.fork.uistyle.TopChromeBack
import com.nuvio.tv.fork.uistyle.UiStyleRules
import com.nuvio.tv.navigateToDrawerRoute
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.theme.NetflixThemeTokens
import com.nuvio.tv.ui.theme.withNuvioDescenderRoom

/*
 * NETFLIX_THEME top navigation (docs/NETFLIX_TV_2026_PARITY_AUDIT.md §4). The current Netflix TV
 * generation puts its shortcuts in a menu across the top; the old left rail is not used by this theme.
 * Presentation only: destinations, Back and route changes are the same owners the G12 top menu uses
 * (`navigateToDrawerRoute`, `UiStyleRules.back`, the one NuvioNavHost). Other themes never reach this.
 */

/** A Netflix-theme destination: text tabs, plus the icon-only Search and Settings entries. */
internal data class NetflixNavEntry(
    val route: String,
    val label: String,
    val kind: Kind,
) {
    enum class Kind { SEARCH, TAB, SETTINGS }
}

/**
 * The order the bar presents: Search first (one D-pad step from the profile), then the content
 * shortcuts, with Settings kept as a low-emphasis trailing icon rather than a content tab.
 */
internal fun netflixNavEntries(
    drawerItems: List<DrawerItem>,
    searchRoute: String,
    settingsRoute: String,
): List<NetflixNavEntry> = buildList {
    drawerItems.firstOrNull { it.route == searchRoute }?.let { add(NetflixNavEntry(it.route, it.label, NetflixNavEntry.Kind.SEARCH)) }
    drawerItems.filter { it.route != searchRoute && it.route != settingsRoute }
        .forEach { add(NetflixNavEntry(it.route, it.label, NetflixNavEntry.Kind.TAB)) }
    drawerItems.firstOrNull { it.route == settingsRoute }?.let { add(NetflixNavEntry(it.route, it.label, NetflixNavEntry.Kind.SETTINGS)) }
}

@Composable
internal fun NetflixTopNavigationScaffold(
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    entries: List<NetflixNavEntry>,
    selectedRoute: String?,
    profile: TopMenuProfile,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit,
) {
    val onRootRoute = currentRoute in rootRoutes
    val contentFocusRequester = remember { FocusRequester() }
    val selectedRequester = remember { FocusRequester() }
    var menuFocused by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) { longPressBackHeld.value = false }

    // Same contract as the G12 top menu: first Back on a root screen focuses the bar, the next one
    // leaves through the app's own exit path. Screens that consume Back themselves keep priority.
    BackHandler(enabled = onRootRoute) {
        when (UiStyleRules.back(onRootRoute, menuFocused, longPressBackHeld.value)) {
            TopChromeBack.FOCUS_CHROME -> runCatching { selectedRequester.requestFocus() }
            TopChromeBack.EXIT_APP -> onExitApp()
            TopChromeBack.IGNORE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixThemeTokens.background),
    ) {
        if (onRootRoute) {
            NetflixTopNavigationBar(
                entries = entries,
                selectedRoute = selectedRoute ?: currentRoute,
                selectedRequester = selectedRequester,
                profile = profile,
                onFocusChanged = { menuFocused = it },
                onNavigate = { route ->
                    onNavigate(route)
                    navigateToDrawerRoute(navController, currentRoute, route)
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .longPressBackToChrome(onRootRoute, longPressBackHeld) {
                    runCatching { selectedRequester.requestFocus() }
                },
        ) {
            // The bar owning focus is what the old expanded sidebar meant to Home: no dwell preview
            // or trailer may start underneath it.
            CompositionLocalProvider(
                LocalSidebarExpanded provides menuFocused,
                LocalContentFocusRequester provides contentFocusRequester,
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    hideBuiltInHeaders = false,
                )
            }
        }
    }
}

@Composable
internal fun NetflixTopNavigationBar(
    entries: List<NetflixNavEntry>,
    selectedRoute: String?,
    selectedRequester: FocusRequester,
    profile: TopMenuProfile?,
    onFocusChanged: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = NetflixThemeTokens.TopNav
    // Settings is not a primary anchor in the reference bar (audit §0); it stays reachable through
    // My Netflix / the profile path. Only content destinations and Search are drawn.
    val shown = entries.filter { it.kind != NetflixNavEntry.Kind.SETTINGS }
    val focusRoute = shown.firstOrNull { it.route == selectedRoute }?.route
        ?: shown.firstOrNull { it.kind == NetflixNavEntry.Kind.TAB }?.route
    val readingDirection = LocalLayoutDirection.current
    // Physical anchors: the brand mark stays at the far LEFT and the profile at the far RIGHT in both
    // English and Arabic; only the destination labels follow the reading direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(tokens.height)
                .background(Brush.verticalGradient(listOf(NetflixThemeTokens.background, NetflixThemeTokens.background.copy(alpha = .0f))))
                .padding(start = NetflixThemeTokens.safeMargin, end = NetflixThemeTokens.safeMargin, top = tokens.topInset)
                .onFocusChanged { onFocusChanged(it.hasFocus) }
                .testTag("netflix_top_nav"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                // Compact Nuvio-owned mark: the reference brand footprint is small (never the Netflix N).
                painter = painterResource(R.drawable.app_logo_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(tokens.brandMarkSize).testTag("netflix_top_nav_brand"),
            )
            Spacer(Modifier.weight(1f))
            CompositionLocalProvider(LocalLayoutDirection provides readingDirection) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.itemGap),
                ) {
                    shown.forEach { entry ->
                        val requester = if (entry.route == focusRoute) selectedRequester else null
                        when (entry.kind) {
                            NetflixNavEntry.Kind.SEARCH -> NetflixTopNavIcon(entry, Icons.Default.Search, entry.route == selectedRoute, requester, onNavigate)
                            else -> NetflixTopNavTab(entry, entry.route == selectedRoute, requester, onNavigate)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (profile != null) {
                CompositionLocalProvider(LocalLayoutDirection provides readingDirection) { NetflixTopNavProfile(profile) }
            } else {
                Spacer(Modifier.width(tokens.avatarSize))
            }
        }
    }
}

/** Selected: a light rounded pill with bold text. Focused: a solid white pill. No underline. */
@Composable
private fun navPillColors(selected: Boolean, focused: Boolean): Pair<Color, Color> {
    val tokens = NetflixThemeTokens.TopNav
    val background = when {
        focused -> NetflixThemeTokens.focus
        selected -> NetflixThemeTokens.focus.copy(alpha = tokens.selectedFillAlpha)
        else -> Color.Transparent
    }
    val content = when {
        focused -> NetflixThemeTokens.focusContent
        selected -> NetflixThemeTokens.textPrimary
        else -> NetflixThemeTokens.textPrimary.copy(alpha = tokens.idleLabelAlpha)
    }
    val animated by animateColorAsState(background, tween(NetflixThemeTokens.focusDurationMillis), label = "netflixTopNavPill")
    return animated to content
}

@Composable
private fun NetflixTopNavTab(
    entry: NetflixNavEntry,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onNavigate: (String) -> Unit,
) {
    val tokens = NetflixThemeTokens.TopNav
    var focused by remember { mutableStateOf(false) }
    val (background, content) = navPillColors(selected, focused)
    Box(
        modifier = Modifier
            .height(tokens.itemHeight)
            .clip(RoundedCornerShape(tokens.itemRadius))
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .semantics { this.selected = selected; role = Role.Tab }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNavigate(entry.route) }
            .padding(horizontal = tokens.itemHorizontalPadding)
            .testTag("netflix_top_nav_${entry.route}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = entry.label,
            style = TextStyle(
                fontFamily = NetflixThemeTokens.fontFamily,
                fontSize = tokens.labelSize,
                fontWeight = if (selected || focused) FontWeight.Medium else FontWeight.Normal,
            ).withNuvioDescenderRoom(),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NetflixTopNavIcon(
    entry: NetflixNavEntry,
    icon: ImageVector,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onNavigate: (String) -> Unit,
) {
    val tokens = NetflixThemeTokens.TopNav
    var focused by remember { mutableStateOf(false) }
    val (background, tint) = navPillColors(selected, focused)
    Box(
        modifier = Modifier
            .size(tokens.itemHeight)
            .clip(CircleShape)
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = entry.label; this.selected = selected; role = Role.Tab }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNavigate(entry.route) }
            .testTag("netflix_top_nav_${entry.route}"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(tokens.iconSize))
    }
}

/** The active profile at the bar's leading edge; selecting it opens Nuvio's own profile selection. */
@Composable
private fun NetflixTopNavProfile(profile: TopMenuProfile) {
    val tokens = NetflixThemeTokens.TopNav
    var focused by remember { mutableStateOf(false) }
    val switchLabel = stringResource(R.string.top_menu_switch_profile, profile.name)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(tokens.itemHeight)
                .clip(RoundedCornerShape(tokens.itemRadius))
                .then(if (focused) Modifier.border(tokens.focusOutline, NetflixThemeTokens.focus, RoundedCornerShape(tokens.itemRadius)) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = switchLabel; role = Role.Button }
                .clickable(onClick = profile.onSwitch)
                .testTag("netflix_top_nav_profile"),
            contentAlignment = Alignment.Center,
        ) {
            ProfileAvatarCircle(
                name = profile.name,
                colorHex = profile.colorHex,
                size = tokens.avatarSize,
                avatarImageUrl = profile.avatarUrl,
                imageCrossfade = false,
                avatarShape = RoundedCornerShape(tokens.itemRadius),
            )
        }
        Spacer(Modifier.padding(top = tokens.indicatorGap).height(tokens.indicatorHeight))
    }
}
