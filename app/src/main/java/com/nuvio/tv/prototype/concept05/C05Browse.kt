package com.nuvio.tv.prototype.concept05

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ColorField
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.player.ProtoKeyboard
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

/** Voice first. The result list is words; the screen becomes whichever result you're on. */
@Composable
internal fun C05Search(session: ProtoSession) {
    val type = c05Type()
    val ar = isArabic()
    var query by remember { mutableStateOf(if (session.demoSearch) MockCatalog.demoQuery(ar) else "") }
    val results = if (session.demoSearch && query == MockCatalog.demoQuery(ar)) MockCatalog.demoResults(ar) else MockCatalog.search(query)
    var bg by remember { mutableStateOf<ProtoTitle?>(results.firstOrNull()) }
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    val frozen = LocalProtoEnv.current.frozen
    val pulse = if (frozen) 0.5f else rememberInfiniteTransition(label = "mic").animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p").value

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(bg, animationSpec = tween(700), label = "bg") { t ->
            if (t != null) ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f })
        }
        Box(Modifier.fillMaxSize().background(startScrim(0f to Color(0xE6000000), 0.6f to Color(0x80000000), 1f to Color(0x33000000))))
        Column(Modifier.fillMaxSize().padding(horizontal = C05.Margin, vertical = 44.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size((28 + 12 * pulse).dp).clip(CircleShape).background(C05.Signal.copy(alpha = 0.25f)))
                    ProtoIcon(Glyph.MIC, size = 18.dp, color = C05.Ink, stroke = 1.6.dp)
                }
                Spacer(Modifier.width(14.dp))
                Txt(query.ifEmpty { tr("Say a title, a person, a mood", "قل عنواناً أو اسماً أو مزاجاً") }, type.big.copy(color = if (query.isEmpty()) C05.Ink3 else C05.Ink), maxLines = 1)
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                results.forEachIndexed { i, t ->
                    C05Action("${(i + 1).toString().padStart(2, '0')}   ${t.title.get()}   ${t.year}", onFocus = { bg = t }) { session.nav.push(ProtoRoute.Details(t.id)) }
                }
            }
            // Typing is the fallback, a single line along the bottom edge.
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProtoKeyboard.keys(ar).forEachIndexed { i, k -> C05Action(k, requester = if (i == 0) first else null) { query += k } }
                C05Action(tr("space", "مسافة")) { query += " " }
                C05Action(tr("delete", "حذف")) { query = query.dropLast(1) }
            }
        }
    }
}

/** The library is a contact sheet: everything at once, labels in slate type. */
@Composable
internal fun C05Library(session: ProtoSession) {
    val type = c05Type()
    val groups = listOf(
        Bi("Kept", "محفوظ") to MockCatalog.watchlist,
        Bi("In progress", "قيد المشاهدة") to MockCatalog.continueWatching,
        Bi("Seen", "شوهد") to MockCatalog.watched,
    )
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    var label by remember { mutableStateOf(groups[0].second.first().title) }
    Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(horizontal = C05.Margin, vertical = 40.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(tr("LIBRARY", "المكتبة"), type.slate)
            Spacer(Modifier.width(18.dp))
            Txt(label.get(), type.title, maxLines = 1)
        }
        Spacer(Modifier.height(18.dp))
        groups.forEachIndexed { g, (name, items) ->
            Txt("${name.get().slate()}  ${items.size.toString().padStart(2, '0')}", type.slateSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { i, t ->
                    var focused by remember { mutableStateOf(false) }
                    val f by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "l")
                    Box(
                        Modifier
                            .width(98.dp)
                            .aspectRatio(2f / 3f)
                            .graphicsLayer { alpha = 0.5f + 0.5f * f; scaleX = 1f + 0.05f * f; scaleY = 1f + 0.05f * f }
                            .border(1.dp, if (focused) C05.Ink else Color.Transparent)
                            .protoFocusable(if (g == 0 && i == 0) first else null, onFocusChange = { focused = it; if (it) label = t.title }, onClick = { session.nav.push(ProtoRoute.Details(t.id)) }),
                    ) { ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize()) }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
internal fun C05Profile(session: ProtoSession) {
    val type = c05Type()
    var idx by remember { mutableIntStateOf(0) }
    val favourites = listOf(MockCatalog.dune2, MockCatalog.pastlives, MockCatalog.severance, MockCatalog.wadjda, MockCatalog.arrival)
    val first = remember { FocusRequester() }
    RequestFocusOnce(first)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(favourites[idx], animationSpec = tween(700), label = "p") { t -> ColorField(t.palette, Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xCC000000)))))
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = C05.Margin)) {
            Txt(tr("WHO IS WATCHING", "من يشاهد"), type.slate)
            Spacer(Modifier.height(10.dp))
            MockCatalog.profiles.forEachIndexed { i, p ->
                var focused by remember { mutableStateOf(false) }
                Txt(
                    p.name.get(),
                    type.big.copy(color = if (focused) C05.Ink else C05.Ink3),
                    Modifier.protoFocusable(if (i == 0) first else null, onFocusChange = { focused = it; if (it) idx = i }, onClick = { session.nav.home() }),
                )
            }
            Spacer(Modifier.height(20.dp))
            Txt(tr("PLAYBACK  BEST MATCH   /   SUBTITLES  ARABIC   /   UI  AUTO-HIDE 3.5S", "التشغيل  الأنسب   /   الترجمة  العربية   /   الواجهة  تختفي بعد 3.5 ث"), type.slateSmall)
        }
    }
}
