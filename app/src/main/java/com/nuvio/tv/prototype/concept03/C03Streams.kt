package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
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
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C03Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c03Type()
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C03Screen {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.12f })
        Row(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 44.dp)) {
            Column(Modifier.width(206.dp).fillMaxHeight()) {
                Txt(t.title.get().up(), type.label.copy(color = C03.Sand), maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Txt(tr("CHOOSE A PRESENTATION", "اختر طريقة العرض"), type.small)
                Spacer(Modifier.height(22.dp))
                StreamMode.entries.forEachIndexed { i, m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(numeral(i + 1), type.label.copy(color = if (m == mode) C03.BronzeLight else C03.SandFaint), Modifier.width(32.dp))
                        C03ListItem(m.label.get().up(), style = type.label, maxScale = 1.12f, selected = m == mode, onFocus = { mode = m }) { mode = m }
                    }
                }
            }
            C03VRule(Modifier.fillMaxHeight())
            Spacer(Modifier.width(30.dp))
            AnimatedContent(mode, transitionSpec = { fadeIn(tween(C03.SLOW, 80, ProtoEasing.Cinematic)) togetherWith fadeOut(tween(160)) }, label = "m", modifier = Modifier.weight(1f)) { m ->
                val ranked = remember(m) { StreamIntelligence.rank(streams, m) }
                if (m == StreamMode.ALL) C03Ledger(ranked, raw = true) { session.nav.push(ProtoRoute.Player(t.id)) }
                else Row {
                    val best = ranked.first()
                    val s = best.stream
                    Column(Modifier.width(300.dp)) {
                        Txt(m.blurb.get().up(), type.small.copy(color = C03.Bronze), maxLines = 2)
                        Spacer(Modifier.height(10.dp))
                        Txt(s.resolution.label.uppercase().digits(), type.numeral)
                        Txt(s.hdr.first().label.up(), type.heading, maxLines = 1)
                        Txt(s.audioLabel.up(), type.heading.copy(color = C03.SandDim), maxLines = 1)
                        Spacer(Modifier.height(14.dp))
                        C03Rule(Modifier.fillMaxWidth())
                        C03Spec(tr("Source", "المصدر"), "${s.source.label} · ${best.tier.get()}")
                        C03Spec(tr("Size", "الحجم"), "${s.sizeLabel} · ${s.bitrateLabel}")
                        C03Spec(tr("State", "الحالة"), if (s.isCached) tr("${s.service} · cached", "${s.service} · مخزّن") else tr("Not cached", "غير مخزّن"))
                        C03Spec(tr("Start", "البدء"), s.startLabel)
                        C03Spec(tr("Subtitles", "الترجمة"), s.subtitleLangs.joinToString(" · "))
                        Spacer(Modifier.height(16.dp))
                        C03SlabButton(tr("PLAY  ·  ${best.score}% MATCH", "تشغيل · تطابق ${best.score}%"), Modifier.fillMaxWidth(), play, primary = true) { session.nav.push(ProtoRoute.Player(t.id)) }
                    }
                    Spacer(Modifier.width(30.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(tr("ALSO IN THE VAULT", "في الخزانة أيضاً"), type.label.copy(color = C03.Bronze))
                        Spacer(Modifier.height(8.dp))
                        C03Ledger(ranked.drop(1).take(6), raw = false) { session.nav.push(ProtoRoute.Player(t.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun C03Ledger(rows: List<RankedStream>, raw: Boolean, onClick: () -> Unit) {
    val type = c03Type()
    val mono = type.small.copy(fontFamily = LocalProtoFonts.current.plexMono, letterSpacing = type.small.letterSpacing * 0f)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        rows.forEach { r ->
            var focused by remember { mutableStateOf(false) }
            val s = r.stream
            val ink = if (focused) C03.Shadow else C03.Sand
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (focused) C03.Sand else Color.Transparent)
                    .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (s.resolution.label == "2160p") "4K" else s.resolution.label.uppercase(), type.label.copy(color = ink), Modifier.width(56.dp))
                    Txt(s.hdr.first().label.let { if (it == "Dolby Vision") "DV" else it }.uppercase(), type.label.copy(color = ink), Modifier.width(76.dp))
                    Txt(s.audio.short.uppercase() + " " + s.channels, type.label.copy(color = ink), Modifier.weight(1f), maxLines = 1)
                    Txt(s.sizeLabel, type.label.copy(color = ink), Modifier.width(62.dp))
                    Box(Modifier.width(8.dp).height(8.dp).background(if (s.isCached) (if (focused) C03.Shadow else C03.BronzeLight) else C03.SandFaint))
                }
                if (raw) {
                    Spacer(Modifier.height(3.dp))
                    Txt(s.filename, mono.copy(color = if (focused) C03.Shadow.copy(alpha = 0.7f) else C03.SandFaint), maxLines = 1)
                }
            }
            C03Rule(Modifier.fillMaxWidth(), C03.Bronze.copy(alpha = 0.2f))
        }
    }
}
