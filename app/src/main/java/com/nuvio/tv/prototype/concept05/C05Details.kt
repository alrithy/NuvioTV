package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C05Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c05Type()
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(startScrim(0f to Color(0xD9000000), 0.55f to Color(0x66000000), 1f to Color.Transparent)))
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = C05.Margin).width(560.dp)) {
            Txt(metaLine(t, "  /  ") + "  /  IMDb ${t.rating}", type.slate)
            Spacer(Modifier.height(10.dp))
            Txt(t.title.get(), type.big, maxLines = 2)
            Spacer(Modifier.height(14.dp))
            Txt(t.synopsis.get(), type.body.copy(color = C05.Ink), maxLines = 4)
            Spacer(Modifier.height(14.dp))
            val castLine = if (isArabic()) "بطولة " + t.cast.take(3).joinToString("، ") { it.name.ar } else "WITH " + t.cast.take(3).joinToString(", ") { it.name.en.uppercase() }
            Txt(castLine, type.slateSmall, maxLines = 1)
            Txt(t.tech.labels().joinToString("  /  ").slate(), type.slateSmall, maxLines = 1)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                C05Action(if (t.progress != null) tr("Resume", "استئناف") else tr("Play", "تشغيل"), Glyph.PLAY, play) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C05Action(tr("Episodes", "الحلقات"), Glyph.EPISODES) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                C05Action(tr("Sources", "المصادر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                C05Action(tr("Trailer", "الإعلان"), Glyph.TRAILER) {}
                C05Action(tr("Keep", "احفظ"), Glyph.PLUS) {}
            }
        }
    }
}

/** The screen becomes the focused episode's still; a thin filmstrip is the only UI. */
@Composable
internal fun C05Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c05Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(ep, animationSpec = tween(600), label = "still") { e -> ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xE6000000))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 34.dp)) {
            Column(Modifier.padding(horizontal = C05.Margin)) {
                val seasonFocus = rememberTabRowFocus()
                Row(Modifier.tabRow(seasonFocus), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    t.seasons.forEachIndexed { i, s ->
                        C05Action(tr("S${s.number}", "م${s.number}") + if (i == seasonIdx) "  •" else "", modifier = Modifier.tabItem(seasonFocus, i == seasonIdx), onFocus = { seasonIdx = i }) { seasonIdx = i }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Txt("${t.title.get().slate()}  /  S${ep.season.toString().padStart(2, '0')} E${ep.number.toString().padStart(2, '0')}  /  ${ep.runtimeMin} MIN" + if (ep.isNew) "  /  NEW" else "", type.slate)
                Txt(ep.title.get(), type.title, maxLines = 1)
                Txt(ep.synopsis.get(), type.body, maxLines = 1)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(session.scroll("c05.eps.$seasonIdx")).padding(horizontal = C05.Margin), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                season.episodes.forEachIndexed { i, e ->
                    var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                    val f by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "e")
                    Box(
                        Modifier
                            .width(112.dp)
                            .aspectRatio(16f / 9f)
                            .graphicsLayer { alpha = 0.45f + 0.55f * f }
                            .border(1.dp, if (focused) C05.Ink else Color.Transparent)
                            .protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) }),
                    ) {
                        ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
                        val p = e.progress ?: if (e.watched) 1f else null
                        if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(2.dp).background(C05.Signal))
                    }
                }
                Spacer(Modifier.width(600.dp))
            }
        }
    }
}
