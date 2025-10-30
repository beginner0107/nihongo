package com.nihongo.conversation.core.grammar

import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType

/**
 * Local fallback grammar analyzer
 * Used when API fails - provides basic pattern matching
 */
object LocalGrammarAnalyzer {

    /**
     * Analyze sentence using local pattern matching
     * Returns basic grammar analysis without API call
     */
    fun analyzeSentence(sentence: String, userLevel: Int = 1): GrammarExplanation {
        val components = mutableListOf<GrammarComponent>()
        var currentIndex = 0

        // Detect particles (助詞)
        val particles = listOf("は", "が", "を", "に", "へ", "と", "で", "から", "まで", "の", "も", "や", "か")
        particles.forEach { particle ->
            var index = sentence.indexOf(particle, currentIndex)
            while (index != -1) {
                components.add(
                    GrammarComponent(
                        text = particle,
                        type = GrammarType.PARTICLE,
                        explanation = getParticleExplanation(particle, userLevel),
                        startIndex = index,
                        endIndex = index + particle.length
                    )
                )
                index = sentence.indexOf(particle, index + 1)
            }
        }

        // Detect common verb endings (動詞)
        val verbPatterns = listOf(
            "ます" to "정중한 현재/미래형",
            "ました" to "정중한 과거형",
            "ません" to "정중한 부정형",
            "ませんでした" to "정중한 과거 부정형",
            "ている" to "진행형",
            "てください" to "공손한 부탁",
            "たい" to "희망 표현",
            "ない" to "부정형"
        )

        verbPatterns.forEach { (pattern, explanation) ->
            var index = sentence.indexOf(pattern)
            while (index != -1) {
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

        // Detect common expressions
        val expressions = listOf(
            "ですか" to ("의문문 어미", GrammarType.EXPRESSION),
            "です" to ("정중한 단정형", GrammarType.AUXILIARY),
            "でした" to ("정중한 과거형", GrammarType.AUXILIARY),
            "ください" to ("공손한 요청", GrammarType.EXPRESSION),
            "お願いします" to ("부탁 표현", GrammarType.EXPRESSION),
            "ありがとう" to ("감사 표현", GrammarType.EXPRESSION),
            "ごめん" to ("사과 표현", GrammarType.EXPRESSION),
            "すみません" to ("사과/부탁 표현", GrammarType.EXPRESSION)
        )

        expressions.forEach { (pattern, typeInfo) ->
            val (explanation, type) = typeInfo
            var index = sentence.indexOf(pattern)
            while (index != -1) {
                components.add(
                    GrammarComponent(
                        text = pattern,
                        type = type,
                        explanation = explanation,
                        startIndex = index,
                        endIndex = index + pattern.length
                    )
                )
                index = sentence.indexOf(pattern, index + 1)
            }
        }

        // Sort components by start index and remove overlaps
        val sortedComponents = components
            .sortedBy { it.startIndex }
            .distinctBy { it.startIndex } // Remove duplicates at same position

        // Generate overall explanation
        val overallExplanation = generateOverallExplanation(sentence, sortedComponents, userLevel)

        // Generate detailed explanation
        val detailedExplanation = generateDetailedExplanation(sentence, sortedComponents, userLevel)

        return GrammarExplanation(
            originalText = sentence,
            components = sortedComponents,
            overallExplanation = overallExplanation,
            detailedExplanation = detailedExplanation,
            examples = emptyList(),
            relatedPatterns = getRelatedPatterns(sortedComponents)
        )
    }

    /**
     * Get explanation for particle
     */
    private fun getParticleExplanation(particle: String, userLevel: Int): String {
        return when (particle) {
            "は" -> "주제 표시 조사 (topic marker)"
            "が" -> "주어 표시 조사 (subject marker)"
            "を" -> "목적어 표시 조사 (object marker)"
            "に" -> "방향/시간/목적 조사 (direction/time/purpose)"
            "へ" -> "방향 조사 (direction marker)"
            "と" -> "함께/인용 조사 (with/quotation)"
            "で" -> "장소/수단 조사 (location/means)"
            "から" -> "출발점/원인 조사 (from/because)"
            "まで" -> "종점/범위 조사 (until/to)"
            "の" -> "소유/설명 조사 (possessive/explanatory)"
            "も" -> "또한 조사 (also/too)"
            "や" -> "나열 조사 (listing)"
            "か" -> "의문 조사 (question marker)"
            else -> "조사"
        }
    }

    /**
     * Generate overall explanation
     */
    private fun generateOverallExplanation(
        sentence: String,
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        return when {
            sentence.endsWith("ですか") || sentence.endsWith("ますか") ->
                "의문문입니다. 정중하게 질문하는 문장이에요."

            sentence.endsWith("ください") ->
                "공손한 부탁이나 요청을 나타내는 문장입니다."

            sentence.endsWith("ます") || sentence.endsWith("です") ->
                "정중한 평서문입니다. 예의 바른 표현이에요."

            sentence.endsWith("ました") || sentence.endsWith("でした") ->
                "정중한 과거형 문장입니다."

            components.any { it.type == GrammarType.PARTICLE } ->
                "기본 문장 구조를 가진 일본어 문장입니다."

            else ->
                "일본어 문장입니다."
        }
    }

    /**
     * Generate detailed explanation
     */
    private fun generateDetailedExplanation(
        sentence: String,
        components: List<GrammarComponent>,
        userLevel: Int
    ): String {
        val particles = components.filter { it.type == GrammarType.PARTICLE }
        val verbs = components.filter { it.type == GrammarType.VERB }
        val expressions = components.filter { it.type == GrammarType.EXPRESSION }

        val parts = mutableListOf<String>()

        if (particles.isNotEmpty()) {
            parts.add("이 문장은 ${particles.size}개의 조사를 포함하고 있습니다.")
        }

        if (verbs.isNotEmpty()) {
            parts.add("동사 활용형이 사용되었습니다.")
        }

        if (expressions.isNotEmpty()) {
            parts.add("일본어 고유 표현이 포함되어 있습니다.")
        }

        if (parts.isEmpty()) {
            parts.add("기본적인 일본어 문장 구조입니다.")
        }

        parts.add("\n[오프라인 분석] API 연결 없이 로컬 패턴 매칭으로 분석한 결과입니다.")

        return parts.joinToString(" ")
    }

    /**
     * Get related grammar patterns
     */
    private fun getRelatedPatterns(components: List<GrammarComponent>): List<String> {
        val patterns = mutableListOf<String>()

        if (components.any { it.text == "は" }) {
            patterns.add("〜は〜です (주제 + 서술)")
        }

        if (components.any { it.text == "を" }) {
            patterns.add("〜を〜する (목적어 + 동사)")
        }

        if (components.any { it.text.contains("ます") }) {
            patterns.add("ます형 정중체")
        }

        if (components.any { it.text.contains("ください") }) {
            patterns.add("〜てください (공손한 요청)")
        }

        if (components.any { it.text == "が" }) {
            patterns.add("〜が〜です (주어 + 서술)")
        }

        return patterns.take(3) // Limit to top 3
    }
}
