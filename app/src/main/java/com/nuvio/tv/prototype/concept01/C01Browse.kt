package com.nuvio.tv.prototype.concept01

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C01Search(session: ProtoSession) {
    val type = c01Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val frozen = LocalProtoEnv.current.frozen
    val blink = if (frozen) 1f else {
        val t = rememberInfiniteTransition(label = "cursor")
        t.animateFloat(1f, 0f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "c").value
    }

    Column(Modifier.fillMaxSize().background(C01.Black).padding(top = 64.dp)) {
        Column(Modifier.padding(horizontal = C01.Margin)) {
            Txt(tr("SEARCH", "البحث"), type.overline)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(
                    if (query.isEmpty()) tr("TITLES, PEOPLE, GENRES", "عناوين، أشخاص، أنواع") else query.cap(),
                    type.display.copy(color = if (query.isEmpty()) C01.Ink25 else C01.Ink),
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(2.dp).height(40.dp).graphicsLayer { alpha = blink }.background(C01.Ink))
            }
        }
        Spacer(Modifier.height(26.dp))
        // The keyboard is a single line of letters — a ticker, not a grid.
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = C01.Margin),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProtoKeyboard.keys(ar).forEachIndexed { i, k ->
                C01Action(k.cap(), requester = if (i == 0) first else null, style = type.label) { query += k }
            }
            Spacer(Modifier.width(10.dp))
            C01Action(tr("SPACE", "مسافة"), style = type.small.copy(color = C01.Ink)) { query += " " }
            C01Action(tr("DELETE", "حذف"), style = type.small.copy(color = C01.Ink)) { query = query.dropLast(1) }
            C01Action(tr("CLEAR", "مسح"), style = type.small.copy(color = C01.Ink)) { query = "" }
        }
        Spacer(Modifier.height(40.dp))
        Column(Modifier.padding(horizontal = C01.Margin)) {
            if (query.isEmpty()) {
                Txt(tr("TRY", "جرّب"), type.small)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    MockCatalog.searchSuggestions.forEach { s ->
                        val label = s.get()
                        C01Action(label.cap(), style = type.label.copy(color = C01.Ink70)) { query = label }
                    }
                }
            } else {
                Txt(tr("RESULTS  ·  ${results.size}", "النتائج  ·  ${results.size}"), type.small)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.horizontalScroll(session.scroll("c01.search")).padding(horizontal = C01.Margin),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            results.forEach { t ->
                C01Frame(t, width = 200.dp, caption = t.title.get().cap(), captionStyle = type.small.copy(color = C01.Ink)) {
                    session.nav.push(ProtoRoute.Details(t.id))
                }
            }
            Spacer(Modifier.width(500.dp))
        }
    }
}

@Composable
internal fun C01Library(session: ProtoSession) {
    val type = c01Type()
    val tabs = listOf(
        Bi("Watchlist", "قائمتي") to MockCatalog.watchlist,
        Bi("Continue", "تابع") to MockCatalog.continueWatching,
        Bi("Seen", "شوهدت") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Column(
        Modifier
            .fillMaxSize()
            .background(C01.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = C01.Margin)
            .padding(top = 60.dp, bottom = 80.dp),
    ) {
        Txt(tr("THE COLLECTION", "المجموعة"), type.overline)
        Spacer(Modifier.height(10.dp))
        Txt(tr("YOUR LIBRARY", "مكتبتك"), type.display)
        Spacer(Modifier.height(20.dp))
        val tabFocus = rememberTabRowFocus()
        Row(Modifier.tabRow(tabFocus), horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            tabs.forEachIndexed { i, (label, items) ->
                C01Action(
                    "${label.get().cap()}  ${items.size}",
                    modifier = Modifier.tabItem(tabFocus, i == tab),
                    requester = if (i == 0) first else null,
                    style = type.label.copy(color = if (i == tab) C01.Ink else C01.Ink45),
                    onFocus = { tab = i },
                ) { tab = i }
            }
        }
        Spacer(Modifier.height(30.dp))
        tabs[tab].second.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(bottom = 22.dp)) {
                row.forEach { t ->
                    C01Frame(
                        t, width = 193.dp,
                        progress = if (tab == 1) t.progress else null,
                        caption = t.title.get().cap() + "   " + t.year,
                        captionStyle = type.small.copy(color = C01.Ink),
                    ) { session.nav.push(ProtoRoute.Details(t.id)) }
                }
            }
        }
    }
}

@Composable
internal fun C01Profile(session: ProtoSession) {
    val type = c01Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val settings = listOf(
        Bi("Playback", "التشغيل") to Bi("Best match · 4K when cached", "الأنسب · 4K عند التخزين"),
        Bi("Audio", "الصوت") to Bi("Original language · Atmos passthrough", "اللغة الأصلية · تمرير أتموس"),
        Bi("Subtitles", "الترجمة") to Bi("Arabic · cinema white · medium", "العربية · أبيض سينمائي · متوسط"),
        Bi("Interface", "الواجهة") to Bi("English · letterbox on", "العربية · الإطار السينمائي مفعّل"),
        Bi("Sources", "المصادر") to Bi("Real-Debrid · 4 add-ons", "ريل ديبريد · 4 إضافات"),
    )
    Column(
        Modifier.fillMaxSize().background(C01.Black).padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(tr("WHO'S WATCHING", "من يشاهد"), type.overline)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
            MockCatalog.profiles.forEachIndexed { i, p ->
                C01Action(p.name.get().cap(), requester = if (i == 0) first else null, style = type.title) { session.nav.home() }
            }
        }
        Spacer(Modifier.height(70.dp))
        Txt(tr("SETTINGS", "الإعدادات"), type.small)
        Spacer(Modifier.height(16.dp))
        settings.forEach { (k, v) ->
            var focused by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .protoFocusable(onFocusChange = { focused = it }, onClick = {})
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Txt(k.get().cap(), type.small.copy(color = if (focused) C01.Ink else C01.Ink45), Modifier.width(240.dp), align = TextAlign.End)
                Spacer(Modifier.width(28.dp))
                Txt(v.get().cap(), type.label.copy(color = if (focused) C01.Ink else C01.Ink70), Modifier.width(420.dp), maxLines = 1)
            }
        }
    }
}
