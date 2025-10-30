package com.nihongo.conversation.core.di

import com.nihongo.conversation.core.cache.FuzzyMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideFuzzyMatcher(): FuzzyMatcher {
        return FuzzyMatcher()
    }
}
