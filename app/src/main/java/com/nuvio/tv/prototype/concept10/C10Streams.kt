package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.BlurredArt
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoStream
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.isBack
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.isEnter
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

private const val AUTO_START_MS = 5000f

/** The recommendation as one sentence: why this one, and what happens when you press play. */
private fun reasonSentence(s: ProtoStream, lang: ProtoLang): String {
    val ar = lang == ProtoLang.AR
    val start = if (s.startSeconds < 60) s.startSeconds.toInt().coerceAtLeast(1) else null
    val first = when {
        !s.isCached -> if (ar) "غير مخزّن بعد، لذا يحتاج إلى تنزيل قبل البدء." else "Not cached yet, so it needs to download before it starts."
        start != null && start <= 2 -> if (ar) "مخزّن على ${s.service}، فيبدأ فوراً." else "Cached on ${s.service}, so it starts instantly."
        start != null -> if (ar) "مخزّن على ${s.service}، ويبدأ خلال $start ثوانٍ تقريباً." else "Cached on ${s.service}; it starts in about $start seconds."
        else -> if (ar) "يبدأ خلال دقائق." else "It starts in a few minutes."
    }
    val subs = if (s.hasArabicSubs) (if (ar) " الترجمة العربية متوفرة." else " Arabic subtitles are included.") else ""
    return first + subs
}

/**
 * Stream intelligence as a single decision with an escape hatch. The best match is stated in a
 * sentence and starts on its own after five seconds; the Play button's light bar drains as the
 * countdown. Any navigation stops the clock. Modes and every raw source are one row below.
 */
@Composable
internal fun C10Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c10Type()
    val lang = LocalProtoLang.current
    val frozen = LocalProtoEnv.current.frozen
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    var raw by remember { mutableStateOf(false) }
    var auto by remember { mutableStateOf(true) }
    var remaining by remember { mutableFloatStateOf(if (frozen) 0.6f else 1f) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    LaunchedEffect(auto) {
        if (!auto || frozen) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (remaining > 0f) {
            val now = withFrameNanos { it }
            remaining = (1f - (now - start) / 1_000_000f / AUTO_START_MS).coerceAtLeast(0f)
        }
        session.nav.push(ProtoRoute.Player(t.id))
    }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            if (auto && e.isDown && !e.isEnter && !e.isBack) auto = false
            false
        },
    ) {
        BlurredArt(t, Modifier.fillMaxSize(), radius = 90.dp, intensity = 0.7f)
        Box(Modifier.fillMaxSize().background(C10.Black.copy(alpha = 0.78f)))
        Column(Modifier.fillMaxSize().padding(horizontal = C10.Margin).padding(top = 40.dp, bottom = 24.dp)) {
            val episode = t.resume?.let { e -> tr(" · S${e.season} E${e.number}", " · الموسم ${e.season} الحلقة ${e.number}") } ?: ""
            Txt(over("Playback", "التشغيل") + "  ·  " + t.title.get() + episode, type.overline.copy(color = C10.Ink3), maxLines = 1)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(mode, transitionSpec = { fadeIn(tween(260, 60)) togetherWith fadeOut(tween(120)) }, label = "pick") { m ->
                val ranked = remember(m) { StreamIntelligence.rank(streams, if (m == StreamMode.ALL) StreamMode.BEST else m) }
                val best = ranked.first()
                Row {
                    Column(Modifier.weight(1.25f)) {
                        Txt(if (m == StreamMode.ALL) over("Best match", "الأنسب") else over(m.label.en, m.label.ar), type.overline)
                        Spacer(Modifier.height(4.dp))
                        Txt(StreamIntelligence.headline(best.stream), type.hero.copy(fontSize = 30.sp, lineHeight = if (lang == ProtoLang.AR) 46.sp else 36.sp), maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Txt(reasonSentence(best.stream, lang), type.body, maxLines = 2)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val secs = kotlin.math.ceil(remaining * AUTO_START_MS / 1000f).toInt().coerceAtLeast(1)
                            C10PlayCountdown(if (auto) tr("Play · $secs", "تشغيل · $secs") else tr("Play", "تشغيل"), if (auto) remaining else null, play) { session.nav.push(ProtoRoute.Player(t.id)) }
                            Spacer(Modifier.width(10.dp))
                            C10Button(if (raw) tr("Hide details", "إخفاء التفاصيل") else tr("Raw details", "التفاصيل الخام"), Glyph.LIST) { raw = !raw }
                            Spacer(Modifier.width(14.dp))
                            Txt(if (auto) tr("Starting on its own — move to stay", "سيبدأ تلقائياً — تحرّك للبقاء") else tr("Auto-start paused", "أُوقف البدء التلقائي"), type.caption, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.width(36.dp))
                    C10SpecSheet(best, raw, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreamMode.entries.forEach { m ->
                    C10Tab(m.label.get(), selected = m == mode, modifier = Modifier.padding(end = 18.dp), onFocus = { mode = m })
                }
            }
            Spacer(Modifier.height(12.dp))
            val list = remember(mode) { StreamIntelligence.rank(streams, mode) }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                (if (mode == StreamMode.ALL) list else list.drop(1).take(4)).forEach { r ->
                    C10StreamRow(r, showRaw = mode == StreamMode.ALL) { session.nav.push(ProtoRoute.Player(t.id)) }
                }
            }
        }
    }
}

/** Primary play whose light bar is the countdown: it drains toward the start edge. */
@Composable
private fun C10PlayCountdown(label: String, remaining: Float?, requester: FocusRequester, onClick: () -> Unit) {
    val type = c10Type()
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    Row(
        Modifier
            .graphicsLayer { scaleX = 1f + 0.03f * f; scaleY = 1f + 0.03f * f }
            .drawBehind {
                val y = size.height + 7.dp.toPx()
                val h = 3.dp.toPx()
                if (remaining != null) {
                    val w = size.width * remaining
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
                    drawRoundRect(C10.GoldDim, Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                    drawRoundRect(C10.Gold, Offset(x, y), Size(w, h), CornerRadius(h / 2))
                }
            }
            .then(if (remaining == null) Modifier.lightBar({ f }, 7.dp) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(C10.Ink)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProtoIcon(Glyph.PLAY, size = 15.dp, color = C10.Black, stroke = 1.7.dp)
        Spacer(Modifier.width(9.dp))
        Txt(label, type.label.copy(color = C10.Black, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

/** What you will get, as a spec sheet with hairlines; raw fields fold in underneath. */
@Composable
private fun C10SpecSheet(r: RankedStream, raw: Boolean, modifier: Modifier) {
    val type = c10Type()
    val s = r.stream
    val mono = LocalProtoFonts.current.plexMono
    val rows = listOf(
        tr("Picture", "الصورة") to (if (s.resolution.label == "2160p") "4K" else s.resolution.label) + " · " + s.hdrLabel,
        tr("Sound", "الصوت") to s.audioLabel,
        tr("Subtitles", "الترجمة") to (if (s.hasArabicSubs) tr("Arabic, English", "العربية، الإنجليزية") else tr("English", "الإنجليزية")),
        tr("Source", "المصدر") to "${s.source.label} · ${s.service}",
        tr("Size", "الحجم") to "${s.sizeLabel} · ${s.bitrateLabel}",
    )
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(C10.Ink.copy(alpha = 0.05f)).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(over(r.tier.en, r.tier.ar), type.overline, Modifier.weight(1f))
            Txt(tr("${r.score}% match", "تطابق ${r.score}٪"), type.caption.copy(color = C10.Ink2))
        }
        Spacer(Modifier.height(6.dp))
        rows.forEach { (k, v) ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(C10.Hair))
            Row(Modifier.padding(vertical = 6.dp)) {
                Txt(k, type.caption, Modifier.width(88.dp), maxLines = 1)
                Txt(v, type.label.copy(fontWeight = FontWeight.Normal), maxLines = 1)
            }
        }
        AnimatedVisibility(raw, enter = expandVertically(tween(260)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(120))) {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(C10.Hair))
                Spacer(Modifier.height(6.dp))
                Txt(s.filename, type.caption.copy(fontFamily = mono, fontSize = 10.sp, letterSpacing = 0.sp), maxLines = 2)
                Txt("${s.addon} · ${s.codec} · ${s.seeders?.let { "$it peers" } ?: "direct"}", type.caption.copy(fontFamily = mono, fontSize = 10.sp, letterSpacing = 0.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun C10StreamRow(r: RankedStream, showRaw: Boolean, onClick: () -> Unit) {
    val type = c10Type()
    val lang = LocalProtoLang.current
    var focused by remember { mutableStateOf(false) }
    val f = focusAnim(focused)
    val s = r.stream
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp).sideBar { f }.clip(RoundedCornerShape(8.dp))
            .background(if (focused) C10.Surface2 else C10.Ink.copy(alpha = 0.03f))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(StreamIntelligence.headline(s), type.label, maxLines = 1)
            Txt(if (showRaw) s.filename else r.reasons.take(2).joinToString("  ·  ") { it.of(lang) }, if (showRaw) type.caption.copy(fontFamily = LocalProtoFonts.current.plexMono, fontSize = 10.sp, letterSpacing = 0.sp) else type.caption, maxLines = 1)
        }
        Spacer(Modifier.width(16.dp))
        Txt(listOf(if (s.isCached) s.startLabel else tr("download", "تنزيل"), s.sizeLabel, s.addon).joinToString("  ·  "), type.caption.copy(color = C10.Ink2), maxLines = 1)
    }
}
