package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C06Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c06Type()
    val lang = LocalProtoLang.current
    val best = remember(t.id) { StreamIntelligence.best(StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode)) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C06Screen(session, if (t.isSeries) 2 else 1, t.title.get()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Row(Modifier.fillMaxWidth().height(300.dp), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.weight(1.25f).fillMaxHeight(), focusable = false, padding = 0.dp) {
                    Box(Modifier.fillMaxSize()) {
                        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE60B0C0E))))
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Txt(t.title.get(), type.display, maxLines = 2)
                            Txt(metaLine(t) + "  ·  " + t.director.get(), type.caption.copy(color = C06.Ink2), maxLines = 1)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                    C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false) {
                        C06Label(tr("Specification", "المواصفات"))
                        Spacer(Modifier.height(8.dp))
                        val s = best.stream
                        C06Spec(tr("Resolution", "الدقة"), s.resolution.label)
                        C06Spec(tr("Dynamic range", "المدى الديناميكي"), s.hdrLabel)
                        C06Spec(tr("Audio", "الصوت"), s.audioLabel)
                        C06Spec(tr("Bitrate", "معدل البت"), "${s.bitrateLabel} ${tr("avg", "متوسط")}")
                        C06Spec(tr("Runtime", "المدة"), if (t.isSeries) tr("${t.totalEpisodes} episodes", "${t.totalEpisodes} حلقة") else formatRuntime(t.runtimeMin, lang))
                        C06Spec(tr("Rating", "التقييم"), "${t.cert} · IMDb ${t.rating}")
                    }
                    C06Module(Modifier.fillMaxWidth(), focusable = false) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            C06Button(if (t.progress != null) tr("Resume", "استئناف") else tr("Play", "تشغيل"), Glyph.PLAY, primary = true, requester = play) { session.nav.push(ProtoRoute.Streams(t.id)) }
                            if (t.isSeries) C06Button(tr("Episodes", "الحلقات"), Glyph.EPISODES) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                            C06Button(tr("Sources", "المصادر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            C06Button(tr("Trailer", "الإعلان"), Glyph.TRAILER) {}
                            C06Button(tr("My list", "قائمتي"), Glyph.PLUS) {}
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.weight(1.25f), focusable = false) {
                    C06Label(tr("Synopsis", "القصة"))
                    Spacer(Modifier.height(6.dp))
                    Txt(t.synopsis.get(), type.body, maxLines = 4)
                }
                C06Module(Modifier.weight(1f), focusable = false) {
                    C06Label(tr("Instant start check", "فحص البدء الفوري"))
                    Spacer(Modifier.height(8.dp))
                    best.reasons.take(4).forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(C06.Ok))
                            Spacer(Modifier.width(8.dp))
                            Txt(r.get(), type.value, maxLines = 1)
                        }
                    }
                }
            }
            C06Module(Modifier.fillMaxWidth(), focusable = false) {
                C06Label(tr("Cast", "الممثلون"))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                    t.cast.forEach { c ->
                        var focused by remember { mutableStateOf(false) }
                        val f = detent(focused)
                        Column(Modifier.width(140.dp).module(f, 8.dp).protoFocusable(onFocusChange = { focused = it }, onClick = {}).padding(10.dp)) {
                            Txt(c.name.get(), type.value, maxLines = 1)
                            Txt(c.role.get(), type.caption, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun C06Spec(label: String, value: String) {
    val type = c06Type()
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Txt(label.lbl(), type.label, Modifier.width(118.dp), maxLines = 1)
        Txt(value, type.value, maxLines = 1)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}

/**
 * The episode selector is a rotary dial: episodes sit on an arc, the one under the ember pointer
 * is selected, and up/down turns the dial with a detent.
 */
@Composable
internal fun C06Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c06Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(start) }
    val dial by animateFloatAsState(focusedEp.toFloat(), tween(C06.DETENT + 80, easing = ProtoEasing.Detent), label = "dial")
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]

    C06Screen(session, 2, t.title.get() + " · " + tr("Episodes", "الحلقات")) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth(), focusable = false) {
                    val seasonFocus = rememberTabRowFocus()
                    Row(Modifier.tabRow(seasonFocus), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        t.seasons.forEachIndexed { i, s ->
                            C06Button(tr("S${s.number}", "م${s.number}") + if (i == seasonIdx) " ●" else "", modifier = Modifier.tabItem(seasonFocus, i == seasonIdx), onFocus = { seasonIdx = i }) { seasonIdx = i }
                        }
                    }
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false, padding = 0.dp) {
                    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                        val cy = maxHeight / 2
                        // Pointer
                        Box(Modifier.offset(x = 6.dp, y = cy - 6.dp).size(12.dp)) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, C06.Ember)
                            }
                        }
                        season.episodes.forEachIndexed { i, e ->
                            val d = i - dial
                            val x = 26.dp + (d * d * 5f).coerceAtMost(80f).dp
                            val y = cy - 20.dp + (d * 46f).dp
                            var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                            Row(
                                Modifier
                                    .offset(x = x, y = y)
                                    .graphicsLayer { alpha = (1f - kotlin.math.abs(d) * 0.22f).coerceIn(0.1f, 1f) }
                                    .protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) }),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Txt(e.number.toString().padStart(2, '0'), type.readoutBig.copy(color = if (focused) C06.Ember else C06.Ink2), Modifier.width(48.dp))
                                Column {
                                    Txt(e.title.get(), type.value.copy(color = if (focused) C06.Ink else C06.Ink2), maxLines = 1)
                                    Txt(tr("${e.runtimeMin} min", "${e.runtimeMin} د") + if (e.watched) tr(" · watched", " · شوهدت") else if (e.isNew) tr(" · new", " · جديدة") else "", type.caption, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth(), focusable = false, padding = 6.dp) {
                    AnimatedContent(ep, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "still") { e ->
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))) {
                            ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
                            val p = e.progress ?: if (e.watched) 1f else null
                            if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(3.dp).background(C06.Ember))
                        }
                    }
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            C06Label(tr("Season ${ep.season} · Episode ${ep.number}", "الموسم ${ep.season} · الحلقة ${ep.number}"))
                            Txt(ep.title.get(), type.title, maxLines = 1)
                            Txt(ep.synopsis.get(), type.body, maxLines = 2)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Txt("${ep.runtimeMin}:00", type.readoutBig)
                            Txt(tr("4K · DV · ATMOS", "4K · DV · أتموس"), type.caption)
                        }
                    }
                }
            }
        }
    }
}
