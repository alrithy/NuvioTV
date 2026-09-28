package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.tr

private val Glass = Color(0xA60C0C0E)

/**
 * The player behaves like the rest of the OS: the film is the room, the controls are one soft
 * pill that fades in. The timeline holds focus, so left/right scrub straight away and centre
 * plays or pauses; everything else is one step down.
 */
@Composable
internal fun C08Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c08Type()
    val clock by rememberProtoClock()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(600, easing = ProtoEasing.Decelerate), label = "show")
    val timeline = remember { FocusRequester() }
    RequestFocusOnce(timeline, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(C08.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        // Dim the room from the edges when controls are up, never the middle of the picture.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.radialGradient(listOf(Color.Transparent, Color(0x99000000)), radius = 1400f)))
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xCC000000))))

        // Soft title chip and the time, like a status bar that only appears when asked.
        Row(
            Modifier.align(Alignment.TopStart).padding(start = C08.Margin, top = 32.dp).graphicsLayer { alpha = show }
                .clip(RoundedCornerShape(50)).background(Glass).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            C08Dot(t.palette.ui)
            Spacer(Modifier.width(10.dp))
            val ep = t.resume
            Txt(t.title.get() + (ep?.let { " · " + it.title.get() } ?: ""), type.label, maxLines = 1)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(end = C08.Margin, top = 28.dp).graphicsLayer { alpha = show }, horizontalAlignment = Alignment.End) {
            Txt(clock.clock24(), type.title)
            val end = (clock.hour24 * 60 + clock.minute + player.remainingSec / 60) % (24 * 60)
            val endLabel = "${(end / 60).toString().padStart(2, '0')}:${(end % 60).toString().padStart(2, '0')}"
            Txt(tr("Ends at $endLabel", "ينتهي عند $endLabel"), type.small.copy(color = C08.Ink2))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp).graphicsLayer { alpha = show },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(player.scrubbing, enter = fadeIn(tween(260)), exit = fadeOut(tween(200))) { C08Filmstrip(player) }
            Box(Modifier.graphicsLayer { alpha = if (panel == PlayerPanel.NONE) 1f else 0f }) { C08Hud(player) }
            Spacer(Modifier.height(12.dp))
            C08Timeline(player, timeline)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C08Round(if (player.playing) Glyph.PAUSE else Glyph.PLAY) { player.togglePlay() }
                C08Round(Glyph.REPLAY10) { player.seekBy(-10f) }
                C08Round(Glyph.SUBTITLES, selected = panel == PlayerPanel.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                C08Round(Glyph.AUDIO, selected = panel == PlayerPanel.AUDIO) { panel = PlayerPanel.AUDIO }
                C08Round(Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C08Pill(tr("Next episode", "الحلقة التالية"), Glyph.NEXT) {}
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 152.dp),
            enter = fadeIn(tween(360)) + slideInVertically(tween(360, easing = ProtoEasing.Decelerate)) { it / 6 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 8 },
        ) {
            C08TrackPanel(player, panel, t.palette.ui) { panel = PlayerPanel.NONE }
        }
    }
}

/** Quality, spoken softly: a few lit words above the timeline. */
@Composable
private fun C08Hud(player: ProtoPlayerState) {
    val type = c08Type()
    val s = player.stream
    val words = listOfNotNull(
        if (s.resolution.label == "2160p") "4K" else s.resolution.label,
        s.hdr.firstOrNull { it.label != "SDR" }?.label,
        s.audioLabel,
        if (s.isCached) tr("Cached · ${s.service}", "مخزّن · ${s.service}") else tr("Streaming", "بث"),
        player.subtitle?.language?.get()?.let { tr("$it subtitles", "ترجمة $it") },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { i, w ->
            if (i > 0) { Spacer(Modifier.width(10.dp)); C08Dot(C08.Ink3, 4.dp); Spacer(Modifier.width(10.dp)) }
            Txt(w, type.small.copy(color = C08.Ink2), maxLines = 1)
        }
    }
}

@Composable
private fun C08Timeline(player: ProtoPlayerState, requester: FocusRequester) {
    val type = c08Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(300, easing = ProtoEasing.Decelerate), label = "tl")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.width(880.dp).clip(RoundedCornerShape(50)).background(Glass).padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProtoIcon(if (player.playing) Glyph.PLAY else Glyph.PAUSE, size = 14.dp, color = C08.Ink2, stroke = 1.6.dp)
            Spacer(Modifier.width(14.dp))
            Txt(formatTimecode((if (player.scrubbing) player.scrubPosition else player.position).toInt()), type.label, Modifier.width(62.dp))
            BoxWithConstraints(
                Modifier.weight(1f).height(18.dp).scrubKeys(player).protoFocusable(requester, onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val h = 4.dp + 3.dp * f
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(C08.SoftStrong))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(h).clip(RoundedCornerShape(50)).background(C08.Ink))
                player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).size(3.dp).clip(CircleShape).background(C08.Black.copy(alpha = 0.6f))) }
                Box(Modifier.offset(x = w * player.displayFraction - 7.dp).size(14.dp).graphicsLayer { alpha = f; scaleX = 0.6f + 0.4f * f; scaleY = 0.6f + 0.4f * f }.clip(CircleShape).background(C08.Ink))
            }
            Spacer(Modifier.width(14.dp))
            Txt("-" + formatTimecode(player.remainingSec), type.label.copy(color = C08.Ink2), maxLines = 1)
        }
    }
}

/** Scrub preview with the same dock magnification as Home: the frame under the thumb grows. */
@Composable
private fun C08Filmstrip(player: ProtoPlayerState) {
    val type = c08Type()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                for (k in -2..2) {
                    val pos = (player.scrubPosition + k * 60f).coerceIn(0f, player.durationSec.toFloat())
                    val w = when (kotlin.math.abs(k)) { 0 -> 220.dp; 1 -> 150.dp; else -> 110.dp }
                    Box(
                        Modifier.width(w).aspectRatio(16f / 9f).then(if (k == 0) Modifier.halo(player.title.palette.ui, 1f) else Modifier)
                            .clip(RoundedCornerShape(20.dp)).graphicsLayer { alpha = when (kotlin.math.abs(k)) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f } },
                    ) {
                        ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (pos / 60f).toInt() + 1, remote = false)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val ch = player.chapters.getOrNull(player.currentChapter)
            Txt(formatTimecode(player.scrubPosition.toInt()) + (ch?.let { "  ·  " + it.title.get() } ?: ""), type.label, maxLines = 1)
        }
    }
}

/** Round soft control. Lights white on focus; a small light underneath marks the open panel. */
@Composable
private fun C08Round(glyph: Glyph, selected: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(300, easing = ProtoEasing.Decelerate), label = "round")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(46.dp).graphicsLayer { scaleX = 1f + 0.08f * f; scaleY = 1f + 0.08f * f }.clip(CircleShape)
                .background(if (focused) C08.Ink.copy(alpha = 0.92f) else Glass)
                .protoFocusable(onFocusChange = { focused = it }, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { ProtoIcon(glyph, size = 18.dp, color = if (focused) C08.Black else C08.Ink, stroke = 1.6.dp) }
        Spacer(Modifier.height(5.dp))
        C08Dot(if (selected) C08.Ink else Color.Transparent, 4.dp)
    }
}

@Composable
private fun C08TrackPanel(player: ProtoPlayerState, panel: PlayerPanel, glow: Color, onClose: () -> Unit) {
    val type = c08Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    val subs = panel == PlayerPanel.SUBTITLES
    val rows: List<Triple<String, String, Boolean>> = if (subs) {
        listOf(Triple(tr("Off", "بدون"), "", player.subtitle == null)) +
            MockCatalog.subtitles.map { s -> Triple(s.language.get() + (s.variant?.let { " · " + it.get() } ?: ""), s.source, player.subtitle?.id == s.id) }
    } else {
        MockCatalog.audioTracks.map { a -> Triple(a.language.get() + (a.note?.let { " · " + it.get() } ?: ""), "${a.format} ${a.channels}", player.audio.id == a.id) }
    }
    Column(Modifier.width(460.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xE6101012)).padding(horizontal = 16.dp, vertical = 16.dp)) {
        Txt(if (subs) tr("Subtitles", "الترجمة") else tr("Sound", "الصوت"), type.title, Modifier.padding(start = 14.dp, bottom = 6.dp))
        rows.forEachIndexed { i, (label, detail, sel) ->
            var focused by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (focused) C08.Ink.copy(alpha = 0.92f) else Color.Transparent)
                    .protoFocusable(if (sel) first else null, onFocusChange = { focused = it }, onClick = {
                        if (subs) player.subtitle = if (i == 0) null else MockCatalog.subtitles[i - 1] else player.audio = MockCatalog.audioTracks[i]
                        onClose()
                    })
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                C08Dot(if (sel) glow else Color.Transparent)
                Spacer(Modifier.width(12.dp))
                Txt(label, type.label.copy(color = if (focused) C08.Black else C08.Ink), Modifier.weight(1f), maxLines = 1)
                if (detail.isNotEmpty()) Txt(detail, type.small.copy(color = if (focused) C08.Black.copy(alpha = 0.55f) else C08.Ink3), maxLines = 1)
            }
        }
    }
}
