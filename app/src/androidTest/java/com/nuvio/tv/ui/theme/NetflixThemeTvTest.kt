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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocaleCache
import com.nuvio.tv.ModernSidebarBlurPanel
import com.nuvio.tv.NetflixSidebarRail
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
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.ProgressBar
import com.nuvio.tv.ui.screens.player.NetflixPlayerLoadingOverlay
import com.nuvio.tv.ui.screens.profile.ProfileSelectionMainContent
import com.nuvio.tv.ui.screens.search.NetflixSearchContent
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchUiState
import com.nuvio.tv.ui.util.asStable
import dev.chrisbanes.haze.HazeState
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
        capture("04-expanded-card-in-home")
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
        compose.onNodeWithTag("netflix_search_result_72").assertIsFocused()
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
        capture("17-search-deep-grid-reentry")
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
        compose.runOnIdle {
            assertEquals(0, restored)
            state = state.copy(catalogRows = listOf(catalog()), error = null)
        }
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("netflix_search_result_8").and(isFocused())).fetchSemanticsNodes().size == 1
        }
        compose.runOnIdle { assertEquals(1, restored) }
        capture("18-search-async-focus-restore")
    }

    @Test fun navigationShowsCollapsedAndExpandedStatesWithSingleRemoteActivation() {
        var route: String? = null
        val entries = navigationItems()
        setContent {
            val requesters = remember { entries.associate { it.route to FocusRequester() } }
            ArtworkBackground()
            Box(Modifier.width(NetflixThemeTokens.navigationExpandedWidth).fillMaxHeight()) {
                ModernSidebarBlurPanel(entries, "home", true, 1f, 1f, 1f, true, false, false,
                    remember { HazeState() }, RoundedCornerShape(0.dp), requesters, {}, { route = it },
                    text("Alex", "أحمد"), "#4D7290", null, false, {})
            }
            LaunchedEffect(Unit) { requesters.getValue("home").requestFocus() }
        }
        capture("09-navigation-expanded")
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals("search", route) }
    }

    @Test fun collapsedNavigationRemainsMinimalOverTheArtwork() {
        setContent {
            ArtworkBackground()
            NetflixSidebarRail(navigationItems(), "home", onExpand = {})
        }
        capture("09-navigation-collapsed")
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
        capture("10-profiles")
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
        capture("11-player-controls")
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
        capture("12-resume-actions")
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
        capture("13-confirmation-dialog")
        press(KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertTrue(dismissed) }
        capture("14-empty-list")
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
        capture("15-network-error")
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.runOnIdle { assertEquals(1, retried) }
    }

    @Test fun playbackLoadingUsesTheSameCinematicSurface() {
        setContent { NetflixPlayerLoadingOverlay(true, artwork(0), null, titles.first(),
            text("Preparing playback", "جارٍ تحضير التشغيل"), .45f) }
        capture("16-playback-loading")
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

    @Composable private fun FullHome(onPlay: (() -> Unit)? = null, onLibrary: () -> Unit = {}, onOpen: () -> Unit) {
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
            modernPresentation = presentation, focusState = HomeScreenFocusState(),
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
    private fun navigationItems() = listOf(DrawerItem("home", text("Home", "الرئيسية"), icon = Icons.Default.Home),
        DrawerItem("search", text("Search", "بحث"), icon = Icons.Default.Search),
        DrawerItem("movies", text("Movies", "أفلام"), icon = Icons.Default.Movie),
        DrawerItem("tv", text("TV Shows", "مسلسلات"), icon = Icons.Default.Tv),
        DrawerItem("library", text("My List", "قائمتي"), icon = Icons.Default.VideoLibrary),
        DrawerItem("settings", text("Settings", "الإعدادات"), icon = Icons.Default.Settings))

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
        File(directory, "$name-$localeTag.png").outputStream().use {
            checkNotNull(instrumentation.uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
