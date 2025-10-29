package com.nihongo.conversation.domain.model

/**
 * Represents the result of a pronunciation practice attempt
 */
data class PronunciationResult(
    val expectedText: String,
    val recognizedText: String,
    val accuracyScore: Int, // 0-100
    val wordComparison: List<WordMatch>
)

/**
 * Represents how well a single word was pronounced
 */
data class WordMatch(
    val expectedWord: String,
    val recognizedWord: String?,
    val matchType: MatchType
)

enum class MatchType {
    EXACT,      // Perfect match
    SIMILAR,    // Close enough (>70% similarity)
    DIFFERENT,  // Mismatch
    MISSING     // Word not recognized
}

/**
 * Calculates pronunciation accuracy between expected and recognized text
 */
object PronunciationScorer {

    /**
     * Compare two Japanese texts and return detailed pronunciation result
     */
    fun calculateScore(expected: String, recognized: String): PronunciationResult {
        // Normalize texts (remove spaces, punctuation)
        val expectedNormalized = normalizeJapanese(expected)
        val recognizedNormalized = normalizeJapanese(recognized)

        // Split into words/characters
        val expectedWords = splitJapaneseText(expectedNormalized)
        val recognizedWords = splitJapaneseText(recognizedNormalized)

        // Compare words
        val wordComparisons = compareWords(expectedWords, recognizedWords)

        // Calculate overall accuracy
        val accuracy = calculateAccuracy(wordComparisons)

        return PronunciationResult(
            expectedText = expected,
            recognizedText = recognized,
            accuracyScore = accuracy,
            wordComparison = wordComparisons
        )
    }

    private fun normalizeJapanese(text: String): String {
        return text
            .replace(Regex("[\\s　]+"), "") // Remove spaces (including full-width)
            .replace(Regex("[。、！？!?,.]+"), "") // Remove punctuation
            .trim()
    }

    private fun splitJapaneseText(text: String): List<String> {
        // Simple splitting by characters for now
        // In a production app, you'd want to use a proper tokenizer like Kuromoji
        return text.chunked(1)
    }

    private fun compareWords(expected: List<String>, recognized: List<String>): List<WordMatch> {
        val result = mutableListOf<WordMatch>()
        val maxLength = maxOf(expected.size, recognized.size)

        for (i in 0 until maxLength) {
            val expectedWord = expected.getOrNull(i)
            val recognizedWord = recognized.getOrNull(i)

            when {
                expectedWord == null -> {
                    // Extra word in recognition (shouldn't happen often)
                    continue
                }
                recognizedWord == null -> {
                    // Missing word
                    result.add(WordMatch(expectedWord, null, MatchType.MISSING))
                }
                expectedWord == recognizedWord -> {
                    // Exact match
                    result.add(WordMatch(expectedWord, recognizedWord, MatchType.EXACT))
                }
                else -> {
                    // Check similarity
                    val similarity = calculateSimilarity(expectedWord, recognizedWord)
                    val matchType = when {
                        similarity >= 0.7 -> MatchType.SIMILAR
                        else -> MatchType.DIFFERENT
                    }
                    result.add(WordMatch(expectedWord, recognizedWord, matchType))
                }
            }
        }

        return result
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        // Levenshtein distance for similarity
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1

        if (longer.isEmpty()) return 1.0

        val editDistance = levenshteinDistance(longer, shorter)
        return (longer.length - editDistance).toDouble() / longer.length
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    private fun calculateAccuracy(comparisons: List<WordMatch>): Int {
        if (comparisons.isEmpty()) return 0

        var weightedScore = 0
        for (match in comparisons) {
            weightedScore += when (match.matchType) {
                MatchType.EXACT -> 100
                MatchType.SIMILAR -> 70
                MatchType.DIFFERENT -> 30
                MatchType.MISSING -> 0
            }
        }

        return (weightedScore / comparisons.size).coerceIn(0, 100)
    }
}
