package com.nuvio.tv.prototype.concept09

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.Episode
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.toArabicDigits
import com.nuvio.tv.prototype.shared.tr

/** The other script, ghosted: the Arabic title under an English one and vice versa. */
@Composable
internal fun C09OtherTitle(t: ProtoTitle, size: Float = 20f) {
    val ar = isArabic()
    val f = LocalProtoFonts.current
    val style = c09Type().ghost.copy(fontSize = size.sp, lineHeight = (size * 1.4f).sp)
    Txt(if (ar) t.title.en else t.title.ar, if (ar) style.copy(fontFamily = f.sora, fontSize = (size * 0.7f).sp) else style, maxLines = 1)
}

@Composable
internal fun C09Chip(text: String, color: Color = C09.Copper) {
    Box(Modifier.border(1.dp, color.copy(alpha = 0.7f), chamfer(5.dp)).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Txt(text, c09Type().tag.copy(color = color), maxLines = 1)
    }
}

@Composable
internal fun C09Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c09Type()
    val ar = isArabic()
    val lang = LocalProtoLang.current
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    val best = remember(t.id) { StreamIntelligence.best(StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode)) }

    C09Stage {
        C09ArtWindow(t, Modifier.align(Alignment.CenterEnd).padding(end = C09.Margin, top = 34.dp, bottom = 34.dp).width(500.dp).fillMaxHeight(), cut = 64.dp)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 56.dp).width(450.dp)) {
            Txt(tag(if (t.isSeries) "Series" else "Film", if (t.isSeries) "مسلسل" else "فيلم") + "  ·  " + t.country.get().let { if (ar) it else it.uppercase() }, type.tag.copy(color = C09.Copper))
            Spacer(Modifier.height(6.dp))
            Txt(t.title.get(), type.display.copy(fontSize = 46.sp, lineHeight = if (ar) 66.sp else 52.sp), maxLines = 2)
            C09OtherTitle(t)
            Spacer(Modifier.height(10.dp))
            Txt(c09Meta(t), type.body, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { t.tech.labels().take(4).forEach { C09Chip(if (ar) it else it.uppercase()) } }
            Spacer(Modifier.height(14.dp))
            Txt(t.synopsis.get(), type.body, maxLines = 4)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(chamfer(1.5.dp)).background(C09.Teal))
                Spacer(Modifier.width(8.dp))
                Txt(tr("Best tonight: ", "الأفضل الليلة: ") + StreamIntelligence.headline(best.stream) + tr(" · ready now", " · جاهز الآن"), type.body.copy(color = C09.Pearl), maxLines = 1)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C09Button(if (t.progress != null) tr("Resume", "استئناف") else tr("Play", "شاهد"), Glyph.PLAY, play, primary = true) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C09Button(tr("Episodes", "الحلقات"), Glyph.EPISODES) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                C09Button(tr("Sources", "المصادر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                C09Button("", Glyph.PLUS) {}
            }
            Spacer(Modifier.height(18.dp))
            Txt(tag("Director", "إخراج") + "   " + t.director.get() + "     " + tag("With", "بطولة") + "   " + t.cast.take(2).joinToString(if (ar) "، " else ", ") { it.name.of(lang) }, type.tag.copy(color = C09.Pearl2), maxLines = 1)
        }
    }
}

private val ordinalsAr = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس")
private val ordinalsEn = listOf("One", "Two", "Three", "Four", "Five", "Six")

private val EpW = 232.dp
private val EpH = 130.dp

@Composable
internal fun C09Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c09Type()
    val ar = isArabic()
    val rtl = LocalProtoLang.current.isRtl
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusIdx by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val scroll = session.scroll("c09.eps.$seasonIdx")
    val density = LocalDensity.current
    // Open with the resume episode at the rail's pivot, the same place focus rests while moving.
    LaunchedEffect(seasonIdx) {
        withFrameNanos { }
        scroll.scrollTo(with(density) { (start * (EpW + 8.dp).toPx() - 110.dp.toPx()).toInt().coerceAtLeast(0) })
    }
    val ep = season.episodes[focusIdx.coerceIn(0, season.episodes.lastIndex)]

    C09Stage {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(230.dp).fillMaxHeight().padding(start = 44.dp, top = 40.dp)) {
                Txt(t.title.get(), type.title, maxLines = 1)
                C09OtherTitle(t, 15f)
                Spacer(Modifier.height(30.dp))
                t.seasons.forEachIndexed { i, s ->
                    C09SeasonItem(
                        if (ar) "الموسم ${ordinalsAr.getOrElse(s.number - 1) { s.number.toString() }}" else "Season ${ordinalsEn.getOrElse(s.number - 1) { s.number.toString() }}",
                        if (ar) "${s.episodes.size} حلقات · ${s.year}".toArabicDigits() else "${s.episodes.size} episodes · ${s.year}",
                        selected = i == seasonIdx,
                    ) { seasonIdx = i }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(ep, transitionSpec = { fadeIn(tween(340, 60)) togetherWith fadeOut(tween(160)) }, label = "ep", modifier = Modifier.padding(top = 40.dp, end = C09.Margin)) { e ->
                    Column(Modifier.width(520.dp)) {
                        Txt(tag("Episode ${e.number}", "الحلقة ${num(e.number)}") + "  ·  " + (if (ar) "${e.runtimeMin} دقيقة".toArabicDigits() else "${e.runtimeMin} min"), type.tag.copy(color = C09.Copper))
                        Spacer(Modifier.height(4.dp))
                        Txt(e.title.get(), type.display, maxLines = 1)
                        Txt(if (ar) e.title.en else e.title.ar, type.ghost, maxLines = 1)
                        Spacer(Modifier.height(8.dp))
                        Txt(e.synopsis.get(), type.body, maxLines = 3)
                        Spacer(Modifier.height(10.dp))
                        val status = when {
                            e.progress != null -> tr("${((1f - e.progress) * e.runtimeMin).toInt()} min left", "متبقٍ ${((1f - e.progress) * e.runtimeMin).toInt()} دقيقة".toArabicDigits())
                            e.watched -> tr("Watched", "شوهدت")
                            e.isNew -> tr("New this week", "جديدة هذا الأسبوع")
                            else -> tr("Not started", "لم تبدأ")
                        }
                        Txt(status, type.body.copy(color = if (e.isNew) C09.Teal else C09.Pearl), maxLines = 1)
                    }
                }
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 34.dp).drawBehind {
                        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, C09.Copper.copy(alpha = 0.6f), Color.Transparent)), Offset(0f, EpH.toPx()), Offset(size.width, EpH.toPx()), 1.dp.toPx())
                    },
                ) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(end = 400.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        season.episodes.forEachIndexed { i, e ->
                            C09EpisodeCard(t, e, i - focusIdx, rtl, reqs[i], onFocus = { focusIdx = i }) { session.nav.push(ProtoRoute.Streams(t.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C09SeasonItem(label: String, detail: String, selected: Boolean, onFocus: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(chamfer(8.dp))
            .background(if (focused) C09.Obsidian3 else Color.Transparent)
            .c09Trace({ trace.value }, 8.dp, base = Color.Transparent, bloom = false)
            .protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onFocus)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(26.dp).background(if (selected) C09.Copper else C09.Line))
        Spacer(Modifier.width(12.dp))
        Column {
            Txt(label, type.label.copy(color = if (selected || focused) C09.Pearl else C09.Pearl2, fontFamily = type.title.fontFamily), maxLines = 1)
            Txt(detail, type.tag, maxLines = 1)
        }
    }
}

@Composable
private fun C09EpisodeCard(t: ProtoTitle, e: Episode, distance: Int, rtl: Boolean, requester: FocusRequester, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    val d by animateFloatAsState(distance.toFloat(), tween(380, easing = ProtoEasing.Decelerate), label = "curve")
    Column(Modifier.protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).curve(d, rtl, pivotY = 1f / 1.3f)) {
        Box(Modifier.width(EpW).height(EpH).clip(chamfer()).c09Trace({ trace.value })) {
            ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color(0xD9000000))))
            Txt(num(e.number), type.numeral, Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 2.dp))
            if (e.watched) Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { ProtoIcon(Glyph.CHECK, size = 12.dp, color = C09.Pearl2, stroke = 1.6.dp) }
            if (e.isNew) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(7.dp).clip(chamfer(2.dp)).background(C09.Teal))
            val p = e.progress ?: if (e.watched) 1f else null
            if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(2.dp).background(if (e.watched) C09.Pearl3 else C09.Copper))
        }
        C09Reflection(t, ArtKind.STILL, EpW, EpH, 0.3f, episode = e)
    }
}
