package com.nuvio.tv.prototype.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nuvio.tv.prototype.shared.data.MockCatalog

/** Overlays that live inside the player. */
enum class PlayerPanel { NONE, SUBTITLES, AUDIO, EPISODES, INFO }

/** Every prototype renders the same eleven destinations so concepts can be compared screen by screen. */
sealed interface ProtoRoute {
    data object Home : ProtoRoute
    data class Details(val titleId: String) : ProtoRoute
    data class Episodes(val titleId: String, val season: Int = 1) : ProtoRoute
    data object Search : ProtoRoute
    data object Library : ProtoRoute
    data class Streams(val titleId: String) : ProtoRoute
    data class Player(val titleId: String, val panel: PlayerPanel = PlayerPanel.NONE) : ProtoRoute
    data object Profile : ProtoRoute
}

/** Screens exposed in the hub's "jump to" strip. */
enum class ProtoScreen(val label: Bi) {
    HOME(Bi("Home", "الرئيسية")),
    FOCUSED(Bi("Focused title", "عنصر محدد")),
    DETAILS(Bi("Details", "التفاصيل")),
    EPISODES(Bi("Episodes", "الحلقات")),
    SEARCH(Bi("Search", "البحث")),
    LIBRARY(Bi("Library", "المكتبة")),
    STREAMS(Bi("Stream picker", "اختيار المصدر")),
    PLAYER(Bi("Player", "المشغل")),
    SUBTITLES(Bi("Subtitles", "الترجمة")),
    AUDIO(Bi("Audio", "الصوت")),
    PROFILE(Bi("Profile", "الملف الشخصي"));

    fun initialStack(): List<ProtoRoute> {
        val movie = MockCatalog.featuredMovieId
        val series = MockCatalog.featuredSeriesId
        return when (this) {
            HOME, FOCUSED -> listOf(ProtoRoute.Home)
            DETAILS -> listOf(ProtoRoute.Home, ProtoRoute.Details(movie))
            EPISODES -> listOf(ProtoRoute.Home, ProtoRoute.Details(series), ProtoRoute.Episodes(series, 2))
            SEARCH -> listOf(ProtoRoute.Home, ProtoRoute.Search)
            LIBRARY -> listOf(ProtoRoute.Home, ProtoRoute.Library)
            STREAMS -> listOf(ProtoRoute.Home, ProtoRoute.Details(movie), ProtoRoute.Streams(movie))
            PLAYER -> listOf(ProtoRoute.Home, ProtoRoute.Details(movie), ProtoRoute.Player(movie))
            SUBTITLES -> listOf(ProtoRoute.Home, ProtoRoute.Details(movie), ProtoRoute.Player(movie, PlayerPanel.SUBTITLES))
            AUDIO -> listOf(ProtoRoute.Home, ProtoRoute.Details(movie), ProtoRoute.Player(movie, PlayerPanel.AUDIO))
            PROFILE -> listOf(ProtoRoute.Home, ProtoRoute.Profile)
        }
    }
}

@Stable
class ProtoNavigator(initial: List<ProtoRoute>) {
    val stack = mutableStateListOf<ProtoRoute>().apply { addAll(initial.ifEmpty { listOf(ProtoRoute.Home) }) }

    /** True when the last transition moved deeper; concepts use it to pick enter/exit direction. */
    var forward by mutableStateOf(true)
        private set

    val current: ProtoRoute get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(route: ProtoRoute) {
        forward = true
        stack.add(route)
    }

    fun replace(route: ProtoRoute) {
        forward = true
        stack[stack.lastIndex] = route
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        forward = false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popTo(predicate: (ProtoRoute) -> Boolean) {
        forward = false
        while (stack.size > 1 && !predicate(stack.last())) stack.removeAt(stack.lastIndex)
    }

    fun home() {
        forward = false
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

/**
 * Per-concept session: navigation plus state that must survive screen transitions
 * (scroll positions, last focused keys, demo flags used by the hub's jump strip).
 */
@Stable
class ProtoSession(
    val screen: ProtoScreen,
    val nav: ProtoNavigator = ProtoNavigator(screen.initialStack()),
) {
    /** Hub "Focused title" jump: Home should open with focus on a deeper, non-default item. */
    val demoFocus: Boolean get() = screen == ProtoScreen.FOCUSED

    /** Hub "Search" jump pre-fills a query so the results state is visible immediately. */
    val demoSearch: Boolean get() = screen == ProtoScreen.SEARCH

    val focus = FocusRegistry()
    private val scrolls = HashMap<String, ScrollState>()
    val values = mutableStateMapOf<String, Any>()

    fun scroll(key: String): ScrollState = scrolls.getOrPut(key) { ScrollState(0) }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> value(key: String, default: T): T = (values[key] as? T) ?: default
    fun set(key: String, value: Any) { values[key] = value }
}
