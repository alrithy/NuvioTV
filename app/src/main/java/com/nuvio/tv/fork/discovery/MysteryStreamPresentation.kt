package com.nuvio.tv.fork.discovery

import com.nuvio.tv.domain.model.Stream

/** Display-only neutral source card. Playback callbacks retain the real source object. */
fun mysteryStreamPresentation(label: String) = Stream(
    name = label, title = null, description = null, url = null, ytId = null,
    infoHash = null, fileIdx = null, externalUrl = null, behaviorHints = null,
    addonName = "", addonLogo = null,
)
