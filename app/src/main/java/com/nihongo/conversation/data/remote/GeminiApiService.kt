package com.nihongo.conversation.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType
import com.nihongo.conversation.domain.model.Hint
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiService @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun sendMessage(
        message: String,
        conversationHistory: List<Pair<String, Boolean>>,
        systemPrompt: String
    ): String {
        return try {
            val chat = model.startChat(
                history = buildHistory(conversationHistory, systemPrompt)
            )
            val response = chat.sendMessage(message)
            val rawText = response.text ?: "エラーが発生しました"
            cleanResponseText(rawText)
        } catch (e: Exception) {
            throw Exception("Failed to get response from Gemini: ${e.message}")
        }
    }

    /**
     * Clean AI response text by removing markdown formatting and furigana
     */
    private fun cleanResponseText(text: String): String {
        return text
            // Remove markdown bold (**text** or __text__)
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            // Remove markdown italic (*text* or _text_)
            .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
            .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
            // Remove furigana in parentheses (both full-width and half-width)
            .replace(Regex("（[^）]*）"), "")
            .replace(Regex("\\([^)]*\\)"), "")
    }

    suspend fun generateHints(
        conversationContext: String,
        userLevel: Int
    ): List<Hint> {
        return try {
            val prompt = """
                현재 일본어 회화 상황: $conversationContext
                사용자 레벨: $userLevel

                위 상황에서 사용자가 다음에 말할 수 있는 일본어 표현 3개를 제공하세요.
                각 표현에 대해 다음 JSON 형식으로 응답하세요:

                [
                  {
                    "japanese": "일본어 표현",
                    "korean": "한국어 번역",
                    "romaji": "로마자 표기",
                    "explanation": "사용 상황 설명"
                  }
                ]

                응답은 반드시 JSON 배열만 포함하고, 다른 텍스트는 포함하지 마세요.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: "[]"

            parseHintsFromJson(jsonText)
        } catch (e: Exception) {
            // Fallback hints on error
            listOf(
                Hint(
                    japanese = "すみません",
                    korean = "죄송합니다 / 실례합니다",
                    romaji = "sumimasen",
                    explanation = "사람을 부르거나 사과할 때 사용"
                ),
                Hint(
                    japanese = "お願いします",
                    korean = "부탁합니다",
                    romaji = "onegaishimasu",
                    explanation = "무언가를 요청할 때 사용"
                ),
                Hint(
                    japanese = "ありがとうございます",
                    korean = "감사합니다",
                    romaji = "arigatou gozaimasu",
                    explanation = "감사를 표현할 때 사용"
                )
            )
        }
    }

    suspend fun explainGrammar(
        sentence: String,
        conversationExamples: List<String>,
        userLevel: Int
    ): GrammarExplanation {
        return try {
            val prompt = """
                다음 일본어 문장의 문법을 한국어로 쉽게 설명해주세요.
                사용자 레벨: $userLevel (1=초급, 2=중급, 3=고급)

                문장: $sentence

                다음 JSON 형식으로 응답하세요:
                {
                  "overallExplanation": "문장 전체의 간단한 설명 (1-2문장)",
                  "detailedExplanation": "문법 구조에 대한 상세한 설명",
                  "components": [
                    {
                      "text": "문법 요소 (예: を, ます)",
                      "type": "PARTICLE|VERB|ADJECTIVE|NOUN|AUXILIARY|CONJUNCTION|ADVERB|EXPRESSION",
                      "explanation": "이 문법 요소에 대한 한국어 설명",
                      "startIndex": 시작위치,
                      "endIndex": 끝위치
                    }
                  ],
                  "examples": [
                    ${conversationExamples.joinToString(",\n") { "\"$it\"" }}
                  ],
                  "relatedPatterns": [
                    "관련 문법 패턴 1",
                    "관련 문법 패턴 2"
                  ]
                }

                주의사항:
                - components는 문장의 모든 주요 문법 요소를 포함해야 합니다
                - startIndex와 endIndex는 실제 문자열 위치를 정확히 지정하세요
                - type은 반드시 제시된 타입 중 하나여야 합니다
                - 사용자 레벨에 맞는 쉬운 설명을 제공하세요
                - examples는 대화 내용에서 관련된 문장들입니다

                응답은 반드시 JSON만 포함하고, 다른 텍스트는 포함하지 마세요.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: "{}"

            parseGrammarExplanationFromJson(jsonText, sentence)
        } catch (e: Exception) {
            // Fallback grammar explanation
            GrammarExplanation(
                originalText = sentence,
                components = emptyList(),
                overallExplanation = "문법 분석을 가져오는 중 오류가 발생했습니다.",
                detailedExplanation = "다시 시도해주세요.",
                examples = conversationExamples,
                relatedPatterns = emptyList()
            )
        }
    }

    private fun parseGrammarExplanationFromJson(
        jsonText: String,
        originalText: String
    ): GrammarExplanation {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val obj = JSONObject(cleanJson)
            val componentsArray = obj.getJSONArray("components")
            val components = mutableListOf<GrammarComponent>()

            for (i in 0 until componentsArray.length()) {
                val compObj = componentsArray.getJSONObject(i)
                components.add(
                    GrammarComponent(
                        text = compObj.getString("text"),
                        type = GrammarType.valueOf(compObj.getString("type")),
                        explanation = compObj.getString("explanation"),
                        startIndex = compObj.getInt("startIndex"),
                        endIndex = compObj.getInt("endIndex")
                    )
                )
            }

            val examplesArray = obj.optJSONArray("examples") ?: JSONArray()
            val examples = mutableListOf<String>()
            for (i in 0 until examplesArray.length()) {
                examples.add(examplesArray.getString(i))
            }

            val relatedArray = obj.optJSONArray("relatedPatterns") ?: JSONArray()
            val relatedPatterns = mutableListOf<String>()
            for (i in 0 until relatedArray.length()) {
                relatedPatterns.add(relatedArray.getString(i))
            }

            GrammarExplanation(
                originalText = originalText,
                components = components,
                overallExplanation = obj.getString("overallExplanation"),
                detailedExplanation = obj.getString("detailedExplanation"),
                examples = examples,
                relatedPatterns = relatedPatterns
            )
        } catch (e: Exception) {
            GrammarExplanation(
                originalText = originalText,
                components = emptyList(),
                overallExplanation = "문법 분석을 파싱하는 중 오류가 발생했습니다.",
                detailedExplanation = e.message ?: "알 수 없는 오류",
                examples = emptyList(),
                relatedPatterns = emptyList()
            )
        }
    }

    private fun parseHintsFromJson(jsonText: String): List<Hint> {
        return try {
            // Remove markdown code blocks if present
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val hints = mutableListOf<Hint>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                hints.add(
                    Hint(
                        japanese = obj.getString("japanese"),
                        korean = obj.getString("korean"),
                        romaji = obj.optString("romaji", null),
                        explanation = obj.optString("explanation", null)
                    )
                )
            }

            hints
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun translateToKorean(japaneseText: String): String {
        return try {
            val prompt = """
                다음 일본어 문장을 자연스러운 한국어로 번역해주세요.
                문장: $japaneseText

                주의사항:
                - 번역문만 출력하세요 (설명이나 다른 텍스트 없이)
                - 자연스러운 한국어 표현을 사용하세요
                - 존댓말로 번역하세요
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim() ?: "번역 실패"
        } catch (e: Exception) {
            "번역 오류: ${e.message}"
        }
    }

    private fun buildHistory(
        history: List<Pair<String, Boolean>>,
        systemPrompt: String
    ) = buildList {
        add(
            content("model") {
                text(systemPrompt)
            }
        )
        history.forEach { (text, isUser) ->
            add(
                content(if (isUser) "user" else "model") {
                    text(text)
                }
            )
        }
    }
}
