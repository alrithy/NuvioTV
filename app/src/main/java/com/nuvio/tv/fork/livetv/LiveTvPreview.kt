package com.nuvio.tv.fork.livetv

import com.nuvio.tv.fork.resource.LiveTvPreviewBudget

/*
 * The Live TV list's channel preview (G10f, features 226, 227). Settings and rules of Reshaped
 * `LiveTvPreferences` / `LiveTvPreviewPanel` @ 0ccf049, stored per profile beside the menu switch,
 * with the default and the size cap from AdaptiveResources ([LiveTvPreviewBudget]).
 */

/** What the viewer chose; [previews] stays null until they choose, so the device's default applies. */
data class LiveTvPreviewChoice(
    val previews: Boolean? = null,
    val sound: Boolean = true,
)

/** Whether the list previews the focused channel, and with its sound. */
data class LiveTvPreviewSettings(
    val previews: Boolean,
    val sound: Boolean,
) {
    companion object {
        val OFF = LiveTvPreviewSettings(previews = false, sound = false)

        fun resolve(choice: LiveTvPreviewChoice, budget: LiveTvPreviewBudget) =
            LiveTvPreviewSettings(previews = choice.previews ?: budget.onByDefault, sound = choice.sound)
    }
}

internal object LiveTvPreviewRules {
    /** How long focus rests on a channel before its preview starts, so scrolling opens no streams. */
    const val DELAY_MS = 400L

    /**
     * Stalker links are made per play, and portals flag a device that asks for many: those channels
     * show their logo and what is on now, without a picture.
     */
    fun canPreview(channel: LiveTvChannel): Boolean = channel.stalkerCommand == null

    /** A link without .m3u8 can still be HLS: the preview tries that once after an error. */
    fun looksHls(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)
}
