package com.nuvio.tv.ui.screens.uistyle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.R
import com.nuvio.tv.fork.uistyle.NavigationStyle
import com.nuvio.tv.fork.uistyle.TopChromeBack
import com.nuvio.tv.fork.uistyle.UiStyleRules
import com.nuvio.tv.navigateToDrawerRoute
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.screens.settings.rememberRawSvgPainter
import com.nuvio.tv.ui.theme.NuvioTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/*
 * G12a top menu (D058; features 196, 198, 204, 205): one scaffold for both top looks, used in place
 * of official's sidebar when the profile picks Top bar or Pill. FILE_PORT of xnucade/NuvioGlass
 * @ 84098b7 (`GlassScaffold`, `GlassNavPill` with its sliding indicator, `GlassClockPill`), adapted:
 * the menu sits above the content instead of over a hero (no auto-hide, so ordinary focus traversal
 * reaches it with Up from the top row); a full-width BAR look (Cxsmo `TopNavigation` @ 3e0d0fa) and a
 * compact PILL look (Reshaped pill menu @ 0ccf049); the profile button (Cxsmo) switches profile the
 * way official's sidebar does; Back follows official's sidebar (first Back focuses the menu, the next
 * leaves). Navigation itself is official's own `navigateToDrawerRoute`.
 */

private val MenuHeight = 52.dp
private val ItemHeight = 40.dp

@Composable
internal fun TopChromeScaffold(
    style: NavigationStyle,
    clockEnabled: Boolean,
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit,
) {
    val onRootRoute = currentRoute in rootRoutes
    val contentFocusRequester = remember { FocusRequester() }
    val menuFocusRequester = remember { FocusRequester() }
    var menuFocused by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) { longPressBackHeld.value = false }

    BackHandler(enabled = onRootRoute) {
        when (UiStyleRules.back(onRootRoute, menuFocused, longPressBackHeld.value)) {
            TopChromeBack.FOCUS_CHROME -> runCatching { menuFocusRequester.requestFocus() }
            TopChromeBack.EXIT_APP -> onExitApp()
            TopChromeBack.IGNORE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
    ) {
        if (onRootRoute) {
            TopMenuRow(
                style = style,
                clockEnabled = clockEnabled,
                items = drawerItems,
                selectedRoute = selectedDrawerRoute ?: currentRoute,
                menuFocusRequester = menuFocusRequester,
                onMenuFocusChanged = { menuFocused = it },
                onNavigate = { route ->
                    onNavigate(route)
                    navigateToDrawerRoute(navController, currentRoute, route)
                },
                profile = if (showProfileSelector) {
                    TopMenuProfile(activeProfileName, activeProfileColorHex, activeProfileAvatarImageUrl, onSwitchProfile)
                } else {
                    null
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .longPressBackToChrome(onRootRoute, longPressBackHeld) {
                    runCatching { menuFocusRequester.requestFocus() }
                },
        ) {
            CompositionLocalProvider(
                LocalSidebarExpanded provides false,
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

/** Long-press Back on a root screen goes straight to the menu, as official's sidebar does. */
internal fun Modifier.longPressBackToChrome(
    onRootRoute: Boolean,
    longPressBackHeld: MutableState<Boolean>,
    focusChrome: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.key != Key.Back) return@onPreviewKeyEvent false
    if (event.type == KeyEventType.KeyDown && onRootRoute && event.nativeKeyEvent.isLongPress) {
        if (!longPressBackHeld.value) {
            longPressBackHeld.value = true
            focusChrome()
        }
        return@onPreviewKeyEvent true
    }
    if (longPressBackHeld.value) {
        if (event.type == KeyEventType.KeyUp) longPressBackHeld.value = false
        return@onPreviewKeyEvent true
    }
    false
}

internal data class TopMenuProfile(
    val name: String,
    val colorHex: String,
    val avatarUrl: String?,
    val onSwitch: () -> Unit,
)

@Composable
private fun TopMenuRow(
    style: NavigationStyle,
    clockEnabled: Boolean,
    items: List<DrawerItem>,
    selectedRoute: String?,
    menuFocusRequester: FocusRequester,
    onMenuFocusChanged: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    profile: TopMenuProfile?,
) {
    val bar = style == NavigationStyle.TOP_BAR
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (bar) Modifier.background(NuvioTheme.colors.BackgroundElevated) else Modifier)
            .padding(
                horizontal = NuvioTheme.spacing.screen.overscanHorizontal,
                vertical = NuvioTheme.spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (bar) Arrangement.Start else Arrangement.Center,
    ) {
        TopMenu(
            items = items,
            selectedRoute = selectedRoute,
            container = if (bar) Modifier else Modifier.pillSurface(),
            focusRequester = menuFocusRequester,
            onFocusChanged = onMenuFocusChanged,
            onNavigate = onNavigate,
        )
        if (bar) Spacer(modifier = Modifier.weight(1f)) else Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
        if (clockEnabled) {
            TopMenuClock()
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
        }
        if (profile != null) TopMenuProfileButton(profile)
    }
}

/** The PILL look's plain surface (the Glass look uses [glassSurface] instead). */
@Composable
private fun Modifier.pillSurface(): Modifier {
    val shape = RoundedCornerShape(NuvioTheme.radii.full)
    return clip(shape)
        .background(NuvioTheme.colors.BackgroundElevated)
        .border(NuvioTheme.spacing.hairline, NuvioTheme.colors.TextPrimary.copy(alpha = 0.14f), shape)
}

/**
 * The destinations, with one indicator that slides between them (NuvioGlass `GlassNavPill`):
 * opaque on the focused item, a faint marker on the current destination otherwise.
 */
@Composable
internal fun TopMenu(
    items: List<DrawerItem>,
    selectedRoute: String?,
    /** The surface behind the items: none for BAR, a pill for PILL, glass for Glass. */
    container: Modifier,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val density = LocalDensity.current
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.takeIf { it >= 0 } ?: 0
    val activeIndex = focusedIndex ?: selectedIndex
    val bounds = remember { mutableStateMapOf<Int, Pair<Dp, Dp>>() }
    val active = bounds[activeIndex]
    val indicatorSpring = spring<Dp>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
    val indicatorX by animateDpAsState(active?.first ?: 0.dp, indicatorSpring, label = "topMenuIndicatorX")
    val indicatorWidth by animateDpAsState(active?.second ?: 0.dp, indicatorSpring, label = "topMenuIndicatorWidth")
    val indicatorColor by animateColorAsState(
        targetValue = if (focusedIndex != null) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.TextPrimary.copy(alpha = 0.12f),
        label = "topMenuIndicatorColor",
    )
    val shape = RoundedCornerShape(NuvioTheme.radii.full)

    Box(
        modifier = Modifier
            .height(MenuHeight)
            .then(container)
            .padding(horizontal = 6.dp)
            .onFocusChanged { state ->
                onFocusChanged(state.hasFocus)
                if (!state.hasFocus) focusedIndex = null
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (active != null) {
            // Measured in absolute (left-based) positions, so the indicator lines up in RTL too.
            Box(
                modifier = Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .absoluteOffset(x = indicatorX)
                    .width(indicatorWidth)
                    .height(ItemHeight)
                    .clip(shape)
                    .background(indicatorColor),
            )
        }
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEachIndexed { index, item ->
                TopMenuItem(
                    item = item,
                    selected = index == selectedIndex,
                    focused = focusedIndex == index,
                    focusRequester = if (index == selectedIndex) focusRequester else null,
                    onFocused = { focusedIndex = index },
                    onClick = { onNavigate(item.route) },
                    onPlaced = { x, width -> bounds[index] = with(density) { x.toDp() } to width },
                    density = density,
                )
            }
        }
    }
}

@Composable
private fun TopMenuItem(
    item: DrawerItem,
    selected: Boolean,
    focused: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onPlaced: (Float, Dp) -> Unit,
    density: androidx.compose.ui.unit.Density,
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            focused -> NuvioTheme.colors.FocusContent
            selected -> NuvioTheme.colors.TextPrimary
            else -> NuvioTheme.colors.TextSecondary
        },
        label = "topMenuItemColor",
    )
    Row(
        modifier = Modifier
            .height(ItemHeight)
            .onGloballyPositioned { coords ->
                onPlaced(coords.positionInParent().x, with(density) { coords.size.width.toDp() })
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier.size(NuvioTheme.sizes.icons.md)
        when {
            item.iconRes != null -> Icon(
                painter = rememberRawSvgPainter(item.iconRes, NuvioTheme.sizes.icons.md),
                contentDescription = null,
                tint = contentColor,
                modifier = iconModifier,
            )
            item.icon != null -> Icon(imageVector = item.icon, contentDescription = null, tint = contentColor, modifier = iconModifier)
        }
        Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** The time, redrawn on the minute, in the device's 12 / 24-hour format (NuvioGlass `GlassClockPill`). */
@Composable
internal fun TopMenuClock() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        androidx.core.os.ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val is24Hour = remember(configuration) { android.text.format.DateFormat.is24HourFormat(context) }
    val formatter = remember(is24Hour, locale) { DateTimeFormatter.ofPattern(UiStyleRules.clockPattern(is24Hour), locale) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(UiStyleRules.millisToNextMinute(System.currentTimeMillis()))
        }
    }
    Text(
        text = now.format(formatter),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = NuvioTheme.colors.TextPrimary,
        maxLines = 1,
    )
}

/** The active profile; selecting it opens profile selection as official's sidebar profile row does. */
@Composable
internal fun TopMenuProfileButton(profile: TopMenuProfile) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NuvioTheme.radii.full)
    Row(
        modifier = Modifier
            .height(ItemHeight)
            .clip(shape)
            .background(if (focused) NuvioTheme.colors.FocusBackground else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = profile.onSwitch)
            .padding(horizontal = NuvioTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatarCircle(
            name = profile.name,
            colorHex = profile.colorHex,
            size = 30.dp,
            avatarImageUrl = profile.avatarUrl,
            imageCrossfade = false,
        )
        if (focused) {
            Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
            Text(
                text = stringResource(R.string.top_menu_switch_profile, profile.name),
                style = MaterialTheme.typography.labelLarge,
                color = NuvioTheme.colors.FocusContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
