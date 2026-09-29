package com.nuvio.tv.fork.resource

import android.app.ActivityManager
import android.content.Context
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * Reads physical RAM the same way official `NuvioApplication.newImageLoader` does
 * (`ActivityManager.MemoryInfo.totalMem`) plus `isLowRamDevice`, which is opt-in and false on
 * most 2 GB TV boxes, so the RAM cut has to back it up. Runs before Hilt injection, so it reads
 * the registry defaults directly; no user override store exists yet.
 */
fun AdaptiveResources.install(context: Context) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val totalRamBytes = activityManager
        ?.let { ActivityManager.MemoryInfo().also(it::getMemoryInfo).totalMem }
        ?: 0L
    install(
        totalRamBytes = totalRamBytes,
        isLowRamDevice = activityManager?.isLowRamDevice == true,
        mode = FeatureRegistry().mode(FeatureId.ADAPTIVE_RESOURCE_MANAGER)
    )
}
