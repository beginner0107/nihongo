package com.nihongo.conversation.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nihongo.conversation.BuildConfig
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
