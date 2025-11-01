package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scenarios",
    indices = [Index(value = ["slug"], unique = true)]
)
data class Scenario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val difficulty: Int,
    val systemPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),

    // Stable identifier for updates/migrations (e.g., "restaurant_ordering")
    val slug: String = "",

    // Prompt content version for safe updates
    val promptVersion: Int = 1,

    // Enhanced fields for role-play scenarios
    val category: String = "DAILY_CONVERSATION",  // ScenarioCategory enum as string
    val estimatedDuration: Int = 10,              // Minutes
    val hasGoals: Boolean = false,                // Whether this scenario has objectives
    val hasBranching: Boolean = false,            // Whether this has multiple paths
    val replayValue: Int = 1,                     // 1-5 stars for replay value
    val thumbnailEmoji: String = "💬"             // Emoji for UI display
)
