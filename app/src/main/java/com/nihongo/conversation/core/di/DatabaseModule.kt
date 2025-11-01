package com.nihongo.conversation.core.di

import android.content.Context
import androidx.room.Room
import com.nihongo.conversation.data.local.*
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
                NihongoDatabase.MIGRATION_5_6,
                NihongoDatabase.MIGRATION_6_7,
                NihongoDatabase.MIGRATION_8_9,
                NihongoDatabase.MIGRATION_9_10,
                NihongoDatabase.MIGRATION_10_11  // Phase 1: Unique index
                // Removed MIGRATION_2_3 and MIGRATION_3_4 due to SQL formatting issues
                // These migrations will fallback to destructive migration
            )
            // Allow destructive migration if any migration fails (including 2->3, 3->4)
            // This will clear all data but ensure app doesn't crash
            .fallbackToDestructiveMigration()
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
}
