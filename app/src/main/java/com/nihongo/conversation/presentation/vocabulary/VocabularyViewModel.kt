package com.nihongo.conversation.presentation.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.export.AnkiExporter
import com.nihongo.conversation.data.repository.VocabularyRepository
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.domain.model.VocabularyEntry
import com.nihongo.conversation.domain.model.VocabularyStats
import com.nihongo.conversation.domain.model.ReviewQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val profileRepository: com.nihongo.conversation.data.repository.ProfileRepository,
    private val ankiExporter: AnkiExporter
) : ViewModel() {

    private val currentUser = profileRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ============ State ============

    val vocabularyList: StateFlow<List<VocabularyEntry>> = currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            vocabularyRepository.getAllVocabulary(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val vocabularyStats: StateFlow<VocabularyStats?> = currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            vocabularyRepository.getVocabularyStats(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    // ============ Actions ============

    fun searchVocabulary(query: String) {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
            } else {
                vocabularyRepository.searchVocabulary(userId, query).collect { results ->
                    _uiState.update { it.copy(searchQuery = query, searchResults = results) }
                }
            }
        }
    }

    fun deleteVocabulary(entry: VocabularyEntry) {
        viewModelScope.launch {
            try {
                vocabularyRepository.deleteVocabulary(entry)
                _uiState.update { it.copy(message = "削除しました") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "削除に失敗しました") }
            }
        }
    }

    fun toggleMastered(entry: VocabularyEntry) {
        viewModelScope.launch {
            try {
                val updated = entry.copy(isMastered = !entry.isMastered)
                vocabularyRepository.updateVocabulary(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "更新に失敗しました") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun exportToAnki(vocabulary: List<VocabularyEntry>): File? {
        return try {
            ankiExporter.exportToAnkiDeck(vocabulary)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "エクスポートに失敗しました") }
            null
        }
    }

    fun getExportPreview(vocabulary: List<VocabularyEntry>): String {
        return ankiExporter.generatePreview(vocabulary)
    }
}

data class VocabularyUiState(
    val searchQuery: String = "",
    val searchResults: List<VocabularyEntry> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}
