package com.nihongo.conversation.domain.model

data class UserSettings(
    val speechSpeed: Float = 1.0f,     // 0.5 ~ 2.0
    val autoSpeak: Boolean = true,
    val showRomaji: Boolean = true,
    val feedbackEnabled: Boolean = true // Real-time AI feedback
)
