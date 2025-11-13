package com.nihongo.conversation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_points")
data class UserPointsEntity(
    @PrimaryKey val userId: Long,
    val totalPoints: Int = 0,
    val todayPoints: Int = 0,
    val weeklyPoints: Int = 0,
    val level: Int = 1,  // 레벨 (100포인트 = 1레벨)
    val weeklyRank: Int? = null,
    val lastResetDate: Long = System.currentTimeMillis()
)
