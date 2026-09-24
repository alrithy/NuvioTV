package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C06Search(session: ProtoSession) {
    val type = c06Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val shown = results.ifEmpty { MockCatalog.trending }
    var preview by remember { mutableStateOf(shown.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)

    C06Screen(session, 3, tr("Search", "البحث")) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Column(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth(), focusable = false) {
                    C06Label(tr("Query", "عبارة البحث"))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(query.ifEmpty { "—" }, type.readoutBig, Modifier.weight(1f), maxLines = 1)
                        Box(Modifier.width(2.dp).height(26.dp).background(C06.Ember))
                    }
                    Txt(tr("${results.size} matches", "${results.size} نتيجة"), type.caption)
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false, padding = 10.dp) {
                    ProtoKeyboard.keys(ar).chunked(7).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                            row.forEachIndexed { c, k -> C06Key(k, Modifier.weight(1f), if (r == 0 && c == 0) first else null) { query += k } }
                            repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        C06Key(tr("SPACE", "مسافة"), Modifier.weight(3f)) { query += " " }
                        C06Key("⌫", Modifier.weight(1f)) { query = query.dropLast(1) }
                        C06Key(tr("CLR", "مسح"), Modifier.weight(1.5f)) { query = "" }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth().height(150.dp), focusable = false, padding = 0.dp) {
                    Box(Modifier.fillMaxSize()) {
                        Crossfade(preview, animationSpec = tween(240), label = "p") { t -> ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize()) }
                        Column(Modifier.align(Alignment.BottomStart).background(Color(0xCC0B0C0E)).padding(10.dp)) {
                            Txt(preview.title.get(), type.title, maxLines = 1)
                            Txt(preview.tagline.get(), type.caption, maxLines = 1)
                        }
                    }
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false, padding = 10.dp) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        C06Label(tr("Title", "العنوان"), Modifier.weight(1f))
                        C06Label(tr("Year", "السنة"), Modifier.width(56.dp))
                        C06Label(tr("Type", "النوع"), Modifier.width(64.dp))
                        C06Label(tr("Format", "الصيغة"), Modifier.width(56.dp))
                        C06Label("IMDb", Modifier.width(40.dp))
                    }
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        shown.forEach { t -> C06ResultRow(t, onFocus = { preview = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun C06ResultRow(t: ProtoTitle, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Row(
        Modifier.fillMaxWidth().module(f, 6.dp).protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(t.title.get(), type.value.copy(color = if (focused) C06.Ink else C06.Ink2), Modifier.weight(1f), maxLines = 1)
        Txt(t.year.toString(), type.value, Modifier.width(56.dp))
        Txt(if (t.isSeries) tr("Series", "مسلسل") else tr("Film", "فيلم"), type.caption, Modifier.width(64.dp))
        Txt(if (t.tech.dolbyVision) "DV" else if (t.tech.uhd) "4K" else "HD", type.value, Modifier.width(56.dp))
        Txt(t.rating.toString(), type.value, Modifier.width(40.dp))
    }
}

@Composable
private fun C06Key(label: String, modifier: Modifier, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (focused) C06.Ink else C06.PanelHi)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, type.value.copy(color = if (focused) C06.Base else C06.Ink), maxLines = 1)
    }
}

@Composable
internal fun C06Library(session: ProtoSession) {
    val type = c06Type()
    val tabs = listOf(
        Bi("Watchlist", "قائمة المشاهدة") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Watched", "شوهدت") to MockCatalog.watched,
        Bi("Arabic", "عربي") to MockCatalog.arabicCinema,
    )
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C06Screen(session, 4, tr("Library", "المكتبة")) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Column(Modifier.width(190.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth(), focusable = false, padding = 8.dp) {
                    tabs.forEachIndexed { i, (label, items) ->
                        var focused by remember { mutableStateOf(false) }
                        val f = detent(focused)
                        Row(
                            Modifier.fillMaxWidth().module(if (i == tab) maxOf(f, 0.35f) else f, 6.dp)
                                .protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it; if (it) tab = i }, onClick = { tab = i })
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                        ) {
                            Txt(label.get(), type.value.copy(color = if (i == tab) C06.Ink else C06.Ink2), Modifier.weight(1f))
                            Txt(items.size.toString(), type.value.copy(color = if (i == tab) C06.Ember else C06.Ink3))
                        }
                    }
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false) {
                    C06Label(tr("This month", "هذا الشهر"))
                    Spacer(Modifier.height(6.dp))
                    Txt("21:40", type.readoutBig)
                    Txt(tr("hours watched", "ساعات مشاهدة"), type.caption)
                    Spacer(Modifier.height(10.dp))
                    C06Readout(tr("In 4K", "بدقة 4K"), "78%")
                    C06Bar(0.78f, Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    C06Readout(tr("Atmos", "أتموس"), "64%")
                    C06Bar(0.64f, Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    C06Readout(tr("AR subs", "ترجمة ع"), "41%")
                    C06Bar(0.41f, Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                tabs[tab].second.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                        row.forEach { t -> C06Tile(t, Modifier.weight(1f), null) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun C06Profile(session: ProtoSession) {
    val type = c06Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C06Screen(session, 5, tr("Profiles & settings", "الملفات والإعدادات")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                MockCatalog.profiles.forEachIndexed { i, p ->
                    C06Module(Modifier.weight(1f), requester = if (i == 0) first else null, onClick = { session.nav.home() }) { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(p.color.copy(alpha = 0.3f + 0.7f * f)), contentAlignment = Alignment.Center) {
                                Txt(p.name.get().take(1), type.title)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Txt(p.name.get(), type.title, maxLines = 1)
                                Txt(if (p.kids) tr("Kids · PG", "أطفال · PG") else tr("Adult", "بالغ"), type.caption)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        C06Readout(tr("Hours", "ساعات"), tr("${(i * 7 + 12) % 30} h", "${(i * 7 + 12) % 30} س"))
                        C06Readout(tr("Subs", "ترجمة"), if (i % 2 == 0) tr("Arabic", "العربية") else tr("English", "الإنجليزية"))
                    }
                }
            }
            C06Module(Modifier.fillMaxWidth(), focusable = false) {
                C06Label(tr("Playback", "التشغيل"))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                    Column(Modifier.weight(1f)) {
                        C06Toggle(tr("Match frame rate", "مطابقة معدل الإطارات"), true)
                        C06Toggle(tr("Dolby Vision", "دولبي فيجن"), true)
                        C06Toggle(tr("Atmos passthrough", "تمرير أتموس"), true)
                    }
                    Column(Modifier.weight(1f)) {
                        C06Toggle(tr("Auto-play next episode", "تشغيل الحلقة التالية تلقائياً"), true)
                        C06Toggle(tr("Skip intros", "تخطي المقدمات"), false)
                        C06Toggle(tr("Prefer cached sources", "تفضيل المصادر المخزنة"), true)
                    }
                }
            }
        }
    }
}

@Composable
private fun C06Toggle(label: String, initial: Boolean) {
    val type = c06Type()
    var on by remember { mutableStateOf(initial) }
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    val knob by animateDpAsState(if (on) 18.dp else 2.dp, tween(C06.DETENT, easing = ProtoEasing.Detent), label = "k")
    Row(
        Modifier.fillMaxWidth().module(f, 6.dp).protoFocusable(onFocusChange = { focused = it }, onClick = { on = !on }).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, type.value.copy(color = if (focused) C06.Ink else C06.Ink2), Modifier.weight(1f), maxLines = 1)
        Box(Modifier.width(36.dp).height(20.dp).clip(RoundedCornerShape(50)).background(if (on) C06.Ember else C06.PanelFocus)) {
            Box(Modifier.offset(x = knob, y = 2.dp).size(16.dp).clip(CircleShape).background(Color.White))
        }
    }
    Spacer(Modifier.height(4.dp))
}
