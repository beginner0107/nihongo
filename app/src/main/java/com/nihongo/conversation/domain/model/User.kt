package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val level: Int = 1,
    val avatarId: Int = 0, // 0-5 for 6 preset avatars
    val learningGoal: String = "", // e.g., "Travel to Japan", "Watch anime without subtitles"
    val favoriteScenarios: String = "", // Comma-separated scenario IDs
    val nativeLanguage: String = "Korean",
    val bio: String = "", // Short self-introduction
    val studyStartDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
