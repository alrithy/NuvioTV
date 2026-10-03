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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
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

    @Test fun homeHeroShowsMetadataAndPlayUsesTheExistingCallback() {
        var opened = 0
        setContent { FullHome { opened++ } }
        compose.onNodeWithTag("netflix_hero_play").requestFocus().assertIsFocused()
        capture("01-home-hero")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun homeRowsKeepContentReachableFromTheHero() {
        setContent { FullHome {} }
        compose.onNodeWithTag("netflix_hero_play").requestFocus().assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        // The hero's Down hands focus to the rows through the scaffold's content requester.
        compose.onNodeWithTag("netflix_hero_play").assertIsNotFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onAllNodes(isFocused()).assertCountEquals(1)
        capture("02-home-rows")
    }

    @Test fun focusedLandscapeCardMovesWithoutChangingItsLayoutBounds() {
        var clicked = -1
        setContent {
            val initial = remember { FocusRequester() }
            Row(Modifier.padding(horizontal = NetflixThemeTokens.safeMargin).alignForCardFixture(),
                horizontalArrangement = Arrangement.spacedBy(NetflixThemeTokens.cardGap)) {
                // GridContentCard is a grid cell (fillMaxWidth in landscape/Netflix mode); give each card the
                // bounded cell width a grid would, otherwise card 0 takes the Row and the rest measure 0 dp.
                items.take(5).forEachIndexed { index, item ->
                    Box(Modifier.width(NetflixThemeTokens.landscapeCardWidth)) {
                        GridContentCard(item, onClick = { clicked = index }, modifier = Modifier.testTag("fixture_card_$index"),
                            posterCardStyle = netflixCardStyle(), focusRequester = if (index == 1) initial else null)
                    }
                }
            }
            LaunchedEffect(Unit) { initial.requestFocus() }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(isFocused()).fetchSemanticsNodes().size == 1 }
        val before = compose.onNodeWithTag("fixture_card_1").fetchSemanticsNode().boundsInRoot
        val neighbour = compose.onNodeWithTag("fixture_card_2").fetchSemanticsNode().boundsInRoot
        assertTrue("cards have real width", before.width > 0f && neighbour.width > 0f)
        capture("03-focused-card")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, clicked) }
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        val after = compose.onNodeWithTag("fixture_card_1").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        assertEquals(neighbour, compose.onNodeWithTag("fixture_card_2").fetchSemanticsNode().boundsInRoot)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(2, clicked) }
    }

    @Test fun expandedPreviewExposesPlayListAndInformationToTheRemote() {
        var played = 0
        var listed = 0
        var informed = 0
        setContent {
            val focus = remember { FocusRequester() }
            ArtworkBackground()
            Box(Modifier.padding(NetflixThemeTokens.safeMargin)) {
                NetflixExpandedCardContent(carouselItem(items.first(), "fixture"), { played++ }, { listed++ }, { informed++ },
                    playFocusRequester = focus)
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        compose.onNodeWithTag("netflix_card_play").assertIsFocused()
        capture("04-expanded-card")
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("netflix_card_library").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("netflix_card_more_info").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(0, played); assertEquals(1, listed); assertEquals(1, informed) }
    }

    @Test fun homeDwellPreviewRestoresItsAnchorAndAllowsRepeatedRemoteNavigation() {
        var played = 0
        var listed = 0
        var informed = 0
        setContent { FullHome(onPlay = { played++ }, onLibrary = { listed++ }) { informed++ } }
        compose.onNodeWithTag("netflix_hero_play").requestFocus()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        val firstAnchor = "netflix_home_card_focus_trending:fixture:0"
        compose.onNodeWithTag(firstAnchor).requestFocus()
        waitForHomePreview()
        compose.onNodeWithTag("netflix_card_play").assertIsFocused()
        capture("04b-expanded-card-in-home")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, played) }
        val towardNext = if (arabic) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        press(towardNext)
        compose.onNodeWithTag("netflix_card_library").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, listed) }
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag(firstAnchor).and(isFocused())).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("netflix_expanded_card").assertDoesNotExist()
        press(towardNext)
        waitForHomePreview()
        compose.onNodeWithTag("netflix_card_play").assertIsFocused()
        press(towardNext)
        press(towardNext)
        compose.onNodeWithTag("netflix_card_more_info").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, informed) }
        press(KeyEvent.KEYCODE_BACK)
        val secondAnchor = "netflix_home_card_focus_trending:fixture:1"
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag(secondAnchor).and(isFocused())).fetchSemanticsNodes().size == 1
        }
        // Three remote presses before a new dwell; a Popup must not trap row movement.
        repeat(3) { instrumentation.sendKeyDownUpSync(towardNext) }
        compose.waitForIdle()
        compose.onNodeWithTag("netflix_home_card_focus_trending:fixture:4").assertIsFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
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
        capture("08-search")
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
        // One deterministic step from Home to Search, in either direction of reading.
        press(towardStart)
        compose.onNodeWithTag("netflix_top_nav_search").assertIsFocused()
        capture("10-top-nav-search")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("search", route) }
        press(towardStart)
        compose.onNodeWithTag("netflix_top_nav_profile").assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, switched) }
        repeat(5) { press(towardEnd) }
        compose.onNodeWithTag("netflix_top_nav_my_netflix").assertIsFocused()
        capture("11-top-nav-my-netflix")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("my_netflix", route) }
        // Settings stays reachable as the trailing, low-emphasis entry rather than a content tab.
        press(towardEnd)
        compose.onNodeWithTag("netflix_top_nav_settings").assertIsFocused()
        compose.onAllNodes(isFocused()).assertCountEquals(1)
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
        setContent {
            ArtworkBackground()
            Box(Modifier.padding(NetflixThemeTokens.safeMargin)) {
                // Exactly what the row passes on this tier: no trailer URL, unscaled card.
                NetflixExpandedCardContent(carouselItem(items[2], "low"), {}, {}, {},
                    width = NetflixThemeTokens.landscapeCardWidth * policy.expandedScale, trailerPreviewUrl = null)
            }
        }
        compose.onNodeWithTag("netflix_expanded_card").assertIsDisplayed()
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
        focusState: HomeScreenFocusState = HomeScreenFocusState(), onOpen: () -> Unit) {
        // Production provides this from the navigation scaffold; the hero's Down targets it.
        CompositionLocalProvider(LocalContentFocusRequester provides remember { FocusRequester() }) {
            FullHomeContent(onPlay, onLibrary, focusState, onOpen)
        }
    }

    @Composable private fun FullHomeContent(onPlay: (() -> Unit)?, onLibrary: () -> Unit, focusState: HomeScreenFocusState, onOpen: () -> Unit) {
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
            onRemoveContinueWatching = { _, _, _, _ -> }, onSaveFocusState = { _, _, _, _, _, _, _, _ -> })
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
        val first = getPixel(0, 0)
        val stepX = (width / 16).coerceAtLeast(1)
        val stepY = (height / 9).coerceAtLeast(1)
        for (y in 0 until height step stepY) for (x in 0 until width step stepX) if (getPixel(x, y) != first) return false
        return true
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
