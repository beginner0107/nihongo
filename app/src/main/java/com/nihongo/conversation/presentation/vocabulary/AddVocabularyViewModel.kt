package com.nihongo.conversation.presentation.vocabulary

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.data.repository.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddVocabularyUiState(
    val word: String = "",
    val reading: String = "",
    val meaning: String = "",
    val exampleSentence: String = "",
    val difficulty: Int = 1,
    val addToReviewQueue: Boolean = true,

    val wordError: String? = null,
    val meaningError: String? = null,

    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    val clipboardText: String? = null
)

@HiltViewModel
class AddVocabularyViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVocabularyUiState())
    val uiState: StateFlow<AddVocabularyUiState> = _uiState.asStateFlow()

    init {
        checkClipboard()
    }

    fun onWordChanged(word: String) {
        _uiState.update {
            it.copy(
                word = word,
                wordError = null
            )
        }
    }

    fun onReadingChanged(reading: String) {
        _uiState.update {
            it.copy(reading = reading)
        }
    }

    fun onMeaningChanged(meaning: String) {
        _uiState.update {
            it.copy(
                meaning = meaning,
                meaningError = null
            )
        }
    }

    fun onExampleSentenceChanged(example: String) {
        _uiState.update {
            it.copy(exampleSentence = example)
        }
    }

    fun onDifficultyChanged(difficulty: Int) {
        _uiState.update {
            it.copy(difficulty = difficulty)
        }
    }

    fun onAddToReviewQueueChanged(add: Boolean) {
        _uiState.update {
            it.copy(addToReviewQueue = add)
        }
    }

    fun checkClipboard() {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboardManager?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text.length < 100) {
                    _uiState.update {
                        it.copy(clipboardText = text)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore clipboard errors
        }
    }

    fun importFromClipboard() {
        _uiState.value.clipboardText?.let { text ->
            // Try to parse clipboard text
            // Format: "word:reading:meaning" or "word:meaning"
            val parts = text.split(":")
            when {
                parts.size >= 3 -> {
                    _uiState.update {
                        it.copy(
                            word = parts[0].trim(),
                            reading = parts[1].trim(),
                            meaning = parts[2].trim(),
                            clipboardText = null
                        )
                    }
                }
                parts.size == 2 -> {
                    _uiState.update {
                        it.copy(
                            word = parts[0].trim(),
                            meaning = parts[1].trim(),
                            clipboardText = null
                        )
                    }
                }
                else -> {
                    // Just put in word field
                    _uiState.update {
                        it.copy(
                            word = text.trim(),
                            clipboardText = null
                        )
                    }
                }
            }
        }
    }

    fun dismissClipboardSuggestion() {
        _uiState.update {
            it.copy(clipboardText = null)
        }
    }

    fun saveVocabulary() {
        val state = _uiState.value

        // Validation
        var hasError = false

        if (state.word.isBlank()) {
            _uiState.update {
                it.copy(wordError = "単語を入力してください")
            }
            hasError = true
        }

        if (state.meaning.isBlank()) {
            _uiState.update {
                it.copy(meaningError = "意味を入力してください")
            }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = userSessionManager.getCurrentUserIdSync() ?: 1L

                vocabularyRepository.addCustomVocabulary(
                    userId = userId,
                    word = state.word.trim(),
                    reading = state.reading.trim().takeIf { it.isNotBlank() },
                    meaning = state.meaning.trim(),
                    exampleSentence = state.exampleSentence.trim().takeIf { it.isNotBlank() },
                    difficulty = state.difficulty,
                    addToReviewQueue = state.addToReviewQueue
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
            } catch (e: IllegalArgumentException) {
                // Duplicate word
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        wordError = e.message ?: "この単語は既に追加されています"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "保存に失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearForm() {
        _uiState.update {
            AddVocabularyUiState(
                clipboardText = it.clipboardText
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    fun resetSavedState() {
        _uiState.update {
            it.copy(isSaved = false)
        }
    }
}
