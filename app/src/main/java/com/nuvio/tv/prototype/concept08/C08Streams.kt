package com.nuvio.tv.prototype.concept08

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.data.Hdr
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoStream
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.Resolution
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/** How long until it starts, said the way a person would say it. */
private fun startWords(s: ProtoStream, lang: ProtoLang): String {
    val sec = s.startSeconds.toInt().coerceAtLeast(1)
    val ar = lang == ProtoLang.AR
    return when {
        !s.isCached -> if (ar) "يحتاج إلى تنزيل أولاً" else "needs to download first"
        sec <= 1 -> if (ar) "يبدأ فوراً" else "starts right away"
        sec < 60 -> if (ar) "جاهز خلال $sec ثوانٍ تقريباً" else "ready in about $sec seconds"
        else -> if (ar) "جاهز خلال ${sec / 60} دقائق تقريباً" else "ready in about ${sec / 60} minutes"
    }
}

/** The whole recommendation in one plain sentence, no codec vocabulary. */
private fun plainSentence(s: ProtoStream, lang: ProtoLang): String {
    val ar = lang == ProtoLang.AR
    val res = when (s.resolution) {
        Resolution.R2160 -> "4K"
        Resolution.R1080 -> if (ar) "دقة عالية" else "HD"
        Resolution.R720 -> if (ar) "دقة عادية" else "standard picture"
    }
    val light = s.hdr.firstOrNull { it != Hdr.SDR }?.label
    val room = if (s.isAtmos) (if (ar) "صوت يملأ الغرفة" else "sound that fills the room") else (if (ar) "صوت ${s.audio.label}" else "${s.audio.label} sound")
    // Arabic opens with an Arabic word so the paragraph resolves right-to-left even though "4K" leads.
    return if (ar) "صورة $res${light?.let { " بتقنية $it" } ?: ""}، مع $room — ${startWords(s, lang)}."
    else "$res${light?.let { " in $it" } ?: ""}, with $room — ${startWords(s, lang)}."
}

private val choices = listOf(StreamMode.BEST, StreamMode.FASTEST, StreamMode.SMALLER, StreamMode.MAX_QUALITY, StreamMode.BALANCED, StreamMode.ALL)

/**
 * Ready to play: one soft card that says what will happen in a sentence. The alternatives are
 * soft choices below it; focusing one quietly re-lights the card. "All sources" swaps the card
 * for the raw list, for people who want to see everything.
 */
@Composable
internal fun C08Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c08Type()
    val lang = LocalProtoLang.current
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    var raw by remember { mutableStateOf(false) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)
    val episode = t.resume?.let { e -> tr(" · S${t.resumeSeason} E${e.number}", " · الموسم ${t.resumeSeason} · الحلقة ${e.number}") } ?: ""

    C08Screen(t.palette, strength = 0.95f) {
        Column(Modifier.fillMaxSize().padding(top = 44.dp, bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(t.title.get() + episode, type.small, maxLines = 1)
            Spacer(Modifier.weight(1f))
            AnimatedContent(mode, transitionSpec = { fadeIn(tween(500, 120, ProtoEasing.Decelerate)) togetherWith fadeOut(tween(200)) }, label = "mode") { m ->
                if (m == StreamMode.ALL) {
                    C08SourceList(StreamIntelligence.rank(streams, m)) { session.nav.push(ProtoRoute.Player(t.id)) }
                } else {
                    val best = remember(m) { StreamIntelligence.best(streams, m) }
                    Column(
                        Modifier.width(640.dp).clip(RoundedCornerShape(40.dp)).background(C08.Soft).padding(horizontal = 40.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Txt(if (m == StreamMode.BEST) tr("Ready when you are", "جاهز متى شئت") else m.blurb.get(), type.small, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                        Txt(plainSentence(best.stream, lang), type.title.copy(textAlign = TextAlign.Center), maxLines = 3)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            best.reasons.take(3).forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    C08Dot(t.palette.ui, 5.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Txt(r.get(), type.small.copy(color = C08.Ink2), maxLines = 1)
                                }
                            }
                        }
                        if (raw) {
                            Spacer(Modifier.height(14.dp))
                            Txt(best.stream.filename, type.small.copy(fontFamily = LocalProtoFonts.current.plexMono, textAlign = TextAlign.Center), maxLines = 2)
                            Txt("${best.stream.addon} · ${best.stream.service} · ${best.stream.sizeLabel} · ${best.stream.bitrateLabel} · ${best.stream.codec}", type.small, maxLines = 1)
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            C08Pill(tr("Play", "تشغيل"), Glyph.PLAY, play, glow = t.palette.ui) { session.nav.push(ProtoRoute.Player(t.id)) }
                            C08Pill(if (raw) tr("Hide details", "إخفاء التفاصيل") else tr("Details", "التفاصيل"), glow = t.palette.ui) { raw = !raw }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Txt(tr("Or choose what matters tonight", "أو اختر ما يهمّك الليلة"), type.small)
            Spacer(Modifier.height(10.dp))
            val modeFocus = rememberTabRowFocus()
            Row(Modifier.tabRow(modeFocus), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { m ->
                    C08Choice(m.label.get(), selected = m == mode, glow = t.palette.ui, modifier = Modifier.tabItem(modeFocus, m == mode), onFocus = { mode = m }) { mode = m }
                }
            }
        }
    }
}

@Composable
private fun C08SourceList(ranked: List<RankedStream>, onPlay: () -> Unit) {
    val type = c08Type()
    val mono = LocalProtoFonts.current.plexMono
    Column(Modifier.width(820.dp).height(330.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranked.forEach { r ->
            var focused by remember { mutableStateOf(false) }
            val s = r.stream
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (focused) C08.Ink.copy(alpha = 0.92f) else C08.Soft)
                    .protoFocusable(onFocusChange = { focused = it }, onClick = onPlay).padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ink = if (focused) C08.Black else C08.Ink
                val ink2 = if (focused) C08.Black.copy(alpha = 0.6f) else C08.Ink3
                Column(Modifier.weight(1f)) {
                    Txt(StreamIntelligence.headline(s), type.label.copy(color = ink), maxLines = 1)
                    Txt(s.filename, type.small.copy(color = ink2, fontFamily = mono), maxLines = 1)
                }
                Spacer(Modifier.width(16.dp))
                Box(Modifier.width(170.dp), contentAlignment = Alignment.CenterEnd) {
                    Txt(
                        (if (s.isCached) tr("Ready", "جاهز") else tr("${s.seeders ?: 0} peers", "${s.seeders ?: 0} مصدر")) + " · ${s.sizeLabel} · ${s.addon}",
                        type.small.copy(color = ink2),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
