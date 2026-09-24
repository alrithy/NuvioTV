package com.nuvio.tv.prototype.concept02

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
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
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C02Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c02Type()
    val play = remember { FocusRequester() }
    val lang = com.nuvio.tv.prototype.shared.LocalProtoLang.current
    RequestFocusOnce(play)
    C02Screen {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(startScrim(0f to C02.Night, 0.5f to C02.Night.copy(alpha = 0.82f), 1f to C02.Night.copy(alpha = 0.1f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to C02.Night)))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = C02.Margin),
        ) {
            Spacer(Modifier.height(54.dp))
            C02Heading(if (t.isSeries) Bi("Series", "مسلسل") else Bi("Feature film", "فيلم روائي"))
            Spacer(Modifier.height(12.dp))
            Txt(t.title.get(), type.hero.copy(fontSize = type.hero.fontSize * 1.1f), Modifier.width(560.dp), maxLines = 2)
            Txt(t.title.other(), type.heroSecond, maxLines = 1)
            Spacer(Modifier.height(12.dp))
            Txt(metaLine(t) + "  ·  " + t.country.get(), type.meta)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                t.tech.labels().forEach { C02Badge(it.uppercase()) }
                Spacer(Modifier.width(6.dp))
                C02Pill("IMDb ${t.rating}")
            }
            Spacer(Modifier.height(16.dp))
            Txt(t.synopsis.get(), type.body.copy(color = C02.Ink), Modifier.width(500.dp), maxLines = 4)
            t.award?.let {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Crenellation(Modifier.width(22.dp), C02.Gold, 5.dp)
                    Spacer(Modifier.width(8.dp))
                    Txt(it.get(), type.meta.copy(color = C02.GoldLight))
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val label = when {
                    t.isSeries && t.resume != null -> tr("Resume · S${t.resume!!.season} E${t.resume!!.number}", "استئناف · م${t.resume!!.season} ح${t.resume!!.number}")
                    t.progress != null -> tr("Resume · ${t.remainingMin}m left", "استئناف · متبقٍ ${t.remainingMin} د")
                    else -> tr("Play", "تشغيل")
                }
                C02GoldButton(label, Glyph.PLAY, play) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C02GhostButton(tr("Episodes", "الحلقات"), Glyph.EPISODES) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                C02GhostButton(tr("Choose source", "اختر المصدر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                C02GhostButton(tr("Trailer", "الإعلان"), Glyph.TRAILER) {}
                C02GhostButton(tr("My list", "قائمتي"), Glyph.PLUS) {}
            }
            Spacer(Modifier.height(44.dp))
            C02Divider(Modifier.width(860.dp))
            Spacer(Modifier.height(18.dp))
            Row {
                Column(Modifier.width(520.dp)) {
                    C02Heading(Bi("Cast", "طاقم التمثيل"))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        t.cast.forEach { c -> C02CastChip(c.name, c.role) }
                    }
                }
                Spacer(Modifier.width(40.dp))
                Column(Modifier.width(300.dp)) {
                    C02Heading(Bi("At a glance", "لمحة"))
                    Spacer(Modifier.height(12.dp))
                    C02Fact(tr("Director", "الإخراج"), t.director.get())
                    C02Fact(tr("Country", "البلد"), t.country.get())
                    C02Fact(tr("Genre", "النوع"), t.genres.joinToString(" · ") { it.of(lang) })
                    C02Fact(tr("Picture", "الصورة"), t.tech.labels().take(2).joinToString(" · "))
                }
            }
            Spacer(Modifier.height(28.dp))
            C02Heading(Bi("More like this", "أعمال مشابهة"))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MockCatalog.becauseDune.filter { it.id != t.id }.forEach { m ->
                    C02Landscape(m, 178.dp, null, null, null, {}) { session.nav.push(ProtoRoute.Details(m.id)) }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun C02Fact(k: String, v: String) {
    val type = c02Type()
    Row(Modifier.padding(vertical = 3.dp)) {
        Txt(k, type.meta.copy(color = C02.Ink3), Modifier.width(92.dp))
        Txt(v, type.meta.copy(color = C02.Ink), maxLines = 1)
    }
}

@Composable
private fun C02CastChip(name: Bi, role: Bi) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    val f = c02FocusAnim(focused)
    Column(Modifier.width(92.dp).protoFocusable(onFocusChange = { focused = it }, onClick = {}), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .c02Focus(f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(C02.Raised, C02.Surface)))
                .border(1.dp, if (focused) C02.GoldLight else C02.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val initials = name.get().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }
            Txt(initials, type.title.copy(color = C02.GoldLight))
        }
        Spacer(Modifier.height(8.dp))
        Txt(name.get(), type.caption.copy(color = if (focused) C02.Ink else C02.Ink2), maxLines = 1)
        Txt(role.get(), type.captionSecond, maxLines = 1)
    }
}

@Composable
internal fun C02Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c02Type()
    val ar = isArabic()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]

    C02Screen {
        RiyadhSkyline(Modifier.fillMaxSize(), intensity = 0.6f)
        Row(Modifier.fillMaxSize().padding(horizontal = C02.Margin, vertical = 36.dp)) {
            Column(Modifier.width(400.dp).fillMaxHeight()) {
                Txt(t.title.get(), type.title.copy(fontSize = type.title.fontSize * 1.3f))
                Txt(t.title.other(), type.eyebrowSecond)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    t.seasons.forEachIndexed { i, s ->
                        C02GhostButton(tr("Season ${s.number}", "الموسم ${s.number}"), selected = i == seasonIdx, onFocus = { seasonIdx = i }) { seasonIdx = i }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.verticalScroll(session.scroll("c02.eps.$seasonIdx"))) {
                    season.episodes.forEachIndexed { i, e ->
                        var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(C02CardShape)
                                .background(if (focused) C02.Glass else Color.Transparent)
                                .border(1.dp, if (focused) C02.Gold else Color.Transparent, C02CardShape)
                                .protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) })
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Txt(e.number.toString().padStart(2, '0'), type.title.copy(color = if (focused) C02.GoldLight else C02.Ink3), Modifier.width(34.dp))
                            Box(Modifier.width(88.dp).aspectRatio(16f / 9f).clip(C02CardShape)) {
                                ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
                                val p = e.progress ?: if (e.watched) 1f else null
                                if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(2.dp).background(C02.Gold))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Txt(e.title.get(), type.label.copy(color = if (focused) C02.Ink else C02.Ink2), maxLines = 1)
                                Txt(
                                    tr("${e.runtimeMin} min", "${e.runtimeMin} دقيقة") + when {
                                        e.isNew -> tr(" · New", " · جديدة")
                                        e.watched -> tr(" · Watched", " · شوهدت")
                                        e.progress != null -> tr(" · In progress", " · قيد المشاهدة")
                                        else -> ""
                                    },
                                    type.captionSecond.copy(color = if (e.isNew) C02.EmeraldLight else C02.Ink3),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(120.dp))
                }
            }
            Spacer(Modifier.width(28.dp))
            AnimatedContent(ep, transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(160)) }, label = "ep", modifier = Modifier.weight(1f)) { e ->
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(C02CardShape).border(1.dp, C02.Line, C02CardShape)) {
                        ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xCC070A12))))
                        Row(Modifier.align(Alignment.BottomStart).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            C02Badge("4K"); C02Badge("DOLBY VISION"); C02Badge("ATMOS")
                            if (e.isNew) C02Badge(if (ar) "جديدة" else "NEW", emerald = true)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    C02Heading(Bi("Season ${e.season} · Episode ${e.number}", "الموسم ${e.season} · الحلقة ${e.number}"))
                    Spacer(Modifier.height(8.dp))
                    Txt(e.title.get(), type.hero.copy(fontSize = type.hero.fontSize * 0.62f), maxLines = 1)
                    Txt(e.title.other(), type.heroSecond.copy(fontSize = type.heroSecond.fontSize * 0.72f), maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Txt(e.synopsis.get(), type.body, maxLines = 3)
                }
            }
        }
    }
}
