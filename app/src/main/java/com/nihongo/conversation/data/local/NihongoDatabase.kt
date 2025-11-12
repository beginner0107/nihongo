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
import com.nihongo.conversation.domain.model.SentenceCard
import com.nihongo.conversation.domain.model.User
import com.nihongo.conversation.domain.model.VocabularyEntry
import com.nihongo.conversation.data.local.entity.TranslationCacheEntity
import com.nihongo.conversation.data.local.dao.TranslationCacheDao
import com.nihongo.conversation.data.local.entity.GrammarFeedbackCacheEntity
import com.nihongo.conversation.data.local.dao.GrammarFeedbackCacheDao
import com.nihongo.conversation.data.local.entity.DailyQuestEntity
import com.nihongo.conversation.data.local.entity.UserPointsEntity

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
        ScenarioBranch::class,
        SentenceCard::class,
        com.nihongo.conversation.domain.model.ConversationPattern::class,
        com.nihongo.conversation.domain.model.CachedResponse::class,
        com.nihongo.conversation.domain.model.CacheAnalytics::class,
        TranslationCacheEntity::class,
        GrammarFeedbackCacheEntity::class,
        DailyQuestEntity::class,
        UserPointsEntity::class
    ],
    views = [
        ConversationStats::class
    ],
    version = 16,  // Added Quest system (DailyQuestEntity, UserPointsEntity)
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
    abstract fun sentenceCardDao(): SentenceCardDao
    abstract fun conversationPatternDao(): ConversationPatternDao
    abstract fun cachedResponseDao(): CachedResponseDao
    abstract fun cacheAnalyticsDao(): CacheAnalyticsDao
    abstract fun translationCacheDao(): TranslationCacheDao
    abstract fun grammarFeedbackCacheDao(): GrammarFeedbackCacheDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun userPointsDao(): UserPointsDao

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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sentence_cards table for sentence-based flashcards
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sentence_cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        messageId INTEGER,
                        conversationId INTEGER,
                        scenarioTitle TEXT,
                        sentence TEXT NOT NULL,
                        translation TEXT NOT NULL,
                        romanization TEXT,
                        conversationContext TEXT,
                        situationDescription TEXT,
                        pattern TEXT,
                        patternExplanation TEXT,
                        audioUrl TEXT,
                        hasAudio INTEGER NOT NULL DEFAULT 0,
                        difficulty TEXT NOT NULL DEFAULT 'NORMAL',
                        tags TEXT NOT NULL DEFAULT '',
                        repetitions INTEGER NOT NULL DEFAULT 0,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        `interval` INTEGER NOT NULL DEFAULT 1,
                        nextReviewDate INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL DEFAULT 0,
                        incorrectCount INTEGER NOT NULL DEFAULT 0,
                        lastReviewedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        hasCompletedReading INTEGER NOT NULL DEFAULT 0,
                        hasCompletedListening INTEGER NOT NULL DEFAULT 0,
                        hasCompletedFillInBlank INTEGER NOT NULL DEFAULT 0,
                        hasCompletedSpeaking INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON DELETE SET NULL,
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE SET NULL
                    )
                """)

                // Create indices for sentence_cards
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_cards_userId ON sentence_cards(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_cards_messageId ON sentence_cards(messageId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_cards_conversationId ON sentence_cards(conversationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_cards_pattern ON sentence_cards(pattern)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sentence_cards_nextReviewDate ON sentence_cards(nextReviewDate)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop table if exists to ensure clean creation
                database.execSQL("DROP TABLE IF EXISTS conversation_patterns")

                // Create conversation_patterns table for caching system - NO DEFAULT VALUES
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS conversation_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pattern TEXT NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        difficultyLevel INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        conversationTurn INTEGER NOT NULL,
                        keywords TEXT NOT NULL,
                        usageCount INTEGER NOT NULL,
                        lastUsedTimestamp INTEGER NOT NULL,
                        successRate REAL NOT NULL,
                        averageSimilarity REAL NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // Create cached_responses table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_responses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patternId INTEGER NOT NULL,
                        response TEXT NOT NULL,
                        variation INTEGER NOT NULL DEFAULT 0,
                        complexityScore INTEGER NOT NULL DEFAULT 0,
                        characterCount INTEGER NOT NULL,
                        wordCount INTEGER NOT NULL,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        lastUsedTimestamp INTEGER NOT NULL,
                        userSatisfactionScore REAL NOT NULL DEFAULT 0.0,
                        isVerified INTEGER NOT NULL DEFAULT 0,
                        generatedByApi INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(patternId) REFERENCES conversation_patterns(id) ON DELETE CASCADE
                    )
                """)

                // Create cache_analytics table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cache_analytics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        userId INTEGER NOT NULL,
                        scenarioId INTEGER NOT NULL,
                        cacheHits INTEGER NOT NULL DEFAULT 0,
                        cacheMisses INTEGER NOT NULL DEFAULT 0,
                        apiCalls INTEGER NOT NULL DEFAULT 0,
                        averageSimilarityScore REAL NOT NULL DEFAULT 0.0,
                        averageResponseTime INTEGER NOT NULL DEFAULT 0,
                        userContinuationRate REAL NOT NULL DEFAULT 0.0,
                        tokensSaved INTEGER NOT NULL DEFAULT 0,
                        estimatedCostSaved REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Create indices for conversation_patterns - these should be created BEFORE the indices
                // No indices needed - the schema expects no indices

                // Create indices for cached_responses
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_responses_patternId ON cached_responses(patternId)")

                // Create indices for cache_analytics
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cache_analytics_userId ON cache_analytics(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cache_analytics_scenarioId ON cache_analytics(scenarioId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cache_analytics_date ON cache_analytics(date)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add slug and promptVersion to scenarios
                database.execSQL("ALTER TABLE scenarios ADD COLUMN slug TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE scenarios ADD COLUMN promptVersion INTEGER NOT NULL DEFAULT 1")

                // Backfill slugs for known seeded IDs (1..12)
                val mappings = mapOf(
                    1 to "restaurant_ordering",
                    2 to "shopping",
                    3 to "hotel_checkin",
                    4 to "making_friends",
                    5 to "phone_reservation",
                    6 to "hospital_visit",
                    7 to "job_interview",
                    8 to "complaint_handling",
                    9 to "emergency_help",
                    10 to "dating_invite",
                    11 to "business_presentation",
                    12 to "girlfriend_conversation"
                )
                for ((id, slug) in mappings) {
                    database.execSQL("UPDATE scenarios SET slug = '$slug' WHERE id = $id")
                }

                // Create unique index on slug
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scenarios_slug ON scenarios(slug)")
            }
        }

        // Phase 1: Add unique index to conversation_patterns
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create unique index on (scenarioId, difficultyLevel, pattern)
                // This prevents duplicate pattern entries
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_conversation_patterns_scenarioId_difficultyLevel_pattern
                    ON conversation_patterns(scenarioId, difficultyLevel, pattern)
                """)
            }
        }

        // Add translation cache for DeepL/MLKit translation caching
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create translation_cache table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS translation_cache (
                        sourceText TEXT NOT NULL PRIMARY KEY,
                        translatedText TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        sourceLang TEXT NOT NULL,
                        targetLang TEXT NOT NULL
                    )
                """)

                // Create indices for translation_cache
                database.execSQL("CREATE INDEX IF NOT EXISTS index_translation_cache_provider ON translation_cache(provider)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_translation_cache_timestamp ON translation_cache(timestamp)")
            }
        }

        // Add isCustom column to scenarios for deletable custom scenarios
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isCustom column to scenarios table
                database.execSQL("ALTER TABLE scenarios ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Remove level column from users table (use scenario difficulty instead)
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table
                database.execSQL("""
                    CREATE TABLE users_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        avatarId INTEGER NOT NULL,
                        learningGoal TEXT NOT NULL,
                        favoriteScenarios TEXT NOT NULL,
                        nativeLanguage TEXT NOT NULL,
                        bio TEXT NOT NULL,
                        studyStartDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    INSERT INTO users_new (id, name, avatarId, learningGoal, favoriteScenarios, nativeLanguage, bio, studyStartDate, createdAt)
                    SELECT id, name, avatarId, learningGoal, favoriteScenarios, nativeLanguage, bio, studyStartDate, createdAt
                    FROM users
                """.trimIndent())

                database.execSQL("DROP TABLE users")
                database.execSQL("ALTER TABLE users_new RENAME TO users")
            }
        }

        // Add grammar_feedback_cache table for caching grammar analysis results
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE grammar_feedback_cache (
                        messageText TEXT NOT NULL PRIMARY KEY,
                        feedbackJson TEXT NOT NULL,
                        userLevel INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_cache_messageText ON grammar_feedback_cache(messageText)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_feedback_cache_timestamp ON grammar_feedback_cache(timestamp)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create daily_quests table
                database.execSQL("""
                    CREATE TABLE daily_quests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        questType TEXT NOT NULL,
                        targetValue INTEGER NOT NULL,
                        currentValue INTEGER NOT NULL,
                        rewardPoints INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_quests_userId ON daily_quests(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_quests_expiresAt ON daily_quests(expiresAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_quests_isCompleted ON daily_quests(isCompleted)")

                // Create user_points table
                database.execSQL("""
                    CREATE TABLE user_points (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        totalPoints INTEGER NOT NULL,
                        todayPoints INTEGER NOT NULL,
                        weeklyPoints INTEGER NOT NULL,
                        level INTEGER NOT NULL,
                        weeklyRank INTEGER,
                        lastResetDate INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
