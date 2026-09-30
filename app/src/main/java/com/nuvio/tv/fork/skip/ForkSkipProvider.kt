package com.nuvio.tv.fork.skip

/** Skip providers the fork adds beside official IntroDB / AniSkip / Anime-Skip (G9a, G9b; D054). */
enum class ForkSkipProvider(val key: String, val requiresKey: Boolean, val takesKey: Boolean) {
    SKIP_ME("skipme", requiresKey = false, takesKey = false),
    THE_INTRO_DB("theintrodb", requiresKey = false, takesKey = true),
    PUBLIC_META_DB("publicmetadb", requiresKey = true, takesKey = true),

    /** G9b (127): per-movie scene files with skip / mute / warn actions. */
    MOVIE_HAVEN_DB("moviehavendb", requiresKey = false, takesKey = false),

    /** G9b (128): community `.skp` files found by title. */
    VIDEO_SKIP("videoskip", requiresKey = false, takesKey = false),

    /** G9b (129): public jump-scare pages for movies, by title and year. */
    NOT_SCARE("notscare", requiresKey = false, takesKey = false),
}
