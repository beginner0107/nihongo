package com.nihongo.conversation.core.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun voiceDir(): File {
        val dir = File(context.cacheDir, "voice")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempFile(conversationId: Long, timestamp: Long): File {
        val dir = voiceDir()
        val name = "voice_${conversationId}_temp_${timestamp}.m4a"
        return File(dir, name)
    }

    fun buildFinalFile(conversationId: Long, messageId: Long, timestamp: Long): File {
        val dir = voiceDir()
        val name = "voice_${conversationId}_${messageId}_${timestamp}.m4a"
        return File(dir, name)
    }

    fun renameToFinal(tempFile: File, conversationId: Long, messageId: Long, timestamp: Long): File {
        val target = buildFinalFile(conversationId, messageId, timestamp)
        if (tempFile.exists()) {
            tempFile.renameTo(target)
        }
        return target
    }

    fun deleteFile(path: String): Boolean = try {
        File(path).delete()
    } catch (_: Exception) { false }

    fun totalSizeBytes(): Long {
        return voiceDir().listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    fun cleanupByPolicy(maxTotalBytes: Long = 100L * 1024 * 1024, olderThanMs: Long = 30L * 24 * 60 * 60 * 1000): Int {
        val dir = voiceDir()
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: emptyList()
        var removed = 0

        // Remove files older than threshold first
        val now = System.currentTimeMillis()
        files.forEach { f ->
            if (now - f.lastModified() > olderThanMs) {
                if (f.delete()) removed++
            }
        }

        // Enforce max size (delete oldest first)
        var currentSize = totalSizeBytes()
        if (currentSize > maxTotalBytes) {
            val remaining = dir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            for (f in remaining) {
                if (currentSize <= maxTotalBytes) break
                if (f.delete()) {
                    removed++
                    currentSize -= f.length()
                }
            }
        }
        return removed
    }
}

