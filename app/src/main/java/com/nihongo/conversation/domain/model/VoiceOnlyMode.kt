package com.nihongo.conversation.domain.model

/**
 * Voice-only conversation mode state
 * Simulates real-life conversations by hiding all text
 */
data class VoiceOnlySession(
    val isActive: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val conversationId: Long? = null,
    val messageCount: Int = 0,
    val targetDuration: Int = 5, // Minutes
    val transcript: List<TranscriptEntry> = emptyList()
) {
    val elapsedMinutes: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000 / 60).toInt()

    val elapsedSeconds: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000).toInt()

    val remainingMinutes: Int
        get() = maxOf(0, targetDuration - elapsedMinutes)

    val isComplete: Boolean
        get() = elapsedMinutes >= targetDuration

    val completionPercentage: Int
        get() = minOf(100, (elapsedMinutes * 100) / targetDuration)
}

/**
 * Transcript entry for post-conversation review
 */
data class TranscriptEntry(
    val messageId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val wasSpoken: Boolean = false,
    val wasHeard: Boolean = false
)

/**
 * Voice state for visual indicators
 */
enum class VoiceState {
    IDLE,           // Not speaking or listening
    LISTENING,      // Listening to user speech
    PROCESSING,     // Processing speech recognition
    SPEAKING,       // AI is speaking
    THINKING        // AI is generating response
}

/**
 * Voice-only mode settings
 */
data class VoiceOnlySettings(
    val hideTextInput: Boolean = true,
    val hideMessages: Boolean = true,
    val showTranscriptAfter: Boolean = true,
    val autoEndAfterDuration: Boolean = true,
    val defaultDuration: Int = 5, // Minutes
    val requireSpeechOnly: Boolean = true,
    val showVisualCues: Boolean = true,
    val enableVibration: Boolean = true
)
