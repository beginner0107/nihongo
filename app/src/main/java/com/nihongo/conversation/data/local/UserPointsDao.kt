package com.nihongo.conversation.data.local

import androidx.room.*
import com.nihongo.conversation.data.local.entity.UserPointsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPointsDao {
    /**
     * Get user points (with reactive updates)
     */
    @Query("SELECT * FROM user_points WHERE userId = :userId")
    fun getUserPoints(userId: Long): Flow<UserPointsEntity?>

    /**
     * Get user points (one-time)
     */
    @Query("SELECT * FROM user_points WHERE userId = :userId")
    suspend fun getUserPointsOnce(userId: Long): UserPointsEntity?

    /**
     * Insert or update user points
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPoints(userPoints: UserPointsEntity)

    /**
     * Add points to user's total, today, and weekly counts
     */
    @Query("""
        UPDATE user_points
        SET totalPoints = totalPoints + :points,
            todayPoints = todayPoints + :points,
            weeklyPoints = weeklyPoints + :points
        WHERE userId = :userId
    """)
    suspend fun addPoints(userId: Long, points: Int)

    /**
     * Update user level
     */
    @Query("UPDATE user_points SET level = :level WHERE userId = :userId")
    suspend fun updateLevel(userId: Long, level: Int)

    /**
     * Reset daily points (called at midnight)
     */
    @Query("""
        UPDATE user_points
        SET todayPoints = 0, lastResetDate = :resetDate
        WHERE userId = :userId
    """)
    suspend fun resetDailyPoints(userId: Long, resetDate: Long)

    /**
     * Reset weekly points (called on Monday)
     */
    @Query("UPDATE user_points SET weeklyPoints = 0 WHERE userId = :userId")
    suspend fun resetWeeklyPoints(userId: Long)

    /**
     * Update weekly rank
     */
    @Query("UPDATE user_points SET weeklyRank = :rank WHERE userId = :userId")
    suspend fun updateWeeklyRank(userId: Long, rank: Int)

    /**
     * Get all users ordered by weekly points (for leaderboard)
     */
    @Query("SELECT * FROM user_points ORDER BY weeklyPoints DESC LIMIT :limit")
    suspend fun getTopUsersByWeeklyPoints(limit: Int = 10): List<UserPointsEntity>

    /**
     * Get all users ordered by total points
     */
    @Query("SELECT * FROM user_points ORDER BY totalPoints DESC LIMIT :limit")
    suspend fun getTopUsersByTotalPoints(limit: Int = 10): List<UserPointsEntity>

    /**
     * Delete user points (for testing/cleanup)
     */
    @Query("DELETE FROM user_points WHERE userId = :userId")
    suspend fun deleteUserPoints(userId: Long)
}
