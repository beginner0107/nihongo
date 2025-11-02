package com.nihongo.conversation.core.cache

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import com.nihongo.conversation.data.local.dao.TranslationCacheDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Background worker for cleaning up various caches
 * - Coil image cache
 * - Translation cache (entries older than 30 days)
 * - Temporary files
 */
@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val translationCacheDao: TranslationCacheDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "CacheCleanupWorker"
        const val WORK_NAME = "cache_cleanup_work"

        // Translation cache retention: 30 days
        const val TRANSLATION_CACHE_RETENTION_DAYS = 30
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cache cleanup...")

            var totalCleaned = 0L

            // 1. Clean Coil image cache
            val coilCleanedBytes = cleanCoilCache()
            totalCleaned += coilCleanedBytes
            Log.d(TAG, "Coil cache cleaned: ${coilCleanedBytes / 1024} KB")

            // 2. Clean old translation cache entries
            val translationEntriesDeleted = cleanTranslationCache()
            Log.d(TAG, "Translation cache entries deleted: $translationEntriesDeleted")

            // 3. Clean temp files
            val tempFilesCleanedBytes = cleanTempFiles()
            totalCleaned += tempFilesCleanedBytes
            Log.d(TAG, "Temp files cleaned: ${tempFilesCleanedBytes / 1024} KB")

            // 4. Clean app cache directory (old files)
            val appCacheCleanedBytes = cleanAppCache()
            totalCleaned += appCacheCleanedBytes
            Log.d(TAG, "App cache cleaned: ${appCacheCleanedBytes / 1024} KB")

            Log.d(TAG, "Cache cleanup completed. Total cleaned: ${totalCleaned / 1024 / 1024} MB")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cache cleanup failed: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Clean Coil image cache
     */
    private fun cleanCoilCache(): Long {
        return try {
            val imageLoader = context.imageLoader
            val diskCache = imageLoader.diskCache
            val memoryCacheSize = imageLoader.memoryCache?.size?.toLong() ?: 0L

            // Clear memory cache
            imageLoader.memoryCache?.clear()

            // Clear disk cache (optional - can be selective)
            diskCache?.clear()

            val diskCacheSize = diskCache?.size ?: 0L
            memoryCacheSize + diskCacheSize
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean Coil cache: ${e.message}", e)
            0L
        }
    }

    /**
     * Clean translation cache entries older than retention period
     */
    private suspend fun cleanTranslationCache(): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (TRANSLATION_CACHE_RETENTION_DAYS * MILLIS_PER_DAY)
            translationCacheDao.cleanOldCache(cutoffTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean translation cache: ${e.message}", e)
            0
        }
    }

    /**
     * Clean temporary files
     */
    private fun cleanTempFiles(): Long {
        return try {
            val tempDir = context.cacheDir
            var totalSize = 0L

            tempDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("tmp_")) {
                    totalSize += file.length()
                    file.delete()
                }
            }

            totalSize
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean temp files: ${e.message}", e)
            0L
        }
    }

    /**
     * Clean app cache directory (files older than 7 days)
     */
    private fun cleanAppCache(): Long {
        return try {
            val cacheDir = context.cacheDir
            val cutoffTime = System.currentTimeMillis() - (7 * MILLIS_PER_DAY)
            var totalSize = 0L

            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    totalSize += file.length()
                    file.delete()
                }
            }

            totalSize
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean app cache: ${e.message}", e)
            0L
        }
    }
}
