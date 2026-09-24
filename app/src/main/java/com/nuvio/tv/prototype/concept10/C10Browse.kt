package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C10Search(session: ProtoSession) {
    val type = c10Type()
    val ar = isArabic()
    val lang = LocalProtoLang.current
    val clock by rememberProtoClock()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val shown = results.ifEmpty { MockCatalog.trending }
    var lit by remember { mutableStateOf(shown.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val frozen = LocalProtoEnv.current.frozen
    val caret = if (frozen) 1f else rememberInfiniteTransition(label = "caret").animateFloat(1f, 0f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "c").value

    Box(Modifier.fillMaxSize()) {
        C10Ambient(lit.palette, strength = 0.8f)
        Row(Modifier.fillMaxSize().padding(top = 78.dp)) {
            Column(Modifier.width(320.dp).padding(start = C10.Margin)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProtoIcon(Glyph.SEARCH, size = 18.dp, color = C10.Ink2, stroke = 1.6.dp)
                    Spacer(Modifier.width(12.dp))
                    Txt(query.ifEmpty { tr("Titles, people, genres", "أعمال، أشخاص، أنواع") }, type.title.copy(color = if (query.isEmpty()) C10.Ink3 else C10.Ink), Modifier.weight(1f, fill = false), maxLines = 1)
                    Box(Modifier.padding(start = 3.dp).width(2.dp).height(22.dp).graphicsLayer { alpha = caret }.background(C10.Gold))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C10.Hair))
                Spacer(Modifier.height(16.dp))
                val keys = ProtoKeyboard.keys(ar)
                val cols = if (ar) 8 else 6
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keys.chunked(cols).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { c, k -> C10Key(k, 28.dp, if (r == 0 && c == 0) first else null) { query += k } }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C10Button(tr("Space", "مسافة")) { query += " " }
                    C10Button(tr("Delete", "حذف"), Glyph.BACK) { query = query.dropLast(1) }
                    C10Button("", Glyph.MIC) {}
                }
                Spacer(Modifier.height(18.dp))
                Txt(over("Recent", "عمليات بحث سابقة"), type.overline.copy(color = C10.Ink3))
                Spacer(Modifier.height(4.dp))
                Txt(listOf(Bi("Villeneuve", "فيلنوف"), Bi("Severance", "انفصال"), Bi("Theeb", "ذيب")).joinToString("  ·  ") { it.of(lang) }, type.body, maxLines = 1)
            }
            Spacer(Modifier.width(34.dp))
            Column(Modifier.weight(1f).padding(end = C10.Margin)) {
                Txt(if (results.isEmpty()) over("Popular now", "الأكثر مشاهدة الآن") else over("Top result", "أفضل نتيجة"), type.overline)
                Spacer(Modifier.height(8.dp))
                val top = shown.first()
                Row(verticalAlignment = Alignment.Bottom) {
                    C10Card(top, ArtKind.BACKDROP, 300.dp, 16f / 9f, onFocus = { if (it) lit = top }) { session.nav.push(ProtoRoute.Details(top.id)) }
                    Spacer(Modifier.width(18.dp))
                    AnimatedContent(lit, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(120)) }, label = "lit") { l ->
                        Column(Modifier.padding(bottom = 4.dp)) {
                            Txt(l.title.get(), type.title, maxLines = 2)
                            Txt(metaLine(l), type.caption, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            Txt(l.tagline.get(), type.body, maxLines = 3)
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                Txt(if (results.isEmpty()) over("Trending", "رائج") else over("${results.size} results", "${results.size} نتائج"), type.overline.copy(color = C10.Ink3))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    shown.drop(1).take(5).forEach { t -> C10Card(t, ArtKind.POSTER, 88.dp, 2f / 3f, onFocus = { if (it) lit = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                }
            }
        }
        C10TopNav(C10Section.SEARCH, clock) { session.goTo(it) }
    }
}

@Composable
private fun C10Key(k: String, size: Dp, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Box(
        Modifier.lightBar({ f }, 3.dp).size(size).clip(RoundedCornerShape(6.dp))
            .background(if (focused) C10.Ink else C10.Ink.copy(alpha = 0.06f))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(k, type.label.copy(color = if (focused) C10.Black else C10.Ink2)) }
}

@Composable
internal fun C10Library(session: ProtoSession) {
    val type = c10Type()
    val clock by rememberProtoClock()
    val tabs = listOf(
        Bi("Watchlist", "قائمة المشاهدة") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Watched", "شوهدت") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    var lit by remember { mutableStateOf<ProtoTitle>(MockCatalog.watchlist.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Box(Modifier.fillMaxSize()) {
        C10Ambient(lit.palette, strength = 0.8f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 80.dp, bottom = 40.dp)) {
            Row(Modifier.padding(horizontal = C10.Margin), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { i, (name, items) ->
                    C10Tab(name.get() + "  " + items.size, selected = i == tab, modifier = Modifier.padding(end = 22.dp), requester = if (i == 0) first else null, onFocus = { tab = i })
                }
                Spacer(Modifier.weight(1f))
                Txt(tr("Sorted by recently added", "مرتبة حسب الإضافة الأحدث"), type.caption)
            }
            Spacer(Modifier.height(22.dp))
            Column(Modifier.padding(horizontal = C10.Margin), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                tabs[tab].second.chunked(7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { t -> C10Card(t, ArtKind.POSTER, 106.dp, 2f / 3f, onFocus = { if (it) lit = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                    }
                }
            }
        }
        C10TopNav(C10Section.LIBRARY, clock) { session.goTo(it) }
    }
}

private val settings = listOf(
    Triple(Glyph.GLOBE, Bi("Language", "اللغة"), Bi("English · العربية", "العربية · English")),
    Triple(Glyph.SUBTITLES, Bi("Subtitles", "الترجمة"), Bi("Arabic when available", "العربية متى توفرت")),
    Triple(Glyph.SPARKLE, Bi("Playback", "التشغيل"), Bi("Best match, start instantly", "الأنسب، مع بدء فوري")),
    Triple(Glyph.TV, Bi("Ambient tint", "الإضاءة المحيطة"), Bi("On, subtle", "مفعّلة، هادئة")),
    Triple(Glyph.NEXT, Bi("Autoplay next episode", "تشغيل الحلقة التالية تلقائياً"), Bi("After 10 seconds", "بعد ١٠ ثوانٍ")),
    Triple(Glyph.SOURCES, Bi("Sources & add-ons", "المصادر والإضافات"), Bi("4 connected", "٤ متصلة")),
)

@Composable
internal fun C10Profile(session: ProtoSession) {
    val type = c10Type()
    val clock by rememberProtoClock()
    val ar = isArabic()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    var lit by remember { mutableIntStateOf(0) }
    val palettes = listOf(MockCatalog.dune2, MockCatalog.pastlives, MockCatalog.severance, MockCatalog.wadjda, MockCatalog.arrival)
    Box(Modifier.fillMaxSize()) {
        C10Ambient(palettes[lit].palette, strength = 0.8f)
        Column(Modifier.fillMaxSize().padding(top = 84.dp, start = C10.Margin, end = C10.Margin)) {
            Txt(tr("Who's watching?", "من يشاهد؟"), type.hero.copy(fontSize = type.hero.fontSize * 0.8f))
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                MockCatalog.profiles.forEachIndexed { i, p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        C10Avatar(if (ar) p.name.ar.take(1) else p.initial, 76.dp, false, Modifier.onFocusChanged { if (it.hasFocus) lit = i }, color = p.color, requester = if (i == 0) first else null) { session.nav.home() }
                        Spacer(Modifier.height(18.dp))
                        Txt(p.name.get(), type.label.copy(color = if (lit == i) C10.Ink else C10.Ink2))
                        if (p.kids) Txt(over("Kids", "للأطفال"), type.overline.copy(color = C10.Ink3))
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Txt(over("This TV", "هذا التلفاز"), type.overline.copy(color = C10.Ink3))
            Spacer(Modifier.height(8.dp))
            settings.chunked(2).forEach { row ->
                Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (g, k, v) -> C10SettingRow(g, k.get(), v.get(), Modifier.weight(1f)) }
                }
            }
        }
        C10TopNav(C10Section.PROFILE, clock) { session.goTo(it) }
    }
}

@Composable
internal fun C10SettingRow(glyph: Glyph, key: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Row(
        modifier.sideBar { f }.clip(RoundedCornerShape(8.dp))
            .background(if (focused) C10.Surface2 else C10.Ink.copy(alpha = 0.04f))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProtoIcon(glyph, size = 16.dp, color = if (focused) C10.Ink else C10.Ink3, stroke = 1.5.dp)
        Spacer(Modifier.width(14.dp))
        Txt(key, type.label, Modifier.weight(1f), maxLines = 1)
        Txt(value, type.caption.copy(color = C10.Ink2), maxLines = 1)
    }
}
