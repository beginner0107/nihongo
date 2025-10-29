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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: Scenario): Long

    @Update
    suspend fun updateScenario(scenario: Scenario)

    @Delete
    suspend fun deleteScenario(scenario: Scenario)
}
