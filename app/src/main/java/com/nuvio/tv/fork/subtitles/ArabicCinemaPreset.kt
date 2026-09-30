package com.nuvio.tv.fork.subtitles

/**
 * Arabic cinema subtitle preset (G6b, feature 105). REWRITE: no pinned source has one.
 *
 * One press writes these values into the official subtitle style settings; nothing changes until
 * the user applies it, and every value stays editable afterwards. Arabic script carries dots and
 * diacritics above and below the baseline, so the preset is larger and bold with a heavier dark
 * outline (legible over bright scenes without a box) and sits a little higher to keep descenders
 * clear of the frame edge. Colors are ARGB ints as stored by official settings.
 */
object ArabicCinemaPreset {
    const val SIZE = 130
    const val VERTICAL_OFFSET = 8
    const val BOLD = true
    const val TEXT_COLOR = 0xFFFFFFFF.toInt()
    const val BACKGROUND_COLOR = 0x00000000
    const val OUTLINE_ENABLED = true
    const val OUTLINE_COLOR = 0xFF000000.toInt()
    const val OUTLINE_WIDTH = 3

    /** Style fields the preset owns, in official setting order. */
    data class Style(
        val size: Int,
        val verticalOffset: Int,
        val bold: Boolean,
        val textColor: Int,
        val backgroundColor: Int,
        val outlineEnabled: Boolean,
        val outlineColor: Int,
        val outlineWidth: Int,
    )

    val style = Style(
        size = SIZE,
        verticalOffset = VERTICAL_OFFSET,
        bold = BOLD,
        textColor = TEXT_COLOR,
        backgroundColor = BACKGROUND_COLOR,
        outlineEnabled = OUTLINE_ENABLED,
        outlineColor = OUTLINE_COLOR,
        outlineWidth = OUTLINE_WIDTH,
    )

    fun isApplied(current: Style): Boolean = current == style
}
