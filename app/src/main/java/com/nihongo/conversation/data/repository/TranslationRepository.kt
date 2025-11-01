package com.nihongo.conversation.data.repository

import android.util.Log
import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.core.translation.MLKitTranslator
import com.nihongo.conversation.core.translation.TranslationException
import com.nihongo.conversation.data.local.dao.TranslationCacheDao
import com.nihongo.conversation.data.local.entity.TranslationCacheEntity
import com.nihongo.conversation.data.remote.deepl.DeepLApiService
import com.nihongo.conversation.data.remote.deepl.DeepLRequest
import com.nihongo.conversation.data.remote.deepl.TranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Translation Repository
 *
 * Hybrid translation system supporting both:
 * - DeepL API (premium, contextual accuracy)
 * - ML Kit (fast, offline, free)
 *
 * Strategy:
 * 1. Check cache first (30-day retention)
 * 2. Try DeepL if selected and quota available
 * 3. Fallback to ML Kit on DeepL failure
 * 4. Cache all successful translations
 *
 * DeepL API Free constraints (2025-11-01):
 * - 500,000 characters/month
 * - Maximum 2 API keys
 * - Base URL: https://api-free.deepl.com/
 */
@Singleton
class TranslationRepository @Inject constructor(
    private val deepLApiService: DeepLApiService,
    private val mlKitTranslator: MLKitTranslator,
    private val translationCacheDao: TranslationCacheDao,
    private val deepLApiKey: String
) {
    private val TAG = "TranslationRepository"

    // Character usage tracking (reset manually each month)
    private var monthlyCharCount = 0
    private val MONTHLY_LIMIT = 500_000

    /**
     * Translate Japanese text to Korean
     *
     * @param text Japanese text to translate
     * @param provider Preferred translation provider
     * @param useCache Enable/disable cache lookup
     * @param fallbackToMLKit Auto-fallback to ML Kit on DeepL failure
     * @return Translated Korean text
     */
    suspend fun translate(
        text: String,
        provider: TranslationProvider = TranslationProvider.ML_KIT,
        useCache: Boolean = true,
        fallbackToMLKit: Boolean = true
    ): TranslationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            if (text.isBlank()) {
                return@withContext TranslationResult.Error("번역할 텍스트가 비어 있습니다")
            }

            Log.d(TAG, "Translating (${text.length} chars) with provider: $provider, cache: $useCache")

            // 1. Check cache first
            if (useCache) {
                val cached = translationCacheDao.getTranslation(
                    sourceText = text,
                    sourceLang = "ja",
                    targetLang = "ko"
                )

                if (cached != null) {
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Cache hit from ${cached.provider} (${elapsed}ms)")
                    return@withContext TranslationResult.Success(
                        translatedText = cached.translatedText,
                        provider = TranslationProvider.valueOf(cached.provider.uppercase()),
                        fromCache = true,
                        elapsed = elapsed
                    )
                }
            }

            // 2. Try preferred provider
            val result = when (provider) {
                TranslationProvider.DEEP_L -> {
                    translateWithDeepL(text, fallbackToMLKit)
                }
                TranslationProvider.ML_KIT -> {
                    translateWithMLKit(text)
                }
            }

            // 3. Cache successful translation
            if (result is TranslationResult.Success && useCache) {
                try {
                    translationCacheDao.cacheTranslation(
                        TranslationCacheEntity(
                            sourceText = text,
                            translatedText = result.translatedText,
                            provider = result.provider.name.lowercase(),
                            timestamp = System.currentTimeMillis(),
                            sourceLang = "ja",
                            targetLang = "ko"
                        )
                    )
                    Log.d(TAG, "Translation cached (${result.provider})")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cache translation", e)
                    // Don't fail the whole operation if caching fails
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Translation completed: ${result.javaClass.simpleName} (${elapsed}ms)")

            result

        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "Translation failed after ${elapsed}ms", e)
            TranslationResult.Error(e.message ?: "번역 실패")
        }
    }

    /**
     * Translate using DeepL API with optional fallback to ML Kit
     */
    private suspend fun translateWithDeepL(
        text: String,
        fallbackToMLKit: Boolean
    ): TranslationResult {
        return try {
            // Check quota before calling API
            if (monthlyCharCount + text.length > MONTHLY_LIMIT) {
                Log.w(TAG, "DeepL quota exceeded ($monthlyCharCount/$MONTHLY_LIMIT chars)")
                if (fallbackToMLKit) {
                    Log.d(TAG, "Falling back to ML Kit due to quota")
                    return translateWithMLKit(text)
                }
                return TranslationResult.Error("DeepL 월간 한도 초과 (50만자)")
            }

            // Check API key
            if (deepLApiKey.isBlank() || deepLApiKey == "") {
                Log.w(TAG, "DeepL API key not configured")
                if (fallbackToMLKit) {
                    Log.d(TAG, "Falling back to ML Kit (no API key)")
                    return translateWithMLKit(text)
                }
                return TranslationResult.Error("DeepL API 키가 설정되지 않았습니다")
            }

            Log.d(TAG, "Calling DeepL API...")
            val startTime = System.currentTimeMillis()

            val response = deepLApiService.translate(
                authorization = "DeepL-Auth-Key $deepLApiKey",
                request = DeepLRequest(
                    text = listOf(text),
                    sourceLang = "JA",
                    targetLang = "KO"
                )
            )

            val translatedText = response.translations.firstOrNull()?.text

            if (translatedText.isNullOrBlank()) {
                throw Exception("DeepL API returned empty translation")
            }

            // Update usage counter
            monthlyCharCount += text.length

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "DeepL success: '$translatedText' (${elapsed}ms, $monthlyCharCount/$MONTHLY_LIMIT chars used)")

            TranslationResult.Success(
                translatedText = translatedText,
                provider = TranslationProvider.DEEP_L,
                fromCache = false,
                elapsed = elapsed
            )

        } catch (e: Exception) {
            Log.e(TAG, "DeepL API failed", e)

            // Fallback to ML Kit if enabled
            if (fallbackToMLKit) {
                Log.d(TAG, "Falling back to ML Kit after DeepL failure")
                return translateWithMLKit(text)
            }

            TranslationResult.Error("DeepL 번역 실패: ${e.message}")
        }
    }

    /**
     * Translate using ML Kit (on-device)
     */
    private suspend fun translateWithMLKit(text: String): TranslationResult {
        return try {
            Log.d(TAG, "Using ML Kit translator...")
            val startTime = System.currentTimeMillis()

            val translatedText = mlKitTranslator.translate(text)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "ML Kit success: '$translatedText' (${elapsed}ms)")

            TranslationResult.Success(
                translatedText = translatedText,
                provider = TranslationProvider.ML_KIT,
                fromCache = false,
                elapsed = elapsed
            )

        } catch (e: TranslationException) {
            Log.e(TAG, "ML Kit translation failed", e)
            TranslationResult.Error("ML Kit 번역 실패: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit unexpected error", e)
            TranslationResult.Error("번역 중 오류 발생")
        }
    }

    /**
     * Clean old cache entries (>30 days)
     * Call this periodically (e.g., on app start)
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days
            val deletedCount = translationCacheDao.cleanOldCache(cutoffTime)
            Log.d(TAG, "Cleaned $deletedCount old cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old cache", e)
        }
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        try {
            val totalCount = translationCacheDao.getCacheCount()

            CacheStats(
                totalEntries = totalCount,
                estimatedSizeKB = totalCount * 1 // Rough estimate: 1KB per entry
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache stats", e)
            CacheStats(0, 0)
        }
    }

    /**
     * Clear all translation cache
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            translationCacheDao.clearAllCache()
            Log.d(TAG, "All cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * Get current DeepL usage
     */
    fun getDeepLUsage(): UsageStats {
        return UsageStats(
            charactersUsed = monthlyCharCount,
            monthlyLimit = MONTHLY_LIMIT,
            percentageUsed = (monthlyCharCount * 100f / MONTHLY_LIMIT).toInt()
        )
    }

    /**
     * Reset monthly usage counter (call at start of each month)
     */
    fun resetMonthlyUsage() {
        monthlyCharCount = 0
        Log.d(TAG, "Monthly usage counter reset")
    }

    /**
     * Initialize ML Kit translator (download model if needed)
     */
    suspend fun initializeMLKit(downloadIfNeeded: Boolean = true): Result<Unit> {
        return mlKitTranslator.initialize(downloadIfNeeded)
    }

    /**
     * Check if ML Kit model is downloaded
     */
    suspend fun isMLKitReady(): Boolean {
        return mlKitTranslator.isModelDownloaded()
    }
}

/**
 * Translation result sealed class
 */
sealed class TranslationResult {
    data class Success(
        val translatedText: String,
        val provider: TranslationProvider,
        val fromCache: Boolean,
        val elapsed: Long
    ) : TranslationResult()

    data class Error(
        val message: String
    ) : TranslationResult()
}

/**
 * Cache statistics
 */
data class CacheStats(
    val totalEntries: Int,
    val estimatedSizeKB: Int
)

/**
 * DeepL usage statistics
 */
data class UsageStats(
    val charactersUsed: Int,
    val monthlyLimit: Int,
    val percentageUsed: Int
)
