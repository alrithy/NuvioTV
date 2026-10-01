package com.nuvio.tv.updater.model

data class AppUpdate(
    val tag: String,
    val title: String,
    val notes: String,
    val releaseUrl: String?,
    val assetName: String,
    val assetUrl: String,
    val assetSizeBytes: Long?,
    /** G14b (D064): the asset's published SHA-256 (64 hex digits), or null when the release has none. */
    val assetSha256: String? = null
)
