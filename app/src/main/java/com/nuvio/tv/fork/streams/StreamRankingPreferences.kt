package com.nuvio.tv.fork.streams

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.fork.diagnostics.AddonHealthTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G8b stream-ranking inputs: the per-profile "Best quality" list order (`fork_streams`, default
 * off = official add-on order, feature 167) and the per-load [StreamRankContext] (display, add-on
 * health, and the G8c connection estimate). Nothing is written
 * into the official PlayerSettings store; the Best-quality autoplay mode lives there as an official
 * mode value (D053).
 */
@Singleton
class StreamRankingPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
    private val healthTracker: AddonHealthTracker,
) {
    val enabled: Boolean = StreamIntelligence.enabled

    private val bestQualityListOrderKey = booleanPreferencesKey("best_quality_list_order")

    private fun store(profileId: Int = profileManager.activeProfileId.value) = factory.get(profileId, FEATURE)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bestQualityListOrder: Flow<Boolean> = profileManager.activeProfileId.flatMapLatest { profileId ->
        store(profileId).data.map { enabled && (it[bestQualityListOrderKey] ?: false) }
    }

    suspend fun setBestQualityListOrder(value: Boolean) {
        store().edit { it[bestQualityListOrderKey] = value }
    }

    /** Snapshot for one load; false (official order) on any read failure. */
    suspend fun bestQualityListOrderNow(): Boolean =
        enabled && runCatching { bestQualityListOrder.first() }.getOrDefault(false)

    /** Taken once per load so the order does not change while the list is on screen. */
    fun contextFor(installedAddons: List<Addon>, runtimeMinutes: Int? = null): StreamRankContext {
        if (!enabled) return StreamRankContext.NONE
        val health = healthTracker.health.value
        return StreamRankContext(
            displaySupportsDv = displaySupportsDv(),
            addonHealth = installedAddons.mapNotNull { addon ->
                health[addon.baseUrl]?.let { addon.displayName to it.state }
            }.toMap(),
            connectionMbps = runCatching { ConnectionSpeedEstimator.estimateMbps(context) }.getOrNull(),
            runtimeMinutes = runtimeMinutes,
            decodesAv1 = decodesAv1,
        )
    }

    /** Whether a hardware AV1 decoder exists; unknown counts as yes, so nothing is demoted on a guess. */
    private val decodesAv1: Boolean by lazy {
        runCatching {
            android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) } &&
                    (android.os.Build.VERSION.SDK_INT < 29 || info.isHardwareAccelerated)
            }
        }.getOrDefault(true)
    }

    /** Unknown display capabilities count as DV-capable, so nothing is demoted on a guess. */
    @Suppress("DEPRECATION")
    private fun displaySupportsDv(): Boolean {
        val hdrTypes = runCatching {
            context.getSystemService(DisplayManager::class.java)
                ?.getDisplay(Display.DEFAULT_DISPLAY)
                ?.hdrCapabilities
                ?.supportedHdrTypes
        }.getOrNull() ?: return true
        return Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION in hdrTypes
    }

    private companion object {
        const val FEATURE = "fork_streams"
    }
}
