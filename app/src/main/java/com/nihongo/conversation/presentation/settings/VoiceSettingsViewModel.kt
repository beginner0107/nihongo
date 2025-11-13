package com.nihongo.conversation.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.voice.StorageStatus
import com.nihongo.conversation.core.voice.VoiceStorageManager
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.data.repository.VoiceRecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val storageManager: VoiceStorageManager,
    private val voiceRecordingRepository: VoiceRecordingRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VoiceSettingsUiState())
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()
    
    fun loadSettings() {
        viewModelScope.launch {
            // Load storage status
            val status = storageManager.getStorageStatus()
            _uiState.update { it.copy(storageStatus = status) }
            
            // Load statistics
            loadStatistics()
            
            // Load preferences
            // TODO: Load from DataStore when implemented
            _uiState.update {
                it.copy(
                    recordingQuality = RecordingQuality.MEDIUM,
                    autoCleanupEnabled = true,
                    autoCleanupDays = 30
                )
            }
        }
    }
    
    private suspend fun loadStatistics() {
        try {
            val bookmarkedFiles = voiceRecordingRepository.getBookmarkedFiles()
            
            // For now, use mock data - in production, query from repository
            _uiState.update {
                it.copy(
                    totalRecordings = 42,
                    totalDuration = "2시간 15분",
                    averageDuration = "3분 12초",
                    bookmarkedCount = bookmarkedFiles.size
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(snackbarMessage = "통계를 불러올 수 없습니다")
            }
        }
    }
    
    fun cleanupOldFiles() {
        viewModelScope.launch {
            try {
                val bookmarkedFiles = voiceRecordingRepository.getBookmarkedFiles()
                val result = storageManager.deleteOldFiles(
                    olderThanDays = _uiState.value.autoCleanupDays.toLong(),
                    bookmarkedFiles = bookmarkedFiles
                )
                
                _uiState.update {
                    it.copy(
                        snackbarMessage = "${result.deletedCount}개 파일 삭제 (${storageManager.formatSize(result.freedBytes)} 확보)"
                    )
                }
                
                // Reload storage status
                val newStatus = storageManager.getStorageStatus()
                _uiState.update { it.copy(storageStatus = newStatus) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "정리 실패: ${e.message}")
                }
            }
        }
    }
    
    fun deleteAllFiles() {
        viewModelScope.launch {
            try {
                val bookmarkedFiles = voiceRecordingRepository.getBookmarkedFiles()
                val result = storageManager.deleteAllFiles(bookmarkedFiles)
                
                _uiState.update {
                    it.copy(
                        snackbarMessage = "${result.deletedCount}개 파일 삭제됨"
                    )
                }
                
                // Reload storage status
                val newStatus = storageManager.getStorageStatus()
                _uiState.update { it.copy(storageStatus = newStatus) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "삭제 실패: ${e.message}")
                }
            }
        }
    }
    
    fun setRecordingQuality(quality: RecordingQuality) {
        viewModelScope.launch {
            _uiState.update { it.copy(recordingQuality = quality) }
            // TODO: Save to DataStore
        }
    }
    
    fun setAutoCleanup(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(autoCleanupEnabled = enabled) }
            // TODO: Save to DataStore
        }
    }
    
    fun setAutoCleanupDays(days: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(autoCleanupDays = days) }
            // TODO: Save to DataStore
        }
    }
    
    fun showDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllDialog = true) }
    }
    
    fun dismissDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllDialog = false) }
    }
    
    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

data class VoiceSettingsUiState(
    val storageStatus: StorageStatus = StorageStatus.Normal(0, 0),
    val recordingQuality: RecordingQuality = RecordingQuality.MEDIUM,
    val autoCleanupEnabled: Boolean = true,
    val autoCleanupDays: Int = 30,
    val totalRecordings: Int = 0,
    val totalDuration: String = "0분",
    val averageDuration: String = "0초",
    val bookmarkedCount: Int = 0,
    val showDeleteAllDialog: Boolean = false,
    val snackbarMessage: String? = null
)
