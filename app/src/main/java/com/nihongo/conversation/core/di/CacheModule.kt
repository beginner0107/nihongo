package com.nihongo.conversation.core.di

import com.nihongo.conversation.core.cache.FuzzyMatcher
import com.nihongo.conversation.core.cache.FuzzyMatcherConfig
import com.nihongo.conversation.core.cache.JapaneseTextNormalizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Cache module providing fuzzy matching and text normalization
 * Phase 3B: Externalized configuration for better testability
 */
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    /**
     * Provides shared Japanese text normalizer
     * Phase 3B: Singleton for consistency across app (cache, analytics, etc.)
     */
    @Provides
    @Singleton
    fun provideJapaneseTextNormalizer(): JapaneseTextNormalizer {
        return JapaneseTextNormalizer.INSTANCE
    }

    /**
     * Provides fuzzy matcher configuration
     * Phase 3B: Can be customized per environment or testing
     */
    @Provides
    @Singleton
    fun provideFuzzyMatcherConfig(): FuzzyMatcherConfig {
        // Use default configuration for production
        // Can be overridden in tests or per-scenario
        return FuzzyMatcherConfig.default()
    }

    /**
     * Provides FuzzyMatcher with injected dependencies
     * Phase 3B: Now accepts configuration and normalizer
     */
    @Provides
    @Singleton
    fun provideFuzzyMatcher(
        config: FuzzyMatcherConfig,
        normalizer: JapaneseTextNormalizer
    ): FuzzyMatcher {
        return FuzzyMatcher(config, normalizer)
    }
}
