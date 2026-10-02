package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.EpisodeShuffleSettings
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.RandomEpisodePicker
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.fork.discovery.ShuffleRules
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun EpisodeShuffleDialog(
    meta: Meta,
    shuffleSettings: EpisodeShuffleSettings,
    onSaveSettings: suspend (EpisodeShuffleSettings) -> Boolean,
    watchedEpisodes: Set<Pair<Int, Int>>,
    episodeProgress: Map<Pair<Int, Int>, WatchProgress>,
    onDismiss: () -> Unit,
    onPlay: (Video) -> Unit,
    blurUnwatchedEpisodes: Boolean = false,
    showManualPlayOption: Boolean = false,
    onPlayManually: (Video) -> Unit = onPlay,
    onStartFromBeginning: (Video) -> Unit = onPlay,
    // Superfork G9d: last, so official's positional calls (its instrumented tests) keep their meaning.
    onPicked: (Video) -> Unit = {},
    currentSeason: Int? = null,
) {
    // Superfork G9d (171, 173–177): season scope, all-watched fallback and Mystery mode.
    val forkOptions = ShuffleRules.enabled
    val scopeSeason = currentSeason?.takeIf { season -> forkOptions && meta.videos.any { it.season == season } }
    var seasonOnly by remember { mutableStateOf(scopeSeason != null && shuffleSettings.season == scopeSeason) }
    var mystery by remember { mutableStateOf(forkOptions && shuffleSettings.mystery) }
    var fallbackToWatched by remember { mutableStateOf(forkOptions && shuffleSettings.fallbackToWatched) }
    val season = scopeSeason?.takeIf { seasonOnly }
    val picker by produceState<RandomEpisodePicker?>(null, meta.videos, watchedEpisodes, episodeProgress, season) {
        val updated = withContext(Dispatchers.Default) {
            RandomEpisodePicker(meta.id, ShuffleRules.scope(meta.videos, season) { it.season }, watchedEpisodes, episodeProgress)
        }
        updated.inheritHistoryFrom(value)
        value = updated
    }
    var starting by remember { mutableStateOf(false) }
    var selectedEpisode by remember { mutableStateOf<Video?>(null) }
    var includeWatched by remember { mutableStateOf(shuffleSettings.includeWatched) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val readyPicker = picker
    val unwatchedCount = readyPicker?.count(false) ?: 0
    val allCount = readyPicker?.count(true) ?: 0
    val focusAll = includeWatched || unwatchedCount == 0
    val previewEpisode = if (starting) selectedEpisode else selectedEpisode?.let { readyPicker?.find(it.id, includeWatched) }

    fun playEpisode(play: (Video) -> Unit) {
        if (starting) return
        val episode = previewEpisode ?: return
        selectedEpisode = episode
        starting = true
        scope.launch {
            try {
                val settings = EpisodeShuffleSettings(
                    enabled = true, includeWatched = includeWatched,
                    season = season, mystery = mystery, fallbackToWatched = fallbackToWatched
                )
                if (onSaveSettings(settings)) {
                    onPicked(episode)
                    play(episode)
                }
            } finally {
                starting = false
            }
        }
    }

    if (previewEpisode != null) {
        EpisodeShufflePreview(
            meta = meta,
            episode = previewEpisode,
            includeWatched = includeWatched,
            isWatched = readyPicker?.isWatched(previewEpisode) == true,
            isResume = episodeProgress[previewEpisode.season to previewEpisode.episode]?.isInProgress() == true,
            showManualPlayOption = showManualPlayOption,
            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
            mystery = mystery,
            canShuffleAgain = (readyPicker?.count(includeWatched) ?: 0) > 1,
            starting = starting,
            onBack = { if (!starting) selectedEpisode = null },
            onPlay = { playEpisode(onPlay) },
            onPlayManually = { playEpisode(onPlayManually) },
            onStartFromBeginning = { playEpisode(onStartFromBeginning) },
            onShuffleAgain = { if (!starting) selectedEpisode = readyPicker?.pick(includeWatched) }
        )
        return
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.random_episode_title),
        subtitle = stringResource(R.string.shuffle_choose_episodes),
        width = 420.dp
    ) {
        LaunchedEffect(readyPicker != null, focusAll) {
            if (readyPicker != null) focusRequester.requestFocusAfterFrames()
        }
        when {
            readyPicker == null -> Text(stringResource(R.string.random_episode_loading))
            allCount == 0 -> {
                Text(stringResource(R.string.random_episode_empty_subtitle))
                Button(onClick = onDismiss, modifier = Modifier.focusRequester(focusRequester)) {
                    Text(stringResource(R.string.action_close))
                }
            }
            else -> {
                for (include in listOf(false, true)) {
                    Button(
                        onClick = {
                            includeWatched = include
                            selectedEpisode = readyPicker.pick(include)
                        },
                        enabled = include || unwatchedCount > 0,
                        modifier = Modifier.fillMaxWidth()
                            .then(if (include == focusAll) Modifier.focusRequester(focusRequester) else Modifier),
                        colors = ButtonDefaults.colors(
                            containerColor = NuvioTheme.colors.BackgroundCard,
                            contentColor = NuvioTheme.colors.TextPrimary
                        )
                    ) {
                        Text(stringResource(if (include) R.string.random_episode_include_watched else R.string.random_episode_unwatched))
                    }
                }
                if (unwatchedCount == 0) Text(stringResource(R.string.random_episode_caught_up))
                if (forkOptions) {
                    scopeSeason?.let { current ->
                        ShuffleOptionButton(
                            label = stringResource(R.string.shuffle_season_only, current),
                            on = seasonOnly,
                            onToggle = { seasonOnly = !seasonOnly; selectedEpisode = null }
                        )
                    }
                    ShuffleOptionButton(stringResource(R.string.shuffle_mystery_mode), mystery) { mystery = !mystery }
                    ShuffleOptionButton(stringResource(R.string.shuffle_fallback_watched), fallbackToWatched) {
                        fallbackToWatched = !fallbackToWatched
                    }
                }
            }
        }
    }
}

/** Superfork G9d: an on/off option in the shuffle dialog. */
@Composable
private fun ShuffleOptionButton(label: String, on: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            contentColor = NuvioTheme.colors.TextPrimary
        )
    ) {
        Text(stringResource(if (on) R.string.shuffle_option_on else R.string.shuffle_option_off, label))
    }
}
