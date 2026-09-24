package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.kenBurns
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.isEnter
import com.nuvio.tv.prototype.shared.isUp
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.tr

internal class C05Channel(val name: Bi, val items: List<ProtoTitle>)

internal val c05Channels = listOf(
    C05Channel(Bi("Continue", "تابع"), MockCatalog.continueWatching),
    C05Channel(Bi("Tonight", "الليلة"), MockCatalog.tonight),
    C05Channel(Bi("New episodes", "حلقات جديدة"), MockCatalog.newEpisodes),
    C05Channel(Bi("Arabic cinema", "سينما عربية"), MockCatalog.arabicCinema),
    C05Channel(Bi("Trending", "الأكثر رواجاً"), MockCatalog.trending),
)

private val systemItems = listOf(
    Triple(Bi("Search", "البحث"), Glyph.SEARCH, ProtoRoute.Search),
    Triple(Bi("Library", "المكتبة"), Glyph.LIBRARY, ProtoRoute.Library),
    Triple(Bi("Profile", "الملف"), Glyph.PROFILE, ProtoRoute.Profile),
)

/** Cursor across channel (row) and title (column). Channel −1 is the hidden system line. */
private data class Cursor(val channel: Int, val index: Int)

@Composable
internal fun C05Home(session: ProtoSession) {
    val type = c05Type()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var cursor by remember { mutableStateOf(session.value("c05.cursor", if (session.demoFocus) Cursor(1, 2) else Cursor(0, 0))) }
    var lastMove by remember { mutableIntStateOf(0) } // 1 next, -1 prev, 2 down, -2 up
    var systemIndex by remember { mutableIntStateOf(0) }
    var ring by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf(false) }
    val idle = remember { C05Idle() }
    val ui = rememberIdleVisibility(idle)
    val root = remember { FocusRequester() }
    RequestFocusOnce(root, key = map to ring, enabled = !map && !ring)
    ProtoBackHandler(enabled = ring) { ring = false }
    ProtoBackHandler(enabled = !ring && !map) { map = true }

    val channel = c05Channels[cursor.channel.coerceAtLeast(0)]
    val title = channel.items[cursor.index.coerceIn(0, channel.items.lastIndex)]
    androidx.compose.runtime.SideEffect { session.set("c05.cursor", cursor) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            map,
            transitionSpec = {
                if (targetState) (fadeIn(tween(420)) + scaleIn(tween(520), initialScale = 1.15f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(520), targetScale = 0.35f))
                else (fadeIn(tween(420)) + scaleIn(tween(520), initialScale = 0.35f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(520), targetScale = 1.15f))
            },
            label = "zoom",
        ) { zoomedOut ->
            if (zoomedOut) {
                C05Map(cursor.channel.coerceAtLeast(0), cursor.index) { c, i ->
                    cursor = Cursor(c, i)
                    map = false
                }
            } else Box(
                Modifier
                    .fillMaxSize()
                    .protoFocusable(root, onKey = { e ->
                        if (ring) return@protoFocusable false
                        if (e.isEnter) {
                            if (e.isUp) {
                                idle.poke()
                                if (cursor.channel < 0) session.nav.push(systemItems[systemIndex].third) else ring = true
                            }
                            return@protoFocusable true
                        }
                        if (!e.isDown) return@protoFocusable false
                        idle.poke()
                        val next = if (rtl) Key.DirectionLeft else Key.DirectionRight
                        val prev = if (rtl) Key.DirectionRight else Key.DirectionLeft
                        when (e.key) {
                            next -> {
                                if (cursor.channel < 0) systemIndex = (systemIndex + 1).coerceAtMost(systemItems.lastIndex)
                                else if (cursor.index < channel.items.lastIndex) { lastMove = 1; cursor = cursor.copy(index = cursor.index + 1) }
                                true
                            }
                            prev -> {
                                if (cursor.channel < 0) systemIndex = (systemIndex - 1).coerceAtLeast(0)
                                else if (cursor.index > 0) { lastMove = -1; cursor = cursor.copy(index = cursor.index - 1) }
                                true
                            }
                            Key.DirectionDown -> {
                                if (cursor.channel < c05Channels.lastIndex) { lastMove = 2; cursor = Cursor(cursor.channel + 1, 0) }
                                true
                            }
                            Key.DirectionUp -> {
                                if (cursor.channel >= 0) { lastMove = -2; cursor = Cursor(cursor.channel - 1, 0) }
                                true
                            }
                            else -> false
                        }
                    }),
            ) {
                AnimatedContent(
                    title,
                    transitionSpec = {
                        val d = lastMove
                        when (d) {
                            1, -1 -> (slideInHorizontally(tween(620)) { if (d == 1) it / 3 else -it / 3 } + fadeIn(tween(520))) togetherWith (slideOutHorizontally(tween(620)) { if (d == 1) -it / 3 else it / 3 } + fadeOut(tween(420)))
                            else -> (slideInVertically(tween(620)) { if (d == 2) it / 4 else -it / 4 } + fadeIn(tween(520))) togetherWith (slideOutVertically(tween(620)) { if (d == 2) -it / 4 else it / 4 } + fadeOut(tween(420)))
                        }
                    },
                    label = "title",
                ) { t -> ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().kenBurns(40_000, 1.06f)) }

                // The only scrim: a low gradient under the words, and only while words are visible.
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = ui }.background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xB3000000))))

                if (cursor.channel < 0) {
                    // The system line: navigation exists only once you look for it.
                    Row(
                        Modifier.align(Alignment.TopCenter).padding(top = 40.dp).graphicsLayer { alpha = 0.4f + 0.6f * ui },
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                    ) {
                        systemItems.forEachIndexed { i, (label, g, _) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (i == systemIndex) Box(Modifier.width(6.dp).height(6.dp).background(C05.Signal))
                                Spacer(Modifier.width(8.dp))
                                com.nuvio.tv.prototype.shared.ProtoIcon(g, size = 13.dp, color = if (i == systemIndex) C05.Ink else C05.Ink3, stroke = 1.5.dp)
                                Spacer(Modifier.width(8.dp))
                                Txt(label.get().slate(), type.action.copy(color = if (i == systemIndex) C05.Ink else C05.Ink3))
                            }
                        }
                    }
                } else {
                    val above = c05Channels.getOrNull(cursor.channel - 1)?.name?.get() ?: tr("Search · Library · Profile", "البحث · المكتبة · الملف")
                    C05Hint(Glyph.CHEVRON_UP, above, Modifier.align(Alignment.TopCenter).padding(top = 24.dp).graphicsLayer { alpha = ui })
                }
                c05Channels.getOrNull(cursor.channel + 1)?.let {
                    C05Hint(Glyph.CHEVRON_DOWN, it.name.get(), Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).graphicsLayer { alpha = ui * 0.8f })
                }

                // Slate: channel, position, title, one line of truth.
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = C05.Margin, bottom = 48.dp, end = C05.Margin).graphicsLayer { alpha = ui },
                ) {
                    if (cursor.channel >= 0) {
                        Txt(
                            "CH ${(cursor.channel + 1).toString().padStart(2, '0')}  ${channel.name.get().slate()}    ${(cursor.index + 1).toString().padStart(2, '0')}/${channel.items.size.toString().padStart(2, '0')}",
                            type.slate,
                        )
                        Spacer(Modifier.height(10.dp))
                        Txt(title.title.get(), type.big, maxLines = 1)
                        Spacer(Modifier.height(6.dp))
                        Txt((resumeLine(title)?.let { "$it  ·  " } ?: "") + metaLine(title, "  /  ") + "  /  IMDb ${title.rating}", type.slate)
                        Spacer(Modifier.height(14.dp))
                        // Position ticks, like story segments.
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            channel.items.indices.forEach { i ->
                                Box(Modifier.width(if (i == cursor.index) 28.dp else 12.dp).height(2.dp).background(if (i == cursor.index) C05.Ink else C05.Ink3))
                            }
                        }
                    }
                }
                // Onboarding legend: it only teaches the first move, then gets out of the way.
                val legend by animateFloatAsState(if (lastMove == 0 && cursor.channel >= 0) 1f else 0f, tween(400), label = "legend")
                Txt(tr("OK  ACTIONS    ←→  TITLES    ↑↓  CHANNELS    BACK  MAP", "موافق  الإجراءات    ←→  العناوين    ↑↓  القنوات    رجوع  الخريطة"), type.slateSmall, Modifier.align(Alignment.TopEnd).padding(end = C05.Margin, top = 28.dp).graphicsLayer { alpha = ui * 0.8f * legend })

                AnimatedVisibility(ring, modifier = Modifier.align(Alignment.BottomStart), enter = fadeIn(tween(260)), exit = fadeOut(tween(200))) {
                    C05ActionLine(title, session) { ring = false }
                }
            }
        }
    }
}

/** Contextual actions appear as one line of words, exactly where the title was. */
@Composable
private fun C05ActionLine(t: ProtoTitle, session: ProtoSession, onClose: () -> Unit) {
    val type = c05Type()
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))).padding(horizontal = C05.Margin, vertical = 44.dp)) {
        Column {
            Txt(t.title.get(), type.title, maxLines = 1)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                C05Action(if (t.progress != null) tr("Resume", "استئناف") else tr("Play", "تشغيل"), Glyph.PLAY, first) { onClose(); session.nav.push(ProtoRoute.Streams(t.id)) }
                C05Action(tr("About", "عن العمل"), Glyph.INFO) { onClose(); session.nav.push(ProtoRoute.Details(t.id)) }
                if (t.isSeries) C05Action(tr("Episodes", "الحلقات"), Glyph.EPISODES) { onClose(); session.nav.push(ProtoRoute.Episodes(t.id, t.resumeSeason ?: 1)) }
                C05Action(tr("Sources", "المصادر"), Glyph.SOURCES) { onClose(); session.nav.push(ProtoRoute.Streams(t.id)) }
                C05Action(tr("Trailer", "الإعلان"), Glyph.TRAILER) {}
                C05Action(tr("Keep", "احفظ"), Glyph.PLUS) {}
            }
        }
    }
}

/** Zoomed out: every channel as a strip of frames. The only "overview" in the product. */
@Composable
private fun C05Map(channel: Int, index: Int, onPick: (Int, Int) -> Unit) {
    val type = c05Type()
    val current = remember { FocusRequester() }
    RequestFocusOnce(current)
    Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(horizontal = C05.Margin, vertical = 40.dp)) {
        Txt(tr("MAP", "الخريطة"), type.slate)
        Spacer(Modifier.height(16.dp))
        c05Channels.forEachIndexed { c, ch ->
            Txt("CH ${(c + 1).toString().padStart(2, '0')}  ${ch.name.get().slate()}", type.slateSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ch.items.forEachIndexed { i, t ->
                    var focused by remember { mutableStateOf(false) }
                    val f by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "m")
                    Box(
                        Modifier
                            .width(128.dp)
                            .aspectRatio(16f / 9f)
                            .graphicsLayer { alpha = 0.4f + 0.6f * f }
                            .border(1.dp, if (focused) C05.Ink else Color.Transparent)
                            .protoFocusable(if (c == channel && i == index) current else null, onFocusChange = { focused = it }, onClick = { onPick(c, i) }),
                    ) {
                        ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
