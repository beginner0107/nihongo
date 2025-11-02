package com.nihongo.conversation

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.nihongo.conversation.core.cache.CacheCleanupWorker
import com.nihongo.conversation.core.memory.MemoryManager
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.core.util.DataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NihongoApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dataInitializer: DataInitializer

    // Phase 6A: Memory management
    @Inject
    lateinit var memoryManager: MemoryManager

    // Phase 6C-2: Session management with auto-login
    @Inject
    lateinit var sessionManager: UserSessionManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Log initial memory status
        val memoryUsage = memoryManager.getMemoryUsage()
        Log.d(TAG, "App started - Memory: ${memoryUsage.usedMemoryMB}MB / ${memoryUsage.maxMemoryMB}MB (${memoryUsage.percentageUsed}%)")

        applicationScope.launch {
            // Phase 6C-2: Initialize data first, then ensure session
            dataInitializer.initializeDefaultData()

            // Auto-login: Ensure user session is initialized
            val sessionCreated = sessionManager.ensureSessionInitialized()
            if (sessionCreated) {
                Log.i(TAG, "Auto-login successful")
            } else {
                Log.d(TAG, "Session already exists or no default user available")
            }
        }

        // Schedule periodic cache cleanup
        scheduleCacheCleanup()
    }

    /**
     * Schedule periodic cache cleanup (once per day)
     */
    private fun scheduleCacheCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Only run when battery is not low
            .setRequiresDeviceIdle(true)      // Only run when device is idle
            .build()

        val cacheCleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CacheCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
            cacheCleanupRequest
        )

        Log.d(TAG, "Cache cleanup scheduled (daily)")
    }

    /**
     * WorkManager configuration with Hilt support
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Coil ImageLoader configuration
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)  // Use 25% of app memory for image cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)  // 50 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false)  // Don't respect HTTP cache headers
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // Phase 6A: Forward system memory trim callbacks to MemoryManager
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        memoryManager.onTrimMemory(level)

        val levelName = when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            TRIM_MEMORY_MODERATE -> "MODERATE"
            TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        Log.w(TAG, "onTrimMemory: $levelName → MemoryLevel: ${memoryManager.memoryLevel.value}")
    }

    companion object {
        private const val TAG = "NihongoApp"
    }
}
