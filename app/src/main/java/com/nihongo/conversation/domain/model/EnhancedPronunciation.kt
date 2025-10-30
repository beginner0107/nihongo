package com.nihongo.conversation.domain.model

/**
 * Enhanced pronunciation analysis result with Japanese-specific features
 */
data class EnhancedPronunciationResult(
    val basicResult: PronunciationResult,
    val pitchAccent: PitchAccentAnalysis? = null,
    val intonation: IntonationAnalysis? = null,
    val rhythm: RhythmAnalysis? = null,
    val problematicSounds: List<ProblematicSound> = emptyList(),
    val overallScore: PronunciationScore = PronunciationScore()
)

/**
 * Pitch accent analysis (高低アクセント)
 * Japanese uses pitch accent, not stress accent
 */
data class PitchAccentAnalysis(
    val morae: List<Mora>,
    val accentType: AccentType,
    val accentPosition: Int? = null, // Nucleus position (0 = 平板, 1+ = 起伏)
    val pitchPattern: List<PitchPoint>,
    val matchesNative: Boolean = false,
    val confidence: Float = 0f
) {
    /**
     * Get pitch pattern as string for visualization
     * Example: "LHHL" for こんにちは
     */
    val patternString: String
        get() = morae.joinToString("") { if (it.isHigh) "H" else "L" }
}

/**
 * Individual mora (モーラ) - Japanese rhythmic unit
 */
data class Mora(
    val text: String,          // e.g., "こ", "ん", "に"
    val isHigh: Boolean,       // High or low pitch
    val duration: Int,         // Duration in milliseconds
    val startTime: Int,        // Start time in audio
    val pitch: Float,          // Fundamental frequency (Hz)
    val intensity: Float       // Volume/loudness
)

/**
 * Pitch accent types in Japanese
 */
enum class AccentType {
    HEIBAN,      // 平板 - Flat (no drop)
    ATAMADAKA,   // 頭高 - Initial high
    NAKADAKA,    // 中高 - Mid high
    ODAKA        // 尾高 - Final high
}

/**
 * Single point in pitch contour
 */
data class PitchPoint(
    val time: Int,        // Milliseconds
    val frequency: Float, // Hz
    val mora: String      // Associated mora
)

/**
 * Intonation pattern analysis (イントネーション)
 * Sentence-level pitch patterns
 */
data class IntonationAnalysis(
    val sentenceType: SentenceType,
    val pitchContour: List<PitchPoint>,
    val finalRise: Boolean,          // Rising at end (question)
    val finalFall: Boolean,          // Falling at end (statement)
    val matchesExpected: Boolean,
    val suggestions: List<String> = emptyList()
)

/**
 * Japanese sentence types with different intonation
 */
enum class SentenceType {
    STATEMENT,     // 平叙文 - Falling intonation
    QUESTION,      // 疑問文 - Rising intonation
    EXCLAMATION,   // 感嘆文 - Strong emphasis
    COMMAND        // 命令文 - Sharp falling
}

/**
 * Rhythm and speed analysis (リズム・速度)
 */
data class RhythmAnalysis(
    val totalDuration: Int,              // Milliseconds
    val moraDurations: List<Int>,        // Duration per mora
    val averageMoraDuration: Int,        // Average
    val speedRating: SpeedRating,
    val rhythmScore: Float,              // 0-100, consistency
    val pauseLocations: List<Int>,       // Where pauses occurred
    val naturalness: Float,              // 0-100
    val comparison: NativeComparison? = null
)

/**
 * Speaking speed classification
 */
enum class SpeedRating {
    TOO_SLOW,      // 遅すぎる
    SLOW,          // 遅い
    NATURAL,       // 自然
    FAST,          // 速い
    TOO_FAST       // 速すぎる
}

/**
 * Comparison with native speaker
 */
data class NativeComparison(
    val nativeDuration: Int,
    val userDuration: Int,
    val speedRatio: Float,            // user/native
    val rhythmSimilarity: Float,      // 0-100
    val pitchSimilarity: Float        // 0-100
)

/**
 * Problematic sounds in Japanese
 */
data class ProblematicSound(
    val soundType: JapaneseSound,
    val mora: String,
    val position: Int,
    val issue: PronunciationIssue,
    val severity: IssueSeverity,
    val suggestion: String,
    val nativeExample: String? = null
)

/**
 * Common problematic sounds for learners
 */
enum class JapaneseSound {
    RA_GYOU,        // ら行 (r/l confusion)
    TSU_CHU,        // つ/ちゅ distinction
    LONG_VOWEL,     // 長音 (long vowels)
    DOUBLE_CONSONANT, // 促音 (っ)
    N_SOUND,        // ん (syllabic n)
    WA_WO,          // は/を particles
    GA_NGA,         // が/んが distinction
    SHI_CHI,        // し/ち distinction
    TSU_SU,         // つ/す distinction
    FU_HU           // ふ (neither f nor h)
}

/**
 * Types of pronunciation issues
 */
enum class PronunciationIssue {
    WRONG_SOUND,           // 違う音
    WRONG_LENGTH,          // 長さが違う
    WRONG_PITCH,           // 音程が違う
    MISSING_SOUND,         // 音が抜けている
    EXTRA_SOUND,           // 余分な音
    UNCLEAR_ARTICULATION,  // 不明瞭な発音
    WRONG_TIMING           // タイミングが違う
}

/**
 * Severity of pronunciation issue
 */
enum class IssueSeverity {
    CRITICAL,   // 意味が変わる
    HIGH,       // 非常に不自然
    MEDIUM,     // やや不自然
    LOW         // わずかに不自然
}

/**
 * Overall pronunciation scoring
 */
data class PronunciationScore(
    val accuracy: Float = 0f,        // 0-100, correct sounds
    val pitch: Float = 0f,           // 0-100, pitch accent accuracy
    val intonation: Float = 0f,      // 0-100, sentence intonation
    val rhythm: Float = 0f,          // 0-100, timing and rhythm
    val clarity: Float = 0f,         // 0-100, articulation clarity
    val naturalness: Float = 0f      // 0-100, overall naturalness
) {
    val overall: Float
        get() = (accuracy + pitch + intonation + rhythm + clarity + naturalness) / 6

    val grade: PronunciationGrade
        get() = when {
            overall >= 90 -> PronunciationGrade.NATIVE_LIKE
            overall >= 80 -> PronunciationGrade.EXCELLENT
            overall >= 70 -> PronunciationGrade.GOOD
            overall >= 60 -> PronunciationGrade.FAIR
            overall >= 50 -> PronunciationGrade.NEEDS_WORK
            else -> PronunciationGrade.BEGINNER
        }
}

/**
 * Pronunciation proficiency grades
 */
enum class PronunciationGrade {
    NATIVE_LIKE,    // ネイティブレベル
    EXCELLENT,      // 優秀
    GOOD,           // 良い
    FAIR,           // まあまあ
    NEEDS_WORK,     // 要改善
    BEGINNER        // 初心者
}

/**
 * Practice exercise for specific sounds
 */
data class SoundPracticeExercise(
    val soundType: JapaneseSound,
    val targetMora: String,
    val exampleWords: List<String>,
    val minimalPairs: List<Pair<String, String>>, // Contrasting pairs
    val tips: List<String>,
    val nativeAudioUrl: String? = null
)

/**
 * Common minimal pairs for practice
 */
object MinimalPairs {
    val RA_LA = listOf(
        "らっこ" to "楽（らく）",
        "りんご" to "リンゴ",
        "れい" to "零"
    )

    val TSU_CHU = listOf(
        "つくえ" to "注意（ちゅうい）",
        "つづく" to "中（ちゅう）"
    )

    val LONG_SHORT = listOf(
        "おばさん" to "おばあさん", // aunt vs grandmother
        "ここ" to "こうこう",        // here vs high school
        "しゅじん" to "しゅうじん"   // husband vs prisoner
    )

    val DOUBLE_CONSONANT = listOf(
        "かた" to "かった",   // shoulder vs bought
        "きて" to "きって",   // come vs stamp
        "さか" to "さっか"    // slope vs author
    )
}
