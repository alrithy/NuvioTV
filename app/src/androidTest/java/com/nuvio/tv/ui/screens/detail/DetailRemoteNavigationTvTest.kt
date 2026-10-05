package com.nuvio.tv.ui.screens.detail

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.LocaleList
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuvio.tv.LocaleCache
import com.nuvio.tv.domain.model.AppTheme
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.domain.model.MetaCompany
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.MetaTrailer
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.ui.theme.NuvioTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 regression for the TCL C6K Detail crash: the production Detail content (hero, seasons, episodes,
 * More like this, trailers, cast, collection, company rows) walked with real remote key events, one step
 * at a time and as held-down bursts with key repeats, in the Netflix and the classic presentation.
 * A crash anywhere in composition, layout or focus kills the instrumentation process and fails the run.
 */
@RunWith(AndroidJUnit4::class)
class DetailRemoteNavigationTvTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val localeTag get() = InstrumentationRegistry.getArguments().getString("netflix_locale", "en")
    private val arabic get() = localeTag == "ar"

    @Test fun netflixMovieDetailSurvivesStepwiseRemoteWalk() = walk(AppTheme.NETFLIX, movie(), burst = false)
    @Test fun netflixMovieDetailSurvivesHeldRemoteBursts() = walk(AppTheme.NETFLIX, movie(), burst = true)
    @Test fun netflixSeriesDetailSurvivesStepwiseRemoteWalk() = walk(AppTheme.NETFLIX, series(), burst = false)
    @Test fun netflixSeriesDetailSurvivesHeldRemoteBursts() = walk(AppTheme.NETFLIX, series(), burst = true)
    @Test fun netflixMovieWithoutCastSurvivesRemoteWalk() =
        walk(AppTheme.NETFLIX, movie().let { it.copy(meta = it.meta.copy(cast = emptyList(), castMembers = emptyList(), director = emptyList())) }, burst = true)
    @Test fun classicMovieDetailSurvivesHeldRemoteBursts() = walk(AppTheme.WHITE, movie(), burst = true)
    @Test fun classicSeriesDetailSurvivesHeldRemoteBursts() = walk(AppTheme.WHITE, series(), burst = true)

    private data class Fixture(val meta: Meta, val state: MetaDetailsUiState)

    private fun walk(theme: AppTheme, fixture: Fixture, burst: Boolean) {
        setContent(theme) { MetaDetailsContentForTest(fixture.meta, fixture.state) }
        compose.waitUntil(10_000) { compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty() }
        val horizontal = listOf(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT)
        repeat(3) { round ->
            // Down through every section, sweeping each row to its end and back, then all the way up.
            repeat(12) { level ->
                press(KeyEvent.KEYCODE_DPAD_DOWN, burst, count = if (burst) 3 else 1)
                horizontal.forEach { key -> press(key, burst, count = if (burst) 14 else 8) }
                log("theme=$theme round=$round level=$level")
            }
            press(KeyEvent.KEYCODE_DPAD_UP, burst, count = if (burst) 40 else 16)
            press(KeyEvent.KEYCODE_DPAD_DOWN, burst, count = if (burst) 40 else 16)
            press(KeyEvent.KEYCODE_DPAD_RIGHT, burst, count = if (burst) 20 else 8)
            press(KeyEvent.KEYCODE_DPAD_UP, burst, count = if (burst) 40 else 16)
        }
        compose.waitForIdle()
        assertTrue("Detail lost every focus owner after the remote walk",
            compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty())
    }

    /** Stepwise: one key per frame settle. Burst: a held key, auto-repeating every 33 ms like a TV remote. */
    private fun press(keyCode: Int, burst: Boolean, count: Int) {
        if (!burst) {
            repeat(count) {
                instrumentation.sendKeyDownUpSync(keyCode)
                compose.waitForIdle()
            }
            return
        }
        val downTime = SystemClock.uptimeMillis()
        repeat(count) { repeatCount ->
            inject(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, repeatCount))
            SystemClock.sleep(33)
        }
        inject(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
        compose.waitForIdle()
    }

    private fun inject(event: KeyEvent) {
        val sourced = KeyEvent.changeTimeRepeat(event, event.eventTime, event.repeatCount).also {
            it.source = InputDevice.SOURCE_DPAD
        }
        instrumentation.uiAutomation.injectInputEvent(sourced, false)
    }

    private fun log(message: String) = Log.i("DetailRemoteNavigation", message)

    private fun setContent(theme: AppTheme, content: @androidx.compose.runtime.Composable () -> Unit) {
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
                NuvioTheme(appTheme = theme) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) { content() }
                }
            }
        }
    }

    private fun text(english: String, arabicText: String) = if (arabic) arabicText else english

    private fun preview(index: Int, prefix: String) = MetaPreview(
        id = "$prefix:$index", type = ContentType.MOVIE, name = text("Related title $index", "عنوان مشابه $index"),
        poster = artwork(index), posterShape = PosterShape.POSTER, background = artwork(index), logo = null,
        description = text("A related story.", "قصة مشابهة."), releaseInfo = "2024", imdbRating = 7.2f,
        genres = listOf("Drama"), runtime = "101", ageRating = "13+", landscapePoster = artwork(index)
    )

    private fun baseMeta(type: ContentType, videos: List<Video>) = Meta(
        id = "tt-fixture-${type.name.lowercase()}", type = type, name = text("Desert Planet", "كوكب الصحراء"),
        poster = artwork(0), posterShape = PosterShape.POSTER, background = artwork(1), logo = null,
        description = text(
            "A long synopsis that is deliberately longer than three lines on a television so the Read more " +
                "affordance becomes focusable in the hero, exactly as it does for real titles with full overviews. " +
                "It keeps going to make sure truncation happens in every locale and at every density we test.",
            "ملخص طويل عمدًا يتجاوز ثلاثة أسطر على شاشة التلفاز حتى يصبح زر قراءة المزيد قابلًا للتركيز في " +
                "الواجهة، تمامًا كما يحدث مع العناوين الحقيقية ذات الملخصات الكاملة. ويستمر النص للتأكد من القص في كل لغة."
        ),
        releaseInfo = "2021", imdbRating = 8.0f, genres = listOf("Science Fiction", "Adventure", "Drama"),
        runtime = if (type == ContentType.MOVIE) "155" else null,
        director = listOf("Director One"), writer = listOf("Writer One"),
        cast = (1..10).map { "Actor $it" },
        castMembers = (1..10).map { MetaCastMember("Actor $it", "Role $it", artwork(it), tmdbId = 1000 + it) },
        videos = videos,
        productionCompanies = (1..4).map { MetaCompany("Studio $it", artwork(it), tmdbId = 2000 + it) },
        networks = if (type == ContentType.SERIES) (1..2).map { MetaCompany("Network $it", artwork(it), tmdbId = 3000 + it) } else emptyList(),
        ageRating = "PG-13", country = "US", awards = "Won 6 Oscars", language = "en", links = emptyList(),
        trailers = (1..4).map { MetaTrailer(source = "yt$it", type = "Trailer", name = "Trailer $it", ytId = "yt-fixture-$it", lang = "en") }
    )

    private fun movie(): Fixture {
        val meta = baseMeta(ContentType.MOVIE, emptyList())
        return Fixture(meta, MetaDetailsUiState(
            isLoading = false, meta = meta,
            moreLikeThis = (0 until 16).map { preview(it, "more") },
            moreLikeThisSource = MoreLikeThisSource.TMDB,
            collection = (0 until 6).map { preview(it, "collection") },
            collectionName = text("Desert Planet Collection", "مجموعة كوكب الصحراء"),
            tmdbRating = 7.9f, trailerButtonEnabled = true
        ))
    }

    private fun series(): Fixture {
        val episodes = (1..3).flatMap { season ->
            (1..12).map { episode ->
                Video("tt-fixture-series:$season:$episode", text("Episode $episode", "الحلقة $episode"), "2022-01-0${(episode % 9) + 1}",
                    artwork(episode), season = season, episode = episode,
                    overview = text("An episode overview for remote navigation.", "ملخص حلقة لاختبار التنقل بجهاز التحكم."), runtime = 52)
            }
        }
        val meta = baseMeta(ContentType.SERIES, episodes)
        return Fixture(meta, MetaDetailsUiState(
            isLoading = false, meta = meta, seasons = listOf(1, 2, 3), selectedSeason = 1,
            episodesForSeason = episodes.filter { it.season == 1 },
            moreLikeThis = (0 until 16).map { preview(it, "more") },
            moreLikeThisSource = MoreLikeThisSource.TMDB, tmdbRating = 8.3f, trailerButtonEnabled = true
        ))
    }

    private fun artwork(index: Int): String {
        val file = File(context.cacheDir, "detail-remote-artwork-${index % 6}.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(listOf(0xFF486479, 0xFF665645, 0xFF345953, 0xFF555878, 0xFF655063, 0xFF635A46)[index % 6].toInt())
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return file.toURI().toString()
    }
}
