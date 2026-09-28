package com.nuvio.tv.prototype.concept01

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.formatTimecode
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoPlayerState
import com.nuvio.tv.prototype.shared.player.playerWake
import com.nuvio.tv.prototype.shared.player.rememberProtoPlayer
import com.nuvio.tv.prototype.shared.player.scrubKeys
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/**
 * The letterbox is the player chrome: pressing any key slides the black bars in, carrying the
 * title, the quality line and the timeline. When they leave, only the picture remains.
 */
@Composable
internal fun C01Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c01Type()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val chrome by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(520, easing = ProtoEasing.Cinematic), label = "bars")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(C01.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())

        // Filmstrip preview while scrubbing.
        AnimatedVisibility(
            player.scrubbing,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = C01.Bar + 18.dp),
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(200)),
        ) {
            C01Filmstrip(player)
        }

        // Top bar
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(C01.Bar * chrome).background(C01.Black).clipToBounds()) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = C01.Margin).graphicsLayer { alpha = chrome },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(t.title.get().cap(), type.label, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    val ch = player.chapters.getOrNull(player.currentChapter)
                    if (ch != null) Txt((tr("Chapter ${player.currentChapter + 1}", "الفصل ${player.currentChapter + 1}") + "  ·  " + ch.title.get()).cap(), type.small, maxLines = 1)
                }
                // Playback quality line — confidence without chrome.
                val s = player.stream
                Column(horizontalAlignment = Alignment.End) {
                    Txt(tr("PLAYBACK READY", "جاهز للتشغيل"), type.small.copy(color = C01.Ink))
                    Spacer(Modifier.height(4.dp))
                    Txt(
                        listOfNotNull(
                            s.resolution.label, s.hdr.firstOrNull()?.label, s.audioLabel,
                            if (s.isCached) tr("Cached", "مخزّن") else null,
                            player.subtitle?.language?.get()?.let { tr("$it subs", "ترجمة $it") },
                            s.bitrateLabel,
                        ).joinToString("  ·  ").cap(),
                        type.small, maxLines = 1,
                    )
                }
            }
        }

        // Bottom bar
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(C01.Bar * chrome).background(C01.Black).clipToBounds()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = C01.Margin, vertical = 10.dp).graphicsLayer { alpha = chrome },
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                C01Timeline(player)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    C01Glyph(Glyph.REPLAY10, tr("Back 10", "رجوع ١٠")) { player.seekBy(-10f) }
                    Spacer(Modifier.width(26.dp))
                    C01Glyph(if (player.playing) Glyph.PAUSE else Glyph.PLAY, if (player.playing) tr("Pause", "إيقاف") else tr("Play", "تشغيل"), requester = playReq) { player.togglePlay() }
                    Spacer(Modifier.width(26.dp))
                    C01Glyph(Glyph.FORWARD10, tr("Forward 10", "تقدم ١٠")) { player.seekBy(10f) }
                    Spacer(Modifier.width(56.dp))
                    C01Glyph(Glyph.SUBTITLES, tr("Subtitles", "الترجمة")) { panel = PlayerPanel.SUBTITLES }
                    Spacer(Modifier.width(26.dp))
                    C01Glyph(Glyph.AUDIO, tr("Audio", "الصوت")) { panel = PlayerPanel.AUDIO }
                    Spacer(Modifier.width(26.dp))
                    C01Glyph(Glyph.SOURCES, tr("Presentation", "العرض")) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    if (t.isSeries) {
                        Spacer(Modifier.width(26.dp))
                        C01Glyph(Glyph.NEXT, tr("Next episode", "الحلقة التالية")) {}
                    }
                }
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(420, easing = ProtoEasing.Cinematic)) { it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(260)) { it } + fadeOut(tween(200)),
        ) {
            C01TrackPanel(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C01Timeline(player: ProtoPlayerState) {
    val type = c01Type()
    var focused by remember { mutableStateOf(false) }
    // Media timelines run left-to-right in Arabic too, matching how picture time is read.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(formatTimecode(player.displayFraction.times(player.durationSec).toInt()), type.small.copy(color = C01.Ink70), Modifier.width(64.dp))
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .height(12.dp)
                    .scrubKeys(player)
                    .protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val w = maxWidth
                Box(Modifier.fillMaxWidth().height(1.dp).background(C01.Ink25))
                Box(Modifier.fillMaxWidth(player.fraction).height(1.dp).background(C01.Ink70))
                player.chapters.drop(1).forEach { c ->
                    Box(Modifier.offset(x = w * c.startFraction).width(1.dp).height(5.dp).background(C01.Ink45))
                }
                val head = if (focused) 9.dp else 5.dp
                Box(Modifier.offset(x = w * player.displayFraction - head / 2).size(head).clip(CircleShape).background(C01.Ink))
            }
            Txt("−" + formatTimecode(((1f - player.displayFraction) * player.durationSec).toInt()), type.small.copy(color = C01.Ink70), Modifier.padding(start = 14.dp).width(64.dp))
        }
    }
}

@Composable
private fun C01Filmstrip(player: ProtoPlayerState) {
    val type = c01Type()
    val step = 90f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            for (k in -2..2) {
                val pos = (player.scrubPosition + k * step).coerceIn(0f, player.durationSec.toFloat())
                val center = k == 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(if (center) 176.dp else 120.dp).aspectRatio(C01.SCOPE).graphicsLayer { alpha = if (center) 1f else 0.45f }) {
                        ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (pos / 60f).toInt() + 1, remote = false)
                    }
                    if (center) {
                        Spacer(Modifier.height(6.dp))
                        Txt(formatTimecode(pos.toInt(), forceHours = true), type.small.copy(color = C01.Ink))
                        val ch = player.chapters.getOrNull(player.chapterAt(pos / player.durationSec))
                        if (ch != null) Txt(ch.title.get().cap(), type.small, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Player control: the focused glyph opens sideways to reveal its name; neighbours make room. */
@Composable
private fun C01Glyph(glyph: Glyph, label: String, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c01Type()
    var focused by remember { mutableStateOf(false) }
    val light by animateFloatAsState(if (focused) 1f else 0f, tween(C01.FOCUS), label = "g")
    Row(
        Modifier.protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProtoIcon(glyph, size = 18.dp, color = C01.Ink.copy(alpha = 0.45f + 0.55f * light), stroke = 1.4.dp)
        AnimatedVisibility(focused, enter = fadeIn(tween(240)) + expandHorizontally(tween(260)), exit = fadeOut(tween(120)) + shrinkHorizontally(tween(200))) {
            Txt(label.cap(), type.small.copy(color = C01.Ink), Modifier.padding(start = 10.dp), maxLines = 1)
        }
    }
}

@Composable
private fun C01TrackPanel(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c01Type()
    val ar = isArabic()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Column(
        Modifier
            .fillMaxHeight()
            .width(340.dp)
            .background(C01.Black.copy(alpha = 0.97f))
            .padding(horizontal = 36.dp, vertical = 64.dp),
    ) {
        if (panel == PlayerPanel.SUBTITLES) {
            Txt(tr("SUBTITLES", "الترجمة"), type.overline)
            Spacer(Modifier.height(22.dp))
            C01TrackRow(tr("Off", "إيقاف"), null, player.subtitle == null) { player.subtitle = null; onClose() }
            MockCatalog.subtitles.forEachIndexed { i, s ->
                val label = s.language.get() + (s.variant?.let { "  ·  " + it.get() } ?: "")
                C01TrackRow(label, "${s.source} · ${s.format}", player.subtitle?.id == s.id, if (player.subtitle?.id == s.id || (player.subtitle == null && i == 0)) first else null) {
                    player.subtitle = s
                    onClose()
                }
            }
            Spacer(Modifier.height(20.dp))
            Txt(tr("STYLE  ·  CINEMA WHITE  ·  MEDIUM  ·  DELAY 0.0S", "النمط · أبيض سينمائي · متوسط · تأخير 0.0 ث"), type.small)
        } else {
            Txt(tr("AUDIO", "الصوت"), type.overline)
            Spacer(Modifier.height(22.dp))
            MockCatalog.audioTracks.forEach { a ->
                C01TrackRow(
                    a.language.get() + (a.note?.let { "  ·  " + it.get() } ?: ""),
                    "${a.format} ${a.channels}",
                    player.audio.id == a.id,
                    if (player.audio.id == a.id) first else null,
                ) { player.audio = a; onClose() }
            }
            Spacer(Modifier.height(20.dp))
            val s = player.stream
            Txt((tr("Source  ·  ", "المصدر  ·  ") + StreamIntelligence.headline(s)).let { if (ar) it else it.uppercase() }, type.small, maxLines = 2)
        }
    }
}

@Composable
private fun C01TrackRow(label: String, detail: String?, selected: Boolean, requester: FocusRequester? = null, onClick: () -> Unit) {
    val type = c01Type()
    var focused by remember { mutableStateOf(false) }
    val light by animateFloatAsState(if (focused) 1f else 0f, tween(C01.FOCUS), label = "r")
    Row(
        Modifier
            .fillMaxWidth()
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp)) {
            if (selected) Box(Modifier.size(5.dp).clip(CircleShape).background(C01.Ink))
        }
        Column {
            Txt(label.cap(), type.label.copy(color = C01.Ink.copy(alpha = 0.5f + 0.5f * light)), maxLines = 1)
            if (detail != null) Txt(detail.cap(), type.small, maxLines = 1)
        }
    }
}
