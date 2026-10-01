package com.nuvio.tv.fork.foundation

import android.content.Context

/**
 * Device-wide storage of the experimental opt-ins (G13a, D063). Read before Hilt injection, like
 * the resource manager, so every feature-registry read sees one consistent choice for the whole
 * process. Only the group names are stored; nothing secret.
 */
object ExperimentalOptInStore {
    private const val PREFS = "fork_experimental"
    private const val KEY = "opted_in"

    fun stored(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY, emptySet()).orEmpty()

    /** Takes effect on the next app start. */
    fun setOptedIn(context: Context, id: FeatureId, optedIn: Boolean) {
        require(id.experimental) { "only experimental groups can be opted in" }
        val current = stored(context)
        val next = if (optedIn) current + id.name else current - id.name
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY, next).apply()
    }
}
