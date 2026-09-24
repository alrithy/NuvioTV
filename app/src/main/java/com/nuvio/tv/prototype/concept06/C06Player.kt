package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.PlayerPanel
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.kenBurns
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.formatTimecode
import com.nuvio.tv.prototype.shared.player.ProtoPlayerState
import com.nuvio.tv.prototype.shared.player.playerWake
import com.nuvio.tv.prototype.shared.player.rememberProtoPlayer
import com.nuvio.tv.prototype.shared.player.scrubKeys
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/** The player's chrome is an instrument cluster: gauge, timeline, tracks. */
@Composable
internal fun C06Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c06Type()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(C06.DETENT + 60, easing = ProtoEasing.Detent), label = "cluster")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xE60B0C0E))))

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(126.dp).padding(horizontal = 20.dp, vertical = 16.dp)
                .graphicsLayer { alpha = show; translationY = (1f - show) * 24.dp.toPx() },
            horizontalArrangement = Arrangement.spacedBy(C06.Gap),
        ) {
            val s = player.stream
            // Gauge: live bitrate against the stream's peak.
            C06Module(Modifier.width(118.dp).fillMaxHeight(), focusable = false, padding = 8.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    C06Arc(0.82f, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt("${(s.bitrateMbps * 0.9).toInt()}", type.readoutBig.copy(fontSize = type.readoutBig.fontSize * 0.8f))
                        Txt("Mbps", type.label)
                    }
                }
            }
            C06Module(Modifier.weight(1f).fillMaxHeight(), focusable = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt(t.title.get(), type.title, maxLines = 1)
                        player.chapters.getOrNull(player.currentChapter)?.let { Txt("${tr("Ch.", "الفصل")} ${player.currentChapter + 1} · ${it.title.get()}", type.caption, maxLines = 1) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        C06Square(Glyph.REPLAY10) { player.seekBy(-10f) }
                        C06Square(if (player.playing) Glyph.PAUSE else Glyph.PLAY, playReq, primary = true) { player.togglePlay() }
                        C06Square(Glyph.FORWARD10) { player.seekBy(10f) }
                        if (t.isSeries) C06Square(Glyph.NEXT) {}
                    }
                }
                Spacer(Modifier.weight(1f))
                C06Timeline(player)
            }
            C06Module(Modifier.width(220.dp).fillMaxHeight(), focusable = false, padding = 10.dp) {
                C06Readout(tr("Video", "الفيديو"), "${s.resolution.label} · ${s.hdr.first().label}")
                C06Readout(tr("Audio", "الصوت"), player.audio.format + " " + player.audio.channels)
                C06Readout(tr("Subs", "الترجمة"), player.subtitle?.language?.get() ?: tr("Off", "إيقاف"))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    C06Square(Glyph.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                    C06Square(Glyph.AUDIO) { panel = PlayerPanel.AUDIO }
                    C06Square(Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                }
            }
        }

        // Scrub preview is its own module above the console, so the frame is never clipped by the bar.
        AnimatedVisibility(
            player.scrubbing,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 134.dp),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
        ) {
            C06Module(Modifier.width(300.dp), focusable = false, padding = 8.dp) { _ ->
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(4.dp))) {
                    ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false)
                }
                Spacer(Modifier.height(6.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(formatTimecode(player.scrubPosition.toInt(), true), type.readout)
                        Spacer(Modifier.weight(1f))
                        Txt(player.chapters.getOrNull(player.chapterAt(player.scrubFraction))?.title?.get() ?: "", type.caption.copy(color = C06.Ember), maxLines = 1)
                    }
                }
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
            enter = slideInHorizontally(tween(260, easing = ProtoEasing.Detent)) { it / 3 } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(200)) { it / 3 } + fadeOut(tween(140)),
        ) {
            C06TrackModule(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C06Timeline(player: ProtoPlayerState) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column {
            Row {
                Txt(formatTimecode(player.displayFraction.times(player.durationSec).toInt(), true), type.readout)
                Spacer(Modifier.weight(1f))
                if (player.scrubbing) Txt(player.chapters.getOrNull(player.chapterAt(player.scrubFraction))?.title?.get() ?: "", type.caption.copy(color = C06.Ember))
                Spacer(Modifier.weight(1f))
                Txt("-" + formatTimecode(((1 - player.displayFraction) * player.durationSec).toInt(), true), type.readout.copy(color = C06.Ink2))
            }
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(14.dp).scrubKeys(player).protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x22FFFFFF)))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(4.dp).background(if (focused) C06.Ember else C06.Ink))
                player.chapters.forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(1.dp).height(10.dp).background(C06.Ink3)) }
            }
        }
    }
}

@Composable
private fun C06Square(glyph: Glyph, requester: FocusRequester? = null, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
            .background(if (primary) lerpColor(C06.Ember, Color(0xFFFF7A4F), f) else lerpColor(C06.PanelHi, C06.Ink, f))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProtoIcon(glyph, size = 14.dp, color = if (primary) Color.White else lerpColor(C06.Ink, C06.Base, f), stroke = 1.6.dp)
    }
}

@Composable
private fun C06TrackModule(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c06Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    C06Module(Modifier.width(300.dp), focusable = false) {
        C06Label(if (panel == PlayerPanel.SUBTITLES) tr("Subtitle track", "مسار الترجمة") else tr("Audio track", "مسار الصوت"))
        Spacer(Modifier.height(6.dp))
        val rows: List<Triple<String, String, Boolean>> = if (panel == PlayerPanel.SUBTITLES) {
            listOf(Triple(tr("Off", "إيقاف"), "", player.subtitle == null)) + MockCatalog.subtitles.map { s ->
                Triple(s.language.get() + (s.variant?.let { " · " + it.get() } ?: ""), "${s.format} · ${s.source}", player.subtitle?.id == s.id)
            }
        } else {
            MockCatalog.audioTracks.map { a -> Triple(a.language.get() + (a.note?.let { " · " + it.get() } ?: ""), "${a.format} ${a.channels}", player.audio.id == a.id) }
        }
        rows.forEachIndexed { i, (label, detail, sel) ->
            var focused by remember { mutableStateOf(false) }
            val f = detent(focused)
            Row(
                Modifier.fillMaxWidth().module(f, 6.dp).protoFocusable(if (sel) first else null, onFocusChange = { focused = it }, onClick = {
                    if (panel == PlayerPanel.SUBTITLES) player.subtitle = if (i == 0) null else MockCatalog.subtitles[i - 1] else player.audio = MockCatalog.audioTracks[i]
                    onClose()
                }).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(label, type.value.copy(color = if (focused || sel) C06.Ink else C06.Ink2), maxLines = 1)
                    if (detail.isNotEmpty()) Txt(detail, type.caption, maxLines = 1)
                }
                if (sel) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(C06.Ember))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
