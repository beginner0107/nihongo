package com.nihongo.conversation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.ConversationStats
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.ReviewHistory
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User
import com.nihongo.conversation.domain.model.VocabularyEntry

@Database(
    entities = [
        User::class,
        Scenario::class,
        Conversation::class,
        Message::class,
        VocabularyEntry::class,
        ReviewHistory::class
    ],
    views = [
        ConversationStats::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NihongoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun vocabularyDao(): VocabularyDao

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
    }
}
