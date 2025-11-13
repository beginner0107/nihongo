package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_recordings",
    foreignKeys = [
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("messageId", name = "idx_voice_recordings_message"),
        Index("conversationId", name = "idx_voice_recordings_conversation"),
        Index("createdAt", name = "idx_voice_recordings_created_at")
    ]
)
data class VoiceRecording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long? = null,
    val conversationId: Long,
    val filePath: String,
    val fileName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val recordedAt: Long,
    val language: String,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

