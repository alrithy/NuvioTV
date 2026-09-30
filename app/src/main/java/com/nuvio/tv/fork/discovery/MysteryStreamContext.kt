package com.nuvio.tv.fork.discovery

import androidx.lifecycle.SavedStateHandle

/** Route-scoped context survives process recreation even though the shuffle picker is transient. */
class MysteryStreamContext(
    private val state: SavedStateHandle,
    selectedByShuffle: Boolean,
    private val enabled: Boolean = ShuffleRules.enabled,
) {
    val shufflePick = enabled && (state.get<Boolean>(PICK) ?: selectedByShuffle)
    var mystery = enabled && (state.get<Boolean>(MYSTERY) ?: shufflePick)
        private set

    init {
        if (enabled) {
            state[PICK] = shufflePick
            state[MYSTERY] = mystery
        }
    }

    fun resolve(mysteryEnabled: Boolean) {
        mystery = enabled && mysteryEnabled
        if (enabled) state[MYSTERY] = mystery
    }

    companion object {
        const val PICK = "fork_shuffle_pick"
        const val MYSTERY = "fork_mystery_pick"
    }
}
