package com.nuvio.tv.fork.aimedia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory active profile for JVM tests (Fornace `core/profile/ActiveProfileProvider.kt`). */
class FakeActiveProfileProvider(initial: Int = 1) : ActiveProfileProvider {
    val mutable = MutableStateFlow(initial)
    override val activeProfileId: StateFlow<Int> = mutable
}
