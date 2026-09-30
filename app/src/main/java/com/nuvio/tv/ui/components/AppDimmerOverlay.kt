package com.nuvio.tv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.nuvio.tv.fork.dimmer.AppDimmerRules

/** Superfork G9f: the current dimmer level, so dialog windows can dim themselves too. */
val LocalAppDimPercent = compositionLocalOf { 0 }

/**
 * Superfork G9f (206): Cxsmo `AppDimmerOverlay` @ 3e0d0fa. One black draw pass above everything in its
 * window; it has no pointer or key handling, so input reaches the screen below.
 */
@Composable
fun AppDimmerOverlay(dimPercent: Int, modifier: Modifier = Modifier) {
    val alpha = AppDimmerRules.alpha(dimPercent)
    if (alpha <= 0f) return
    Canvas(modifier = modifier.fillMaxSize().zIndex(Float.MAX_VALUE)) {
        drawRect(Color.Black.copy(alpha = alpha))
    }
}
