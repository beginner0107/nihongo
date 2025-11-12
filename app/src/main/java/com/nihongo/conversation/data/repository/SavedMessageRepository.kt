package com.nihongo.conversation.data.repository

import com.nihongo.conversation.data.local.SavedMessageDao
import com.nihongo.conversation.data.local.entity.SavedMessageEntity
import com.nihongo.conversation.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository for managing saved messages
 *
 * Phase 5: ChatScreen Enhancement
 */
interface SavedMessageRepository {
    fun getSavedMessages(userId: Long): Flow<List<SavedMessageData>>
    fun isMessageSaved(userId: Long, messageId: Long): Flow<Boolean>
    suspend fun saveMessage(
        userId: Long,
        message: Message,
        scenarioTitle: String,
        userNote: String? = null,
        tags: String? = null
    ): Result<Long>
    suspend fun unsaveMessage(userId: Long, messageId: Long): Result<Unit>
    fun getSavedMessageCount(userId: Long): Flow<Int>
}

class SavedMessageRepositoryImpl @Inject constructor(
    private val savedMessageDao: SavedMessageDao
) : SavedMessageRepository {

    override fun getSavedMessages(userId: Long): Flow<List<SavedMessageData>> {
        return savedMessageDao.getSavedMessages(userId).map { entities ->
            entities.map { entity ->
                SavedMessageData(
                    id = entity.id,
                    messageId = entity.messageId,
                    messageContent = entity.messageContent,
                    isUserMessage = entity.isUserMessage,
                    scenarioTitle = entity.scenarioTitle,
                    userNote = entity.userNote,
                    tags = entity.tags?.split(",")?.map { it.trim() } ?: emptyList(),
                    savedAt = entity.savedAt
                )
            }
        }
    }

    override fun isMessageSaved(userId: Long, messageId: Long): Flow<Boolean> {
        return savedMessageDao.isMessageSaved(userId, messageId)
    }

    override suspend fun saveMessage(
        userId: Long,
        message: Message,
        scenarioTitle: String,
        userNote: String?,
        tags: String?
    ): Result<Long> {
        return try {
            // Check if already saved
            val existing = savedMessageDao.getSavedMessage(userId, message.id)
            if (existing != null) {
                return Result.success(existing.id)
            }

            // Create saved message entity
            val savedMessage = SavedMessageEntity(
                messageId = message.id,
                userId = userId,
                messageContent = message.content,
                isUserMessage = message.isUser,
                scenarioTitle = scenarioTitle,
                userNote = userNote,
                tags = tags
            )

            val id = savedMessageDao.insertSavedMessage(savedMessage)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsaveMessage(userId: Long, messageId: Long): Result<Unit> {
        return try {
            savedMessageDao.deleteSavedMessageByMessageId(userId, messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedMessageCount(userId: Long): Flow<Int> {
        return savedMessageDao.getSavedMessageCount(userId)
    }
}

/**
 * Domain model for saved message
 */
data class SavedMessageData(
    val id: Long,
    val messageId: Long,
    val messageContent: String,
    val isUserMessage: Boolean,
    val scenarioTitle: String,
    val userNote: String?,
    val tags: List<String>,
    val savedAt: Long
)
