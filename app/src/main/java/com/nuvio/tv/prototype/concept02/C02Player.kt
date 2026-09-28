package com.nuvio.tv.prototype.concept02

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
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
import com.nuvio.tv.prototype.shared.ts

@Composable
internal fun C02Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c02Type()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(420, easing = ProtoEasing.Decelerate), label = "chrome")
    val playReq = remember { FocusRequester() }
    RequestFocusOnce(playReq, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0f to C02.Night.copy(alpha = 0.7f), 0.25f to Color.Transparent, 0.6f to Color.Transparent, 1f to C02.Night.copy(alpha = 0.92f))))

        // Top: bilingual title and the quality HUD.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = C02.Margin, vertical = 30.dp).graphicsLayer { alpha = show; translationY = (1f - show) * -16.dp.toPx() },
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Txt(t.title.get(), type.title.copy(fontSize = type.title.fontSize * 1.25f))
                Txt(t.title.other(), type.eyebrowSecond)
                val ch = player.chapters.getOrNull(player.currentChapter)
                if (ch != null) {
                    Spacer(Modifier.height(6.dp))
                    Txt(tr("Chapter ${player.currentChapter + 1} · ", "الفصل ${player.currentChapter + 1} · ") + ch.title.get(), type.meta)
                }
            }
            C02Hud(player)
        }

        // Bottom: timeline + controls.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = C02.Margin, vertical = 28.dp).graphicsLayer { alpha = show; translationY = (1f - show) * 20.dp.toPx() },
        ) {
            C02Timeline(player)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    C02Round(Glyph.REPLAY10) { player.seekBy(-10f) }
                    C02Round(if (player.playing) Glyph.PAUSE else Glyph.PLAY, big = true, requester = playReq) { player.togglePlay() }
                    C02Round(Glyph.FORWARD10) { player.seekBy(10f) }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    C02GhostButton(tr("Subtitles", "الترجمة"), Glyph.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                    C02GhostButton(tr("Audio", "الصوت"), Glyph.AUDIO) { panel = PlayerPanel.AUDIO }
                    C02GhostButton(tr("Source", "المصدر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    if (t.isSeries) C02GhostButton(tr("Next episode", "الحلقة التالية"), Glyph.NEXT) {}
                }
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(460, easing = ProtoEasing.Decelerate)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(180)),
        ) {
            C02TrackSheet(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

/** Playback quality HUD: a compact glass pill, emerald when everything is ready. */
@Composable
private fun C02Hud(player: ProtoPlayerState) {
    val type = c02Type()
    val s = player.stream
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(C02.Night.copy(alpha = 0.72f))
            .border(1.dp, C02.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(C02.EmeraldLight))
            Spacer(Modifier.width(8.dp))
            Txt(tr("Playback ready", "جاهز للتشغيل"), type.label)
            Spacer(Modifier.width(8.dp))
            Txt(tr("جاهز للتشغيل", "Playback ready"), type.captionSecond)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            C02Badge(s.resolution.label.uppercase())
            s.hdr.filter { it.label != "SDR" }.forEach { C02Badge(it.label.uppercase()) }
            C02Badge(s.audio.short.uppercase() + " " + s.channels)
        }
        Spacer(Modifier.height(6.dp))
        Txt(
            listOfNotNull(
                if (s.isCached) tr("${s.service} cached", "مخزّن على ${s.service}") else null,
                player.subtitle?.let { tr("${it.language.en} subtitles", "ترجمة ${it.language.ar}") },
                s.bitrateLabel,
            ).joinToString("  ·  "),
            type.captionSecond.copy(color = C02.Ink2),
        )
    }
}

@Composable
private fun C02Timeline(player: ProtoPlayerState) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column {
            BoxWithConstraints(Modifier.fillMaxWidth().height(if (player.scrubbing) 96.dp else 0.dp)) {
                if (player.scrubbing) {
                    val x = maxWidth * player.scrubFraction
                    Column(
                        Modifier.offset(x = (x - 70.dp).coerceIn(0.dp, maxWidth - 140.dp)).width(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)).border(1.5.dp, C02.GoldLight, RoundedCornerShape(6.dp))) {
                            ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (player.scrubPosition / 60f).toInt() + 1, remote = false)
                        }
                        Txt(formatTimecode(player.scrubPosition.toInt()), type.caption, align = TextAlign.Center)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(formatTimecode(player.position.toInt()), type.meta.copy(color = C02.Ink), Modifier.width(62.dp))
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .scrubKeys(player)
                        .protoFocusable(onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val w = maxWidth
                    val h = if (focused) 4.dp else 2.dp
                    Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(2.dp)).background(Color(0x40FFFFFF)))
                    Box(
                        Modifier.fillMaxWidth(player.displayFraction).height(h).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(C02.Gold, C02.GoldLight)))
                            .drawBehind {
                                drawCircle(Brush.radialGradient(listOf(C02.Gold.copy(alpha = 0.5f), Color.Transparent), center = Offset(size.width, size.height / 2), radius = 30f), radius = 30f, center = Offset(size.width, size.height / 2))
                            },
                    )
                    player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(2.dp).height(h).background(C02.Night)) }
                    val knob = if (focused) 14.dp else 8.dp
                    Box(Modifier.offset(x = w * player.displayFraction - knob / 2).size(knob).clip(CircleShape).background(C02.GoldLight))
                }
                Txt("−" + formatTimecode(player.remainingSec), type.meta.copy(color = C02.Ink2), Modifier.padding(start = 12.dp).width(62.dp))
            }
        }
    }
}

@Composable
private fun C02Round(glyph: Glyph, big: Boolean = false, requester: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = c02FocusAnim(focused)
    val size = if (big) 50.dp else 40.dp
    Box(
        Modifier
            .size(size)
            .c02Focus(f)
            .clip(CircleShape)
            .background(if (focused) C02.Gold else C02.Night.copy(alpha = 0.6f))
            .border(1.dp, if (focused) Color.Transparent else C02.Line, CircleShape)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProtoIcon(glyph, size = if (big) 18.dp else 15.dp, color = if (focused) C02.Night else C02.Ink, stroke = 1.6.dp)
    }
}

@Composable
private fun C02TrackSheet(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c02Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    Row(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Brush.verticalGradient(listOf(C02.Night.copy(alpha = 0.9f), C02.Night)))
            .drawBehind { drawLine(C02.GoldDim, Offset(0f, 0f), Offset(size.width, 0f), 1.5f) }
            .padding(horizontal = C02.Margin, vertical = 22.dp),
    ) {
        Column(Modifier.weight(1f)) {
            C02Heading(if (panel == PlayerPanel.SUBTITLES) Bi("Subtitles", "الترجمة") else Bi("Audio", "الصوت"))
            Spacer(Modifier.height(12.dp))
            if (panel == PlayerPanel.SUBTITLES) {
                val items = listOf(null) + MockCatalog.subtitles
                items.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { s ->
                            val selected = player.subtitle?.id == s?.id
                            C02TrackChip(
                                s?.language ?: Bi("Off", "إيقاف"),
                                s?.let { listOfNotNull(it.variant?.get(), it.format).joinToString(" · ") },
                                selected,
                                if (selected) first else null,
                            ) { player.subtitle = s; onClose() }
                        }
                    }
                }
            } else {
                MockCatalog.audioTracks.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { a ->
                            val selected = player.audio.id == a.id
                            C02TrackChip(a.language, "${a.format} ${a.channels}" + (a.note?.let { " · " + it.get() } ?: ""), selected, if (selected) first else null) { player.audio = a; onClose() }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(24.dp))
        // Live preview of how subtitles will look — in both scripts.
        Column(
            Modifier.width(260.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1410)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            C02Heading(Bi("Preview", "معاينة"))
            Spacer(Modifier.height(20.dp))
            Txt("الصحراء لا تنسى من عبرها.", ts(LocalProtoFonts.current.readex, 15.sp, color = C02.GoldLight, lineHeight = 24.sp), align = TextAlign.Center)
            Txt("The desert remembers those who cross it.", ts(LocalProtoFonts.current.sora, 12.sp, color = C02.Ink), align = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Txt(tr("Warm gold · medium · soft shadow", "ذهبي دافئ · متوسط · ظل ناعم"), type.captionSecond, align = TextAlign.Center)
        }
    }
}

@Composable
private fun C02TrackChip(label: Bi, detail: String?, selected: Boolean, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(C02.Radius))
            .background(if (focused) C02.Ink else if (selected) C02.Raised else C02.Surface)
            .border(1.dp, if (selected && !focused) C02.Gold else Color.Transparent, RoundedCornerShape(C02.Radius))
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(label.get(), type.label.copy(color = if (focused) C02.Night else C02.Ink), maxLines = 1)
            Txt(detail ?: label.other(), type.captionSecond.copy(color = if (focused) C02.Night.copy(alpha = 0.6f) else C02.Ink3), maxLines = 1)
        }
        if (selected) ProtoIcon(Glyph.CHECK, size = 12.dp, color = if (focused) C02.Night else C02.GoldLight, stroke = 1.8.dp)
    }
}
