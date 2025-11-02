package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.domain.model.Scenario
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios")
    fun getAllScenarios(): Flow<List<Scenario>>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    fun getScenarioById(id: Long): Flow<Scenario?>

    @Query("SELECT * FROM scenarios WHERE slug = :slug LIMIT 1")
    fun getScenarioBySlug(slug: String): Flow<Scenario?>

    @Query("SELECT * FROM scenarios WHERE slug = :slug LIMIT 1")
    suspend fun getScenarioBySlugSync(slug: String): Scenario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: Scenario): Long

    @Update
    suspend fun updateScenario(scenario: Scenario)

    @Delete
    suspend fun deleteScenario(scenario: Scenario)

    @Query("DELETE FROM scenarios WHERE id = :scenarioId")
    suspend fun deleteScenarioById(scenarioId: Long)

    /**
     * Upsert scenario by slug with version check
     * - If scenario doesn't exist: insert
     * - If exists and promptVersion increased: update
     * - If exists and promptVersion same: skip
     */
    @androidx.room.Transaction
    suspend fun upsertBySlug(scenario: Scenario) {
        val existing = getScenarioBySlugSync(scenario.slug)
        when {
            existing == null -> {
                insertScenario(scenario)
                android.util.Log.d("ScenarioDao", "✨ Inserted new scenario: ${scenario.slug} (v${scenario.promptVersion})")
            }
            existing.promptVersion < scenario.promptVersion -> {
                // Update keeping original ID and createdAt
                updateScenario(scenario.copy(id = existing.id, createdAt = existing.createdAt))
                android.util.Log.d("ScenarioDao", "🔄 Updated scenario: ${scenario.slug} (v${existing.promptVersion} → v${scenario.promptVersion})")
            }
            else -> {
                android.util.Log.d("ScenarioDao", "⏭️  Skipped scenario: ${scenario.slug} (v${scenario.promptVersion} unchanged)")
            }
        }
    }
}
