package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.data.local.entity.SavedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMessageDao {

    @Query("SELECT * FROM saved_messages WHERE userId = :userId ORDER BY savedAt DESC")
    fun getSavedMessages(userId: Long): Flow<List<SavedMessageEntity>>

    @Query("SELECT * FROM saved_messages WHERE userId = :userId AND messageId = :messageId LIMIT 1")
    suspend fun getSavedMessage(userId: Long, messageId: Long): SavedMessageEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_messages WHERE userId = :userId AND messageId = :messageId)")
    fun isMessageSaved(userId: Long, messageId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMessage(savedMessage: SavedMessageEntity): Long

    @Update
    suspend fun updateSavedMessage(savedMessage: SavedMessageEntity)

    @Query("DELETE FROM saved_messages WHERE userId = :userId AND messageId = :messageId")
    suspend fun deleteSavedMessageByMessageId(userId: Long, messageId: Long)

    @Query("SELECT COUNT(*) FROM saved_messages WHERE userId = :userId")
    fun getSavedMessageCount(userId: Long): Flow<Int>
}
