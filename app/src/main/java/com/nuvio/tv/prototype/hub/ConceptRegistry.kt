package com.nuvio.tv.prototype.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.prototype.concept01.Concept01
import com.nuvio.tv.prototype.concept02.Concept02
import com.nuvio.tv.prototype.concept03.Concept03
import com.nuvio.tv.prototype.concept04.Concept04
import com.nuvio.tv.prototype.concept05.Concept05
import com.nuvio.tv.prototype.concept06.Concept06
import com.nuvio.tv.prototype.concept07.Concept07
import com.nuvio.tv.prototype.concept08.Concept08
import com.nuvio.tv.prototype.concept09.Concept09
import com.nuvio.tv.prototype.concept10.Concept10
import com.nuvio.tv.prototype.shared.Bi
import com.nuvio.tv.prototype.shared.ProtoSession

@Immutable
data class ConceptInfo(
    val number: Int,
    val name: Bi,
    val tagline: Bi,
    val idea: Bi,
    val swatches: List<Color>,
    val content: @Composable (ProtoSession) -> Unit,
) {
    val code: String get() = number.toString().padStart(2, '0')
}

object ConceptRegistry {
    val concepts: List<ConceptInfo> = listOf(
        ConceptInfo(
            1, Bi("Cinematic Black", "الأسود السينمائي"),
            Bi("The screen is a cinema. The interface is the title sequence.", "الشاشة صالة سينما، والواجهة هي شارة البداية."),
            Bi("Letterboxed home that opens to full frame; a single cinemascope reel; focus by light, not borders.", "رئيسية بإطار سينمائي عريض ينفتح إلى الشاشة الكاملة؛ شريط واحد بنسبة سينما سكوب؛ التحديد بالضوء لا بالإطارات."),
            listOf(Color(0xFF000000), Color(0xFF1A1A1A), Color(0xFFF2EEE6)),
        ) { Concept01(it) },
        ConceptInfo(
            2, Bi("Riyadh After Dark", "الرياض ليلاً"),
            Bi("A city at night: warm gold light, bilingual by design.", "مدينة في الليل: ضوء ذهبي دافئ، ولغتان بتصميم واحد."),
            Bi("Every heading lives in Arabic and English at once; a hospitality-grade evening concierge picks tonight's lineup.", "كل عنوان يعيش بالعربية والإنجليزية معاً؛ كونسيرج مسائي بمستوى الضيافة الفاخرة يختار برنامج الليلة."),
            listOf(Color(0xFF070A12), Color(0xFFC9A45C), Color(0xFF0F5A47)),
        ) { Concept02(it) },
        ConceptInfo(
            3, Bi("Desert Monolith", "مونوليث الصحراء"),
            Bi("Architecture of calm. Stone, sand, bronze and very large type.", "عمارة من السكينة. حجر ورمل وبرونز وحروف ضخمة."),
            Bi("No card rows: titles are carved in a vertical list of monumental type, artwork seen through a single aperture.", "بلا صفوف بطاقات: العناوين منحوتة في قائمة رأسية من الحروف الضخمة، والصورة تُرى من نافذة واحدة."),
            listOf(Color(0xFF1C1916), Color(0xFFC8B49A), Color(0xFF8C6A43)),
        ) { Concept03(it) },
        ConceptInfo(
            4, Bi("Liquid Cinema", "السينما السائلة"),
            Bi("The artwork paints the interface.", "الصورة ترسم الواجهة."),
            Bi("Adaptive color from the focused title floods the room; glass only where hierarchy needs it; neighbours part around focus.", "ألوان متكيفة من العنوان المحدد تغمر المكان؛ الزجاج فقط حيث يخدم التسلسل البصري؛ البطاقات المجاورة تفسح للعنصر المحدد."),
            listOf(Color(0xFF1B2A4A), Color(0xFFE8A05A), Color(0xFFFFFFFF)),
        ) { Concept04(it) },
        ConceptInfo(
            5, Bi("Zero Chrome", "بلا واجهة"),
            Bi("The content is the interface.", "المحتوى هو الواجهة."),
            Bi("One title fills the screen; left/right changes title, up/down changes channel, Back zooms out to a map of everything.", "عنوان واحد يملأ الشاشة؛ يمين ويسار لتغيير العنوان، أعلى وأسفل لتغيير القناة، والرجوع يبتعد إلى خريطة لكل شيء."),
            listOf(Color(0xFF000000), Color(0xFF3A2416), Color(0xFFE8A05A)),
        ) { Concept05(it) },
        ConceptInfo(
            6, Bi("Luxury Media Console", "كونسول الوسائط الفاخر"),
            Bi("An instrument panel for cinema.", "لوحة قيادة للسينما."),
            Bi("Precise modules, brushed metal and a signal-orange detent focus; information density handled like a car's cluster.", "وحدات دقيقة ومعدن مصقول وتحديد بلون برتقالي إشاري؛ كثافة المعلومات تُدار كلوحة سيارة فاخرة."),
            listOf(Color(0xFF0D0E10), Color(0xFF9EA4AC), Color(0xFFE4572E)),
        ) { Concept06(it) },
        ConceptInfo(
            7, Bi("Editorial Cinema", "السينما التحريرية"),
            Bi("A magazine you watch.", "مجلة تُشاهَد."),
            Bi("A curated issue instead of rows: cover story, asymmetric spreads, pull quotes and a numbered tonight list.", "عدد منسّق بدل الصفوف: قصة غلاف، وصفحات غير متناظرة، واقتباسات، وقائمة مرقّمة لهذه الليلة."),
            listOf(Color(0xFF121110), Color(0xFFF1EBDF), Color(0xFFD9412B)),
        ) { Concept07(it) },
        ConceptInfo(
            8, Bi("Ambient TV OS", "نظام التلفاز المحيطي"),
            Bi("A television that behaves like a lamp.", "تلفاز يتصرف كمصباح."),
            Bi("The room glows with the focused title's colors; a magnifying content dock; a clock-first home designed for OLED.", "الغرفة تتوهج بألوان العنوان المحدد؛ منصة محتوى مكبِّرة؛ رئيسية تبدأ بالساعة ومصممة لشاشات OLED."),
            listOf(Color(0xFF000000), Color(0xFF3B2A5A), Color(0xFFE8B3A0)),
        ) { Concept08(it) },
        ConceptInfo(
            9, Bi("Future Arabia", "مستقبل عربي"),
            Bi("An Arabic-first entertainment operating system.", "نظام ترفيه يبدأ من العربية."),
            Bi("Chamfered geometry, a light-tracing focus, a curved spatial gallery and Arabic display type as the hero.", "هندسة مشطوفة، وتحديد يرسم الضوء على الحواف، ومعرض مكاني منحنٍ، والخط العربي بطلاً للواجهة."),
            listOf(Color(0xFF0A0A0C), Color(0xFFEDE8E0), Color(0xFFB8745A)),
        ) { Concept09(it) },
        ConceptInfo(
            10, Bi("Nuvio Signature", "توقيع نوفيو"),
            Bi("A proposal for what Nuvio should become.", "مقترح لما يجب أن تصبح عليه نوفيو."),
            Bi("OLED black, adaptive ambient light, a signature light-bar focus, a quiet muted-gold accent and intelligent playback everywhere.", "أسود OLED، وضوء محيطي متكيف، وتحديد بشريط ضوئي مميز، ولمسة ذهبية هادئة، وتشغيل ذكي في كل مكان."),
            listOf(Color(0xFF050505), Color(0xFFD8B26E), Color(0xFFF4F1EA)),
        ) { Concept10(it) },
    )
}
