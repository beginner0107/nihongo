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

/**
 * Furigana (reading guide) display type
 *
 * Examples:
 * - HIRAGANA: 注文(ちゅうもん)
 * - KATAKANA: 注文(チュウモン)
 */
enum class FuriganaType {
    HIRAGANA,  // Default: use hiragana readings
    KATAKANA;  // Use katakana readings (converts hiragana → katakana)

    val displayName: String
        get() = when (this) {
            HIRAGANA -> "ひらがな"
            KATAKANA -> "カタカナ"
        }
}

data class UserSettings(
    val speechSpeed: Float = 1.0f,     // 0.5 ~ 2.0
    val autoSpeak: Boolean = true,
    val showRomaji: Boolean = true,
    val feedbackEnabled: Boolean = true, // Real-time AI feedback
    val textSize: TextSizePreference = TextSizePreference.NORMAL,
    val contrastMode: ContrastMode = ContrastMode.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showFurigana: Boolean = false,  // Show furigana on AI messages
    val furiganaType: FuriganaType = FuriganaType.HIRAGANA,  // Hiragana or Katakana
    val enableVoiceRecording: Boolean = false  // Auto-save voice recordings after STT (disabled by default to avoid double-speaking)
)
