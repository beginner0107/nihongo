package com.nihongo.conversation.core.util

import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataInitializer @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend fun initializeDefaultData() {
        // Check if user already exists
        val existingUser = repository.getUser(1L).first()
        if (existingUser == null) {
            repository.createUser(
                User(
                    id = 1L,
                    name = "学習者",
                    level = 1
                )
            )
        }

        // Check if default scenario exists
        val existingScenario = repository.getScenario(1L).first()
        if (existingScenario == null) {
            repository.createScenario(
                Scenario(
                    id = 1L,
                    title = "レストランでの注文",
                    description = "レストランで注文する練習をします",
                    difficulty = 1,
                    systemPrompt = """
                        あなたは日本のレストランの店員です。
                        お客様に丁寧に接客してください。
                        簡単な日本語を使い、お客様が学習できるようにサポートしてください。
                        メニューには、ラーメン（800円）、カレーライス（700円）、寿司（1200円）があります。
                        お客様の注文を受け取り、丁寧に対応してください。
                    """.trimIndent()
                )
            )
        }
    }
}
