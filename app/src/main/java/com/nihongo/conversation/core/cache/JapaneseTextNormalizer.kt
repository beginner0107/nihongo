package com.nihongo.conversation.core.cache

import java.text.Normalizer

/**
 * Japanese text normalizer for consistent text comparison
 * Phase 3B: Extracted from FuzzyMatcher for reuse across cache and analytics
 *
 * Normalization steps:
 * 1. NFKC normalization (full/half-width unification)
 * 2. Punctuation and emoji removal
 * 3. Katakana → Hiragana conversion
 * 4. Prolonged sound mark removal
 * 5. Lowercase conversion
 */
class JapaneseTextNormalizer {

    /**
     * Normalize Japanese text for comparison
     */
    fun normalize(text: String): String {
        // Step 1: NFKC normalization (compatibility decomposition + composition)
        val nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC)

        // Step 2: Strip punctuation, emoji, symbols, and whitespace
        val stripped = nfkc.replace(Regex("[\\p{Punct}\\s\\p{So}\\p{Sk}\\p{Sm}]+"), "")

        // Step 3: Convert katakana to hiragana for matching
        val hiragana = stripped.map { char ->
            when (char.code) {
                in 0x30A1..0x30F6 -> (char.code - 0x60).toChar() // カタカナ → ひらがな
                else -> char
            }
        }.joinToString("")

        // Step 4: Remove prolonged sound marks
        // Step 5: Convert to lowercase (for romaji/mixed text)
        return hiragana.replace("ー", "").lowercase()
    }

    /**
     * Tokenize normalized Japanese text into character bigrams
     * Filters out common particles to reduce noise
     */
    fun tokenize(text: String, particles: Set<String>): Set<String> {
        val normalized = normalize(text)
        val tokens = mutableSetOf<String>()

        // Add individual characters (filter particles)
        normalized.toCharArray().forEach { char ->
            val charStr = char.toString()
            if (charStr !in particles) {
                tokens.add(charStr)
            }
        }

        // Add bigrams (filter bigrams containing particles)
        for (i in 0 until normalized.length - 1) {
            val bigram = normalized.substring(i, i + 2)
            if (!particles.any { particle -> bigram.contains(particle) }) {
                tokens.add(bigram)
            }
        }

        return tokens
    }

    companion object {
        /**
         * Shared singleton instance for efficiency
         */
        val INSTANCE = JapaneseTextNormalizer()
    }
}
