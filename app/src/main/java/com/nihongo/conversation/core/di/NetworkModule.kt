package com.nihongo.conversation.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network module providing optimized HTTP clients
 *
 * Key optimizations:
 * - GZIP decompression for responses (automatic with OkHttp)
 * - Connection pooling for request reuse
 * - Timeouts optimized for mobile networks
 * - Secure header logging (debug only)
 * - Japanese locale headers for better API responses
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides OkHttpClient with secure logging and Japanese locale support
     *
     * GZIP handling:
     * - OkHttp automatically decompresses gzip responses
     * - Adds "Accept-Encoding: gzip" header automatically
     * - Note: Does NOT compress request bodies (would need custom interceptor)
     *
     * Connection pooling:
     * - Reuses TCP connections for multiple requests
     * - Reduces latency by ~200-300ms per request
     * - Up to 5 idle connections kept alive
     *
     * Security:
     * - Header-level logging only (no request/response bodies)
     * - Sensitive headers redacted (Authorization, API keys)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Phase 3: Secure logging - headers only, redact sensitive data
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS  // Changed from BODY
            redactHeader("Authorization")
            redactHeader("X-API-Key")
            redactHeader("X-Goog-Api-Key")
        }

        return OkHttpClient.Builder()
            // Phase 3B: Add Japanese locale headers for better API responses
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept-Language", "ja-JP,ko-KR;q=0.9")
                    .header("User-Agent", "Nihongo/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }

            // Connection pooling
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 30,
                    TimeUnit.SECONDS
                )
            )

            // Timeouts optimized for mobile networks
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

            // Phase 3A: Secure logging (debug builds only)
            .apply {
                if (com.nihongo.conversation.BuildConfig.DEBUG) {
                    addInterceptor(loggingInterceptor)
                }
            }

            // Retry on connection failure
            .retryOnConnectionFailure(true)

            .build()
    }

    /**
     * Provides Gson for JSON serialization/deserialization
     * Phase 3B: Optimized for minimal payload size
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            // Phase 3B: Removed serializeNulls() to reduce payload size
            // Nulls are omitted from JSON, reducing bytes over the wire
            .create()
    }
}
