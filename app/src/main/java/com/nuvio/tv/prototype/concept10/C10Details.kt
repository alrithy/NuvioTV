package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.Episode
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.data.fmt
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C10Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c10Type()
    val lang = LocalProtoLang.current
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    val scroll = session.scroll("c10.details.${t.id}")
    val more = remember(t.id) { (MockCatalog.becauseDune + MockCatalog.hiddenGems).distinct().filter { it.id != t.id }.take(7) }

    Box(Modifier.fillMaxSize()) {
        C10Ambient(t.palette, strength = 1.2f)
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.74f).fillMaxHeight(0.88f).graphicsLayer { translationY = -scroll.value * 0.4f }) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), alignment = Alignment.TopEnd)
            Box(Modifier.fillMaxSize().background(startScrim(0f to C10.Black, 0.45f to C10.Black.copy(alpha = 0.55f), 0.8f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to C10.Black)))
        }
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(Modifier.padding(start = C10.Margin, top = 78.dp).width(500.dp)) {
                Txt(over(if (t.isSeries) "Series · ${t.primaryGenre.en}" else "Film · ${t.primaryGenre.en}", (if (t.isSeries) "مسلسل · " else "فيلم · ") + t.primaryGenre.ar), type.overline)
                Spacer(Modifier.height(6.dp))
                TitleReveal(t.title.get(), type.hero, key = t.id)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(metaLine(t, withGenre = false) + "  ·  ★ " + fmt(t.rating, 1), type.body.copy(color = C10.Ink), maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    t.tech.labels().take(3).forEach { C10Chip(it); Spacer(Modifier.width(5.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Txt(t.synopsis.get(), type.body, maxLines = 3)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(C10.Gold))
                    Spacer(Modifier.width(8.dp))
                    Txt(bestLine(t), type.caption.copy(color = C10.Ink), maxLines = 1)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    C10Button(playLabel(t), Glyph.PLAY, play, primary = true) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    if (t.isSeries) C10Button(tr("Episodes", "الحلقات"), Glyph.EPISODES) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                    C10Button(tr("Trailer", "الإعلان"), Glyph.TRAILER) {}
                    C10Button(tr("Sources", "المصادر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    C10Button("", Glyph.PLUS) {}
                }
                Spacer(Modifier.height(18.dp))
                Txt(
                    t.director.get() + "   ·   " + t.cast.take(3).joinToString(if (lang == ProtoLang.AR) "، " else ", ") { it.name.of(lang) },
                    type.caption, maxLines = 1,
                )
            }
            Spacer(Modifier.height(40.dp))
            C10RowHeader(tr("More like this", "أعمال مشابهة"))
            C10RowPivot {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(session.scroll("c10.more.${t.id}")).padding(horizontal = C10.Margin).padding(top = 8.dp, bottom = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    more.forEach { m -> C10Card(m, ArtKind.POSTER, 112.dp, 2f / 3f) { session.nav.push(ProtoRoute.Details(m.id)) } }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Which episode the app thinks you want next: the one in progress, else the first unwatched. */
private fun upNext(t: ProtoTitle): Episode? {
    val all = t.seasons.flatMap { it.episodes }
    val r = t.resume
    if (r != null && (r.progress ?: 0f) < 0.9f) return r
    return all.firstOrNull { !it.watched }
}

@Composable
internal fun C10Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c10Type()
    val lang = LocalProtoLang.current
    val next = remember(t.id) { upNext(t) }
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = season.episodes.indexOfFirst { it == next }.takeIf { it >= 0 } ?: 0
    var focusIdx by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start], key = seasonIdx)
    val scroll = session.scroll("c10.eps.$seasonIdx")
    val density = LocalDensity.current
    // Open on the up-next episode sitting on the row's start line, where focus rests.
    LaunchedEffect(seasonIdx) {
        withFrameNanos { }
        scroll.scrollTo(with(density) { (start * (248.dp + 14.dp).toPx()).toInt() })
    }
    val ep = season.episodes[focusIdx.coerceIn(0, season.episodes.lastIndex)]

    Box(Modifier.fillMaxSize()) {
        C10Ambient(t.palette)
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.62f).height(330.dp)) {
            Crossfade(ep, animationSpec = tween(500, easing = ProtoEasing.Decelerate), label = "still") { e ->
                ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize().graphicsLayer { alpha = 0.7f }, episode = e)
            }
            Box(Modifier.fillMaxSize().background(startScrim(0f to C10.Black, 0.5f to C10.Black.copy(alpha = 0.4f), 1f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to C10.Black)))
        }
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            val seasonFocus = rememberTabRowFocus()
            Row(Modifier.padding(horizontal = C10.Margin).tabRow(seasonFocus), verticalAlignment = Alignment.CenterVertically) {
                Txt(t.title.get(), type.title, maxLines = 1)
                Spacer(Modifier.width(24.dp))
                t.seasons.forEachIndexed { i, s ->
                    C10Tab(tr("Season ${s.number}", "الموسم ${s.number}"), selected = i == seasonIdx, modifier = Modifier.tabItem(seasonFocus, i == seasonIdx).padding(end = 14.dp), onFocus = { seasonIdx = i })
                }
            }
            Spacer(Modifier.height(30.dp))
            AnimatedContent(ep, transitionSpec = { fadeIn(tween(280, 60)) togetherWith fadeOut(tween(120)) }, label = "ep", modifier = Modifier.padding(horizontal = C10.Margin)) { e ->
                Column(Modifier.width(470.dp)) {
                    val status = when {
                        e == next && e.progress != null -> over("Up next · resume", "التالي · استئناف")
                        e == next -> over("Up next", "التالي")
                        e.isNew -> over("New this week", "جديدة هذا الأسبوع")
                        e.watched -> over("Watched", "شوهدت")
                        else -> over("Episode ${e.number}", "الحلقة ${e.number}")
                    }
                    Txt(status, type.overline)
                    Spacer(Modifier.height(4.dp))
                    Txt("${e.number} · " + e.title.get(), type.headline, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Txt(e.synopsis.get(), type.body, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    val left = e.progress?.let { ((1f - it) * e.runtimeMin).toInt() }
                    Txt(
                        if (left != null) tr("${e.runtimeMin} min · $left min left", "${e.runtimeMin} دقيقة · متبقٍ $left") else tr("${e.runtimeMin} min", "${e.runtimeMin} دقيقة") +
                            (e.airDate?.let { "  ·  " + it.of(lang) } ?: ""),
                        type.caption, maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            C10RowPivot {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = C10.Margin).padding(top = 8.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    season.episodes.forEachIndexed { i, e ->
                        C10Card(t, ArtKind.STILL, 248.dp, 16f / 9f, requester = reqs[i], episode = e, onFocus = { if (it) focusIdx = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) }, overlay = { _ ->
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xD9000000))))
                            if (e == next) Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(C10.Gold).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Txt(over("Up next", "التالي"), type.overline.copy(color = C10.Black))
                            }
                            else if (e.isNew) Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(C10.Ink.copy(alpha = 0.14f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Txt(over("New", "جديدة"), type.overline.copy(color = C10.Ink))
                            }
                            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                                Txt("${e.number}  " + e.title.get(), type.label, Modifier.padding(horizontal = 12.dp), maxLines = 1)
                                val p = e.progress ?: if (e.watched) 1f else null
                                if (p != null) C10Progress(p) else Spacer(Modifier.height(10.dp))
                            }
                        })
                    }
                }
            }
        }
    }
}
