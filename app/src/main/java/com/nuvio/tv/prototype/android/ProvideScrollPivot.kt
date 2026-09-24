package com.nuvio.tv.prototype.shared

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

/** Installs a concept's scroll pivot for every scrollable below it (Android implementation). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideScrollPivot(pivot: ScrollPivot, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val spec = remember(pivot, density) { PivotSpec(pivot, density.density) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
private class PivotSpec(private val pivot: ScrollPivot, private val density: Float) : BringIntoViewSpec {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override val scrollAnimationSpec: AnimationSpec<Float> = tween(pivot.durationMs, easing = pivot.easing)

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (containerSize <= 0f) return 0f
        return when (pivot) {
            is ScrollPivot.Anchor -> offset - (containerSize * pivot.fraction + pivot.offsetDp * density)
            is ScrollPivot.Center -> (offset + size / 2f) - containerSize / 2f
            is ScrollPivot.Padded -> {
                val pad = pivot.paddingDp * density
                val leading = offset - pad
                val trailing = offset + size + pad - containerSize
                when {
                    leading < 0f -> leading
                    trailing > 0f -> trailing
                    else -> 0f
                }
            }
        }
    }
}
