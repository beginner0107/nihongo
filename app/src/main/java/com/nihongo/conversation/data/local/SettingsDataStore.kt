package com.nihongo.conversation.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nihongo.conversation.domain.model.UserSettings
import com.nihongo.conversation.domain.model.TextSizePreference
import com.nihongo.conversation.domain.model.ContrastMode
import com.nihongo.conversation.domain.model.ThemeMode
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
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SHOW_ROMAJI = booleanPreferencesKey("show_romaji")
        val FEEDBACK_ENABLED = booleanPreferencesKey("feedback_enabled")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val CONTRAST_MODE = stringPreferencesKey("contrast_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
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
                speechSpeed = preferences[PreferencesKeys.SPEECH_SPEED] ?: 1.0f,
                autoSpeak = preferences[PreferencesKeys.AUTO_SPEAK] ?: true,
                showRomaji = preferences[PreferencesKeys.SHOW_ROMAJI] ?: true,
                feedbackEnabled = preferences[PreferencesKeys.FEEDBACK_ENABLED] ?: true,
                textSize = preferences[PreferencesKeys.TEXT_SIZE]?.let {
                    try { TextSizePreference.valueOf(it) }
                    catch (e: Exception) { TextSizePreference.NORMAL }
                } ?: TextSizePreference.NORMAL,
                contrastMode = preferences[PreferencesKeys.CONTRAST_MODE]?.let {
                    try { ContrastMode.valueOf(it) }
                    catch (e: Exception) { ContrastMode.NORMAL }
                } ?: ContrastMode.NORMAL,
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                    try { ThemeMode.valueOf(it) }
                    catch (e: Exception) { ThemeMode.SYSTEM }
                } ?: ThemeMode.SYSTEM
            )
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

    suspend fun updateTextSize(textSize: TextSizePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_SIZE] = textSize.name
        }
    }

    suspend fun updateContrastMode(contrastMode: ContrastMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTRAST_MODE] = contrastMode.name
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }
}
