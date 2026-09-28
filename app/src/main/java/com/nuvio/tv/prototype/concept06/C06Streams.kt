package com.nuvio.tv.prototype.concept06

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
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.data.fmt
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

private const val NETWORK_MBPS = 480.0

private fun StreamMode.icon(): Glyph = when (this) {
    StreamMode.BEST -> Glyph.SPARKLE
    StreamMode.MAX_QUALITY -> Glyph.GEM
    StreamMode.FASTEST -> Glyph.BOLT
    StreamMode.BALANCED -> Glyph.BALANCE
    StreamMode.SMALLER -> Glyph.FEATHER
    StreamMode.ALL -> Glyph.LIST
}

@Composable
internal fun C06Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c06Type()
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val ranked = remember(mode) { StreamIntelligence.rank(streams, mode) }
    val best = ranked.first()
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C06Screen(session, if (t.isSeries) 2 else 1, t.title.get() + " · " + tr("Sources", "المصادر")) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
            val modeFocus = rememberTabRowFocus()
            C06Module(Modifier.width(176.dp).fillMaxHeight().tabRow(modeFocus), focusable = false, padding = 8.dp) {
                C06Label(tr("Mode", "الوضع"), Modifier.padding(6.dp))
                StreamMode.entries.forEach { m ->
                    var focused by remember { mutableStateOf(false) }
                    val f = detent(focused)
                    Row(
                        Modifier.fillMaxWidth().tabItem(modeFocus, m == mode).module(if (m == mode) maxOf(f, 0.4f) else f, 6.dp)
                            .protoFocusable(onFocusChange = { focused = it; if (it) mode = m }, onClick = { mode = m })
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProtoIcon(m.icon(), size = 13.dp, color = if (m == mode) C06.Ember else C06.Ink3, stroke = 1.5.dp)
                        Spacer(Modifier.width(8.dp))
                        Txt(m.label.get(), type.value.copy(color = if (m == mode || focused) C06.Ink else C06.Ink2), maxLines = 1)
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
                C06Module(Modifier.fillMaxWidth(), focusable = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            C06Label(mode.label.get() + " · " + tr("${best.score}% match", "تطابق ${best.score}%"))
                            Txt(StreamIntelligence.headline(best.stream), type.display.copy(fontSize = type.display.fontSize * 0.62f), maxLines = 1)
                            Txt(best.reasons.map { it.get() }.joinToString("  ·  "), type.caption, maxLines = 1)
                        }
                        Spacer(Modifier.width(14.dp))
                        // Headroom: what the stream needs against what the network can give.
                        Column(Modifier.width(190.dp)) {
                            C06Readout(tr("Needs", "يحتاج"), best.stream.bitrateLabel)
                            C06Readout(tr("Network", "الشبكة"), "${NETWORK_MBPS.toInt()} Mbps")
                            C06Bar((best.stream.bitrateMbps / NETWORK_MBPS).toFloat() * 3f, Modifier.fillMaxWidth().padding(vertical = 5.dp), mark = 1f / 3f)
                            C06Readout(tr("Headroom", "الهامش"), "${fmt(NETWORK_MBPS / best.stream.bitrateMbps, 1)}×")
                        }
                        Spacer(Modifier.width(14.dp))
                        C06Button(tr("Play best", "شغّل الأنسب"), Glyph.PLAY, primary = true, requester = play) { session.nav.push(ProtoRoute.Player(t.id)) }
                    }
                }
                C06Module(Modifier.fillMaxWidth().weight(1f), focusable = false, padding = 8.dp) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        C06Label(tr("Quality", "الجودة"), Modifier.width(62.dp))
                        C06Label("HDR", Modifier.width(92.dp))
                        C06Label(tr("Audio", "الصوت"), Modifier.weight(1f))
                        C06Label(tr("Size", "الحجم"), Modifier.width(58.dp))
                        C06Label(tr("Rate", "المعدل"), Modifier.width(62.dp))
                        C06Label(tr("Source", "المصدر"), Modifier.width(84.dp))
                        C06Label(tr("Start", "البدء"), Modifier.width(48.dp))
                        C06Label("", Modifier.width(10.dp))
                    }
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ranked.forEach { r -> C06StreamRow(r, raw = mode == StreamMode.ALL) { session.nav.push(ProtoRoute.Player(t.id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun C06StreamRow(r: RankedStream, raw: Boolean, onClick: () -> Unit) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    val s = r.stream
    val ink = if (focused) C06.Ink else C06.Ink2
    Column(Modifier.fillMaxWidth().module(f, 6.dp).protoFocusable(onFocusChange = { focused = it }, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(if (s.resolution.label == "2160p") "4K" else s.resolution.label, type.value.copy(color = ink), Modifier.width(62.dp))
            Txt(s.hdr.first().label, type.value.copy(color = ink), Modifier.width(92.dp), maxLines = 1)
            Txt(s.audioLabel, type.value.copy(color = ink), Modifier.weight(1f), maxLines = 1)
            Txt(s.sizeLabel, type.value.copy(color = ink), Modifier.width(58.dp))
            Txt(s.bitrateLabel, type.value.copy(color = ink), Modifier.width(62.dp))
            Txt(s.source.label, type.caption, Modifier.width(84.dp), maxLines = 1)
            Txt(s.startLabel, type.caption, Modifier.width(48.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (s.isCached) C06.Ok else C06.Ink3))
        }
        if (raw) Txt(s.filename, type.caption.copy(fontFamily = LocalProtoFonts.current.plexMono, color = C06.Ink3), maxLines = 1)
    }
    Spacer(Modifier.height(3.dp))
}
