package com.nuvio.tv.prototype.hub

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.Glyph
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.LocalProtoLang
import com.nuvio.tv.prototype.shared.ProtoIcon
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoScreen
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.art.filmGrain
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr
import com.nuvio.tv.prototype.shared.ts

private val HubBg = Color(0xFF0A0A0B)
private val HubInk = Color(0xFFF3F1EC)
private val HubMuted = Color(0xFF8E8C88)
private val HubLine = Color(0x1FFFFFFF)

@Composable
fun PrototypeHub(state: PrototypeState) {
    val fonts = LocalProtoFonts.current
    val ar = isArabic()
    val body = if (ar) fonts.plexArabic else fonts.geist
    val display = if (ar) fonts.alexandria else fonts.geist
    val requesters = remember { List(10) { FocusRequester() } }
    var selected by remember { mutableStateOf(state.hubFocus) }
    RequestFocusOnce(requesters[(state.hubFocus - 1).coerceIn(0, 9)])

    Box(
        Modifier
            .fillMaxSize()
            .background(HubBg)
            .filmGrain(0.05f),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x33D8B26E), Color.Transparent), center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.6f),
                radius = size.width * 0.6f, center = Offset(size.width * 0.85f, 0f),
            )
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 34.dp)) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Txt("NUVIO  ·  " + tr("DESIGN DIRECTIONS", "اتجاهات التصميم"), ts(body, 10.sp, FontWeight.Medium, HubMuted, if (ar) 0.em else 0.32.em))
                    Spacer(Modifier.height(6.dp))
                    Txt(tr("Ten products. One catalog.", "عشرة منتجات. محتوى واحد."), ts(display, 30.sp, FontWeight.Light, HubInk, (-0.01).em, lineHeight = 40.sp))
                    Txt(
                        tr("Open a concept, then use the remote as you would at home. Nothing here touches the production UI.", "افتح أي مفهوم واستخدم جهاز التحكم كما في المنزل. لا شيء هنا يمس الواجهة الحالية للتطبيق."),
                        ts(body, 12.sp, color = HubMuted, lineHeight = 18.sp),
                    )
                }
                Spacer(Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HubToggle("EN", state.lang == ProtoLang.EN, body) { state.lang = ProtoLang.EN }
                        HubToggle("العربية", state.lang == ProtoLang.AR, fonts.plexArabic) { state.lang = ProtoLang.AR }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HubToggle(tr("Live artwork", "صور حية"), state.remoteArt, body) { state.remoteArt = true }
                        HubToggle(tr("Illustrated", "رسوم"), !state.remoteArt, body) { state.remoteArt = false }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            // Concept grid
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 5) {
                        val info = ConceptRegistry.concepts[row * 5 + col]
                        ConceptTile(
                            info = info,
                            requester = requesters[info.number - 1],
                            modifier = Modifier.weight(1f),
                            onFocus = { selected = info.number; state.hubFocus = info.number },
                            onOpen = { state.open(info.number, ProtoScreen.HOME) },
                        )
                    }
                }
                if (row == 0) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(HubLine))
            Spacer(Modifier.height(12.dp))
            // Focused concept description
            val info = ConceptRegistry.concepts[(selected - 1).coerceIn(0, 9)]
            AnimatedContent(info, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) }, label = "desc") { c ->
                Row(verticalAlignment = Alignment.Top) {
                    Txt(c.code, ts(fonts.geist, 30.sp, FontWeight.Light, HubInk, lineHeight = 32.sp), Modifier.width(56.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(c.name.get() + "  —  " + c.tagline.get(), ts(body, 14.sp, FontWeight.Medium, HubInk, lineHeight = 20.sp), maxLines = 1)
                        Txt(c.idea.get(), ts(body, 12.sp, color = HubMuted, lineHeight = 18.sp), maxLines = 2)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Jump strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(tr("OPEN AT", "افتح على"), ts(body, 10.sp, FontWeight.Medium, HubMuted, if (ar) 0.em else 0.2.em), Modifier.width(72.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProtoScreen.entries.forEach { s ->
                        HubChip(s.label.get(), body) { state.open(selected, s) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Txt(
                tr(
                    "Remote: BACK returns here  ·  MENU opens the quick switcher  ·  1–0 jump to a concept  ·  CH± next / previous",
                    "جهاز التحكم: الرجوع يعيدك هنا  ·  القائمة تفتح المبدّل السريع  ·  الأرقام ١–٠ للانتقال إلى مفهوم  ·  القنوات للتالي والسابق",
                ),
                ts(body, 10.sp, color = HubMuted.copy(alpha = 0.7f)),
            )
        }
    }
}

@Composable
private fun ConceptTile(
    info: ConceptInfo,
    requester: FocusRequester,
    modifier: Modifier,
    onFocus: () -> Unit,
    onOpen: () -> Unit,
) {
    val fonts = LocalProtoFonts.current
    val ar = LocalProtoLang.current == ProtoLang.AR
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(220), label = "s")
    val border by animateColorAsState(if (focused) HubInk else Color.Transparent, tween(200), label = "b")
    val lift by animateDpAsState(if (focused) (-4).dp else 0.dp, tween(220), label = "l")
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; translationY = lift.toPx() }
            .protoFocusable(requester, onFocusChange = { focused = it; if (it) onFocus() }, onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, border, RoundedCornerShape(10.dp)),
        ) {
            ConceptThumbnail(info.number, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(info.code, ts(fonts.geist, 11.sp, FontWeight.Medium, if (focused) HubInk else HubMuted))
            Spacer(Modifier.width(6.dp))
            Txt(
                info.name.get(),
                ts(if (ar) fonts.plexArabic else fonts.geist, 12.sp, FontWeight.Medium, if (focused) HubInk else HubInk.copy(alpha = 0.75f)),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HubToggle(label: String, active: Boolean, family: androidx.compose.ui.text.font.FontFamily, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> HubInk
        active -> Color(0x26FFFFFF)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (active || focused) Color.Transparent else HubLine, RoundedCornerShape(50))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Txt(label, ts(family, 11.sp, FontWeight.Medium, if (focused) HubBg else HubInk))
    }
}

@Composable
private fun HubChip(label: String, family: androidx.compose.ui.text.font.FontFamily, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) HubInk else Color(0x14FFFFFF))
            .protoFocusable(onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, ts(family, 10.sp, FontWeight.Medium, if (focused) HubBg else HubInk.copy(alpha = 0.85f)), maxLines = 1)
        if (focused) {
            Spacer(Modifier.width(4.dp))
            ProtoIcon(Glyph.CHEVRON_RIGHT, size = 10.dp, color = HubBg, stroke = 2.dp)
        }
    }
}
