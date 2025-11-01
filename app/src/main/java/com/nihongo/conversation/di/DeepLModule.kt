package com.nihongo.conversation.di

import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.data.remote.deepl.DeepLApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeepLRetrofit

/**
 * Hilt Module for DeepL API
 *
 * DeepL API Free 제약사항:
 * - 한 달에 500,000자까지 번역
 * - 최대 2개의 API 키 생성 및 관리
 * - Base URL: https://api-free.deepl.com
 */
@Module
@InstallIn(SingletonComponent::class)
object DeepLModule {

    @Provides
    @Singleton
    @DeepLRetrofit
    fun provideDeepLOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @DeepLRetrofit
    fun provideDeepLRetrofit(
        @DeepLRetrofit okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api-free.deepl.com/")  // DeepL Free API endpoint
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepLApiService(
        @DeepLRetrofit retrofit: Retrofit
    ): DeepLApiService {
        return retrofit.create(DeepLApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("DeepLApiKey")
    fun provideDeepLApiKey(): String {
        return BuildConfig.DEEPL_API_KEY
    }
}
