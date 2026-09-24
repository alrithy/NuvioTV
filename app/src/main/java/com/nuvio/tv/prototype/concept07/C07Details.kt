package com.nuvio.tv.prototype.concept07

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberTabRowFocus
import com.nuvio.tv.prototype.shared.tabItem
import com.nuvio.tv.prototype.shared.tabRow
import com.nuvio.tv.prototype.shared.tr

/** Details as a feature article: kicker, headline, deck, lead photograph, drop cap, pull quote. */
@Composable
internal fun C07Details(session: ProtoSession, titleId: String) {
    val t = MockCatalog.title(titleId)
    val type = c07Type()
    val lang = LocalProtoLang.current
    val watch = remember { FocusRequester() }
    RequestFocusOnce(watch)

    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).verticalScroll(rememberScrollState()).padding(horizontal = C07.Margin)) {
        Spacer(Modifier.height(34.dp))
        Txt((tr("Feature", "مقال") + "  ·  " + t.genres.joinToString("  ·  ") { it.of(lang) }).kick(), type.kicker)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(t.title.get(), type.headline, Modifier.weight(1f), maxLines = 2)
            Column(horizontalAlignment = Alignment.End) {
                Txt(t.award?.get() ?: "IMDb ${t.rating}", type.caption.copy(color = C07.Ink2))
                Txt("${t.year} · ${if (t.isSeries) tr("${t.totalEpisodes} episodes", "${t.totalEpisodes} حلقة") else formatRuntime(t.runtimeMin, lang)} · ${t.cert}", type.caption)
            }
        }
        Spacer(Modifier.height(6.dp))
        Txt(t.tagline.get(), type.deck, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        C07Rule()
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(tr("By ", "بقلم ") + t.director.get(), type.nav)
            Spacer(Modifier.weight(1f))
            C07Link(if (t.progress != null) tr("Continue watching", "أكمل المشاهدة") else tr("Watch now", "شاهد الآن"), watch) { session.nav.push(ProtoRoute.Streams(t.id)) }
            if (t.isSeries) C07Link(tr("Contents", "المحتويات")) { session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
            C07Link(tr("Choose a source", "اختر مصدراً")) { session.nav.push(ProtoRoute.Streams(t.id)) }
            C07Link(tr("Trailer", "الإعلان")) {}
            C07Link(tr("Save for later", "احفظ لاحقاً")) {}
        }
        Spacer(Modifier.height(18.dp))
        C07Image(t, ArtKind.BACKDROP, Modifier.fillMaxWidth().aspectRatio(2.6f)) { session.nav.push(ProtoRoute.Streams(t.id)) }
        Spacer(Modifier.height(6.dp))
        Txt(tr("Lead image: ", "الصورة الرئيسية: ") + t.title.get() + " (${t.year}). " + t.tech.labels().joinToString(" · "), type.caption)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            // Body with a drop cap (Latin) or a vermilion rule (Arabic, whose letters must stay joined).
            Row(Modifier.weight(1.3f)) {
                if (!type.arabic) {
                    val s = t.synopsis.en
                    Txt(s.take(1), type.headline.copy(fontSize = type.headline.fontSize * 1.35f, lineHeight = type.headline.lineHeight * 1.1f, color = C07.Red), Modifier.padding(end = 10.dp))
                    Txt(s.drop(1), type.body.copy(color = C07.Ink, fontSize = type.body.fontSize * 1.15f, lineHeight = type.body.lineHeight * 1.15f))
                } else {
                    Box(Modifier.width(3.dp).height(80.dp).background(C07.Red))
                    Spacer(Modifier.width(12.dp))
                    Txt(t.synopsis.ar, type.body.copy(color = C07.Ink, fontSize = type.body.fontSize * 1.15f, lineHeight = type.body.lineHeight * 1.15f))
                }
            }
            // In brief: the sidebar box.
            Column(Modifier.weight(1f).border(1.dp, C07.Rule).padding(16.dp)) {
                Txt(tr("IN BRIEF", "باختصار"), type.kicker)
                Spacer(Modifier.height(8.dp))
                C07Fact(tr("Directed by", "إخراج"), t.director.get())
                C07Fact(tr("Starring", "بطولة"), t.cast.take(2).joinToString(tr(", ", "، ")) { it.name.of(lang) })
                C07Fact(tr("Country", "البلد"), t.country.get())
                C07Fact(tr("Presented in", "العرض"), t.tech.labels().take(3).joinToString(" · ").ifEmpty { "HD" })
            }
        }
        t.quote?.let { q ->
            Spacer(Modifier.height(30.dp))
            C07Rule(color = C07.Red)
            Spacer(Modifier.height(14.dp))
            Txt("“${q.get()}”", type.quote.copy(fontSize = type.quote.fontSize * 1.2f, lineHeight = type.quote.lineHeight * 1.2f))
            Txt("— ${t.quoteSource}", type.caption)
        }
        Spacer(Modifier.height(30.dp))
        Txt(tr("CAST", "طاقم التمثيل"), type.kicker)
        Spacer(Modifier.height(8.dp))
        C07Rule()
        t.cast.chunked(3).forEach { row ->
            Row(Modifier.padding(vertical = 8.dp)) {
                row.forEach { c ->
                    Column(Modifier.weight(1f)) {
                        Txt(c.name.get(), type.headlineS, maxLines = 1)
                        Txt(c.role.get(), type.caption, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun C07Fact(k: String, v: String) {
    val type = c07Type()
    Column(Modifier.padding(vertical = 4.dp)) {
        Txt(k.kick(), type.caption)
        Txt(v, type.headlineS.copy(fontSize = type.headlineS.fontSize * 0.8f), maxLines = 1)
    }
}

/** Episodes as a table of contents with the focused chapter's plate beside it. */
@Composable
internal fun C07Episodes(session: ProtoSession, titleId: String, initialSeason: Int) {
    val t = MockCatalog.title(titleId)
    val type = c07Type()
    var seasonIdx by remember { mutableIntStateOf(t.seasons.indexOfFirst { it.number == initialSeason }.coerceAtLeast(0)) }
    val season = t.seasons[seasonIdx]
    val start = if (season.number == t.resumeSeason) (t.resumeEpisode ?: 1) - 1 else 0
    var focusedEp by remember(seasonIdx) { mutableIntStateOf(start) }
    val reqs = remember(seasonIdx) { List(season.episodes.size) { FocusRequester() } }
    RequestFocusOnce(reqs[start.coerceIn(0, reqs.lastIndex)], key = seasonIdx)
    val ep = season.episodes[focusedEp.coerceIn(0, season.episodes.lastIndex)]

    Row(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).padding(horizontal = C07.Margin, vertical = 34.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Txt(tr("CONTENTS", "المحتويات"), type.kicker)
            Txt(t.title.get(), type.headlineM, maxLines = 1)
            val seasonFocus = rememberTabRowFocus()
            Row(Modifier.tabRow(seasonFocus), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                t.seasons.forEachIndexed { i, s ->
                    C07Link(tr("Season ${s.number}", "الموسم ${s.number}") + if (i == seasonIdx) " ●" else "", modifier = Modifier.tabItem(seasonFocus, i == seasonIdx), onFocus = { seasonIdx = i }) { seasonIdx = i }
                }
            }
            Spacer(Modifier.height(8.dp))
            C07Rule(color = C07.Ink.copy(alpha = 0.6f))
            Column(Modifier.verticalScroll(session.scroll("c07.eps.$seasonIdx"))) {
                season.episodes.forEachIndexed { i, e ->
                    var focused by remember(seasonIdx, i) { mutableStateOf(false) }
                    Column(Modifier.protoFocusable(reqs[i], onFocusChange = { focused = it; if (it) focusedEp = i }, onClick = { session.nav.push(ProtoRoute.Streams(t.id)) })) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Txt(e.number.toString(), type.headlineS.copy(color = if (focused) C07.Red else C07.Ink3), Modifier.width(36.dp))
                            Txt(e.title.get(), type.headlineS.copy(color = if (focused) C07.Ink else C07.Ink2), Modifier.weight(1f), maxLines = 1)
                            Txt(if (e.isNew) tr("new", "جديدة") else if (e.watched) tr("read", "مقروءة") else "", type.caption.copy(color = C07.Red))
                            Spacer(Modifier.width(10.dp))
                            Txt("${e.runtimeMin}", type.headlineS.copy(color = C07.Ink3))
                        }
                        C07Rule(color = if (focused) C07.Red else C07.Rule)
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(40.dp))
            Crossfade(ep, animationSpec = tween(420), label = "plate") { e ->
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) { ProtoArtwork(t, ArtKind.STILL, Modifier.fillMaxSize(), episode = e) }
                    Spacer(Modifier.height(6.dp))
                    Txt(tr("Plate ${e.number}. ", "لوحة ${e.number}. ") + e.title.get(), type.caption)
                    Spacer(Modifier.height(12.dp))
                    Txt(e.synopsis.get(), type.deck, maxLines = 3)
                }
            }
        }
    }
}
