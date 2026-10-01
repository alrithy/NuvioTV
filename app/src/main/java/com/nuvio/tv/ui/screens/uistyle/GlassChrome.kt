package com.nuvio.tv.ui.screens.uistyle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.fork.uistyle.GlassEffect
import com.nuvio.tv.fork.uistyle.TopChromeBack
import com.nuvio.tv.fork.uistyle.UiStyleRules
import com.nuvio.tv.navigateToDrawerRoute
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.theme.NuvioTheme
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

/*
 * G12b / G12c Glass chrome (D060, D061; features 193, 194, 197, 199, 200). FILE_PORT of xnucade/NuvioGlass @ 84098b7
 * (`GlassScaffold`, `GlassSurface`, `GlassTokens`, `LocalGlassChromeReveal`), adapted: the menu,
 * clock and profile button are the G12a top-menu parts; how the glass is drawn comes from
 * [UiStyleRules.glassEffect] (API level, AdaptiveResources tier, "Lightweight effects") instead of an
 * API check alone; Back follows the same contract as the other top menus.
 *
 * The chrome floats over official's Modern home and its full-bleed hero. On Home it hides itself when
 * idle and comes back on Up from the first row (the rows list calls [LocalGlassChromeReveal]). It is
 * only shown on root screens, never over the player. Cinematic Glass also puts the focused title's
 * artwork full screen behind everything ([LocalCinematicGlass]). On capable devices the pills are
 * Reshaped's liquid glass ([LiquidGlassBackdrop]) instead of a blur.
 */

/**
 * Lets the home rows hand Up to the Glass chrome when there is no row above. Returns true when the
 * chrome took the key. A no-op (false) everywhere else, so official layouts are unchanged.
 */
val LocalGlassChromeReveal = staticCompositionLocalOf<() -> Boolean> { { false } }

/**
 * True under Cinematic Glass: official's Modern home then shows its full-screen hero backdrop
 * whatever that setting says, without writing it (D061). False everywhere else.
 */
val LocalCinematicGlass = staticCompositionLocalOf { false }

/** NuvioGlass `GlassTokens`. */
private object GlassTokens {
    val blurRadius = 24.dp
    const val inputScale = 0.66f
    const val noiseFactor = 0.04f
    const val tintAlpha = 0.28f
    const val tintAlphaFlat = 0.82f
    const val highlightAlpha = 0.10f
    const val borderTopAlpha = 0.22f
    const val borderBottomAlpha = 0.07f
    const val scrimAlpha = 0.30f
    const val contentDimWhileFocused = 0.45f
    val hiddenOffset = 120.dp
    val scrimHeight = 160.dp

    /** Neutral, slightly cool base so the tint reads as glass rather than a grey card. */
    val base = Color(0xFF16161A)
}

/**
 * Frosted surface: blur what is behind, tint it, then a hairline that is bright at the top and
 * nearly gone at the bottom, like a bevel catching light. [GlassEffect.FLAT] (or no haze state)
 * keeps the same look with an opaque tint and no blur. [GlassEffect.LIQUID] with a recorder draws
 * the capsule lens instead, which brings its own rim light.
 */
@Composable
internal fun Modifier.glassSurface(
    shape: Shape,
    effect: GlassEffect,
    hazeState: HazeState?,
    liquid: LiquidGlassBackdrop? = null,
    focus: Float = 0f,
): Modifier {
    if (effect == GlassEffect.LIQUID && liquid != null) {
        return clip(shape).liquidGlassSurface(liquid, focus, NuvioTheme.colors.FocusBackground)
    }
    val liveBlur = effect == GlassEffect.BLUR && hazeState != null
    val tint = remember(liveBlur) {
        val alpha = if (liveBlur) GlassTokens.tintAlpha else GlassTokens.tintAlphaFlat
        Brush.verticalGradient(
            listOf(
                GlassTokens.base.copy(alpha = (alpha + GlassTokens.highlightAlpha).coerceAtMost(1f)),
                GlassTokens.base.copy(alpha = alpha),
            ),
        )
    }
    val edge = remember {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = GlassTokens.borderTopAlpha), Color.White.copy(alpha = GlassTokens.borderBottomAlpha)),
        )
    }
    return clip(shape)
        .then(
            if (liveBlur) {
                Modifier.hazeEffect(state = hazeState!!) {
                    blurRadius = GlassTokens.blurRadius
                    noiseFactor = GlassTokens.noiseFactor
                    inputScale = HazeInputScale.Fixed(GlassTokens.inputScale)
                }
            } else {
                Modifier
            },
        )
        .background(brush = tint, shape = shape)
        .border(width = NuvioTheme.spacing.hairline, brush = edge, shape = shape)
}

@Composable
internal fun GlassChromeScaffold(
    effect: GlassEffect,
    cinematic: Boolean,
    clockEnabled: Boolean,
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    profile: TopMenuProfile?,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit,
) {
    val hazeState = remember(effect) { if (effect == GlassEffect.BLUR) HazeState() else null }
    val liquid = rememberLiquidGlassBackdrop(effect == GlassEffect.LIQUID)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val contentFocusRequester = remember { FocusRequester() }
    val menuFocusRequester = remember { FocusRequester() }
    val onRootRoute = currentRoute in rootRoutes
    val autoHides = currentRoute == Screen.Home.route

    var chromeVisible by remember { mutableStateOf(true) }
    var chromeFocused by remember { mutableStateOf(false) }
    // Once the chrome has been used, going back down to the rows dismisses it promptly.
    var chromeHasHeldFocus by remember { mutableStateOf(false) }
    var pendingMenuFocus by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) { longPressBackHeld.value = false }
    LaunchedEffect(onRootRoute, currentRoute) { chromeVisible = onRootRoute }
    LaunchedEffect(chromeFocused) { if (chromeFocused) chromeVisible = true }
    // Focus can only land once the chrome is back in view.
    LaunchedEffect(pendingMenuFocus, chromeVisible) {
        if (!pendingMenuFocus || !chromeVisible) return@LaunchedEffect
        delay(80)
        runCatching { menuFocusRequester.requestFocus() }
        pendingMenuFocus = false
    }
    // The liquid lens follows the screen only while the chrome shows; hidden, its recording is dropped.
    LaunchedEffect(liquid, chromeVisible, onRootRoute) {
        if (liquid == null) return@LaunchedEffect
        if (chromeVisible && onRootRoute) liquid.refreshWhileShown() else liquid.clear(density, layoutDirection)
    }
    LaunchedEffect(chromeVisible, chromeFocused, currentRoute) {
        if (!chromeVisible || chromeFocused || !onRootRoute || !autoHides) return@LaunchedEffect
        delay(UiStyleRules.glassChromeHideDelayMs(chromeHasHeldFocus))
        chromeVisible = false
    }

    val revealChrome: () -> Boolean = {
        if (onRootRoute && !chromeFocused) {
            chromeVisible = true
            pendingMenuFocus = true
            true
        } else {
            false
        }
    }

    BackHandler(enabled = onRootRoute) {
        when (UiStyleRules.back(onRootRoute, chromeFocused, longPressBackHeld.value)) {
            TopChromeBack.FOCUS_CHROME -> revealChrome()
            TopChromeBack.EXIT_APP -> onExitApp()
            TopChromeBack.IGNORE -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) liquid?.poke()
                false
            }
            .longPressBackToChrome(onRootRoute, longPressBackHeld) { revealChrome() },
    ) {
        val contentDim by animateFloatAsState(
            targetValue = if (chromeFocused) GlassTokens.contentDimWhileFocused else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "glassContentDim",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentDim }
                .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                .liquidGlassSource(liquid?.takeIf { chromeVisible && onRootRoute }),
        ) {
            CompositionLocalProvider(
                LocalSidebarExpanded provides false,
                LocalContentFocusRequester provides contentFocusRequester,
                LocalGlassChromeReveal provides revealChrome,
                LocalCinematicGlass provides cinematic,
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    hideBuiltInHeaders = true,
                )
            }
        }

        if (onRootRoute) {
            // Always composed on a root screen and moved with a graphicsLayer when hidden, so it stays
            // in the focus graph: ordinary traversal reaches it only from the top of the content.
            val hiddenOffsetPx = with(LocalDensity.current) { GlassTokens.hiddenOffset.toPx() }
            val shift by animateFloatAsState(
                targetValue = if (chromeVisible) 0f else -hiddenOffsetPx,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "glassChromeShift",
            )
            val chromeAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "glassChromeAlpha",
            )
            val pill = RoundedCornerShape(NuvioTheme.radii.full)
            val focusFraction by animateFloatAsState(
                targetValue = if (chromeFocused) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "glassChromeFocus",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = shift
                        alpha = chromeAlpha
                    },
            ) {
                // A soft scrim so light text survives a bright backdrop, gone before the hero title.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GlassTokens.scrimHeight)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = GlassTokens.scrimAlpha), Color.Transparent))),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = NuvioTheme.spacing.screen.overscanHorizontal,
                            vertical = NuvioTheme.spacing.screen.vertical,
                        )
                        .onFocusChanged { state ->
                            chromeFocused = state.hasFocus
                            if (state.hasFocus) chromeHasHeldFocus = true
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopMenu(
                        items = drawerItems,
                        selectedRoute = selectedDrawerRoute ?: currentRoute,
                        container = Modifier.glassSurface(pill, effect, hazeState, liquid, focusFraction),
                        focusRequester = menuFocusRequester,
                        onFocusChanged = {},
                        onNavigate = { route ->
                            chromeVisible = false
                            onNavigate(route)
                            navigateToDrawerRoute(navController, currentRoute, route)
                        },
                    )
                    if (clockEnabled || profile != null) {
                        Row(
                            modifier = Modifier
                                .height(52.dp)
                                .glassSurface(pill, effect, hazeState, liquid, focusFraction)
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (clockEnabled) {
                                Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                                TopMenuClock()
                                Spacer(modifier = Modifier.width(NuvioTheme.spacing.md))
                            }
                            if (profile != null) TopMenuProfileButton(profile)
                        }
                    }
                }
            }
        }
    }
}
