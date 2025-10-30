package com.nihongo.conversation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.ConversationStats
import com.nihongo.conversation.domain.model.GrammarFeedback
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.PronunciationHistory
import com.nihongo.conversation.domain.model.ReviewHistory
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.ScenarioBranch
import com.nihongo.conversation.domain.model.ScenarioGoal
import com.nihongo.conversation.domain.model.ScenarioOutcome
import com.nihongo.conversation.domain.model.User
import com.nihongo.conversation.domain.model.VocabularyEntry

@Database(
    entities = [
        User::class,
        Scenario::class,
        Conversation::class,
        Message::class,
        VocabularyEntry::class,
        ReviewHistory::class,
        PronunciationHistory::class,
        GrammarFeedback::class,
        ScenarioGoal::class,
        ScenarioOutcome::class,
        ScenarioBranch::class
    ],
    views = [
        ConversationStats::class
    ],
    version = 7,
    exportSchema = false
)
abstract class NihongoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun pronunciationHistoryDao(): PronunciationHistoryDao
    abstract fun grammarFeedbackDao(): GrammarFeedbackDao
    abstract fun scenarioGoalDao(): ScenarioGoalDao
    abstract fun scenarioOutcomeDao(): ScenarioOutcomeDao
    abstract fun scenarioBranchDao(): ScenarioBranchDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isCompleted column to conversations table
                database.execSQL("ALTER TABLE conversations ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create vocabulary_entries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS vocabulary_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        word TEXT NOT NULL,
                        reading TEXT,
                        meaning TEXT NOT NULL,
                        exampleSentence TEXT,
                        sourceConversationId INTEGER,
                        difficulty INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        lastReviewedAt INTEGER,
                        nextReviewAt INTEGER NOT NULL,
                        reviewCount INTEGER NOT NULL DEFAULT 0,
                        correctCount INTEGER NOT NULL DEFAULT 0,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        interval INTEGER NOT NULL DEFAULT 0,
                        isMastered INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                    )
                """)

                // Create indices for vocabulary_entries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_entries_userId ON vocabulary_entries(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_entries_word ON vocabulary_entries(word)")

                // Create review_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS review_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vocabularyId INTEGER NOT NULL,
                        reviewedAt INTEGER NOT NULL,
                        quality INTEGER NOT NULL,
                        timeSpentMs INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(vocabularyId) REFERENCES vocabulary_entries(id) ON DELETE CASCADE
                    )
                """)

                // Create indices for review_history
                database.execSQL("CREATE INDEX IF NOT EXISTS index_review_history_vocabularyId ON review_history(vocabularyId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_review_history_reviewedAt ON review_history(reviewedAt)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add performance optimization indices

                // Conversations table indices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_userId ON conversations(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_scenarioId ON conversations(scenarioId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_isCompleted ON conversations(isCompleted)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_user_scenario_status ON conversations(userId, scenarioId, isCompleted)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_conv_updated ON conversations(updatedAt)")

                // Messages table indices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_msg_conv_time ON messages(conversationId, timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_msg_timestamp ON messages(timestamp)")

                // Vocabulary entries additional indices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_entries_nextReviewAt ON vocabulary_entries(nextReviewAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_vocab_user_review ON vocabulary_entries(userId, nextReviewAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_vocab_user_mastered ON vocabulary_entries(userId, isMastered)")

                // Create database view for conversation statistics
                // Note: Must match exact SQL from @DatabaseView annotation including backticks
                database.execSQL("""
                    CREATE VIEW `conversation_stats` AS SELECT
                            c.id as conversationId,
                            c.userId,
                            c.scenarioId,
                            c.createdAt,
                            c.updatedAt,
                            c.isCompleted,
                            COUNT(m.id) as messageCount,
                            SUM(CASE WHEN m.isUser = 1 THEN 1 ELSE 0 END) as userMessageCount,
                            SUM(CASE WHEN m.isUser = 0 THEN 1 ELSE 0 END) as aiMessageCount,
                            MAX(m.timestamp) as lastMessageTime,
                            AVG(m.complexityScore) as avgComplexity,
                            (c.updatedAt - c.createdAt) as duration
                        FROM conversations c
                        LEFT JOIN messages m ON c.id = m.conversationId
                        GROUP BY c.id
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create pronunciation_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pronunciation_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        messageId INTEGER,
                        vocabularyId INTEGER,
                        expectedText TEXT NOT NULL,
                        recognizedText TEXT NOT NULL,
                        accuracyScore INTEGER NOT NULL,
                        wordComparisonJson TEXT NOT NULL,
                        practicedAt INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        attemptNumber INTEGER NOT NULL DEFAULT 1,
                        source TEXT NOT NULL DEFAULT 'CHAT',
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON DELETE SET NULL,
                        FOREIGN KEY(vocabularyId) REFERENCES vocabulary_entries(id) ON DELETE SET NULL
                    )
                """)

                // Create indices for pronunciation_history
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pronunciation_history_userId ON pronunciation_history(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pronunciation_history_messageId ON pronunciation_history(messageId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pronunciation_history_vocabularyId ON pronunciation_history(vocabularyId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pronunciation_history_practicedAt ON pronunciation_history(practicedAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_user_date ON pronunciation_history(userId, practicedAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_pronunciation_user_score ON pronunciation_history(userId, accuracyScore)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create grammar_feedback table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS grammar_feedback (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        messageId INTEGER NOT NULL,
                        originalText TEXT NOT NULL,
                        correctedText TEXT,
                        feedbackType TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        betterExpression TEXT,
                        additionalNotes TEXT,
                        grammarPattern TEXT,
                        userAcknowledged INTEGER NOT NULL DEFAULT 0,
                        userAppliedCorrection INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON DELETE CASCADE
                    )
                """)

                // Create indices for grammar_feedback
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_userId ON grammar_feedback(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_messageId ON grammar_feedback(messageId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_createdAt ON grammar_feedback(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_feedbackType ON grammar_feedback(feedbackType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_severity ON grammar_feedback(severity)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_grammar_user_type ON grammar_feedback(userId, feedbackType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_grammar_user_date ON grammar_feedback(userId, createdAt)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to scenarios table
                database.execSQL("ALTER TABLE scenarios ADD COLUMN category TEXT NOT NULL DEFAULT 'DAILY_CONVERSATION'")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN estimatedDuration INTEGER NOT NULL DEFAULT 10")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN hasGoals INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN hasBranching INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN replayValue INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN thumbnailEmoji TEXT NOT NULL DEFAULT '💬'")

                // Create scenario_goals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenario_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        goalType TEXT NOT NULL,
                        description TEXT NOT NULL,
                        descriptionKorean TEXT NOT NULL,
                        targetValue INTEGER NOT NULL DEFAULT 1,
                        keywords TEXT,
                        isRequired INTEGER NOT NULL DEFAULT 1,
                        points INTEGER NOT NULL DEFAULT 10,
                        `order` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(scenarioId) REFERENCES scenarios(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_goals_scenarioId ON scenario_goals(scenarioId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_goals_isRequired ON scenario_goals(isRequired)")

                // Create scenario_outcomes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenario_outcomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        outcomeType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        titleKorean TEXT NOT NULL,
                        description TEXT NOT NULL,
                        descriptionKorean TEXT NOT NULL,
                        requiredGoals TEXT,
                        minScore INTEGER NOT NULL DEFAULT 0,
                        maxScore INTEGER NOT NULL DEFAULT 100,
                        triggerKeywords TEXT,
                        FOREIGN KEY(scenarioId) REFERENCES scenarios(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_outcomes_scenarioId ON scenario_outcomes(scenarioId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_outcomes_outcomeType ON scenario_outcomes(outcomeType)")

                // Create scenario_branches table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scenario_branches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        triggerPoint INTEGER NOT NULL,
                        triggerKeywords TEXT,
                        pathAPrompt TEXT NOT NULL,
                        pathADescription TEXT NOT NULL,
                        pathADescriptionKorean TEXT NOT NULL,
                        pathBPrompt TEXT NOT NULL,
                        pathBDescription TEXT NOT NULL,
                        pathBDescriptionKorean TEXT NOT NULL,
                        pathCPrompt TEXT,
                        pathCDescription TEXT,
                        pathCDescriptionKorean TEXT,
                        FOREIGN KEY(scenarioId) REFERENCES scenarios(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_branches_scenarioId ON scenario_branches(scenarioId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scenario_branches_triggerPoint ON scenario_branches(triggerPoint)")
            }
        }
    }
}
