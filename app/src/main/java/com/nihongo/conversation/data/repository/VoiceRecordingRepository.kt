package com.nihongo.conversation.data.repository

import com.nihongo.conversation.core.voice.VoiceFileManager
import com.nihongo.conversation.data.local.VoiceRecordingDao
import com.nihongo.conversation.domain.model.VoiceRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface VoiceRecordingRepository {
    suspend fun savePending(
        conversationId: Long,
        tempFilePath: String,
        durationMs: Long,
        fileSizeBytes: Long,
        recordedAt: Long,
        language: String
    ): Long

    suspend fun linkToMessage(recordingId: Long, messageId: Long): VoiceRecording?

    suspend fun getById(id: Long): VoiceRecording?

    fun observeByConversation(conversationId: Long): Flow<List<VoiceRecording>>

    suspend fun deleteOld(olderThan: Long): Int

    suspend fun totalSize(): Long

    suspend fun cleanup(maxSizeBytes: Long): Int
    
    // Phase 3: Bookmark functionality
    suspend fun toggleBookmark(recordingId: Long): VoiceRecording?
    
    suspend fun getBookmarkedFiles(): Set<String>
    
    suspend fun deleteRecording(recordingId: Long): Boolean
}

@Singleton
class VoiceRecordingRepositoryImpl @Inject constructor(
    private val dao: VoiceRecordingDao,
    private val fileManager: VoiceFileManager
) : VoiceRecordingRepository {
    override suspend fun savePending(
        conversationId: Long,
        tempFilePath: String,
        durationMs: Long,
        fileSizeBytes: Long,
        recordedAt: Long,
        language: String
    ): Long {
        val file = java.io.File(tempFilePath)
        val rec = VoiceRecording(
            conversationId = conversationId,
            messageId = null,
            filePath = file.absolutePath,
            fileName = file.name,
            durationMs = durationMs,
            fileSizeBytes = fileSizeBytes,
            recordedAt = recordedAt,
            language = language
        )
        return dao.insert(rec)
    }

    override suspend fun linkToMessage(recordingId: Long, messageId: Long): VoiceRecording? {
        val existing = dao.getById(recordingId) ?: return null
        val timestamp = existing.recordedAt
        val finalFile = fileManager.renameToFinal(
            java.io.File(existing.filePath),
            existing.conversationId,
            messageId,
            timestamp
        )
        val updated = existing.copy(
            messageId = messageId,
            filePath = finalFile.absolutePath,
            fileName = finalFile.name
        )
        dao.update(updated)
        return updated
    }

    override suspend fun getById(id: Long): VoiceRecording? = dao.getById(id)

    override fun observeByConversation(conversationId: Long): Flow<List<VoiceRecording>> =
        dao.observeByConversation(conversationId)

    override suspend fun deleteOld(olderThan: Long): Int = dao.deleteOlderThan(olderThan)

    override suspend fun totalSize(): Long = dao.getTotalSizeBytes()

    override suspend fun cleanup(maxSizeBytes: Long): Int {
        // Best-effort: Always run file-level cleanup as well
        return fileManager.cleanupByPolicy(maxTotalBytes = maxSizeBytes)
    }
    
    override suspend fun toggleBookmark(recordingId: Long): VoiceRecording? {
        val recording = dao.getById(recordingId) ?: return null
        val updated = recording.copy(isBookmarked = !recording.isBookmarked)
        dao.update(updated)
        return updated
    }
    
    override suspend fun getBookmarkedFiles(): Set<String> {
        return dao.getBookmarkedRecordings()
            .map { it.fileName }
            .toSet()
    }
    
    override suspend fun deleteRecording(recordingId: Long): Boolean {
        val recording = dao.getById(recordingId) ?: return false
        val file = java.io.File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
        dao.delete(recording)
        return true
    }
}

