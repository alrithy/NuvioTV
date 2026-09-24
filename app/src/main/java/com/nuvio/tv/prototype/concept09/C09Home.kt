package com.nuvio.tv.prototype.concept09

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.ProtoTime
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.newEpisode
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.toArabicDigits
import com.nuvio.tv.prototype.shared.tr

private enum class ShelfKind { TONIGHT, CONTINUE, NEW, ARABIC, TRENDING }

/** Each shelf is named by one Arabic word, set huge. The word is the headline of the home. */
private class C09Shelf(val kind: ShelfKind, val word: String, val name: Bi, val items: List<ProtoTitle>)

private val shelves = listOf(
    C09Shelf(ShelfKind.TONIGHT, "الليلة", Bi("Tonight", "لهذه الليلة"), MockCatalog.tonight),
    C09Shelf(ShelfKind.CONTINUE, "تابع", Bi("Continue watching", "تابع المشاهدة"), MockCatalog.continueWatching),
    C09Shelf(ShelfKind.NEW, "جديد", Bi("New episodes", "حلقات جديدة"), MockCatalog.newEpisodes),
    C09Shelf(ShelfKind.ARABIC, "عربي", Bi("Arabic cinema", "سينما عربية"), MockCatalog.arabicCinema),
    C09Shelf(ShelfKind.TRENDING, "رائج", Bi("Trending in Riyadh", "رائج في الرياض"), MockCatalog.trending),
)

private val CardW = 184.dp
private val CardH = 104.dp
private const val REFLECT = 0.3f

/** One line on why this title is here, specific to its shelf. */
@Composable
private fun why(s: C09Shelf, t: ProtoTitle, index: Int, clock: ProtoTime): String {
    val lang = LocalProtoLang.current
    return when (s.kind) {
        ShelfKind.TONIGHT -> {
            val end = (clock.hour24 * 60 + clock.minute + t.runtimeMin) % (24 * 60)
            val hhmm = "${(end / 60).toString().padStart(2, '0')}:${(end % 60).toString().padStart(2, '0')}"
            if (t.isSeries) tr("An episode ends by $hhmm", "تنتهي الحلقة قبل $hhmm".toArabicDigits())
            else tr("Start now, it ends at $hhmm", "ابدأ الآن، ينتهي عند $hhmm".toArabicDigits())
        }
        ShelfKind.CONTINUE -> resumeLine(t)?.let { if (lang == ProtoLang.AR) it.toArabicDigits() else it } ?: ""
        ShelfKind.NEW -> t.newEpisode()?.let { e -> tr("New: S${e.season} E${e.number} · ${e.title.en}", "جديد: الموسم ${e.season} · الحلقة ${e.number} · ${e.title.ar}".toArabicDigits()) } ?: ""
        ShelfKind.ARABIC -> t.award?.get() ?: t.country.get()
        ShelfKind.TRENDING -> tr("No. ${index + 1} in Riyadh this week", "رقم ${index + 1} في الرياض هذا الأسبوع".toArabicDigits())
    }
}

@Composable
internal fun C09Home(session: ProtoSession) {
    val type = c09Type()
    val clock by rememberProtoClock()
    val rtl = LocalProtoLang.current.isRtl
    var shelf by remember { mutableIntStateOf(session.value("c09.shelf", 0)) }
    val s = shelves[shelf]
    var focusIdx by remember(shelf) { mutableIntStateOf(session.value("c09.idx.$shelf", if (session.demoFocus) 2 else 0).coerceIn(0, s.items.lastIndex)) }
    var lit by remember { mutableStateOf(s.items[focusIdx]) }
    val reqs = remember(shelf) { List(s.items.size) { FocusRequester() } }
    val play = remember { FocusRequester() }

    LaunchedEffect(shelf) {
        session.set("c09.shelf", shelf)
        lit = s.items[focusIdx]
        repeat(8) {
            withFrameNanos { }
            if (runCatching { reqs[focusIdx].requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    C09Stage {
        Row(Modifier.fillMaxSize()) {
            C09NavRail(C09Section.TONIGHT, clock) { session.goTo(it) }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                C09ArtWindow(lit, Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = C09.Margin).width(470.dp).height(268.dp))

                // The word.
                Column(Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 6.dp)) {
                    AnimatedContent(
                        shelf,
                        transitionSpec = {
                            val down = targetState > initialState
                            (fadeIn(tween(420, 80)) + slideInVertically(tween(420, easing = ProtoEasing.Decelerate)) { if (down) it / 3 else -it / 3 }) togetherWith
                                (fadeOut(tween(180)) + slideOutVertically(tween(260)) { if (down) -it / 4 else it / 4 })
                        },
                        label = "word",
                    ) { i -> Txt(shelves[i].word, type.mega, maxLines = 1) }
                }

                // The lit title.
                Column(Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 180.dp).width(430.dp)) {
                    AnimatedContent(lit, transitionSpec = { fadeIn(tween(360, 60)) togetherWith fadeOut(tween(160)) }, label = "lit") { t ->
                        Column {
                            C09Dual(t.title, type.display)
                            Txt(c09Meta(t), type.body, maxLines = 1)
                            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(5.dp).clip(chamfer(1.dp)).background(C09.Teal))
                                Spacer(Modifier.width(8.dp))
                                Txt(why(s, t, s.items.indexOf(t), clock), type.body.copy(color = C09.Pearl), maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        C09Button(if (lit.progress != null) tr("Resume", "استئناف") else tr("Play", "شاهد"), Glyph.PLAY, play, primary = true) { session.nav.push(ProtoRoute.Streams(lit.id)) }
                        C09Button(tr("Details", "التفاصيل"), Glyph.INFO) { session.nav.push(ProtoRoute.Details(lit.id)) }
                        C09Button("", Glyph.PLUS) {}
                    }
                }

                // The curved gallery on its copper horizon.
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(Modifier.padding(start = 44.dp, end = C09.Margin), verticalAlignment = Alignment.CenterVertically) {
                        Txt(tag(s.name.en, s.name.ar), type.tag.copy(color = C09.Pearl2), Modifier.weight(1f), maxLines = 1)
                        Txt(if (rtl) "${shelf + 1} / ${shelves.size}".toArabicDigits() else "${(shelf + 1).toString().padStart(2, '0')} / ${shelves.size.toString().padStart(2, '0')}", type.tag, maxLines = 1)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().drawBehind {
                            val y = CardH.toPx()
                            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, C09.Copper.copy(alpha = 0.6f), Color.Transparent)), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(session.scroll("c09.row.$shelf"))
                                .onPreviewKeyEvent { e ->
                                    if (!e.isDown) return@onPreviewKeyEvent false
                                    when {
                                        e.key == Key.DirectionDown && shelf < shelves.lastIndex -> { shelf++; true }
                                        e.key == Key.DirectionUp && shelf > 0 -> { shelf--; true }
                                        else -> false
                                    }
                                }
                                .padding(start = 44.dp, end = 400.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            s.items.forEachIndexed { i, t ->
                                C09CurvedCard(t, i - focusIdx, rtl, reqs[i], onFocus = {
                                    focusIdx = i
                                    lit = t
                                    session.set("c09.idx.$shelf", i)
                                }) { session.nav.push(ProtoRoute.Details(t.id)) }
                            }
                        }
                    }
                    val next = shelves.getOrNull(shelf + 1)
                    Row(Modifier.padding(start = 44.dp).height(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (next != null) {
                            ProtoIcon(Glyph.CHEVRON_DOWN, size = 10.dp, color = C09.Pearl3, stroke = 1.4.dp)
                            Spacer(Modifier.width(8.dp))
                            Txt(next.word, type.ghost.copy(fontSize = type.ghost.fontSize * 0.85f), maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            if (!rtl) Txt(next.name.en.uppercase(), type.tag, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C09CurvedCard(t: ProtoTitle, distance: Int, rtl: Boolean, requester: FocusRequester, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c09Type()
    var focused by remember { mutableStateOf(false) }
    val trace = rememberTrace(focused)
    val d by animateFloatAsState(distance.toFloat(), tween(380, easing = ProtoEasing.Decelerate), label = "curve")
    Column(Modifier.curve(d, rtl, pivotY = 1f / (1f + REFLECT))) {
        Box(
            Modifier.width(CardW).height(CardH).clip(chamfer())
                .c09Trace({ trace.value })
                .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick),
        ) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC000000))))
            Txt(t.title.get(), type.label.copy(fontFamily = type.title.fontFamily), Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp), maxLines = 1)
            val p = t.progress
            if (p != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(p).height(2.dp).background(C09.Copper))
        }
        C09Reflection(t, ArtKind.BACKDROP, CardW, CardH, REFLECT)
    }
}

/** The art sits in a large chamfered window; its start edge dissolves into obsidian. */
@Composable
internal fun C09ArtWindow(t: ProtoTitle, modifier: Modifier, cut: Dp = 44.dp) {
    Box(modifier.clip(chamfer(cut))) {
        Crossfade(t, animationSpec = tween(520, easing = ProtoEasing.Decelerate), label = "window") { x ->
            ProtoArtwork(x, ArtKind.BACKDROP, Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().background(startScrim(0f to C09.Obsidian, 0.28f to C09.Obsidian.copy(alpha = 0.35f), 0.55f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to C09.Obsidian.copy(alpha = 0.7f))))
    }
}
