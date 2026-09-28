package com.nuvio.tv.prototype.concept07

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RestoreFocus
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.formatRuntime
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

/** Short editorial notes for tonight's numbered list — the "why", not the synopsis. */
private val notes = mapOf(
    "shogun" to ("Ten hours of patience that pay off in a single look." to "عشر ساعات من الصبر تُثمر في نظرة واحدة."),
    "br2049" to ("Put the lights down. This one is about the light." to "أطفئ الأنوار، فهذا الفيلم عن الضوء."),
    "arrival" to ("The quietest science fiction film you will ever cry at." to "أهدأ فيلم خيال علمي قد يبكيك."),
    "pastlives" to ("Watch it with someone. Talk about it after." to "شاهده مع أحد، وتحدثا عنه بعدها."),
    "theeb" to ("A western, a coming-of-age story and a desert, all at once." to "فيلم غرب وحكاية نضوج وصحراء في آن واحد."),
)

@Composable
internal fun C07Home(session: ProtoSession) {
    val type = c07Type()
    val cover = MockCatalog.dune2
    RestoreFocus(session.focus, if (session.demoFocus) "tonight:2" else "cover")
    Column(Modifier.fillMaxSize().background(C07.Paper).filmGrain(0.05f).verticalScroll(session.scroll("c07.home"))) {
        C07Masthead(0) { i ->
            when (i) {
                4 -> session.nav.push(ProtoRoute.Search)
                5 -> session.nav.push(ProtoRoute.Library)
                6 -> session.nav.push(ProtoRoute.Profile)
                else -> Unit
            }
        }
        Spacer(Modifier.height(22.dp))
        C07Cover(cover, session.focus.requester("cover"), onFocus = { session.focus.lastFocused = "cover" }) { session.nav.push(ProtoRoute.Details(cover.id)) }
        Spacer(Modifier.height(40.dp))

        Row(Modifier.padding(horizontal = C07.Margin), horizontalArrangement = Arrangement.spacedBy(C07.Gutter)) {
            // Tonight, numbered: a finite list, not an endless row.
            Column(Modifier.weight(1.15f)) {
                Txt(tr("TONIGHT, IN THE ORDER WE'D WATCH THEM", "الليلة، بالترتيب الذي نقترحه"), type.kicker)
                Spacer(Modifier.height(8.dp))
                C07Rule()
                MockCatalog.tonight.take(5).forEachIndexed { i, t ->
                    C07NumberedRow(i + 1, t, session.focus.requester("tonight:$i"), onFocus = { session.focus.lastFocused = "tonight:$i" }) { session.nav.push(ProtoRoute.Details(t.id)) }
                }
            }
            // The Arabic new wave: an asymmetric spread with a pull quote.
            Column(Modifier.weight(1f)) {
                Txt(tr("THE ARABIC NEW WAVE", "الموجة العربية الجديدة"), type.kicker)
                Spacer(Modifier.height(8.dp))
                C07Rule()
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    C07Image(MockCatalog.wadjda, ArtKind.POSTER, Modifier.width(150.dp).aspectRatio(2f / 3f), session.focus.requester("arabic:0"), onFocus = { session.focus.lastFocused = "arabic:0" }) {
                        session.nav.push(ProtoRoute.Details(MockCatalog.wadjda.id))
                    }
                    Column(Modifier.weight(1f)) {
                        C07Image(MockCatalog.theeb, ArtKind.BACKDROP, Modifier.fillMaxWidth().aspectRatio(16f / 10f), session.focus.requester("arabic:1"), onFocus = { session.focus.lastFocused = "arabic:1" }) {
                            session.nav.push(ProtoRoute.Details(MockCatalog.theeb.id))
                        }
                        Spacer(Modifier.height(12.dp))
                        C07Image(MockCatalog.capernaum, ArtKind.BACKDROP, Modifier.fillMaxWidth().aspectRatio(16f / 10f), session.focus.requester("arabic:2"), onFocus = { session.focus.lastFocused = "arabic:2" }) {
                            session.nav.push(ProtoRoute.Details(MockCatalog.capernaum.id))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    Box(Modifier.width(3.dp).height(76.dp).background(C07.Red))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Txt((MockCatalog.wadjda.quote?.get() ?: "").let { "“$it”" }, type.quote, maxLines = 3)
                        Txt(tr("On Wadjda, the first feature shot entirely in Saudi Arabia", "عن «وجدة»، أول فيلم روائي يُصوَّر بالكامل في السعودية"), type.caption)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))

        Column(Modifier.padding(horizontal = C07.Margin)) {
            Txt(tr("BOOKMARKS", "إشارات مرجعية"), type.kicker)
            Spacer(Modifier.height(8.dp))
            C07Rule()
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(session.scroll("c07.bookmarks")), horizontalArrangement = Arrangement.spacedBy(C07.Gutter)) {
                MockCatalog.continueWatching.forEachIndexed { i, t ->
                    Column(Modifier.width(200.dp)) {
                        C07Image(t, ArtKind.BACKDROP, Modifier.fillMaxWidth().aspectRatio(16f / 9f), session.focus.requester("cw:$i"), onFocus = { session.focus.lastFocused = "cw:$i" }) {
                            session.nav.push(ProtoRoute.Details(t.id))
                        }
                        Spacer(Modifier.height(8.dp))
                        Txt(t.title.get(), type.headlineS, maxLines = 1)
                        val total = if (t.isSeries) t.resume?.runtimeMin ?: t.runtimeMin else t.runtimeMin
                        val at = ((t.progress ?: 0f) * total).toInt()
                        Txt(tr("Page $at of $total", "صفحة $at من $total"), type.caption)
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun C07Cover(t: ProtoTitle, requester: FocusRequester, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c07Type()
    val lang = LocalProtoLang.current
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    Box(
        Modifier
            .padding(horizontal = C07.Margin)
            .fillMaxWidth()
            .height(310.dp)
            .redRule(f)
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick),
    ) {
        // The photograph sits to the end side; the headline breaks into it.
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.width(300.dp))
            Box(Modifier.fillMaxSize().clipToBounds()) {
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { val s = 1f + 0.05f * f; scaleX = s; scaleY = s })
                Box(Modifier.fillMaxSize().background(startScrim(0f to C07.Paper, 0.35f to C07.Paper.copy(alpha = 0.4f), 0.6f to Color.Transparent)))
            }
        }
        Column(Modifier.fillMaxHeight().width(540.dp), verticalArrangement = Arrangement.Center) {
            Txt(tr("COVER STORY  ·  CRITIC'S PICK", "قصة الغلاف  ·  اختيار الناقد"), type.kicker)
            Spacer(Modifier.height(10.dp))
            Txt(if (type.arabic) "الصحراء\nتتذكّر" else "The desert\nremembers", type.headline, maxLines = 3)
            Spacer(Modifier.height(10.dp))
            Txt(t.quote?.get() ?: "", type.deck, Modifier.width(400.dp), maxLines = 2)
            Spacer(Modifier.height(12.dp))
            Txt(t.title.get() + "  ·  " + t.director.get() + "  ·  " + formatRuntime(t.runtimeMin, lang), type.nav.copy(color = if (focused) C07.Ink else C07.Ink2))
        }
    }
}

@Composable
private fun C07NumberedRow(n: Int, t: ProtoTitle, requester: FocusRequester, onFocus: () -> Unit, onClick: () -> Unit) {
    val type = c07Type()
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    val lang = LocalProtoLang.current
    Column(Modifier.protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(n.toString(), type.numeral.copy(color = C07.Red.copy(alpha = 0.55f + 0.45f * f)), Modifier.width(46.dp))
            Column(Modifier.weight(1f)) {
                Txt(t.title.get(), type.headlineS.copy(color = if (focused) C07.Ink else C07.Ink.copy(alpha = 0.85f)), maxLines = 1)
                val note = notes[t.id]
                Txt(if (note != null) (if (lang.isRtl) note.second else note.first) else t.tagline.get(), type.body.copy(fontSize = type.body.fontSize * 0.95f), maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(96.dp).aspectRatio(16f / 9f).clipToBounds()) {
                ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { val s = 1f + 0.08f * f; scaleX = s; scaleY = s; alpha = 0.75f + 0.25f * f })
            }
        }
        C07Rule(color = if (focused) C07.Red else C07.Rule)
    }
}

@Composable
internal fun C07Image(t: ProtoTitle, kind: ArtKind, modifier: Modifier, requester: FocusRequester? = null, onFocus: () -> Unit = {}, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = c07Anim(focused)
    Box(modifier.redRule(f).clipToBounds().protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick)) {
        ProtoArtwork(t, kind, Modifier.fillMaxSize().graphicsLayer { val s = 1f + 0.06f * f; scaleX = s; scaleY = s })
    }
}
