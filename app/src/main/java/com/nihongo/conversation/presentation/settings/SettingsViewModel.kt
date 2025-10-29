package com.nihongo.conversation.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = settingsDataStore.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun updateDifficultyLevel(level: Int) {
        viewModelScope.launch {
            settingsDataStore.updateDifficultyLevel(level)
        }
    }

    fun updateSpeechSpeed(speed: Float) {
        viewModelScope.launch {
            settingsDataStore.updateSpeechSpeed(speed)
        }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoSpeak(enabled)
        }
    }

    fun updateShowRomaji(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateShowRomaji(enabled)
        }
    }
}
