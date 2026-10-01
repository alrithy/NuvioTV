package com.nuvio.tv.ui.screens.uistyle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex

/**
 * G12d screensaver (feature 292): a black scrim faded in over everything, the App Dimmer included.
 * FILE_PORT of Cxsmo-ai/NuvioTV-Custom @ 3e0d0fa `ScreensaverOverlay`. Purely visual: not focusable
 * and consumes nothing; the waking key press is swallowed in the activity's key dispatch.
 */
@Composable
fun ScreensaverOverlay(visible: Boolean, dimPercent: Int, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) dimPercent.coerceIn(0, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = if (visible) 1_400 else 250),
        label = "screensaverDim",
    )
    if (alpha <= 0.001f) return
    Canvas(modifier = modifier.fillMaxSize().zIndex(Float.MAX_VALUE)) {
        drawRect(Color.Black.copy(alpha = alpha))
    }
}
