package com.nuvio.tv.prototype.concept07

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.RankedStream
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.StreamMode
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/** The recommendation written as a short paragraph, the way an editor would explain a pick. */
private fun prose(r: RankedStream, lang: ProtoLang): String {
    val s = r.stream
    val hdr = s.hdr.firstOrNull { it.label != "SDR" }?.label
    return if (lang == ProtoLang.AR) {
        "نسخة ${s.source.label} بدقة ${s.resolution.label}${hdr?.let { " مع $it" } ?: ""}، وصوت ${s.audioLabel}. " +
            (if (s.isCached) "مخزّنة مسبقاً على ${s.service}، فتبدأ خلال ${s.startLabel}. " else "تحتاج إلى تنزيل قبل البدء. ") +
            (if (s.hasArabicSubs) "والترجمة العربية متوفرة." else "")
    } else {
        "The ${s.resolution.label} ${s.source.label}${hdr?.let { " in $it" } ?: ""}, with ${s.audioLabel} sound. " +
            (if (s.isCached) "It is already cached on ${s.service}, so it begins in ${s.startLabel}. " else "It needs to download before it can begin. ") +
            (if (s.hasArabicSubs) "Arabic subtitles are included." else "")
    }
}

@Composable
internal fun C07Streams(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c07Type()
    val lang = LocalProtoLang.current
    val streams = remember(t.id) { StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode) }
    var mode by remember { mutableStateOf(StreamMode.BEST) }
    val play = remember { FocusRequester() }
    RequestFocusOnce(play)

    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).verticalScroll(rememberScrollState()).padding(horizontal = C07.Margin, vertical = 30.dp)) {
        Txt(tr("HOW TO WATCH", "كيف تشاهد") + "  ·  " + t.title.get().kick(), type.kicker)
        Spacer(Modifier.height(8.dp))
        val modeFocus = rememberTabRowFocus()
        Row(Modifier.tabRow(modeFocus), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StreamMode.entries.forEach { m -> C07Link(m.label.get() + if (m == mode) " ●" else "", modifier = Modifier.tabItem(modeFocus, m == mode), onFocus = { mode = m }) { mode = m } }
        }
        Spacer(Modifier.height(10.dp))
        C07Rule(color = C07.Ink.copy(alpha = 0.6f))
        Spacer(Modifier.height(18.dp))
        AnimatedContent(mode, transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(140)) }, label = "m") { m ->
            val ranked = remember(m) { StreamIntelligence.rank(streams, m) }
            val best = ranked.first()
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    Column(Modifier.weight(1.3f)) {
                        Txt(if (m == StreamMode.BEST) tr("EDITOR'S CHOICE", "اختيار المحرر") else m.label.get().kick(), type.kicker)
                        Spacer(Modifier.height(6.dp))
                        Txt(StreamIntelligence.headline(best.stream), type.headlineM, maxLines = 2)
                        Spacer(Modifier.height(10.dp))
                        Txt(prose(best, lang), type.deck, maxLines = 4)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            C07Link(tr("Begin watching", "ابدأ المشاهدة"), play) { session.nav.push(ProtoRoute.Player(t.id)) }
                            Txt(tr("${best.score}% match · ${best.stream.sizeLabel}", "تطابق ${best.score}% · ${best.stream.sizeLabel}"), type.caption, Modifier.padding(top = 6.dp))
                        }
                    }
                    Column(Modifier.weight(1f).border(1.dp, C07.Rule).padding(14.dp)) {
                        Txt(tr("THE FINE PRINT", "التفاصيل الدقيقة"), type.kicker)
                        Spacer(Modifier.height(6.dp))
                        Txt(best.stream.filename, type.caption.copy(fontFamily = LocalProtoFonts.current.plexMono), maxLines = 3)
                        Spacer(Modifier.height(6.dp))
                        Txt("${best.stream.addon} · ${best.stream.bitrateLabel} · ${best.stream.codec}", type.caption)
                    }
                }
                Spacer(Modifier.height(26.dp))
                Txt(if (m == StreamMode.ALL) tr("EVERY LISTING", "كل الإعلانات") else tr("CLASSIFIEDS", "إعلانات مبوبة"), type.kicker)
                Spacer(Modifier.height(6.dp))
                C07Rule()
                Spacer(Modifier.height(12.dp))
                (if (m == StreamMode.ALL) ranked else ranked.drop(1).take(6)).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                        row.forEach { r -> C07Classified(r, Modifier.weight(1f), raw = m == StreamMode.ALL) { session.nav.push(ProtoRoute.Player(t.id)) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

/** A source as a classified ad: boxed, headline in caps, terse copy. */
@Composable
private fun C07Classified(r: RankedStream, modifier: Modifier, raw: Boolean, onClick: () -> Unit) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    val s = r.stream
    Column(
        modifier.redRule(f).border(1.dp, if (focused) C07.Ink else C07.Rule)
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick).padding(12.dp),
    ) {
        Txt(StreamIntelligence.headline(s).kick(), type.kicker.copy(color = if (focused) C07.Red else C07.Ink), maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Txt(
            (if (s.isCached) tr("Cached, ready now. ", "مخزّن وجاهز الآن. ") else tr("Downloads first. ", "يُنزَّل أولاً. ")) +
                "${s.source.label}, ${s.sizeLabel}. ${s.addon}.",
            type.body,
            maxLines = 2,
        )
        if (raw) Txt(s.filename, type.caption.copy(fontFamily = LocalProtoFonts.current.plexMono), maxLines = 1)
    }
}
