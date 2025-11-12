package com.nihongo.conversation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Saved Message Entity - For bookmarking important messages
 *
 * Phase 5: ChatScreen Enhancement
 * Allows users to bookmark messages for later review
 */
@Entity(
    tableName = "saved_messages",
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["userId"]),
        Index(value = ["savedAt"])
    ]
)
data class SavedMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val messageId: Long,  // Reference to Message.id
    val userId: Long,

    // Denormalized fields for faster queries (avoid JOIN)
    val messageContent: String,
    val isUserMessage: Boolean,
    val scenarioTitle: String,

    // Optional note
    val userNote: String? = null,

    // Tags for categorization
    val tags: String? = null,  // Comma-separated: "grammar,vocabulary,example"

    val savedAt: Long = System.currentTimeMillis()
)
