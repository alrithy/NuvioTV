package com.nuvio.tv.fork.seek.local

import android.content.Context
import com.nuvio.tv.fork.resource.AdaptiveResources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Generate previews on device". Default from the resource budget (on, except on low-RAM
 * devices) until the user changes it; stored in app preferences under Reshaped's names.
 */
internal object LocalSeekPreviewSettings {
    private const val PREFS = "nuvio_seekr_settings"
    private const val KEY_LOCAL_ENABLED = "local_previews_enabled"

    private val _enabled = MutableStateFlow(false)

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            _enabled.value = prefs(context).getBoolean(
                KEY_LOCAL_ENABLED,
                AdaptiveResources.policy.seekPreviewBudget.localPreviewsByDefault,
            )
            loaded = true
        }
    }

    fun enabled(context: Context): StateFlow<Boolean> {
        ensureLoaded(context)
        return _enabled.asStateFlow()
    }

    fun setEnabled(context: Context, value: Boolean) {
        _enabled.value = value
        loaded = true
        prefs(context).edit().putBoolean(KEY_LOCAL_ENABLED, value).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
