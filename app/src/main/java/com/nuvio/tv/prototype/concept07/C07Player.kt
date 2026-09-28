package com.nuvio.tv.prototype.concept07

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
import com.nuvio.tv.prototype.shared.tr

private fun roman(n: Int): String {
    val v = intArrayOf(10, 9, 5, 4, 1)
    val s = arrayOf("X", "IX", "V", "IV", "I")
    var x = n
    val b = StringBuilder()
    for (i in v.indices) while (x >= v[i]) { b.append(s[i]); x -= v[i] }
    return b.toString()
}

/** The player captions the film like a photograph: chapter title in italic, a single rule. */
@Composable
internal fun C07Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c07Type()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(420), label = "cap")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0.62f to Color.Transparent, 1f to C07.Paper.copy(alpha = 0.95f))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = C07.Margin, vertical = 30.dp).graphicsLayer { alpha = show }) {
            val ch = player.chapters.getOrNull(player.currentChapter)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Txt(t.title.get().kick(), type.kicker)
                    Txt(tr("Chapter ${roman(player.currentChapter + 1)} — ", "الفصل ${player.currentChapter + 1} — ") + (ch?.title?.get() ?: ""), type.quote, maxLines = 1)
                }
                val s = player.stream
                Txt("${s.resolution.label} · ${s.hdr.first().label} · ${s.audioLabel}" + (if (s.isCached) tr(" · cached", " · مخزّن") else ""), type.caption.copy(color = C07.Ink2))
            }
            Spacer(Modifier.height(12.dp))
            C07Timeline(player)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                C07Link(if (player.playing) tr("Pause", "إيقاف مؤقت") else tr("Play", "تشغيل"), playReq) { player.togglePlay() }
                C07Link(tr("Back ten", "رجوع عشر")) { player.seekBy(-10f) }
                C07Link(tr("Subtitles", "الترجمة")) { panel = PlayerPanel.SUBTITLES }
                C07Link(tr("Audio", "الصوت")) { panel = PlayerPanel.AUDIO }
                C07Link(tr("Source", "المصدر")) { session.nav.push(ProtoRoute.Streams(t.id)) }
            }
        }
        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(420)) { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)),
        ) {
            C07Sidebar(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C07Timeline(player: ProtoPlayerState) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column {
            BoxWithConstraints(Modifier.fillMaxWidth().height(if (player.scrubbing) 92.dp else 0.dp)) {
                if (player.scrubbing) {
                    Column(Modifier.offset(x = (maxWidth * player.scrubFraction - 70.dp).coerceIn(0.dp, maxWidth - 140.dp)).width(140.dp)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false) }
                        Txt(formatTimecode(player.scrubPosition.toInt()), type.caption.copy(color = C07.Ink))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(formatTimecode(player.position.toInt()), type.nav, Modifier.width(64.dp))
                BoxWithConstraints(
                    Modifier.weight(1f).height(14.dp).scrubKeys(player).protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val w = maxWidth
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C07.Rule))
                    Box(Modifier.fillMaxWidth(player.displayFraction).height(if (focused) 3.dp else 1.dp).background(C07.Ink))
                    player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(1.dp).height(8.dp).background(C07.Ink3)) }
                    Box(Modifier.offset(x = w * player.displayFraction - 1.dp).width(3.dp).height(14.dp).background(C07.Red))
                }
                Txt(formatTimecode(player.durationSec), type.nav, Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun C07Sidebar(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c07Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Column(Modifier.fillMaxHeight().width(330.dp).background(C07.Paper).padding(horizontal = 28.dp, vertical = 36.dp)) {
        Txt(if (panel == PlayerPanel.SUBTITLES) tr("SUBTITLES", "الترجمة") else tr("AUDIO", "الصوت"), type.kicker)
        Spacer(Modifier.height(6.dp))
        C07Rule(color = C07.Ink.copy(alpha = 0.6f))
        val rows = if (panel == PlayerPanel.SUBTITLES) {
            listOf(Triple(tr("None", "بلا ترجمة"), "", player.subtitle == null)) + MockCatalog.subtitles.map { s -> Triple(s.language.get() + (s.variant?.let { ", " + it.get() } ?: ""), "${s.format} · ${s.source}", player.subtitle?.id == s.id) }
        } else {
            MockCatalog.audioTracks.map { a -> Triple(a.language.get() + (a.note?.let { ", " + it.get() } ?: ""), "${a.format} ${a.channels}", player.audio.id == a.id) }
        }
        rows.forEachIndexed { i, (label, detail, sel) ->
            var focused by remember { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().protoFocusable(if (sel) first else null, onFocusChange = { focused = it }, onClick = {
                    if (panel == PlayerPanel.SUBTITLES) player.subtitle = if (i == 0) null else MockCatalog.subtitles[i - 1] else player.audio = MockCatalog.audioTracks[i]
                    onClose()
                }).padding(vertical = 6.dp),
            ) {
                Txt((if (sel) "● " else "") + label, type.headlineS.copy(color = if (focused) C07.Red else if (sel) C07.Ink else C07.Ink2), maxLines = 1)
                if (detail.isNotEmpty()) Txt(detail, type.caption, maxLines = 1)
            }
        }
    }
}
