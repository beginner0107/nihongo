package com.nihongo.conversation.core.cache

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy matching utility for Japanese conversation patterns
 * Uses multiple algorithms to find best pattern match:
 * - Levenshtein distance for character similarity
 * - Token-based matching for word similarity
 * - Keyword matching for semantic similarity
 *
 * Phase 1 Improvements:
 * - NFKC normalization for Japanese text
 * - Katakana → Hiragana conversion
 * - Particle filtering in tokens
 * - Early exit for Levenshtein with length guard
 *
 * Phase 3B Improvements:
 * - Externalized configuration for testability
 * - Shared normalizer for consistency across app
 */
class FuzzyMatcher(
    private val config: FuzzyMatcherConfig = FuzzyMatcherConfig.default(),
    private val normalizer: JapaneseTextNormalizer = JapaneseTextNormalizer.INSTANCE
) {

    companion object {
        // Backward compatibility constants
        const val DEFAULT_THRESHOLD = 0.8f
        const val KEYWORD_THRESHOLD = 0.7f
        const val HIGH_THRESHOLD = 0.9f
    }

    /**
     * Phase 2: Get adaptive threshold based on conversation turn
     * First turns need lower threshold (more lenient)
     * Later turns can be higher (more precise)
     */
    fun getAdaptiveThreshold(turnNumber: Int, scenarioId: Long? = null): Float {
        return when {
            turnNumber == 1 -> 0.7f  // First turn: more lenient
            turnNumber < 5 -> 0.75f   // Early conversation: medium
            else -> 0.85f             // Later turns: stricter
        }
    }

    /**
     * Calculate similarity between two strings (0.0 to 1.0)
     * Higher score = more similar
     */
    fun calculateSimilarity(input: String, pattern: String): Float {
        val normalizedInput = normalizer.normalize(input)
        val normalizedPattern = normalizer.normalize(pattern)

        // Exact match
        if (normalizedInput == normalizedPattern) return 1.0f

        // Calculate multiple similarity metrics
        val levenshteinSim = calculateLevenshteinSimilarity(normalizedInput, normalizedPattern)
        val tokenSim = calculateTokenSimilarity(normalizedInput, normalizedPattern)
        val substringBonus = if (normalizedInput.contains(normalizedPattern) ||
            normalizedPattern.contains(normalizedInput)
        ) 0.2f else 0.0f

        // Weighted average (Levenshtein 40%, Token 40%, Substring 20%)
        return min(1.0f, (levenshteinSim * 0.4f) + (tokenSim * 0.4f) + substringBonus)
    }

    /**
     * Calculate similarity with keyword boosting
     */
    fun calculateSimilarityWithKeywords(
        input: String,
        pattern: String,
        keywords: List<String>
    ): Float {
        val baseSimilarity = calculateSimilarity(input, pattern)

        // Boost score if input contains important keywords
        val normalizedInput = normalizer.normalize(input)
        val keywordMatches = keywords.count { keyword ->
            normalizedInput.contains(normalizer.normalize(keyword))
        }

        val keywordBonus = if (keywords.isNotEmpty()) {
            (keywordMatches.toFloat() / keywords.size) * 0.2f
        } else 0.0f

        return min(1.0f, baseSimilarity + keywordBonus)
    }

    /**
     * Public API for normalization (delegates to injected normalizer)
     * Phase 3B: Now uses shared JapaneseTextNormalizer
     */
    fun normalizeForJapanese(text: String): String = normalizer.normalize(text)

    /**
     * Calculate Levenshtein distance-based similarity (Phase 1 with early exit)
     * Returns value from 0.0 (completely different) to 1.0 (identical)
     */
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
        // Phase 1: Early exit if length difference is too large
        val lengthDiff = abs(s1.length - s2.length)
        if (lengthDiff > config.maxLengthDiff) {
            return 0.0f  // Skip expensive calculation
        }

        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)

        return if (maxLength == 0) 1.0f
        else 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Levenshtein distance algorithm
     * Calculates minimum edit operations to transform s1 into s2
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Create distance matrix
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first row and column
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Fill matrix
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = min(
                    min(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1       // insertion
                    ),
                    dp[i - 1][j - 1] + cost    // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Token-based similarity (Jaccard similarity)
     * Compares word/character tokens between strings
     */
    private fun calculateTokenSimilarity(s1: String, s2: String): Float {
        // For Japanese, tokenize by characters (since words aren't space-separated)
        val tokens1 = normalizer.tokenize(s1, config.particles)
        val tokens2 = normalizer.tokenize(s2, config.particles)

        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0f
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0f

        // Calculate Jaccard similarity
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size

        return if (union == 0) 0.0f else intersection.toFloat() / union
    }

    /**
     * Public API for tokenization (delegates to normalizer)
     * Phase 3B: Now uses shared JapaneseTextNormalizer with configured particles
     */
    fun tokenizeJapanese(text: String): Set<String> =
        normalizer.tokenize(text, config.particles)

    /**
     * Find best matching pattern from a list
     * Returns pair of (patternIndex, similarity) or null if no good match
     */
    fun findBestMatch(
        input: String,
        patterns: List<String>,
        threshold: Float = 0.8f
    ): Pair<Int, Float>? {
        var bestIndex = -1
        var bestScore = 0.0f

        patterns.forEachIndexed { index, pattern ->
            val score = calculateSimilarity(input, pattern)
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }

        return if (bestScore >= threshold && bestIndex != -1) {
            Pair(bestIndex, bestScore)
        } else null
    }

    /**
     * Find best matching pattern with keywords
     */
    fun findBestMatchWithKeywords(
        input: String,
        patternsWithKeywords: List<Pair<String, List<String>>>,
        threshold: Float = 0.8f
    ): Triple<Int, Float, String>? {
        var bestIndex = -1
        var bestScore = 0.0f
        var bestPattern = ""

        patternsWithKeywords.forEachIndexed { index, (pattern, keywords) ->
            val score = calculateSimilarityWithKeywords(input, pattern, keywords)
            if (score > bestScore) {
                bestScore = score
                bestIndex = index
                bestPattern = pattern
            }
        }

        return if (bestScore >= threshold && bestIndex != -1) {
            Triple(bestIndex, bestScore, bestPattern)
        } else null
    }

}

