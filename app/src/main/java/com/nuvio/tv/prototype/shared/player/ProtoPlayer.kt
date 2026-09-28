package com.nuvio.tv.prototype.shared.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.tv.prototype.shared.LocalProtoEnv
import com.nuvio.tv.prototype.shared.data.AudioTrack
import com.nuvio.tv.prototype.shared.data.Chapter
import com.nuvio.tv.prototype.shared.data.MockCatalog
import com.nuvio.tv.prototype.shared.data.ProtoStream
import com.nuvio.tv.prototype.shared.data.ProtoTitle
import com.nuvio.tv.prototype.shared.data.StreamIntelligence
import com.nuvio.tv.prototype.shared.data.SubtitleTrack
import kotlinx.coroutines.delay
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.nuvio.tv.prototype.shared.isBack
import com.nuvio.tv.prototype.shared.isDown
import com.nuvio.tv.prototype.shared.isEnter
import com.nuvio.tv.prototype.shared.isUp

/**
 * Mock playback state shared by every concept's player. Position ticks in real time on TV and
 * is frozen for screenshots. Scrubbing is separate from position so concepts can preview frames
 * before committing a seek.
 */
@Stable
class ProtoPlayerState(val title: ProtoTitle, startFraction: Float) {
    val durationSec: Int = (if (title.isSeries) (title.resume?.runtimeMin ?: title.runtimeMin) else title.runtimeMin) * 60
    var position by mutableFloatStateOf(startFraction * durationSec)
    var playing by mutableStateOf(true)
    var controlsVisible by mutableStateOf(true)
    var scrubbing by mutableStateOf(false)
    var scrubPosition by mutableFloatStateOf(position)
    var stream: ProtoStream by mutableStateOf(StreamIntelligence.best(StreamIntelligence.streamsFor(title)).stream)
    var subtitle: SubtitleTrack? by mutableStateOf(MockCatalog.subtitles.first())
    var audio: AudioTrack by mutableStateOf(MockCatalog.audioTracks.first())
    var lastInteraction by mutableStateOf(0L)

    val chapters: List<Chapter> get() = title.chapters
    val fraction: Float get() = (position / durationSec).coerceIn(0f, 1f)
    val scrubFraction: Float get() = (scrubPosition / durationSec).coerceIn(0f, 1f)
    val displayFraction: Float get() = if (scrubbing) scrubFraction else fraction
    val remainingSec: Int get() = (durationSec - position).toInt()

    fun chapterAt(f: Float): Int = chapters.indexOfLast { it.startFraction <= f }.coerceAtLeast(0)
    val currentChapter: Int get() = chapterAt(displayFraction)

    fun poke() {
        lastInteraction = System.nanoTime()
        controlsVisible = true
    }

    fun togglePlay() {
        playing = !playing
        poke()
    }

    fun beginScrub() {
        if (!scrubbing) {
            scrubbing = true
            scrubPosition = position
        }
    }

    fun scrubBy(seconds: Float) {
        beginScrub()
        scrubPosition = (scrubPosition + seconds).coerceIn(0f, durationSec.toFloat())
        poke()
    }

    fun commitScrub() {
        if (scrubbing) {
            position = scrubPosition
            scrubbing = false
        }
        poke()
    }

    fun cancelScrub() {
        scrubbing = false
        poke()
    }

    fun seekBy(seconds: Float) {
        position = (position + seconds).coerceIn(0f, durationSec.toFloat())
        poke()
    }

    fun jumpToChapter(index: Int) {
        val c = chapters.getOrNull(index) ?: return
        beginScrub()
        scrubPosition = c.startFraction * durationSec
        poke()
    }
}

@Composable
fun rememberProtoPlayer(title: ProtoTitle, autoHideMs: Long = 4500): ProtoPlayerState {
    val frozen = LocalProtoEnv.current.frozen
    val state = remember(title.id) { ProtoPlayerState(title, title.progress ?: 0.42f) }
    if (!frozen) {
        LaunchedEffect(state) {
            while (true) {
                delay(1000)
                if (state.playing && !state.scrubbing) state.position = (state.position + 1f).coerceAtMost(state.durationSec.toFloat())
            }
        }
        LaunchedEffect(state.lastInteraction, state.playing, state.scrubbing) {
            if (state.playing && !state.scrubbing) {
                delay(autoHideMs)
                state.controlsVisible = false
            }
        }
    }
    return state
}

/** On-screen keyboard layouts for Search. Arabic gets its own alphabet, not a transliteration. */
object ProtoKeyboard {
    val english: List<String> = ("abcdefghijklmnopqrstuvwxyz").map { it.toString() } + listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val arabic: List<String> = listOf(
        "ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط",
        "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي", "ة", "ى", "ء", "أ",
    )
    val digits: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    fun keys(arabic: Boolean): List<String> = if (arabic) this.arabic else english
}

/**
 * Wakes hidden player chrome: the first key press only reveals controls (it is consumed),
 * later presses keep them alive. Back is never swallowed.
 */
fun androidx.compose.ui.Modifier.playerWake(state: ProtoPlayerState): androidx.compose.ui.Modifier =
    this.then(
        androidx.compose.ui.Modifier.onPreviewKeyEvent { e ->
            if (e.isBack) return@onPreviewKeyEvent false
            if (!state.controlsVisible) {
                if (e.isDown) state.poke()
                true
            } else {
                if (e.isDown) state.lastInteraction = System.nanoTime()
                false
            }
        },
    )

/** Timeline keys: left/right scrub (preview only), centre commits the seek or toggles play. */
fun androidx.compose.ui.Modifier.scrubKeys(state: ProtoPlayerState, stepSec: Float = 30f): androidx.compose.ui.Modifier =
    this.then(
        androidx.compose.ui.Modifier.onKeyEvent { e ->
            when {
                e.key == Key.DirectionRight && e.isDown -> { state.scrubBy(stepSec); true }
                e.key == Key.DirectionLeft && e.isDown -> { state.scrubBy(-stepSec); true }
                e.isEnter && e.isUp -> { if (state.scrubbing) state.commitScrub() else state.togglePlay(); true }
                e.isEnter -> true
                else -> false
            }
        },
    )
