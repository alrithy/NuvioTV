package com.nuvio.tv.ui.screens.livetv

import android.content.Context
import com.nuvio.tv.R
import com.nuvio.tv.fork.livetv.LiveTvError

/** A load failure as a translated message (Reshaped `LiveTvScreenModel` @ 0ccf049). */
internal fun LiveTvError.message(context: Context): String = context.getString(
    when (this) {
        LiveTvError.InvalidUrl -> R.string.live_tv_error_invalid_url
        LiveTvError.NoChannels -> R.string.live_tv_error_no_channels
        LiveTvError.LoadFailed -> R.string.live_tv_error_load_failed
        LiveTvError.FileEmpty -> R.string.live_tv_error_file_empty
        LiveTvError.FileNoChannels -> R.string.live_tv_error_file_no_channels
        LiveTvError.StalkerRequired -> R.string.live_tv_error_stalker_required
        LiveTvError.StalkerInvalidUrl -> R.string.live_tv_error_stalker_invalid_url
        LiveTvError.StalkerNoChannels -> R.string.live_tv_error_stalker_no_channels
        LiveTvError.StalkerFailed -> R.string.live_tv_error_stalker_failed
        LiveTvError.StalkerToken -> R.string.live_tv_error_stalker_token
        LiveTvError.StalkerIncomplete -> R.string.live_tv_error_stalker_incomplete
        LiveTvError.XtreamRequired -> R.string.live_tv_error_xtream_required
        LiveTvError.XtreamInvalidUrl -> R.string.live_tv_error_xtream_invalid_url
        LiveTvError.XtreamNoChannels -> R.string.live_tv_error_xtream_no_channels
        LiveTvError.XtreamFailed -> R.string.live_tv_error_xtream_failed
    },
)
