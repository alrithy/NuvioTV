package com.nuvio.tv.prototype.concept07

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C07Search(session: ProtoSession) {
    val type = c07Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    var focused by remember { mutableStateOf<ProtoTitle?>(results.firstOrNull()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f)) {
        C07Masthead(4) { i -> if (i == 5) session.nav.push(ProtoRoute.Library) else if (i == 6) session.nav.push(ProtoRoute.Profile) else if (i < 4) session.nav.home() }
        Row(Modifier.fillMaxSize().padding(horizontal = C07.Margin, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            Column(Modifier.weight(1.2f)) {
                Txt(tr("THE INDEX", "الفهرس"), type.kicker)
                Txt(query.ifEmpty { tr("Look something up…", "ابحث عن شيء…") }, type.headline.copy(color = if (query.isEmpty()) C07.Ink3 else C07.Ink), maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    ProtoKeyboard.keys(ar).forEachIndexed { i, k ->
                        var f by remember { mutableStateOf(false) }
                        Txt(
                            if (ar) k else k.uppercase(),
                            type.headlineS.copy(color = if (f) C07.Red else C07.Ink2),
                            Modifier.protoFocusable(if (i == 0) first else null, onFocusChange = { f = it }, onClick = { query += k }).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    C07Link(tr("space", "مسافة")) { query += " " }
                    C07Link(tr("erase", "حذف")) { query = query.dropLast(1) }
                }
                Spacer(Modifier.height(10.dp))
                C07Rule(color = C07.Ink.copy(alpha = 0.6f))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    results.forEach { t -> C07IndexEntry(t, onFocus = { focused = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                }
            }
            Column(Modifier.weight(0.8f)) {
                val t = focused
                if (t != null) {
                    Box(Modifier.height(280.dp).aspectRatio(2f / 3f).clipToBounds()) { ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(6.dp))
                    Txt(t.tagline.get(), type.deck, Modifier.width(260.dp), maxLines = 2)
                }
            }
        }
    }
}

/** Index entry with dotted leaders, the way a printed index sets title and page. */
@Composable
private fun C07IndexEntry(t: ProtoTitle, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Txt(t.title.get(), type.headlineS.copy(color = if (focused) C07.Red else C07.Ink), maxLines = 1)
        Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 8.dp)) {
            drawLine(if (focused) C07.Red else C07.Ink3, Offset(0f, size.height), Offset(size.width, size.height), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
        }
        Txt("${t.year}", type.headlineS.copy(color = if (focused) C07.Red else C07.Ink2))
    }
}

/** Back issues: every saved title as the cover of its own issue. */
@Composable
internal fun C07Library(session: ProtoSession) {
    val type = c07Type()
    val tabs = listOf(
        Bi("Reading list", "قائمة القراءة") to MockCatalog.watchlist,
        Bi("Bookmarked", "مع إشارة") to MockCatalog.continueWatching,
        Bi("Read", "مقروءة") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).verticalScroll(rememberScrollState())) {
        C07Masthead(5) { i -> if (i == 4) session.nav.push(ProtoRoute.Search) else if (i == 6) session.nav.push(ProtoRoute.Profile) else if (i < 4) session.nav.home() }
        Column(Modifier.padding(horizontal = C07.Margin, vertical = 18.dp)) {
            val tabFocus = rememberTabRowFocus()
            Row(Modifier.tabRow(tabFocus), verticalAlignment = Alignment.Bottom) {
                Txt(tr("Back issues", "الأعداد السابقة"), type.headlineM, Modifier.weight(1f))
                tabs.forEachIndexed { i, (label, items) ->
                    C07Link("${label.get()} ${items.size}" + if (i == tab) " ●" else "", if (i == 0) first else null, Modifier.tabItem(tabFocus, i == tab).padding(start = 18.dp), onFocus = { tab = i }) { tab = i }
                }
            }
            Spacer(Modifier.height(10.dp))
            C07Rule()
            Spacer(Modifier.height(18.dp))
            tabs[tab].second.chunked(5).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(bottom = 22.dp)) {
                    row.forEachIndexed { c, t -> C07IssueCover(t, 38 - r * 5 - c) { session.nav.push(ProtoRoute.Details(t.id)) } }
                }
            }
        }
    }
}

@Composable
private fun C07IssueCover(t: ProtoTitle, issue: Int, onClick: () -> Unit) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    Column(Modifier.width(152.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.72f).redRule(f).clipToBounds()
                .border(1.dp, if (focused) C07.Ink else C07.Rule)
                .protoFocusable(onFocusChange = { focused = it }, onClick = onClick),
        ) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { val s = 1.02f + 0.05f * f; scaleX = s; scaleY = s }, remote = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xAA000000), 0.3f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color(0xCC000000))))
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Txt(if (type.arabic) "نوفيو" else "Nuvio", type.headlineM.copy(fontSize = type.headlineM.fontSize * 0.9f))
                Txt("No. $issue", type.caption.copy(color = C07.Ink2))
                Spacer(Modifier.weight(1f))
                Txt(t.title.get(), type.headlineS, maxLines = 2)
                Txt(t.primaryGenre.get().kick(), type.kicker)
            }
        }
    }
}

/** The masthead page: profiles are contributors, settings are the colophon. */
@Composable
internal fun C07Profile(session: ProtoSession) {
    val type = c07Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val roles = listOf(
        Bi("Editor-in-chief", "رئيس التحرير"), Bi("Film critic", "ناقدة سينمائية"), Bi("Contributing editor", "محررة مساهمة"),
        Bi("Junior edition", "الطبعة الصغيرة"), Bi("Guest columnist", "كاتب ضيف"),
    )
    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).verticalScroll(rememberScrollState())) {
        C07Masthead(6) { i -> if (i == 4) session.nav.push(ProtoRoute.Search) else if (i == 5) session.nav.push(ProtoRoute.Library) else if (i < 4) session.nav.home() }
        Row(Modifier.padding(horizontal = C07.Margin, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            Column(Modifier.weight(1f)) {
                Txt(tr("MASTHEAD", "هيئة التحرير"), type.kicker)
                Spacer(Modifier.height(6.dp))
                C07Rule()
                MockCatalog.profiles.forEachIndexed { i, p ->
                    var focused by remember { mutableStateOf(false) }
                    Column(Modifier.protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it }, onClick = { session.nav.home() }).padding(vertical = 8.dp)) {
                        Txt(roles[i].get().kick(), type.caption.copy(color = if (focused) C07.Red else C07.Ink3))
                        Txt(p.name.get(), type.headlineM.copy(color = if (focused) C07.Ink else C07.Ink2))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Txt(tr("COLOPHON", "بيانات الطبعة"), type.kicker)
                Spacer(Modifier.height(6.dp))
                C07Rule()
                Spacer(Modifier.height(10.dp))
                Txt(
                    tr(
                        "This edition is set in Instrument Serif and Instrument Sans, with Amiri for Arabic. Subtitles are printed in ivory at medium size without a box. Playback chooses the best presentation that starts instantly; Arabic subtitles are preferred where they exist.",
                        "صُفّت هذه الطبعة بخط «أميري» للعربية و«إنسترومنت» للاتينية. تُطبع الترجمة بلون عاجي وحجم متوسط دون خلفية. يختار التشغيل أفضل عرض يبدأ فوراً، مع تفضيل الترجمة العربية متى توفرت.",
                    ),
                    type.deck,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    C07Link(tr("Typography", "الخطوط")) {}
                    C07Link(tr("Playback", "التشغيل")) {}
                    C07Link(tr("Sources", "المصادر")) {}
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
