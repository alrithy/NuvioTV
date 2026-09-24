package com.nuvio.tv.prototype.concept06

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.ProtoRoute
import com.nuvio.tv.prototype.shared.ProtoSession
import com.nuvio.tv.prototype.shared.RestoreFocus
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.ArtKind
import com.nuvio.tv.prototype.shared.art.ProtoArtwork
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.formatTimecode
import com.nuvio.tv.prototype.shared.metaLine
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.startScrim
import com.nuvio.tv.prototype.shared.tr

/** Home is a dashboard of modules, each with one job, laid out on a strict 8dp grid. */
@Composable
internal fun C06Home(session: ProtoSession) {
    val type = c06Type()
    val resume = MockCatalog.dune2
    val next = MockCatalog.severance
    var preview by remember { mutableStateOf<ProtoTitle>(if (session.demoFocus) MockCatalog.arrival else resume) }
    RestoreFocus(session.focus, if (session.demoFocus) "tonight:2" else "resume")

    C06Screen(session, 0, tr("Home", "الرئيسية")) {
        Column(Modifier.fillMaxSize().verticalScroll(session.scroll("c06.home")), verticalArrangement = Arrangement.spacedBy(C06.Gap)) {
            Row(Modifier.fillMaxWidth().height(214.dp), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                // Resume module: the primary instrument.
                C06Module(Modifier.weight(2f).fillMaxHeight(), focusable = false, padding = 0.dp) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(0.9f).fillMaxHeight().padding(16.dp)) {
                            C06Label(if (preview.id == resume.id) tr("Resume", "استئناف") else tr("Preview", "معاينة"))
                            Spacer(Modifier.height(6.dp))
                            Txt(preview.title.get(), type.display, maxLines = 2)
                            Txt(metaLine(preview), type.caption, maxLines = 1)
                            Spacer(Modifier.weight(1f))
                            val p = preview.progress ?: 0f
                            Row(verticalAlignment = Alignment.Bottom) {
                                Txt(formatTimecode((p * preview.runtimeMin * 60).toInt(), true), type.readoutBig)
                                Spacer(Modifier.width(6.dp))
                                Txt("/ " + formatTimecode(preview.runtimeMin * 60, true), type.caption, Modifier.padding(bottom = 4.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            C06Bar(p, Modifier.fillMaxWidth(), mark = preview.chapters.getOrNull(3)?.startFraction)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                C06Button(tr("Resume", "استئناف"), Glyph.PLAY, primary = true, requester = session.focus.requester("resume"), onFocus = { preview = resume; session.focus.lastFocused = "resume" }) {
                                    session.nav.push(ProtoRoute.Streams(resume.id))
                                }
                                C06Button(tr("Details", "التفاصيل"), onFocus = { preview = resume }) { session.nav.push(ProtoRoute.Details(resume.id)) }
                            }
                        }
                        Box(Modifier.weight(1.1f).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(8.dp))) {
                            Crossfade(preview, animationSpec = tween(360), label = "pv") { t -> ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize()) }
                            Box(Modifier.fillMaxSize().background(startScrim(0f to Color(0x99000000), 0.4f to Color.Transparent)))
                            Row(Modifier.align(Alignment.BottomEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                preview.tech.labels().take(3).forEach { C06Tag(it) }
                            }
                        }
                    }
                }
                // Up next module.
                C06Module(
                    Modifier.weight(1f).fillMaxHeight(),
                    requester = session.focus.requester("next"),
                    onFocus = { preview = next; session.focus.lastFocused = "next" },
                    onClick = { session.nav.push(ProtoRoute.Episodes(next.id, 2)) },
                ) { f ->
                    C06Label(tr("Up next", "التالي"))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))) {
                        ProtoArtwork(next, ArtKind.STILL, Modifier.fillMaxSize().graphicsLayer { alpha = 0.7f + 0.3f * f }, episode = next.resume)
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(next.progress ?: 0f).height(3.dp).background(C06.Ember))
                    }
                    Spacer(Modifier.height(8.dp))
                    Txt(next.title.get(), type.title, maxLines = 1)
                    Txt(tr("S2 · E4 · ${next.remainingMin} min left", "م2 · ح4 · متبقٍ ${next.remainingMin} د"), type.caption)
                }
            }

            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                // Readiness: everything that decides whether tonight will look and sound right.
                C06Module(Modifier.weight(1.05f).fillMaxHeight(), onClick = { session.nav.push(ProtoRoute.Profile) }) {
                    C06Label(tr("Playback readiness", "جاهزية التشغيل"))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                            C06Arc(0.92f, Modifier.fillMaxSize(), C06.Ok)
                            Txt("92", type.readout)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            C06Readout(tr("Display", "الشاشة"), "2160p DV")
                            C06Readout(tr("Audio", "الصوت"), "Atmos")
                            C06Readout(tr("Network", "الشبكة"), "480 Mbps")
                            C06Readout(tr("Debrid", "الخدمة"), tr("211 days", "211 يوماً"))
                        }
                    }
                }
                // Tonight: three picks, each an instrument tile.
                C06Module(Modifier.weight(1.7f).fillMaxHeight(), focusable = false) {
                    C06Label(tr("Tonight", "الليلة"))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                        MockCatalog.tonight.take(3).forEachIndexed { i, t ->
                            C06Tile(t, Modifier.weight(1f), session.focus.requester("tonight:$i"), onFocus = { preview = t; session.focus.lastFocused = "tonight:$i" }) {
                                session.nav.push(ProtoRoute.Details(t.id))
                            }
                        }
                    }
                }
                // Arabic cinema: posters.
                C06Module(Modifier.weight(0.95f).fillMaxHeight(), focusable = false) {
                    C06Label(tr("Arabic cinema", "سينما عربية"))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                        MockCatalog.arabicCinema.take(2).forEachIndexed { i, t ->
                            C06Poster(t, 70.dp, session.focus.requester("arabic:$i"), onFocus = { preview = t; session.focus.lastFocused = "arabic:$i" }) {
                                session.nav.push(ProtoRoute.Details(t.id))
                            }
                        }
                    }
                }
            }

            // Shelf: one long instrument strip.
            C06Module(Modifier.fillMaxWidth(), focusable = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    C06Label(tr("Dolby Vision + Atmos", "دولبي فيجن + أتموس"))
                    Spacer(Modifier.weight(1f))
                    Txt("${MockCatalog.dolbyVisionAtmos.size} ${tr("titles", "عناوين")}", type.caption)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(session.scroll("c06.shelf")), horizontalArrangement = Arrangement.spacedBy(C06.Gap)) {
                    MockCatalog.dolbyVisionAtmos.forEachIndexed { i, t ->
                        C06Tile(t, Modifier.width(150.dp), session.focus.requester("dv:$i"), onFocus = { preview = t; session.focus.lastFocused = "dv:$i" }) {
                            session.nav.push(ProtoRoute.Details(t.id))
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
internal fun C06Readout(label: String, value: String) {
    val type = c06Type()
    Row {
        Txt(label.lbl(), type.label, Modifier.width(78.dp), maxLines = 1)
        Txt(value, type.value, maxLines = 1)
    }
}

@Composable
internal fun C06Tag(text: String) {
    val type = c06Type()
    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xB30B0C0E)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Txt(text.uppercase(), type.label.copy(color = C06.Ink, fontSize = 8.sp), maxLines = 1)
    }
}

/** Landscape tile: art, then a two-line readout. Focus brightens and draws the ember bar. */
@Composable
internal fun C06Tile(t: ProtoTitle, modifier: Modifier, requester: FocusRequester?, onFocus: () -> Unit = {}, onClick: () -> Unit) {
    val type = c06Type()
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Column(modifier.module(f, 8.dp).protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).padding(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(5.dp))) {
            ProtoArtwork(t, ArtKind.BACKDROP, Modifier.fillMaxSize().graphicsLayer { alpha = 0.72f + 0.28f * f })
        }
        Spacer(Modifier.height(6.dp))
        Txt(t.title.get(), type.value.copy(color = if (focused) C06.Ink else C06.Ink2), maxLines = 1)
        Txt("${t.year} · ${if (t.tech.dolbyVision) "DV" else if (t.tech.uhd) "4K" else "HD"} · IMDb ${t.rating}", type.caption, maxLines = 1)
    }
}

@Composable
internal fun C06Poster(t: ProtoTitle, width: Dp, requester: FocusRequester?, onFocus: () -> Unit = {}, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f = detent(focused)
    Column(Modifier.width(width).module(f, 8.dp).protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onClick).padding(4.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(5.dp))) {
            ProtoArtwork(t, ArtKind.POSTER, Modifier.fillMaxSize().graphicsLayer { alpha = 0.72f + 0.28f * f })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0xCC000000))))
            Txt(t.title.get(), c06Type().caption.copy(color = C06.Ink), Modifier.align(Alignment.BottomStart).padding(5.dp), maxLines = 2)
        }
    }
}
