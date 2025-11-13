package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.VoiceRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceRecordingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: VoiceRecording): Long

    @Update
    suspend fun update(recording: VoiceRecording)

    @Delete
    suspend fun delete(recording: VoiceRecording)

    @Query("SELECT * FROM voice_recordings WHERE id = :id")
    suspend fun getById(id: Long): VoiceRecording?

    @Query("SELECT * FROM voice_recordings WHERE messageId = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: Long): VoiceRecording?

    @Query("SELECT * FROM voice_recordings WHERE conversationId = :conversationId ORDER BY recordedAt ASC")
    fun observeByConversation(conversationId: Long): Flow<List<VoiceRecording>>

    @Query("DELETE FROM voice_recordings WHERE recordedAt < :olderThan AND isBookmarked = 0")
    suspend fun deleteOlderThan(olderThan: Long): Int

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM voice_recordings")
    suspend fun getTotalSizeBytes(): Long
    
    @Query("SELECT * FROM voice_recordings WHERE isBookmarked = 1")
    suspend fun getBookmarkedRecordings(): List<VoiceRecording>
}

