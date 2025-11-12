package com.nihongo.conversation.core.grammar

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType
import com.nihongo.conversation.domain.model.FuriganaType

/**
 * Kuromoji-based Grammar Analyzer
 *
 * Uses Kuromoji (Japanese morphological analyzer) for accurate, fast, offline grammar analysis.
 * Replaces Gemini API calls for grammar analysis to improve speed and save API quota.
 *
 * Features:
 * - 100% offline (no API calls)
 * - Fast analysis (< 100ms per sentence)
 * - Accurate part-of-speech tagging (MeCab IPADIC dictionary)
 * - Thread-safe singleton with lazy initialization
 *
 * @see com.atilika.kuromoji.ipadic.Tokenizer
 */
object KuromojiGrammarAnalyzer {

    private const val TAG = "KuromojiGrammar"

    // Lazy initialization to avoid loading dictionary on app startup
    private val tokenizer: Tokenizer by lazy {
        android.util.Log.d(TAG, "Initializing Kuromoji tokenizer (first use only)...")
        val start = System.currentTimeMillis()
        val t = Tokenizer()
        val elapsed = System.currentTimeMillis() - start
        android.util.Log.d(TAG, "Tokenizer initialized in ${elapsed}ms")
        t
    }

    /**
     * Analyze Japanese sentence using Kuromoji morphological analyzer
     *
     * @param sentence Japanese text to analyze
     * @param userLevel User JLPT level (1=N5/N4, 2=N3/N2, 3=N1) - affects explanation detail
     * @return GrammarExplanation with components, examples, and patterns
     */
    fun analyzeSentence(sentence: String, userLevel: Int = 1): GrammarExplanation {
        val startTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "Analyzing: '$sentence' (level=$userLevel)")

        try {
            // Tokenize with Kuromoji
            val tokens = tokenizer.tokenize(sentence)
            android.util.Log.d(TAG, "Tokenized into ${tokens.size} tokens")

            // Track position manually since Kuromoji doesn't provide character positions
            var currentPosition = 0

            // Convert tokens to grammar components
            val components = tokens.mapIndexed { index, token ->
                val pos = token.partOfSpeechLevel1  // Main POS (動詞, 助詞, etc.)
                val grammarType = mapPosToGrammarType(token)
                val explanation = generateExplanation(token, userLevel)

                android.util.Log.v(TAG, "  [$index] ${token.surface} → $pos → $grammarType")

                val startIndex = currentPosition
                val endIndex = currentPosition + token.surface.length
                currentPosition = endIndex

                GrammarComponent(
                    text = token.surface,
                    type = grammarType,
                    explanation = explanation,
                    startIndex = startIndex,
                    endIndex = endIndex
                )
            }.filter { component ->
                // Filter out punctuation symbols, keep only meaningful symbols
                if (component.type == GrammarType.SYMBOL) {
                    // Keep meaningful symbols (？！〜・), filter out punctuation (。、（）「」等)
                    component.text in listOf("？", "！", "〜", "・", "?", "!")
                } else {
                    true  // Keep all non-symbol components
                }
            }

            // Generate overall and detailed explanations
            val overallExplanation = generateOverallExplanation(sentence, components, userLevel)
            val detailedExplanation = generateDetailedExplanation(components, userLevel)
            val examples = generateExamples(components)
            val relatedPatterns = getRelatedPatterns(components)

            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.d(TAG, "✅ Analysis completed in ${elapsed}ms")

            return GrammarExplanation(
                originalText = sentence,
                components = components,
                overallExplanation = overallExplanation,
                detailedExplanation = detailedExplanation,
                examples = examples,
                relatedPatterns = relatedPatterns
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Analysis failed: ${e.message}", e)

            // Fallback to LocalGrammarAnalyzer on error
            return LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel)
        }
    }

    /**
     * Map Kuromoji POS tags to GrammarType
     *
     * Kuromoji provides detailed POS information:
     * - partOfSpeechLevel1: Main category (動詞, 助詞, etc.)
     * - partOfSpeechLevel2: Sub-category
     * - partOfSpeechLevel3: Detailed classification
     * - conjugationForm: 活用形 (連用形, 終止形, etc.)
     *
     * Updated 2025-11-02: Accurate mapping to 12 GrammarTypes
     */
    private fun mapPosToGrammarType(token: Token): GrammarType {
        return when (token.partOfSpeechLevel1) {
            "動詞" -> GrammarType.VERB
            "助詞" -> GrammarType.PARTICLE
            "助動詞" -> GrammarType.AUXILIARY
            "名詞" -> GrammarType.NOUN
            "形容詞" -> GrammarType.ADJECTIVE
            "副詞" -> GrammarType.ADVERB
            "連体詞" -> GrammarType.RENTAISHI         // ← Fixed: was ADJECTIVE
            "接続詞" -> GrammarType.CONJUNCTION       // ← Fixed: was EXPRESSION
            "感動詞" -> GrammarType.INTERJECTION      // ← Fixed: was EXPRESSION
            "接頭詞" -> GrammarType.PREFIX            // ← Fixed: was EXPRESSION
            "記号" -> GrammarType.SYMBOL              // ← Fixed: was EXPRESSION
            else -> GrammarType.EXPRESSION
        }
    }

    /**
     * Generate Korean explanation for a token based on POS
     */
    private fun generateExplanation(token: Token, userLevel: Int): String {
        val mainPos = token.partOfSpeechLevel1
        val subPos = token.partOfSpeechLevel2 ?: "*"
        val conjugation = token.conjugationForm ?: "*"

        return when (mainPos) {
            "動詞" -> {
                val baseForm = token.baseForm
                val meaning = JMdictHelper.lookup(baseForm)

                val detail = when (conjugation) {
                    "連用形" -> "연용형 (て형, た형 앞)"
                    "終止形" -> "종지형 (문장 끝)"
                    "未然形" -> "미연형 (ない, う 앞)"
                    "仮定形" -> "가정형 (ば 앞)"
                    "命令形" -> "명령형"
                    "基本形" -> "기본형"
                    else -> "동사"
                }

                if (meaning != null) {
                    if (userLevel == 1) "동사: $meaning ($detail)" else "$meaning ($detail)"
                } else {
                    if (userLevel == 1) "동사: $detail" else detail
                }
            }
            "助詞" -> {
                val particleType = when (subPos) {
                    "格助詞" -> "격조사"
                    "接続助詞" -> "접속조사"
                    "副助詞" -> "부조사"
                    "終助詞" -> "종조사"
                    else -> "조사"
                }
                getParticleExplanation(token.surface, particleType, userLevel)
            }
            "助動詞" -> {
                when (token.surface) {
                    "です" -> "정중체/단정"
                    "だ" -> "단정/평서"
                    "ます" -> "정중체 동사"
                    "た" -> "과거/완료"
                    "ない" -> "부정"
                    "ようだ", "そうだ" -> "추측/양태"
                    "たい" -> "희망"
                    "れる", "られる" -> "수동/가능/존경"
                    "せる", "させる" -> "사역"
                    else -> "조동사"
                }
            }
            "名詞" -> {
                // Try to lookup meaning in dictionary
                val baseForm = token.baseForm  // Get dictionary form
                val meaning = JMdictHelper.lookup(baseForm)
                    ?: JMdictHelper.lookup(token.surface)  // Fallback to surface form

                if (meaning != null) {
                    when (subPos) {
                        "代名詞" -> "대명사: $meaning"
                        "数" -> "숫자: $meaning"
                        "非自立" -> "의존명사: $meaning"
                        else -> "명사: $meaning"
                    }
                } else {
                    // No dictionary entry found, show base form
                    val info = if (baseForm != token.surface) {
                        " (${baseForm})"
                    } else {
                        ""
                    }
                    when (subPos) {
                        "代名詞" -> "대명사$info"
                        "数" -> "숫자$info"
                        "非自立" -> "의존명사$info"
                        else -> "명사$info"
                    }
                }
            }
            "形容詞" -> {
                val baseForm = token.baseForm
                val meaning = JMdictHelper.lookup(baseForm)
                    ?: JMdictHelper.lookup(token.surface)

                if (meaning != null) {
                    if (conjugation != "*") "형용사: $meaning ($conjugation)"
                    else "형용사: $meaning"
                } else {
                    if (conjugation != "*") "형용사 ($conjugation)"
                    else "형용사"
                }
            }
            "副詞" -> {
                val meaning = JMdictHelper.lookup(token.surface)
                    ?: JMdictHelper.lookup(token.baseForm)

                if (meaning != null) "부사: $meaning"
                else "부사"
            }
            "連体詞" -> "연체사"
            "接続詞" -> "접속사"
            "感動詞" -> {
                val meaning = JMdictHelper.lookup(token.surface)
                if (meaning != null) "감탄사: $meaning"
                else "감탄사"
            }
            "接頭詞" -> "접두사"
            "記号" -> when (subPos) {
                "句点" -> "마침표"
                "読点" -> "쉼표"
                else -> "기호"
            }
            else -> "기타"
        }
    }

    /**
     * Get particle-specific explanation (similar to LocalGrammarAnalyzer)
     */
    private fun getParticleExplanation(particle: String, type: String, userLevel: Int): String {
        val info = when (particle) {
            "は" -> "주제 표시 (은/는)"
            "が" -> "주어 표시 (이/가)"
            "を" -> "목적어 표시 (을/를)"
            "に" -> "방향/시간/대상 (에/에게)"
            "へ" -> "방향 (으로)"
            "と" -> "함께/인용 (와/과)"
            "で" -> "수단/장소 (에서/로)"
            "から" -> "시작점 (부터/에서)"
            "まで" -> "종점 (까지)"
            "の" -> "소유/관계 (~의)"
            "も" -> "추가 (도/역시)"
            "や" -> "예시 나열 (이나/와)"
            "か" -> "의문 (~인가)"
            "ね" -> "확인 (~네요)"
            "よ" -> "강조 (~요)"
            else -> type
        }

        return if (userLevel == 1) "$type: $info" else info
    }

    /**
     * Generate overall explanation based on sentence structure
     */
    private fun generateOverallExplanation(
        sentence: String,
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        val hasVerb = components.any { it.type == GrammarType.VERB }
        val hasAuxiliary = components.any { it.type == GrammarType.AUXILIARY }
        val particles = components.filter { it.type == GrammarType.PARTICLE }

        return when {
            sentence.endsWith("ですか") || sentence.endsWith("ますか") ->
                "정중한 의문문입니다. 상대방에게 예의 바르게 질문하는 표현이에요."

            sentence.endsWith("か") ->
                "의문문입니다. 무언가를 묻는 문장이에요."

            sentence.contains("ください") ->
                "요청이나 부탁을 나타내는 문장입니다. 공손하게 부탁하는 표현이에요."

            sentence.endsWith("ました") || sentence.endsWith("でした") ->
                "과거형 정중체 문장입니다. 이미 일어난 일을 정중하게 표현해요."

            sentence.endsWith("ます") || sentence.endsWith("です") ->
                "현재 또는 미래형 정중체 문장입니다. 예의 바른 표현이에요."

            sentence.contains("ません") || components.any { it.text == "ない" } ->
                "부정문입니다. 어떤 동작을 하지 않는다는 의미예요."

            components.any { it.text == "たい" } ->
                "희망이나 욕구를 나타내는 문장입니다. ~하고 싶다는 표현이에요."

            hasVerb && hasAuxiliary && particles.isNotEmpty() ->
                "기본적인 일본어 문장 구조를 갖춘 문장입니다. 동사, 조사, 조동사가 사용되었어요."

            else ->
                "일본어 문장입니다. ${components.size}개의 형태소로 구성되어 있어요."
        }
    }

    /**
     * Generate detailed explanation with component breakdown
     */
    private fun generateDetailedExplanation(
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        val particles = components.filter { it.type == GrammarType.PARTICLE }
        val verbs = components.filter { it.type == GrammarType.VERB }
        val auxiliaries = components.filter { it.type == GrammarType.AUXILIARY }
        val nouns = components.filter { it.type == GrammarType.NOUN }

        val parts = mutableListOf<String>()

        if (nouns.isNotEmpty()) {
            val nounList = nouns.take(3).joinToString(", ") { "'${it.text}'" }
            parts.add("명사 [$nounList] 등이 사용되었습니다.")
        }

        if (particles.isNotEmpty()) {
            val particleList = particles.map { it.text }.distinct().joinToString(", ")
            parts.add("조사 [${particleList}]가 문장 구조를 만들고 있습니다.")
        }

        if (verbs.isNotEmpty()) {
            parts.add("${verbs.size}개의 동사가 사용되었습니다.")
        }

        if (auxiliaries.isNotEmpty()) {
            val auxList = auxiliaries.map { it.text }.distinct().joinToString(", ")
            parts.add("조동사 [${auxList}]로 정중도나 시제를 표현하고 있습니다.")
        }

        when (userLevel) {
            1 -> parts.add("💡 초급 팁: 조사와 동사 활용에 주목하세요.")
            2 -> parts.add("💡 중급 팁: 문장의 뉘앙스와 정중도를 파악해보세요.")
            3 -> parts.add("💡 고급 팁: 더 자연스러운 표현으로 바꿔보는 연습을 해보세요.")
        }

        if (parts.isEmpty()) {
            parts.add("형태소 분석이 완료되었습니다.")
        }

        return parts.joinToString(" ")
    }

    /**
     * Generate example sentences based on detected patterns
     */
    private fun generateExamples(components: List<GrammarComponent>): List<String> {
        val examples = mutableListOf<String>()

        if (components.any { it.text == "は" }) {
            examples.add("私は学生です。(저는 학생입니다)")
        }

        if (components.any { it.text == "を" }) {
            examples.add("本を読みます。(책을 읽습니다)")
        }

        if (components.any { it.text == "ます" }) {
            examples.add("毎日勉強します。(매일 공부합니다)")
        }

        if (components.any { it.text == "たい" }) {
            examples.add("日本へ行きたいです。(일본에 가고 싶습니다)")
        }

        if (components.any { it.text == "ください" }) {
            examples.add("教えてください。(가르쳐 주세요)")
        }

        return examples.take(3) // Limit to 3 examples
    }

    /**
     * Get related grammar patterns
     */
    private fun getRelatedPatterns(components: List<GrammarComponent>): List<String> {
        val patterns = mutableSetOf<String>()

        if (components.any { it.text == "は" }) {
            patterns.add("〜は〜です (주제 제시)")
        }

        if (components.any { it.text == "を" }) {
            patterns.add("〜を〜する (타동사 문형)")
        }

        if (components.any { it.text == "ます" }) {
            patterns.add("ます형 (정중체)")
        }

        if (components.any { it.type == GrammarType.VERB && it.explanation.contains("連用形") }) {
            patterns.add("〜て형 (연용형 활용)")
        }

        if (components.any { it.text == "たい" }) {
            patterns.add("〜たい (희망 표현)")
        }

        if (components.any { it.text == "ない" }) {
            patterns.add("〜ない (부정형)")
        }

        return patterns.take(4).toList()
    }

    /**
     * Pre-load tokenizer (optional, call on app startup for faster first analysis)
     */
    fun preload() {
        android.util.Log.d(TAG, "Preloading Kuromoji tokenizer...")
        tokenizer // Access to trigger lazy initialization
    }

    /**
     * Convert Japanese text (including kanji) to hiragana readings
     *
     * Uses Kuromoji's reading information to convert kanji to hiragana.
     * This is useful for pronunciation guides.
     *
     * @param japanese Japanese text (may contain kanji, hiragana, katakana)
     * @return List of reading strings for each token
     *
     * Example:
     * - Input: "注文してください"
     * - Output: ["ちゅうもん", "して", "ください"]
     */
    fun getReadings(japanese: String): List<String> {
        return try {
            tokenizer.tokenize(japanese).map { token ->
                // Use reading if available (converts kanji to hiragana)
                // Otherwise use surface form (already hiragana/katakana)
                token.reading ?: token.surface
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get readings", e)
            // Fallback: return original text as single token
            listOf(japanese)
        }
    }

    /**
     * Adds furigana (reading guide) to kanji characters in Japanese text
     *
     * Examples:
     * - Input: "注文してください", type: HIRAGANA
     *   Output: "注文(ちゅうもん)してください"
     *
     * - Input: "注文してください", type: KATAKANA
     *   Output: "注文(チュウモン)してください"
     *
     * @param text Japanese text that may contain kanji
     * @param type Furigana display type (HIRAGANA or KATAKANA)
     * @return Text with furigana in parentheses after kanji words
     */
    fun addFuriganaToKanji(text: String, type: FuriganaType = FuriganaType.HIRAGANA): String {
        return try {
            val tokens = tokenizer.tokenize(text)
            val result = StringBuilder()

            tokens.forEach { token ->
                val surface = token.surface
                val reading = token.reading

                // Check if this token contains kanji
                val hasKanji = surface.any { char ->
                    char in '\u4E00'..'\u9FFF' // CJK Unified Ideographs range
                }

                if (hasKanji && reading != null && reading != surface) {
                    // Convert reading to desired type
                    val displayReading = when (type) {
                        FuriganaType.HIRAGANA -> reading  // Kuromoji provides hiragana by default
                        FuriganaType.KATAKANA -> hiraganaToKatakana(reading)
                    }
                    // Add furigana: "注文(ちゅうもん)" or "注文(チュウモン)"
                    result.append(surface).append("(").append(displayReading).append(")")
                } else {
                    // No kanji or no reading available, use as-is
                    result.append(surface)
                }
            }

            result.toString()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to add furigana", e)
            // Fallback: return original text
            text
        }
    }

    /**
     * Convert hiragana string to katakana
     *
     * Examples:
     * - "ちゅうもん" → "チュウモン"
     * - "ください" → "クダサイ"
     *
     * Uses Unicode character offset:
     * - Hiragana range: U+3040 ~ U+309F
     * - Katakana range: U+30A0 ~ U+30FF
     * - Offset: 0x60 (96)
     */
    private fun hiraganaToKatakana(hiragana: String): String {
        return hiragana.map { char ->
            if (char in '\u3040'..'\u309F') {
                // Convert hiragana to katakana by adding offset
                (char.code + 0x60).toChar()
            } else {
                // Not hiragana, keep as-is (katakana, punctuation, etc.)
                char
            }
        }.joinToString("")
    }
}
