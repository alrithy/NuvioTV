package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

/** Controls live on one floating glass island; the picture is never covered edge to edge. */
@Composable
internal fun C04Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c04Type()
    val accent = rememberAccent(t)
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(360, easing = ProtoEasing.Decelerate), label = "island")
    // The island steps back while a track card is open, so only one glass layer is ever in front.
    val island by animateFloatAsState(if (panel == PlayerPanel.NONE) show else 0f, tween(300, easing = ProtoEasing.Decelerate), label = "islandOnly")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())

        // Quality HUD
        Row(
            Modifier.align(Alignment.TopEnd).padding(28.dp).graphicsLayer { alpha = show }.glass(50.dp).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF7CE0A8)))
            Spacer(Modifier.width(8.dp))
            val s = player.stream
            Txt(
                listOfNotNull(tr("Ready", "جاهز"), s.resolution.label, s.hdr.first().label, s.audio.short + " " + s.channels, if (s.isCached) tr("Cached", "مخزّن") else null, player.subtitle?.code).joinToString("  ·  "),
                type.label,
                maxLines = 1,
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .width(760.dp)
                .graphicsLayer { alpha = island; translationY = (1f - island) * 30.dp.toPx(); scaleX = 0.96f + 0.04f * island; scaleY = 0.96f + 0.04f * island },
        ) {
            if (player.scrubbing) C04ScrubBubble(player)
            Column(Modifier.fillMaxWidth().bloom(accent, 0.35f, 28.dp).glass(28.dp, strong = true).padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    C04Circle(if (player.playing) Glyph.PAUSE else Glyph.PLAY, 46.dp, accent, playReq) { player.togglePlay() }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(t.title.get(), type.section, maxLines = 1)
                        player.chapters.getOrNull(player.currentChapter)?.let { Txt(it.title.get(), type.caption, maxLines = 1) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        C04Circle(Glyph.REPLAY10, 38.dp, accent) { player.seekBy(-10f) }
                        C04Circle(Glyph.FORWARD10, 38.dp, accent) { player.seekBy(10f) }
                        C04Circle(Glyph.SUBTITLES, 38.dp, accent) { panel = PlayerPanel.SUBTITLES }
                        C04Circle(Glyph.AUDIO, 38.dp, accent) { panel = PlayerPanel.AUDIO }
                        C04Circle(Glyph.SOURCES, 38.dp, accent) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                C04Timeline(player, accent)
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterEnd).padding(28.dp),
            enter = fadeIn(tween(280)) + scaleIn(tween(360, easing = ProtoEasing.Decelerate), initialScale = 0.92f),
            exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.95f),
        ) {
            C04TrackCard(player, panel, accent) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C04Timeline(player: ProtoPlayerState, accent: Color) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(formatTimecode(player.displayFraction.times(player.durationSec).toInt()), type.caption, Modifier.width(58.dp))
            BoxWithConstraints(
                Modifier.weight(1f).height(18.dp).scrubKeys(player).protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val h = if (focused) 8.dp else 5.dp
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF)))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(h).clip(RoundedCornerShape(50)).background(accent))
                player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(2.dp).height(h).background(Color(0x66000000))) }
                val knob = if (focused) 16.dp else 0.dp
                Box(Modifier.offset(x = w * player.displayFraction - knob / 2).size(knob).clip(CircleShape).background(Color.White))
            }
            Txt("-" + formatTimecode(player.remainingSec), type.caption, Modifier.padding(start = 10.dp).width(58.dp))
        }
    }
}

@Composable
private fun C04ScrubBubble(player: ProtoPlayerState) {
    val type = c04Type()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(126.dp)) {
            val x = maxWidth * player.scrubFraction
            Column(Modifier.offset(x = (x - 84.dp).coerceIn(0.dp, maxWidth - 168.dp)).width(168.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(168.dp).height(95.dp).clip(RoundedCornerShape(20.dp))) {
                    ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.glass(50.dp).padding(horizontal = 10.dp, vertical = 3.dp)) { Txt(formatTimecode(player.scrubPosition.toInt()), type.label) }
            }
        }
    }
}

@Composable
private fun C04Circle(glyph: Glyph, size: androidx.compose.ui.unit.Dp, accent: Color, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C04.FOCUS_MS), label = "cc")
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = 1f + 0.12f * f; scaleY = 1f + 0.12f * f }
            .bloom(accent, f, 50.dp)
            .clip(CircleShape)
            .background(if (focused) Color.White else Color(0x22FFFFFF))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProtoIcon(glyph, size = size * 0.4f, color = if (focused) C04.Dark else Color.White, stroke = 1.8.dp)
    }
}

@Composable
private fun C04TrackCard(player: ProtoPlayerState, panel: PlayerPanel, accent: Color, onClose: () -> Unit) {
    val type = c04Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Column(Modifier.width(300.dp).bloom(accent, 0.4f, 28.dp).glass(28.dp, strong = true).padding(18.dp)) {
        Txt(if (panel == PlayerPanel.SUBTITLES) tr("Subtitles", "الترجمة") else tr("Audio", "الصوت"), type.title)
        Spacer(Modifier.height(12.dp))
        if (panel == PlayerPanel.SUBTITLES) {
            C04TrackRow(tr("Off", "إيقاف"), null, player.subtitle == null, null, accent) { player.subtitle = null; onClose() }
            MockCatalog.subtitles.forEach { s ->
                val sel = player.subtitle?.id == s.id
                C04TrackRow(s.language.get() + (s.variant?.let { " · " + it.get() } ?: ""), "${s.source} · ${s.format}", sel, if (sel) first else null, accent) { player.subtitle = s; onClose() }
            }
        } else {
            MockCatalog.audioTracks.forEach { a ->
                val sel = player.audio.id == a.id
                C04TrackRow(a.language.get() + (a.note?.let { " · " + it.get() } ?: ""), "${a.format} ${a.channels}", sel, if (sel) first else null, accent) { player.audio = a; onClose() }
            }
        }
    }
}

@Composable
private fun C04TrackRow(label: String, detail: String?, selected: Boolean, requester: FocusRequester?, accent: Color, onClick: () -> Unit) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(label, type.label.copy(color = if (focused) C04.Dark else C04.Ink), maxLines = 1)
            if (detail != null) Txt(detail, type.caption.copy(color = if (focused) C04.Dark.copy(alpha = 0.6f) else C04.Ink3), maxLines = 1)
        }
        if (selected) Box(Modifier.size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            ProtoIcon(Glyph.CHECK, size = 12.dp, color = C04.Dark, stroke = 2.dp)
        }
    }
}
