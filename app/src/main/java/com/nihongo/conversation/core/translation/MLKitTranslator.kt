package com.nihongo.conversation.core.translation

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Translation Service
 * Provides on-device Japanese to Korean translation
 *
 * Features:
 * - Fast translation (1-2 seconds)
 * - Works offline after initial model download (~50MB)
 * - No API quota limitations
 * - Privacy-friendly (data never leaves device)
 */
@Singleton
class MLKitTranslator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "MLKitTranslator"

    // Japanese to Korean translator
    private var translator: Translator? = null

    // Mutex to guard initialization
    private val initMutex = Mutex()

    // Simple in-memory LRU cache (100 entries)
    private val translationCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 100
        }
    }

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.JAPANESE)
        .setTargetLanguage(TranslateLanguage.KOREAN)
        .build()

    private val jaModel = TranslateRemoteModel.Builder(TranslateLanguage.JAPANESE).build()
    private val koModel = TranslateRemoteModel.Builder(TranslateLanguage.KOREAN).build()
    private val modelManager = RemoteModelManager.getInstance()

    /**
     * Check if both translation models (Japanese and Korean) are downloaded
     * Both languages are required for JP→KR translation to work
     */
    suspend fun isModelDownloaded(): Boolean {
        return try {
            val downloadedModels = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            val downloadedLanguages = downloadedModels.map { it.language }.toSet()

            val hasJapanese = TranslateLanguage.JAPANESE in downloadedLanguages
            val hasKorean = TranslateLanguage.KOREAN in downloadedLanguages
            val bothDownloaded = hasJapanese && hasKorean

            Log.d(TAG, "Model status - JP: $hasJapanese, KR: $hasKorean, Both: $bothDownloaded")
            bothDownloaded
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model status", e)
            false
        }
    }

    /**
     * Initialize translator (downloads model if needed)
     * Call this on app start with WiFi connection
     *
     * Thread-safe: Uses mutex to prevent concurrent initialization
     *
     * @param downloadIfNeeded If true, downloads model automatically
     * @return Result indicating success or failure
     */
    suspend fun initialize(downloadIfNeeded: Boolean = true): Result<Unit> = initMutex.withLock {
        return@withLock try {
            // If already initialized, return success
            if (translator != null) {
                Log.d(TAG, "Translator already initialized")
                return@withLock Result.success(Unit)
            }

            Log.d(TAG, "Initializing translator...")

            // Check if model is already downloaded
            val isDownloaded = isModelDownloaded()

            if (!isDownloaded && downloadIfNeeded) {
                Log.d(TAG, "Model not downloaded, starting download...")
                downloadModel()
            } else if (!isDownloaded) {
                Log.w(TAG, "Model not downloaded and auto-download disabled")
                return@withLock Result.failure(Exception("번역 모델이 다운로드되지 않았습니다"))
            }

            // Create translator instance
            translator = Translation.getClient(options)

            // Download model conditions (WiFi preferred)
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            // Ensure model is ready
            translator?.downloadModelIfNeeded(conditions)?.await()

            Log.d(TAG, "Translator initialized successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize translator", e)
            Result.failure(e)
        }
    }

    /**
     * Translate Japanese text to Korean
     *
     * Uses LRU cache to accelerate repeated phrases (100 entries max)
     *
     * @param japaneseText Text to translate
     * @return Translated Korean text
     * @throws Exception if translation fails or model not ready
     */
    suspend fun translate(japaneseText: String): String {
        val startTime = System.currentTimeMillis()

        try {
            if (japaneseText.isBlank()) {
                throw IllegalArgumentException("빈 텍스트는 번역할 수 없습니다")
            }

            // Check cache first
            synchronized(translationCache) {
                translationCache[japaneseText]?.let { cached ->
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Cache hit: '$japaneseText' → '$cached' (${elapsed}ms)")
                    return cached
                }
            }

            // Ensure translator is initialized
            val translatorInstance = translator ?: run {
                Log.w(TAG, "Translator not initialized, initializing now...")
                initialize(downloadIfNeeded = false).getOrThrow()
                translator!!
            }

            Log.d(TAG, "Translating: '$japaneseText' (${japaneseText.length} chars)")

            // Translate using suspendCancellableCoroutine
            val result = suspendCancellableCoroutine<String> { continuation ->
                translatorInstance.translate(japaneseText)
                    .addOnSuccessListener { translatedText ->
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Translation success: '$translatedText' (${elapsed}ms)")

                        // Add to cache
                        synchronized(translationCache) {
                            translationCache[japaneseText] = translatedText
                        }

                        continuation.resume(translatedText)
                    }
                    .addOnFailureListener { exception ->
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Translation failed after ${elapsed}ms", exception)
                        continuation.resumeWithException(exception)
                    }
            }

            return result

        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "Translation error after ${elapsed}ms", e)
            throw TranslationException("ML Kit 번역 실패: ${e.message}", cause = e)
        }
    }

    /**
     * Download the translation model
     * Shows download progress via Flow
     *
     * @return Flow emitting download progress (0.0 to 1.0)
     */
    fun downloadModel(): Flow<DownloadProgress> = callbackFlow {
        Log.d(TAG, "Starting model download...")

        try {
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            val translatorInstance = translator ?: Translation.getClient(options).also {
                translator = it
            }

            // Send initial progress
            trySend(DownloadProgress.Started)

            translatorInstance.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Log.d(TAG, "Model download completed")
                    trySend(DownloadProgress.Completed)
                    close()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Model download failed", exception)
                    trySend(DownloadProgress.Failed(exception.message ?: "다운로드 실패"))
                    close(exception)
                }

            // Note: ML Kit doesn't provide progress callbacks
            // We can only know when it starts and completes
            // For UI, you can show indeterminate progress

        } catch (e: Exception) {
            Log.e(TAG, "Error starting download", e)
            trySend(DownloadProgress.Failed(e.message ?: "다운로드 시작 실패"))
            close(e)
        }

        awaitClose {
            Log.d(TAG, "Download flow closed")
        }
    }

    /**
     * Delete both downloaded models (Japanese and Korean) to free up space (~100MB)
     */
    suspend fun deleteModel(): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting translation models (JP + KR)...")

            // Delete Japanese model
            try {
                modelManager.deleteDownloadedModel(jaModel).await()
                Log.d(TAG, "Japanese model deleted")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete Japanese model (may not exist)", e)
            }

            // Delete Korean model
            try {
                modelManager.deleteDownloadedModel(koModel).await()
                Log.d(TAG, "Korean model deleted")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete Korean model (may not exist)", e)
            }

            // Close translator and clear cache
            translator?.close()
            translator = null

            synchronized(translationCache) {
                translationCache.clear()
            }

            Log.d(TAG, "Models deleted successfully, cache cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete models", e)
            Result.failure(e)
        }
    }

    /**
     * Close the translator and release resources
     */
    fun close() {
        try {
            translator?.close()
            translator = null
            Log.d(TAG, "Translator closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing translator", e)
        }
    }

    /**
     * Get model size information
     */
    fun getModelSize(): String {
        return "약 100MB" // Japanese + Korean models are approximately 100MB total
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        synchronized(translationCache) {
            return "Cache: ${translationCache.size}/100 entries"
        }
    }

    /**
     * Clear translation cache
     */
    fun clearCache() {
        synchronized(translationCache) {
            val size = translationCache.size
            translationCache.clear()
            Log.d(TAG, "Cache cleared: $size entries removed")
        }
    }
}

/**
 * Download progress states
 */
sealed class DownloadProgress {
    object Started : DownloadProgress()
    object Completed : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

/**
 * Custom exception for translation errors
 */
class TranslationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
