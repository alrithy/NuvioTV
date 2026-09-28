package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.CacheState
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoStream
import com.nuvio.tv.prototype.shared.data.Resolution
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.isEnter
import com.nuvio.tv.prototype.shared.isUp
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/** One sentence describing the choice, spoken plainly. */
private fun sentence(s: ProtoStream, lang: ProtoLang): String {
    val ar = lang == ProtoLang.AR
    val res = when (s.resolution) {
        Resolution.R2160 -> "4K"
        Resolution.R1080 -> "1080p"
        Resolution.R720 -> "720p"
    }
    val hdr = s.hdr.firstOrNull { it.label != "SDR" }?.label
    val cached = when (s.cache) {
        CacheState.CACHED -> if (ar) "مخزّن على ${s.service}" else "cached on ${s.service}"
        CacheState.DIRECT -> if (ar) "بث مباشر" else "streamed directly"
        CacheState.UNCACHED -> if (ar) "غير مخزّن" else "not cached"
    }
    return if (ar) {
        "سنشغّل نسخة $res ${s.source.label}${hdr?.let { " بتقنية $it" } ?: ""} مع صوت ${s.audioLabel} — $cached، ويبدأ خلال ${s.startLabel}."
    } else {
        "Playing the $res ${s.source.label}${hdr?.let { " in $it" } ?: ""} with ${s.audioLabel} — $cached, starting in ${s.startLabel}."
    }
}

/**
 * One decision. The product chooses; you only confirm, or nudge left/right between intents.
 * A countdown plays the choice automatically. Down reveals every raw source.
 */
@Composable
internal fun C05Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c05Type()
    val lang = LocalProtoLang.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    val modes = StreamMode.entries.filter { it != StreamMode.ALL }
    var modeIdx by remember { mutableIntStateOf(0) }
    var raw by remember { mutableStateOf(false) }
    val mode = modes[modeIdx]
    val best = remember(mode) { StreamIntelligence.best(streams, mode) }
    val root = remember { FocusRequester() }
    RequestFocusOnce(root, key = raw, enabled = !raw)
    ProtoBackHandler(enabled = raw) { raw = false }
    val frozen = LocalProtoEnv.current.frozen
    val countdown = remember(modeIdx) { Animatable(0f) }
    LaunchedEffect(modeIdx, raw) {
        if (frozen) { countdown.snapTo(0.6f); return@LaunchedEffect }
        if (!raw) {
            countdown.snapTo(0f)
            countdown.animateTo(1f, tween(6000, easing = LinearEasing))
            session.nav.push(ProtoRoute.Player(t.id))
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color(0x33000000), 1f to Color(0xF0000000))))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .protoFocusable(root, onKey = { e ->
                    if (raw) return@protoFocusable false
                    if (e.isEnter) { if (e.isUp) session.nav.push(ProtoRoute.Player(t.id)); return@protoFocusable true }
                    if (!e.isDown) return@protoFocusable false
                    val next = if (rtl) Key.DirectionLeft else Key.DirectionRight
                    val prev = if (rtl) Key.DirectionRight else Key.DirectionLeft
                    when (e.key) {
                        next -> { modeIdx = (modeIdx + 1).coerceAtMost(modes.lastIndex); true }
                        prev -> { modeIdx = (modeIdx - 1).coerceAtLeast(0); true }
                        Key.DirectionDown -> { raw = true; true }
                        else -> false
                    }
                })
                .padding(horizontal = C05.Margin, vertical = 46.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                modes.forEachIndexed { i, m -> Txt(m.label.get().slate(), type.slateSmall.copy(color = if (i == modeIdx) C05.Ink else C05.Ink3)) }
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(best, transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(160)) }, label = "s") { r ->
                Txt(sentence(r.stream, lang), type.title.copy(fontSize = type.title.fontSize * 0.72f), Modifier.width(760.dp), maxLines = 3)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(760.dp).height(2.dp).background(Color(0x33FFFFFF))) {
                Box(Modifier.fillMaxWidth(countdown.value).height(2.dp).background(C05.Signal))
            }
            Spacer(Modifier.height(10.dp))
            Txt(tr("OK  PLAY NOW    ←→  CHANGE INTENT    ↓  ALL SOURCES", "موافق  شغّل الآن    ←→  غيّر الهدف    ↓  كل المصادر"), type.slateSmall)
        }
        AnimatedVisibility(raw, enter = fadeIn(tween(260)), exit = fadeOut(tween(200))) {
            C05RawSources(streams) { session.nav.push(ProtoRoute.Player(t.id)) }
        }
    }
}

@Composable
private fun C05RawSources(streams: List<ProtoStream>, onPick: () -> Unit) {
    val type = c05Type()
    val mono = type.slate.copy(fontFamily = LocalProtoFonts.current.plexMono)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Column(Modifier.fillMaxSize().background(Color(0xF2000000)).verticalScroll(rememberScrollState()).padding(horizontal = C05.Margin, vertical = 40.dp)) {
        Txt(tr("ALL SOURCES  ${streams.size}", "كل المصادر  ${streams.size}"), type.slate)
        Spacer(Modifier.height(12.dp))
        StreamIntelligence.rank(streams, StreamMode.ALL).forEachIndexed { i, r ->
            val s = r.stream
            C05Action(
                "${if (s.isCached) "●" else "○"} ${s.sizeLabel.padEnd(7)} ${s.addon.padEnd(12)} ${s.filename}",
                requester = if (i == 0) first else null,
                onClick = onPick,
            )
        }
        Spacer(Modifier.height(6.dp))
        Txt("● ${tr("cached / direct", "مخزّن / مباشر")}   ○ ${tr("needs download", "يحتاج تنزيل")}", mono.copy(color = C05.Ink3))
    }
}
