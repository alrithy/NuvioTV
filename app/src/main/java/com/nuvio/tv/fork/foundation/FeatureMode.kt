package com.nuvio.tv.fork.foundation

/** Activation mode for a Superfork feature group. */
enum class FeatureMode {
    /** Superfork-specific behavior is disabled; official Nuvio behavior applies. */
    OFF,

    /** Explicitly enabled. */
    ON,

    /** Runtime policy/compatibility logic decides. */
    AUTO,
}
