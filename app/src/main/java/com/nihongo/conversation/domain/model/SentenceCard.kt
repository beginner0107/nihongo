package com.nihongo.conversation.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sentence-based flashcard from real conversations
 * Context-based learning is more effective than isolated words
 */
@Entity(
    tableName = "sentence_cards",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("userId"),
        Index("messageId"),
        Index("conversationId"),
        Index("pattern"),
        Index("nextReviewDate")
    ]
)
data class SentenceCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Long,

    // Source context
    val messageId: Long? = null,              // Original message this came from
    val conversationId: Long? = null,         // Conversation context
    val scenarioTitle: String? = null,        // Scenario name for context

    // Sentence content
    val sentence: String,                     // Full Japanese sentence
    val translation: String,                  // English/Korean translation
    val romanization: String? = null,         // Romaji for beginners

    // Context information
    val conversationContext: String? = null,  // Previous message for context
    val situationDescription: String? = null, // What situation this was used in

    // Grammar pattern (if applicable)
    val pattern: String? = null,              // e.g., "〜てください", "〜ことができる"
    val patternExplanation: String? = null,   // Grammar explanation

    // Audio
    val audioUrl: String? = null,             // URL to native audio
    val hasAudio: Boolean = false,

    // Practice data
    val difficulty: CardDifficulty = CardDifficulty.NORMAL,
    val tags: String = "",                    // Comma-separated tags

    // SRS (Spaced Repetition System)
    val repetitions: Int = 0,                 // Number of successful reviews
    val easeFactor: Float = 2.5f,             // Ease factor (1.3 - 2.5)
    val interval: Int = 1,                    // Days until next review
    val nextReviewDate: Long = System.currentTimeMillis(),

    // Statistics
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),

    // Practice modes completed
    val hasCompletedReading: Boolean = false,
    val hasCompletedListening: Boolean = false,
    val hasCompletedFillInBlank: Boolean = false,
    val hasCompletedSpeaking: Boolean = false
) {
    val successRate: Float
        get() {
            val total = correctCount + incorrectCount
            return if (total > 0) (correctCount.toFloat() / total) * 100f else 0f
        }

    val isDue: Boolean
        get() = System.currentTimeMillis() >= nextReviewDate

    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * Card difficulty level
 */
enum class CardDifficulty {
    EASY,       // Simple, common sentences
    NORMAL,     // Standard sentences
    HARD,       // Complex grammar or vocabulary
    VERY_HARD   // Advanced patterns
}

/**
 * Fill-in-the-blank exercise for a sentence
 */
data class FillInTheBlankExercise(
    val sentenceCardId: Long,
    val sentence: String,                     // Original sentence
    val blankedSentence: String,              // Sentence with ___ blanks
    val blanks: List<Blank>,                  // List of blanked words/phrases
    val hints: List<String> = emptyList(),    // Hints for each blank
    val distractors: List<String> = emptyList() // Wrong answer choices
)

/**
 * Single blank in a sentence
 */
data class Blank(
    val position: Int,                        // Position in sentence
    val correctAnswer: String,                // Correct word/phrase
    val startIndex: Int,                      // Character start index
    val endIndex: Int,                        // Character end index
    val blankType: BlankType,
    val hint: String? = null
)

/**
 * Types of blanks for different learning focuses
 */
enum class BlankType {
    PARTICLE,       // は、が、を、に、で、etc.
    VERB,           // 食べる、行く、見る
    ADJECTIVE,      // 大きい、きれいな
    NOUN,           // 本、人、時間
    PATTERN,        // Grammar pattern (〜ています、〜ことができる)
    ENTIRE_PHRASE   // Multiple words
}

/**
 * Practice session for sentence cards
 */
data class SentencePracticeSession(
    val id: Long = 0,
    val userId: Long,
    val cards: List<SentenceCard>,
    val practiceMode: PracticeMode,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    val results: MutableList<PracticeResult> = mutableListOf()
) {
    val isComplete: Boolean
        get() = results.size == cards.size

    val accuracy: Float
        get() {
            if (results.isEmpty()) return 0f
            val correct = results.count { it.wasCorrect }
            return (correct.toFloat() / results.size) * 100f
        }

    val currentCardIndex: Int
        get() = results.size
}

/**
 * Practice mode for sentences
 */
enum class PracticeMode {
    READING,         // Show sentence, recall meaning
    LISTENING,       // Listen to audio, type what you hear
    FILL_IN_BLANK,   // Complete missing words
    SPEAKING,        // Record yourself saying the sentence
    MIXED            // Random mix of all modes
}

/**
 * Result of practicing a single card
 */
data class PracticeResult(
    val cardId: Long,
    val practiceMode: PracticeMode,
    val wasCorrect: Boolean,
    val userAnswer: String? = null,
    val timeSpent: Int,                       // Milliseconds
    val difficulty: ReviewDifficulty
)

/**
 * User's assessment of difficulty during review
 */
enum class ReviewDifficulty {
    AGAIN,      // Completely forgot, show again soon
    HARD,       // Difficult, but got it
    GOOD,       // Correct with normal effort
    EASY        // Very easy, increase interval significantly
}

/**
 * Pattern template for sentence generation
 */
data class SentencePattern(
    val id: Long = 0,
    val pattern: String,                      // e.g., "〜てください"
    val explanation: String,                  // Grammar explanation
    val jlptLevel: String? = null,            // N5, N4, N3, N2, N1
    val exampleSentences: List<String> = emptyList(),
    val commonUseCases: List<String> = emptyList(),
    val difficulty: CardDifficulty = CardDifficulty.NORMAL
)

/**
 * Study statistics for sentence cards
 */
data class SentenceStudyStats(
    val totalCards: Int,
    val dueCards: Int,
    val newCards: Int,
    val reviewedToday: Int,
    val averageAccuracy: Float,
    val totalStudyTime: Int,                  // Minutes
    val currentStreak: Int,                   // Days
    val cardsByDifficulty: Map<CardDifficulty, Int> = emptyMap(),
    val cardsByPattern: Map<String, Int> = emptyMap(),
    val strongestPatterns: List<String> = emptyList(),
    val weakestPatterns: List<String> = emptyList()
)
