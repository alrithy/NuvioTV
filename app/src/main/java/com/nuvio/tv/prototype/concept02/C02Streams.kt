package com.nuvio.tv.prototype.concept02

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoIcon
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
import com.nuvio.tv.prototype.shared.ts

internal fun StreamMode.glyph(): Glyph = when (this) {
    StreamMode.BEST -> Glyph.SPARKLE
    StreamMode.MAX_QUALITY -> Glyph.GEM
    StreamMode.FASTEST -> Glyph.BOLT
    StreamMode.BALANCED -> Glyph.BALANCE
    StreamMode.SMALLER -> Glyph.FEATHER
    StreamMode.ALL -> Glyph.LIST
}

@Composable
internal fun C02Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c02Type()
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    C02Screen {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.22f })
        RiyadhSkyline(Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f }, intensity = 0.7f)
        Column(Modifier.fillMaxSize().padding(horizontal = C02.Margin, vertical = 30.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    C02Heading(Bi("Choose how to watch", "اختر طريقة المشاهدة"))
                    Spacer(Modifier.height(6.dp))
                    Txt(t.title.get() + "   ·   " + t.title.other(), type.title)
                }
                C02Pill(tr("${streams.size} sources · ${streams.count { it.isCached }} ready", "${streams.size} مصدراً · ${streams.count { it.isCached }} جاهزة"), dot = C02.EmeraldLight)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxSize()) {
                // Mode rail
                Column(Modifier.width(196.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StreamMode.entries.forEach { m -> C02ModeItem(m, m == mode) { mode = m } }
                }
                Spacer(Modifier.width(20.dp))
                AnimatedContent(mode, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(140)) }, label = "mode", modifier = Modifier.weight(1f)) { m ->
                    val r = remember(m) { StreamIntelligence.rank(streams, m) }
                    if (m == StreamMode.ALL) C02AllSources(r) { session.nav.push(ProtoRoute.Player(t.id)) }
                    else C02Recommendation(m, r, play) { session.nav.push(ProtoRoute.Player(t.id)) }
                }
            }
        }
    }
}

@Composable
private fun C02ModeItem(m: StreamMode, selected: Boolean, onSelect: () -> Unit) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(C02.Radius))
            .background(if (focused) C02.Raised else if (selected) C02.Glass else Color.Transparent)
            .protoFocusable(onFocusChange = { focused = it; if (it) onSelect() }, onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(2.dp).height(22.dp).background(if (selected) C02.Gold else Color.Transparent))
        Spacer(Modifier.width(10.dp))
        ProtoIcon(m.glyph(), size = 14.dp, color = if (selected) C02.GoldLight else C02.Ink2, stroke = 1.4.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Txt(m.label.get(), type.label.copy(color = if (selected || focused) C02.Ink else C02.Ink2), maxLines = 1)
            Txt(m.label.other(), type.captionSecond, maxLines = 1)
        }
    }
}

@Composable
private fun C02Recommendation(mode: StreamMode, ranked: List<RankedStream>, play: FocusRequester, onPlay: () -> Unit) {
    val type = c02Type()
    val best = ranked.first()
    val s = best.stream
    Row {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(C02.Raised.copy(alpha = 0.92f), C02.Surface.copy(alpha = 0.92f))))
                .border(1.dp, C02.GoldDim, RoundedCornerShape(10.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).border(1.dp, C02.Gold, CircleShape), contentAlignment = Alignment.Center) {
                    ProtoIcon(mode.glyph(), size = 12.dp, color = C02.GoldLight, stroke = 1.4.dp)
                }
                Spacer(Modifier.width(10.dp))
                C02Heading(Bi("Concierge recommends", "توصية الكونسيرج"))
                Spacer(Modifier.weight(1f))
                Txt(tr("${best.score}% match", "تطابق ${best.score}%"), type.meta.copy(color = C02.GoldLight))
            }
            Spacer(Modifier.height(14.dp))
            val head = StreamIntelligence.headline(s).split(" · ")
            Txt(head.take(2).joinToString("  ·  "), type.hero.copy(fontSize = type.hero.fontSize * 0.78f), maxLines = 1)
            Txt(head.drop(2).joinToString(" "), type.heroSecond.copy(fontFamily = LocalProtoFonts.current.sora, color = C02.GoldLight), maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Txt(mode.blurb.get(), type.meta)
            Spacer(Modifier.height(16.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    C02Spec(tr("Source", "المصدر"), "${s.source.label} · ${best.tier.get()}")
                    C02Spec(tr("Size", "الحجم"), "${s.sizeLabel} · ${s.bitrateLabel}")
                    C02Spec(tr("Service", "الخدمة"), if (s.isCached) tr("${s.service} · Cached", "${s.service} · مخزّن") else tr("Not cached", "غير مخزّن"), emerald = s.isCached)
                }
                Column(Modifier.weight(1f)) {
                    C02Spec(tr("Starts", "يبدأ خلال"), s.startLabel)
                    C02Spec(tr("Subtitles", "الترجمة"), if (s.hasArabicSubs) tr("Arabic available", "العربية متوفرة") else tr("English only", "الإنجليزية فقط"), emerald = s.hasArabicSubs)
                    C02Spec(tr("Add-on", "الإضافة"), s.addon)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C02GoldButton(tr("Play best", "شغّل الأنسب"), Glyph.PLAY, play, onClick = onPlay)
                C02GhostButton(tr("Technical details", "التفاصيل التقنية"), Glyph.INFO) {}
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.width(230.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
            C02Heading(Bi("Alternatives", "بدائل"))
            Spacer(Modifier.height(8.dp))
            ranked.drop(1).take(5).forEach { r -> C02AltRow(r, onPlay) }
        }
    }
}

@Composable
private fun C02Spec(k: String, v: String, emerald: Boolean = false) {
    val type = c02Type()
    Column(Modifier.padding(bottom = 10.dp)) {
        Txt(k, type.captionSecond.copy(color = C02.Ink3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (emerald) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(C02.EmeraldLight))
                Spacer(Modifier.width(6.dp))
            }
            Txt(v, type.label, maxLines = 1)
        }
    }
}

@Composable
private fun C02AltRow(r: RankedStream, onClick: () -> Unit) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    val s = r.stream
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(C02.Radius))
            .background(if (focused) C02.Raised else C02.Surface.copy(alpha = 0.6f))
            .border(1.dp, if (focused) C02.Gold else Color.Transparent, RoundedCornerShape(C02.Radius))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            C02Badge(if (s.resolution.label == "2160p") "4K" else s.resolution.label.uppercase())
            s.hdr.firstOrNull { it.label != "SDR" }?.let { C02Badge(if (it.label == "Dolby Vision") "DV" else it.label) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (s.isCached) C02.EmeraldLight else C02.Ink3))
        }
        Spacer(Modifier.height(6.dp))
        Txt(s.audioLabel, type.label, maxLines = 1)
        Txt("${s.source.label} · ${s.sizeLabel} · ${s.addon}", type.captionSecond, maxLines = 1)
    }
}

@Composable
private fun C02AllSources(ranked: List<RankedStream>, onClick: () -> Unit) {
    val type = c02Type()
    val mono = ts(LocalProtoFonts.current.plexMono, 10.sp, color = C02.Ink3)
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(C02.Surface.copy(alpha = 0.9f))
            .border(1.dp, C02.Line, RoundedCornerShape(10.dp))
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ranked.forEachIndexed { i, r ->
            var focused by remember { mutableStateOf(false) }
            val s = r.stream
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (focused) C02.Raised else Color.Transparent)
                    .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (s.isCached) C02.EmeraldLight else C02.Ink3))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Txt(StreamIntelligence.headline(s) + "  ·  " + s.sizeLabel, type.label.copy(color = if (focused) C02.Ink else C02.Ink2), maxLines = 1)
                    Txt(s.filename, mono, maxLines = 1)
                }
                Txt("${s.addon} · ${s.seeders?.let { "$it peers" } ?: "direct"}", type.captionSecond, maxLines = 1)
            }
            if (i < ranked.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(C02.Line.copy(alpha = 0.3f)))
        }
    }
}
