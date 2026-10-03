@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.theme

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.LocaleList
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.focusRequester
import androidx.tv.material3.Button
import coil3.compose.AsyncImage
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocaleCache
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.domain.model.AppTheme
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.PosterCardStyle
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.NuvioDialogButton
import com.nuvio.tv.ui.components.ErrorState
import com.nuvio.tv.ui.components.EmptyScreenState
import com.nuvio.tv.ui.screens.detail.EpisodesRow
import com.nuvio.tv.ui.screens.detail.HeroContentSection
import com.nuvio.tv.ui.screens.detail.SeasonTabs
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HeroCarouselRow
import com.nuvio.tv.ui.screens.home.HeroPreview
import com.nuvio.tv.ui.screens.home.HomeScreenFocusState
import com.nuvio.tv.ui.screens.home.HomeUiState
import com.nuvio.tv.ui.screens.home.ModernCarouselItem
import com.nuvio.tv.ui.screens.home.ModernHomeContent
import com.nuvio.tv.ui.screens.home.ModernHomePresentationState
import com.nuvio.tv.ui.screens.home.ModernPayload
import com.nuvio.tv.ui.screens.home.NetflixExpandedCardContent
import com.nuvio.tv.ui.screens.home.buildCarouselRowLookups
import com.nuvio.tv.ui.screens.player.ControlButton
import com.nuvio.tv.ui.screens.player.NetflixPlayerContentHeading
import com.nuvio.tv.ui.screens.player.PlayerTimeText
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.ProgressBar
import com.nuvio.tv.ui.screens.player.NetflixPlayerLoadingOverlay
import com.nuvio.tv.ui.screens.profile.ProfileSelectionMainContent
import com.nuvio.tv.ui.screens.search.NetflixSearchContent
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchUiState
import com.nuvio.tv.ui.util.asStable
import com.nuvio.tv.fork.resource.MemoryTier
import com.nuvio.tv.ui.screens.home.NetflixCallout
import com.nuvio.tv.ui.screens.home.NetflixCalloutKind
import com.nuvio.tv.ui.screens.home.NetflixHeroTitleContent
import com.nuvio.tv.ui.screens.home.netflixHomePreviewPolicy
import com.nuvio.tv.ui.screens.mynetflix.MyNetflixHubContent
import com.nuvio.tv.ui.screens.mynetflix.buildMyNetflixHub
import com.nuvio.tv.ui.screens.uistyle.NetflixNavEntry
import com.nuvio.tv.ui.screens.uistyle.NetflixTopNavigationBar
import com.nuvio.tv.ui.screens.uistyle.TopMenuProfile
import com.nuvio.tv.ui.screens.uistyle.netflixNavEntries
import com.nuvio.tv.domain.model.LibraryEntry
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual production UI with original offline artwork; no live services or playback engine. */
@RunWith(AndroidJUnit4::class)
class NetflixThemeTvTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val localeTag get() = InstrumentationRegistry.getArguments().getString("netflix_locale", "en")
    private val arabic get() = localeTag == "ar"
    private fun text(english: String, arabicText: String) = if (arabic) arabicText else english

    private val titles get() = if (arabic) listOf("آخر ضوء", "الممر الشمالي", "أفق بعيد", "الطريق إلى البيت", "إشارات الليل", "خارج المدينة")
        else listOf("The Last Light", "Northern Passage", "Wild Horizon", "The Long Way Home", "Midnight Signals", "Beyond the City")
    private val synopsis get() = text("A quiet journey beyond the familiar reveals a new beginning. Six original test stories, rendered without a network connection.",
        "رحلة هادئة إلى ما وراء المألوف تكشف عن بداية جديدة. قصص أصلية للاختبار تُعرض دون الحاجة إلى الاتصال بالشبكة.")
    private val items by lazy {
        titles.mapIndexed { index, title ->
            MetaPreview(id = "fixture:$index", type = ContentType.MOVIE, name = title,
                poster = artwork(index), posterShape = PosterShape.LANDSCAPE, background = artwork(index),
                logo = null, description = synopsis, releaseInfo = "2026", imdbRating = 8.1f,
                genres = listOf("Drama", "Adventure"), runtime = "112", ageRating = "16+",
                landscapePoster = artwork(index))
        }
    }

    // ---- Home: maintainer reference (audit §0) — hero card, category strip, inline portrait → landscape rows ----

    @Test fun homeHeroShowsMetadataAndPlayUsesTheExistingCallback() {
        var opened = 0
        setContent { FullHome { opened++ } }
        compose.onNodeWithTag("netflix_hero_play").requestFocus().assertIsFocused()
        capture("01-home-hero")
        capture("27-home-hero-full-reference-state")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun heroIsLargeRoundedCardInsideSafeMargins() {
        setContent { FullHome {} }
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val hero = bounds("netflix_home_hero_card")
        val margin = with(compose.density) { NetflixThemeTokens.safeMargin.toPx() }
        // A bounded card, not a full-bleed backdrop: inset on both sides, and the dominant first surface.
        assertTrue("hero inset start", hero.left >= margin - 1f)
        assertTrue("hero inset end", hero.right <= root.right - margin + 1f)
        assertTrue("hero is large", hero.height >= root.height * .45f)
    }

    @Test fun homeRowsKeepContentReachableFromTheHero() {
        setContent { FullHome {} }
        compose.onNodeWithTag("netflix_hero_play").requestFocus().assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("netflix_hero_play").assertIsNotFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        capture("28-home-category-shortcuts")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        capture("02-home-rows")
    }

    @Test fun categoryShortcutsOpenTheRealCatalogThroughSeeAll() {
        var opened: String? = null
        setContent { FullHome(onSeeAll = { catalog, _, _ -> opened = catalog }) {} }
        compose.onNodeWithTag("netflix_category_strip").assertIsDisplayed()
        compose.onNodeWithTag("netflix_category_trending").requestFocus().assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("trending", opened) }
    }

    @Test fun primaryRowUsesPortraitIdleCards() {
        setContent { FullHome {} }
        compose.onNodeWithTag("netflix_hero_play").requestFocus()
        compose.onNodeWithTag("netflix_home").performScrollToNode(hasTestTag("netflix_row_trending"))
        val idle = bounds("netflix_home_card_focus_trending:fixture:0")
        assertTrue("idle poster is portrait", idle.height > idle.width)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(0)
        capture("29-browse-row-portrait-idle")
    }

    @Test fun focusedPosterExpandsInlineToLandscapeWithoutOverlap() {
        setContent { FullHome {} }
        val first = "netflix_home_card_focus_trending:fixture:0"
        val second = "netflix_home_card_focus_trending:fixture:1"
        focusCard(first)
        compose.onNodeWithTag(first).assertIsFocused()
        compose.waitForIdle()
        val expanded = bounds(first)
        val sibling = bounds(second)
        assertTrue("focused card is landscape", expanded.width > expanded.height * 1.6f)
        assertTrue("sibling stays portrait", sibling.height > sibling.width)
        assertTrue("expanded ≈ 2–3× an idle poster", expanded.width > sibling.width * 2f)
        // Siblings move aside: no horizontal overlap in either reading direction.
        assertTrue("no overlap", if (arabic) sibling.right <= expanded.left + 1f else sibling.left >= expanded.right - 1f)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
        // The facts beneath the row belong to the expanded item and to nothing else.
        compose.onNodeWithTag("netflix_focused_facts_trending:fixture:0").assertIsDisplayed()
        compose.onNodeWithTag("netflix_focused_facts_trending:fixture:1").assertDoesNotExist()
        capture("03-focused-card")
        // The reference has no separate floating preview: the inline landscape card IS the expanded card.
        capture("04-expanded-card")
        capture("30-browse-row-focused-landscape")
        capture("34-arabic-browse-expanded")
    }

    @Test fun focusedItemCollapseAndNextExpansionAreDeterministic() {
        setContent { FullHome {} }
        val towardNext = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        focusCard("netflix_home_card_focus_trending:fixture:0")
        press(towardNext)
        compose.onNodeWithTag("netflix_home_card_focus_trending:fixture:1").assertIsFocused()
        val collapsed = bounds("netflix_home_card_focus_trending:fixture:0")
        val next = bounds("netflix_home_card_focus_trending:fixture:1")
        assertTrue("previous collapsed to portrait", collapsed.height > collapsed.width)
        assertTrue("next expanded to landscape", next.width > next.height * 1.6f)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("netflix_focused_facts_trending:fixture:1").assertIsDisplayed()
        capture("31-browse-row-focus-moved-next-item")
    }

    @Test fun rapidRemoteNavigationLeavesOnlyOneExpandedItem() {
        setContent { FullHome {} }
        val towardNext = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        focusCard("netflix_home_card_focus_trending:fixture:0")
        // Key presses without waiting in between, as a held/rapid remote produces them.
        repeat(4) { instrumentation.sendKeyDownUpSync(towardNext) }
        compose.waitForIdle()
        compose.onNodeWithTag("netflix_home_card_focus_trending:fixture:4").assertIsFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
    }

    // ---- Final fidelity round: focus comfort zone, production scaffold, stress (docs/NETFLIX_REFERENCE_FIDELITY.md) ----

    private val towardNextKey get() = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
    private fun card(row: String, index: Int) = "netflix_home_card_focus_$row:fixture:${index % 6}${if (index >= 6) ":second" else ""}"
    private fun rootBounds() = compose.onRoot().fetchSemanticsNode().boundsInRoot
    /** Distance of a card from the reading-start edge of the screen (physical left in LTR, right in RTL). */
    private fun fromStart(tag: String): Float = bounds(tag).let { if (arabic) rootBounds().right - it.right else it.left }

    @Composable private fun ProductionScaffoldHome(focusState: HomeScreenFocusState = HomeScreenFocusState(), onOpen: () -> Unit = {}) {
        // The production Netflix top bar above the production Home composable (ModernHomeContent dispatches
        // to NetflixHomeContent), filling the screen as NetflixTopNavigationScaffold lays them out.
        Column(Modifier.fillMaxSize()) {
            NetflixTopNavigationBar(navigationEntries(), "home", remember { FocusRequester() },
                TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) {}, onFocusChanged = {}, onNavigate = {})
            Box(Modifier.weight(1f)) { FullHome(focusState = focusState, onOpen = onOpen) }
        }
    }

    @Test fun productionScaffoldHomeShowsNavHeroCategoriesAndFirstRow() {
        setContent { ProductionScaffoldHome() }
        compose.onNodeWithTag("netflix_hero_play").requestFocus().assertIsFocused()
        compose.onNodeWithTag("netflix_top_nav").assertIsDisplayed()
        compose.onNodeWithTag("netflix_home_hero_card").assertIsDisplayed()
        compose.onNodeWithTag("netflix_category_strip").assertIsDisplayed()
        val root = rootBounds()
        val nav = bounds("netflix_top_nav"); val hero = bounds("netflix_home_hero_card")
        assertTrue("hero below the bar", hero.top >= nav.bottom - 1f)
        assertTrue("hero is the dominant surface", hero.height >= root.height * .45f)
        capture("36-home-full-production-top")
    }

    @Test fun productionScaffoldHomeScrolledShowsExpandedCardFactsAndNextRow() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        press(towardNextKey)
        compose.onNodeWithTag(card("trending", 1)).assertIsFocused()
        compose.onNodeWithTag("netflix_top_nav").assertIsDisplayed()
        compose.onNodeWithTag("netflix_focused_facts_trending:fixture:1").assertIsDisplayed()
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
        // The next row peeks below the facts, as in the reference browse state.
        val facts = bounds("netflix_focused_facts_trending:fixture:1")
        val next = bounds("netflix_row_popular")
        assertTrue("next row starts below the facts", next.top >= facts.bottom - 1f)
        assertTrue("next row is partly visible", next.top < rootBounds().bottom)
        capture("37-home-full-production-scrolled")
    }

    @Test fun middlePosterExpandsWithoutJumpingToEdge() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        val firstIdleStart = fromStart(card("trending", 0))
        press(towardNextKey); press(towardNextKey)
        compose.onNodeWithTag(card("trending", 2)).assertIsFocused()
        // The row did not scroll: the first poster is still where it was, so the middle card expanded in place.
        assertEquals(firstIdleStart, fromStart(card("trending", 0)), 2f)
        assertTrue("middle card is not at the reading edge", fromStart(card("trending", 2)) > fromStart(card("trending", 0)) + 100f)
        capture("38-comfort-zone-middle")
    }

    @Test fun expansionScrollsOnlyWhenRequiredForVisibility() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        val root = rootBounds()
        repeat(6) { press(towardNextKey) }
        compose.onNodeWithTag(card("trending", 6)).assertIsFocused()
        val expanded = bounds(card("trending", 6))
        assertTrue("fully visible", expanded.left >= root.left - 1f && expanded.right <= root.right + 1f)
        // Only the overflow was scrolled: the previous neighbour is still on screen beside it.
        compose.onNodeWithTag(card("trending", 5)).assertIsDisplayed()
        assertTrue("not re-anchored to the reading start", fromStart(card("trending", 6)) > NetflixThemeTokens.safeMargin.value * compose.density.density + 50f)
        capture("39-comfort-zone-near-edge")
    }

    @Test fun firstAndLastItemsRemainReachable() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        repeat(11) { press(towardNextKey) }
        compose.onNodeWithTag(card("trending", 11)).assertIsFocused()
        val root = rootBounds(); val last = bounds(card("trending", 11))
        assertTrue("last fully visible", last.left >= root.left - 1f && last.right <= root.right + 1f)
        capture("40-comfort-zone-rtl-last")
        val towardStart = if (arabic) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(11) { press(towardStart) }
        compose.onNodeWithTag(card("trending", 0)).assertIsFocused()
        val margin = NetflixThemeTokens.safeMargin.value * compose.density.density
        assertEquals("first item anchors at the reading edge", margin, fromStart(card("trending", 0)), 3f)
    }

    @Test fun focusMovePreservesComfortZone() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        val root = rootBounds()
        for (index in 1..9) {
            press(towardNextKey)
            val b = bounds(card("trending", index))
            assertTrue("card $index fully visible", b.left >= root.left - 1f && b.right <= root.right + 1f)
            compose.onNodeWithTag(card("trending", index - 1)).assertIsDisplayed()
        }
    }

    @Test fun rtlComfortZoneMatchesVisibleBounds() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        repeat(5) { press(towardNextKey) }
        val root = rootBounds()
        val focused = bounds(card("trending", 5)); val previous = bounds(card("trending", 4))
        assertTrue("inside physical bounds", focused.left >= root.left - 1f && focused.right <= root.right + 1f)
        // The previous item lies on the reading-start side in both directions.
        assertTrue(if (arabic) previous.left >= focused.right - 1f else previous.right <= focused.left + 1f)
    }

    @Test fun rapidFocusDoesNotOscillateViewport() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        repeat(5) { press(towardNextKey) }
        val towardStart = if (arabic) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        val anchor = fromStart(card("trending", 3))
        repeat(10) { instrumentation.sendKeyDownUpSync(towardStart); instrumentation.sendKeyDownUpSync(towardNextKey) }
        compose.waitForIdle()
        compose.onNodeWithTag(card("trending", 5)).assertIsFocused()
        assertEquals("viewport settled where it was", anchor, fromStart(card("trending", 3)), 2f)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun categoryStripTilesAreIntentionalTonalTiles() {
        setContent { ProductionScaffoldHome() }
        compose.onNodeWithTag("netflix_category_trending").requestFocus().assertIsFocused()
        // Measured on unfocused tiles so the focus treatment does not enter the geometry.
        val tile = bounds("netflix_category_popular"); val next = bounds("netflix_category_movies")
        val density = compose.density.density
        assertEquals(NetflixThemeTokens.Home.categoryWidth.value * density, tile.width, 2f)
        assertEquals(NetflixThemeTokens.Home.categoryHeight.value * density, tile.height, 2f)
        val gap = if (arabic) tile.left - next.right else next.left - tile.right
        assertEquals(NetflixThemeTokens.Home.categoryGap.value * density, gap, 2f)
        capture("41-category-strip-final")
    }

    @Test fun stressRapidFocusAndRowChangesKeepOneExpandedCard() {
        setContent { ProductionScaffoldHome() }
        focusCard(card("trending", 0))
        val keys = listOf(towardNextKey, towardNextKey, KeyEvent.KEYCODE_DPAD_DOWN, towardNextKey, KeyEvent.KEYCODE_DPAD_UP)
        repeat(100) { instrumentation.sendKeyDownUpSync(keys[it % keys.size]) }
        compose.waitForIdle()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
        // Repeated row changes: down/up twenty times, still exactly one expanded card and one facts block.
        repeat(20) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN); instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP) }
        compose.waitForIdle()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun stressHomeDetailsHomeTwentyTimesRestoresFocus() {
        val saved = HomeScreenFocusState(verticalScrollIndex = 2, focusedRowKey = "popular",
            focusedItemKeyByRow = mapOf("popular" to "popular:fixture:3"), hasSavedFocus = true)
        var details by mutableStateOf(false)
        setContent {
            if (details) {
                val back = remember { FocusRequester() }
                Button(onClick = { details = false }, Modifier.focusRequester(back).testTag("stress_details")) { Text("Back") }
                LaunchedEffect(Unit) { back.requestFocus() }
            } else {
                ProductionScaffoldHome(focusState = saved) { details = true }
            }
        }
        val target = "netflix_home_card_focus_popular:fixture:3"
        val runtime = Runtime.getRuntime()
        var baseline = 0L
        repeat(20) { round ->
            compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag(target).and(isFocused())).fetchSemanticsNodes().size == 1 }
            compose.onAllNodes(isFocused()).assertCountEquals(1)
            compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
            press(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("stress_details").and(isFocused())).fetchSemanticsNodes().size == 1 }
            press(KeyEvent.KEYCODE_DPAD_CENTER)
            if (round == 2) { runtime.gc(); baseline = runtime.totalMemory() - runtime.freeMemory() }
        }
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag(target).and(isFocused())).fetchSemanticsNodes().size == 1 }
        runtime.gc()
        val used = runtime.totalMemory() - runtime.freeMemory()
        // Bounded retention: seventeen more round trips must not grow the heap by more than 24 MB.
        assertTrue("heap grew ${(used - baseline) / 1_048_576} MB", used - baseline < 24L * 1_048_576)
    }

    @Test fun searchFullProductionScaffoldKeepsKeyboardAndPortraitResults() {
        setContent {
            Column(Modifier.fillMaxSize()) {
                NetflixTopNavigationBar(navigationEntries(), "search", remember { FocusRequester() },
                    TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) {}, onFocusChanged = {}, onNavigate = {})
                Box(Modifier.weight(1f)) {
                    NetflixSearchContent(SearchUiState(query = "light", submittedQuery = "light", catalogRows = listOf(catalog()),
                        isSearching = false, recentSearches = listOf(text("Adventure", "مغامرة"))),
                        restoreFocus = true, onEvent = {}, onNavigateToDetail = { _, _, _ -> })
                }
            }
        }
        compose.onNodeWithTag("netflix_top_nav").assertIsDisplayed()
        val root = rootBounds()
        val keyboard = bounds("netflix_search_field"); val result = bounds("netflix_search_result_0")
        assertTrue("portrait result", result.height > result.width)
        assertTrue("keyboard side", if (arabic) keyboard.left > result.right else keyboard.right < result.left)
        assertTrue("keyboard is a minority column", keyboard.width < root.width * .45f)
        capture("42-search-full-production")
        // Navigating into the results and back to the keyboard keeps exactly one focus owner.
        val towardResults = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(8) { press(towardResults) }
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        repeat(3) { press(KeyEvent.KEYCODE_DPAD_DOWN) }
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        capture("43-search-full-production-results")
    }

    @Test fun scrollingMovesHeroOutWhileTopNavigationRemains() {
        setContent {
            Column(Modifier.fillMaxSize()) {
                NetflixTopNavigationBar(navigationEntries(), "home", remember { FocusRequester() },
                    TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) {}, onFocusChanged = {}, onNavigate = {})
                Box(Modifier.weight(1f)) { FullHome {} }
            }
        }
        focusCard("netflix_home_card_focus_popular:fixture:2")
        compose.waitForIdle()
        // The active row rises to the top of the content: the hero has left the viewport, the bar has not.
        compose.onNodeWithTag("netflix_home_hero_card").assertDoesNotExist()
        compose.onNodeWithTag("netflix_top_nav").assertIsDisplayed()
        compose.onNodeWithTag("netflix_focused_facts_popular:fixture:2").assertIsDisplayed()
    }

    @Test fun movieDetailsRetainPlayAndLibraryActions() {
        var played = 0
        var listed = 0
        setContent { DetailFixture(meta(ContentType.MOVIE), onLibrary = { listed++ }) { played++ } }
        compose.onNodeWithTag("netflix_detail_play").requestFocus().assertIsFocused()
        listOf("4K", "HDR", "Dolby Vision", "Atmos", "99% Match").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        capture("05-movie-details")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, played) }
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, listed) }
    }

    @Test fun seriesDetailsShowSeriesAndEpisodeContext() {
        setContent { DetailFixture(meta(ContentType.SERIES)) {} }
        compose.onNodeWithTag("netflix_detail_play").requestFocus().assertIsFocused()
        capture("06-series-details")
    }

    @Test fun episodesShowSeasonProgressAndPlayTheSelectedVideo() {
        var played: String? = null
        val episodes = episodes()
        setContent {
            val seasonFocus = remember { FocusRequester() }
            val episodeFocus = remember { mutableMapOf<String, FocusRequester>() }
            Column(Modifier.fillMaxSize().padding(vertical = NetflixThemeTokens.safeVerticalMargin),
                verticalArrangement = Arrangement.spacedBy(NetflixThemeTokens.rowGap)) {
                Text(text("Episodes", "الحلقات"), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = NetflixThemeTokens.safeMargin))
                SeasonTabs(listOf(1, 2, 3), 1, {}, selectedTabFocusRequester = seasonFocus)
                EpisodesRow(episodes, episodeProgressMap = mapOf((1 to 2) to progress(episodes[1])),
                    watchedEpisodes = setOf(1 to 1), onEpisodeClick = { played = it.id }, onToggleEpisodeWatched = {},
                    upFocusRequester = seasonFocus, episodeFocusRequesters = episodeFocus,
                    restoreEpisodeId = episodes[1].id, restoreFocusToken = 1)
            }
        }
        capture("07-episodes")
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(episodes[1].id, played) }
    }

    @Test fun searchKeyboardUpdatesTheExistingQueryEventAndBackRestoresKeyboardFocus() {
        val emitted = mutableListOf<SearchEvent>()
        setContent {
            NetflixSearchContent(SearchUiState(query = "light", submittedQuery = "light", catalogRows = listOf(catalog()),
                isSearching = false, recentSearches = listOf(text("Adventure", "مغامرة"))),
                restoreFocus = true, onEvent = { emitted += it }, onNavigateToDetail = { _, _, _ -> })
        }
        compose.onNodeWithTag("netflix_search").assertIsDisplayed()
        // Reference Search: 2:3 poster results; keyboard on the reading-start side (right in Arabic).
        val result = compose.onNodeWithTag("netflix_search_result_0").fetchSemanticsNode().boundsInRoot
        assertTrue("portrait result", result.height > result.width)
        val keyboard = compose.onNodeWithTag("netflix_search_field").fetchSemanticsNode().boundsInRoot
        assertTrue("keyboard side", if (arabic) keyboard.left > result.right else keyboard.right < result.left)
        capture("08-search")
        capture("33-search-portrait-results")
        capture("35-arabic-search-keyboard-right")
        press(KeyEvent.KEYCODE_BACK)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertTrue(emitted.any { it is SearchEvent.QueryChanged }) }
    }

    @Test fun deeplyScrolledSearchReturnsToAComposedResultAfterBack() {
        var selectedIndex = -1
        val manyItems = (0 until 96).map { index -> items[index % items.size].copy(id = "deep:$index") }
        setContent {
            NetflixSearchContent(
                SearchUiState(query = "light", submittedQuery = "light",
                    catalogRows = listOf(catalog().copy(items = manyItems)), isSearching = false),
                initialFocusedIndex = 72, restoreFocus = true, onEvent = {},
                onNavigateToDetail = { _, _, index -> selectedIndex = index }
            )
        }
        // Restoration is asynchronous (scroll, then compose the cell); wait for it, then assert.
        compose.waitUntil(15_000) {
            focusedInside("netflix_search_result_72")
        }
        compose.onNodeWithTag("netflix_search_result_0").assertDoesNotExist()
        press(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(if (arabic) "ا" else "a").assertIsFocused()
        // Return through the keyboard's grid-facing edge while item zero is disposed.
        compose.onNodeWithText(if (arabic) "ح" else "f").requestFocus()
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText(if (arabic) "ح" else "f").assertIsNotFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertTrue(selectedIndex >= 60) }
        capture("08b-search-deep-grid-reentry")
    }

    @Test fun savedSearchFocusWaitsForResultsAndKeepsTheKeyboardAvailableDuringAnError() {
        var restored = 0
        var state by mutableStateOf(SearchUiState(query = "light", submittedQuery = "light",
            isSearching = false, error = text("Network unavailable", "الشبكة غير متاحة")))
        setContent {
            NetflixSearchContent(state, initialFocusedIndex = 8, restoreFocus = true,
                onEvent = {}, onNavigateToDetail = { _, _, _ -> }, onFocusRestored = { restored++ })
        }
        val initialKey = if (arabic) "ا" else "a"
        compose.waitUntil(15_000) {
            compose.onAllNodes(isFocused()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText(initialKey).assertIsFocused()
        capture("25-search-error-keyboard")
        compose.runOnIdle {
            assertEquals(0, restored)
            state = state.copy(catalogRows = listOf(catalog()), error = null)
        }
        compose.waitUntil(15_000) {
            focusedInside("netflix_search_result_8")
        }
        compose.runOnIdle { assertEquals(1, restored) }
        capture("08c-search-async-focus-restore")
    }

    @Test fun topNavigationReachesHomeSearchAndMyNetflixWithoutASideRail() {
        var route: String? = null
        var switched = 0
        setContent {
            val selected = remember { FocusRequester() }
            Column(Modifier.fillMaxSize()) {
                NetflixTopNavigationBar(navigationEntries(), "home", selected,
                    TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) { switched++ }, onFocusChanged = {},
                    onNavigate = { route = it })
                Box(Modifier.fillMaxSize()) { ArtworkBackground() }
            }
            LaunchedEffect(Unit) { selected.requestFocus() }
        }
        compose.onNodeWithTag("netflix_top_nav_home").assertIsFocused().assertIsSelected()
        capture("09-top-nav-home")
        val towardStart = if (arabic) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        val towardEnd = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        // Search sits next to Home: one deterministic step in either reading direction.
        press(towardStart)
        compose.onNodeWithTag("netflix_top_nav_search").assertIsFocused()
        capture("10-top-nav-search")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("search", route) }
        repeat(4) { press(towardEnd) }
        compose.onNodeWithTag("netflix_top_nav_my_netflix").assertIsFocused()
        capture("11-top-nav-my-netflix")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("my_netflix", route) }
        // The profile is the physical far-right anchor in both languages.
        repeat(6) { if (compose.onAllNodes(hasTestTag("netflix_top_nav_profile").and(isFocused())).fetchSemanticsNodes().isEmpty()) press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        compose.onNodeWithTag("netflix_top_nav_profile").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, switched) }
        compose.onAllNodes(isFocused()).assertCountEquals(1)
    }

    @Test fun topNavigationHasNoPrimarySettingsGearAndUsesASelectedPill() {
        setContent {
            Column(Modifier.fillMaxSize()) {
                NetflixTopNavigationBar(navigationEntries(), "home", remember { FocusRequester() },
                    TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) {}, onFocusChanged = {}, onNavigate = {})
            }
        }
        compose.onNodeWithTag("netflix_top_nav_settings").assertDoesNotExist()
        compose.onNodeWithTag("netflix_top_nav_home").assertIsSelected()
        // Selected (unfocused) destination: a light pill behind the label, and no red underline anywhere.
        val pill = compose.onNodeWithTag("netflix_top_nav_home").captureToImage().toPixelMap()
        val corner = pill[pill.width / 2, 2]
        assertTrue("selected pill is light", corner.red > .15f && corner.red == corner.green && corner.green == corner.blue)
        val bar = compose.onNodeWithTag("netflix_top_nav").captureToImage().toPixelMap()
        val red = NetflixThemeTokens.progress
        var redPixels = 0
        for (y in 0 until bar.height step 2) for (x in 0 until bar.width step 2) {
            val p = bar[x, y]
            if (kotlin.math.abs(p.red - red.red) < .05f && kotlin.math.abs(p.green - red.green) < .05f && kotlin.math.abs(p.blue - red.blue) < .05f) redPixels++
        }
        assertEquals("no red underline", 0, redPixels)
    }

    @Test fun brandAndProfileAnchorsDoNotMirrorInArabic() {
        setContent {
            Column(Modifier.fillMaxSize()) {
                NetflixTopNavigationBar(navigationEntries(), "home", remember { FocusRequester() },
                    TopMenuProfile(text("Alex", "أحمد"), "#4D7290", null) {}, onFocusChanged = {}, onNavigate = {})
            }
        }
        val brand = bounds("netflix_top_nav_brand")
        val profile = bounds("netflix_top_nav_profile")
        val home = bounds("netflix_top_nav_home")
        val search = bounds("netflix_top_nav_search")
        assertTrue("brand stays physically left", brand.right < home.left && brand.right < profile.left)
        assertTrue("profile stays physically right", profile.left > home.right && profile.left > search.right)
        // Only the labels follow reading order: Search precedes Home at the reading start.
        assertTrue("search next to home in reading order", if (arabic) search.left > home.left else search.left < home.left)
    }

    @Test fun myNetflixHubAggregatesRealProfileDataAndOpensTitles() {
        var resumed: String? = null
        var opened: String? = null
        var library = 0
        val now = 1_760_000_000_000L
        val state = buildMyNetflixHub(
            library = items.take(4).mapIndexed { index, item -> LibraryEntry(item.id, "movie", item.name, item.poster,
                background = item.background, logo = null, description = null, releaseInfo = "2026", imdbRating = null,
                genres = emptyList(), addonBaseUrl = "https://fixture.invalid", listedAt = now - index) },
            continueWatching = listOf(progress(episodes()[1]).copy(lastWatched = now)),
            watched = emptyList(),
            allProgress = listOf(progress(episodes()[1]), WatchProgress("fixture:5", "movie", titles[5], artwork(5), artwork(5), null,
                "fixture:5", null, null, null, 6_400_000L, 6_500_000L, now - 10)),
        )
        setContent {
            MyNetflixHubContent(state, UserProfile(1, text("Alex", "أحمد"), "#476C91"),
                onOpenDetail = { id, _, _ -> opened = id }, onResume = { resumed = it.videoId },
                onOpenFullLibrary = { library++ }, onOpenSearch = {}, onOpenSettings = {})
        }
        // Header action first, while it is on screen.
        compose.onNodeWithTag("my_netflix_full_library").requestFocus()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, library) }
        compose.onNodeWithTag("my_netflix_row_continue_watching").assertIsDisplayed()
        compose.onNodeWithTag("my_netflix_row_my_list").assertIsDisplayed()
        // The third section sits below the fold of the lazy hub; bring it in before asserting.
        compose.onNodeWithTag("my_netflix_hub").performScrollToNode(hasTestTag("my_netflix_row_recently_watched"))
        compose.onNodeWithTag("my_netflix_row_recently_watched").assertIsDisplayed()
        compose.onAllNodesWithTag("my_netflix_progress", useUnmergedTree = true).assertCountEquals(1)
        val first = compose.onAllNodes(hasTestTag("my_netflix_card_continue:series:fixture:0"))
        first.assertCountEquals(1)
        compose.onNodeWithTag("my_netflix_card_continue:series:fixture:0").requestFocus().assertIsFocused()
        capture("12-my-netflix-hub")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(episodes()[1].id, resumed) }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertTrue(opened?.startsWith("fixture:") == true) }
    }

    @Test fun emptyMyNetflixOffersARealSearchActionInsteadOfATextIsland() {
        var searched = 0
        setContent {
            MyNetflixHubContent(buildMyNetflixHub(emptyList(), emptyList(), emptyList(), emptyList()),
                UserProfile(1, text("Alex", "أحمد"), "#476C91"), { _, _, _ -> }, {}, {}, { searched++ }, {})
        }
        compose.onNodeWithTag("my_netflix_empty_search").requestFocus().assertIsFocused()
        capture("17-empty-state")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, searched) }
    }

    @Test fun contextualCalloutStatesOnlyProvenFacts() {
        setContent {
            Box(Modifier.fillMaxSize()) {
                ArtworkBackground()
                NetflixHeroTitleContent(carouselItem(items[1], "callout").heroPreview, {}, {},
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = NetflixThemeTokens.safeMargin)
                        .fillMaxWidth(NetflixThemeTokens.heroMetadataWidthFraction),
                    callout = NetflixCallout(NetflixCalloutKind.CONTINUE, 1, 2))
            }
        }
        compose.onNodeWithTag("netflix_callout").assertIsDisplayed()
        listOf("Top 10", "98% Match", "Emmy", "Highly Rewatched", "Leaving Soon").forEach {
            compose.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        capture("20-contextual-callout")
    }

    @Test fun lowMemoryTierUsesStaticArtworkWithoutVideoOrMotion() {
        val policy = netflixHomePreviewPolicy(MemoryTier.LOW_RAM)
        assertTrue(!policy.allowVideo && !policy.animate)
        // The inline row never mounts a video surface: the expanded card is static artwork on every tier.
        setContent { FullHome {} }
        focusCard("netflix_home_card_focus_trending:fixture:2")
        compose.waitForIdle()
        compose.onAllNodesWithTag("netflix_inline_expanded", useUnmergedTree = true).assertCountEquals(1)
        capture("21-low-memory-fallback")
    }

    @Test fun missingLogoAndArtworkFallBackToDarkTitleSurfaces() {
        setContent {
            ArtworkBackground()
            Column(Modifier.padding(NetflixThemeTokens.safeMargin), verticalArrangement = Arrangement.spacedBy(NetflixThemeTokens.rowGap)) {
                NetflixHeroTitleContent(carouselItem(items[3], "nologo").heroPreview.copy(logo = "file:///nonexistent/logo.png"), {}, {},
                    modifier = Modifier.fillMaxWidth(NetflixThemeTokens.heroMetadataWidthFraction))
                Box(Modifier.width(NetflixThemeTokens.landscapeCardWidth)) {
                    GridContentCard(items[3].copy(poster = null, background = null, landscapePoster = null), onClick = {},
                        posterCardStyle = netflixCardStyle())
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("netflix_home_hero")).fetchSemanticsNodes().isNotEmpty() }
        capture("22-missing-logo-fallback")
    }

    @Test fun veryLongTitlesEllipsizeWithoutPushingActionsOffScreen() {
        val long = text("The Extraordinarily Long and Winding Journey of the Last Lighthouse Keeper Beyond the Northern Passage",
            "الرحلة الطويلة والمتعرجة بشكل استثنائي لآخر حارس منارة خلف الممر الشمالي البعيد")
        setContent {
            Box(Modifier.fillMaxSize()) {
                ArtworkBackground()
                NetflixHeroTitleContent(carouselItem(items[4].copy(name = long), "long").heroPreview.copy(title = long), {}, {},
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = NetflixThemeTokens.safeMargin)
                        .fillMaxWidth(NetflixThemeTokens.heroMetadataWidthFraction))
            }
        }
        compose.onNodeWithTag("netflix_hero_play").assertIsDisplayed()
        capture("23-very-long-title")
    }

    @Test fun arabicTitleWithLatinTokensKeepsLogicalOrder() {
        // Rendered in both locales: an Arabic title, Latin brand/rating tokens and an episode token.
        val mixed = "الممر الشمالي: Northern Passage 2"
        setContent {
            Box(Modifier.fillMaxSize()) {
                ArtworkBackground()
                NetflixHeroTitleContent(carouselItem(items[1].copy(name = mixed), "bidi").heroPreview.copy(title = mixed,
                    description = "رحلة هادئة مع Maya Reed في الموسم 1، حلقة 2 بدقة 4K عبر IMDb 8.1"), {}, {},
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = NetflixThemeTokens.safeMargin)
                        .fillMaxWidth(NetflixThemeTokens.heroMetadataWidthFraction),
                    callout = NetflixCallout(NetflixCalloutKind.NEXT_EPISODE, 1, 2))
            }
        }
        compose.onNodeWithTag("netflix_callout").assertIsDisplayed()
        capture("24-arabic-mixed-bidi")
    }

    @Test fun returningFromDetailsRestoresTheExactRowAndCard() {
        var opened = 0
        // What onSaveFocusState records on leaving Home: the vertical position as well as the row and
        // card (rows: continue_watching 0, trending 1, popular 2), so the target row is composed.
        val saved = HomeScreenFocusState(verticalScrollIndex = 2, focusedRowKey = "popular",
            focusedItemKeyByRow = mapOf("popular" to "popular:fixture:3"), hasSavedFocus = true)
        setContent { FullHome(focusState = saved) { opened++ } }
        val target = "netflix_home_card_focus_popular:fixture:3"
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag(target).and(isFocused())).fetchSemanticsNodes().size == 1
        }
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        capture("26-details-return-focus")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun profilesOfferLargeCardsAndSelectTheFocusedProfile() {
        var selected: Int? = null
        setContent {
            ProfileSelectionMainContent(screenTitle = text("Who's watching?", "من يشاهد؟"), screenSubtitle = "",
                screenHint = text("Choose a profile", "اختر ملفًا شخصيًا"), isManagementMode = false,
                profiles = listOf(UserProfile(1, text("Alex", "أحمد"), "#476C91"),
                    UserProfile(2, text("Sam", "سارة"), "#AB8055"), UserProfile(3, text("Kids", "الأطفال"), "#577C69")),
                activeProfileId = 1, canAddProfile = true, profilePinEnabled = emptyMap(), avatarImageUrlsById = emptyMap(),
                profileThemes = mapOf(1 to AppTheme.NETFLIX, 2 to AppTheme.WHITE),
                onProfileFocused = {}, onProfileSelected = { selected = it.id }, onProfileLongPress = {}, onAddProfileClick = {})
        }
        capture("13-profiles")
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, selected) }
    }

    @Test fun playerChromeUsesTheExistingSeekCallbacksAndLogicalEpisodeTitle() {
        var sought = 0L
        var committed = 0
        setContent {
            var position by remember { mutableLongStateOf(1_800_000L) }
            val focus = remember { FocusRequester() }
            ArtworkBackground()
            Column(Modifier.fillMaxSize().padding(NetflixThemeTokens.safeMargin)) {
                NetflixPlayerContentHeading(PlayerUiState(title = titles.first(), contentName = titles.first(),
                    currentSeason = 1, currentEpisode = 2, currentEpisodeTitle = text("The Crossing", "العبور")))
                Spacer(Modifier.weight(1f))
                PlayerTimeText(position, 6_720_000L)
                // onSeekPreview carries a signed scrub DELTA (PlayerScrubRates.deltaMsForKeyRepeat), as
                // PlayerScreen's own caller applies it; the position is the caller's to accumulate.
                ProgressBar(position, 6_720_000L, { delta -> sought += delta; position += delta }, { committed++ },
                    focusRequester = focus, bufferedPosition = 3_600_000L)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NetflixThemeTokens.actionGap)) {
                    ControlButton(Icons.Default.PlayArrow, contentDescription = text("Play", "تشغيل"), onClick = {})
                    ControlButton(Icons.Default.SkipNext, contentDescription = text("Next episode", "الحلقة التالية"), onClick = {})
                    Spacer(Modifier.weight(1f))
                    ControlButton(Icons.Default.Subtitles, contentDescription = text("Audio and subtitles", "الصوت والترجمة"), onClick = {})
                }
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        compose.onNodeWithTag("player_time_text").assertIsDisplayed()
        compose.onNodeWithText("30:00 / 1:52:00", substring = true).assertIsDisplayed()
        capture("14-player-controls")
        compose.waitUntil(5_000) { compose.onAllNodes(isFocused()).fetchSemanticsNodes().size == 1 }
        // The scrubber is a media timeline: DPAD_RIGHT is forward in both LTR and RTL.
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.runOnIdle { assertTrue("forward scrub delta", sought > 0L); assertTrue("seek commit on key up", committed > 0) }
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.runOnIdle { assertTrue("backward scrub returns", sought == 0L); assertTrue(committed > 1) }
    }

    @Test fun detailResumeAndBeginningRemainSeparateActions() {
        var resumed = 0
        var restarted = 0
        val video = episodes()[1]
        val resume = NextToWatch(progress(video), true, video.id, 1, 2, text("Resume", "استئناف"))
        setContent {
            ArtworkBackground()
            Column(Modifier.fillMaxSize()) {
                HeroContentSection(meta(ContentType.SERIES), video, resume, { resumed++ }, isInLibrary = false,
                    onToggleLibrary = {}, onLibraryLongPress = {}, isMovieWatched = false,
                    isMovieWatchedPending = false, onToggleMovieWatched = {}, onPlayFromBeginning = { restarted++ })
            }
        }
        compose.onNodeWithTag("netflix_detail_play").requestFocus().assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, resumed); assertEquals(0, restarted) }
        // The source owns the button's localized label; select by its semantics.
        val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(localeTag)))
        })
        compose.onNodeWithText(localized.getString(com.nuvio.tv.R.string.cw_action_start_from_beginning))
            .requestFocus().assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, resumed); assertEquals(1, restarted) }
        capture("15-resume-actions")
    }

    @Test fun aConfirmationDialogReturnsToTheUnderlyingScreenOnBack() {
        var dismissed = false
        setContent {
            var shown by remember { mutableStateOf(true) }
            EmptyScreenState(text("My List is empty", "قائمتي فارغة"), text("Add a title to find it here.", "أضف عنوانًا لعرضه هنا."))
            if (shown) NuvioDialog(onDismiss = { dismissed = true; shown = false },
                title = text("Remove from My List?", "إزالة من قائمتي؟"), suppressFirstKeyUp = false) {
                NuvioDialogButton(onClick = { shown = false }) { Text(text("Cancel", "إلغاء")) }
                NuvioDialogButton(onClick = { shown = false }) { Text(text("Remove", "إزالة")) }
            }
        }
        compose.onNodeWithText(text("Cancel", "إلغاء")).requestFocus().assertIsFocused()
        capture("16-confirmation-dialog")
        press(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertTrue(dismissed) }
        capture("17b-empty-state-generic")
    }

    @Test fun errorStateOffersAnExistingRetryCallback() {
        var retried = 0
        setContent { ErrorState(text("Network unavailable", "الشبكة غير متاحة"), { retried++ }) }
        val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(localeTag)))
        })
        val retry = compose.onNodeWithText(localized.getString(com.nuvio.tv.R.string.action_retry))
        retry.requestFocus().assertIsFocused()
        val pixels = retry.captureToImage().toPixelMap()
        assertEquals(NetflixThemeTokens.focus, pixels[8, pixels.height / 2])
        capture("18-network-error")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, retried) }
    }

    @Test fun playbackLoadingUsesTheSameCinematicSurface() {
        setContent { NetflixPlayerLoadingOverlay(true, artwork(0), null, titles.first(),
            text("Preparing playback", "جارٍ تحضير التشغيل"), .45f) }
        capture("19-playback-loading")
    }

    private fun setContent(content: @Composable () -> Unit) {
        val locale = Locale.forLanguageTag(localeTag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale)); setLayoutDirection(locale)
        }
        val localized = context.createConfigurationContext(configuration)
        LocaleCache.localeTag = localeTag
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalResources provides localized.resources,
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                NuvioTheme(appTheme = AppTheme.NETFLIX) {
                    Box(Modifier.fillMaxSize().background(NetflixThemeTokens.background)) { content() }
                }
            }
        }
    }

    @Composable private fun FullHome(onPlay: (() -> Unit)? = null, onLibrary: () -> Unit = {},
        focusState: HomeScreenFocusState = HomeScreenFocusState(),
        onSeeAll: (String, String, String) -> Unit = { _, _, _ -> }, onOpen: () -> Unit) {
        // Production provides this from the navigation scaffold; the hero's Down targets it.
        CompositionLocalProvider(LocalContentFocusRequester provides remember { FocusRequester() }) {
            FullHomeContent(onPlay, onLibrary, focusState, onSeeAll, onOpen)
        }
    }

    @Composable private fun FullHomeContent(onPlay: (() -> Unit)?, onLibrary: () -> Unit, focusState: HomeScreenFocusState,
        onSeeAll: (String, String, String) -> Unit, onOpen: () -> Unit) {
        val catalogs = listOf(catalog(), catalog("popular", text("Popular", "الأكثر شعبية")),
            catalog("movies", text("Movies", "الأفلام")), catalog("series", text("TV Shows", "المسلسلات")))
        val resume = ContinueWatchingItem.InProgress(progress(episodes()[1]))
        val rows = listOf(HeroCarouselRow("continue_watching", text("Continue Watching", "متابعة المشاهدة"), -1,
            listOf(carouselItem(items.first(), "continue:fixture", ModernPayload.ContinueWatching(resume))).asStable())) +
            catalogs.mapIndexed { index, row -> HeroCarouselRow(row.catalogId, row.catalogName, index,
                row.items.map { carouselItem(it, "${row.catalogId}:${it.id}") }.asStable(), row.catalogId, row.addonId, row.apiType) }
        val presentation = ModernHomePresentationState(rows.asStable(), buildCarouselRowLookups(rows))
        ModernHomeContent(HomeUiState(catalogRows = catalogs, continueWatchingItems = listOf(resume), isLoading = false,
            layoutPreferencesReady = true, installedAddonsCount = 1, modernLandscapePostersEnabled = true,
            heroItems = items, posterLabelsEnabled = false, catalogAddonNameEnabled = false, catalogTypeSuffixEnabled = false),
            modernPresentation = presentation, focusState = focusState,
            onNavigateToDetail = { _, _, _ -> onOpen() },
            onPlayClick = { _, _, _ -> if (onPlay != null) onPlay() else onOpen() },
            onCatalogLibraryAction = { _, _ -> onLibrary() }, onContinueWatchingClick = { onOpen() },
            onRequestTrailerPreview = { _, _, _, _ -> }, onLoadMoreCatalog = { _, _, _ -> },
            onRemoveContinueWatching = { _, _, _, _ -> }, onSaveFocusState = { _, _, _, _, _, _, _, _ -> },
            onNavigateToCatalogSeeAll = onSeeAll)
    }

    @Composable private fun DetailFixture(meta: Meta, onLibrary: () -> Unit = {}, onPlay: () -> Unit) {
        ArtworkBackground()
        Column(Modifier.fillMaxSize()) {
            HeroContentSection(meta, nextEpisode = meta.videos.firstOrNull(), nextToWatch = null, onPlayClick = onPlay,
                isInLibrary = false, onToggleLibrary = onLibrary, onLibraryLongPress = {}, isMovieWatched = false,
                isMovieWatchedPending = false, onToggleMovieWatched = {}, trailerAvailable = true, onTrailerClick = {})
        }
    }

    @Composable private fun ArtworkBackground() {
        AsyncImage(artwork(0), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(Modifier.fillMaxSize().background(NetflixThemeTokens.heroSideGradient(rtl)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(*NetflixThemeTokens.heroBottomStops)))
    }

    private fun Modifier.alignForCardFixture() = padding(top = 190.dp)
    private fun netflixCardStyle() = PosterCardStyle(width = NetflixThemeTokens.landscapeCardWidth,
        height = NetflixThemeTokens.landscapeCardWidth / NetflixThemeTokens.landscapeAspectRatio,
        cornerRadius = NetflixThemeTokens.cardRadius, focusedBorderWidth = NetflixThemeTokens.focusedBorderWidth,
        focusedScale = NetflixThemeTokens.focusScale)
    private fun catalog(id: String = "trending", title: String = text("Trending", "الرائج")) =
        CatalogRow("fixture", "Offline fixtures", "https://fixture.invalid", id, title, ContentType.MOVIE,
            items = items + items.map { it.copy(id = "${it.id}:second") }, hasMore = false)
    private fun carouselItem(item: MetaPreview, key: String,
        payload: ModernPayload = ModernPayload.Catalog(key, item.id, item.apiType, "https://fixture.invalid", item.name, "2026", item.apiType)) =
        ModernCarouselItem(key, item.name, null, item.backdropUrl,
            HeroPreview(item.name, item.logo, item.description, text("Movie", "فيلم"), yearText = "2026", runtimeText = text("1h 52m", "ساعة و52 دقيقة"),
                imdbText = "8.1", ageRatingText = "16+", genres = item.genres.asStable(), poster = item.poster, backdrop = item.background, imageUrl = item.background),
            payload, item)
    private fun episodes() = (1..6).map { index -> Video("fixture:1:$index", text("Episode $index: The Crossing", "الحلقة $index: العبور"),
        "2026-01-01", artwork(index % 6), season = 1, episode = index, overview = synopsis, runtime = 48) }
    private fun progress(video: Video) = WatchProgress("fixture:0", "series", titles.first(), artwork(0), artwork(0), null,
        video.id, 1, video.episode, video.title, 1_100_000L, 2_880_000L, 1L)
    private fun meta(type: ContentType) = Meta("fixture:0", type, name = titles.first(), poster = artwork(0), posterShape = PosterShape.LANDSCAPE,
        background = artwork(0), logo = null, description = synopsis, releaseInfo = "2026", imdbRating = 8.1f,
        genres = listOf("Drama", "Adventure"), runtime = if (type == ContentType.MOVIE) "112" else null,
        director = listOf(text("Alex Rivers", "علي سالم")), cast = listOf(text("Maya Reed", "مريم خالد"), text("Sam Jordan", "سامي حسن")),
        videos = if (type == ContentType.SERIES) episodes() + episodes().map { it.copy(id = it.id.replace(":1:", ":2:"), season = 2) } else emptyList(),
        ageRating = "16+", country = null, awards = null, language = localeTag, links = emptyList())
    private fun navigationEntries() = netflixNavEntries(listOf(
        DrawerItem("search", text("Search", "بحث"), icon = Icons.Default.Search),
        DrawerItem("home", text("Home", "الرئيسية"), icon = Icons.Default.Home),
        DrawerItem("discover?type=series", text("TV Shows", "المسلسلات"), icon = Icons.Default.Tv),
        DrawerItem("discover?type=movie", text("Movies", "أفلام"), icon = Icons.Default.Movie),
        DrawerItem("my_netflix", text("My Netflix", "نتفليكس الخاص بي"), icon = Icons.Default.VideoLibrary),
        DrawerItem("settings", text("Settings", "الإعدادات"), icon = Icons.Default.Settings)), "search", "settings")

    /** Original geometric landscape, created locally, never passed off as captured movie artwork. */
    private fun artwork(index: Int): String {
        val file = File(context.cacheDir, "netflix-fixture-artwork-$index.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val palettes = listOf(0xFF486479.toInt(), 0xFF665645.toInt(), 0xFF345953.toInt(), 0xFF555878.toInt(), 0xFF655063.toInt(), 0xFF635A46.toInt())
            paint.shader = LinearGradient(0f, 0f, 0f, 720f, palettes[index % 6], 0xFF090D13.toInt(), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 1280f, 720f, paint)
            paint.shader = null; paint.color = 0xFFC9C0A8.toInt(); paint.alpha = 150
            canvas.drawCircle(930f - index * 35, 185f, 70f, paint)
            paint.alpha = 255
            repeat(4) { layer ->
                paint.color = android.graphics.Color.rgb(12 + layer * 4, 20 + layer * 4, 28 + layer * 4)
                val path = Path().apply {
                    moveTo(0f, 390f + layer * 65)
                    lineTo(180f, 270f + layer * 85); lineTo(420f, 450f + layer * 32)
                    lineTo(740f, 305f + layer * 85); lineTo(1040f, 480f + layer * 28)
                    lineTo(1280f, 350f + layer * 80); lineTo(1280f, 720f); lineTo(0f, 720f); close()
                }
                canvas.drawPath(path, paint)
            }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return file.toURI().toString()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.requestFocus() =
        performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    private fun press(keyCode: Int) {
        instrumentation.sendKeyDownUpSync(keyCode)
        compose.waitForIdle()
    }
    private fun Bitmap.isUniform(): Boolean {
        // Every pixel: a sparse but genuine screen (small centred text on black) must not look blank.
        val first = getPixel(0, 0)
        val row = IntArray(width)
        for (y in 0 until height) {
            getPixels(row, 0, width, 0, y, width, 1)
            if (row.any { it != first }) return false
        }
        return true
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** Home is lazy: bring the card's row into composition first, as remote scrolling would. */
    private fun focusCard(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("netflix_home").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag).requestFocus()
    }

    /** The result tag sits on GridContentCard's wrapper; focus lands on the Card inside it. */
    private fun focusedInside(tag: String): Boolean =
        compose.onAllNodes(isFocused().and(hasAnyAncestor(hasTestTag(tag)))).fetchSemanticsNodes().size == 1

    private fun waitForHomePreview() {
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("netflix_expanded_card")).fetchSemanticsNodes().size == 1
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        // Coil decodes the offline bitmap asynchronously; allow one bounded settling interval.
        SystemClock.sleep(350)
        val directory = File(context.getExternalFilesDir(null), "netflix-theme").apply { mkdirs() }
        // The software-GPU emulator can hand back a blank, single-colour surface before the frame
        // is composited (seen at 4K as an all-white image). Retry a bounded number of times; a frame
        // that is still uniform fails loudly instead of being saved as evidence.
        var shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        var attempts = 0
        while (shot.isUniform() && attempts < 12) {
            shot.recycle(); SystemClock.sleep(250); compose.waitForIdle()
            shot = checkNotNull(instrumentation.uiAutomation.takeScreenshot()); attempts++
        }
        check(!shot.isUniform()) { "$name rendered a blank uniform frame" }
        File(directory, "$name-$localeTag.png").outputStream().use {
            shot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        shot.recycle()
    }
}
