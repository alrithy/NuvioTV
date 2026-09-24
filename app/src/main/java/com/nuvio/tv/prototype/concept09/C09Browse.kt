package com.nuvio.tv.prototype.concept09

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoIcon
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
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.toArabicDigits
import com.nuvio.tv.prototype.shared.tr
import com.nuvio.tv.prototype.shared.ts

/** Chamfered poster with the tracing light and the title set inside the frame. */
@Composable
internal fun C09Poster(t: ProtoTitle, width: Dp, requester: FocusRequester? = null, showTitle: Boolean = true, onFocus: (Boolean) -> Unit = {}, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Box(
        Modifier.width(width).aspectRatio(2f / 3f).clip(chamfer())
            .c09Trace({ trace.value })
            .protoFocusable(requester, onFocusChange = { focused = it; onFocus(it) }, onClick = onClick),
    ) {
        ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize().graphicsLayer { alpha = if (focused) 1f else 0.82f })
        if (showTitle) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xE6000000))))
            Txt(t.title.get(), type.label.copy(fontFamily = type.title.fontFamily, fontWeight = FontWeight.SemiBold), Modifier.align(Alignment.BottomStart).padding(10.dp), maxLines = 2)
        }
        val p = t.progress
        if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(2.dp).background(C09.Copper))
    }
}

@Composable
internal fun C09Search(session: ProtoSession) {
    val type = c09Type()
    val ar = isArabic()
    val lang = LocalProtoLang.current
    val clock by rememberProtoClock()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val frozen = LocalProtoEnv.current.frozen
    val caret = if (frozen) 1f else rememberInfiniteTransition(label = "caret").animateFloat(1f, 0f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "c").value

    C09Stage {
        Row(Modifier.fillMaxSize()) {
            C09NavRail(C09Section.SEARCH, clock) { session.goTo(it) }
            Column(Modifier.width(390.dp).fillMaxHeight().padding(start = 40.dp, top = 34.dp)) {
                Txt(tag("Search", "ابحث"), type.tag.copy(color = C09.Copper))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(query.ifEmpty { tr("Type a title", "اكتب اسم عمل") }, type.display.copy(fontSize = 40.sp, color = if (query.isEmpty()) C09.Pearl3 else C09.Pearl), Modifier.weight(1f, fill = false), maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(3.dp).height(36.dp).graphicsLayer { alpha = caret }.background(C09.Copper))
                }
                Spacer(Modifier.height(18.dp))
                val keys = ProtoKeyboard.keys(ar)
                val cols = if (ar) 8 else 9
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    keys.chunked(cols).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { c, k -> C09Key(if (ar) k.toArabicDigits() else k, 34.dp, if (r == 0 && c == 0) first else null) { query += k } }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    C09Button(tr("Space", "مسافة")) { query += " " }
                    C09Button("", Glyph.BACK) { query = query.dropLast(1) }
                    C09Button(tr("Voice", "صوت"), Glyph.MIC) {}
                }
                Spacer(Modifier.height(16.dp))
                Txt(tag("Try", "جرّب"), type.tag)
                Spacer(Modifier.height(4.dp))
                Txt(MockCatalog.searchSuggestions.joinToString("  ·  ") { it.of(lang) }, type.body, maxLines = 2)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = 34.dp, end = C09.Margin, start = 20.dp)) {
                val shown = results.ifEmpty { MockCatalog.trending }
                Txt(
                    if (results.isEmpty()) tag("Trending in Riyadh", "رائج في الرياض")
                    else tag("${results.size} results", "${results.size} نتائج".toArabicDigits()),
                    type.tag.copy(color = C09.Pearl2),
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    shown.take(8).chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { t -> C09Poster(t, 94.dp) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C09Key(k: String, size: Dp, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Box(
        Modifier.size(size).clip(chamfer(6.dp))
            .background(if (focused) C09.Obsidian3 else C09.Obsidian2)
            .c09Trace({ trace.value }, 6.dp, bloom = false)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(k, type.label.copy(fontFamily = type.title.fontFamily, fontSize = 15.sp, color = if (focused) C09.CopperHi else C09.Pearl)) }
}

@Composable
internal fun C09Library(session: ProtoSession) {
    val type = c09Type()
    val clock by rememberProtoClock()
    val tabs = listOf(
        Bi("Saved", "المحفوظ") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Watched", "شوهدت") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C09Stage {
        Row(Modifier.fillMaxSize()) {
            C09NavRail(C09Section.LIBRARY, clock) { session.goTo(it) }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 40.dp, end = C09.Margin, top = 30.dp, bottom = 30.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt("مكتبتي", type.mega.copy(fontSize = 64.sp, lineHeight = 76.sp), maxLines = 1)
                    Spacer(Modifier.width(14.dp))
                    if (!isArabic()) Txt("LIBRARY", type.tag, Modifier.padding(bottom = 16.dp))
                }
                Spacer(Modifier.height(8.dp))
                val tabFocus = rememberTabRowFocus()
                Row(Modifier.tabRow(tabFocus), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { i, (name, items) ->
                        C09Tab(name.get() + "  " + num(items.size), selected = i == tab, Modifier.tabItem(tabFocus, i == tab)) { tab = i }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    tabs[tab].second.chunked(6).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEachIndexed { c, t -> C09Poster(t, 112.dp, if (r == 0 && c == 0) first else null) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C09Tab(label: String, selected: Boolean, modifier: Modifier, onFocus: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Box(
        modifier.clip(chamfer(8.dp))
            .background(if (selected) C09.Obsidian3 else Color.Transparent)
            .c09Trace({ trace.value }, 8.dp, base = if (selected) C09.Line else Color.Transparent, bloom = false)
            .protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onFocus)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Txt(label, type.label.copy(color = if (selected || focused) C09.Pearl else C09.Pearl3), maxLines = 1) }
}

private val settings = listOf(
    Bi("Interface", "الواجهة") to Bi("Arabic first, English beside", "العربية أولاً والإنجليزية بجانبها"),
    Bi("Numerals", "الأرقام") to Bi("Arabic-Indic  ٠١٢٣", "هندية  ٠١٢٣"),
    Bi("Calendar", "التقويم") to Bi("Hijri with Gregorian", "هجري مع الميلادي"),
    Bi("Subtitles", "الترجمة") to Bi("Arabic when available", "العربية متى توفرت"),
    Bi("Audio", "الصوت") to Bi("Original language", "اللغة الأصلية"),
    Bi("Playback", "التشغيل") to Bi("Best match, instant start", "الأنسب مع بدء فوري"),
)

@Composable
internal fun C09Profile(session: ProtoSession) {
    val type = c09Type()
    val fonts = LocalProtoFonts.current
    val clock by rememberProtoClock()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C09Stage {
        Row(Modifier.fillMaxSize()) {
            C09NavRail(C09Section.PROFILE, clock) { session.goTo(it) }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 40.dp, end = C09.Margin, top = 30.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt("من يشاهد؟", type.mega.copy(fontSize = 60.sp, lineHeight = 74.sp), maxLines = 1)
                    Spacer(Modifier.width(14.dp))
                    if (!isArabic()) Txt("WHO'S WATCHING", type.tag, Modifier.padding(bottom = 14.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    MockCatalog.profiles.forEachIndexed { i, p ->
                        var focused by remember { mutableStateOf(false) }
                        val trace = rememberTrace(focused)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(92.dp).clip(octagon(22.dp))
                                    .background(Brush.linearGradient(listOf(p.color.copy(alpha = 0.55f), C09.Obsidian2)))
                                    .c09Trace({ trace.value }, 22.dp)
                                    .protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it }, onClick = { session.nav.home() }),
                                contentAlignment = Alignment.Center,
                            ) {
                                // The avatar is the first Arabic letter of the name.
                                Txt(p.name.ar.take(1), ts(fonts.reemKufi, 42.sp, FontWeight.SemiBold, C09.Pearl, lineHeight = 52.sp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Txt(p.name.get(), type.label.copy(color = if (focused) C09.Pearl else C09.Pearl2))
                            if (p.kids) Txt(tag("Kids", "أطفال"), type.tag.copy(color = C09.Teal))
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                Txt(tag("This TV", "هذا التلفاز"), type.tag.copy(color = C09.Copper))
                Spacer(Modifier.height(8.dp))
                settings.chunked(3).forEach { row ->
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (k, v) -> C09Setting(k.get(), v.get(), Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun C09Setting(key: String, value: String, modifier: Modifier) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Row(
        modifier.clip(chamfer(8.dp)).background(if (focused) C09.Obsidian3 else C09.Obsidian2)
            .c09Trace({ trace.value }, 8.dp, bloom = false)
            .protoFocusable(onFocusChange = { focused = it }, onClick = {})
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(key, type.tag, maxLines = 1)
            Txt(value, type.label, maxLines = 1)
        }
        ProtoIcon(Glyph.CHEVRON_RIGHT, size = 12.dp, color = C09.Pearl3, stroke = 1.5.dp)
    }
}
