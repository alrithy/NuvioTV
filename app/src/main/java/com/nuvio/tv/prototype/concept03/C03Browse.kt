package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C03Search(session: ProtoSession) {
    val type = c03Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val shown = results.ifEmpty { MockCatalog.trending }
    var focused by remember { mutableStateOf(shown.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)

    C03Screen {
        Column(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 40.dp)) {
            Txt(tr("SEARCH THE HALLS", "ابحث في القاعات"), type.label.copy(color = C03.Bronze))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(query.ifEmpty { tr("BEGIN TYPING", "ابدأ الكتابة") }.up(), type.monument.copy(color = if (query.isEmpty()) C03.SandFaint else C03.Sand), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(2.dp).height(34.dp).background(C03.Bronze))
            }
            Spacer(Modifier.height(18.dp))
            C03Rule(Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(274.dp)) {
                    ProtoKeyboard.keys(ar).chunked(7).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                            row.forEachIndexed { c, k -> C03Key(k.up(), if (r == 0 && c == 0) first else null) { query += k } }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        C03SlabButton(tr("SPACE", "مسافة"), Modifier.weight(1f)) { query += " " }
                        C03SlabButton(tr("ERASE", "حذف"), Modifier.weight(1f)) { query = query.dropLast(1) }
                    }
                }
                Spacer(Modifier.width(26.dp))
                C03VRule(Modifier.fillMaxHeight())
                Spacer(Modifier.width(26.dp))
                Column(Modifier.width(290.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Txt((if (results.isEmpty()) tr("MOST VISITED", "الأكثر زيارة") else tr("FOUND  ·  ${results.size}", "وُجد  ·  ${results.size}")), type.small)
                    Spacer(Modifier.height(8.dp))
                    shown.forEach { t ->
                        C03ListItem(t.title.get().up(), style = type.label.copy(color = C03.Sand), maxScale = 1.3f, onFocus = { focused = t }) {
                            session.nav.push(ProtoRoute.Details(t.id))
                        }
                    }
                }
                Spacer(Modifier.width(20.dp))
                Box(Modifier.weight(1f).fillMaxHeight().stoneEdge().background(C03.Shadow)) {
                    Crossfade(focused, animationSpec = tween(800, easing = ProtoEasing.Cinematic), label = "a") { t -> ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize()) }
                    Box(Modifier.fillMaxSize().background(C03Lintel))
                }
            }
        }
    }
}

@Composable
private fun C03Key(label: String, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c03Type()
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(35.dp)
            .background(if (focused) C03.Sand else C03.Basalt)
            .border(1.dp, if (focused) C03.Sand else C03.Bronze.copy(alpha = 0.2f))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, type.button.copy(color = if (focused) C03.Shadow else C03.Sand, letterSpacing = type.button.letterSpacing * 0f), maxLines = 1)
    }
}

/** The archive is a ledger: numbered entries, columns of facts, one aperture for the entry in hand. */
@Composable
internal fun C03Library(session: ProtoSession) {
    val type = c03Type()
    val lang = LocalProtoLang.current
    val tabs = listOf(
        Bi("Watchlist", "قائمة المشاهدة") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Seen", "شوهدت") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(tabs[0].second.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C03Screen {
        Row(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 40.dp)) {
            Column(Modifier.width(560.dp).fillMaxHeight()) {
                Txt(tr("THE ARCHIVE", "الأرشيف"), type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(6.dp))
                Txt(tr("YOUR COLLECTION", "مجموعتك"), type.monument, maxLines = 1)
                Spacer(Modifier.height(14.dp))
                val tabFocus = rememberTabRowFocus()
                Row(Modifier.tabRow(tabFocus), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    tabs.forEachIndexed { i, (label, items) ->
                        Box(Modifier.width(150.dp)) {
                            C03ListItem("${label.get().up()}  ${items.size.toString().digits()}", Modifier.tabItem(tabFocus, i == tab), requester = if (i == 0) first else null, style = type.label, maxScale = 1.1f, selected = i == tab, onFocus = { tab = i }) { tab = i }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Ledger header
                Row(Modifier.padding(vertical = 6.dp)) {
                    Txt(tr("NO.", "رقم"), type.small, Modifier.width(44.dp))
                    Txt(tr("TITLE", "العنوان"), type.small, Modifier.weight(1f))
                    Txt(tr("YEAR", "السنة"), type.small, Modifier.width(62.dp))
                    Txt(tr("LENGTH", "المدة"), type.small, Modifier.width(80.dp))
                    Txt(tr("FORMAT", "الصيغة"), type.small, Modifier.width(78.dp))
                }
                C03Rule(Modifier.fillMaxWidth(), C03.Bronze.copy(alpha = 0.6f))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    tabs[tab].second.forEachIndexed { i, t -> C03LedgerRow(i + 1, t, lang, onFocus = { focused = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                }
            }
            Spacer(Modifier.width(30.dp))
            Box(Modifier.weight(1f).fillMaxHeight().stoneEdge().background(C03.Shadow)) {
                Crossfade(focused, animationSpec = tween(800, easing = ProtoEasing.Cinematic), label = "a") { t -> ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize()) }
                Box(Modifier.fillMaxSize().background(C03Lintel))
            }
        }
    }
}

@Composable
private fun C03LedgerRow(n: Int, t: ProtoTitle, lang: com.nuvio.tv.prototype.shared.ProtoLang, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c03Type()
    var focused by remember { mutableStateOf(false) }
    val ink = if (focused) C03.Shadow else C03.Sand
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) C03.Sand else Color.Transparent)
                .protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Txt(n.toString().padStart(2, '0').digits(), type.label.copy(color = if (focused) C03.Shadow else C03.Bronze), Modifier.width(44.dp))
            Txt(t.title.get().up(), type.label.copy(color = ink), Modifier.weight(1f), maxLines = 1)
            Txt(t.year.toString().digits(), type.label.copy(color = ink), Modifier.width(62.dp))
            Txt(if (t.isSeries) "${t.totalEpisodes} EP" else formatRuntime(t.runtimeMin, lang), type.label.copy(color = ink), Modifier.width(80.dp), maxLines = 1)
            Txt(if (t.tech.dolbyVision) "DV" else if (t.tech.uhd) "4K" else "HD", type.label.copy(color = ink), Modifier.width(78.dp))
        }
        C03Rule(Modifier.fillMaxWidth(), C03.Bronze.copy(alpha = 0.2f))
    }
}

@Composable
internal fun C03Profile(session: ProtoSession) {
    val type = c03Type()
    var focusedIdx by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C03Screen {
        Row(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 44.dp)) {
            Column(Modifier.width(430.dp)) {
                Txt(tr("WHO ENTERS", "من يدخل"), type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(30.dp))
                MockCatalog.profiles.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(p.color))
                        Spacer(Modifier.width(8.dp))
                        C03ListItem(p.name.get().up(), requester = if (i == 0) first else null, style = type.heading, maxScale = 1.35f, onFocus = { focusedIdx = i }) { session.nav.home() }
                    }
                }
            }
            C03VRule(Modifier.fillMaxHeight())
            Spacer(Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                val p = MockCatalog.profiles[focusedIdx]
                Txt(tr("PREFERENCES OF ", "تفضيلات ") + p.name.get().up(), type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(14.dp))
                C03Rule(Modifier.fillMaxWidth())
                C03Spec(tr("Language", "اللغة"), tr("English · Arabic subtitles", "العربية · ترجمة عربية"), 150.dp)
                C03Spec(tr("Playback", "التشغيل"), tr("Best match · 4K when cached", "الأنسب · 4K عند التخزين"), 150.dp)
                C03Spec(tr("Sound", "الصوت"), tr("Original · Atmos passthrough", "الأصلي · تمرير أتموس"), 150.dp)
                C03Spec(tr("Subtitles", "الترجمة"), tr("Sand · medium · no box", "رملي · متوسط · بلا خلفية"), 150.dp)
                C03Spec(tr("Maturity", "الفئة العمرية"), if (p.kids) tr("Up to PG", "حتى PG") else tr("All titles", "كل العناوين"), 150.dp)
                C03Spec(tr("Sources", "المصادر"), "Real-Debrid · Torrentio · Comet", 150.dp)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C03SlabButton(tr("EDIT PROFILE", "تعديل الملف")) {}
                    C03SlabButton(tr("SETTINGS", "الإعدادات")) {}
                }
            }
        }
    }
}
