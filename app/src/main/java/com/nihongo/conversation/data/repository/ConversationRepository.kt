package com.nihongo.conversation.data.repository

import com.nihongo.conversation.core.difficulty.DifficultyManager
import com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer
import com.nihongo.conversation.core.util.Result
import com.nihongo.conversation.data.local.*
import com.nihongo.conversation.data.remote.GeminiApiService
import com.nihongo.conversation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val userDao: UserDao,
    private val scenarioDao: ScenarioDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val geminiApi: GeminiApiService,
    private val difficultyManager: DifficultyManager
) {
    // User operations
    fun getUser(userId: Long): Flow<User?> = userDao.getUserById(userId)
    suspend fun createUser(user: User): Long = userDao.insertUser(user)

    // Scenario operations
    fun getAllScenarios(): Flow<List<Scenario>> = scenarioDao.getAllScenarios()
    fun getScenario(id: Long): Flow<Scenario?> = scenarioDao.getScenarioById(id)
    fun getScenarioBySlug(slug: String): Flow<Scenario?> = scenarioDao.getScenarioBySlug(slug)
    suspend fun createScenario(scenario: Scenario): Long = scenarioDao.insertScenario(scenario)
    suspend fun updateScenario(scenario: Scenario) = scenarioDao.updateScenario(scenario)
    suspend fun deleteScenario(scenarioId: Long) = scenarioDao.deleteScenarioById(scenarioId)

    // Conversation operations
    fun getConversation(id: Long): Flow<Conversation?> = conversationDao.getConversationById(id)
    fun getUserConversations(userId: Long): Flow<List<Conversation>> =
        conversationDao.getConversationsByUser(userId)

    fun getCompletedConversations(userId: Long): Flow<List<Conversation>> =
        conversationDao.getCompletedConversationsByUser(userId)

    suspend fun createConversation(conversation: Conversation): Long =
        conversationDao.insertConversation(conversation)

    /**
     * Get existing active conversation ID for user and scenario without creating a new one.
     * Returns null if no active conversation exists.
     */
    suspend fun getExistingConversation(userId: Long, scenarioId: Long): Long? {
        return conversationDao.getLatestActiveConversationByUserAndScenario(userId, scenarioId)?.id
    }

    /**
     * Get existing active conversation or create a new one for the user and scenario.
     * This allows resuming conversations when navigating back to the chat screen.
     */
    suspend fun getOrCreateConversation(userId: Long, scenarioId: Long): Long {
        // Try to get the most recent active conversation for this user + scenario
        val existingConversation = conversationDao.getLatestActiveConversationByUserAndScenario(userId, scenarioId)

        return if (existingConversation != null) {
            // Resume existing conversation - update timestamp
            val updated = existingConversation.copy(updatedAt = System.currentTimeMillis())
            conversationDao.updateConversation(updated)
            existingConversation.id
        } else {
            // Create new conversation
            val newConversation = Conversation(
                userId = userId,
                scenarioId = scenarioId,
                isCompleted = false
            )
            conversationDao.insertConversation(newConversation)
        }
    }

    /**
     * Mark the current conversation as completed.
     * This saves it to history and allows starting a new conversation.
     */
    suspend fun completeConversation(conversationId: Long) {
        val conversation = conversationDao.getConversationById(conversationId).first()
        conversation?.let {
            val completed = it.copy(
                isCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.updateConversation(completed)
        }
    }

    /**
     * Delete a conversation and all its messages.
     */
    suspend fun deleteConversation(conversationId: Long) {
        // Delete all messages first
        messageDao.deleteMessagesByConversation(conversationId)
        // Delete the conversation
        conversationDao.deleteConversationById(conversationId)
    }

    /**
     * Get completed conversations for a user and scenario (chat history).
     */
    fun getCompletedConversations(userId: Long, scenarioId: Long): Flow<List<Conversation>> =
        conversationDao.getCompletedConversationsByUserAndScenario(userId, scenarioId)

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

            // Analyze vocabulary complexity
            val complexity = difficultyManager.analyzeVocabularyComplexity(aiResponse)
            val complexityScore = difficultyManager.getComplexityScore(complexity)

            // Save AI message with complexity score
            val aiMsg = Message(
                conversationId = conversationId,
                content = aiResponse,
                isUser = false,
                complexityScore = complexityScore
            )
            messageDao.insertMessage(aiMsg)

            emit(Result.Success(aiMsg))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    /**
     * Send message with streaming response for instant feel
     * Emits partial messages as they arrive from Gemini API
     */
    suspend fun sendMessageStream(
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

            // Stream AI response
            val fullResponse = StringBuilder()
            var messageId: Long = 0

            geminiApi.sendMessageStream(
                message = userMessage,
                conversationHistory = history,
                systemPrompt = systemPrompt
            ).collect { chunk ->
                fullResponse.append(chunk)

                // Create partial message with current content (without furigana for TTS/analysis)
                val partialMsg = Message(
                    id = messageId,
                    conversationId = conversationId,
                    content = fullResponse.toString(),
                    isUser = false,
                    complexityScore = 0 // Will be calculated at end
                )

                // Save to DB on first chunk
                if (messageId == 0L) {
                    messageId = messageDao.insertMessage(partialMsg)
                } else {
                    // Update existing message
                    messageDao.updateMessage(partialMsg.copy(id = messageId))
                }

                // Emit partial result for UI
                emit(Result.Success(partialMsg.copy(id = messageId)))
            }

            // Calculate final complexity score
            val finalResponse = fullResponse.toString()
            val complexity = difficultyManager.analyzeVocabularyComplexity(finalResponse)
            val complexityScore = difficultyManager.getComplexityScore(complexity)

            // Update message with final complexity score
            val finalMsg = Message(
                id = messageId,
                conversationId = conversationId,
                content = finalResponse,
                isUser = false,
                complexityScore = complexityScore
            )
            messageDao.updateMessage(finalMsg)

        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    suspend fun getHints(
        conversationHistory: List<Message>,
        userLevel: Int,
        scenarioSystemPrompt: String = ""
    ): List<Hint> {
        val context = conversationHistory.takeLast(5).joinToString("\n") { message ->
            if (message.isUser) "User: ${message.content}"
            else "AI: ${message.content}"
        }

        // Extract AI's last message for context
        val aiLastMessage = conversationHistory
            .lastOrNull { !it.isUser }?.content ?: ""

        return geminiApi.generateHints(
            conversationContext = context,
            userLevel = userLevel,
            scenarioPrompt = scenarioSystemPrompt,
            aiLastMessage = aiLastMessage
        )
    }

    suspend fun explainGrammar(
        sentence: String,
        conversationHistory: List<Message>,
        userLevel: Int
    ): GrammarExplanation {
        // Extract relevant examples from conversation
        val examples = conversationHistory
            .filter { !it.isUser }
            .map { it.content }
            .takeLast(5)

        return geminiApi.explainGrammar(sentence, examples, userLevel)
    }

    suspend fun translateToKorean(japaneseText: String): String {
        return geminiApi.translateToKorean(japaneseText)
    }

    /**
     * Generate simple text response using Gemini API
     * Used for AI-powered prompt generation
     */
    suspend fun generateSimpleText(prompt: String): String {
        return geminiApi.generateSimpleText(prompt)
    }

    /**
     * Update an existing message
     */
    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message)
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
    }
}
