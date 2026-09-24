package com.nuvio.tv.prototype.concept02

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoLang
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
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.resumeLine
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

private class C02Section(val title: Bi, val items: List<ProtoTitle>, val poster: Boolean = false, val continueRow: Boolean = false)

private val sections = listOf(
    C02Section(Bi("Continue Watching", "تابع المشاهدة"), MockCatalog.continueWatching, continueRow = true),
    C02Section(Bi("Arabic Cinema", "السينما العربية"), MockCatalog.arabicCinema, poster = true),
    C02Section(Bi("Made for this screen · Dolby Vision + Atmos", "مصمم لهذه الشاشة · دولبي فيجن وأتموس"), MockCatalog.dolbyVisionAtmos),
    C02Section(Bi("Because you watched Dune", "لأنك شاهدت كثيب"), MockCatalog.becauseDune),
)

@Composable
internal fun C02Home(session: ProtoSession) {
    val type = c02Type()
    val focusedDefault = if (session.demoFocus) "arabic:1" else "continue:0"
    var focused by remember { mutableStateOf(session.value("c02.title", if (session.demoFocus) MockCatalog.theeb.id else MockCatalog.hero.id)) }
    val title = MockCatalog.title(focused)
    RestoreFocus(session.focus, focusedDefault)

    C02Screen {
        RiyadhSkyline(Modifier.fillMaxSize(), intensity = 0.55f)
        // Hero art follows focus, fading into the night on the start side and below.
        Box(Modifier.fillMaxWidth(0.66f).height(360.dp).align(Alignment.TopEnd)) {
            Crossfade(title, animationSpec = tween(650, easing = ProtoEasing.Decelerate), label = "art") { t ->
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(startScrim(0f to C02.Night, 0.4f to C02.Night.copy(alpha = 0.55f), 1f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to C02.Night)))
            Crenellation(Modifier.fillMaxWidth().align(Alignment.TopCenter).offset(y = 66.dp), C02.Gold.copy(alpha = 0.18f), 6.dp)
        }

        Column(Modifier.fillMaxSize()) {
            C02Header(session, 0)
            // Fixed lockup
            Box(Modifier.padding(horizontal = C02.Margin).height(212.dp)) {
                AnimatedContent(
                    title,
                    transitionSpec = { (fadeIn(tween(380, 60)) + slideInVertically(tween(420, easing = ProtoEasing.Decelerate)) { it / 14 }) togetherWith fadeOut(tween(160)) },
                    label = "lockup",
                ) { t -> C02Lockup(t, session) }
            }
            // Rows viewport: the focused row always lands at the top edge.
            ProvideScrollPivot(ScrollPivot.Anchor(0f, durationMs = 360, easing = ProtoEasing.Decelerate)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(session.scroll("c02.rows")),
                ) {
                    sections.forEach { s ->
                        C02HeadingRow(s)
                        ProvideScrollPivot(ScrollPivot.Padded(48f, 280, ProtoEasing.Decelerate)) {
                            Row(
                                Modifier
                                    .horizontalScroll(session.scroll("c02.row.${s.title.en}"))
                                    .padding(horizontal = C02.Margin, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                s.items.forEachIndexed { i, t ->
                                    val key = (if (s.continueRow) "continue" else if (s.poster) "arabic" else s.title.en) + ":$i"
                                    val onFocus = { focused = t.id; session.set("c02.title", t.id); session.focus.lastFocused = key }
                                    val open = { session.nav.push(ProtoRoute.Details(t.id)) }
                                    if (s.poster) {
                                        C02Poster(t, 104.dp, session.focus.requester(key), onFocus, open)
                                    } else {
                                        C02Landscape(t, 178.dp, session.focus.requester(key), if (s.continueRow) t.progress else null, if (s.continueRow) resumeLine(t) else null, onFocus, open)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(260.dp))
                }
            }
        }
    }
}

@Composable
private fun C02HeadingRow(s: C02Section) {
    C02Heading(s.title, Modifier.padding(start = C02.Margin, end = C02.Margin, top = 10.dp), count = null)
}

@Composable
internal fun C02Header(session: ProtoSession, selected: Int) {
    val type = c02Type()
    val clock by rememberProtoClock()
    val lang = LocalProtoLang.current
    val ar = isArabic()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = C02.Margin).padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Txt("NUVIO", type.eyebrow.copy(color = C02.GoldLight, letterSpacing = type.eyebrow.letterSpacing * (if (ar) 0f else 1.3f)))
            Txt("نوفيو", type.eyebrowSecond)
        }
        Spacer(Modifier.width(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            C02Nav.forEachIndexed { i, item ->
                C02NavItem(item, i == selected) {
                    when (i) {
                        4 -> session.nav.push(ProtoRoute.Search)
                        5 -> session.nav.push(ProtoRoute.Library)
                        else -> Unit
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Txt(clock.clock12(lang), type.label)
            Txt(tr("Riyadh · 31°", "الرياض · 31°"), type.captionSecond.copy(color = C02.Ink2))
        }
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, C02.Gold, CircleShape)
                .protoFocusable(onClick = { session.nav.push(ProtoRoute.Profile) }),
            contentAlignment = Alignment.Center,
        ) {
            Txt(if (ar) "ف" else "F", type.label.copy(color = C02.GoldLight))
        }
    }
}

@Composable
private fun C02NavItem(item: Bi, selected: Boolean, onClick: () -> Unit) {
    val type = c02Type()
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(RoundedCornerShape(C02.Radius))
            .background(if (focused) C02.Glass else Color.Transparent)
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(item.get(), type.label.copy(color = if (focused || selected) C02.Ink else C02.Ink2))
        Txt(item.other(), type.captionSecond.copy(color = if (selected) C02.GoldDim else C02.Ink3))
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(14.dp).height(1.5.dp).background(if (selected) C02.Gold else Color.Transparent))
    }
}

/** Bilingual title lockup with the evening concierge note. */
@Composable
private fun C02Lockup(t: ProtoTitle, session: ProtoSession) {
    val type = c02Type()
    val clock by rememberProtoClock()
    val lang = LocalProtoLang.current
    Column(Modifier.width(520.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
        C02Heading(
            when {
                t.progress != null -> Bi("Continue tonight", "أكمل الليلة")
                t.id == MockCatalog.hero.id -> Bi("Tonight's premiere", "عرض الليلة")
                else -> Bi("Selected for you", "مختار لك")
            },
        )
        Spacer(Modifier.height(10.dp))
        Txt(t.title.get(), type.hero, maxLines = 1)
        Txt(t.title.other(), type.heroSecond, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(metaLine(t) + "  ·  IMDb ${t.rating}", type.meta)
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (t.tech.uhd) C02Badge("4K")
                if (t.tech.dolbyVision) C02Badge("DOLBY VISION")
                if (t.tech.atmos) C02Badge("ATMOS")
            }
        }
        Spacer(Modifier.height(10.dp))
        // Hospitality detail: the concierge knows what time it is.
        val minutes = t.remainingMin ?: t.runtimeMin
        val endH = (clock.hour24 * 60 + clock.minute + minutes) / 60 % 24
        val endM = (clock.hour24 * 60 + clock.minute + minutes) % 60
        val end = com.nuvio.tv.prototype.shared.ProtoTime(endH, endM, 0, 0, 0).clock12(lang)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProtoIcon(Glyph.CLOCK, size = 12.dp, color = C02.Gold, stroke = 1.4.dp)
            Spacer(Modifier.width(8.dp))
            Txt(tr("Finishes by $end  ·  ", "ينتهي عند $end  ·  ") + (resumeLine(t) ?: t.tagline.get()), type.meta.copy(color = C02.GoldLight), maxLines = 1)
        }
    }
}

@Composable
internal fun C02Landscape(
    t: ProtoTitle,
    width: Dp,
    requester: FocusRequester?,
    progress: Float?,
    detail: String?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val f = c02FocusAnim(focused)
    Column(Modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .c02Focus(f)
                .clip(C02CardShape)
                .goldBorder(f)
                .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick),
        ) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize())
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66000000)))
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(3.dp).background(C02.Gold))
            }
        }
        Spacer(Modifier.height(8.dp))
        C02Caption(t.title, detail, focused)
    }
}

@Composable
internal fun C02Poster(t: ProtoTitle, width: Dp, requester: FocusRequester?, onFocus: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = c02FocusAnim(focused)
    Column(Modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .c02Focus(f)
                .clip(C02CardShape)
                .goldBorder(f)
                .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick),
        ) {
            ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xE6070A12))))
                Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    Txt(t.title.ar, c02Type().caption.copy(color = C02.GoldLight), maxLines = 1)
                    Txt(t.title.en.uppercase(), c02Type().captionSecond.copy(color = C02.Ink), maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        C02Caption(t.title, t.year.toString() + " · " + t.country.get(), focused)
    }
}
