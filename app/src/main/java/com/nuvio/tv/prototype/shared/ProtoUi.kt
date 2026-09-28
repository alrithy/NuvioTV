package com.nuvio.tv.prototype.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Thin wrapper over BasicText so concepts never pick up Material defaults. */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    minLines: Int = 1,
) {
    var s = style
    if (color != Color.Unspecified) s = s.copy(color = color)
    if (align != null) s = s.copy(textAlign = align)
    BasicText(text = text, modifier = modifier, style = s, maxLines = maxLines, overflow = overflow, minLines = minLines)
}

/** Builds a TextStyle with the usual TV-safe defaults. */
fun ts(
    family: androidx.compose.ui.text.font.FontFamily,
    size: TextUnit,
    weight: androidx.compose.ui.text.font.FontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
    color: Color = Color.White,
    tracking: TextUnit = 0.em,
    lineHeight: TextUnit = TextUnit.Unspecified,
    italic: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size,
    fontWeight = weight,
    color = color,
    letterSpacing = tracking,
    lineHeight = if (lineHeight == TextUnit.Unspecified) (size.value * 1.3f).sp else lineHeight,
    fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
    // Bilingual UI: each string picks its own paragraph direction from its first strong character,
    // so English inside an Arabic layout (and vice versa) keeps punctuation on the correct side.
    textDirection = androidx.compose.ui.text.style.TextDirection.Content,
)

/** Arabic script needs taller line boxes than Latin at the same size. */
fun TextStyle.forArabic(arabic: Boolean, lineFactor: Float = 1.55f): TextStyle =
    if (!arabic) this else copy(letterSpacing = 0.em, lineHeight = (fontSize.value * lineFactor).sp)

/** Stable content key: player panels open inside the player rather than as new screens. */
fun ProtoRoute.contentKey(): Any = when (this) {
    is ProtoRoute.Player -> "player:$titleId"
    else -> this
}

/**
 * Hosts a concept's screens. Back pops the concept's own stack; the hub handles the root.
 * Every concept supplies its own transition so navigation feels different per product.
 */
@Composable
fun ConceptHost(
    session: ProtoSession,
    modifier: Modifier = Modifier,
    pivot: ScrollPivot? = null,
    transition: AnimatedContentTransitionScope<ProtoRoute>.(forward: Boolean) -> ContentTransform,
    content: @Composable (ProtoRoute) -> Unit,
) {
    val nav = session.nav
    ProtoBackHandler(enabled = nav.canPop) { nav.pop() }
    val body: @Composable () -> Unit = {
        AnimatedContent(
            targetState = nav.current,
            modifier = modifier.fillMaxSize(),
            transitionSpec = { transition(nav.forward) },
            contentKey = { it.contentKey() },
            contentAlignment = Alignment.Center,
            label = "concept",
        ) { route ->
            Box(Modifier.fillMaxSize()) { content(route) }
        }
    }
    if (pivot != null) {
        ProvideScrollPivot(pivot) { body() }
    } else {
        body()
    }
}

/** Small focus-state holder to cut boilerplate in cards. */
class FocusFlag {
    var value by mutableStateOf(false)
}

@Composable
fun rememberFocusFlag(): FocusFlag = remember { FocusFlag() }

/**
 * Horizontal gradient that starts on the layout's start edge (left in LTR, right in RTL).
 * Scrims behind text must follow the reading direction.
 */
@Composable
fun startScrim(vararg stops: Pair<Float, Color>): androidx.compose.ui.graphics.Brush {
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val s = if (rtl) stops.map { (1f - it.first) to it.second }.reversed().toTypedArray() else arrayOf(*stops)
    return androidx.compose.ui.graphics.Brush.horizontalGradient(*s)
}

/** Title metadata line in the current language: "2024 · 2h 46m · PG-13 · Science Fiction". */
@Composable
fun metaLine(t: com.nuvio.tv.prototype.shared.data.ProtoTitle, separator: String = "  ·  ", withGenre: Boolean = true): String {
    val lang = LocalProtoLang.current
    val parts = buildList {
        add(t.year.toString())
        if (t.isSeries) add(if (lang == ProtoLang.AR) "${t.seasons.size} ${if (t.seasons.size == 1) "موسم" else "مواسم"}" else "${t.seasons.size} Season${if (t.seasons.size == 1) "" else "s"}")
        else add(formatRuntime(t.runtimeMin, lang))
        add(t.cert)
        if (withGenre) add(t.primaryGenre.of(lang))
    }
    return parts.joinToString(separator)
}

/** Continue-watching status line in the current language. */
@Composable
fun resumeLine(t: com.nuvio.tv.prototype.shared.data.ProtoTitle): String? {
    val lang = LocalProtoLang.current
    val remaining = t.remainingMin ?: return null
    val ep = t.resume
    return if (lang == ProtoLang.AR) {
        if (ep != null) "الموسم ${ep.season} · الحلقة ${ep.number} · متبقٍ $remaining د" else "متبقٍ $remaining دقيقة"
    } else {
        if (ep != null) "S${ep.season} · E${ep.number} · $remaining min left" else "$remaining min left"
    }
}

/** First unwatched "new" episode, for new-episode badges. */
fun com.nuvio.tv.prototype.shared.data.ProtoTitle.newEpisode(): com.nuvio.tv.prototype.shared.data.Episode? =
    seasons.flatMap { it.episodes }.firstOrNull { it.isNew }
