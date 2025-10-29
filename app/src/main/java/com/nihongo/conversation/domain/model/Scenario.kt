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
    val createdAt: Long = System.currentTimeMillis()
)
