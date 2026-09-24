package com.nuvio.tv.prototype.concept02

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C02Search(session: ProtoSession) {
    val type = c02Type()
    val ar = isArabic()
    var arabicKeys by remember { mutableStateOf(ar) }
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C02Screen {
        RiyadhSkyline(Modifier.fillMaxSize(), intensity = 0.5f)
        Row(Modifier.fillMaxSize().padding(horizontal = C02.Margin, vertical = 36.dp)) {
            Column(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C02.Surface.copy(alpha = 0.85f))
                    .border(1.dp, C02.Line, RoundedCornerShape(10.dp))
                    .padding(18.dp),
            ) {
                C02Heading(Bi("Search", "البحث"))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp).border(1.dp, C02.Line, RoundedCornerShape(C02.Radius)).padding(horizontal = 12.dp)) {
                    ProtoIcon(Glyph.SEARCH, size = 14.dp, color = C02.Gold, stroke = 1.6.dp)
                    Spacer(Modifier.width(10.dp))
                    Txt(query.ifEmpty { tr("Titles, people, genres", "عناوين، أشخاص، أنواع") }, type.title.copy(color = if (query.isEmpty()) C02.Ink3 else C02.Ink), Modifier.weight(1f), maxLines = 1)
                    Box(Modifier.width(1.5.dp).height(18.dp).background(C02.Gold))
                }
                Spacer(Modifier.height(14.dp))
                val keys = ProtoKeyboard.keys(arabicKeys)
                keys.chunked(6).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 5.dp)) {
                        row.forEachIndexed { c, k ->
                            C02Key(k, Modifier.weight(1f), if (r == 0 && c == 0) first else null) { query += k }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    C02Key(if (arabicKeys) "EN" else "ع", Modifier.weight(1f)) { arabicKeys = !arabicKeys }
                    C02Key(tr("Space", "مسافة"), Modifier.weight(2f)) { query += " " }
                    C02Key("⌫", Modifier.weight(1f)) { query = query.dropLast(1) }
                    C02Key(tr("Clear", "مسح"), Modifier.weight(1.4f)) { query = "" }
                }
            }
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (results.isEmpty()) {
                    C02Heading(Bi("Trending in Riyadh tonight", "الأكثر رواجاً في الرياض الليلة"))
                    Spacer(Modifier.height(12.dp))
                    MockCatalog.trending.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                            row.forEach { t -> C02Landscape(t, 170.dp, null, null, null, {}) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        }
                    }
                } else {
                    C02Heading(Bi("Top result", "أفضل نتيجة"))
                    Spacer(Modifier.height(10.dp))
                    val top = results.first()
                    C02Landscape(top, 330.dp, null, null, top.tagline.get(), {}) { session.nav.push(ProtoRoute.Details(top.id)) }
                    Spacer(Modifier.height(18.dp))
                    C02Heading(Bi("Also matching", "نتائج أخرى"), count = results.size - 1)
                    Spacer(Modifier.height(10.dp))
                    results.drop(1).chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                            row.forEach { t -> C02Poster(t, 92.dp, null, {}) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C02Key(label: String, modifier: Modifier = Modifier, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (focused) C02.Gold else C02.Glass)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, type.label.copy(color = if (focused) C02.Night else C02.Ink, fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal), maxLines = 1)
    }
}

@Composable
internal fun C02Library(session: ProtoSession) {
    val type = c02Type()
    val tabs = listOf(
        Bi("My list", "قائمتي") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Watched", "شوهدت") to MockCatalog.watched,
        Bi("Arabic", "عربي") to MockCatalog.arabicCinema,
    )
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C02Screen {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            C02Header(session, 5)
            Column(Modifier.padding(horizontal = C02.Margin)) {
                Spacer(Modifier.height(10.dp))
                Txt(tr("Your library", "مكتبتك"), type.hero)
                Txt(tr("مكتبتك", "Your library"), type.heroSecond)
                Spacer(Modifier.height(16.dp))
                val tabFocus = rememberTabRowFocus()
                Row(Modifier.tabRow(tabFocus), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { i, (label, items) ->
                        C02GhostButton("${label.get()}  ${items.size}", requester = if (i == 0) first else null, modifier = Modifier.tabItem(tabFocus, i == tab), selected = i == tab, onFocus = { tab = i }) { tab = i }
                    }
                }
                Spacer(Modifier.height(18.dp))
                C02Divider(Modifier.width(864.dp))
                Spacer(Modifier.height(18.dp))
                tabs[tab].second.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                        row.forEach { t ->
                            C02Landscape(t, 204.dp, null, if (tab == 1) t.progress else null, t.year.toString() + " · " + t.primaryGenre.get(), {}) {
                                session.nav.push(ProtoRoute.Details(t.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun C02Profile(session: ProtoSession) {
    val type = c02Type()
    val clock by rememberProtoClock()
    val lang = LocalProtoLang.current
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C02Screen {
        RiyadhSkyline(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(top = 58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            C02Heading(Bi(clock.greeting(com.nuvio.tv.prototype.shared.ProtoLang.EN), clock.greeting(com.nuvio.tv.prototype.shared.ProtoLang.AR)))
            Spacer(Modifier.height(10.dp))
            Txt(tr("Who's watching tonight?", "من يشاهد الليلة؟"), type.hero, align = TextAlign.Center)
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                MockCatalog.profiles.forEachIndexed { i, p ->
                    var focused by remember { mutableStateOf(false) }
                    val f = c02FocusAnim(focused)
                    Column(
                        Modifier.protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it }, onClick = { session.nav.home() }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(78.dp)
                                .c02Focus(f)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(p.color.copy(alpha = 0.9f), p.color.copy(alpha = 0.35f))))
                                .border(2.dp, if (focused) C02.GoldLight else C02.Line, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Txt(p.name.get().take(1), type.hero.copy(fontSize = 30.sp, color = C02.Night))
                        }
                        Spacer(Modifier.height(10.dp))
                        Txt(p.name.get(), type.label.copy(color = if (focused) C02.Ink else C02.Ink2))
                        Txt(p.name.other(), type.captionSecond)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(Bi("Language", "اللغة"), Bi("العربية + English", "العربية + English"), Glyph.GLOBE),
                    Triple(Bi("Subtitles", "الترجمة"), Bi("Arabic · warm gold", "العربية · ذهبي دافئ"), Glyph.SUBTITLES),
                    Triple(Bi("Playback", "التشغيل"), Bi("Best match · Atmos", "الأنسب · أتموس"), Glyph.PLAY),
                    Triple(Bi("Evening mode", "الوضع المسائي"), Bi("Warm dim after ${if (lang == com.nuvio.tv.prototype.shared.ProtoLang.AR) "10 م" else "10 PM"}", "خفوت دافئ بعد 10 م"), Glyph.CLOCK),
                ).forEach { (k, v, g) ->
                    var focused by remember { mutableStateOf(false) }
                    Column(
                        Modifier
                            .width(176.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (focused) C02.Raised else C02.Surface.copy(alpha = 0.7f))
                            .border(1.dp, if (focused) C02.Gold else C02.Line, RoundedCornerShape(8.dp))
                            .protoFocusable(onFocusChange = { focused = it }, onClick = {})
                            .padding(14.dp),
                    ) {
                        ProtoIcon(g, size = 16.dp, color = C02.Gold, stroke = 1.4.dp)
                        Spacer(Modifier.height(10.dp))
                        Txt(k.get(), type.label)
                        Txt(k.other(), type.captionSecond)
                        Spacer(Modifier.height(6.dp))
                        Txt(v.get(), type.meta.copy(color = C02.GoldLight), maxLines = 1)
                    }
                }
            }
        }
    }
}
