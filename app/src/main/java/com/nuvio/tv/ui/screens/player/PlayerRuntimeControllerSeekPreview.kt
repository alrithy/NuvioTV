@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.tv.ui.screens.player

import androidx.media3.extractor.ExtractorsFactory
import com.nuvio.tv.fork.seek.SeekIntelligence
import com.nuvio.tv.fork.seek.local.LocalPreviewSources

/**
 * G7a (108, 109): registers this ExoPlayer stream for on-device seek previews and wraps its
 * extractors so the video keyframes playback already downloads are copied to the preview track
 * (forwarded unchanged; nothing is downloaded twice). SEEK_INTELLIGENCE OFF returns [delegate]
 * untouched. MPV never gets here, so it keeps the official scrubber.
 */
internal fun PlayerRuntimeController.seekPreviewExtractorsFactory(
    delegate: ExtractorsFactory,
    url: String,
): ExtractorsFactory {
    if (!SeekIntelligence.enabled) return delegate
    return LocalPreviewSources.register(
        owner = this,
        context = context,
        sourceKey = url,
        factory = delegate,
    )
}
