package com.nihongo.conversation.data.repository

import com.nihongo.conversation.core.util.Result
import com.nihongo.conversation.data.local.*
import com.nihongo.conversation.data.remote.GeminiApiService
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val userDao: UserDao,
    private val scenarioDao: ScenarioDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val geminiApi: GeminiApiService
) {
    // User operations
    fun getUser(userId: Long): Flow<User?> = userDao.getUserById(userId)
    suspend fun createUser(user: User): Long = userDao.insertUser(user)

    // Scenario operations
    fun getAllScenarios(): Flow<List<Scenario>> = scenarioDao.getAllScenarios()
    fun getScenario(id: Long): Flow<Scenario?> = scenarioDao.getScenarioById(id)
    suspend fun createScenario(scenario: Scenario): Long = scenarioDao.insertScenario(scenario)

    // Conversation operations
    fun getConversation(id: Long): Flow<Conversation?> = conversationDao.getConversationById(id)
    fun getUserConversations(userId: Long): Flow<List<Conversation>> =
        conversationDao.getConversationsByUser(userId)

    suspend fun createConversation(conversation: Conversation): Long =
        conversationDao.insertConversation(conversation)

    // Message operations
    fun getMessages(conversationId: Long): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun sendMessage(
        conversationId: Long,
        userMessage: String,
        conversationHistory: List<Message>,
        systemPrompt: String
    ): Flow<Result<Message>> = flow {
        emit(Result.Loading)

        try {
            // Save user message
            val userMsg = Message(
                conversationId = conversationId,
                content = userMessage,
                isUser = true
            )
            messageDao.insertMessage(userMsg)

            // Prepare history for API
            val history = conversationHistory.map { it.content to it.isUser }

            // Get AI response
            val aiResponse = geminiApi.sendMessage(
                message = userMessage,
                conversationHistory = history,
                systemPrompt = systemPrompt
            )

            // Save AI message
            val aiMsg = Message(
                conversationId = conversationId,
                content = aiResponse,
                isUser = false
            )
            messageDao.insertMessage(aiMsg)

            emit(Result.Success(aiMsg))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    suspend fun getHints(
        conversationHistory: List<Message>,
        userLevel: Int
    ): List<Hint> {
        val context = conversationHistory.takeLast(5).joinToString("\n") { message ->
            if (message.isUser) "사용자: ${message.content}"
            else "AI: ${message.content}"
        }

        return geminiApi.generateHints(context, userLevel)
    }
}
