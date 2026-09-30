package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.R
import com.nuvio.tv.data.local.SubtitleStyleSettings
import com.nuvio.tv.fork.subtitles.ArabicCinemaPreset
import com.nuvio.tv.fork.subtitles.SubtitleIntelligence

/**
 * "Arabic cinema preset" row (G6b, feature 105): one press writes [ArabicCinemaPreset] into the
 * official subtitle style settings through the official setters. Hidden while
 * SUBTITLE_INTELLIGENCE is OFF; never applied on its own.
 */
@Composable
internal fun subtitlePresetSettingsItems(
    style: SubtitleStyleSettings,
    enabled: Boolean,
    onUpdate: PlaybackSettingsUpdate,
) {
    if (!SubtitleIntelligence.enabled) return
    val applied = ArabicCinemaPreset.isApplied(
        ArabicCinemaPreset.Style(
            size = style.size,
            verticalOffset = style.verticalOffset,
            bold = style.bold,
            textColor = style.textColor,
            backgroundColor = style.backgroundColor,
            outlineEnabled = style.outlineEnabled,
            outlineColor = style.outlineColor,
            outlineWidth = style.outlineWidth,
        ),
    )
    SettingsActionRow(
        title = stringResource(R.string.subtitle_preset_arabic_cinema),
        subtitle = stringResource(R.string.subtitle_preset_arabic_cinema_desc),
        value = if (applied) stringResource(R.string.subtitle_preset_applied) else null,
        enabled = enabled,
        onClick = {
            val preset = ArabicCinemaPreset.style
            onUpdate {
                setSubtitleSize(preset.size)
                setSubtitleVerticalOffset(preset.verticalOffset)
                setSubtitleBold(preset.bold)
                setSubtitleTextColor(preset.textColor)
                setSubtitleBackgroundColor(preset.backgroundColor)
                setSubtitleOutlineEnabled(preset.outlineEnabled)
                setSubtitleOutlineColor(preset.outlineColor)
                setSubtitleOutlineWidth(preset.outlineWidth)
            }
        },
    )
}
