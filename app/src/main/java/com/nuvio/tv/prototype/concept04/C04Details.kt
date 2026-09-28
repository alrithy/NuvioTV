package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
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
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/** The home window grows into the page: a large rounded frame that settles into place. */
@Composable
internal fun C04Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c04Type()
    val accent = rememberAccent(t)
    val grow = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(700, easing = ProtoEasing.Decelerate)) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    var tab by remember { mutableIntStateOf(0) }

    C04Room(t, dim = 0.45f) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = C04.Margin)) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .graphicsLayer { scaleX = grow.value; scaleY = grow.value }
                    .bloom(accent, 0.7f, C04.WindowRadius)
                    .clip(RoundedCornerShape(C04.WindowRadius)),
            ) {
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xB3000000))))
                Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                    Txt(t.title.get(), type.hero, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Txt(metaLine(t) + "  ·  IMDb ${t.rating}", type.meta.copy(color = C04.Ink2))
                }
                Row(Modifier.align(Alignment.BottomEnd).padding(22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    t.tech.labels().forEach { C04Chip(it) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = when {
                    t.isSeries && t.resume != null -> tr("Resume S${t.resume!!.season} · E${t.resume!!.number}", "استئناف م${t.resume!!.season} · ح${t.resume!!.number}")
                    t.progress != null -> tr("Resume · ${t.remainingMin} min left", "استئناف · متبقٍ ${t.remainingMin} دقيقة")
                    else -> tr("Play", "تشغيل")
                }
                C04Pill(label, Glyph.PLAY, primary = true, accent = accent, requester = play) { session.nav.push(ProtoRoute.Streams(t.id)) }
                Spacer(Modifier.width(10.dp))
                if (t.isSeries) {
                    C04Pill(tr("Episodes", "الحلقات"), Glyph.EPISODES, accent = accent) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                    Spacer(Modifier.width(10.dp))
                }
                C04Pill(tr("Sources", "المصادر"), Glyph.SOURCES, accent = accent) { session.nav.push(ProtoRoute.Streams(t.id)) }
                Spacer(Modifier.width(10.dp))
                C04Pill(tr("Trailer", "الإعلان"), Glyph.TRAILER, accent = accent) {}
                Spacer(Modifier.width(10.dp))
                C04Pill("", Glyph.PLUS, accent = accent) {}
                Spacer(Modifier.weight(1f))
                C04Segmented(listOf(tr("Overview", "نظرة عامة"), tr("Cast", "الممثلون"), tr("More", "المزيد")), tab, { tab = it })
            }
            Spacer(Modifier.height(18.dp))
            AnimatedContent(tab, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(140)) }, label = "tab") { tb ->
                when (tb) {
                    0 -> Row {
                        Txt(t.synopsis.get(), type.body.copy(color = C04.Ink), Modifier.width(520.dp), maxLines = 4)
                        Spacer(Modifier.width(40.dp))
                        Column {
                            Txt(t.director.get(), type.label)
                            Txt(t.genres.map { it.get() }.joinToString(" · "), type.caption)
                            t.award?.let { Spacer(Modifier.height(8.dp)); C04Chip(it.get()) }
                        }
                    }
                    1 -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        t.cast.forEach { c ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                                var focused by remember { mutableStateOf(false) }
                                Box(
                                    Modifier.size(64.dp).bloom(t.palette.ui, if (focused) 1f else 0f, 50.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(t.palette.mid, t.palette.ground)))
                                        .protoFocusable(onFocusChange = { focused = it }, onClick = {}),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Txt(c.name.get().split(" ").take(2).joinToString("") { it.take(1) }, type.title)
                                }
                                Spacer(Modifier.height(8.dp))
                                Txt(c.name.get(), type.label, maxLines = 1)
                                Txt(c.role.get(), type.caption, maxLines = 1)
                            }
                        }
                    }
                    else -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MockCatalog.becauseDune.filter { it.id != t.id }.forEach { m -> C04Card(m, 180.dp, poster = false) { session.nav.push(ProtoRoute.Details(m.id)) } }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
internal fun C04Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c04Type()
    val accent = rememberAccent(t)
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableStateOf<Int?>(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[(focusedEp ?: start).coerceIn(0, season.episodes.lastIndex)]

    C04Room(t, dim = 0.5f) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            Row(Modifier.padding(horizontal = C04.Margin), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(t.title.get(), type.hero)
                    Txt(tr("${season.episodes.size} episodes · ${season.year}", "${season.episodes.size} حلقات · ${season.year}"), type.meta)
                }
                C04Segmented(t.seasons.map { tr("Season ${it.number}", "الموسم ${it.number}") }, seasonIdx, { seasonIdx = it })
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(session.scroll("c04.eps.$seasonIdx")).padding(horizontal = C04.Margin, vertical = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                season.episodes.forEachIndexed { i, e ->
                    var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                    val dx = displacement(i, focusedEp, focusedEp != null)
                    Column(Modifier.width(250.dp).graphicsLayer { translationX = dx * density }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(141.dp)
                                .graphicsLayer { val s = if (focused) 1.08f else 1f; scaleX = s; scaleY = s }
                                .bloom(accent, if (focused) 1f else 0f)
                                .clip(RoundedCornerShape(C04.CardRadius))
                                .protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) }),
                        ) {
                            ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0x99000000))))
                            Txt("${e.number}", type.hero.copy(fontSize = type.hero.fontSize * 0.9f), Modifier.align(Alignment.BottomStart).padding(12.dp))
                            val p = e.progress ?: if (e.watched) 1f else null
                            if (p != null) Box(Modifier.align(Alignment.BottomEnd).padding(14.dp).width(70.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                                Box(Modifier.fillMaxWidth(p).height(4.dp).background(Color.White))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Txt(e.title.get(), type.label, maxLines = 1)
                        Txt(tr("${e.runtimeMin} min", "${e.runtimeMin} دقيقة") + if (e.isNew) tr(" · New", " · جديدة") else "", type.caption)
                    }
                }
                Spacer(Modifier.width(500.dp))
            }
            AnimatedContent(ep, transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(140)) }, label = "ep", modifier = Modifier.padding(horizontal = C04.Margin)) { e ->
                Column(Modifier.glass(22.dp).padding(18.dp).width(560.dp)) {
                    Txt(tr("Episode ${e.number}", "الحلقة ${e.number}") + " · " + e.title.get(), type.title)
                    Spacer(Modifier.height(6.dp))
                    Txt(e.synopsis.get(), type.body, maxLines = 2)
                }
            }
        }
    }
}
