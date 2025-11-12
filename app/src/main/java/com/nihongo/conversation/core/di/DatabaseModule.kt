package com.nihongo.conversation.core.di

import android.content.Context
import androidx.room.Room
import com.nihongo.conversation.data.local.*
import com.nihongo.conversation.data.local.dao.TranslationCacheDao
import com.nihongo.conversation.data.local.dao.GrammarFeedbackCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNihongoDatabase(
        @ApplicationContext context: Context
    ): NihongoDatabase {
        return Room.databaseBuilder(
            context,
            NihongoDatabase::class.java,
            "nihongo_database"
        )
            .addMigrations(
                NihongoDatabase.MIGRATION_1_2,
                NihongoDatabase.MIGRATION_2_3,  // Phase 3: Restored
                NihongoDatabase.MIGRATION_3_4,  // Phase 3: Restored
                NihongoDatabase.MIGRATION_4_5,  // Phase 3: Restored
                NihongoDatabase.MIGRATION_5_6,
                NihongoDatabase.MIGRATION_6_7,
                NihongoDatabase.MIGRATION_7_8,  // Phase 3: Restored
                NihongoDatabase.MIGRATION_8_9,
                NihongoDatabase.MIGRATION_9_10,
                NihongoDatabase.MIGRATION_10_11,  // Phase 1: Unique index
                NihongoDatabase.MIGRATION_11_12,  // DeepL translation cache
                NihongoDatabase.MIGRATION_12_13,  // Custom scenario support
                NihongoDatabase.MIGRATION_13_14,  // Remove User.level (use scenario difficulty)
                NihongoDatabase.MIGRATION_14_15,  // Grammar feedback cache
                NihongoDatabase.MIGRATION_15_16,  // Quest system
                NihongoDatabase.MIGRATION_16_17   // Phase 5: Message bookmarking
            )
            // Phase 5: All migrations provided - no destructive migration needed
            // This prevents user data loss in production
            // Complete migration path: 1→2→3→4→5→6→7→8→9→10→11→12→13→14→15→16→17
            .build()
    }

    @Provides
    fun provideUserDao(database: NihongoDatabase): UserDao = database.userDao()

    @Provides
    fun provideScenarioDao(database: NihongoDatabase): ScenarioDao = database.scenarioDao()

    @Provides
    fun provideConversationDao(database: NihongoDatabase): ConversationDao =
        database.conversationDao()

    @Provides
    fun provideMessageDao(database: NihongoDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideVocabularyDao(database: NihongoDatabase): VocabularyDao = database.vocabularyDao()

    @Provides
    fun providePronunciationHistoryDao(database: NihongoDatabase): PronunciationHistoryDao =
        database.pronunciationHistoryDao()

    @Provides
    fun provideGrammarFeedbackDao(database: NihongoDatabase): GrammarFeedbackDao =
        database.grammarFeedbackDao()

    @Provides
    fun provideScenarioGoalDao(database: NihongoDatabase): ScenarioGoalDao =
        database.scenarioGoalDao()

    @Provides
    fun provideScenarioOutcomeDao(database: NihongoDatabase): ScenarioOutcomeDao =
        database.scenarioOutcomeDao()

    @Provides
    fun provideScenarioBranchDao(database: NihongoDatabase): ScenarioBranchDao =
        database.scenarioBranchDao()

    @Provides
    fun provideSentenceCardDao(database: NihongoDatabase): SentenceCardDao =
        database.sentenceCardDao()

    @Provides
    fun provideConversationPatternDao(database: NihongoDatabase): ConversationPatternDao =
        database.conversationPatternDao()

    @Provides
    fun provideCachedResponseDao(database: NihongoDatabase): CachedResponseDao =
        database.cachedResponseDao()

    @Provides
    fun provideCacheAnalyticsDao(database: NihongoDatabase): CacheAnalyticsDao =
        database.cacheAnalyticsDao()

    @Provides
    fun provideTranslationCacheDao(database: NihongoDatabase): TranslationCacheDao =
        database.translationCacheDao()

    @Provides
    fun provideGrammarFeedbackCacheDao(database: NihongoDatabase): GrammarFeedbackCacheDao =
        database.grammarFeedbackCacheDao()

    @Provides
    fun provideDailyQuestDao(database: NihongoDatabase): DailyQuestDao =
        database.dailyQuestDao()

    @Provides
    fun provideUserPointsDao(database: NihongoDatabase): UserPointsDao =
        database.userPointsDao()

    // Phase 5: Temporarily disabled
    // @Provides
    // fun provideSavedMessageDao(database: NihongoDatabase): SavedMessageDao =
    //     database.savedMessageDao()
}
