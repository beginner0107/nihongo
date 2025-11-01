package com.nihongo.conversation

import android.app.Application
import android.util.Log
import com.nihongo.conversation.core.memory.MemoryManager
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.core.util.DataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NihongoApp : Application() {

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
