package com.nihongo.conversation.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.ProfileRepository
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val name: String = "",
    val selectedAvatarId: Int = 0,
    val learningGoal: String = "",
    val selectedScenarios: Set<Long> = emptySet(),
    val nativeLanguage: String = "Korean",
    val bio: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Available scenarios for selection
    val availableScenarios = conversationRepository.getAllScenarios()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                profileRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        val favoriteScenarios = user.favoriteScenarios
                            .split(",")
                            .filter { it.isNotBlank() }
                            .mapNotNull { it.toLongOrNull() }
                            .toSet()

                        _uiState.update {
                            it.copy(
                                user = user,
                                name = user.name,
                                selectedAvatarId = user.avatarId,
                                learningGoal = user.learningGoal,
                                selectedScenarios = favoriteScenarios,
                                nativeLanguage = user.nativeLanguage,
                                bio = user.bio,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "プロフィールの読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun selectAvatar(avatarId: Int) {
        _uiState.update { it.copy(selectedAvatarId = avatarId) }
    }

    fun updateLearningGoal(goal: String) {
        _uiState.update { it.copy(learningGoal = goal) }
    }

    fun toggleScenario(scenarioId: Long) {
        val current = _uiState.value.selectedScenarios
        val updated = if (current.contains(scenarioId)) {
            current - scenarioId
        } else {
            current + scenarioId
        }
        _uiState.update { it.copy(selectedScenarios = updated) }
    }

    fun updateNativeLanguage(language: String) {
        _uiState.update { it.copy(nativeLanguage = language) }
    }

    fun updateBio(bio: String) {
        _uiState.update { it.copy(bio = bio) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            try {
                val state = _uiState.value

                // Validate
                if (state.name.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "名前を入力してください"
                        )
                    }
                    return@launch
                }

                // Save profile
                profileRepository.saveProfile(
                    name = state.name,
                    avatarId = state.selectedAvatarId,
                    learningGoal = state.learningGoal,
                    favoriteScenarios = state.selectedScenarios.toList(),
                    nativeLanguage = state.nativeLanguage,
                    bio = state.bio
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "保存に失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
