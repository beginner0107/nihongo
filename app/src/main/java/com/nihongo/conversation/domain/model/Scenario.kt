package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scenarios")
data class Scenario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val difficulty: Int,
    val systemPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),

    // Enhanced fields for role-play scenarios
    val category: String = "DAILY_CONVERSATION",  // ScenarioCategory enum as string
    val estimatedDuration: Int = 10,              // Minutes
    val hasGoals: Boolean = false,                // Whether this scenario has objectives
    val hasBranching: Boolean = false,            // Whether this has multiple paths
    val replayValue: Int = 1,                     // 1-5 stars for replay value
    val thumbnailEmoji: String = "💬"             // Emoji for UI display
)
