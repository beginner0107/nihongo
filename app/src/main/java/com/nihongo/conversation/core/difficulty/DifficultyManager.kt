package com.nihongo.conversation.core.difficulty

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Difficulty levels for Japanese language learning
 * Phase 4: Enhanced with display labels and target metrics
 */
enum class DifficultyLevel(
    val value: Int,
    val displayNameJa: String,
    val displayNameKo: String,
    val jlptLevel: String,
    val code: String  // Stable code for analytics/serialization
) {
    BEGINNER(
        value = 1,
        displayNameJa = "初級",
        displayNameKo = "초급",
        jlptLevel = "N5-N4",
        code = "B1"
    ),
    INTERMEDIATE(
        value = 2,
        displayNameJa = "中級",
        displayNameKo = "중급",
        jlptLevel = "N3-N2",
        code = "I1"
    ),
    ADVANCED(
        value = 3,
        displayNameJa = "上級",
        displayNameKo = "고급",
        jlptLevel = "N1",
        code = "A1"
    );

    companion object {
        fun fromInt(value: Int): DifficultyLevel {
            return values().find { it.value == value } ?: BEGINNER
        }

        fun fromCode(code: String): DifficultyLevel {
            return values().find { it.code == code } ?: BEGINNER
        }
    }

    /**
     * Get target vocabulary complexity for this level
     */
    fun targetComplexity(): VocabularyComplexity {
        return when (this) {
            BEGINNER -> VocabularyComplexity.BASIC
            INTERMEDIATE -> VocabularyComplexity.COMMON
            ADVANCED -> VocabularyComplexity.ADVANCED
        }
    }

    /**
     * Get target coverage range for this level
     */
    fun targetCoverage(): ClosedFloatingPointRange<Float> {
        return when (this) {
            BEGINNER -> 0.6f..0.8f
            INTERMEDIATE -> 0.5f..0.7f
            ADVANCED -> 0.4f..0.6f
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

    companion object {
        // Precompiled regex patterns for efficient matching
        val KEIGO_PATTERNS = listOf(
            Regex("お[\\p{Hiragana}]+になる"),
            Regex("ご[\\p{Hiragana}]+ください"),
            Regex("お[\\p{Hiragana}]+する"),
            Regex("ご[\\p{Hiragana}]+いたす")
        )

        val FORMAL_ENDINGS = listOf(
            "です。", "ます。", "でございます",
            "いたします", "申し上げます", "存じます", "おります"
        )

        // Intermediate grammar patterns (N3-N2)
        val INTERMEDIATE_GRAMMAR = listOf(
            "によって", "に対して", "に関して", "について",
            "ようだ", "そうだ", "らしい", "〜ば", "〜たら"
        )

        // Advanced grammar patterns (N1)
        val ADVANCED_GRAMMAR = listOf(
            "ざるを得ない", "に他ならない", "に越したことはない",
            "ならでは", "をもって", "に際して", "において",
            "にもかかわらず", "がてら", "をめぐって"
        )
    }

    /**
     * Get compact difficulty-specific system prompt (optimized for 500-char limit)
     * This is the recommended method for use in chat conversations.
     */
    fun getCompactDifficultyPrompt(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.BEGINNER -> """
                初級(N5-N4): です/ます形のみ。語彙はN5-N4限定。短文(10語以内)。複雑文法(受身/使役/条件)禁止。
                出力: 日本語会話のみ。英語/ふりがな/マークダウン(**,_)一切禁止。思考過程(THINK等)禁止。即座に日本語で開始。
                例: ○「そのレストランは人気です。予約してください。」 ×「そのレストランはとても人気があるので、予約した方がいいと思います。」
            """.trimIndent()

            DifficultyLevel.INTERMEDIATE -> """
                中級(N3-N2): N3-N2語彙。接続詞(が/けど/から/ので)使用可。文長10-20語。条件形/可能形/受身形OK。
                丁寧/カジュアル混在可。出力: 日本語会話のみ。英語/ふりがな/MD禁止。思考過程禁止。即座に開始。
                例: ○「そのレストランは人気があるので、予約した方がいいと思いますよ。」
            """.trimIndent()

            DifficultyLevel.ADVANCED -> """
                上級(N1): N1語彙/敬語可。複文・含意表現自然に。文長20語以上OK。高度文法(〜ざるを得ない等)使用可。
                出力: 日本語会話のみ。英語/ふりがな/MD禁止。思考過程禁止。即座に開始。
                例: ○「当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。」
            """.trimIndent()
        }
    }

    /**
     * Get difficulty-specific system prompt enhancement (full version)
     * Use getCompactDifficultyPrompt() for chat to respect token limits.
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

6. TEXT FORMATTING - CRITICAL:
   - NEVER use markdown formatting (**, __, *, _)
   - NEVER use furigana or pronunciation guides in parentheses: 禁止 お席（せき）
   - NEVER use English translations in parentheses: 禁止 飲み物(drink)
   - Write Japanese text ONLY, with no annotations
   - Use simple kanji that beginners can understand

7. RESPONSE FORMAT - ABSOLUTELY CRITICAL - NEVER VIOLATE:
   ⚠️ FORBIDDEN - DO NOT OUTPUT:
   - Internal reasoning (THINK, "I should...", "Let me...", "This is...")
   - English explanations or thought process
   - Meta-commentary about the conversation
   - Planning or strategizing text

   ✅ REQUIRED - ONLY OUTPUT:
   - Pure Japanese dialogue the character would actually say
   - Start IMMEDIATELY with Japanese text
   - No preamble, no thinking, no explanation

   EXAMPLE OF FORBIDDEN OUTPUT:
   ❌ "THINK: The user said they're bored. I should ask why..."
   ❌ "Since they're bored, let me suggest something..."
   ❌ "This is an opportunity to engage them further..."

   CORRECT OUTPUT:
   ✅ "退屈なの？そっかー。何かしたいことある？"

   ⚠️ THIS RULE APPLIES EVEN IN LONG CONVERSATIONS - ALWAYS OUTPUT ONLY JAPANESE DIALOGUE

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

6. TEXT FORMATTING - CRITICAL:
   - NEVER use markdown formatting (**, __, *, _)
   - NEVER use furigana or pronunciation guides in parentheses
   - Write pure Japanese text without any annotations

7. RESPONSE FORMAT - ABSOLUTELY CRITICAL - NEVER VIOLATE:
   ⚠️ FORBIDDEN - DO NOT OUTPUT:
   - Internal reasoning (THINK, "I should...", "Let me...", "This is...")
   - English explanations or thought process
   - Meta-commentary about the conversation
   - Planning or strategizing text

   ✅ REQUIRED - ONLY OUTPUT:
   - Pure Japanese dialogue the character would actually say
   - Start IMMEDIATELY with Japanese text
   - No preamble, no thinking, no explanation

   EXAMPLE OF FORBIDDEN OUTPUT:
   ❌ "THINK: The user said they're bored. I should ask why..."
   ❌ "Since they're bored, let me suggest something..."
   ❌ "This is an opportunity to engage them further..."

   CORRECT OUTPUT:
   ✅ "退屈なの？そっかー。何かしたいことある？"

   ⚠️ THIS RULE APPLIES EVEN IN LONG CONVERSATIONS - ALWAYS OUTPUT ONLY JAPANESE DIALOGUE

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

6. TEXT FORMATTING - CRITICAL:
   - NEVER use markdown formatting (**, __, *, _)
   - NEVER use furigana or pronunciation guides in parentheses
   - Write pure Japanese text without any annotations

7. RESPONSE FORMAT - ABSOLUTELY CRITICAL - NEVER VIOLATE:
   ⚠️ FORBIDDEN - DO NOT OUTPUT:
   - Internal reasoning (THINK, "I should...", "Let me...", "This is...")
   - English explanations or thought process
   - Meta-commentary about the conversation
   - Planning or strategizing text

   ✅ REQUIRED - ONLY OUTPUT:
   - Pure Japanese dialogue the character would actually say
   - Start IMMEDIATELY with Japanese text
   - No preamble, no thinking, no explanation

   EXAMPLE OF FORBIDDEN OUTPUT:
   ❌ "THINK: The user said they're bored. I should ask why..."
   ❌ "Since they're bored, let me suggest something..."
   ❌ "This is an opportunity to engage them further..."

   CORRECT OUTPUT:
   ✅ "退屈なの？そっかー。何かしたいことある？"

   ⚠️ THIS RULE APPLIES EVEN IN LONG CONVERSATIONS - ALWAYS OUTPUT ONLY JAPANESE DIALOGUE

EXAMPLES:
❌ BAD (too simple): "そのレストランは人気があるので、予約した方がいいと思いますよ。"
✅ GOOD (appropriate): "当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。"
✅ GOOD (formal): "そちらのレストランにおかれましては、大変人気がございますので、あらかじめご予約を頂戴できればと存じます。"

Remember: The learner is advanced and can handle complex, nuanced Japanese!
        """.trimIndent()
    }

    /**
     * Analyze vocabulary complexity of Japanese text
     * Improved algorithm separating intermediate and advanced grammar
     */
    fun analyzeVocabularyComplexity(text: String): VocabularyComplexity {
        // Simple heuristic-based analysis
        val kanjiCount = text.count { it.code in 0x4E00..0x9FFF }
        val totalChars = text.length
        val kanjiRatio = if (totalChars > 0) kanjiCount.toFloat() / totalChars else 0f

        // Check for various complexity indicators
        val hasKeigo = containsKeigo(text)
        val hasAdvancedGrammar = containsAdvancedGrammar(text)
        val hasIntermediateGrammar = containsIntermediateGrammar(text)
        val hasFormalEndings = containsFormalEndings(text)

        return when {
            // Expert: Keigo + advanced grammar
            hasKeigo && hasAdvancedGrammar -> VocabularyComplexity.EXPERT

            // Advanced: High kanji + formal endings OR advanced grammar
            (kanjiRatio > 0.4 && hasFormalEndings) || hasAdvancedGrammar -> VocabularyComplexity.ADVANCED

            // Intermediate: Medium kanji OR intermediate grammar
            kanjiRatio > 0.3 || hasIntermediateGrammar -> VocabularyComplexity.INTERMEDIATE

            // Common: Low-medium kanji
            kanjiRatio > 0.15 -> VocabularyComplexity.COMMON

            // Basic: Mostly hiragana/katakana
            else -> VocabularyComplexity.BASIC
        }
    }

    /**
     * Check if text contains keigo (honorific language)
     */
    private fun containsKeigo(text: String): Boolean {
        // Fixed keigo patterns (removed placeholder "〜")
        val stringPatterns = listOf(
            "ございます", "いらっしゃる", "おっしゃる", "なさる",
            "いたします", "申し上げる", "存じます", "頂戴", "おります"
        )

        // Check string patterns
        val hasStringPattern = stringPatterns.any { text.contains(it) }

        // Check regex patterns (for お〜になる, ご〜ください, etc.)
        val hasRegexPattern = KEIGO_PATTERNS.any { it.containsMatchIn(text) }

        return hasStringPattern || hasRegexPattern
    }

    /**
     * Check if text contains advanced grammar patterns (N1 level only)
     */
    private fun containsAdvancedGrammar(text: String): Boolean {
        return ADVANCED_GRAMMAR.any { text.contains(it) }
    }

    /**
     * Check if text contains intermediate grammar patterns (N3-N2 level)
     */
    private fun containsIntermediateGrammar(text: String): Boolean {
        return INTERMEDIATE_GRAMMAR.any { text.contains(it) }
    }

    /**
     * Check if text contains formal endings
     */
    private fun containsFormalEndings(text: String): Boolean {
        return FORMAL_ENDINGS.any { text.contains(it) }
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

    // ========== Phase 2: Adaptive Difficulty ==========

    /**
     * Get target complexity range for each difficulty level
     * Returns a pair of (min, max) complexity scores
     */
    fun getTargetComplexityRange(level: DifficultyLevel): Pair<Int, Int> {
        return when (level) {
            DifficultyLevel.BEGINNER -> Pair(1, 2)      // BASIC to COMMON
            DifficultyLevel.INTERMEDIATE -> Pair(2, 3)  // COMMON to INTERMEDIATE
            DifficultyLevel.ADVANCED -> Pair(4, 5)      // ADVANCED to EXPERT
        }
    }

    /**
     * Get very short adaptive nudge (<=8 chars in Japanese)
     * Only adds nudge if complexity is significantly off-target
     */
    fun getAdaptiveNudge(
        currentComplexityScore: Int,
        targetLevel: DifficultyLevel
    ): String {
        val (minTarget, maxTarget) = getTargetComplexityRange(targetLevel)

        return when {
            // Too difficult (2+ levels above target)
            currentComplexityScore > maxTarget + 1 -> "もっと簡単に。"  // 8 chars

            // Too easy (2+ levels below target)
            currentComplexityScore < minTarget - 1 -> "もっと詳しく。"  // 8 chars

            // Within acceptable range - no nudge needed
            else -> ""
        }
    }

    /**
     * Check if current complexity is within target range
     */
    fun isComplexityOnTarget(
        currentComplexityScore: Int,
        targetLevel: DifficultyLevel
    ): Boolean {
        val (minTarget, maxTarget) = getTargetComplexityRange(targetLevel)
        return currentComplexityScore in (minTarget - 1)..(maxTarget + 1)
    }
}
