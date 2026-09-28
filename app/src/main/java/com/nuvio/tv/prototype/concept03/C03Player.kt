package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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

/** A single basalt slab rises from the base of the picture; everything else is left untouched. */
@Composable
internal fun C03Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c03Type()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(C03.SLOW, easing = ProtoEasing.Cinematic), label = "slab")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(C03.Shadow).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())

        if (player.scrubbing) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(bottom = 140.dp, start = 200.dp, end = 200.dp)) {
                    val x = maxWidth * player.scrubFraction
                    Column(Modifier.offset(x = (x - 90.dp).coerceIn(0.dp, maxWidth - 180.dp)).width(180.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).stoneEdge().background(C03.Shadow)) {
                            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false)
                        }
                        Box(Modifier.fillMaxWidth().background(C03.Basalt).padding(6.dp)) {
                            Txt(formatTimecode(player.scrubPosition.toInt(), true), type.label.copy(color = C03.Sand))
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - show) * 130.dp.toPx(); alpha = show }
                .background(C03.Basalt.copy(alpha = 0.94f))
                .padding(horizontal = C03.Margin, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Txt(formatTimecode(player.displayFraction.times(player.durationSec).toInt(), true), type.numeral.copy(fontSize = type.numeral.fontSize * 0.55f), Modifier.width(190.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(t.title.get().up(), type.label.copy(color = C03.Sand), maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    player.chapters.getOrNull(player.currentChapter)?.let { Txt(numeral(player.currentChapter + 1) + "  ·  " + it.title.get().up(), type.small.copy(color = C03.Bronze), maxLines = 1) }
                }
                Spacer(Modifier.height(12.dp))
                C03Timeline(player)
                Spacer(Modifier.height(8.dp))
                val s = player.stream
                Txt(listOf(s.resolution.label, s.hdr.first().label, s.audioLabel, if (s.isCached) tr("${s.service} cached", "مخزّن") else "", s.bitrateLabel).filter { it.isNotEmpty() }.joinToString("  ·  ").up(), type.small, maxLines = 1)
            }
            Spacer(Modifier.width(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                C03Square(Glyph.REPLAY10) { player.seekBy(-10f) }
                C03Square(if (player.playing) Glyph.PAUSE else Glyph.PLAY, playReq) { player.togglePlay() }
                C03Square(Glyph.FORWARD10) { player.seekBy(10f) }
                Spacer(Modifier.width(8.dp))
                C03Square(Glyph.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                C03Square(Glyph.AUDIO) { panel = PlayerPanel.AUDIO }
                C03Square(Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(tween(520, easing = ProtoEasing.Cinematic)) { -it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(320)) { -it } + fadeOut(tween(200)),
        ) {
            C03TrackSlab(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C03Timeline(player: ProtoPlayerState) {
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(14.dp).scrubKeys(player).protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
            contentAlignment = Alignment.CenterStart,
        ) {
            val w = maxWidth
            Box(Modifier.fillMaxWidth().height(2.dp).background(C03.SandFaint))
            Box(Modifier.fillMaxWidth(player.displayFraction).height(2.dp).background(if (focused) C03.Sand else C03.Bronze))
            // Carved chapter notches.
            player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(2.dp).height(10.dp).background(C03.Stone)) }
            Box(Modifier.offset(x = w * player.displayFraction - 1.dp).width(3.dp).height(if (focused) 14.dp else 10.dp).background(C03.Sand))
        }
    }
}

@Composable
private fun C03Square(glyph: Glyph, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(38.dp)
            .background(if (focused) C03.Sand else C03.Stone)
            .border(1.dp, if (focused) C03.Sand else C03.Bronze.copy(alpha = 0.35f))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProtoIcon(glyph, size = 15.dp, color = if (focused) C03.Shadow else C03.Sand, stroke = 1.3.dp)
    }
}

@Composable
private fun C03TrackSlab(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c03Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Column(
        Modifier
            .fillMaxHeight()
            .width(330.dp)
            .background(C03.Basalt)
            .stoneEdge()
            .padding(horizontal = 30.dp, vertical = 44.dp),
    ) {
        Txt(if (panel == PlayerPanel.SUBTITLES) tr("SUBTITLES", "الترجمة") else tr("SOUND", "الصوت"), type.label.copy(color = C03.Bronze))
        Spacer(Modifier.height(18.dp))
        if (panel == PlayerPanel.SUBTITLES) {
            C03ListItem(tr("NONE", "بلا ترجمة"), style = type.label.copy(color = C03.Sand), maxScale = 1.2f, selected = player.subtitle == null) { player.subtitle = null; onClose() }
            MockCatalog.subtitles.forEachIndexed { i, s ->
                val sel = player.subtitle?.id == s.id
                C03ListItem(
                    (s.language.get() + (s.variant?.let { " · " + it.get() } ?: "")).up() + "   " + s.format,
                    requester = if (sel || (player.subtitle == null && i == 0)) first else null,
                    style = type.label.copy(color = C03.Sand), maxScale = 1.2f, selected = sel,
                ) { player.subtitle = s; onClose() }
            }
        } else {
            MockCatalog.audioTracks.forEach { a ->
                val sel = player.audio.id == a.id
                Column {
                    C03ListItem((a.language.get() + (a.note?.let { " · " + it.get() } ?: "")).up(), requester = if (sel) first else null, style = type.label.copy(color = C03.Sand), maxScale = 1.2f, selected = sel) { player.audio = a; onClose() }
                    Txt("${a.format} ${a.channels}".up(), type.small, Modifier.padding(start = 17.dp, bottom = 6.dp))
                }
            }
        }
    }
}
