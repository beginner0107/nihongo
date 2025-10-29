package com.nihongo.conversation.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.domain.model.Hint
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiService @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-pro",
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
            response.text ?: "エラーが発生しました"
        } catch (e: Exception) {
            throw Exception("Failed to get response from Gemini: ${e.message}")
        }
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
