package com.nihongo.conversation.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihongo.conversation.domain.model.ScenarioBranch
import com.nihongo.conversation.domain.model.ScenarioGoal
import com.nihongo.conversation.domain.model.ScenarioOutcome
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<ScenarioGoal>): List<Long>

    @Query("SELECT * FROM scenario_goals WHERE scenarioId = :scenarioId ORDER BY `order` ASC")
    fun getGoalsForScenario(scenarioId: Long): Flow<List<ScenarioGoal>>

    @Query("SELECT * FROM scenario_goals WHERE scenarioId = :scenarioId AND isRequired = 1")
    suspend fun getRequiredGoals(scenarioId: Long): List<ScenarioGoal>

    @Query("SELECT * FROM scenario_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): ScenarioGoal?

    @Query("DELETE FROM scenario_goals WHERE scenarioId = :scenarioId")
    suspend fun deleteGoalsForScenario(scenarioId: Long)
}

@Dao
interface ScenarioOutcomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutcomes(outcomes: List<ScenarioOutcome>): List<Long>

    @Query("SELECT * FROM scenario_outcomes WHERE scenarioId = :scenarioId ORDER BY minScore DESC")
    suspend fun getOutcomesForScenario(scenarioId: Long): List<ScenarioOutcome>

    @Query("SELECT * FROM scenario_outcomes WHERE id = :outcomeId")
    suspend fun getOutcomeById(outcomeId: Long): ScenarioOutcome?

    @Query("""
        SELECT * FROM scenario_outcomes
        WHERE scenarioId = :scenarioId
        AND minScore <= :score
        AND maxScore >= :score
        ORDER BY minScore DESC
        LIMIT 1
    """)
    suspend fun getOutcomeByScore(scenarioId: Long, score: Int): ScenarioOutcome?

    @Query("DELETE FROM scenario_outcomes WHERE scenarioId = :scenarioId")
    suspend fun deleteOutcomesForScenario(scenarioId: Long)
}

@Dao
interface ScenarioBranchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<ScenarioBranch>): List<Long>

    @Query("SELECT * FROM scenario_branches WHERE scenarioId = :scenarioId ORDER BY triggerPoint ASC")
    suspend fun getBranchesForScenario(scenarioId: Long): List<ScenarioBranch>

    @Query("""
        SELECT * FROM scenario_branches
        WHERE scenarioId = :scenarioId
        AND triggerPoint = :messageCount
        LIMIT 1
    """)
    suspend fun getBranchAtPoint(scenarioId: Long, messageCount: Int): ScenarioBranch?

    @Query("DELETE FROM scenario_branches WHERE scenarioId = :scenarioId")
    suspend fun deleteBranchesForScenario(scenarioId: Long)
}
