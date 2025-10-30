package com.nihongo.conversation.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nihongo.conversation.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val DIFFICULTY_LEVEL = intPreferencesKey("difficulty_level")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
        val FEEDBACK_ENABLED = booleanPreferencesKey("feedback_enabled")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                difficultyLevel = preferences[PreferencesKeys.DIFFICULTY_LEVEL] ?: 1,
                speechSpeed = preferences[PreferencesKeys.SPEECH_SPEED] ?: 1.0f,
                autoSpeak = preferences[PreferencesKeys.AUTO_SPEAK] ?: true,
                showRomaji = preferences[PreferencesKeys.SHOW_ROMAJI] ?: true,
                feedbackEnabled = preferences[PreferencesKeys.FEEDBACK_ENABLED] ?: true
            )
        }

    suspend fun updateDifficultyLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIFFICULTY_LEVEL] = level
        }
    }

    suspend fun updateSpeechSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_SPEED] = speed
        }
    }

    suspend fun updateAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SPEAK] = enabled
        }
    }

    suspend fun updateShowRomaji(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ROMAJI] = enabled
        }
    }

    suspend fun updateFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FEEDBACK_ENABLED] = enabled
        }
    }
}
