package com.nuvio.tv.fork.uistyle

import android.os.SystemClock

/**
 * The app's one screensaver (G12d, feature 292), shared by the activity's key and focus hooks, the
 * player's playback state and the trailer pool. The decisions are [ScreensaverMachine]'s.
 */
object Screensaver {
    val machine = ScreensaverMachine { SystemClock.elapsedRealtime() }
}
