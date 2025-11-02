package com.nihongo.conversation.core.cache

import android.content.Context
import android.util.Log
import coil.imageLoader
import com.nihongo.conversation.data.local.dao.TranslationCacheDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache Manager for manual cache operations
 * - Get cache size
 * - Clear specific caches
 * - Clear all caches
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val translationCacheDao: TranslationCacheDao
) {

    companion object {
        private const val TAG = "CacheManager"
    }

    /**
     * Get cache size information
     */
    suspend fun getCacheSize(): CacheSize = withContext(Dispatchers.IO) {
        try {
            val imageLoader = context.imageLoader

            val coilMemorySize = imageLoader.memoryCache?.size?.toLong() ?: 0L
            val coilDiskSize = imageLoader.diskCache?.size ?: 0L
            val translationCacheCount = translationCacheDao.getCacheCount()
            val appCacheSize = calculateDirectorySize(context.cacheDir)

            CacheSize(
                coilMemory = coilMemorySize,
                coilDisk = coilDiskSize,
                translationEntries = translationCacheCount,
                appCache = appCacheSize,
                total = coilMemorySize + coilDiskSize + appCacheSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache size: ${e.message}", e)
            CacheSize()
        }
    }

    /**
     * Clear all caches
     */
    suspend fun clearAllCaches(): Boolean = withContext(Dispatchers.IO) {
        try {
            clearImageCache()
            clearTranslationCache()
            clearAppCache()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all caches: ${e.message}", e)
            false
        }
    }

    /**
     * Clear image cache (Coil)
     */
    suspend fun clearImageCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageLoader = context.imageLoader
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
            Log.d(TAG, "Image cache cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear image cache: ${e.message}", e)
            false
        }
    }

    /**
     * Clear translation cache
     */
    suspend fun clearTranslationCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            translationCacheDao.clearAllCache()
            Log.d(TAG, "Translation cache cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear translation cache: ${e.message}", e)
            false
        }
    }

    /**
     * Clear app cache directory
     */
    suspend fun clearAppCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.deleteRecursively()
            Log.d(TAG, "App cache cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear app cache: ${e.message}", e)
            false
        }
    }

    /**
     * Calculate total size of a directory
     */
    private fun calculateDirectorySize(directory: java.io.File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}

/**
 * Cache size information
 */
data class CacheSize(
    val coilMemory: Long = 0L,
    val coilDisk: Long = 0L,
    val translationEntries: Int = 0,
    val appCache: Long = 0L,
    val total: Long = 0L
) {
    /**
     * Format size in human-readable format
     */
    fun formatTotal(): String {
        return formatBytes(total)
    }

    fun formatCoil(): String {
        return formatBytes(coilMemory + coilDisk)
    }

    fun formatAppCache(): String {
        return formatBytes(appCache)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024} MB"
            else -> "${bytes / 1024 / 1024 / 1024} GB"
        }
    }
}
