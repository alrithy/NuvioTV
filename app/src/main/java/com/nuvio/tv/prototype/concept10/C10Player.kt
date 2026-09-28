package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoLang
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
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.formatTimecode
import com.nuvio.tv.prototype.shared.player.ProtoPlayerState
import com.nuvio.tv.prototype.shared.player.playerWake
import com.nuvio.tv.prototype.shared.player.rememberProtoPlayer
import com.nuvio.tv.prototype.shared.player.scrubKeys
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.tr
import kotlin.math.roundToInt

/** Where the picture is, so glass can blur exactly what sits behind it. */
private class Backdrop(val origin: Offset, val size: IntSize, val content: @Composable () -> Unit)

/**
 * Frosted glass: on devices that can blur, the island re-draws the picture behind itself, offset
 * to line up, and blurs it. Elsewhere it falls back to a dark translucent pane.
 */
@Composable
private fun Glass(backdrop: Backdrop, shape: Shape, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val env = LocalProtoEnv.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { pos = it.positionInRoot() - backdrop.origin }.clip(shape)) {
        if (env.supportsBlur && backdrop.size != IntSize.Zero) {
            Box(
                Modifier.matchParentSize()
                    .layout { m, c ->
                        val p = m.measure(Constraints.fixed(backdrop.size.width, backdrop.size.height))
                        layout(c.maxWidth, c.maxHeight) { p.place(-pos.x.roundToInt(), -pos.y.roundToInt()) }
                    }
                    .blur(30.dp),
            ) { backdrop.content() }
        }
        Box(Modifier.matchParentSize().background(Color(if (env.supportsBlur) 0x8C0E0E10 else 0xE60E0E10)))
        Box(Modifier.matchParentSize().border(1.dp, C10.Hair, shape))
        Column(content = content)
    }
}

@Composable
internal fun C10Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c10Type()
    val frozen = LocalProtoEnv.current.frozen
    val rtl = LocalProtoLang.current.isRtl
    val clock by rememberProtoClock()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(320, easing = ProtoEasing.Decelerate), label = "show")
    val timeline = remember { FocusRequester() }
    RequestFocusOnce(timeline, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    // One push-in value drives the picture and every frosted copy of it, so they stay aligned.
    val kb = if (frozen) 1f else rememberInfiniteTransition(label = "kb").animateFloat(1f, 1.08f, infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Reverse), label = "s").value
    val picture: @Composable () -> Unit = { ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { scaleX = kb; scaleY = kb }) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val backdrop = Backdrop(origin, size, picture)

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player).onGloballyPositioned { origin = it.positionInRoot(); size = it.size }) {
        picture()
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0f to Color(0x99000000), 0.22f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color(0xB3000000))))

        Column(Modifier.align(Alignment.TopStart).padding(start = C10.Margin, top = 32.dp).graphicsLayer { alpha = show }) {
            val ep = t.resume
            Txt(if (ep != null) over("S${ep.season} · E${ep.number}", "الموسم ${ep.season} · الحلقة ${ep.number}") else over("Now playing", "يُعرض الآن"), type.overline)
            Txt(t.title.get(), type.title, maxLines = 1)
            if (ep != null) Txt(ep.title.get(), type.caption.copy(color = C10.Ink2), maxLines = 1)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(end = C10.Margin, top = 32.dp).graphicsLayer { alpha = show }, horizontalAlignment = Alignment.End) {
            val end = (clock.hour24 * 60 + clock.minute + player.remainingSec / 60) % (24 * 60)
            val endLabel = "${(end / 60).toString().padStart(2, '0')}:${(end % 60).toString().padStart(2, '0')}"
            Txt(clock.clock24() + tr("  ·  ends $endLabel", "  ·  ينتهي $endLabel"), type.label.copy(color = C10.Ink2))
            Spacer(Modifier.height(4.dp))
            C10Hud(player)
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp).graphicsLayer { alpha = show }, horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(player.scrubbing, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) { C10Filmstrip(player) }
            Glass(backdrop, RoundedCornerShape(20.dp), Modifier.width(800.dp)) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
                    C10Timeline(player, timeline)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        C10Round(if (player.playing) Glyph.PAUSE else Glyph.PLAY) { player.togglePlay() }
                        Spacer(Modifier.width(8.dp))
                        C10Round(Glyph.REPLAY10) { player.seekBy(-10f) }
                        Spacer(Modifier.width(8.dp))
                        C10Round(Glyph.FORWARD10) { player.seekBy(10f) }
                        Spacer(Modifier.weight(1f))
                        C10Button(tr("Subtitles", "الترجمة"), Glyph.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                        Spacer(Modifier.width(8.dp))
                        C10Button(tr("Audio", "الصوت"), Glyph.AUDIO) { panel = PlayerPanel.AUDIO }
                        Spacer(Modifier.width(8.dp))
                        C10Button(tr("Source", "المصدر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                        if (t.isSeries) {
                            Spacer(Modifier.width(8.dp))
                            C10Button(tr("Next episode", "الحلقة التالية"), Glyph.NEXT) {}
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(320, easing = ProtoEasing.Decelerate)) { if (rtl) -it else it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(240, easing = ProtoEasing.Exit)) { if (rtl) -it else it } + fadeOut(tween(180)),
        ) {
            C10TrackSheet(player, panel, backdrop) { panel = PlayerPanel.NONE }
        }
    }
}

/** Playback Quality HUD: one quiet line, a gold dot when the best match is playing. */
@Composable
private fun C10Hud(player: ProtoPlayerState) {
    val type = c10Type()
    val s = player.stream
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(C10.Gold))
        Spacer(Modifier.width(7.dp))
        Txt(
            listOfNotNull(
                tr("Best match", "الأنسب"),
                if (s.resolution.label == "2160p") "4K" else s.resolution.label,
                s.hdr.firstOrNull { it.label != "SDR" }?.label,
                s.audioLabel,
                if (s.isCached) tr("Cached", "مخزّن") else null,
                player.subtitle?.language?.get(),
            ).joinToString("  ·  "),
            type.caption.copy(color = C10.Ink2), maxLines = 1,
        )
    }
}

@Composable
private fun C10Timeline(player: ProtoPlayerState, requester: FocusRequester) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    val lang = LocalProtoLang.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(formatTimecode((if (player.scrubbing) player.scrubPosition else player.position).toInt()), type.label, Modifier.width(62.dp))
            BoxWithConstraints(
                Modifier.weight(1f).height(20.dp).scrubKeys(player).protoFocusable(requester, onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val w = maxWidth
                val h = 4.dp + 2.dp * f
                Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF)))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(h).clip(RoundedCornerShape(3.dp)).background(C10.Ink))
                player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction).width(2.dp).height(h).background(Color(0x99000000))) }
                // The playhead is the light bar turned upright.
                Box(Modifier.offset(x = w * player.displayFraction - 1.5.dp).width(3.dp).height(10.dp + 8.dp * f).clip(RoundedCornerShape(2.dp)).background(C10.Gold))
            }
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalProtoLang provides lang) {
                val ch = player.chapters.getOrNull(player.currentChapter)
                // LRM anchors the line left-to-right so an Arabic chapter name can't move the minus sign.
                Txt("\u200E-" + formatTimecode(player.remainingSec) + (ch?.let { "  ·  " + it.title.get() } ?: ""), type.caption.copy(color = C10.Ink2, textAlign = TextAlign.End), Modifier.width(170.dp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun C10Filmstrip(player: ProtoPlayerState) {
    val type = c10Type()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                for (k in -2..2) {
                    val pos = (player.scrubPosition + k * 60f).coerceIn(0f, player.durationSec.toFloat())
                    val center = k == 0
                    Box(Modifier.width(if (center) 196.dp else 128.dp).then(if (center) Modifier.lightBar({ 1f }, 7.dp) else Modifier)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).graphicsLayer { alpha = if (center) 1f else 0.5f }) {
                            ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (pos / 60f).toInt() + 1, remote = false)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Txt(formatTimecode(player.scrubPosition.toInt()), type.label)
        }
    }
}

@Composable
private fun C10Round(glyph: Glyph, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Box(
        Modifier.lightBar({ f }, 6.dp).size(40.dp).graphicsLayer { scaleX = 1f + 0.06f * f; scaleY = 1f + 0.06f * f }
            .clip(CircleShape).background(if (focused) C10.Ink else Color(0x1FFFFFFF))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ProtoIcon(glyph, size = 17.dp, color = if (focused) C10.Black else C10.Ink, stroke = 1.7.dp) }
}

private fun sample(code: String): String = when (code.lowercase()) {
    "ar" -> "لم نأتِ إلى هنا لنختبئ."
    "fr" -> "Nous ne sommes pas venus ici pour nous cacher."
    "es" -> "No vinimos aquí para escondernos."
    "tr" -> "Buraya saklanmaya gelmedik."
    else -> "We didn't come here to hide."
}

@Composable
private fun C10TrackSheet(player: ProtoPlayerState, panel: PlayerPanel, backdrop: Backdrop, onClose: () -> Unit) {
    val type = c10Type()
    val lang = LocalProtoLang.current
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    val subs = panel == PlayerPanel.SUBTITLES
    Glass(backdrop, RoundedCornerShape(18.dp), Modifier.fillMaxHeight().padding(vertical = 22.dp).padding(end = 22.dp).width(360.dp)) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 18.dp, vertical = 18.dp)) {
            Txt(if (subs) tr("Subtitles", "الترجمة") else tr("Audio", "الصوت"), type.title)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (subs) {
                    C10TrackRow(tr("Off", "بدون"), "", player.subtitle == null, if (player.subtitle == null) first else null) { player.subtitle = null; onClose() }
                    MockCatalog.subtitles.forEach { s ->
                        val sel = player.subtitle?.id == s.id
                        C10TrackRow(s.language.of(lang) + (s.variant?.let { " · " + it.of(lang) } ?: ""), "${s.format} · ${s.source}", sel, if (sel) first else null) { player.subtitle = s; onClose() }
                    }
                } else {
                    MockCatalog.audioTracks.forEach { a ->
                        val sel = player.audio.id == a.id
                        C10TrackRow(a.language.of(lang) + (a.note?.let { " · " + it.of(lang) } ?: ""), "${a.format} ${a.channels}", sel, if (sel) first else null) { player.audio = a; onClose() }
                    }
                }
            }
            if (subs) {
                Spacer(Modifier.height(10.dp))
                // Preview on the actual frame, set the way it will appear.
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
                    ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), remote = false)
                    val code = player.subtitle?.code
                    if (code != null) Txt(
                        sample(code),
                        type.label.copy(color = Color.White, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)),
                        Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 10.dp), maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun C10TrackRow(label: String, detail: String, selected: Boolean, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 3.dp).sideBar { f }.clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0x26FFFFFF) else Color.Transparent)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(18.dp)) { if (selected) ProtoIcon(Glyph.CHECK, size = 13.dp, color = C10.Gold, stroke = 1.8.dp) }
        Spacer(Modifier.width(6.dp))
        Txt(label, type.label.copy(color = if (selected || focused) C10.Ink else C10.Ink2), Modifier.weight(1f), maxLines = 1)
        if (detail.isNotEmpty()) Txt(detail, type.caption, maxLines = 1)
    }
}
