package com.nihongo.conversation.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.offlineDataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_data")

/**
 * Offline support manager for caching responses and queueing messages
 */
@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val dataStore = context.offlineDataStore

    companion object {
        private val CACHED_RESPONSES = stringPreferencesKey("cached_responses")
        private val PENDING_MESSAGES = stringPreferencesKey("pending_messages")
        private val COMMON_PHRASES = stringPreferencesKey("common_phrases")
        private const val MAX_CACHED_RESPONSES = 50
        private const val MAX_PENDING_MESSAGES = 20
    }

    /**
     * Data classes for offline storage
     */
    data class CachedResponse(
        val key: String,
        val response: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class PendingMessage(
        val conversationId: Long,
        val userMessage: String,
        val systemPrompt: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class CommonPhrase(
        val japanese: String,
        val korean: String,
        val category: String
    )

    /**
     * Cache a response for offline access
     */
    suspend fun cacheResponse(key: String, response: String) {
        dataStore.edit { prefs ->
            val cached = getCachedResponsesInternal(prefs).toMutableList()

            // Remove duplicate key if exists
            cached.removeAll { it.key == key }

            // Add new response
            cached.add(CachedResponse(key, response))

            // Keep only last N responses
            val limited = if (cached.size > MAX_CACHED_RESPONSES) {
                cached.sortedByDescending { it.timestamp }.take(MAX_CACHED_RESPONSES)
            } else {
                cached
            }

            prefs[CACHED_RESPONSES] = gson.toJson(limited)
        }
    }

    /**
     * Get cached response if available
     */
    suspend fun getCachedResponse(key: String): String? {
        val cached = dataStore.data.first()
        return getCachedResponsesInternal(cached).find { it.key == key }?.response
    }

    /**
     * Get all cached responses
     */
    fun getAllCachedResponses(): Flow<List<CachedResponse>> {
        return dataStore.data.map { prefs ->
            getCachedResponsesInternal(prefs)
        }
    }

    /**
     * Queue a message for sending when online
     */
    suspend fun queueMessage(
        conversationId: Long,
        userMessage: String,
        systemPrompt: String
    ) {
        dataStore.edit { prefs ->
            val pending = getPendingMessagesInternal(prefs).toMutableList()

            pending.add(
                PendingMessage(
                    conversationId = conversationId,
                    userMessage = userMessage,
                    systemPrompt = systemPrompt
                )
            )

            // Keep only last N messages
            val limited = if (pending.size > MAX_PENDING_MESSAGES) {
                pending.takeLast(MAX_PENDING_MESSAGES)
            } else {
                pending
            }

            prefs[PENDING_MESSAGES] = gson.toJson(limited)
        }
    }

    /**
     * Get all pending messages
     */
    suspend fun getPendingMessages(): List<PendingMessage> {
        val prefs = dataStore.data.first()
        return getPendingMessagesInternal(prefs)
    }

    /**
     * Remove a pending message after successful send
     */
    suspend fun removePendingMessage(pendingMessage: PendingMessage) {
        dataStore.edit { prefs ->
            val pending = getPendingMessagesInternal(prefs).toMutableList()
            pending.remove(pendingMessage)
            prefs[PENDING_MESSAGES] = gson.toJson(pending)
        }
    }

    /**
     * Clear all pending messages
     */
    suspend fun clearPendingMessages() {
        dataStore.edit { prefs ->
            prefs[PENDING_MESSAGES] = gson.toJson(emptyList<PendingMessage>())
        }
    }

    /**
     * Store common phrases for offline use
     */
    suspend fun storeCommonPhrases(phrases: List<CommonPhrase>) {
        dataStore.edit { prefs ->
            prefs[COMMON_PHRASES] = gson.toJson(phrases)
        }
    }

    /**
     * Get common phrases
     */
    suspend fun getCommonPhrases(): List<CommonPhrase> {
        val prefs = dataStore.data.first()
        val json = prefs[COMMON_PHRASES] ?: return emptyList()

        return try {
            val type = object : TypeToken<List<CommonPhrase>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search common phrases by Japanese text
     */
    suspend fun searchCommonPhrases(query: String): List<CommonPhrase> {
        return getCommonPhrases().filter {
            it.japanese.contains(query, ignoreCase = true) ||
            it.korean.contains(query, ignoreCase = true)
        }
    }

    /**
     * Clear all cached data
     */
    suspend fun clearCache() {
        dataStore.edit { prefs ->
            prefs[CACHED_RESPONSES] = gson.toJson(emptyList<CachedResponse>())
        }
    }

    /**
     * Get cache size in bytes (approximate)
     */
    suspend fun getCacheSize(): Long {
        val prefs = dataStore.data.first()
        val cachedJson = prefs[CACHED_RESPONSES] ?: ""
        val pendingJson = prefs[PENDING_MESSAGES] ?: ""
        val phrasesJson = prefs[COMMON_PHRASES] ?: ""

        return (cachedJson.length + pendingJson.length + phrasesJson.length).toLong()
    }

    // Internal helper methods
    private fun getCachedResponsesInternal(prefs: Preferences): List<CachedResponse> {
        val json = prefs[CACHED_RESPONSES] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CachedResponse>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getPendingMessagesInternal(prefs: Preferences): List<PendingMessage> {
        val json = prefs[PENDING_MESSAGES] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
