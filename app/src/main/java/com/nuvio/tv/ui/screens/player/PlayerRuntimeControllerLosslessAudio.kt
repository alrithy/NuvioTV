package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.fork.audio.AudioTrackCandidate
import com.nuvio.tv.fork.audio.LosslessAudioDefault
import kotlinx.coroutines.flow.update

/**
 * G5b (feature 37): applies the lossless default once per stream, after the persisted/remembered
 * restore pass, on both engines' tracks-ready paths. Ported from ysosrs 45e0984
 * `applyLosslessAudioDefaultIfUnset`. Runs only while the user turned "Prefer lossless audio" on;
 * it selects through the same [selectAudioTrack] as any other selection and is never remembered.
 */
internal fun PlayerRuntimeController.applyLosslessAudioDefaultIfUnset(audioTracks: List<TrackInfo>) {
    if (!preferLosslessAudioDefault || audioTracks.isEmpty() || losslessAudioDefaultAppliedForStream) return
    losslessAudioDefaultAppliedForStream = true

    if (rememberedTrackPreference?.audio != null || persistedTrackPreference?.audio != null ||
        persistedAudioPreferenceSeenForStream || pendingEngineSwitchTrackPreference != null
    ) {
        logSwitchTrace(stage = "lossless-default", message = "result=skip reason=preference-present")
        return
    }

    val pick = LosslessAudioDefault.pickDefaultIndex(
        tracks = audioTracks.map { AudioTrackCandidate(it.index, it.codec, it.name, it.language, it.channelCount) },
        preferredLanguages = mpvPreferredAudioLanguages,
        languageMatches = { trackLanguage, preferred -> PlayerSubtitleUtils.matchesLanguageCode(trackLanguage, preferred) }
    )
    val selectedIndex = audioTracks.indexOfFirst { it.isSelected }.takeIf { it >= 0 }
        ?: _uiState.value.selectedAudioTrackIndex
    if (pick == null || pick == selectedIndex) {
        logSwitchTrace(
            stage = "lossless-default",
            message = "result=${if (pick == null) "none" else "already-selected"} pick=$pick selected=$selectedIndex"
        )
        return
    }
    val target = audioTracks[pick]
    logSwitchTrace(
        stage = "lossless-default",
        message = "result=apply index=$pick codec=${target.codec} lang=${target.language} " +
            "channels=${target.channelCount} was=$selectedIndex"
    )
    selectAudioTrack(pick)
    _uiState.update { it.copy(selectedAudioTrackIndex = pick) }
}
