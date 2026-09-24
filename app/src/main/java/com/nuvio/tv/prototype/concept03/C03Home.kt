package com.nuvio.tv.prototype.concept03

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr

private class Hall(val name: Bi, val items: List<ProtoTitle>)

private val halls = listOf(
    Hall(Bi("Continue", "تابع"), MockCatalog.continueWatching),
    Hall(Bi("Tonight", "الليلة"), MockCatalog.tonight),
    Hall(Bi("New", "جديد"), MockCatalog.newEpisodes),
    Hall(Bi("Arabic", "عربي"), MockCatalog.arabicCinema),
    Hall(Bi("Vision", "رؤى"), MockCatalog.becauseDune),
    Hall(Bi("Dolby", "دولبي"), MockCatalog.dolbyVisionAtmos),
)

/**
 * Home as a monument: halls (sections) on the left, the titles of the current hall carved in a
 * vertical list, and one tall aperture through which the focused title is seen. No cards, no rows.
 */
@Composable
internal fun C03Home(session: ProtoSession) {
    val type = c03Type()
    val lang = LocalProtoLang.current
    var hall by remember { mutableIntStateOf(session.value("c03.hall", if (session.demoFocus) 1 else 0)) }
    val positions = remember { session.value("c03.pos", HashMap<Int, Int>().apply { if (session.demoFocus) put(1, 2) }) }
    val items = halls[hall].items
    var focused by remember { mutableStateOf(items[(positions[hall] ?: 0).coerceIn(0, items.lastIndex)]) }
    val itemReqs = remember(hall) { List(items.size) { FocusRequester() } }
    val hallReqs = remember { List(halls.size) { FocusRequester() } }

    LaunchedEffect(Unit) {
        repeat(8) {
            withFrameNanos { }
            if (runCatching { itemReqs[(positions[hall] ?: 0).coerceIn(0, items.lastIndex)].requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    LaunchedEffect(hall) {
        session.set("c03.hall", hall)
        session.set("c03.pos", positions)
    }

    C03Screen {
        Row(Modifier.fillMaxSize().padding(horizontal = C03.Margin, vertical = 44.dp)) {
            // I — Halls
            Column(Modifier.width(142.dp).fillMaxHeight()) {
                Txt("NUVIO", type.label.copy(color = C03.Sand))
                Spacer(Modifier.height(4.dp))
                Txt(tr("THE HALLS", "القاعات"), type.small)
                Spacer(Modifier.height(26.dp))
                halls.forEachIndexed { i, h ->
                    C03HallItem(numeral(i + 1), h.name.get().up(), i == hall, hallReqs[i]) {
                        if (hall != i) {
                            hall = i
                            focused = h.items[(positions[i] ?: 0).coerceIn(0, h.items.lastIndex)]
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                C03Rule(Modifier.width(60.dp))
                Spacer(Modifier.height(10.dp))
                C03HallItem("", tr("SEARCH", "البحث"), false, null) { session.nav.push(ProtoRoute.Search) }
                C03HallItem("", tr("ARCHIVE", "الأرشيف"), false, null) { session.nav.push(ProtoRoute.Library) }
                C03HallItem("", tr("PROFILE", "الملف"), false, null) { session.nav.push(ProtoRoute.Profile) }
            }
            C03VRule(Modifier.fillMaxHeight())
            Spacer(Modifier.width(30.dp))

            // II — The carved list and the plaque
            Column(Modifier.width(340.dp).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(tr("HALL ", "القاعة ") + numeral(hall + 1), type.label.copy(color = C03.Bronze))
                    Spacer(Modifier.width(10.dp))
                    C03Rule(Modifier.width(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Txt(halls[hall].name.get().up(), type.label)
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(214.dp)
                        .verticalScroll(session.scroll("c03.list.$hall")),
                ) {
                    Spacer(Modifier.height(90.dp))
                    items.forEachIndexed { i, t ->
                        C03ListItem(
                            t.title.get().up(),
                            requester = itemReqs[i],
                            style = type.listItem,
                            onFocus = {
                                focused = t
                                positions[hall] = i
                            },
                        ) { session.nav.push(ProtoRoute.Details(t.id)) }
                    }
                    Spacer(Modifier.height(110.dp))
                }
                Spacer(Modifier.height(18.dp))
                C03Rule(Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                AnimatedContent(focused, transitionSpec = { fadeIn(tween(C03.SLOW, 120, ProtoEasing.Cinematic)) togetherWith fadeOut(tween(180)) }, label = "plaque") { t ->
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Txt(t.year.toString().digits(), type.numeral)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.padding(bottom = 8.dp)) {
                                Txt((if (t.isSeries) tr("${t.seasons.size} seasons", "${t.seasons.size} مواسم") else formatRuntime(t.runtimeMin, lang)).up(), type.label.copy(color = C03.Sand))
                                Txt(t.primaryGenre.get().up() + "  ·  " + t.country.get().up(), type.small)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Txt(resumeLine(t)?.up() ?: t.tagline.get(), type.label.copy(color = C03.BronzeLight), maxLines = 1)
                        Spacer(Modifier.height(6.dp))
                        Txt(t.synopsis.get(), type.body, maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.width(34.dp))

            // III — The aperture
            Box(Modifier.weight(1f).fillMaxHeight().stoneEdge().background(C03.Shadow)) {
                Crossfade(focused, animationSpec = tween(900, easing = ProtoEasing.Cinematic), label = "aperture") { t ->
                    ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.02f; scaleY = 1.02f })
                }
                Box(Modifier.fillMaxSize().background(C03Lintel))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    C03TechLine(focused)
                }
            }
        }
    }
}

@Composable
private fun C03TechLine(t: ProtoTitle) {
    val type = c03Type()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        t.tech.labels().take(4).forEach { Txt(it.up(), type.small.copy(color = C03.Sand.copy(alpha = 0.8f))) }
    }
}

@Composable
private fun C03HallItem(num: String, label: String, selected: Boolean, requester: FocusRequester?, onFocusSelect: () -> Unit) {
    val type = c03Type()
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .protoFocusable(requester, onFocusChange = { focused = it; if (it && num.isNotEmpty()) onFocusSelect() }, onClick = onFocusSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(2.dp).height(18.dp).background(if (selected) C03.Bronze else if (focused) C03.Sand else C03.Stone))
        Spacer(Modifier.width(10.dp))
        if (num.isNotEmpty()) {
            Txt(num, type.label.copy(color = if (selected || focused) C03.BronzeLight else C03.SandFaint), Modifier.width(36.dp))
        }
        Txt(label, type.label.copy(color = if (focused) C03.Sand else if (selected) C03.SandDim else C03.SandFaint), maxLines = 1)
    }
}
