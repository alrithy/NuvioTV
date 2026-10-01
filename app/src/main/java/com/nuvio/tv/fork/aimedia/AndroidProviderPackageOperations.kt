package com.nuvio.tv.fork.aimedia

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/host/AndroidProviderPackageOperations.kt`; package renamed.

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Android recovery actions used by the provider center. System UI performs every mutation. */
class AndroidProviderPackageOperations(
    private val context: Context,
) : ProviderPackageOperations {
    override fun canRequestPackageInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    override fun openUnknownSourcesSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    override fun openUninstall(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: RuntimeException) {
        false
    }
}
