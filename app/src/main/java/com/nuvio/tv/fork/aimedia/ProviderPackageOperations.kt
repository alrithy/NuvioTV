package com.nuvio.tv.fork.aimedia

// G13b (D063): FILE_PORT of Fornace/nuvio-ai @ 518af71 `app/src/main/…/core/media/provider/host/AndroidProviderPackageOperations.kt`; package renamed.

interface ProviderPackageOperations {
    fun canRequestPackageInstalls(): Boolean
    fun openUnknownSourcesSettings(): Boolean
    fun openUninstall(packageName: String): Boolean
}
