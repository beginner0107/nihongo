package com.nihongo.conversation.core.grammar

import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType

/**
 * Enhanced Local Grammar Analyzer
 * Provides fast, offline grammar analysis for common patterns
 * Used as primary analyzer for simple sentences and fallback for API failures
 */
object LocalGrammarAnalyzer {

    // Cache for analyzed sentences
    private val cache = mutableMapOf<String, GrammarExplanation>()

    /**
     * Check if sentence can be analyzed locally (avoiding API call)
     */
    fun canAnalyzeLocally(sentence: String): Boolean {
        // Multi-line sentences should use API
        if (sentence.contains("\n")) return false

        // Long sentences are too complex for local analysis
        if (sentence.length > 50) return false

        // Check cache first
        if (cache.containsKey(sentence)) return true

        // Short sentences with common patterns
        if (sentence.length < 30) {
            val commonEndings = listOf(
                "です", "ます", "ました", "ません", "でした",
                "ですか", "ますか", "ましたか", "ませんか",
                "ください", "たい", "ない", "なかった"
            )
            if (commonEndings.any { sentence.endsWith(it) }) return true
        }

        // Common greetings and expressions
        val commonPhrases = listOf(
            "こんにちは", "おはよう", "こんばんは", "さようなら",
            "ありがとう", "すみません", "ごめん", "お願いします",
            "はじめまして", "よろしく", "いただきます", "ごちそうさま"
        )
        if (commonPhrases.any { sentence.contains(it) }) return true

        return false
    }

    /**
     * Enhanced sentence analysis with better pattern recognition
     */
    fun analyzeSentence(sentence: String, userLevel: Int = 1): GrammarExplanation {
        // Return cached result if available
        cache[sentence]?.let { return it }

        val components = mutableListOf<GrammarComponent>()

        // Enhanced particle detection with detailed explanations
        val particlesWithExplanations = mapOf(
            "は" to ParticleInfo("주제 표시", "은/는", "문장의 주제를 나타냅니다"),
            "が" to ParticleInfo("주어 표시", "이/가", "동작의 주체를 나타냅니다"),
            "を" to ParticleInfo("목적어 표시", "을/를", "동작의 대상을 나타냅니다"),
            "に" to ParticleInfo("방향/시간", "에/에게", "장소, 시간, 대상을 나타냅니다"),
            "へ" to ParticleInfo("방향", "으로", "이동의 방향을 나타냅니다"),
            "と" to ParticleInfo("함께/나열", "와/과", "동반자나 나열을 나타냅니다"),
            "で" to ParticleInfo("수단/장소", "에서/로", "행동 장소나 수단을 나타냅니다"),
            "から" to ParticleInfo("시작점", "부터/에서", "시간이나 장소의 시작점"),
            "まで" to ParticleInfo("종점", "까지", "범위의 끝을 나타냅니다"),
            "の" to ParticleInfo("소유/관계", "~의", "소유나 관계를 나타냅니다"),
            "も" to ParticleInfo("추가", "도/역시", "같은 내용의 추가를 나타냅니다"),
            "や" to ParticleInfo("불완전 나열", "이나/와", "예시적 나열을 나타냅니다"),
            "か" to ParticleInfo("의문", "~인가", "질문을 나타냅니다"),
            "ね" to ParticleInfo("확인", "~네요", "동의나 확인을 구합니다"),
            "よ" to ParticleInfo("강조", "~요", "단정이나 강조를 나타냅니다"),
            "だけ" to ParticleInfo("한정", "~만", "범위를 한정합니다"),
            "ばかり" to ParticleInfo("정도", "~뿐", "대략적 정도를 나타냅니다"),
            "など" to ParticleInfo("등등", "~등", "예시를 나타냅니다")
        )

        // Detect particles
        particlesWithExplanations.forEach { (particle, info) ->
            var index = sentence.indexOf(particle)
            while (index != -1) {
                val explanation = if (userLevel == 1) {
                    "${info.korean}: ${info.meaning}"
                } else {
                    "${info.function} (${info.korean})"
                }

                components.add(
                    GrammarComponent(
                        text = particle,
                        type = GrammarType.PARTICLE,
                        explanation = explanation,
                        startIndex = index,
                        endIndex = index + particle.length
                    )
                )
                index = sentence.indexOf(particle, index + 1)
            }
        }

        // Enhanced verb patterns with more variations
        val verbPatterns = mapOf(
            // Polite forms
            "ます" to VerbInfo("정중 현재/미래", "동작을 정중하게 표현"),
            "ました" to VerbInfo("정중 과거", "과거 동작을 정중하게 표현"),
            "ません" to VerbInfo("정중 부정", "동작을 하지 않음을 정중하게 표현"),
            "ませんでした" to VerbInfo("정중 과거 부정", "과거에 하지 않았음을 표현"),
            "ましょう" to VerbInfo("정중 권유", "함께 하자는 제안"),

            // Te-form patterns
            "ている" to VerbInfo("진행형", "현재 진행 중인 동작"),
            "ていた" to VerbInfo("과거 진행", "과거에 진행 중이던 동작"),
            "てください" to VerbInfo("정중한 요청", "공손하게 부탁하는 표현"),
            "てもいい" to VerbInfo("허가", "해도 된다는 허락"),
            "てはいけない" to VerbInfo("금지", "하면 안 된다는 금지"),

            // Want/Need patterns
            "たい" to VerbInfo("희망", "하고 싶다는 욕구 표현"),
            "たかった" to VerbInfo("과거 희망", "하고 싶었던 과거 욕구"),
            "たくない" to VerbInfo("희망 부정", "하고 싶지 않다는 표현"),

            // Can/Cannot patterns
            "できる" to VerbInfo("가능", "할 수 있다는 능력"),
            "できない" to VerbInfo("불가능", "할 수 없다는 표현"),

            // Plain forms
            "ない" to VerbInfo("부정형", "동작을 하지 않음"),
            "なかった" to VerbInfo("과거 부정", "과거에 하지 않았음"),
            "だろう" to VerbInfo("추측", "아마도 그럴 것이라는 추측"),
            "かもしれない" to VerbInfo("가능성", "그럴지도 모른다는 표현")
        )

        // Detect verb patterns
        verbPatterns.forEach { (pattern, info) ->
            var index = sentence.indexOf(pattern)
            while (index != -1) {
                val explanation = if (userLevel == 1) {
                    "${info.type}: ${info.meaning}"
                } else {
                    info.type
                }

                components.add(
                    GrammarComponent(
                        text = pattern,
                        type = GrammarType.VERB,
                        explanation = explanation,
                        startIndex = index,
                        endIndex = index + pattern.length
                    )
                )
                index = sentence.indexOf(pattern, index + 1)
            }
        }

        // Common expressions and sentence endings
        val expressions = mapOf(
            // Copula patterns
            "です" to ExpressionInfo(GrammarType.AUXILIARY, "정중 종결", "정중한 문장 종결"),
            "でした" to ExpressionInfo(GrammarType.AUXILIARY, "정중 과거", "과거를 정중하게 표현"),
            "ですか" to ExpressionInfo(GrammarType.EXPRESSION, "정중 의문", "정중한 질문"),
            "でしょう" to ExpressionInfo(GrammarType.AUXILIARY, "추측", "아마도 그럴 것이라는 추측"),

            // Request patterns
            "ください" to ExpressionInfo(GrammarType.EXPRESSION, "요청", "공손한 부탁"),
            "お願いします" to ExpressionInfo(GrammarType.EXPRESSION, "정중한 부탁", "매우 공손한 부탁"),

            // Greeting patterns
            "ありがとう" to ExpressionInfo(GrammarType.EXPRESSION, "감사", "고마움을 표현"),
            "ごめん" to ExpressionInfo(GrammarType.EXPRESSION, "사과", "미안함을 표현"),
            "すみません" to ExpressionInfo(GrammarType.EXPRESSION, "사과/실례", "죄송함이나 실례를 표현"),

            // Other common patterns
            "そうです" to ExpressionInfo(GrammarType.EXPRESSION, "동의/전문", "그렇다는 동의나 전해들은 정보"),
            "と思います" to ExpressionInfo(GrammarType.EXPRESSION, "생각 표현", "개인적 의견이나 생각")
        )

        // Detect expressions
        expressions.forEach { (pattern, info) ->
            var index = sentence.indexOf(pattern)
            while (index != -1) {
                val explanation = if (userLevel == 1) {
                    "${info.type}: ${info.meaning}"
                } else {
                    info.type
                }

                components.add(
                    GrammarComponent(
                        text = pattern,
                        type = info.grammarType,
                        explanation = explanation,
                        startIndex = index,
                        endIndex = index + pattern.length
                    )
                )
                index = sentence.indexOf(pattern, index + 1)
            }
        }

        // Sort and remove overlaps
        val sortedComponents = components
            .sortedBy { it.startIndex }
            .fold(mutableListOf<GrammarComponent>()) { acc, component ->
                if (acc.isEmpty() || acc.last().endIndex <= component.startIndex) {
                    acc.add(component)
                }
                acc
            }

        // Generate explanations
        val overallExplanation = generateOverallExplanation(sentence, sortedComponents, userLevel)
        val detailedExplanation = generateDetailedExplanation(sentence, sortedComponents, userLevel)

        val result = GrammarExplanation(
            originalText = sentence,
            components = sortedComponents,
            overallExplanation = overallExplanation,
            detailedExplanation = detailedExplanation,
            examples = generateExamples(sortedComponents),
            relatedPatterns = getRelatedPatterns(sortedComponents)
        )

        // Cache the result
        if (cache.size < 100) { // Limit cache size
            cache[sentence] = result
        }

        return result
    }

    /**
     * Generate overall explanation based on sentence structure
     */
    private fun generateOverallExplanation(
        sentence: String,
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        return when {
            // Questions
            sentence.endsWith("ですか") || sentence.endsWith("ますか") ->
                "정중한 의문문입니다. 상대방에게 예의 바르게 질문하는 표현이에요."

            sentence.endsWith("か") ->
                "의문문입니다. 무언가를 묻는 문장이에요."

            // Requests
            sentence.contains("ください") ->
                "요청이나 부탁을 나타내는 문장입니다. 공손하게 부탁하는 표현이에요."

            sentence.contains("お願いします") ->
                "매우 정중한 부탁 표현입니다."

            // Past tense
            sentence.endsWith("ました") || sentence.endsWith("でした") ->
                "과거형 정중체 문장입니다. 이미 일어난 일을 정중하게 표현해요."

            // Present/Future polite
            sentence.endsWith("ます") || sentence.endsWith("です") ->
                "현재 또는 미래형 정중체 문장입니다. 예의 바른 표현이에요."

            // Negative
            sentence.contains("ません") || sentence.contains("ない") ->
                "부정문입니다. 어떤 동작을 하지 않는다는 의미예요."

            // Want/Desire
            sentence.contains("たい") ->
                "희망이나 욕구를 나타내는 문장입니다."

            // Progressive
            sentence.contains("ている") ->
                "진행형 문장입니다. 현재 진행 중이거나 상태를 나타내요."

            // Conditional/Speculative
            sentence.contains("でしょう") || sentence.contains("だろう") ->
                "추측이나 가능성을 나타내는 문장입니다."

            // Default based on components
            components.any { it.type == GrammarType.PARTICLE } ->
                "기본적인 일본어 문장 구조를 갖춘 문장입니다."

            else ->
                "일본어 표현입니다."
        }
    }

    /**
     * Generate detailed explanation with component analysis
     */
    private fun generateDetailedExplanation(
        sentence: String,
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        val particles = components.filter { it.type == GrammarType.PARTICLE }
        val verbs = components.filter { it.type == GrammarType.VERB }
        val expressions = components.filter { it.type == GrammarType.EXPRESSION || it.type == GrammarType.AUXILIARY }

        val parts = mutableListOf<String>()

        // Analyze particles
        if (particles.isNotEmpty()) {
            val particleNames = particles.map { it.text }.distinct().joinToString(", ")
            parts.add("조사 [${particleNames}]가 사용되어 문장 구조를 만들고 있습니다.")
        }

        // Analyze verbs
        if (verbs.isNotEmpty()) {
            val verbTypes = verbs.map {
                when {
                    it.text.contains("ます") -> "정중체"
                    it.text.contains("ている") -> "진행형"
                    it.text.contains("ない") -> "부정형"
                    it.text.contains("たい") -> "희망형"
                    else -> "동사"
                }
            }.distinct().joinToString(", ")
            parts.add("${verbTypes} 활용이 사용되었습니다.")
        }

        // Analyze expressions
        if (expressions.isNotEmpty()) {
            parts.add("일본어 특유의 표현이 포함되어 있습니다.")
        }

        // Level-specific advice
        when (userLevel) {
            1 -> parts.add("💡 초급자 팁: 조사와 동사 활용에 주목하세요.")
            2 -> parts.add("💡 중급자 팁: 문장의 뉘앙스와 정중도를 파악해보세요.")
            3 -> parts.add("💡 고급자 팁: 더 자연스러운 표현으로 바꿔보는 연습을 해보세요.")
        }

        if (parts.isEmpty()) {
            parts.add("기본적인 일본어 문장입니다.")
        }

        return parts.joinToString(" ")
    }

    /**
     * Generate example sentences
     */
    private fun generateExamples(components: List<GrammarComponent>): List<String> {
        val examples = mutableListOf<String>()

        // Generate examples based on detected patterns
        if (components.any { it.text == "は" }) {
            examples.add("私は学生です。(저는 학생입니다)")
        }

        if (components.any { it.text == "を" }) {
            examples.add("本を読みます。(책을 읽습니다)")
        }

        if (components.any { it.text.contains("ます") }) {
            examples.add("毎日勉強します。(매일 공부합니다)")
        }

        if (components.any { it.text.contains("ください") }) {
            examples.add("待ってください。(기다려 주세요)")
        }

        return examples.take(2) // Limit to 2 examples
    }

    /**
     * Get related grammar patterns
     */
    private fun getRelatedPatterns(components: List<GrammarComponent>): List<String> {
        val patterns = mutableSetOf<String>()

        // Add patterns based on components
        if (components.any { it.text == "は" }) {
            patterns.add("〜は〜です (주제 제시)")
        }

        if (components.any { it.text == "を" }) {
            patterns.add("〜を〜する (타동사 문형)")
        }

        if (components.any { it.text.contains("ます") }) {
            patterns.add("ます형 (정중체)")
        }

        if (components.any { it.text.contains("ている") }) {
            patterns.add("〜ている (진행/상태)")
        }

        if (components.any { it.text.contains("たい") }) {
            patterns.add("〜たい (희망 표현)")
        }

        return patterns.take(3).toList() // Limit to 3 patterns
    }

    // Data classes for better organization
    private data class ParticleInfo(
        val function: String,
        val korean: String,
        val meaning: String
    )

    private data class VerbInfo(
        val type: String,
        val meaning: String
    )

    private data class ExpressionInfo(
        val grammarType: GrammarType,
        val type: String,
        val meaning: String
    )
}