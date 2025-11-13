package com.nihongo.conversation.core.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2: 음성 파일 관리 시스템
 * 
 * 기능:
 * - 저장 공간 모니터링
 * - 오래된 파일 자동 삭제
 * - 수동 파일 삭제
 * - 북마크된 파일 보호
 */
@Singleton
class VoiceStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: VoiceFileManager
) {
    companion object {
        const val WARNING_THRESHOLD_BYTES = 50L * 1024 * 1024 // 50MB
        const val MAX_STORAGE_BYTES = 100L * 1024 * 1024 // 100MB
        const val AUTO_DELETE_DAYS = 30L
    }
    
    /**
     * 전체 음성 파일 크기 계산
     */
    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) {
        val voiceDir = File(context.cacheDir, "voice")
        if (!voiceDir.exists()) return@withContext 0L
        
        voiceDir.walkTopDown()
            .filter { it.isFile && it.extension == "m4a" }
            .sumOf { it.length() }
    }
    
    /**
     * 저장 공간 상태 확인
     */
    suspend fun getStorageStatus(): StorageStatus = withContext(Dispatchers.IO) {
        val totalSize = getTotalSize()
        val fileCount = getVoiceFileCount()
        
        when {
            totalSize >= MAX_STORAGE_BYTES -> StorageStatus.Critical(totalSize, fileCount)
            totalSize >= WARNING_THRESHOLD_BYTES -> StorageStatus.Warning(totalSize, fileCount)
            else -> StorageStatus.Normal(totalSize, fileCount)
        }
    }
    
    /**
     * 음성 파일 개수
     */
    private fun getVoiceFileCount(): Int {
        val voiceDir = File(context.cacheDir, "voice")
        if (!voiceDir.exists()) return 0
        
        return voiceDir.walkTopDown()
            .filter { it.isFile && it.extension == "m4a" }
            .count()
    }
    
    /**
     * 오래된 파일 삭제 (북마크 제외)
     */
    suspend fun deleteOldFiles(
        olderThanDays: Long = AUTO_DELETE_DAYS,
        bookmarkedFiles: Set<String> = emptySet()
    ): CleanupResult = withContext(Dispatchers.IO) {
        val voiceDir = File(context.cacheDir, "voice")
        if (!voiceDir.exists()) return@withContext CleanupResult(0, 0L)
        
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000)
        var deletedCount = 0
        var freedBytes = 0L
        
        voiceDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                file.extension == "m4a" &&
                file.lastModified() < cutoffTime &&
                !bookmarkedFiles.contains(file.name)
            }
            .forEach { file ->
                val size = file.length()
                if (file.delete()) {
                    deletedCount++
                    freedBytes += size
                }
            }
        
        CleanupResult(deletedCount, freedBytes)
    }
    
    /**
     * 저장 공간 초과 시 정리
     */
    suspend fun cleanupExcessStorage(
        maxSizeBytes: Long = MAX_STORAGE_BYTES,
        bookmarkedFiles: Set<String> = emptySet()
    ): CleanupResult = withContext(Dispatchers.IO) {
        val totalSize = getTotalSize()
        if (totalSize <= maxSizeBytes) {
            return@withContext CleanupResult(0, 0L)
        }
        
        val targetReduction = totalSize - maxSizeBytes
        var deletedCount = 0
        var freedBytes = 0L
        
        val voiceDir = File(context.cacheDir, "voice")
        if (!voiceDir.exists()) return@withContext CleanupResult(0, 0L)
        
        // 오래된 파일부터 삭제 (북마크 제외)
        voiceDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                file.extension == "m4a" &&
                !bookmarkedFiles.contains(file.name)
            }
            .sortedBy { it.lastModified() } // 오래된 순
            .forEach { file ->
                if (freedBytes >= targetReduction) {
                    return@forEach
                }
                
                val size = file.length()
                if (file.delete()) {
                    deletedCount++
                    freedBytes += size
                }
            }
        
        CleanupResult(deletedCount, freedBytes)
    }
    
    /**
     * 특정 파일 삭제
     */
    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.exists() && file.delete()
    }
    
    /**
     * 모든 음성 파일 삭제 (북마크 제외)
     */
    suspend fun deleteAllFiles(
        bookmarkedFiles: Set<String> = emptySet()
    ): CleanupResult = withContext(Dispatchers.IO) {
        val voiceDir = File(context.cacheDir, "voice")
        if (!voiceDir.exists()) return@withContext CleanupResult(0, 0L)
        
        var deletedCount = 0
        var freedBytes = 0L
        
        voiceDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                file.extension == "m4a" &&
                !bookmarkedFiles.contains(file.name)
            }
            .forEach { file ->
                val size = file.length()
                if (file.delete()) {
                    deletedCount++
                    freedBytes += size
                }
            }
        
        CleanupResult(deletedCount, freedBytes)
    }
    
    /**
     * 저장 공간 정보 포맷팅
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

sealed class StorageStatus {
    abstract val totalSize: Long
    abstract val fileCount: Int
    
    data class Normal(
        override val totalSize: Long,
        override val fileCount: Int
    ) : StorageStatus()
    
    data class Warning(
        override val totalSize: Long,
        override val fileCount: Int
    ) : StorageStatus()
    
    data class Critical(
        override val totalSize: Long,
        override val fileCount: Int
    ) : StorageStatus()
}

data class CleanupResult(
    val deletedCount: Int,
    val freedBytes: Long
)
