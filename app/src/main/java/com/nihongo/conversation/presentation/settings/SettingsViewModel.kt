package com.nihongo.conversation.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.cache.CacheManager
import com.nihongo.conversation.core.cache.CacheSize
import com.nihongo.conversation.core.translation.DownloadProgress
import com.nihongo.conversation.core.translation.MLKitTranslator
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.domain.model.UserSettings
import com.nihongo.conversation.domain.model.TextSizePreference
import com.nihongo.conversation.domain.model.ContrastMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val mlKitTranslator: MLKitTranslator,
    private val cacheManager: CacheManager
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = settingsDataStore.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _translationModelState = MutableStateFlow<TranslationModelState>(TranslationModelState.Loading)
    val translationModelState: StateFlow<TranslationModelState> = _translationModelState.asStateFlow()

    private val _cacheSize = MutableStateFlow<CacheSize>(CacheSize())
    val cacheSize: StateFlow<CacheSize> = _cacheSize.asStateFlow()

    private val _cacheCleanupState = MutableStateFlow<CacheCleanupState>(CacheCleanupState.Idle)
    val cacheCleanupState: StateFlow<CacheCleanupState> = _cacheCleanupState.asStateFlow()

    init {
        checkModelStatus()
        loadCacheSize()
    }

    private fun checkModelStatus() {
        viewModelScope.launch {
            try {
                val isDownloaded = mlKitTranslator.isModelDownloaded()
                _translationModelState.value = if (isDownloaded) {
                    TranslationModelState.Downloaded(mlKitTranslator.getModelSize())
                } else {
                    TranslationModelState.NotDownloaded
                }
            } catch (e: Exception) {
                _translationModelState.value = TranslationModelState.Error(e.message ?: "모델 상태 확인 실패")
            }
        }
    }

    fun downloadTranslationModel() {
        viewModelScope.launch {
            mlKitTranslator.downloadModel().collect { progress ->
                _translationModelState.value = when (progress) {
                    is DownloadProgress.Started -> TranslationModelState.Downloading(0f)
                    is DownloadProgress.Completed -> {
                        TranslationModelState.Downloaded(mlKitTranslator.getModelSize())
                    }
                    is DownloadProgress.Failed -> TranslationModelState.Error(progress.error)
                }
            }
        }
    }

    fun deleteTranslationModel() {
        viewModelScope.launch {
            try {
                _translationModelState.value = TranslationModelState.Loading
                mlKitTranslator.deleteModel()
                _translationModelState.value = TranslationModelState.NotDownloaded
            } catch (e: Exception) {
                _translationModelState.value = TranslationModelState.Error(e.message ?: "모델 삭제 실패")
            }
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

    fun updateTextSize(textSize: TextSizePreference) {
        viewModelScope.launch {
            settingsDataStore.updateTextSize(textSize)
        }
    }

    fun updateContrastMode(contrastMode: ContrastMode) {
        viewModelScope.launch {
            settingsDataStore.updateContrastMode(contrastMode)
        }
    }

    fun loadCacheSize() {
        viewModelScope.launch {
            try {
                val size = cacheManager.getCacheSize()
                _cacheSize.value = size
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    fun clearAllCaches() {
        viewModelScope.launch {
            try {
                _cacheCleanupState.value = CacheCleanupState.Cleaning
                val success = cacheManager.clearAllCaches()
                _cacheCleanupState.value = if (success) {
                    loadCacheSize()  // Refresh cache size
                    CacheCleanupState.Success
                } else {
                    CacheCleanupState.Error("캐시 정리 실패")
                }
            } catch (e: Exception) {
                _cacheCleanupState.value = CacheCleanupState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun resetCacheCleanupState() {
        _cacheCleanupState.value = CacheCleanupState.Idle
    }
}

/**
 * State of cache cleanup operation
 */
sealed class CacheCleanupState {
    object Idle : CacheCleanupState()
    object Cleaning : CacheCleanupState()
    object Success : CacheCleanupState()
    data class Error(val message: String) : CacheCleanupState()
}

/**
 * State of the translation model
 */
sealed class TranslationModelState {
    object Loading : TranslationModelState()
    object NotDownloaded : TranslationModelState()
    data class Downloading(val progress: Float) : TranslationModelState()
    data class Downloaded(val size: String) : TranslationModelState()
    data class Error(val message: String) : TranslationModelState()
}
