package com.nuvio.tv.prototype.concept10

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoEasing
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.ProvideScrollPivot
import com.nuvio.tv.prototype.shared.ScrollPivot
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.rememberProtoClock
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

/** Rows keep focus on the content's start line, a fixed place for the eye. */
@Composable
internal fun C10RowPivot(content: @Composable () -> Unit) =
    ProvideScrollPivot(ScrollPivot.Anchor(0f, offsetDp = 56f, durationMs = 300, easing = ProtoEasing.Decelerate), content)

@Composable
internal fun C10RowHeader(title: String, detail: String? = null) {
    val type = c10Type()
    Row(Modifier.padding(horizontal = C10.Margin).padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
        Txt(title, type.title.copy(fontSize = type.title.fontSize * 0.8f), maxLines = 1)
        if (detail != null) {
            Spacer(Modifier.width(12.dp))
            Txt(detail, type.caption, Modifier.padding(bottom = 2.dp), maxLines = 1)
        }
    }
}

@Composable
internal fun C10Home(session: ProtoSession) {
    val type = c10Type()
    val lang = LocalProtoLang.current
    val clock by rememberProtoClock()
    val featured = MockCatalog.title(MockCatalog.featuredMovieId)
    var lit by remember { mutableStateOf(MockCatalog.title(session.value("c10.lit", featured.id))) }
    val scroll = session.scroll("c10.home")
    val heroPlay = remember { FocusRequester() }
    val cwReqs = remember { List(MockCatalog.continueWatching.size) { FocusRequester() } }
    val onLit: (ProtoTitle) -> Unit = { lit = it; session.set("c10.lit", it.id) }

    LaunchedEffect(Unit) {
        repeat(8) {
            withFrameNanos { }
            val target = if (session.demoFocus) cwReqs[2] else heroPlay
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Box(Modifier.fillMaxSize()) {
        C10Ambient(lit.palette)
        // Hero art: full bleed, parallax at half speed, dissolving into black and into the tint.
        Box(Modifier.fillMaxWidth().height(420.dp).graphicsLayer { translationY = -scroll.value * 0.5f; alpha = (1f - scroll.value / 700f).coerceIn(0f, 1f) }) {
            Crossfade(lit, animationSpec = tween(600, easing = ProtoEasing.Decelerate), label = "hero") { t ->
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize(), alignment = Alignment.TopEnd)
            }
            Box(Modifier.fillMaxSize().background(startScrim(0f to C10.Black, 0.38f to C10.Black.copy(alpha = 0.72f), 0.7f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to C10.Black)))
        }

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // Hero copy.
            Column(Modifier.padding(start = C10.Margin, top = 92.dp).width(540.dp)) {
                val nu = nextUp(lit)
                val lead = when {
                    nu != null -> nu.text.of(lang)
                    lit.id == featured.id -> tr("New in Dolby Vision", "جديد بتقنية دولبي فيجن")
                    else -> lit.primaryGenre.of(lang)
                }
                Txt(lead.let { if (lang.isRtl) it else it.uppercase() }, type.overline, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                TitleReveal(lit.title.get(), type.hero, key = lit.id, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(metaLine(lit), type.body.copy(color = C10.Ink), maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    lit.tech.labels().take(3).forEach { C10Chip(it); Spacer(Modifier.width(5.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(lit, transitionSpec = { fadeIn(tween(300, 80)) togetherWith fadeOut(tween(120)) }, label = "syn") { t ->
                    Txt(t.synopsis.get(), type.body, maxLines = 2)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    C10Button(playLabel(lit), Glyph.PLAY, heroPlay, primary = true) { session.nav.push(ProtoRoute.Streams(lit.id)) }
                    C10Button(tr("Details", "التفاصيل")) { session.nav.push(ProtoRoute.Details(lit.id)) }
                    C10Button("", Glyph.PLUS) {}
                }
            }
            Spacer(Modifier.height(34.dp))

            // Continue: the intelligence lives on the card.
            C10RowHeader(tr("Continue watching", "تابع المشاهدة"))
            C10RowPivot {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(session.scroll("c10.cw")).padding(horizontal = C10.Margin).padding(top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MockCatalog.continueWatching.forEachIndexed { i, t ->
                        C10ContinueCard(t, cwReqs[i], onFocus = { onLit(t) }) { session.nav.push(ProtoRoute.Streams(t.id)) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            C10Tonight(session, onLit)
            Spacer(Modifier.height(26.dp))

            C10PosterRow(session, tr("Because you watched Dune", "لأنك شاهدت «كثيب»"), null, MockCatalog.becauseDune, onLit)
            C10PosterRow(session, tr("Arabic cinema", "سينما عربية"), tr("Five films, one language", "خمسة أفلام، لغة واحدة"), MockCatalog.arabicCinema, onLit)
            C10PosterRow(session, tr("Dolby Vision & Atmos", "دولبي فيجن وأتموس"), tr("Best on this TV", "الأفضل على هذه الشاشة"), MockCatalog.dolbyVisionAtmos, onLit)
            Spacer(Modifier.height(60.dp))
        }

        C10TopNav(C10Section.HOME, clock) { session.goTo(it) }
    }
}

@Composable
private fun C10ContinueCard(t: ProtoTitle, requester: FocusRequester, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c10Type()
    val lang = LocalProtoLang.current
    val nu = nextUp(t)
    C10Card(t, if (t.isSeries) ArtKind.STILL else ArtKind.BACKDROP, 236.dp, 16f / 9f, requester = requester, episode = t.resume, onFocus = { if (it) onFocus() }, onClick = onClick, overlay = { _ ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xE6000000))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
                Txt(t.title.get(), type.label, maxLines = 1)
                if (nu != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    if (nu.highlight) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(C10.Gold))
                        Spacer(Modifier.width(6.dp))
                    }
                    Txt(nu.text.of(lang), type.caption.copy(color = if (nu.highlight) C10.Gold else C10.Ink2), maxLines = 1)
                }
            }
            val p = t.progress
            if (p != null) C10Progress(p) else Spacer(Modifier.height(10.dp))
        }
    })
}

/**
 * One editorial block per home, never more: a pairing with a reason, written like a programme
 * note. It breaks the rhythm of rows exactly once.
 */
@Composable
private fun C10Tonight(session: ProtoSession, onLit: (ProtoTitle) -> Unit) {
    val type = c10Type()
    val lawrence = MockCatalog.lawrence
    val theeb = MockCatalog.theeb
    Row(
        Modifier.padding(horizontal = C10.Margin).fillMaxWidth().height(208.dp).clip(RoundedCornerShape(14.dp)).background(C10.Surface),
    ) {
        Box(Modifier.weight(1.15f).fillMaxHeight()) {
            ProtoArtwork(lawrence, ArtKind.BACKDROP, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.55f to Color.Transparent, 1f to C10.Surface)))
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, end = 26.dp, top = 22.dp, bottom = 20.dp)) {
            Txt(over("Tonight · a double bill", "الليلة · عرض مزدوج"), type.overline)
            Spacer(Modifier.height(6.dp))
            Txt(tr("Two deserts, sixty years apart.", "صحراءان، بينهما ستون عاماً."), type.headline, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Txt(
                tr(
                    "Lawrence of Arabia in its 4K restoration, then Theeb — the same sand, seen from the other side.",
                    "«لورنس العرب» بنسخته المرمّمة بدقة 4K، ثم «ذيب» — الرمال نفسها من الجهة الأخرى.",
                ),
                type.body, maxLines = 2,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                C10Button(tr("Start with Lawrence", "ابدأ بـ«لورنس»"), Glyph.PLAY, onFocus = { onLit(lawrence) }) { session.nav.push(ProtoRoute.Streams(lawrence.id)) }
                C10Button(tr("Theeb", "ذيب"), onFocus = { onLit(theeb) }) { session.nav.push(ProtoRoute.Details(theeb.id)) }
            }
        }
    }
}

@Composable
private fun C10PosterRow(session: ProtoSession, title: String, detail: String?, items: List<ProtoTitle>, onLit: (ProtoTitle) -> Unit) {
    C10RowHeader(title, detail)
    C10RowPivot {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(session.scroll("c10.row.$title")).padding(horizontal = C10.Margin).padding(top = 8.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items.forEach { t ->
                C10Card(t, ArtKind.POSTER, 118.dp, 2f / 3f, onFocus = { if (it) onLit(t) }) { session.nav.push(ProtoRoute.Details(t.id)) }
            }
        }
    }
}

/** The quality line used wherever a title offers to play: the best stream, stated plainly. */
@Composable
internal fun bestLine(t: ProtoTitle): String {
    val best = remember(t.id) { StreamIntelligence.best(StreamIntelligence.streamsFor(t, t.resumeSeason, t.resumeEpisode)) }
    val s = best.stream
    return StreamIntelligence.headline(s) + (if (s.isCached) tr(" · starts instantly", " · يبدأ فوراً") else tr(" · needs to download", " · يحتاج إلى تنزيل")) +
        (if (s.hasArabicSubs) tr(" · Arabic subtitles", " · ترجمة عربية") else "")
}
