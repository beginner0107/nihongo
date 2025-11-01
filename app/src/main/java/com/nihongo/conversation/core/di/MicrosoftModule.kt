package com.nihongo.conversation.core.di

import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.data.remote.microsoft.MicrosoftTranslatorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for Microsoft Translator Retrofit instance
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MicrosoftRetrofit

/**
 * Hilt module providing Microsoft Translator API dependencies
 *
 * Base URL: https://api.cognitive.microsofttranslator.com/
 * Region: Korea Central
 * Free Tier: 2M chars/month
 */
@Module
@InstallIn(SingletonComponent::class)
object MicrosoftModule {

    @Provides
    @Singleton
    @MicrosoftRetrofit
    fun provideMicrosoftOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @MicrosoftRetrofit
    fun provideMicrosoftRetrofit(
        @MicrosoftRetrofit okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMicrosoftTranslatorService(
        @MicrosoftRetrofit retrofit: Retrofit
    ): MicrosoftTranslatorService {
        return retrofit.create(MicrosoftTranslatorService::class.java)
    }

    @Provides
    @Singleton
    @Named("MicrosoftApiKey")
    fun provideMicrosoftApiKey(): String {
        return BuildConfig.MICROSOFT_TRANSLATOR_KEY
    }

    @Provides
    @Singleton
    @Named("MicrosoftRegion")
    fun provideMicrosoftRegion(): String {
        return "koreacentral"
    }
}
