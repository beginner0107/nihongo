package com.nihongo.conversation.core.util

import com.nihongo.conversation.data.local.ScenarioDao
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataInitializer - Orchestrates app initialization
 *
 * Responsibilities:
 * - Create default user if doesn't exist
 * - Seed scenarios via ScenarioSeeds
 * - Initialize response cache
 *
 * Content moved to:
 * - Scenarios → ScenarioSeeds.kt
 * - Cache patterns → CacheInitializer.kt
 */
@Singleton
class DataInitializer @Inject constructor(
    private val repository: ConversationRepository,
    private val scenarioDao: ScenarioDao,
    private val scenarioSeeds: ScenarioSeeds,
    private val cacheInitializer: com.nihongo.conversation.core.cache.CacheInitializer
) {
    companion object {
        private const val TAG = "DataInitializer"
    }

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "🚀 Starting app initialization...")

        // 1. Create default user if needed
        val existingUser = repository.getUser(1L).first()
        if (existingUser == null) {
            repository.createUser(
                User(
                    name = "학습자"
                )
            )
            android.util.Log.d(TAG, "✅ Created default user")
        }

        // 2. Seed all scenarios (upsert by slug)
        scenarioSeeds.seedAll(scenarioDao)

        // 3. Initialize response cache
        cacheInitializer.initializeCache()

        android.util.Log.d(TAG, "🎉 App initialization complete")
    }
}
