package com.nihongo.conversation.core.di

import com.nihongo.conversation.data.repository.SavedMessageRepository
import com.nihongo.conversation.data.repository.SavedMessageRepositoryImpl
import com.nihongo.conversation.data.repository.VoiceRecordingRepository
import com.nihongo.conversation.data.repository.VoiceRecordingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing Repository implementations
 *
 * Phase 5: ChatScreen Enhancement
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSavedMessageRepository(
        impl: SavedMessageRepositoryImpl
    ): SavedMessageRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRecordingRepository(
        impl: VoiceRecordingRepositoryImpl
    ): VoiceRecordingRepository
}
