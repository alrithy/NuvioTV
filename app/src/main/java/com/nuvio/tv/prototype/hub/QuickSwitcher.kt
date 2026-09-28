package com.nuvio.tv.prototype.hub

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.prototype.shared.LocalProtoFonts
import com.nuvio.tv.prototype.shared.ProtoBackHandler
import com.nuvio.tv.prototype.shared.ProtoLang
import com.nuvio.tv.prototype.shared.ProtoScreen
import com.nuvio.tv.prototype.shared.RequestFocusOnce
import com.nuvio.tv.prototype.shared.Txt
import com.nuvio.tv.prototype.shared.isArabic
import com.nuvio.tv.prototype.shared.protoFocusable
import com.nuvio.tv.prototype.shared.tr
import com.nuvio.tv.prototype.shared.ts

/** Side panel for switching concept, entry screen and language without leaving the TV. */
@Composable
fun QuickSwitcher(state: PrototypeState) {
    val fonts = LocalProtoFonts.current
    val ar = isArabic()
    val family = if (ar) fonts.plexArabic else fonts.geist
    val first = remember { FocusRequester() }
    ProtoBackHandler { state.switcherOpen = false }
    RequestFocusOnce(first)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0x99000000), Color(0xEE000000)))),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .width(560.dp)
                .background(Color(0xF20E0E10))
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.weight(1.1f)) {
                Txt(tr("CONCEPT", "المفهوم"), ts(family, 10.sp, FontWeight.Medium, Color(0xFF8E8C88), if (ar) 0.em else 0.24.em))
                Spacer(Modifier.height(10.dp))
                ConceptRegistry.concepts.forEach { c ->
                    SwitchRow(
                        label = "${c.code}   ${c.name.get()}",
                        active = state.concept == c.number,
                        family = family,
                        requester = if (c.number == (state.concept ?: 1)) first else null,
                    ) { state.open(c.number, state.screen) }
                }
            }
            Column(Modifier.weight(1f)) {
                Txt(tr("OPEN AT", "افتح على"), ts(family, 10.sp, FontWeight.Medium, Color(0xFF8E8C88), if (ar) 0.em else 0.24.em))
                Spacer(Modifier.height(10.dp))
                ProtoScreen.entries.forEach { s ->
                    SwitchRow(s.label.get(), state.screen == s, family) { state.open(state.concept ?: 1, s) }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) {
                        SwitchRow("English", state.lang == ProtoLang.EN, fonts.geist) { state.lang = ProtoLang.EN; state.switcherOpen = false }
                    }
                    Box(Modifier.weight(1f)) {
                        SwitchRow("العربية", state.lang == ProtoLang.AR, fonts.plexArabic) { state.lang = ProtoLang.AR; state.switcherOpen = false }
                    }
                }
                SwitchRow(tr("Back to hub", "العودة إلى القائمة"), false, family) { state.toHub() }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    active: Boolean,
    family: androidx.compose.ui.text.font.FontFamily,
    requester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color(0xFFF3F1EC) else if (active) Color(0x1AFFFFFF) else Color.Transparent)
            .protoFocusable(requester, onFocusChange = { focused = it }, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Txt(label, ts(family, 12.sp, if (active) FontWeight.Medium else FontWeight.Normal, if (focused) Color(0xFF0A0A0B) else Color(0xFFF3F1EC)), maxLines = 1)
    }
}
