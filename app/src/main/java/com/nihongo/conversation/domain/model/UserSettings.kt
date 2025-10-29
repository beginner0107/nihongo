package com.nihongo.conversation.domain.model

data class UserSettings(
    val difficultyLevel: Int = 1,      // 1 = 初級, 2 = 中級, 3 = 上級
    val speechSpeed: Float = 1.0f,     // 0.5 ~ 2.0
    val autoSpeak: Boolean = true,
    val showRomaji: Boolean = true
)
