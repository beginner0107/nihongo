package com.nihongo.conversation.core.cache

import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy matching utility for Japanese conversation patterns
 * Uses multiple algorithms to find best pattern match:
 * - Levenshtein distance for character similarity
 * - Token-based matching for word similarity
 * - Keyword matching for semantic similarity
 */
class FuzzyMatcher {

    /**
     * Calculate similarity between two strings (0.0 to 1.0)
     * Higher score = more similar
     */
    fun calculateSimilarity(input: String, pattern: String): Float {
        val normalizedInput = normalize(input)
        val normalizedPattern = normalize(pattern)

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
        val normalizedInput = normalize(input)
        val keywordMatches = keywords.count { keyword ->
            normalizedInput.contains(normalize(keyword))
        }

        val keywordBonus = if (keywords.isNotEmpty()) {
            (keywordMatches.toFloat() / keywords.size) * 0.2f
        } else 0.0f

        return min(1.0f, baseSimilarity + keywordBonus)
    }

    /**
     * Normalize Japanese text for comparison
     * - Remove extra whitespace
     * - Convert to lowercase (for romaji/english)
     * - Normalize particles
     */
    private fun normalize(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), "")
            .lowercase()
    }

    /**
     * Calculate Levenshtein distance-based similarity
     * Returns value from 0.0 (completely different) to 1.0 (identical)
     */
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Float {
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
        val tokens1 = tokenize(s1)
        val tokens2 = tokenize(s2)

        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0f
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0f

        // Calculate Jaccard similarity
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size

        return if (union == 0) 0.0f else intersection.toFloat() / union
    }

    /**
     * Tokenize Japanese text
     * Creates bigrams for better matching
     */
    private fun tokenize(text: String): Set<String> {
        val tokens = mutableSetOf<String>()

        // Add individual characters
        tokens.addAll(text.toCharArray().map { it.toString() })

        // Add bigrams (2-character sequences)
        for (i in 0 until text.length - 1) {
            tokens.add(text.substring(i, i + 2))
        }

        return tokens
    }

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

    companion object {
        // Default similarity threshold for cache hits
        const val DEFAULT_THRESHOLD = 0.8f

        // Lower threshold for keyword-based matching
        const val KEYWORD_THRESHOLD = 0.7f

        // Very high threshold for sensitive scenarios
        const val HIGH_THRESHOLD = 0.9f
    }
}
