package com.Wffv9FNa.redditshim

object Config {
    /**
     * Target Reddit client package name.
     * Change this to your preferred Reddit client:
     * - Sync for Reddit: "com.laurencedawson.reddit_sync"
     * - Relay for Reddit: "free.reddit.news"
     * - Boost for Reddit: "com.rubenmayayo.reddit"
     * - Infinity for Reddit: "ml.docilealligator.infinityforreddit"
     * - Joey for Reddit: "o.o.joey"
     * - Official Reddit: "com.reddit.frontpage"
     */
    const val TARGET_PACKAGE = "com.laurencedawson.reddit_sync"

    /**
     * Reddit destination domains that signal end of redirect chain.
     * When we hit any of these, stop resolving redirects.
     */
    val REDDIT_DOMAINS = setOf(
        "reddit.com",
        "www.reddit.com",
        "old.reddit.com",
        "new.reddit.com",
        "redd.it",
        "v.redd.it",
        "i.redd.it",
        "redditmedia.com"
    )

    /**
     * Maximum number of redirects to follow.
     * Prevents infinite redirect loops.
     */
    const val MAX_REDIRECT_HOPS = 8

    /**
     * Connection timeout in milliseconds.
     */
    const val CONNECT_TIMEOUT_MS = 5000L

    /**
     * Read timeout in milliseconds.
     */
    const val READ_TIMEOUT_MS = 5000L

    /**
     * Write timeout in milliseconds.
     */
    const val WRITE_TIMEOUT_MS = 5000L
}
