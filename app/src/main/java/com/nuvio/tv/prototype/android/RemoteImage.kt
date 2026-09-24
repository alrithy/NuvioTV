package com.nuvio.tv.prototype.shared.art

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Remote artwork layer (Android). Kept in the android/ folder so the pure-Compose concept code
 * can also be compiled and rendered by a desktop harness that substitutes a no-op implementation.
 */
@Composable
internal fun RemoteImage(url: String, alignment: Alignment, modifier: Modifier, onLoaded: () -> Unit) {
    val ctx = LocalPlatformContext.current
    val request = remember(url) { ImageRequest.Builder(ctx).data(url).crossfade(450).build() }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier = modifier,
        onSuccess = { onLoaded() },
    )
}
