package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
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
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/** The artwork glows into the room: its light bleeds past the frame onto black. */
@Composable
internal fun C08Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c08Type()
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    C08Screen(t.palette, strength = 0.9f) {
        Row(Modifier.fillMaxSize().padding(horizontal = C08.Margin), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(metaLine(t), type.small)
                Spacer(Modifier.height(8.dp))
                Txt(t.title.get(), type.hero, maxLines = 2)
                Spacer(Modifier.height(10.dp))
                Txt(t.synopsis.get(), type.body, maxLines = 4)
                Spacer(Modifier.height(8.dp))
                Txt(t.tech.labels().joinToString("   "), type.small)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    C08Pill(if (t.progress != null) tr("Resume", "استئناف") else tr("Play", "تشغيل"), Glyph.PLAY, play, glow = t.palette.ui) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    if (t.isSeries) C08Pill(tr("Episodes", "الحلقات"), glow = t.palette.ui) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                    C08Pill(tr("Sources", "المصادر"), glow = t.palette.ui) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    C08Pill("", Glyph.PLUS, glow = t.palette.ui) {}
                }
            }
            Spacer(Modifier.width(40.dp))
            Box(Modifier.width(440.dp).aspectRatio(16f / 10f).halo(t.palette.ui, 1f).clip(RoundedCornerShape(36.dp))) {
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
internal fun C08Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c08Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableStateOf<Int?>(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[(focusedEp ?: start).coerceIn(0, season.episodes.lastIndex)]
    C08Screen(t.palette) {
        Column(Modifier.fillMaxSize().padding(top = 48.dp)) {
            Row(Modifier.padding(horizontal = C08.Margin), verticalAlignment = Alignment.CenterVertically) {
                Txt(t.title.get(), type.hero, Modifier.weight(1f), maxLines = 1)
                t.seasons.forEachIndexed { i, s ->
                    C08Choice(tr("Season ${s.number}", "الموسم ${s.number}"), selected = i == seasonIdx, glow = t.palette.ui, onFocus = { seasonIdx = i }) { seasonIdx = i }
                }
            }
            AnimatedContent(ep, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(200)) }, label = "ep", modifier = Modifier.padding(horizontal = C08.Margin, vertical = 24.dp)) { e ->
                Column(Modifier.width(620.dp)) {
                    Txt(tr("Episode ${e.number} · ${e.runtimeMin} min", "الحلقة ${e.number} · ${e.runtimeMin} دقيقة"), type.small)
                    Txt(e.title.get(), type.hero.copy(fontSize = type.hero.fontSize * 0.8f), maxLines = 1)
                    Txt(e.synopsis.get(), type.body, maxLines = 2)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.horizontalScroll(session.scroll("c08.eps.$seasonIdx")).padding(horizontal = C08.Margin).padding(bottom = 40.dp).height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                season.episodes.forEachIndexed { i, e ->
                    val w = dockSize(i, focusedEp, 150.dp, 1.3f)
                    var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                    val f by animateFloatAsState(if (focused) 1f else 0f, tween(340, easing = ProtoEasing.Decelerate), label = "e")
                    Box(
                        Modifier.width(w).aspectRatio(16f / 9f).halo(t.palette.ui, f).clip(RoundedCornerShape(22.dp))
                            .protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i else if (focusedEp == i) focusedEp = null }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) }),
                    ) {
                        ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize().graphicsLayer { alpha = 0.7f + 0.3f * f }, episode = e)
                        val p = e.progress ?: if (e.watched) 1f else null
                        if (p != null) Box(Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                            Box(Modifier.fillMaxWidth(p).height(3.dp).background(Color.White))
                        }
                    }
                }
                Spacer(Modifier.width(500.dp))
            }
        }
    }
}

@Composable
internal fun C08Search(session: ProtoSession) {
    val type = c08Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    var focusedIdx by remember { mutableStateOf<Int?>(null) }
    val shown = results.ifEmpty { MockCatalog.trending }
    val lit = shown[(focusedIdx ?: 0).coerceIn(0, shown.lastIndex)]
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C08Screen(lit.palette) {
        Column(Modifier.fillMaxSize().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.width(520.dp).clip(RoundedCornerShape(50)).background(C08.Soft).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                ProtoIcon(Glyph.SEARCH, size = 18.dp, color = C08.Ink, stroke = 1.5.dp)
                Spacer(Modifier.width(14.dp))
                Txt(query.ifEmpty { tr("What would you like to watch?", "ماذا تود أن تشاهد؟") }, type.title.copy(color = if (query.isEmpty()) C08.Ink3 else C08.Ink), Modifier.weight(1f), maxLines = 1)
                ProtoIcon(Glyph.MIC, size = 18.dp, color = C08.Ink2, stroke = 1.5.dp)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = C08.Margin), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProtoKeyboard.keys(ar).forEachIndexed { i, k -> C08Key(k, if (i == 0) first else null) { query += k } }
                C08Key("⌫") { query = query.dropLast(1) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(lit, transitionSpec = { fadeIn(tween(500, 150)) togetherWith fadeOut(tween(200)) }, label = "top") { l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt(if (focusedIdx == null) tr("Top result", "أفضل نتيجة") else metaLine(l), type.small)
                    Txt(l.title.get(), type.hero, maxLines = 1)
                    if (focusedIdx == null) Txt(metaLine(l), type.body, maxLines = 1)
                }
            }
            Spacer(Modifier.weight(1f))
            Txt(if (results.isEmpty()) tr("Popular this evening", "رائج هذا المساء") else tr("${results.size} results", "${results.size} نتائج"), type.small)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(bottom = 40.dp).height(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                shown.take(7).forEachIndexed { i, t ->
                    C08DockTile(t, dockSize(i, focusedIdx, 84.dp), null, focusedIdx == i, onFocus = { f -> if (f) focusedIdx = i else if (focusedIdx == i) focusedIdx = null }) { session.nav.push(ProtoRoute.Details(t.id)) }
                }
            }
        }
    }
}

@Composable
private fun C08Key(k: String, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c08Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(260), label = "k")
    Box(
        Modifier.size(34.dp).graphicsLayer { scaleX = 1f + 0.2f * f; scaleY = 1f + 0.2f * f }.clip(CircleShape)
            .background(if (focused) C08.Ink else C08.Soft)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(k, type.label.copy(color = if (focused) C08.Black else C08.Ink)) }
}

@Composable
internal fun C08Library(session: ProtoSession) {
    val type = c08Type()
    val shelves = listOf(
        Bi("Saved for later", "محفوظ لوقت لاحق") to MockCatalog.watchlist,
        Bi("Still watching", "ما زلت تشاهد") to MockCatalog.continueWatching,
        Bi("Watched", "شاهدتها") to MockCatalog.watched,
    )
    var lit by remember { mutableStateOf<ProtoTitle>(MockCatalog.watchlist.first()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C08Screen(lit.palette, strength = 0.6f) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = C08.Margin, vertical = 40.dp)) {
            Txt(tr("Library", "المكتبة"), type.hero)
            shelves.forEachIndexed { s, (name, items) ->
                var focusedIdx by remember { mutableStateOf<Int?>(null) }
                val rowHeight by animateDpAsState(if (focusedIdx != null) 124.dp else 76.dp, tween(340, easing = ProtoEasing.Decelerate), label = "shelf")
                Spacer(Modifier.height(16.dp))
                Txt(name.get(), type.small)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.height(rowHeight), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    items.forEachIndexed { i, t ->
                        C08DockTile(t, dockSize(i, focusedIdx, 72.dp, 1.28f), if (s == 0 && i == 0) first else null, focusedIdx == i, onFocus = { f ->
                            if (f) { focusedIdx = i; lit = t } else if (focusedIdx == i) focusedIdx = null
                        }) { session.nav.push(ProtoRoute.Details(t.id)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun C08Profile(session: ProtoSession) {
    val type = c08Type()
    val lang = LocalProtoLang.current
    var idx by remember { mutableIntStateOf(0) }
    val favourites = listOf(MockCatalog.dune2, MockCatalog.pastlives, MockCatalog.severance, MockCatalog.wadjda, MockCatalog.arrival)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    C08Screen(favourites[idx].palette) {
        Column(Modifier.fillMaxSize().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(tr("Who's here?", "من هنا؟"), type.hero)
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                MockCatalog.profiles.forEachIndexed { i, p ->
                    var focused by remember { mutableStateOf(false) }
                    val f by animateFloatAsState(if (focused) 1f else 0f, tween(360), label = "o")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(84.dp).graphicsLayer { scaleX = 1f + 0.15f * f; scaleY = 1f + 0.15f * f }.halo(p.color, f).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(p.color, p.color.copy(alpha = 0.25f))))
                                .protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it; if (it) idx = i }, onClick = { session.nav.home() }),
                        )
                        Spacer(Modifier.height(12.dp))
                        Txt(p.name.get(), type.label.copy(color = if (focused) C08.Ink else C08.Ink2))
                    }
                }
            }
            Spacer(Modifier.height(44.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C08Pill(tr("Ambient after 5 min", "الوضع المحيطي بعد 5 دقائق")) {}
                C08Pill(tr("Night dimming on", "خفوت ليلي مفعّل")) {}
                C08Pill(tr("OLED pixel shift", "إزاحة بكسلات OLED")) {}
                C08Pill(if (lang.isRtl) "English" else "العربية") {}
            }
        }
    }
}
