package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.PlayerPanel
import com.nuvio.tv.prototype.shared.ProtoBackHandler
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
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

@Composable
internal fun C05Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c05Type()
    val player = rememberProtoPlayer(t, autoHideMs = 3000)
    var panel by remember { mutableStateOf(initialPanel) }
    val ui by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(300), label = "ui")
    val timeline = remember { FocusRequester() }
    RequestFocusOnce(timeline, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        // While scrubbing, the whole picture becomes the preview. No thumbnails.
        if (player.scrubbing) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false)
            Txt(formatTimecode(player.scrubPosition.toInt(), true), type.big, Modifier.align(Alignment.TopStart).padding(C05.Margin))
        } else {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = ui }.background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color(0xB3000000))))

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = C05.Margin, vertical = 30.dp).graphicsLayer { alpha = ui }) {
            val s = player.stream
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(t.title.get().slate(), type.slate, modifier = Modifier.weight(1f), maxLines = 1)
                Txt("${s.resolution.label}  /  ${s.hdr.first().label}  /  ${s.audio.short} ${s.channels}  /  ${if (s.isCached) tr("CACHED", "مخزّن") else tr("STREAM", "بث")}  /  ${player.subtitle?.code ?: tr("NO SUBS", "بلا ترجمة")}".slate(), type.slateSmall, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            C05Hairline(player, timeline)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                C05Action(if (player.playing) tr("Pause", "إيقاف") else tr("Play", "تشغيل")) { player.togglePlay() }
                C05Action(tr("Subtitles", "الترجمة")) { panel = PlayerPanel.SUBTITLES }
                C05Action(tr("Audio", "الصوت")) { panel = PlayerPanel.AUDIO }
                C05Action(tr("Sources", "المصادر")) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C05Action(tr("Next", "التالي")) {}
            }
        }

        AnimatedVisibility(panel != PlayerPanel.NONE, modifier = Modifier.align(Alignment.CenterEnd), enter = fadeIn(tween(260)), exit = fadeOut(tween(200))) {
            C05Tracks(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C05Hairline(player: ProtoPlayerState, requester: FocusRequester) {
    val type = c05Type()
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(formatTimecode(player.position.toInt(), true), type.slate.copy(color = C05.Ink), Modifier.width(78.dp))
            BoxWithConstraints(
                Modifier.weight(1f).height(12.dp).scrubKeys(player, 60f).protoFocusable(requester, onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(1.dp).background(C05.Ink3))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(1.dp).background(C05.Ink))
                Box(Modifier.offset(x = w * player.displayFraction - 1.dp).width(2.dp).height(if (focused) 12.dp else 6.dp).background(C05.Signal))
            }
            Txt(formatTimecode(player.durationSec, true), type.slate, Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun C05Tracks(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c05Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Box(Modifier.fillMaxHeight().width(420.dp).background(startScrim(0f to Color.Transparent, 0.35f to Color(0xCC000000), 1f to Color(0xF2000000)))) {
        Column(Modifier.align(Alignment.CenterEnd).padding(horizontal = C05.Margin)) {
            Txt(if (panel == PlayerPanel.SUBTITLES) tr("SUBTITLES", "الترجمة") else tr("AUDIO", "الصوت"), type.slate)
            Spacer(Modifier.height(10.dp))
            if (panel == PlayerPanel.SUBTITLES) {
                C05Action((if (player.subtitle == null) "● " else "  ") + tr("Off", "إيقاف")) { player.subtitle = null; onClose() }
                MockCatalog.subtitles.forEach { s ->
                    val sel = player.subtitle?.id == s.id
                    C05Action((if (sel) "● " else "  ") + s.language.get() + (s.variant?.let { " " + it.get() } ?: "") + "  " + s.format, requester = if (sel) first else null) { player.subtitle = s; onClose() }
                }
            } else {
                MockCatalog.audioTracks.forEach { a ->
                    val sel = player.audio.id == a.id
                    C05Action((if (sel) "● " else "  ") + a.language.get() + "  ${a.format} ${a.channels}", requester = if (sel) first else null) { player.audio = a; onClose() }
                }
            }
        }
    }
}
