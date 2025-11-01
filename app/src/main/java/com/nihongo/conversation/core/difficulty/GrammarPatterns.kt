package com.nihongo.conversation.core.difficulty

/**
 * Japanese grammar patterns separated from vocabulary
 * Phase 4: Extracted from CommonVocabulary for cleaner separation
 *
 * These are structural/functional patterns, not content words
 */
object GrammarPatterns {

    /**
     * N4 level grammar patterns
     * Expressions and connectors for elementary learners
     */
    val N4_GRAMMAR = setOf(
        // Adverbs and expressions
        "多分", "きっと", "たぶん", "もちろん", "実は", "やっぱり",
        "ちょっと", "もう", "まだ", "また", "すぐ", "ゆっくり"
    )

    /**
     * N3 level grammar patterns
     * Intermediate postpositional phrases and connectors
     */
    val N3_GRAMMAR = setOf(
        // Postpositional phrases
        "によって", "に対して", "について", "に関して", "のために",

        // Auxiliary verbs and expressions
        "ようだ", "そうだ", "らしい", "はずだ", "べきだ", "わけだ"
    )

    /**
     * N2 level grammar patterns
     * Upper-intermediate conditional and connective patterns
     */
    val N2_GRAMMAR = setOf(
        // Conditional forms
        "〜ば", "〜たら", "〜なら", "〜と",

        // Connective patterns
        "にもかかわらず", "だけでなく", "ばかりでなく",
        "一方", "反面", "代わりに", "くせに"
    )

    /**
     * N1 level grammar patterns
     * Advanced formal and literary patterns
     */
    val N1_GRAMMAR = setOf(
        "ざるを得ない", "に他ならない", "に越したことはない",
        "ならでは", "をもって", "に際して", "において",
        "にもかかわらず", "がてら", "をめぐって",
        "を問わず", "はもとより", "をはじめ", "にわたって"
    )

    /**
     * All grammar patterns combined
     */
    val ALL_GRAMMAR = N4_GRAMMAR + N3_GRAMMAR + N2_GRAMMAR + N1_GRAMMAR

    /**
     * Check if a text segment is primarily a grammar pattern
     */
    fun isGrammarPattern(text: String): Boolean {
        return ALL_GRAMMAR.contains(text)
    }

    /**
     * Count grammar pattern matches in text
     */
    fun countGrammarPatterns(text: String): Int {
        return ALL_GRAMMAR.count { pattern -> text.contains(pattern) }
    }

    /**
     * Get grammar complexity level based on patterns used
     */
    fun analyzeGrammarComplexity(text: String): GrammarComplexity {
        val hasN1 = N1_GRAMMAR.any { text.contains(it) }
        val hasN2 = N2_GRAMMAR.any { text.contains(it) }
        val hasN3 = N3_GRAMMAR.any { text.contains(it) }
        val hasN4 = N4_GRAMMAR.any { text.contains(it) }

        return when {
            hasN1 -> GrammarComplexity.ADVANCED
            hasN2 -> GrammarComplexity.UPPER_INTERMEDIATE
            hasN3 -> GrammarComplexity.INTERMEDIATE
            hasN4 -> GrammarComplexity.ELEMENTARY
            else -> GrammarComplexity.BASIC
        }
    }
}

/**
 * Grammar complexity levels
 */
enum class GrammarComplexity {
    BASIC,              // Simple です/ます forms only
    ELEMENTARY,         // N4 grammar patterns
    INTERMEDIATE,       // N3 grammar patterns
    UPPER_INTERMEDIATE, // N2 grammar patterns
    ADVANCED            // N1 grammar patterns
}
