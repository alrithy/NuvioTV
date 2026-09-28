package com.nuvio.tv.prototype.concept01

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/**
 * "Presentation": the stream picker speaks in cinema terms. One recommendation, stated like a
 * projection-booth card; alternatives are quiet lines; raw filenames only under All Sources.
 */
@Composable
internal fun C01Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c01Type()
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val ranked = remember(mode) { StreamIntelligence.rank(streams, mode) }
    val best = ranked.first()
    val play = remember { FocusRequester() }
    val lang = LocalProtoLang.current
    RequestFocusOnce(play)

    Box(Modifier.fillMaxSize().background(C01.Black)) {
        Box(Modifier.fillMaxSize().padding(vertical = C01.Bar)) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f })
            Box(Modifier.fillMaxSize().background(startScrim(0f to C01.Black, 0.6f to C01.Black.copy(alpha = 0.6f), 1f to C01.Black.copy(alpha = 0.2f))))
            Row(Modifier.fillMaxSize().padding(horizontal = C01.Margin, vertical = 28.dp)) {
                Column(Modifier.weight(1f)) {
                    TitleSequenceText(mode.label.get().cap(), type.overline.copy(color = C01.Ink), key = "o$mode", maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    TitleSequenceText(mode.blurb.get().cap(), type.small, key = "b$mode", maxLines = 1)
                    Spacer(Modifier.height(20.dp))
                    val headline = StreamIntelligence.headline(best.stream).split(" · ")
                    TitleSequenceText(headline.take(2).joinToString("   ").cap(), type.display.copy(fontSize = 34.sp, letterSpacing = if (type.arabic) 0.em else 0.12.em), key = "h$mode", delayMs = 80, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    TitleSequenceText(headline.drop(2).joinToString("   ").cap(), type.title, key = "a$mode", delayMs = 180, maxLines = 1)
                    Spacer(Modifier.height(16.dp))
                    val s = best.stream
                    TitleSequenceText(
                        listOf(s.source.label, s.sizeLabel, if (s.isCached) tr("${s.service} cached", "مخزّن على ${s.service}") else tr("Not cached", "غير مخزّن"), tr("Starts ${s.startLabel}", "يبدأ خلال ${s.startLabel}")).joinToString("   ·   ").cap(),
                        type.meta, key = "d$mode", delayMs = 260, maxLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    Txt(best.reasons.drop(1).joinToString("   ·   ") { it.of(lang) }.cap(), type.small.copy(color = C01.Ink70), maxLines = 1)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                        C01Action(tr("Play", "تشغيل").cap(), glyph = Glyph.PLAY, requester = play, style = type.label) {
                            session.nav.push(ProtoRoute.Player(t.id))
                        }
                        Txt(tr("${best.score}% MATCH", "تطابق ${best.score}%"), type.small)
                    }
                }
                Spacer(Modifier.width(36.dp))
                // Alternatives: numbered lines, not cards.
                Column(Modifier.width(330.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Txt(tr("ALSO AVAILABLE", "متاح أيضاً"), type.small)
                    Spacer(Modifier.height(12.dp))
                    ranked.drop(1).take(if (mode == StreamMode.ALL) 10 else 5).forEachIndexed { i, r ->
                        C01SourceLine(i + 2, r, raw = mode == StreamMode.ALL) { session.nav.push(ProtoRoute.Player(t.id)) }
                    }
                }
            }
        }
        C01Bar(C01.Bar, Modifier.align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt(t.title.get().cap(), type.label.copy(color = C01.Ink70))
                Spacer(Modifier.weight(1f))
                Txt(tr("PRESENTATION", "العرض"), type.small)
            }
        }
        C01Bar(C01.Bar, Modifier.align(Alignment.BottomCenter)) {
            val modeFocus = rememberTabRowFocus()
            Row(Modifier.tabRow(modeFocus), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                StreamMode.entries.forEach { m ->
                    C01Action(
                        m.label.get().cap(),
                        modifier = Modifier.tabItem(modeFocus, m == mode),
                        style = type.small.copy(color = if (m == mode) C01.Ink else C01.Ink45),
                        onFocus = { mode = m },
                    ) { mode = m }
                }
            }
        }
    }
}

@Composable
private fun C01SourceLine(index: Int, r: RankedStream, raw: Boolean, onClick: () -> Unit) {
    val type = c01Type()
    var focused by remember { mutableStateOf(false) }
    val light by animateFloatAsState(if (focused) 1f else 0f, tween(C01.FOCUS), label = "l")
    val s = r.stream
    Row(
        Modifier
            .fillMaxWidth()
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Txt(index.toString().padStart(2, '0'), type.small.copy(color = C01.Ink.copy(alpha = 0.3f + 0.7f * light)), Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Txt(StreamIntelligence.headline(s).cap(), type.small.copy(color = C01.Ink.copy(alpha = 0.55f + 0.45f * light)), maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Txt(
                if (raw) s.filename else "${s.source.label} · ${s.sizeLabel} · ${s.addon}",
                type.small.copy(color = C01.Ink45, letterSpacing = type.small.letterSpacing * (if (raw) 0f else 1f)),
                maxLines = 1,
            )
        }
        Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(if (s.isCached) C01.Ink.copy(alpha = 0.4f + 0.6f * light) else C01.Ink25))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(C01.Hair))
}
