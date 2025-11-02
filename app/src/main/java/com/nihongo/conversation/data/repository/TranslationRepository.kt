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
import com.nihongo.conversation.data.remote.microsoft.MicrosoftTranslateRequest
import com.nihongo.conversation.data.remote.microsoft.MicrosoftTranslatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Unified Translation Repository
 *
 * 3-Provider Hybrid translation system:
 * - Microsoft Translator (primary, 2M chars/month)
 * - DeepL API (high accuracy, 500k chars/month)
 * - ML Kit (offline fallback, unlimited)
 *
 * Strategy:
 * 1. Check cache first (permanent retention)
 * 2. Try Microsoft if selected and quota available
 * 3. Fallback to DeepL if Microsoft fails/quota exceeded
 * 4. Final fallback to ML Kit
 * 5. Cache all successful translations
 */
@Singleton
class TranslationRepository @Inject constructor(
    private val microsoftTranslatorService: MicrosoftTranslatorService,
    private val deepLApiService: DeepLApiService,
    private val mlKitTranslator: MLKitTranslator,
    private val translationCacheDao: TranslationCacheDao,
    @Named("MicrosoftApiKey") private val microsoftApiKey: String,
    @Named("MicrosoftRegion") private val microsoftRegion: String,
    @Named("DeepLApiKey") private val deepLApiKey: String
) {
    private val TAG = "TranslationRepository"

    // Character usage tracking
    private var microsoftMonthlyChars = 0
    private var deepLMonthlyChars = 0

    private val MICROSOFT_MONTHLY_LIMIT = 2_000_000
    private val DEEPL_MONTHLY_LIMIT = 500_000

    /**
     * Translate text between Japanese and Korean
     *
     * @param text Source text to translate
     * @param sourceLang Source language code ("ja" or "ko")
     * @param targetLang Target language code ("ko" or "ja")
     * @param provider Preferred translation provider
     * @param useCache Enable/disable cache lookup
     * @param fallbackChain Auto-fallback providers in order
     * @return Translated text
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "ko",
        provider: TranslationProvider = TranslationProvider.MICROSOFT,
        useCache: Boolean = true,
        fallbackChain: List<TranslationProvider> = listOf(TranslationProvider.DEEP_L, TranslationProvider.ML_KIT)
    ): TranslationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            if (text.isBlank()) {
                return@withContext TranslationResult.Error("번역할 텍스트가 비어 있습니다")
            }

            Log.d(TAG, "Translating $sourceLang→$targetLang (${text.length} chars) with provider: $provider, cache: $useCache")

            // 1. Check cache first
            if (useCache) {
                val cached = translationCacheDao.getTranslation(
                    sourceText = text,
                    sourceLang = sourceLang,
                    targetLang = targetLang
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

            // 2. Try preferred provider with fallback chain
            var result: TranslationResult? = null
            val providersToTry = listOf(provider) + fallbackChain.filter { it != provider }

            for (currentProvider in providersToTry) {
                result = when (currentProvider) {
                    TranslationProvider.MICROSOFT -> translateWithMicrosoft(text, sourceLang, targetLang)
                    TranslationProvider.DEEP_L -> translateWithDeepL(text, sourceLang, targetLang)
                    TranslationProvider.ML_KIT -> translateWithMLKit(text, sourceLang, targetLang)
                }

                if (result is TranslationResult.Success) {
                    Log.d(TAG, "Translation succeeded with provider: $currentProvider")
                    break
                } else {
                    Log.w(TAG, "Provider $currentProvider failed, trying next...")
                }
            }

            // If all providers failed, return the last error
            if (result !is TranslationResult.Success) {
                return@withContext result ?: TranslationResult.Error("모든 번역 제공자가 실패했습니다")
            }

            // 3. Cache successful translation
            if (useCache) {
                try {
                    translationCacheDao.cacheTranslation(
                        TranslationCacheEntity(
                            sourceText = text,
                            translatedText = result.translatedText,
                            provider = result.provider.name.lowercase(),
                            timestamp = System.currentTimeMillis(),
                            sourceLang = sourceLang,
                            targetLang = targetLang
                        )
                    )
                    Log.d(TAG, "Translation cached (${result.provider})")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cache translation", e)
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Translation completed: ${result.provider} (${elapsed}ms)")

            result

        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "Translation failed after ${elapsed}ms", e)
            TranslationResult.Error(e.message ?: "번역 실패")
        }
    }

    /**
     * Translate using Microsoft Translator API
     */
    private suspend fun translateWithMicrosoft(
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "ko"
    ): TranslationResult {
        return try {
            // Check quota
            if (microsoftMonthlyChars + text.length > MICROSOFT_MONTHLY_LIMIT) {
                Log.w(TAG, "Microsoft quota exceeded ($microsoftMonthlyChars/$MICROSOFT_MONTHLY_LIMIT chars)")
                return TranslationResult.Error("Microsoft 월간 한도 초과 (200만자)")
            }

            // Check API key
            if (microsoftApiKey.isBlank()) {
                Log.w(TAG, "Microsoft API key not configured")
                return TranslationResult.Error("Microsoft API 키가 설정되지 않았습니다")
            }

            Log.d(TAG, "Calling Microsoft Translator API ($sourceLang→$targetLang)...")
            val startTime = System.currentTimeMillis()

            val response = microsoftTranslatorService.translate(
                subscriptionKey = microsoftApiKey,
                region = microsoftRegion,
                from = sourceLang,
                to = targetLang,
                texts = listOf(MicrosoftTranslateRequest(text))
            )

            val translatedText = response.firstOrNull()?.translations?.firstOrNull()?.text

            if (translatedText.isNullOrBlank()) {
                throw Exception("Microsoft API returned empty translation")
            }

            // Update usage counter
            microsoftMonthlyChars += text.length

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Microsoft success: '$translatedText' (${elapsed}ms, $microsoftMonthlyChars/$MICROSOFT_MONTHLY_LIMIT chars used)")

            TranslationResult.Success(
                translatedText = translatedText,
                provider = TranslationProvider.MICROSOFT,
                fromCache = false,
                elapsed = elapsed
            )

        } catch (e: Exception) {
            Log.e(TAG, "Microsoft API failed", e)
            TranslationResult.Error("Microsoft 번역 실패: ${e.message}")
        }
    }

    /**
     * Translate using DeepL API
     */
    private suspend fun translateWithDeepL(
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "ko"
    ): TranslationResult {
        return try {
            // Check quota
            if (deepLMonthlyChars + text.length > DEEPL_MONTHLY_LIMIT) {
                Log.w(TAG, "DeepL quota exceeded ($deepLMonthlyChars/$DEEPL_MONTHLY_LIMIT chars)")
                return TranslationResult.Error("DeepL 월간 한도 초과 (50만자)")
            }

            // Check API key
            if (deepLApiKey.isBlank()) {
                Log.w(TAG, "DeepL API key not configured")
                return TranslationResult.Error("DeepL API 키가 설정되지 않았습니다")
            }

            Log.d(TAG, "Calling DeepL API ($sourceLang→$targetLang)...")
            val startTime = System.currentTimeMillis()

            val response = deepLApiService.translate(
                authorization = "DeepL-Auth-Key $deepLApiKey",
                request = DeepLRequest(
                    text = listOf(text),
                    sourceLang = sourceLang.uppercase(),  // DeepL uses uppercase language codes
                    targetLang = targetLang.uppercase()
                )
            )

            val translatedText = response.translations.firstOrNull()?.text

            if (translatedText.isNullOrBlank()) {
                throw Exception("DeepL API returned empty translation")
            }

            // Update usage counter
            deepLMonthlyChars += text.length

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "DeepL success: '$translatedText' (${elapsed}ms, $deepLMonthlyChars/$DEEPL_MONTHLY_LIMIT chars used)")

            TranslationResult.Success(
                translatedText = translatedText,
                provider = TranslationProvider.DEEP_L,
                fromCache = false,
                elapsed = elapsed
            )

        } catch (e: Exception) {
            Log.e(TAG, "DeepL API failed", e)
            TranslationResult.Error("DeepL 번역 실패: ${e.message}")
        }
    }

    /**
     * Translate using ML Kit (on-device)
     */
    private suspend fun translateWithMLKit(
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "ko"
    ): TranslationResult {
        return try {
            // ML Kit currently only supports ja→ko (hardcoded in MLKitTranslator)
            if (sourceLang != "ja" || targetLang != "ko") {
                Log.w(TAG, "ML Kit only supports ja→ko translation, requested: $sourceLang→$targetLang")
                return TranslationResult.Error("ML Kit는 일본어→한국어만 지원합니다")
            }

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
     */
    suspend fun cleanOldCache() = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
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
                estimatedSizeKB = totalCount * 1
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
     * Get quota usage for all providers
     */
    fun getQuotaUsage(): QuotaUsage {
        return QuotaUsage(
            microsoft = ProviderQuota(
                charactersUsed = microsoftMonthlyChars,
                monthlyLimit = MICROSOFT_MONTHLY_LIMIT,
                percentageUsed = (microsoftMonthlyChars * 100f / MICROSOFT_MONTHLY_LIMIT).toInt()
            ),
            deepL = ProviderQuota(
                charactersUsed = deepLMonthlyChars,
                monthlyLimit = DEEPL_MONTHLY_LIMIT,
                percentageUsed = (deepLMonthlyChars * 100f / DEEPL_MONTHLY_LIMIT).toInt()
            )
        )
    }

    /**
     * Reset monthly usage counters (call at start of each month)
     */
    fun resetMonthlyUsage() {
        microsoftMonthlyChars = 0
        deepLMonthlyChars = 0
        Log.d(TAG, "Monthly usage counters reset")
    }

    /**
     * Initialize ML Kit translator
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
 * Quota usage for all providers
 */
data class QuotaUsage(
    val microsoft: ProviderQuota,
    val deepL: ProviderQuota
)

/**
 * Individual provider quota info
 */
data class ProviderQuota(
    val charactersUsed: Int,
    val monthlyLimit: Int,
    val percentageUsed: Int
)
