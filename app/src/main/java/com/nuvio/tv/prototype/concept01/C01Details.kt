package com.nuvio.tv.prototype.concept01

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/** Opens the letterbox: bars retract from 69dp to nothing as the scene "goes full frame". */
@Composable
internal fun rememberOpeningBars(): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val bars = remember { Animatable(1f) }
    LaunchedEffect(Unit) { bars.animateTo(0f, tween(900, delayMillis = 150, easing = ProtoEasing.Cinematic)) }
    return bars
}

@Composable
internal fun C01Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c01Type()
    val bars = rememberOpeningBars()
    val primary = remember { FocusRequester() }
    RequestFocusOnce(primary)
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize().background(C01.Black)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().filmGrain(0.05f))
        Box(Modifier.fillMaxSize().background(startScrim(0f to C01.Black.copy(alpha = 0.92f), 0.5f to C01.Black.copy(alpha = 0.55f), 0.85f to C01.Black.copy(alpha = 0.1f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to C01.Black.copy(alpha = 0f), 1f to C01.Black)))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = C01.Margin),
        ) {
            Spacer(Modifier.height(118.dp))
            val credit = if (t.isSeries) t.director.get() else tr("A film by ", "فيلم من إخراج ") + t.director.get()
            TitleSequenceText(credit.cap(), type.overline, key = "credit")
            Spacer(Modifier.height(14.dp))
            TitleSequenceText(t.title.get().cap(), type.display.copy(fontSize = type.display.fontSize * 1.12f), key = "title", delayMs = 120, modifier = Modifier.width(640.dp))
            Spacer(Modifier.height(14.dp))
            TitleSequenceText(metaLine(t) + "  ·  IMDb ${t.rating}", type.meta, key = "meta", delayMs = 260, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            // The billing block: technical capability set like the small print on a one-sheet.
            TitleSequenceText(t.tech.labels().joinToString("   |   ").cap(), type.small.copy(color = C01.Ink70), key = "tech", delayMs = 380, maxLines = 1)
            Spacer(Modifier.height(18.dp))
            Txt(t.synopsis.get(), type.body, Modifier.width(480.dp), maxLines = 4)
            Spacer(Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                val resume = t.progress != null
                val playLabel = when {
                    t.isSeries && t.resume != null -> tr("Resume S${t.resume!!.season} E${t.resume!!.number}", "استئناف الحلقة ${t.resume!!.number}")
                    resume -> tr("Resume · ${t.remainingMin} min left", "استئناف · متبقٍ ${t.remainingMin} د")
                    else -> tr("Play", "تشغيل")
                }
                C01Action(playLabel.cap(), glyph = Glyph.PLAY, requester = primary, style = type.label) {
                    session.nav.push(ProtoRoute.Streams(t.id))
                }
                if (t.isSeries) {
                    C01Action(tr("Episodes", "الحلقات").cap(), style = type.label) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                } else if (resume) {
                    C01Action(tr("From the beginning", "من البداية").cap(), style = type.label) { session.nav.push(ProtoRoute.Streams(t.id)) }
                }
                C01Action(tr("Presentation", "العرض").cap(), style = type.label) { session.nav.push(ProtoRoute.Streams(t.id)) }
                C01Action(tr("Trailer", "الإعلان").cap(), style = type.label) {}
                C01Action(tr("+ Watchlist", "+ قائمتي").cap(), style = type.label) {}
            }

            Spacer(Modifier.height(96.dp))
            // End credits.
            Txt(tr("CAST", "طاقم التمثيل"), type.small, Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            t.cast.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                    Txt(c.role.get().cap(), type.small.copy(color = C01.Ink45), Modifier.width(260.dp), align = TextAlign.End, maxLines = 1)
                    Spacer(Modifier.width(28.dp))
                    Txt(c.name.get().cap(), type.label, Modifier.width(260.dp), maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Txt((if (t.isSeries) "" else tr("Directed by  ", "إخراج  ")) + t.director.get(), type.small.copy(color = C01.Ink70))
            }
            Spacer(Modifier.height(56.dp))
            Txt(tr("MORE LIKE THIS", "أعمال مشابهة"), type.small)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MockCatalog.becauseDune.filter { it.id != t.id }.forEach { m ->
                    C01Frame(m, width = 190.dp, caption = m.title.get().cap(), captionStyle = type.small.copy(color = C01.Ink)) {
                        session.nav.push(ProtoRoute.Details(m.id))
                    }
                }
                Spacer(Modifier.width(400.dp))
            }
            Spacer(Modifier.height(60.dp))
        }

        // Letterbox bars retracting.
        Box(Modifier.fillMaxWidth().height(C01.Bar).graphicsLayer { scaleY = bars.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }.background(C01.Black).align(Alignment.TopCenter))
        Box(Modifier.fillMaxWidth().height(C01.Bar).graphicsLayer { scaleY = bars.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }.background(C01.Black).align(Alignment.BottomCenter))
    }
}

@Composable
internal fun C01Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c01Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val startEp = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(startEp) }
    val requesters = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(requesters[startEp.coerceIn(0, requesters.lastIndex)], key = seasonIdx)
    val ar = isArabic()

    Box(Modifier.fillMaxSize().background(C01.Black)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.32f })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to C01.Black.copy(alpha = 0.6f), 0.5f to C01.Black.copy(alpha = 0.2f), 1f to C01.Black)))
        Column(Modifier.fillMaxSize().padding(top = 56.dp)) {
            Column(Modifier.padding(horizontal = C01.Margin)) {
                Txt(t.title.get().cap(), type.title)
                Spacer(Modifier.height(18.dp))
                val seasonFocus = rememberTabRowFocus()
                Row(Modifier.tabRow(seasonFocus), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    t.seasons.forEachIndexed { i, s ->
                        C01Action(
                            tr("Season ${s.number}", "الموسم ${s.number}").cap(),
                            modifier = Modifier.tabItem(seasonFocus, i == seasonIdx),
                            style = type.label.copy(color = if (i == seasonIdx) C01.Ink else C01.Ink45),
                            onFocus = { seasonIdx = i },
                        ) { seasonIdx = i }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Row(
                Modifier.horizontalScroll(session.scroll("c01.eps.$seasonIdx")).padding(horizontal = C01.Margin),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                season.episodes.forEachIndexed { i, e ->
                    Column(Modifier.width(216.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Txt(e.number.toString().padStart(2, '0'), type.numeral.copy(color = if (i == focusedEp) C01.Ink else C01.Ink25))
                            Spacer(Modifier.width(10.dp))
                            if (e.watched) Txt(tr("SEEN", "شوهدت"), type.small, Modifier.padding(bottom = 6.dp))
                            if (e.isNew) Txt(tr("NEW", "جديدة"), type.small.copy(color = C01.Ink), Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        C01Frame(
                            title = t,
                            width = 216.dp,
                            requester = requesters[i],
                            progress = e.progress ?: if (e.watched) 1f else null,
                            variant = e.number * 7 + e.season,
                            onFocus = { focusedEp = i },
                            onClick = { session.nav.push(ProtoRoute.Streams(t.id)) },
                            caption = e.title.get().cap(),
                            captionStyle = type.small.copy(color = C01.Ink),
                        )
                    }
                }
                Spacer(Modifier.width(500.dp))
            }
            Spacer(Modifier.height(28.dp))
            val e = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]
            AnimatedContent(e, transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(160)) }, label = "ep", modifier = Modifier.padding(horizontal = C01.Margin)) { ep ->
                Column(Modifier.width(560.dp)) {
                    Txt(ep.title.get().let { if (ar) it else it.uppercase() }, type.title.copy(fontSize = type.title.fontSize * 0.8f))
                    Spacer(Modifier.height(8.dp))
                    Txt(
                        (tr("Season ${ep.season} · Episode ${ep.number} · ${ep.runtimeMin} min", "الموسم ${ep.season} · الحلقة ${ep.number} · ${ep.runtimeMin} دقيقة")).cap(),
                        type.meta,
                    )
                    Spacer(Modifier.height(10.dp))
                    Txt(ep.synopsis.get(), type.body, maxLines = 2)
                }
            }
        }
    }
}
