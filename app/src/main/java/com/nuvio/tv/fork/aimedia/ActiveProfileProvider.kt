package com.nuvio.tv.fork.aimedia

import kotlinx.coroutines.flow.StateFlow

/**
 * The active profile, as the provider center sees it (G13b). Fornace adds this seam to official's
 * `core/profile`; here it stays inside `fork/aimedia` and is backed by official's `ProfileManager`
 * in the DI module, so official profile code is not edited.
 */
interface ActiveProfileProvider {
    val activeProfileId: StateFlow<Int>
}
