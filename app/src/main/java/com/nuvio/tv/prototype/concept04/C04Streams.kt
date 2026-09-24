package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C04Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c04Type()
    val accent = rememberAccent(t)
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    val rise = remember { Animatable(40f) }
    LaunchedEffect(Unit) { rise.animateTo(0f, tween(520, easing = ProtoEasing.Decelerate)) }

    C04Room(t, dim = 0.5f) {
        Column(Modifier.fillMaxSize().padding(horizontal = C04.Margin, vertical = 30.dp)) {
            Txt(t.title.get(), type.hero)
            Txt(tr("How would you like to watch?", "كيف تود المشاهدة؟"), type.body)
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = rise.value * density }
                    .glass(28.dp)
                    .padding(20.dp),
            ) {
                C04Segmented(StreamMode.entries.map { it.label.get() }, mode.ordinal, { mode = StreamMode.entries[it] })
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    mode,
                    transitionSpec = { (fadeIn(tween(320)) + slideInVertically(tween(360, easing = ProtoEasing.Decelerate)) { it / 16 }) togetherWith fadeOut(tween(140)) },
                    label = "mode",
                ) { m ->
                    val ranked = remember(m) { StreamIntelligence.rank(streams, m) }
                    if (m == StreamMode.ALL) C04AllSources(ranked) { session.nav.push(ProtoRoute.Player(t.id)) }
                    else Row {
                        C04Best(ranked.first(), m, accent, play) { session.nav.push(ProtoRoute.Player(t.id)) }
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ranked.drop(1).take(5).forEach { r -> C04Alt(r, accent) { session.nav.push(ProtoRoute.Player(t.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C04Best(r: RankedStream, mode: StreamMode, accent: Color, play: FocusRequester, onPlay: () -> Unit) {
    val type = c04Type()
    val s = r.stream
    Row(Modifier.width(500.dp)) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Color(0x26FFFFFF), 0f, 360f, false, style = Stroke(7.dp.toPx()))
                drawArc(accent, -90f, 360f * r.score / 100f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("${r.score}%", type.title)
                Txt(tr("match", "تطابق"), type.meta)
            }
        }
        Spacer(Modifier.width(20.dp))
        Column {
            Txt(mode.label.get().uppercase(), type.meta.copy(color = accent))
            Spacer(Modifier.height(4.dp))
            Txt(StreamIntelligence.headline(s).split(" · ").take(2).joinToString(" · "), type.hero.copy(fontSize = type.hero.fontSize * 0.8f), maxLines = 1)
            Txt(s.audioLabel, type.title.copy(color = C04.Ink2), maxLines = 1)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                C04Chip(s.source.label)
                C04Chip(s.sizeLabel)
                C04Chip(if (s.isCached) tr("${s.service} cached", "مخزّن") else tr("Not cached", "غير مخزّن"))
                C04Chip(tr("Starts ${s.startLabel}", "يبدأ ${s.startLabel}"))
            }
            Spacer(Modifier.height(8.dp))
            Txt(r.reasons.map { it.get() }.joinToString("  ·  "), type.caption, maxLines = 2)
            Spacer(Modifier.height(16.dp))
            C04Pill(tr("Play best", "شغّل الأنسب"), Glyph.PLAY, primary = true, accent = accent, requester = play, onClick = onPlay)
        }
    }
}

@Composable
private fun C04Alt(r: RankedStream, accent: Color, onClick: () -> Unit) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    val s = r.stream
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { val k = if (focused) 1.03f else 1f; scaleX = k; scaleY = k }
            .bloom(accent, if (focused) 0.7f else 0f, 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0x40FFFFFF) else Color(0x14FFFFFF))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(StreamIntelligence.headline(s), type.label, maxLines = 1)
            Txt("${s.source.label} · ${s.sizeLabel} · ${s.addon}", type.caption, maxLines = 1)
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (s.isCached) Color(0xFF7CE0A8) else Color(0x55FFFFFF)))
    }
}

@Composable
private fun C04AllSources(ranked: List<RankedStream>, onClick: () -> Unit) {
    val type = c04Type()
    val mono = type.caption.copy(fontFamily = LocalProtoFonts.current.plexMono)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranked.forEach { r ->
            var focused by remember { mutableStateOf(false) }
            val s = r.stream
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (focused) Color(0x40FFFFFF) else Color(0x10FFFFFF))
                    .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row {
                    Txt(StreamIntelligence.headline(s) + " · " + s.sizeLabel, type.label, Modifier.weight(1f), maxLines = 1)
                    Txt(s.addon + " · " + (if (s.isCached) tr("cached", "مخزّن") else "${s.seeders} peers"), type.caption)
                }
                Txt(s.filename, mono, maxLines = 1)
            }
        }
    }
}
