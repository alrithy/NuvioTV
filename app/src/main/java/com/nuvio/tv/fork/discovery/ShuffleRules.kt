package com.nuvio.tv.fork.discovery

import com.nuvio.tv.fork.foundation.FeatureId
import com.nuvio.tv.fork.foundation.FeatureMode
import com.nuvio.tv.fork.foundation.FeatureRegistry

/**
 * G9d (171, 173–177) on official episode shuffle (D054): a season scope, the all-watched fallback
 * and Mystery mode. Cxsmo `RandomEpisodeDialog` @ 3e0d0fa (season pool, "unwatched only" falling back
 * to the whole pool, `mysteryMode`) moved onto official's persistent per-show shuffle. Pure.
 */
object ShuffleRules {
    val enabled: Boolean = FeatureRegistry().mode(FeatureId.DISCOVERY_SKIP_RECOMMENDATIONS) != FeatureMode.OFF

    /**
     * Episodes of [season] only; every episode when [season] is null or no episode of it is left
     * (a season the add-on no longer lists never empties the pool).
     */
    fun <T> scope(episodes: List<T>, season: Int?, seasonOf: (T) -> Int?): List<T> {
        if (season == null) return episodes
        return episodes.filter { seasonOf(it) == season }.ifEmpty { episodes }
    }

    /**
     * Whether watched episodes join the pool: when the user asked for them, or (173, opt-in) when
     * "unwatched only" has nothing left, instead of official's empty pool.
     */
    fun includeWatched(configured: Boolean, fallback: Boolean, unwatchedCount: Int): Boolean =
        configured || (fallback && unwatchedCount == 0)
}
