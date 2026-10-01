package com.nuvio.tv.fork.aimedia

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/host/AndroidPackageInstallerBridge.kt`; package renamed.

import java.io.File

sealed interface ProviderInstallerResult {
    data class Installed(val packageName: String) : ProviderInstallerResult
    data class Rejected(val reason: ProviderInstallerRejectionReason) : ProviderInstallerResult
}

enum class ProviderInstallerRejectionReason {
    UNKNOWN_SOURCES_PERMISSION_REQUIRED,
    SIGNATURE_CONFLICT,
    USER_CANCELLED,
    INVALID_APK,
    STORAGE,
    TIMEOUT,
    SECURITY,
    IO,
    OTHER,
}

sealed interface ProviderInstallerEvent {
    data object AwaitingUserConfirmation : ProviderInstallerEvent
    data object Accepted : ProviderInstallerEvent
    data class Rejected(val reason: ProviderInstallerRejectionReason) : ProviderInstallerEvent
}

fun interface ProviderInstallerStatusListener {
    fun onEvent(event: ProviderInstallerEvent)
}

interface PackageInstallerBridge {
    /**
     * Writes the APK into a PackageInstaller session and commits it, which triggers the
     * system's user-confirmation prompt. Suspends until the install finishes or is
     * rejected; reports status transitions through [statusListener].
     */
    suspend fun install(
        apkFile: File,
        packageName: String?,
        statusListener: ProviderInstallerStatusListener? = null
    ): ProviderInstallerResult
}
