package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val level: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
