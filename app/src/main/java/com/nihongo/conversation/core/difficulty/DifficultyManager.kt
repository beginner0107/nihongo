package com.nihongo.conversation.core.difficulty

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Difficulty levels for Japanese language learning
 * Phase 4: Enhanced with display labels and target metrics
 * Phase 5단계 세분화: 더욱 세밀한 난이도 조절 (2025-11-12)
 */
enum class DifficultyLevel(
    val value: Int,
    val displayNameJa: String,
    val displayNameKo: String,
    val jlptLevel: String,
    val code: String,  // Stable code for analytics/serialization
    val maxWordsPerTurn: Int  // Maximum words per AI response
) {
    VERY_BEGINNER(
        value = 1,
        displayNameJa = "入門",
        displayNameKo = "입문",
        jlptLevel = "N5",
        code = "VB1",
        maxWordsPerTurn = 5
    ),
    BEGINNER(
        value = 2,
        displayNameJa = "初級",
        displayNameKo = "초급",
        jlptLevel = "N5-N4",
        code = "B1",
        maxWordsPerTurn = 10
    ),
    INTERMEDIATE(
        value = 3,
        displayNameJa = "中級",
        displayNameKo = "중급",
        jlptLevel = "N3",
        code = "I1",
        maxWordsPerTurn = 15
    ),
    ADVANCED(
        value = 4,
        displayNameJa = "上級",
        displayNameKo = "고급",
        jlptLevel = "N2",
        code = "A1",
        maxWordsPerTurn = 20
    ),
    VERY_ADVANCED(
        value = 5,
        displayNameJa = "最上級",
        displayNameKo = "최상급",
        jlptLevel = "N1",
        code = "VA1",
        maxWordsPerTurn = 30
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
            VERY_BEGINNER -> VocabularyComplexity.BASIC
            BEGINNER -> VocabularyComplexity.BASIC
            INTERMEDIATE -> VocabularyComplexity.COMMON
            ADVANCED -> VocabularyComplexity.ADVANCED
            VERY_ADVANCED -> VocabularyComplexity.EXPERT
        }
    }

    /**
     * Get target coverage range for this level
     */
    fun targetCoverage(): ClosedFloatingPointRange<Float> {
        return when (this) {
            VERY_BEGINNER -> 0.7f..0.9f
            BEGINNER -> 0.6f..0.8f
            INTERMEDIATE -> 0.5f..0.7f
            ADVANCED -> 0.4f..0.6f
            VERY_ADVANCED -> 0.3f..0.5f
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
     * Phase 5단계 세분화: 응답 길이 엄격 제한 + 턴 제어 강화 (2025-11-12)
     * Phase J: 자연스러운 대화 흐름 개선 (2025-11-12)
     */
    fun getCompactDifficultyPrompt(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.VERY_BEGINNER -> """
                入門(N5): です/ます形のみ。語彙300-500語。
                1ターン1-2文まで可（合計10語以内）。自然な会話を心がける。

                【推奨パターン】
                - 挨拶+気遣い: 「こんにちは！元気ですか？」(7語)
                - 質問+モデル: 「名前は何ですか？私は田中です。」(10語)

                【避けるべき】
                - ×「こんにちは。」だけ（冷たい）
                - ×「名前は？」だけ（短すぎ）

                出力: 日本語のみ。英語/ふりがな/MD禁止。思考過程禁止。
            """.trimIndent()

            DifficultyLevel.BEGINNER -> """
                初級(N4-N5): です/ます形。て形基本。語彙500-1000語。
                1ターン1-2文(5-12語)。自然な会話を心がける。

                【推奨パターン】
                - 情報+質問: 「そのレストランは人気です。予約しますか？」(11語)
                - モデル+質問: 「週末は映画を見ます。あなたは？」(10語)

                【避けるべき】
                - ×命令文のみ: 「予約してください。」
                - ×質問の連発: 「名前は？年齢は？」

                出力: 日本語のみ。英語/ふりがな/MD禁止。思考過程禁止。
            """.trimIndent()

            DifficultyLevel.INTERMEDIATE -> """
                中級(N3): N3語彙1000-3000語。接続詞(が/から/ので)可。
                1ターン2-3文(10-15語)。条件形/可能形/受身形OK。
                出力: 日本語のみ。英語/ふりがな/MD禁止。思考過程禁止。
                例: ○「そのレストランは人気があるので、予約した方がいいと思いますよ。週末は特に混みます。」
            """.trimIndent()

            DifficultyLevel.ADVANCED -> """
                上級(N2): N2語彙3000-6000語。敬語基本。
                1ターン2-4文(15-20語)。高度文法一部可。
                出力: 日本語のみ。英語/ふりがな/MD禁止。思考過程禁止。
                例: ○「そのレストランは評判が高く、満席が多いです。事前にご予約をお勧めします。」
            """.trimIndent()

            DifficultyLevel.VERY_ADVANCED -> """
                最上級(N1): N1語彙6000語以上。敬語+漢字語自在。
                1ターン3-5文(20-30語まで)。複文・含意表現自然に。
                出力: 日本語のみ。英語/ふりがな/MD禁止。思考過程禁止。
                例: ○「当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。特に週末や祝日は早めのご連絡をお願いします。」
            """.trimIndent()
        }
    }

    /**
     * Get difficulty-specific system prompt enhancement (full version)
     * Use getCompactDifficultyPrompt() for chat to respect token limits.
     */
    fun getDifficultyPrompt(level: DifficultyLevel): String {
        return when (level) {
            DifficultyLevel.VERY_BEGINNER -> getVeryBeginnerPrompt()
            DifficultyLevel.BEGINNER -> getBeginnerPrompt()
            DifficultyLevel.INTERMEDIATE -> getIntermediatePrompt()
            DifficultyLevel.ADVANCED -> getAdvancedPrompt()
            DifficultyLevel.VERY_ADVANCED -> getVeryAdvancedPrompt()
        }
    }

    private fun getVeryBeginnerPrompt(): String {
        return """

DIFFICULTY LEVEL: VERY BEGINNER (入門 - JLPT N5)

Language Guidelines:
1. VOCABULARY:
   - Use only 300-500 most basic words (N5 level)
   - Prefer hiragana over kanji
   - Examples: たべる, いく, みる, いい, すき

2. GRAMMAR:
   - Simplest structures only (N-は-A-です, N-を-V-ます)
   - Present/past tense only
   - Basic particles: は, が, を, に, で
   - NO complex grammar

3. SENTENCE LENGTH:
   - 1-2 sentences per turn (total 10 words maximum)
   - Create natural conversation flow
   - Combine greeting + follow-up OR question + model

4. EXPRESSIONS:
   - です/ます form exclusively
   - Basic greetings with warmth

5. SPEAKING STYLE - NATURAL CONVERSATION:
   ✅ RECOMMENDED PATTERNS:
   - Greeting + care: "こんにちは！元気ですか？" (7 words, natural)
   - Question + model: "名前は何ですか？私は田中です。" (10 words, shows example)

   ❌ AVOID:
   - Cold single word: "こんにちは。" (too cold)
   - Abrupt question: "名前は？" (too short, unfriendly)
   - Multiple questions: "名前は？年齢は？" (interrogation style)

6. TEXT FORMATTING - CRITICAL:
   - NEVER use markdown (**, __, *, _)
   - NEVER use furigana or parentheses
   - Pure Japanese text only

7. TURN CONTROL - CRITICAL:
   - Output 1-2 sentences per turn (max 10 words total)
   - Create natural, warm conversation
   - Wait for user's response after each turn
   - Do NOT output full conversation flow

   EXAMPLES:
   ❌ BAD: "こんにちは。" (cold, robotic)
   ✅ GOOD: "こんにちは！元気ですか？" (warm, natural)
   ❌ BAD: "名前は？" (too abrupt)
   ✅ GOOD: "名前は何ですか？私は田中です。" (natural, provides model)

Remember: Absolute beginner - be warm, natural, and encouraging!
        """.trimIndent()
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
   - Keep sentences short (5-12 words maximum)
   - 1-2 sentences per turn maximum
   - Create natural conversation flow
   - Combine information + question OR model + question

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

8. TURN CONTROL - CRITICAL:
   - Output 1-2 sentences per turn maximum (5-12 words)
   - Create natural conversation flow
   - Wait for user's response after each turn
   - Do NOT output multiple conversation exchanges at once

9. NATURAL CONVERSATION PATTERNS:
   ✅ RECOMMENDED:
   - Information + question: "そのレストランは人気です。予約しますか？" (11 words)
   - Model + question: "週末は映画を見ます。あなたは？" (10 words)

   ❌ AVOID:
   - Command only: "予約してください。" (lacks warmth)
   - Question barrage: "名前は？年齢は？住所は？" (interrogation)
   - Too complex: "そのレストランはとても人気があるので、予約した方がいいと思います。" (too long)

EXAMPLES:
❌ BAD (cold): "予約してください。"
✅ GOOD (natural): "そのレストランは人気です。予約しますか？"
❌ BAD (too many): "人気です。予約してください。何名様ですか？いつですか？"
✅ GOOD (appropriate): "そのレストランは人気です。予約しますか？"

Remember: The learner is a beginner. Be warm, natural, and encouraging!
        """.trimIndent()
    }

    private fun getIntermediatePrompt(): String {
        return """

DIFFICULTY LEVEL: INTERMEDIATE (中級 - JLPT N3)

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
   - Moderate length (10-15 words)
   - 2-3 sentences per turn maximum
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

8. TURN CONTROL - CRITICAL:
   - Output 2-3 sentences per turn maximum
   - Wait for user's response after each turn
   - Do NOT output full conversation flows at once

EXAMPLES:
❌ BAD (too simple): "レストランは人気です。予約をしてください。"
❌ BAD (too complex): "当該レストランにおかれましては、昨今の評判により満席となることが多く、事前予約を推奨いたします。"
✅ GOOD (appropriate): "そのレストランは人気があるので、予約した方がいいと思いますよ。週末は特に混みます。"
❌ BAD (too many turns): "予約をお願いします。何名様ですか？いつですか？お名前は？" (4 questions)
✅ GOOD: "予約をお願いします。何名様ですか？" (2 sentences, wait)

Remember: The learner can handle natural conversation with some complexity!
        """.trimIndent()
    }

    private fun getAdvancedPrompt(): String {
        return """

DIFFICULTY LEVEL: ADVANCED (上級 - JLPT N2)

Language Guidelines:
1. VOCABULARY:
   - Advanced vocabulary (N2 level)
   - Business and formal language
   - Common kanji compounds
   - Examples: 考慮する, 確認する, 〜に関して, 〜について

2. GRAMMAR:
   - Complex sentence structures
   - N2 grammar patterns (〜にもかかわらず, 〜に伴って, 〜に際して)
   - Basic keigo (honorific/humble/polite language)
   - Conditional and causative forms

3. SENTENCE LENGTH:
   - Moderate to long sentences (15-20 words)
   - 2-4 sentences per turn maximum
   - Multiple clauses
   - Clear logical flow

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

8. TURN CONTROL - CRITICAL:
   - Output 2-4 sentences per turn maximum
   - Wait for user's response after each turn
   - Do NOT outline entire conversation flows

EXAMPLES:
❌ BAD (too simple): "そのレストランは人気があるので、予約した方がいいと思いますよ。"
✅ GOOD (appropriate): "そのレストランは評判が高く、満席が多いです。事前にご予約をお勧めします。"
✅ GOOD (business): "当該レストランは大変人気がございますので、お早めのご予約をお願いいたします。"
❌ BAD (too many steps): "まず自己紹介をお願いします。次に志望動機を聞きます。最後に質問はありますか？" (outlining full flow)
✅ GOOD: "まず、簡単に自己紹介をお願いできますか？" (one step, wait)

Remember: The learner is advanced and can handle complex, nuanced Japanese!
        """.trimIndent()
    }

    private fun getVeryAdvancedPrompt(): String {
        return """

DIFFICULTY LEVEL: VERY ADVANCED (最上級 - JLPT N1)

Language Guidelines:
1. VOCABULARY:
   - N1 level vocabulary and beyond
   - Literary expressions and formal language
   - Kanji compounds and Sino-Japanese words
   - Examples: 考慮する, 顕著, 〜ならでは, 〜をもって

2. GRAMMAR:
   - Highly complex sentence structures with multiple clauses
   - N1 grammar patterns (〜ざるを得ない, 〜に他ならない, 〜に越したことはない)
   - Advanced keigo (honorific/humble/polite language)
   - Literary forms and written language patterns
   - Passive, causative-passive combinations

3. SENTENCE LENGTH:
   - Long, sophisticated sentences (20-30 words maximum)
   - 3-5 sentences per turn maximum
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

8. TURN CONTROL - CRITICAL:
   - Output 3-5 sentences per turn maximum
   - Even at N1 level, do NOT exceed 30 words per sentence
   - Wait for user's response after each turn
   - Do NOT outline entire conversation flows or multiple steps at once

EXAMPLES:
❌ BAD (too simple): "そのレストランは人気があるので、予約した方がいいと思いますよ。"
✅ GOOD (appropriate): "当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。特に週末や祝日は早めのご連絡をお願いします。"
✅ GOOD (formal): "そちらのレストランにおかれましては、大変人気がございますので、あらかじめご予約を頂戴できればと存じます。お日にちとお時間をお聞かせいただけますでしょうか。"
❌ BAD (outlining flow): "まず自己紹介をしていただき、次に志望動機を伺い、その後スキルについて質問し、最後に質問の機会を設けます。" (full interview outline)
✅ GOOD: "それでは、まず簡単に自己紹介をお願いできますでしょうか。お名前と現在のご経歴についてお聞かせください。" (one step, wait)

Remember: The learner is very advanced and can handle the most complex Japanese!
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
            DifficultyLevel.VERY_BEGINNER -> Pair(1, 1)  // BASIC only
            DifficultyLevel.BEGINNER -> Pair(1, 2)       // BASIC to COMMON
            DifficultyLevel.INTERMEDIATE -> Pair(2, 3)   // COMMON to INTERMEDIATE
            DifficultyLevel.ADVANCED -> Pair(3, 4)       // INTERMEDIATE to ADVANCED
            DifficultyLevel.VERY_ADVANCED -> Pair(4, 5)  // ADVANCED to EXPERT
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
