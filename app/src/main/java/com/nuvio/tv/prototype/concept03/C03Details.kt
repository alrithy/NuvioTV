package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/** The aperture widens to become the picture; the wall beside it carries the specification. */
@Composable
internal fun C03Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c03Type()
    val lang = LocalProtoLang.current
    val open = remember { Animatable(0.62f) }
    LaunchedEffect(Unit) { open.animateTo(1f, tween(900, 100, ProtoEasing.Cinematic)) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C03Screen {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().weight(1.25f).background(C03.Shadow), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(open.value).stoneEdge()) {
                    ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(C03Lintel))
                }
            }
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(C03.Basalt)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp, vertical = 44.dp),
            ) {
                Txt((if (t.isSeries) tr("SERIES", "مسلسل") else tr("FEATURE", "فيلم")) + "  ·  " + t.year.toString().digits(), type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(12.dp))
                Txt(t.title.get().up(), type.monument, maxLines = 3)
                Spacer(Modifier.height(10.dp))
                Txt(t.tagline.get(), type.body.copy(color = C03.BronzeLight), maxLines = 2)
                Spacer(Modifier.height(20.dp))
                C03Rule(Modifier.fillMaxWidth())
                C03Spec(tr("Length", "المدة"), if (t.isSeries) tr("${t.totalEpisodes} episodes", "${t.totalEpisodes} حلقة") else formatRuntime(t.runtimeMin, lang))
                C03Spec(tr("Picture", "الصورة"), t.tech.labels().filter { it != "Dolby Atmos" }.joinToString(" · ").ifEmpty { "HD" })
                C03Spec(tr("Sound", "الصوت"), if (t.tech.atmos) "Dolby Atmos" else "5.1")
                C03Spec(tr("Rating", "التصنيف"), "${t.cert} · IMDb ${t.rating}")
                C03Spec(tr("Direction", "الإخراج"), t.director.get())
                Spacer(Modifier.height(16.dp))
                Txt(t.synopsis.get(), type.body, maxLines = 5)
                Spacer(Modifier.height(22.dp))
                val playLabel = when {
                    t.isSeries && t.resume != null -> tr("RESUME  ·  S${t.resume!!.season} E${t.resume!!.number}", "استئناف · الحلقة ${t.resume!!.number}")
                    t.progress != null -> tr("RESUME  ·  ${t.remainingMin} MIN", "استئناف · ${t.remainingMin} دقيقة")
                    else -> tr("PLAY", "تشغيل")
                }
                C03SlabButton(playLabel, Modifier.fillMaxWidth(), play, primary = true) { session.nav.push(ProtoRoute.Streams(t.id)) }
                Spacer(Modifier.height(8.dp))
                if (t.isSeries) {
                    C03SlabButton(tr("EPISODES", "الحلقات"), Modifier.fillMaxWidth()) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C03SlabButton(tr("SOURCES", "المصادر"), Modifier.weight(1f)) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    C03SlabButton(tr("TRAILER", "الإعلان"), Modifier.weight(1f)) {}
                }
                Spacer(Modifier.height(30.dp))
                Txt(tr("CAST", "الممثلون"), type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(8.dp))
                t.cast.forEach { c -> C03Spec(c.role.get(), c.name.get(), 150.dp) }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
internal fun C03Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c03Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]

    C03Screen {
        Row(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 44.dp)) {
            val seasonFocus = rememberTabRowFocus()
            Column(Modifier.width(120.dp).fillMaxHeight().tabRow(seasonFocus)) {
                Txt(t.title.get().up(), type.label.copy(color = C03.Sand), maxLines = 2)
                Spacer(Modifier.height(24.dp))
                t.seasons.forEachIndexed { i, s ->
                    C03ListItem(
                        tr("SEASON ", "الموسم ") + numeral(s.number),
                        Modifier.tabItem(seasonFocus, i == seasonIdx),
                        style = type.label,
                        maxScale = 1.15f,
                        selected = i == seasonIdx,
                        onFocus = { seasonIdx = i },
                    ) { seasonIdx = i }
                }
            }
            C03VRule(Modifier.fillMaxHeight())
            Spacer(Modifier.width(26.dp))
            Column(Modifier.width(380.dp).fillMaxHeight().verticalScroll(session.scroll("c03.eps.$seasonIdx"))) {
                Spacer(Modifier.height(120.dp))
                season.episodes.forEachIndexed { i, e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(
                            e.number.toString().padStart(2, '0').digits(),
                            type.heading.copy(color = if (i == focusedEp) C03.BronzeLight else C03.SandFaint),
                            Modifier.width(52.dp),
                        )
                        C03ListItem(
                            e.title.get().up(),
                            Modifier.weight(1f),
                            requester = reqs[i],
                            style = type.label.copy(color = C03.Sand),
                            maxScale = 1.3f,
                            onFocus = { focusedEp = i },
                        ) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    }
                }
                Spacer(Modifier.height(160.dp))
            }
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.fillMaxWidth().weight(1f).stoneEdge().background(C03.Shadow)) {
                    Crossfade(ep, animationSpec = tween(700, easing = ProtoEasing.Cinematic), label = "still") { e ->
                        ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.02f; scaleY = 1.02f }, episode = e)
                    }
                    Box(Modifier.fillMaxSize().background(C03Lintel))
                }
                Spacer(Modifier.height(14.dp))
                Txt(ep.number.toString().padStart(2, '0').digits() + "  ·  " + tr("${ep.runtimeMin} MIN", "${ep.runtimeMin} دقيقة") + if (ep.watched) tr("  ·  SEEN", "  ·  شوهدت") else if (ep.isNew) tr("  ·  NEW", "  ·  جديدة") else "", type.label.copy(color = C03.Bronze))
                Spacer(Modifier.height(6.dp))
                Txt(ep.synopsis.get(), type.body, maxLines = 2)
            }
        }
    }
}
