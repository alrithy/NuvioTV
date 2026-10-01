package com.nuvio.tv.fork.watchparty

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * One Watch Party session for the app (G11a, D056). Antonino `watchparty/WatchPartyModule.kt`
 * @ ff597b1. The WebView transport is only created when a room is opened or joined.
 */
@Module
@InstallIn(SingletonComponent::class)
object WatchPartyModule {
    @Provides
    @Singleton
    fun provideWatchPartySession(@ApplicationContext context: Context): WatchPartySession =
        WatchPartySession(
            transportFactory = { WebViewWatchPartyTransport(context) },
            deviceName = { deviceName(context) },
        )

    /** The name the other people in the room see: the TV's own name, else its model. */
    private fun deviceName(context: Context): String =
        runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: Build.MODEL
}

/** For screens outside Hilt view models (the player, the navigation host). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WatchPartyEntryPoint {
    fun watchPartySession(): WatchPartySession

    fun featureRegistry(): FeatureRegistry
}

/** Whether Watch Party shows anywhere: OFF hides every entry and no WebView is ever created. */
fun FeatureRegistry.watchPartyEnabled(): Boolean = mode(FeatureId.WATCH_PARTY) != FeatureMode.OFF
