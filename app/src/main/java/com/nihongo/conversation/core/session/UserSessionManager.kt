package com.nihongo.conversation.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import com.nihongo.conversation.core.difficulty.DifficultyLevel
import com.nihongo.conversation.data.repository.ConversationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

/**
 * Represents a coherent user session state
 *
 * @property id User ID from database (null if not logged in)
 * @property name User's display name
 * @property level User's learning level (1=Beginner, 2=Intermediate, 3=Advanced)
 */
data class UserSession(
    val id: Long?,
    val name: String,
    val level: Int
) {
    val isLoggedIn: Boolean get() = id != null

    val difficultyLevel: DifficultyLevel
        get() = when (level) {
            1 -> DifficultyLevel.BEGINNER
            2 -> DifficultyLevel.INTERMEDIATE
            3 -> DifficultyLevel.ADVANCED
            else -> DifficultyLevel.BEGINNER // Fallback for invalid levels
        }

    companion object {
        val EMPTY = UserSession(id = null, name = "ゲスト", level = 1)
    }
}

/**
 * Represents a recent user entry for quick user switching
 *
 * @property id User ID from database
 * @property name User's display name
 * @property level User's learning level
 * @property lastAccessTime Timestamp of last access (used for LRU ordering)
 */
data class RecentUser(
    val id: Long,
    val name: String,
    val level: Int,
    val lastAccessTime: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Parse from DataStore string format: "id:name:level:timestamp"
         */
        fun fromString(str: String): RecentUser? {
            return try {
                val parts = str.split(":")
                if (parts.size == 4) {
                    RecentUser(
                        id = parts[0].toLong(),
                        name = parts[1],
                        level = parts[2].toInt(),
                        lastAccessTime = parts[3].toLong()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Convert to DataStore string format: "id:name:level:timestamp"
     */
    fun toSerializedString(): String {
        return "$id:$name:$level:$lastAccessTime"
    }
}

/**
 * Manages user session state with persistent storage
 *
 * Features:
 * - Stores current userId, userName, userLevel
 * - Reactive Flow-based updates with distinctUntilChanged
 * - Coherent session state via single UserSession object
 * - Auto-login with default user
 * - Multi-user support
 */
@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ConversationRepository
) {
    companion object {
        private const val TAG = "UserSessionManager"
        private const val DEFAULT_USER_ID = 1L
        private const val MAX_RECENT_USERS = 5  // LRU cache size
    }

    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_LEVEL = intPreferencesKey("user_level")
        val RECENT_USERS = stringPreferencesKey("recent_users")  // Comma-separated list
    }

    /**
     * Primary session Flow - provides coherent, consistent snapshots of user state.
     * Prefer this over individual flows to avoid fragmented recompositions.
     */
    val session: Flow<UserSession> = context.userSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSession(
                id = preferences[PreferencesKeys.USER_ID],
                name = preferences[PreferencesKeys.USER_NAME] ?: "ゲスト",
                level = preferences[PreferencesKeys.USER_LEVEL] ?: 1
            )
        }
        .distinctUntilChanged()

    /**
     * Flow of current user ID (null if no user logged in)
     * @deprecated Prefer using [session] for coherent state
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
        .distinctUntilChanged()

    /**
     * Flow of current user name
     * @deprecated Prefer using [session] for coherent state
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
        .distinctUntilChanged()

    /**
     * Flow of current user level (1=Beginner, 2=Intermediate, 3=Advanced)
     * @deprecated Prefer using [session] for coherent state
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
        .distinctUntilChanged()

    /**
     * Flow of current difficulty level (mapped from user level)
     */
    val currentDifficultyLevel: Flow<DifficultyLevel> = session
        .map { it.difficultyLevel }
        .distinctUntilChanged()

    /**
     * Flow of recent users (LRU cache for quick user switching)
     * Returns list ordered by most recent access
     */
    val recentUsers: Flow<List<RecentUser>> = context.userSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val recentUsersString = preferences[PreferencesKeys.RECENT_USERS] ?: ""
            if (recentUsersString.isEmpty()) {
                emptyList()
            } else {
                recentUsersString.split(",")
                    .mapNotNull { RecentUser.fromString(it) }
                    .sortedByDescending { it.lastAccessTime }  // Most recent first
            }
        }
        .distinctUntilChanged()

    /**
     * Set current user session and add to recent users
     *
     * @param userId User ID from database
     * @param userName User's display name
     * @param addToRecent Whether to add this user to recent users list (default: true)
     */
    suspend fun setCurrentUser(userId: Long, userName: String, addToRecent: Boolean = true) {
        context.userSessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_NAME] = userName
        }

        // Add to recent users for quick switching
        if (addToRecent) {
            addRecentUserInternal(userId, userName)
        }
    }

    /**
     * Add user to recent users LRU cache (internal use)
     */
    private suspend fun addRecentUserInternal(userId: Long, userName: String) {
        context.userSessionDataStore.edit { preferences ->
            val currentRecentUsers = preferences[PreferencesKeys.RECENT_USERS] ?: ""
            val currentList = if (currentRecentUsers.isEmpty()) {
                emptyList()
            } else {
                currentRecentUsers.split(",").mapNotNull { RecentUser.fromString(it) }
            }

            // Remove existing entry for this user (if any)
            val filteredList = currentList.filter { it.id != userId }

            // Add new entry at the front (most recent) - level fixed to 1 (unused)
            val newUser = RecentUser(userId, userName, 1, System.currentTimeMillis())
            val updatedList = listOf(newUser) + filteredList

            // Keep only MAX_RECENT_USERS entries (LRU eviction)
            val finalList = updatedList.take(MAX_RECENT_USERS)

            // Save back to preferences
            val serialized = finalList.joinToString(",") { it.toSerializedString() }
            preferences[PreferencesKeys.RECENT_USERS] = serialized

            Log.d(TAG, "Added to recent users: userId=$userId, name=$userName (total: ${finalList.size})")
        }
    }

    /**
     * Update user level (when user progresses)
     *
     * @param level New level (will be clamped to 1-3)
     */
    suspend fun updateUserLevel(level: Int) {
        val validLevel = level.coerceIn(1, 3)
        context.userSessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_LEVEL] = validLevel
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
     * Get coherent session snapshot synchronously for non-reactive use cases
     *
     * @return Current UserSession snapshot
     */
    suspend fun sessionSync(): UserSession {
        return session.first()
    }

    /**
     * Get current user ID synchronously
     *
     * @return userId or null if no user logged in
     * @deprecated Prefer using [sessionSync] for coherent state
     */
    suspend fun getCurrentUserIdSync(): Long? {
        return currentUserId.first()
    }

    /**
     * Get current user level synchronously
     *
     * @return userLevel (default 1)
     * @deprecated Prefer using [sessionSync] for coherent state
     */
    suspend fun getCurrentUserLevelSync(): Int {
        return currentUserLevel.first()
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return sessionSync().isLoggedIn
    }

    /**
     * Ensure a valid session exists, creating one if necessary.
     * This implements auto-login by setting up a default user session
     * if no session is currently active.
     *
     * Should be called during app initialization (e.g., in NihongoApp)
     * after DataInitializer has run.
     *
     * @return true if session was created, false if session already existed
     */
    suspend fun ensureSessionInitialized(): Boolean {
        val currentSession = sessionSync()

        if (currentSession.isLoggedIn) {
            Log.d(TAG, "Session already initialized: userId=${currentSession.id}, name=${currentSession.name}")
            return false
        }

        // Try to get default user from database
        val defaultUser = repository.getUser(DEFAULT_USER_ID).first()

        if (defaultUser != null) {
            Log.i(TAG, "Auto-login: Setting session to default user (id=$DEFAULT_USER_ID, name=${defaultUser.name})")
            setCurrentUser(
                userId = defaultUser.id,
                userName = defaultUser.name
            )
            return true
        } else {
            Log.w(TAG, "No default user found in database (expected id=$DEFAULT_USER_ID). Session remains uninitialized.")
            return false
        }
    }

    /**
     * Switch to a different user (for multi-user support)
     *
     * This is a convenience method that fetches user from database and sets session.
     * If the user doesn't exist in DB, the operation fails gracefully.
     *
     * @param userId User ID to switch to
     * @return true if switch was successful, false if user not found
     */
    suspend fun switchUser(userId: Long): Boolean {
        val user = repository.getUser(userId).first()

        return if (user != null) {
            Log.i(TAG, "Switching to user: id=$userId, name=${user.name}")
            setCurrentUser(
                userId = user.id,
                userName = user.name,
                addToRecent = true
            )
            true
        } else {
            Log.w(TAG, "Cannot switch to user id=$userId: user not found in database")
            false
        }
    }

    /**
     * Get recent users list synchronously
     *
     * @return List of recent users, ordered by most recent access
     */
    suspend fun getRecentUsersSync(): List<RecentUser> {
        return recentUsers.first()
    }

    /**
     * Clear recent users list
     */
    suspend fun clearRecentUsers() {
        context.userSessionDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.RECENT_USERS)
        }
        Log.d(TAG, "Recent users list cleared")
    }
}
