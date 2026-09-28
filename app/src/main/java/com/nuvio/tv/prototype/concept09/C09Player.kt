package com.nuvio.tv.prototype.concept09

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.PlayerPanel
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.kenBurns
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.formatTimecode
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoPlayerState
import com.nuvio.tv.prototype.shared.player.playerWake
import com.nuvio.tv.prototype.shared.player.rememberProtoPlayer
import com.nuvio.tv.prototype.shared.player.scrubKeys
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

/** Timecodes follow the interface numerals: Arabic-Indic in Arabic. */
@Composable
private fun tc(sec: Int): String = num(formatTimecode(sec))

@Composable
internal fun C09Player(session: ProtoSession, titleId: String, initialPanel: PlayerPanel) {
    val t = MockCatalog.title(titleId)
    val type = c09Type()
    val ar = isArabic()
    val player = rememberProtoPlayer(t)
    var panel by remember { mutableStateOf(initialPanel) }
    val show by animateFloatAsState(if (player.controlsVisible || panel != PlayerPanel.NONE) 1f else 0f, tween(380, easing = ProtoEasing.Decelerate), label = "show")
    val timeline = remember { FocusRequester() }
    RequestFocusOnce(timeline, enabled = panel == PlayerPanel.NONE)
    ProtoBackHandler(enabled = panel != PlayerPanel.NONE) { panel = PlayerPanel.NONE }
    ProtoBackHandler(enabled = panel == PlayerPanel.NONE && player.scrubbing) { player.cancelScrub() }

    Box(Modifier.fillMaxSize().background(Color.Black).playerWake(player)) {
        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns())
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = show }.background(Brush.verticalGradient(0f to Color(0xB3000000), 0.25f to Color.Transparent, 0.55f to Color.Transparent, 1f to C09.Obsidian.copy(alpha = 0.96f))))

        // Title, dual script.
        Column(Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 30.dp).graphicsLayer { alpha = show }) {
            Txt(tag("Now playing", "يُعرض الآن"), type.tag.copy(color = C09.Copper))
            Txt(t.title.get() + (t.resume?.let { " · " + it.title.get() } ?: ""), type.title, maxLines = 1)
            C09OtherTitle(t, 14f)
        }
        // Quality HUD: chamfered chips, teal only for the "ready" signal.
        Row(Modifier.align(Alignment.TopEnd).padding(end = C09.Margin, top = 34.dp).graphicsLayer { alpha = if (panel == PlayerPanel.NONE) show else 0f }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val s = player.stream
            C09Chip(if (s.resolution.label == "2160p") "4K" else s.resolution.label)
            s.hdr.firstOrNull { it.label != "SDR" }?.let { C09Chip(if (ar) it.label else it.label.uppercase()) }
            C09Chip(if (ar) s.audioLabel else s.audioLabel.uppercase())
            if (s.isCached) C09Chip(tag("Cached · ${s.service}", "مخزّن · ${s.service}"), C09.Teal)
            player.subtitle?.let { C09Chip(tag("${it.language.en} subs", "ترجمة ${it.language.ar}"), C09.Pearl2) }
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 44.dp, vertical = 26.dp).graphicsLayer { alpha = show }) {
            AnimatedVisibility(player.scrubbing, enter = fadeIn(tween(240)), exit = fadeOut(tween(180))) { C09Filmstrip(player) }
            val ch = player.chapters.getOrNull(player.currentChapter)
            if (ch != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt(tag("Chapter ${player.currentChapter + 1}", "الفصل ${num(player.currentChapter + 1)}"), type.tag.copy(color = C09.Copper))
                    Spacer(Modifier.width(10.dp))
                    Txt(ch.title.get(), type.label.copy(fontFamily = type.title.fontFamily), maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
            }
            C09Timeline(player, timeline)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C09Button(if (player.playing) tr("Pause", "إيقاف") else tr("Play", "تشغيل"), if (player.playing) Glyph.PAUSE else Glyph.PLAY, primary = true) { player.togglePlay() }
                C09Button(tr("10 s", "١٠ ث"), Glyph.REPLAY10) { player.seekBy(-10f) }
                C09Button(tr("Subtitles", "الترجمة"), Glyph.SUBTITLES) { panel = PlayerPanel.SUBTITLES }
                C09Button(tr("Audio", "الصوت"), Glyph.AUDIO) { panel = PlayerPanel.AUDIO }
                C09Button(tr("Sources", "المصادر"), Glyph.SOURCES) { session.nav.push(ProtoRoute.Streams(t.id)) }
                if (t.isSeries) C09Button(tr("Next", "التالية"), Glyph.NEXT) {}
            }
        }

        AnimatedVisibility(
            panel != PlayerPanel.NONE,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(360, easing = ProtoEasing.Decelerate)) { if (ar) -it else it } + fadeIn(tween(240)),
            exit = slideOutHorizontally(tween(260, easing = ProtoEasing.Exit)) { if (ar) -it else it } + fadeOut(tween(200)),
        ) {
            C09TrackPanel(player, panel) { panel = PlayerPanel.NONE }
        }
    }
}

@Composable
private fun C09Timeline(player: ProtoPlayerState, requester: FocusRequester) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(260), label = "tl")
    val lang = LocalProtoLang.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalProtoLang provides lang) {
                Txt(tc((if (player.scrubbing) player.scrubPosition else player.position).toInt()), type.label, Modifier.width(70.dp))
            }
            BoxWithConstraints(
                Modifier.weight(1f).height(20.dp).scrubKeys(player).protoFocusable(requester, onFocusChange = { focused = it; if (!it) player.cancelScrub() }),
                contentAlignment = Alignment.CenterStart,
            ) {
                val w = maxWidth
                val h = 3.dp + 2.dp * f
                Box(Modifier.fillMaxWidth().height(h).background(C09.Line))
                Box(Modifier.fillMaxWidth(player.displayFraction).height(h).background(Brush.horizontalGradient(listOf(C09.Copper, C09.CopperHi))))
                player.chapters.drop(1).forEach { c -> Box(Modifier.offset(x = w * c.startFraction - 1.dp).width(2.dp).height(10.dp).background(C09.Obsidian)) }
                // Thumb: a small copper diamond, the chamfer taken to its limit.
                Box(Modifier.offset(x = w * player.displayFraction - 7.dp).size(14.dp).graphicsLayer { scaleX = 0.7f + 0.3f * f; scaleY = 0.7f + 0.3f * f }.rotate(45f).background(if (focused) C09.Pearl else C09.CopperHi))
            }
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalProtoLang provides lang) {
                Txt("-" + tc(player.remainingSec), type.label.copy(color = C09.Pearl2, textAlign = TextAlign.End), Modifier.width(80.dp))
            }
        }
    }
}

/** Scrub preview on the same curve as the galleries: the frame under the thumb faces you. */
@Composable
private fun C09Filmstrip(player: ProtoPlayerState) {
    val type = c09Type()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                for (k in -3..3) {
                    val pos = (player.scrubPosition + k * 45f).coerceIn(0f, player.durationSec.toFloat())
                    Box(Modifier.curve(k.toFloat(), rtl = false, angle = 16f).width(150.dp).height(84.dp).clip(chamfer(10.dp)).c09Trace({ if (k == 0) 1f else 0f }, 10.dp)) {
                        ProtoArtwork(player.title, ArtKind.BACKDROP, Modifier.fillMaxSize(), variant = (pos / 60f).toInt() + 1, remote = false)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Txt(num(formatTimecode(player.scrubPosition.toInt())), type.label.copy(color = C09.CopperHi))
        }
    }
}

/** Sample line so the choice can be judged before closing the panel. */
private fun sampleLine(code: String): String = when (code.lowercase()) {
    "ar" -> "لا أحد يعرف الصحراء كما يعرفها أهلها."
    "fr" -> "Personne ne connaît le désert comme ceux qui y vivent."
    "es" -> "Nadie conoce el desierto como quienes viven en él."
    "tr" -> "Çölü kimse orada yaşayanlar kadar tanımaz."
    else -> "No one knows the desert like the people who live in it."
}

@Composable
private fun C09TrackPanel(player: ProtoPlayerState, panel: PlayerPanel, onClose: () -> Unit) {
    val type = c09Type()
    val ar = isArabic()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first, key = panel)
    val subs = panel == PlayerPanel.SUBTITLES
    Column(
        Modifier.fillMaxHeight().width(360.dp).padding(vertical = 24.dp).clip(CutCornerLeading)
            .background(C09.Obsidian.copy(alpha = 0.98f)).padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Txt(tag(if (subs) "Subtitles" else "Audio", if (subs) "الترجمة" else "الصوت"), type.tag.copy(color = C09.Copper))
        Txt(if (subs) tr("Subtitles", "الترجمة") else tr("Sound", "الصوت"), type.display.copy(fontSize = 28.sp, lineHeight = if (ar) 42.sp else 34.sp))
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (subs) {
                C09TrackRow(tr("Off", "بدون ترجمة"), "", "", player.subtitle == null, if (player.subtitle == null) first else null) { player.subtitle = null; onClose() }
                MockCatalog.subtitles.forEach { s ->
                    val sel = player.subtitle?.id == s.id
                    C09TrackRow(s.language.get() + (s.variant?.let { " · " + it.get() } ?: ""), if (ar) s.language.en else s.language.ar, s.source, sel, if (sel) first else null) { player.subtitle = s; onClose() }
                }
            } else {
                MockCatalog.audioTracks.forEach { a ->
                    val sel = player.audio.id == a.id
                    C09TrackRow(a.language.get() + (a.note?.let { " · " + it.get() } ?: ""), if (ar) a.language.en else a.language.ar, "${a.format} ${a.channels}", sel, if (sel) first else null) { player.audio = a; onClose() }
                }
            }
        }
        if (subs) {
            Spacer(Modifier.height(10.dp))
            Txt(tag("Preview", "معاينة"), type.tag)
            Spacer(Modifier.height(4.dp))
            val code = player.subtitle?.code ?: "en"
            Box(Modifier.fillMaxWidth().clip(chamfer(8.dp)).background(Color(0xFF26262C)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Txt(if (player.subtitle == null) "—" else sampleLine(code), type.label.copy(fontFamily = if (code.equals("ar", ignoreCase = true)) LocalProtoFonts.current.readex else type.label.fontFamily, textAlign = TextAlign.Center), maxLines = 2)
            }
        }
    }
}

/** Only the reading-start corners are cut on the panel, so it reads as a blade entering. */
private val CutCornerLeading = androidx.compose.foundation.shape.CutCornerShape(topStart = 28.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 28.dp)

@Composable
private fun C09TrackRow(label: String, other: String, detail: String, selected: Boolean, requester: FocusRequester?, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(chamfer(8.dp))
            .background(if (focused) C09.Obsidian3 else Color.Transparent)
            .c09Trace({ trace.value }, 8.dp, base = Color.Transparent, bloom = false)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(24.dp).background(if (selected) C09.Copper else Color.Transparent))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Txt(label, type.label.copy(color = if (selected || focused) C09.Pearl else C09.Pearl2), maxLines = 1)
            // The other script: Arabic is never tracked, so it takes the ghost face rather than the Latin tag.
            if (other.isNotEmpty()) Txt(other, if (isArabic()) type.tag.copy(fontFamily = LocalProtoFonts.current.sora, fontSize = 8.sp) else type.ghost.copy(fontSize = 11.sp, lineHeight = 16.sp), maxLines = 1)
        }
        if (detail.isNotEmpty()) Txt(num(detail), type.tag, maxLines = 1)
    }
}
