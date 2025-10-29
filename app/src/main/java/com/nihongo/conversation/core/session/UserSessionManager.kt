package com.nihongo.conversation.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Manages user session state with persistent storage
 *
 * Features:
 * - Stores current userId, userName, userLevel
 * - Reactive Flow-based updates
 * - Auto-login last selected user
 * - Multi-user support
 */
@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_LEVEL = intPreferencesKey("user_level")
    }

    /**
     * Flow of current user ID (null if no user logged in)
     */
    val currentUserId: Flow<Long?> = context.userSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_ID]
        }

    /**
     * Flow of current user name
     */
    val currentUserName: Flow<String> = context.userSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: "ゲスト"
        }

    /**
     * Flow of current user level (1=Beginner, 2=Intermediate, 3=Advanced)
     */
    val currentUserLevel: Flow<Int> = context.userSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_LEVEL] ?: 1
        }

    /**
     * Set current user session
     *
     * @param userId User ID from database
     * @param userName User's display name
     * @param userLevel User's learning level
     */
    suspend fun setCurrentUser(userId: Long, userName: String, userLevel: Int) {
        context.userSessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_NAME] = userName
            preferences[PreferencesKeys.USER_LEVEL] = userLevel
        }
    }

    /**
     * Update user level (when user progresses)
     */
    suspend fun updateUserLevel(level: Int) {
        context.userSessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_LEVEL] = level
        }
    }

    /**
     * Update user name
     */
    suspend fun updateUserName(name: String) {
        context.userSessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    /**
     * Clear current user session (logout)
     */
    suspend fun clearSession() {
        context.userSessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Get current user ID synchronously
     *
     * @return userId or null if no user logged in
     */
    suspend fun getCurrentUserIdSync(): Long? {
        return currentUserId.first()
    }

    /**
     * Get current user level synchronously
     *
     * @return userLevel (default 1)
     */
    suspend fun getCurrentUserLevelSync(): Int {
        return currentUserLevel.first()
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentUserIdSync() != null
    }
}
