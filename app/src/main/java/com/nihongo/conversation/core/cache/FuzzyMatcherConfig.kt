package com.nihongo.conversation.core.cache

/**
 * Configuration for FuzzyMatcher behavior
 * Phase 3B: Externalized settings for better testability and per-scenario tuning
 */
data class FuzzyMatcherConfig(
    /**
     * Default similarity threshold for cache hits (0.0 to 1.0)
     */
    val defaultThreshold: Float = 0.8f,

    /**
     * Lower threshold for keyword-based matching
     */
    val keywordThreshold: Float = 0.7f,

    /**
     * Very high threshold for sensitive scenarios
     */
    val highThreshold: Float = 0.9f,

    /**
     * Maximum allowed length difference for Levenshtein early exit
     */
    val maxLengthDiff: Int = 8,

    /**
     * Common Japanese particles to filter from tokens
     */
    val particles: Set<String> = DEFAULT_PARTICLES
) {
    companion object {
        val DEFAULT_PARTICLES = setOf(
            "は", "が", "を", "に", "で", "と", "の", "へ", "も",
            "から", "まで", "や", "より", "か", "ね", "よ"
        )

        /**
         * Default configuration for general use
         */
        fun default() = FuzzyMatcherConfig()

        /**
         * Strict configuration for sensitive scenarios (e.g., medical, legal)
         */
        fun strict() = FuzzyMatcherConfig(
            defaultThreshold = 0.9f,
            keywordThreshold = 0.85f,
            highThreshold = 0.95f
        )

        /**
         * Lenient configuration for beginner scenarios
         */
        fun lenient() = FuzzyMatcherConfig(
            defaultThreshold = 0.7f,
            keywordThreshold = 0.6f,
            highThreshold = 0.8f
        )
    }
}
