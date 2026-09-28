package com.nuvio.tv.prototype.concept04

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.ProvideScrollPivot
import com.nuvio.tv.prototype.shared.RestoreFocus
import com.nuvio.tv.prototype.shared.ScrollPivot
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.resumeLine

private class C04Row(val key: String, val title: Bi, val items: List<ProtoTitle>, val poster: Boolean)

private val rows = listOf(
    C04Row("cw", Bi("Continue Watching", "تابع المشاهدة"), MockCatalog.continueWatching, poster = false),
    C04Row("tonight", Bi("Recommended Tonight", "مقترح لهذه الليلة"), MockCatalog.tonight, poster = true),
    C04Row("arabic", Bi("Arabic Cinema", "السينما العربية"), MockCatalog.arabicCinema, poster = false),
    C04Row("dv", Bi("Dolby Vision + Atmos", "دولبي فيجن + أتموس"), MockCatalog.dolbyVisionAtmos, poster = true),
)

@Composable
internal fun C04Home(session: ProtoSession) {
    val type = c04Type()
    var focused by remember { mutableStateOf(MockCatalog.title(session.value("c04.title", if (session.demoFocus) MockCatalog.br2049.id else MockCatalog.hero.id))) }
    val accent = rememberAccent(focused)
    RestoreFocus(session.focus, if (session.demoFocus) "tonight:1" else "cw:0")

    C04Room(focused) {
        Column(Modifier.fillMaxSize()) {
            C04NavCapsule(session, 0)
            // Hero: information on the start side, the film seen through a rounded window.
            Row(Modifier.fillMaxWidth().height(232.dp).padding(horizontal = C04.Margin), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(focused, transitionSpec = { fadeIn(tween(420, 120)) togetherWith fadeOut(tween(160)) }, label = "info", modifier = Modifier.weight(1f)) { t ->
                    Column {
                        C04Chip(resumeLine(t) ?: t.primaryGenre.get())
                        Spacer(Modifier.height(10.dp))
                        Txt(t.title.get(), type.hero, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Txt(metaLine(t) + "  ·  IMDb ${t.rating}", type.meta)
                        Spacer(Modifier.height(10.dp))
                        Txt(t.synopsis.get(), type.body, maxLines = 2)
                    }
                }
                Spacer(Modifier.width(30.dp))
                Box(
                    Modifier
                        .width(420.dp)
                        .height(222.dp)
                        .bloom(accent, 0.8f, C04.WindowRadius)
                        .clip(RoundedCornerShape(C04.WindowRadius)),
                ) {
                    Crossfade(focused, animationSpec = tween(700, easing = ProtoEasing.Decelerate), label = "window") { t -> ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize()) }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x66000000))))
                    Row(Modifier.align(Alignment.BottomStart).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        focused.tech.labels().take(3).forEach { C04Chip(it) }
                    }
                }
            }
            // The focused row settles at the top of the viewport with its heading still showing.
            ProvideScrollPivot(ScrollPivot.Anchor(0f, offsetDp = 50f, durationMs = 380, easing = ProtoEasing.Decelerate)) {
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(session.scroll("c04.rows"))) {
                    rows.forEach { row ->
                        C04RowView(row, session, accent) { t ->
                            focused = t
                            session.set("c04.title", t.id)
                        }
                    }
                    Spacer(Modifier.height(240.dp))
                }
            }
        }
    }
}

@Composable
private fun C04RowView(row: C04Row, session: ProtoSession, accent: Color, onFocusTitle: (ProtoTitle) -> Unit) {
    val type = c04Type()
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    Column {
        Row(Modifier.padding(start = C04.Margin, end = C04.Margin, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(row.title.get(), type.section)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(18.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        }
        ProvideScrollPivot(ScrollPivot.Center(360, ProtoEasing.Decelerate)) {
            Row(
                Modifier.horizontalScroll(session.scroll("c04.row.${row.key}")).padding(horizontal = C04.Margin, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.items.forEachIndexed { i, t ->
                    val key = "${row.key}:$i"
                    C04Card(
                        t,
                        width = if (row.poster) 108.dp else 196.dp,
                        poster = row.poster,
                        requester = session.focus.requester(key),
                        dx = displacement(i, focusedIndex, focusedIndex != null),
                        progress = if (row.key == "cw") t.progress else null,
                        onFocusChange = { f ->
                            if (f) {
                                focusedIndex = i
                                session.focus.lastFocused = key
                                onFocusTitle(t)
                            } else if (focusedIndex == i) {
                                focusedIndex = null
                            }
                        },
                    ) { session.nav.push(ProtoRoute.Details(t.id)) }
                }
                Spacer(Modifier.width(400.dp))
            }
        }
    }
}

@Composable
internal fun C04Card(
    t: ProtoTitle,
    width: Dp,
    poster: Boolean,
    requester: FocusRequester? = null,
    dx: Float = 0f,
    progress: Float? = null,
    showCaption: Boolean = true,
    onFocusChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
) {
    val type = c04Type()
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(C04.FOCUS_MS, easing = ProtoEasing.Decelerate), label = "c")
    Column(Modifier.width(width).graphicsLayer { translationX = dx * density }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (poster) 2f / 3f else 16f / 9f)
                .graphicsLayer { scaleX = 1f + (C04.FOCUS_SCALE - 1f) * f; scaleY = 1f + (C04.FOCUS_SCALE - 1f) * f }
                .bloom(t.palette.ui, f)
                .clip(RoundedCornerShape(C04.CardRadius))
                .protoFocusable(requester, onFocusChange = { focused = it; onFocusChange(it) }, onClick = onClick),
        ) {
            ProtoArtwork(t, if (poster) ArtKind.POSTER else ArtKind.BACKDROP, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.12f * f)))
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                }
            }
        }
        if (showCaption) {
            Spacer(Modifier.height(10.dp))
            Txt(t.title.get(), type.label.copy(color = if (focused) C04.Ink else C04.Ink2), Modifier.graphicsLayer { alpha = 0.7f + 0.3f * f }, maxLines = 1)
        }
    }
}

@Composable
internal fun C04NavCapsule(session: ProtoSession, selected: Int) {
    val type = c04Type()
    Box(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        Txt("nuvio", type.title.copy(fontSize = type.title.fontSize * 0.9f), Modifier.align(Alignment.CenterStart).padding(start = C04.Margin))
        Row(Modifier.glass(50.dp).padding(4.dp)) {
            listOf(
                Triple(Glyph.HOME, Bi("Home", "الرئيسية"), null),
                Triple(Glyph.SEARCH, Bi("Search", "البحث"), ProtoRoute.Search),
                Triple(Glyph.LIBRARY, Bi("Library", "المكتبة"), ProtoRoute.Library),
                Triple(Glyph.PROFILE, Bi("Profile", "الملف"), ProtoRoute.Profile),
            ).forEachIndexed { i, (g, label, route) ->
                var focused by remember { mutableStateOf(false) }
                val active = i == selected
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (focused) C04.Ink else if (active) C04.GlassStrong else Color.Transparent)
                        .protoFocusable(onFocusChange = { focused = it }, onClick = { route?.let { session.nav.push(it) } ?: session.nav.home() })
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProtoIcon(g, size = 13.dp, color = if (focused) C04.Dark else C04.Ink, stroke = 1.7.dp)
                    if (active || focused) {
                        Spacer(Modifier.width(7.dp))
                        Txt(label.get(), type.label.copy(color = if (focused) C04.Dark else C04.Ink))
                    }
                }
            }
        }
    }
}
