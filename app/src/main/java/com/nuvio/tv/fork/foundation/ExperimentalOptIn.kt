package com.nuvio.tv.fork.foundation

/**
 * The only way an experimental feature group turns on (G13a, D063). Each group in
 * [FeatureId.experimental] stays [FeatureMode.OFF] until the user opts in on the Settings →
 * Experimental screen; the stored choice is read once at app start ([install]) and applies to
 * every [FeatureRegistry] built afterwards, so a change takes effect on the next start. Anything
 * that is not an experimental group is ignored: a stored value can never switch a stable group.
 */
object ExperimentalOptIn {
    @Volatile
    private var optedIn: Set<FeatureId> = emptySet()

    val enabled: Set<FeatureId> get() = optedIn

    fun install(stored: Set<String>) {
        optedIn = parse(stored)
    }

    fun parse(stored: Set<String>): Set<FeatureId> =
        FeatureId.entries.filter { it.experimental && it.name in stored }.toSet()

    /** What the registry applies on top of its defaults. */
    fun overrides(): Map<FeatureId, FeatureMode> = optedIn.associateWith { FeatureMode.ON }

    internal fun resetForTest() {
        optedIn = emptySet()
    }
}
