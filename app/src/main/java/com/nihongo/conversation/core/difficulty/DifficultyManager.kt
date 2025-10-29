package com.nihongo.conversation.core.difficulty

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Difficulty levels for Japanese language learning
 */
enum class DifficultyLevel(val value: Int) {
    BEGINNER(1),    // 初級: JLPT N5-N4
    INTERMEDIATE(2), // 中級: JLPT N3-N2
    ADVANCED(3);     // 上級: JLPT N1

    companion object {
        fun fromInt(value: Int): DifficultyLevel {
            return values().find { it.value == value } ?: BEGINNER
        }
    }
}

/**
 * Vocabulary complexity level
 */
enum class VocabularyComplexity {
    BASIC,      // Common daily words
    COMMON,     // Frequently used words
    INTERMEDIATE, // Standard vocabulary
    ADVANCED,   // Specialized/formal words
    EXPERT      // Rare, technical, or literary
}

/**
 * Manages AI difficulty adjustments based on user's proficiency level
 */
@Singleton
class DifficultyManager @Inject constructor() {

    /**
     * Get difficulty-specific system prompt enhancement
     */
    fun getDifficultyPrompt(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.BEGINNER -> getBeginnerPrompt()
            DifficultyLevel.INTERMEDIATE -> getIntermediatePrompt()
            DifficultyLevel.ADVANCED -> getAdvancedPrompt()
        }
    }

    private fun getBeginnerPrompt(): String {
        return """

DIFFICULTY LEVEL: BEGINNER (初級 - JLPT N5-N4)

Language Guidelines:
1. VOCABULARY:
   - Use only basic, common words (N5-N4 level)
   - Avoid kanji compounds; prefer hiragana or simple kanji
   - Examples: 食べる, 行く, 見る, きれい, おいしい

2. GRAMMAR:
   - Use simple sentence structures (subject-object-verb)
   - Present/past tense only
   - Common particles: は, が, を, に, で, と
   - Avoid complex grammar (causative, passive, conditionals)

3. SENTENCE LENGTH:
   - Keep sentences short (5-10 words maximum)
   - One idea per sentence
   - Use frequent particles to show relationships clearly

4. EXPRESSIONS:
   - Basic greetings and common phrases only
   - です/ます form exclusively (polite form)
   - Avoid casual forms (だ, である, etc.)

5. SPEAKING STYLE:
   - Speak slowly and clearly
   - Repeat key words if necessary
   - Use simple, encouraging responses
   - Break complex ideas into multiple simple sentences

EXAMPLES:
❌ BAD (too complex): "そのレストランはとても人気があるので、予約した方がいいと思います。"
✅ GOOD (appropriate): "そのレストランは人気です。予約をしてください。"

Remember: The learner is a beginner. Use the simplest Japanese possible!
        """.trimIndent()
    }

    private fun getIntermediatePrompt(): String {
        return """

DIFFICULTY LEVEL: INTERMEDIATE (中級 - JLPT N3-N2)

Language Guidelines:
1. VOCABULARY:
   - Use common to intermediate vocabulary (N3-N2 level)
   - Mix of kanji and kana appropriately
   - Include common compound words
   - Examples: 準備する, 残念, 素晴らしい, 〜によって

2. GRAMMAR:
   - Compound sentences with connectors (が, けど, から, ので)
   - Conditional forms (たら, ば, なら)
   - Potential form, passive form, causative form
   - Te-form combinations (〜ている, 〜てみる, 〜てあげる)
   - Common grammar patterns (〜ようだ, 〜そうだ, 〜らしい)

3. SENTENCE LENGTH:
   - Moderate length (10-20 words)
   - Multiple clauses connected naturally
   - Varied sentence structures

4. EXPRESSIONS:
   - Common idiomatic expressions
   - Casual forms appropriate for context (だ, じゃない)
   - Mix of polite and casual based on situation
   - Natural conversational patterns

5. SPEAKING STYLE:
   - Natural, conversational pace
   - Use both formal and informal expressions as appropriate
   - Include filler words (あの、えっと、そうですね)
   - Provide nuanced responses

EXAMPLES:
❌ BAD (too simple): "レストランは人気です。予約をしてください。"
❌ BAD (too complex): "当該レストランにおかれましては、昨今の評判により満席となることが多く、事前予約を推奨いたします。"
✅ GOOD (appropriate): "そのレストランは人気があるので、予約した方がいいと思いますよ。"

Remember: The learner can handle natural conversation with some complexity!
        """.trimIndent()
    }

    private fun getAdvancedPrompt(): String {
        return """

DIFFICULTY LEVEL: ADVANCED (上級 - JLPT N1)

Language Guidelines:
1. VOCABULARY:
   - Advanced vocabulary including specialized terms (N1 level)
   - Literary expressions and formal language
   - Kanji compounds and Sino-Japanese words
   - Examples: 考慮する, 顕著, 〜に際して, 〜をもって

2. GRAMMAR:
   - Complex sentence structures with multiple clauses
   - Advanced grammar patterns (〜ざるを得ない, 〜に他ならない, 〜に越したことはない)
   - Keigo (honorific/humble/polite language) appropriately
   - Literary forms and written language patterns
   - Passive, causative-passive combinations

3. SENTENCE LENGTH:
   - Long, sophisticated sentences (20+ words)
   - Multiple nested clauses
   - Complex logical relationships

4. EXPRESSIONS:
   - Idiomatic expressions and proverbs (ことわざ)
   - Formal business/academic language
   - Cultural references and nuanced expressions
   - Four-character idioms (四字熟語)

5. SPEAKING STYLE:
   - Formal and sophisticated
   - Use keigo when contextually appropriate
   - Nuanced and indirect communication
   - Show cultural awareness
   - Literary or poetic expressions when suitable

EXAMPLES:
❌ BAD (too simple): "そのレストランは人気があるので、予約した方がいいと思いますよ。"
✅ GOOD (appropriate): "当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。"
✅ GOOD (formal): "そちらのレストランにおかれましては、大変人気がございますので、あらかじめご予約を頂戴できればと存じます。"

Remember: The learner is advanced and can handle complex, nuanced Japanese!
        """.trimIndent()
    }

    /**
     * Analyze vocabulary complexity of Japanese text
     */
    fun analyzeVocabularyComplexity(text: String): VocabularyComplexity {
        // Simple heuristic-based analysis
        val kanjiCount = text.count { it.code in 0x4E00..0x9FFF }
        val totalChars = text.length
        val kanjiRatio = if (totalChars > 0) kanjiCount.toFloat() / totalChars else 0f

        // Check for advanced patterns
        val hasKeigo = containsKeigo(text)
        val hasAdvancedGrammar = containsAdvancedGrammar(text)
        val hasFormalEndings = containsFormalEndings(text)

        return when {
            hasKeigo && hasAdvancedGrammar -> VocabularyComplexity.EXPERT
            (kanjiRatio > 0.4 && hasFormalEndings) -> VocabularyComplexity.ADVANCED
            kanjiRatio > 0.3 || hasAdvancedGrammar -> VocabularyComplexity.INTERMEDIATE
            kanjiRatio > 0.15 -> VocabularyComplexity.COMMON
            else -> VocabularyComplexity.BASIC
        }
    }

    /**
     * Check if text contains keigo (honorific language)
     */
    private fun containsKeigo(text: String): Boolean {
        val keigoPatterns = listOf(
            "ございます", "いらっしゃる", "おっしゃる", "なさる",
            "いたします", "申し上げる", "存じます", "頂戴",
            "お〜になる", "ご〜ください", "〜られる"
        )
        return keigoPatterns.any { text.contains(it) }
    }

    /**
     * Check if text contains advanced grammar patterns
     */
    private fun containsAdvancedGrammar(text: String): Boolean {
        val advancedPatterns = listOf(
            "ざるを得ない", "に他ならない", "に越したことはない",
            "ならではの", "をもって", "に際して", "において",
            "によって", "に対して", "に関して", "について"
        )
        return advancedPatterns.any { text.contains(it) }
    }

    /**
     * Check if text contains formal endings
     */
    private fun containsFormalEndings(text: String): Boolean {
        val formalPatterns = listOf(
            "でございます", "いたします", "申し上げます",
            "存じます", "おります", "〜ます。"
        )
        return formalPatterns.any { text.contains(it) }
    }

    /**
     * Get complexity level as integer (1-5)
     */
    fun getComplexityScore(complexity: VocabularyComplexity): Int {
        return when (complexity) {
            VocabularyComplexity.BASIC -> 1
            VocabularyComplexity.COMMON -> 2
            VocabularyComplexity.INTERMEDIATE -> 3
            VocabularyComplexity.ADVANCED -> 4
            VocabularyComplexity.EXPERT -> 5
        }
    }

    /**
     * Get user-friendly complexity description in Japanese
     */
    fun getComplexityDescription(complexity: VocabularyComplexity): String {
        return when (complexity) {
            VocabularyComplexity.BASIC -> "基本 (N5-N4レベル)"
            VocabularyComplexity.COMMON -> "一般 (N4-N3レベル)"
            VocabularyComplexity.INTERMEDIATE -> "中級 (N3-N2レベル)"
            VocabularyComplexity.ADVANCED -> "上級 (N2-N1レベル)"
            VocabularyComplexity.EXPERT -> "専門 (N1+レベル)"
        }
    }
}
