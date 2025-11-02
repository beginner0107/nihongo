package com.nihongo.conversation.domain.model

enum class TextSizePreference {
    SMALL,    // 0.85x
    NORMAL,   // 1.0x
    LARGE,    // 1.15x
    XLARGE;   // 1.3x

    val scale: Float
        get() = when (this) {
            SMALL -> 0.85f
            NORMAL -> 1.0f
            LARGE -> 1.15f
            XLARGE -> 1.3f
        }
}

enum class ContrastMode {
    NORMAL,
    HIGH;

    val isHighContrast: Boolean
        get() = this == HIGH
}

data class UserSettings(
    val speechSpeed: Float = 1.0f,     // 0.5 ~ 2.0
    val autoSpeak: Boolean = true,
    val showRomaji: Boolean = true,
    val feedbackEnabled: Boolean = true, // Real-time AI feedback
    val textSize: TextSizePreference = TextSizePreference.NORMAL,
    val contrastMode: ContrastMode = ContrastMode.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
