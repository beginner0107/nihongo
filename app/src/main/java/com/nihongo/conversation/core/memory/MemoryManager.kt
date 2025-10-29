package com.nihongo.conversation.core.memory

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory management utility for monitoring and optimizing memory usage
 */
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _memoryLevel = MutableStateFlow(MemoryLevel.NORMAL)
    val memoryLevel: StateFlow<MemoryLevel> = _memoryLevel.asStateFlow()

    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Memory pressure levels
     */
    enum class MemoryLevel {
        NORMAL,      // Plenty of memory available
        LOW,         // Memory getting low, start clearing caches
        CRITICAL     // Very low memory, clear all non-essential data
    }

    /**
     * Memory configuration based on device capabilities
     */
    data class MemoryConfig(
        val maxMessageHistory: Int,      // Max messages to keep in memory
        val maxCacheSize: Int,            // Max cache entries
        val maxImageCacheSize: Long,      // Image cache size in bytes
        val enableAggressiveCaching: Boolean
    )

    /**
     * Get memory configuration based on device capabilities
     */
    fun getMemoryConfig(): MemoryConfig {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)

        return when {
            // Low-end device (< 2GB RAM)
            totalMemoryMB < 2048 -> MemoryConfig(
                maxMessageHistory = 50,
                maxCacheSize = 20,
                maxImageCacheSize = 5 * 1024 * 1024,  // 5MB
                enableAggressiveCaching = false
            )
            // Mid-range device (2-4GB RAM)
            totalMemoryMB < 4096 -> MemoryConfig(
                maxMessageHistory = 100,
                maxCacheSize = 50,
                maxImageCacheSize = 10 * 1024 * 1024, // 10MB
                enableAggressiveCaching = true
            )
            // High-end device (4GB+ RAM)
            else -> MemoryConfig(
                maxMessageHistory = 200,
                maxCacheSize = 100,
                maxImageCacheSize = 20 * 1024 * 1024, // 20MB
                enableAggressiveCaching = true
            )
        }
    }

    /**
     * Check if device is currently low on memory
     */
    fun isLowMemory(): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }

    /**
     * Get current memory usage information
     */
    fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return MemoryUsage(
            usedMemoryMB = usedMemory / (1024 * 1024),
            maxMemoryMB = maxMemory / (1024 * 1024),
            availableMemoryMB = memoryInfo.availMem / (1024 * 1024),
            percentageUsed = (usedMemory.toFloat() / maxMemory * 100).toInt()
        )
    }

    /**
     * Handle memory trim callback from system
     */
    fun onTrimMemory(level: Int) {
        _memoryLevel.value = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryLevel.CRITICAL

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> MemoryLevel.LOW

            else -> MemoryLevel.NORMAL
        }
    }

    /**
     * Force garbage collection (use sparingly)
     */
    fun forceGarbageCollection() {
        System.gc()
        System.runFinalization()
    }

    data class MemoryUsage(
        val usedMemoryMB: Long,
        val maxMemoryMB: Long,
        val availableMemoryMB: Long,
        val percentageUsed: Int
    )
}
