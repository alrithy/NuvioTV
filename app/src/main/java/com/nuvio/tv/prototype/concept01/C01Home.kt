package com.nuvio.tv.prototype.concept01

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.newEpisode
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr
import kotlinx.coroutines.delay

/** A reel is one programmed section. Only one reel is on screen at a time. */
private class Reel(val name: Bi, val items: List<ProtoTitle>, val kind: String)

private val reels = listOf(
    Reel(Bi("Continue", "تابع"), MockCatalog.continueWatching, "continue"),
    Reel(Bi("Tonight's Programme", "برنامج الليلة"), MockCatalog.tonight, "tonight"),
    Reel(Bi("New Episodes", "حلقات جديدة"), MockCatalog.newEpisodes, "new"),
    Reel(Bi("Presented in Dolby Vision", "بتقنية دولبي فيجن"), MockCatalog.dolbyVisionAtmos, "dv"),
    Reel(Bi("Arabic Cinema", "السينما العربية"), MockCatalog.arabicCinema, "arabic"),
    Reel(Bi("Because You Watched Dune", "لأنك شاهدت كثيب"), MockCatalog.becauseDune, "because"),
)

@Composable
internal fun C01Home(session: ProtoSession) {
    val type = c01Type()
    var reelIndex by remember { mutableIntStateOf(session.value("c01.reel", if (session.demoFocus) 1 else 0)) }
    val positions = remember { session.value("c01.positions", HashMap<Int, Int>().apply { if (session.demoFocus) put(1, 2) }) }
    val reel = reels[reelIndex]
    var focused by remember { mutableStateOf(reel.items[positions[reelIndex] ?: 0]) }
    val requesters = remember(reelIndex) { List(reel.items.size) { FocusRequester() } }
    val navRequesters = remember { List(4) { FocusRequester() } }
    val reelEnter = remember { Animatable(1f) }
    var firstEntry by remember { mutableStateOf(true) }

    LaunchedEffect(reelIndex) {
        session.set("c01.reel", reelIndex)
        session.set("c01.positions", positions)
        if (!firstEntry) {
            reelEnter.snapTo(0f)
            reelEnter.animateTo(1f, tween(420, easing = ProtoEasing.Cinematic))
        }
        repeat(6) {
            withFrameNanos { }
            if (runCatching { requesters[(positions[reelIndex] ?: 0).coerceIn(0, reel.items.lastIndex)].requestFocus() }.isSuccess) {
                firstEntry = false
                return@LaunchedEffect
            }
        }
    }

    // Dwell: metadata beyond the title only reveals after the eye settles.
    var dwell by remember(focused.id) { mutableStateOf(false) }
    LaunchedEffect(focused.id) {
        delay(900)
        dwell = true
    }

    Box(Modifier.fillMaxSize().background(C01.Black)) {
        // The picture: a 2.39:1 window between the bars.
        Box(Modifier.fillMaxSize().padding(vertical = C01.Bar).filmGrain(0.06f)) {
            Crossfade(focused, animationSpec = tween(C01.DISSOLVE, easing = ProtoEasing.Cinematic), label = "hero") { t ->
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), alignment = Alignment.Center)
            }
            Box(Modifier.fillMaxSize().background(startScrim(0f to C01.Black.copy(alpha = 0.88f), 0.45f to C01.Black.copy(alpha = 0.35f), 0.75f to C01.Black.copy(alpha = 0f))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to C01.Black.copy(alpha = 0f), 1f to C01.Black.copy(alpha = 0.7f))))

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = C01.Margin, bottom = 26.dp)
                    .width(560.dp),
            ) {
                val overline = when (reel.kind) {
                    "continue" -> (tr("Continue", "تابع") + "  ·  " + (resumeLine(focused) ?: "")).cap()
                    "new" -> focused.newEpisode()?.let { tr("New episode · S${it.season} E${it.number}", "حلقة جديدة · الموسم ${it.season} الحلقة ${it.number}") }?.cap() ?: reel.name.get().cap()
                    else -> reel.name.get().cap()
                }
                TitleSequenceText(overline, type.overline, key = focused.id + reel.kind, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                TitleSequenceText(focused.title.get().cap(), type.display, key = focused.id, delayMs = 90)
                Spacer(Modifier.height(10.dp))
                TitleSequenceText(metaLine(focused) + "  ·  IMDb ${focused.rating}", type.meta, key = focused.id + "m", delayMs = 220, maxLines = 1)
                AnimatedVisibility(dwell, enter = fadeIn(tween(600)), exit = fadeOut(tween(150))) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Txt(focused.synopsis.get(), type.body, maxLines = 2)
                        Spacer(Modifier.height(10.dp))
                        Txt(focused.tech.labels().joinToString("   ").cap(), type.small, maxLines = 1)
                    }
                }
            }
        }

        // Top bar: wordmark and a whisper of navigation.
        C01Bar(C01.Bar, Modifier.align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Txt("NUVIO", type.label.copy(color = C01.Ink70))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    val items = listOf(
                        Bi("Home", "الرئيسية") to null,
                        Bi("Search", "البحث") to ProtoRoute.Search,
                        Bi("Library", "المكتبة") to ProtoRoute.Library,
                        Bi("Profile", "الملف") to ProtoRoute.Profile,
                    )
                    items.forEachIndexed { i, (label, route) ->
                        C01Action(
                            label.get().cap(),
                            requester = navRequesters[i],
                            style = type.small.copy(color = C01.Ink),
                        ) { route?.let { session.nav.push(it) } }
                    }
                }
            }
        }

        // Bottom bar: the reel.
        C01Bar(C01.Bar, Modifier.align(Alignment.BottomCenter)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(196.dp).padding(end = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(tr("REEL ", "الشريط ") + "${reelIndex + 1}".padStart(2, '0') + " / " + "${reels.size}".padStart(2, '0'), type.small)
                        Spacer(Modifier.width(8.dp))
                        ProtoIcon(Glyph.CHEVRON_UP, size = 8.dp, color = C01.Ink25, stroke = 1.4.dp)
                        ProtoIcon(Glyph.CHEVRON_DOWN, size = 8.dp, color = C01.Ink25, stroke = 1.4.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Txt(reel.name.get().cap(), type.small.copy(color = C01.Ink), maxLines = 1)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            alpha = reelEnter.value
                            translationY = (1f - reelEnter.value) * 14.dp.toPx()
                        }
                        .onPreviewKeyEvent { e ->
                            if (!e.isDown) return@onPreviewKeyEvent false
                            when {
                                e.key == Key.DirectionDown && reelIndex < reels.lastIndex -> { reelIndex++; true }
                                e.key == Key.DirectionUp && reelIndex > 0 -> { reelIndex--; true }
                                else -> false
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        Modifier.horizontalScroll(session.scroll("c01.reel.$reelIndex")),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        reel.items.forEachIndexed { i, t ->
                            C01Frame(
                                title = t,
                                width = 98.dp,
                                requester = requesters[i],
                                progress = if (reel.kind == "continue") t.progress else null,
                                onFocus = {
                                    focused = t
                                    positions[reelIndex] = i
                                },
                                onClick = { session.nav.push(ProtoRoute.Details(t.id)) },
                            )
                        }
                        Spacer(Modifier.width(600.dp))
                    }
                }
            }
        }
    }
}
