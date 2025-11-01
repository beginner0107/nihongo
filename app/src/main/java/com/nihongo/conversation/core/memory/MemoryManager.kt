package com.nihongo.conversation.core.memory

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.nihongo.conversation.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory management utility for monitoring and optimizing memory usage
 *
 * Phase 6A improvements:
 * - Reactive memoryConfigFlow that adapts to memory pressure
 * - Better device classification (isLowRamDevice, memoryClass, available ratio)
 * - Debug-only force GC
 */
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _memoryLevel = MutableStateFlow(MemoryLevel.NORMAL)
    val memoryLevel: StateFlow<MemoryLevel> = _memoryLevel.asStateFlow()

    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // Phase 6A: Reactive memory configuration
    private val _baseMemoryConfig = MutableStateFlow(calculateBaseMemoryConfig())
    private val _memoryConfigFlow = MutableStateFlow(calculateBaseMemoryConfig())

    /**
     * Reactive memory configuration that adjusts based on memory pressure
     */
    val memoryConfigFlow: StateFlow<MemoryConfig> = _memoryConfigFlow.asStateFlow()

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
     * @deprecated Use memoryConfigFlow for reactive updates
     */
    @Deprecated("Use memoryConfigFlow for reactive updates")
    fun getMemoryConfig(): MemoryConfig = memoryConfigFlow.value

    /**
     * Phase 6A: Calculate base memory config with improved device classification
     */
    private fun calculateBaseMemoryConfig(): MemoryConfig {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val availableRatio = availableMemoryMB.toFloat() / totalMemoryMB

        // Phase 6A: Check if device is low-RAM device
        val isLowRamDevice = activityManager.isLowRamDevice

        // Phase 6A: Get memory class (heap size available to app)
        val memoryClass = activityManager.memoryClass  // MB
        val largeMemoryClass = activityManager.largeMemoryClass  // MB

        Log.d(TAG, "Device Memory - Total: ${totalMemoryMB}MB, Available: ${availableMemoryMB}MB (${(availableRatio * 100).toInt()}%), " +
                "MemoryClass: ${memoryClass}MB, LargeMemoryClass: ${largeMemoryClass}MB, LowRAM: $isLowRamDevice")

        return when {
            // Phase 6A: Critical available memory (< 10% available)
            availableRatio < 0.1f -> {
                Log.w(TAG, "Critical available memory: ${(availableRatio * 100).toInt()}% - using minimal config")
                MemoryConfig(
                    maxMessageHistory = 30,
                    maxCacheSize = 10,
                    maxImageCacheSize = 2 * 1024 * 1024,  // 2MB
                    enableAggressiveCaching = false
                )
            }
            // Phase 6A: Low-RAM device (system flag)
            isLowRamDevice -> {
                Log.i(TAG, "Low-RAM device detected - using conservative config")
                MemoryConfig(
                    maxMessageHistory = 50,
                    maxCacheSize = 20,
                    maxImageCacheSize = 5 * 1024 * 1024,  // 5MB
                    enableAggressiveCaching = false
                )
            }
            // Low-end device (< 2GB RAM or small memoryClass)
            totalMemoryMB < 2048 || memoryClass < 128 -> {
                Log.i(TAG, "Low-end device - using basic config")
                MemoryConfig(
                    maxMessageHistory = 50,
                    maxCacheSize = 20,
                    maxImageCacheSize = 5 * 1024 * 1024,  // 5MB
                    enableAggressiveCaching = false
                )
            }
            // Mid-range device (2-4GB RAM)
            totalMemoryMB < 4096 -> {
                Log.i(TAG, "Mid-range device - using standard config")
                MemoryConfig(
                    maxMessageHistory = 100,
                    maxCacheSize = 50,
                    maxImageCacheSize = 10 * 1024 * 1024, // 10MB
                    enableAggressiveCaching = true
                )
            }
            // High-end device (4GB+ RAM)
            else -> {
                Log.i(TAG, "High-end device - using full config")
                MemoryConfig(
                    maxMessageHistory = 200,
                    maxCacheSize = 100,
                    maxImageCacheSize = 20 * 1024 * 1024, // 20MB
                    enableAggressiveCaching = true
                )
            }
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
        val newLevel = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryLevel.CRITICAL

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> MemoryLevel.LOW

            else -> MemoryLevel.NORMAL
        }

        if (newLevel != _memoryLevel.value) {
            Log.w(TAG, "Memory level changed: ${_memoryLevel.value} → $newLevel")
            _memoryLevel.value = newLevel

            // Phase 6A: Update memory config based on new level
            updateMemoryConfig(newLevel)

            // Phase 6A: Only force GC on CRITICAL and in debug builds
            if (newLevel == MemoryLevel.CRITICAL && (BuildConfig.DEBUG || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE)) {
                forceGarbageCollection()
            }
        }
    }

    /**
     * Phase 6A: Update memory config based on current memory level
     */
    private fun updateMemoryConfig(level: MemoryLevel) {
        val baseConfig = _baseMemoryConfig.value
        val newConfig = when (level) {
            MemoryLevel.CRITICAL -> MemoryConfig(
                maxMessageHistory = (baseConfig.maxMessageHistory * 0.3).toInt(),  // 70% reduction
                maxCacheSize = (baseConfig.maxCacheSize * 0.3).toInt(),
                maxImageCacheSize = (baseConfig.maxImageCacheSize * 0.3).toLong(),
                enableAggressiveCaching = false
            )
            MemoryLevel.LOW -> MemoryConfig(
                maxMessageHistory = (baseConfig.maxMessageHistory * 0.5).toInt(),  // 50% reduction
                maxCacheSize = (baseConfig.maxCacheSize * 0.5).toInt(),
                maxImageCacheSize = (baseConfig.maxImageCacheSize * 0.5).toLong(),
                enableAggressiveCaching = false
            )
            MemoryLevel.NORMAL -> baseConfig
        }
        _memoryConfigFlow.value = newConfig
        Log.i(TAG, "Memory config updated: maxMessages=${newConfig.maxMessageHistory}, maxCache=${newConfig.maxCacheSize}")
    }

    /**
     * Phase 6A: Force garbage collection (debug builds or CRITICAL only)
     */
    private fun forceGarbageCollection() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Force GC requested (memory level: ${_memoryLevel.value})")
        }
        val beforeMem = getMemoryUsage()
        System.gc()
        System.runFinalization()
        val afterMem = getMemoryUsage()
        Log.i(TAG, "Force GC completed - Freed: ${beforeMem.usedMemoryMB - afterMem.usedMemoryMB}MB")
    }

    /**
     * Phase 6A: Simulate memory pressure for testing (debug builds only)
     */
    @VisibleForTesting
    fun simulateMemoryPressure(level: MemoryLevel) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "SIMULATED memory pressure: $level")
            _memoryLevel.value = level
        } else {
            Log.e(TAG, "simulateMemoryPressure() can only be used in debug builds")
        }
    }

    data class MemoryUsage(
        val usedMemoryMB: Long,
        val maxMemoryMB: Long,
        val availableMemoryMB: Long,
        val percentageUsed: Int
    )

    companion object {
        private const val TAG = "MemoryManager"
    }
}
