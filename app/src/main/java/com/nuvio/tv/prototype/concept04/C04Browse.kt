package com.nuvio.tv.prototype.concept04

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C04Search(session: ProtoSession) {
    val type = c04Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    var focused by remember { mutableStateOf(results.firstOrNull() ?: MockCatalog.hero) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C04Room(focused, dim = 0.5f) {
        Column(Modifier.fillMaxSize()) {
            C04NavCapsule(session, 1)
            Row(Modifier.fillMaxSize().padding(horizontal = C04.Margin, vertical = 16.dp)) {
                Column(Modifier.width(300.dp)) {
                    Row(Modifier.fillMaxWidth().glass(50.dp).padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProtoIcon(Glyph.SEARCH, size = 15.dp, color = C04.Ink, stroke = 1.8.dp)
                        Spacer(Modifier.width(10.dp))
                        Txt(query.ifEmpty { tr("Search", "ابحث") }, type.section.copy(color = if (query.isEmpty()) C04.Ink3 else C04.Ink), Modifier.weight(1f), maxLines = 1)
                        ProtoIcon(Glyph.MIC, size = 15.dp, color = C04.Ink2, stroke = 1.6.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.glass(24.dp).padding(10.dp)) {
                        ProtoKeyboard.keys(ar).chunked(6).forEachIndexed { r, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                                row.forEachIndexed { c, k -> C04Key(k, Modifier.weight(1f), if (r == 0 && c == 0) first else null) { query += k } }
                                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            C04Key(tr("space", "مسافة"), Modifier.weight(3f)) { query += " " }
                            C04Key("⌫", Modifier.weight(1.5f)) { query = query.dropLast(1) }
                            C04Key(tr("clear", "مسح"), Modifier.weight(1.5f)) { query = "" }
                        }
                    }
                }
                Spacer(Modifier.width(26.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Txt(if (results.isEmpty()) tr("Popular right now", "الأكثر رواجاً الآن") else tr("${results.size} results", "${results.size} نتائج"), type.section)
                    Spacer(Modifier.height(4.dp))
                    (results.ifEmpty { MockCatalog.trending }).chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                            row.forEach { t -> C04Card(t, 112.dp, poster = true, onFocusChange = { if (it) focused = t }) { session.nav.push(ProtoRoute.Details(t.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C04Key(label: String, modifier: Modifier, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(32.dp)
            .graphicsLayer { val s = if (focused) 1.12f else 1f; scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) C04.Ink else Color(0x14FFFFFF))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, type.label.copy(color = if (focused) C04.Dark else C04.Ink), maxLines = 1)
    }
}

@Composable
internal fun C04Library(session: ProtoSession) {
    val type = c04Type()
    val tabs = listOf(
        Bi("My List", "قائمتي") to MockCatalog.watchlist,
        Bi("Continue", "تابع") to MockCatalog.continueWatching,
        Bi("Watched", "شوهدت") to MockCatalog.watched,
    )
    var tab by remember { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(tabs[0].second.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C04Room(focused, dim = 0.45f) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            C04NavCapsule(session, 2)
            Row(Modifier.padding(horizontal = C04.Margin, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(tr("Library", "المكتبة"), type.hero, Modifier.weight(1f))
                C04Segmented(tabs.map { (l, items) -> "${l.get()} ${items.size}" }, tab, { tab = it }, firstRequester = first)
            }
            tabs[tab].second.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(horizontal = C04.Margin, vertical = 12.dp)) {
                    row.forEach { t ->
                        C04Card(t, 199.dp, poster = false, progress = if (tab == 1) t.progress else null, onFocusChange = { if (it) focused = t }) { session.nav.push(ProtoRoute.Details(t.id)) }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
internal fun C04Profile(session: ProtoSession) {
    val type = c04Type()
    var focusedTitle by remember { mutableStateOf<ProtoTitle>(MockCatalog.shogun) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val favourites = listOf(MockCatalog.shogun, MockCatalog.pastlives, MockCatalog.wadjda, MockCatalog.thebear, MockCatalog.parasite)
    C04Room(focusedTitle, dim = 0.4f) {
        Column(Modifier.fillMaxSize().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(tr("Who's watching?", "من يشاهد؟"), type.hero)
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                MockCatalog.profiles.forEachIndexed { i, p ->
                    var focused by remember { mutableStateOf(false) }
                    val fav = favourites[i]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .graphicsLayer { val s = if (focused) 1.12f else 1f; scaleX = s; scaleY = s }
                                .bloom(fav.palette.ui, if (focused) 1f else 0f, 30.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(Brush.linearGradient(listOf(fav.palette.horizon, fav.palette.mid, fav.palette.ground)))
                                .protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it; if (it) focusedTitle = fav }, onClick = { session.nav.home() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Txt(p.name.get().take(1), type.hero.copy(color = Color.White))
                        }
                        Spacer(Modifier.height(12.dp))
                        Txt(p.name.get(), type.label)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Txt(tr("Each profile's colour comes from the film they watch most.", "لون كل ملف مستوحى من الفيلم الأكثر مشاهدة لديه."), type.caption)
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple(Glyph.GLOBE, tr("Language", "اللغة"), tr("English · العربية", "العربية · English")),
                    Triple(Glyph.SUBTITLES, tr("Subtitles", "الترجمة"), tr("Arabic · soft shadow", "العربية · ظل ناعم")),
                    Triple(Glyph.SPARKLE, tr("Ambient colour", "الألوان المتكيفة"), tr("On · follows artwork", "مفعّلة · تتبع الصورة")),
                    Triple(Glyph.PLAY, tr("Playback", "التشغيل"), tr("Best match", "الأنسب")),
                ).forEach { (g, k, v) ->
                    var focused by remember { mutableStateOf(false) }
                    Column(
                        Modifier.width(170.dp).glass(22.dp, strong = focused).protoFocusable(onFocusChange = { focused = it }, onClick = {}).padding(16.dp),
                    ) {
                        ProtoIcon(g, size = 16.dp, color = C04.Ink, stroke = 1.6.dp)
                        Spacer(Modifier.height(12.dp))
                        Txt(k, type.label)
                        Txt(v, type.caption, maxLines = 1)
                    }
                }
            }
        }
    }
}
