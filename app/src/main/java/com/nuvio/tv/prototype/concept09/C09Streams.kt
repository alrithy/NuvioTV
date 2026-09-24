package com.nuvio.tv.prototype.concept09

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoStream
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.data.fmt
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

@Composable
private fun startText(s: ProtoStream): String =
    if (!s.isCached) tr("Downloads first", "يُنزَّل أولاً")
    else if (s.startSeconds < 60) num(tr("~${fmt(s.startSeconds, 0)} s", "~${fmt(s.startSeconds, 0)} ث"))
    else num(tr("~${(s.startSeconds / 60).toInt()} min", "~${(s.startSeconds / 60).toInt()} د"))

/**
 * How to watch: modes are a vertical list on the reading side; the chosen stream is one large
 * chamfered plate with its match score as display numerals. All Sources becomes a raw table.
 */
@Composable
internal fun C09Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c09Type()
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C09Stage {
        Row(Modifier.fillMaxSize().padding(start = 44.dp, end = C09.Margin, top = 30.dp, bottom = 26.dp)) {
            Column(Modifier.width(236.dp).fillMaxHeight()) {
                Txt(tag("How to watch", "كيف تشاهد"), type.tag.copy(color = C09.Copper))
                Txt(t.title.get(), type.title, maxLines = 1)
                C09OtherTitle(t, 14f)
                Spacer(Modifier.height(20.dp))
                StreamMode.entries.forEach { m ->
                    C09ModeItem(m.label.get(), m.blurb.get(), m == mode) { mode = m }
                }
            }
            Spacer(Modifier.width(28.dp))
            AnimatedContent(mode, transitionSpec = { fadeIn(tween(320, 60)) togetherWith fadeOut(tween(140)) }, label = "mode", modifier = Modifier.weight(1f)) { m ->
                val ranked = remember(m) { StreamIntelligence.rank(streams, m) }
                if (m == StreamMode.ALL) C09SourceTable(ranked) { session.nav.push(ProtoRoute.Player(t.id)) }
                else C09Plate(ranked, play) { session.nav.push(ProtoRoute.Player(t.id)) }
            }
        }
    }
}

@Composable
private fun C09ModeItem(label: String, blurb: String, selected: Boolean, onFocus: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(chamfer(8.dp))
            .background(if (focused) C09.Obsidian3 else Color.Transparent)
            .c09Trace({ trace.value }, 8.dp, base = Color.Transparent, bloom = false)
            .protoFocusable(onFocusChange = { focused = it; if (it) onFocus() }, onClick = onFocus)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(28.dp).background(if (selected) C09.Copper else C09.Line))
        Spacer(Modifier.width(12.dp))
        Column {
            Txt(label, type.label.copy(fontFamily = type.title.fontFamily, color = if (selected || focused) C09.Pearl else C09.Pearl2), maxLines = 1)
            if (selected || focused) Txt(blurb, type.tag.copy(fontSize = if (isArabic()) 10.sp else 8.sp), maxLines = 1)
        }
    }
}

@Composable
private fun C09Plate(ranked: List<RankedStream>, play: FocusRequester, onPlay: () -> Unit) {
    val type = c09Type()
    val best = ranked.first()
    val s = best.stream
    var raw by remember { mutableStateOf(false) }
    Column {
        Column(Modifier.fillMaxWidth().clip(chamfer(26.dp)).background(C09.Obsidian2).c09Trace({ 0f }, 26.dp).padding(horizontal = 26.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Txt(tag(best.tier.en, best.tier.ar), type.tag.copy(color = C09.Copper))
                    Txt(StreamIntelligence.headline(s), type.display.copy(fontSize = 26.sp, lineHeight = if (isArabic()) 40.sp else 32.sp), maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    best.reasons.take(3).forEach { r ->
                        Row(Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(5.dp).clip(chamfer(1.dp)).background(C09.Teal))
                            Spacer(Modifier.width(8.dp))
                            Txt(r.get(), type.body.copy(color = C09.Pearl), maxLines = 1)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Txt(num("${best.score}") + tr("%", "٪"), type.numeral.copy(fontSize = 52.sp, lineHeight = 60.sp, color = C09.CopperHi))
                    Txt(tag("Match", "تطابق"), type.tag)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C09Spec(tag("Starts", "البدء"), startText(s), Modifier.weight(1f))
                C09Spec(tag("Size", "الحجم"), num(s.sizeLabel), Modifier.weight(1f))
                C09Spec(tag("Bitrate", "معدل البت"), num(s.bitrateLabel), Modifier.weight(1f))
                C09Spec(tag("Source", "المصدر"), s.source.label + " · " + s.service, Modifier.weight(1.4f))
            }
            if (raw) {
                Spacer(Modifier.height(10.dp))
                Txt(s.filename, type.tag.copy(fontFamily = LocalProtoFonts.current.plexMono, fontSize = 10.sp, letterSpacing = 0.sp, color = C09.Pearl2), maxLines = 2)
                Txt("${s.addon} · ${s.codec} · ${s.audioLabel}", type.tag.copy(fontFamily = LocalProtoFonts.current.plexMono, fontSize = 10.sp, letterSpacing = 0.sp), maxLines = 1)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C09Button(tr("Play", "شاهد"), Glyph.PLAY, play, primary = true, onClick = onPlay)
                C09Button(if (raw) tr("Hide raw", "إخفاء التفاصيل") else tr("Raw details", "التفاصيل الخام"), Glyph.LIST) { raw = !raw }
            }
        }
        Spacer(Modifier.height(14.dp))
        Txt(tag("Also good", "خيارات أخرى جيدة"), type.tag)
        Spacer(Modifier.height(6.dp))
        ranked.drop(1).take(3).forEach { r -> C09SourceRow(r, compact = true, onClick = onPlay) }
    }
}

@Composable
private fun C09Spec(key: String, value: String, modifier: Modifier) {
    val type = c09Type()
    Column(modifier.clip(chamfer(6.dp)).background(C09.Obsidian).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Txt(key, type.tag, maxLines = 1)
        Txt(value, type.label, maxLines = 1)
    }
}

@Composable
private fun C09SourceRow(r: RankedStream, compact: Boolean, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    val s = r.stream
    Row(
        Modifier.fillMaxWidth().padding(bottom = 5.dp).clip(chamfer(8.dp))
            .background(if (focused) C09.Obsidian3 else C09.Obsidian2)
            .c09Trace({ trace.value }, 8.dp, bloom = false)
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(chamfer(1.5.dp)).background(if (s.isCached) C09.Teal else C09.Pearl3))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Txt(StreamIntelligence.headline(s), type.label, maxLines = 1)
            if (!compact) Txt(s.filename, type.tag.copy(fontFamily = LocalProtoFonts.current.plexMono, fontSize = 9.sp, letterSpacing = 0.sp), maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        Txt(startText(s) + "  ·  " + num(s.sizeLabel) + "  ·  " + s.addon + (if (!compact) "  ·  " + num("${s.seeders ?: "—"}") else ""), type.tag.copy(color = C09.Pearl2), maxLines = 1)
    }
}

@Composable
private fun C09SourceTable(ranked: List<RankedStream>, onClick: () -> Unit) {
    val type = c09Type()
    Column {
        Row {
            Txt(tag("Every source", "كل المصادر") + "  ·  " + num(ranked.size), type.tag.copy(color = C09.Pearl2), Modifier.weight(1f))
            Txt(tag("Start · Size · Provider · Peers", "البدء · الحجم · المزود · المصادر"), type.tag)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            ranked.forEach { r -> C09SourceRow(r, compact = false, onClick = onClick) }
        }
    }
}
