package com.nihongo.conversation.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.data.local.UserDao
import com.nihongo.conversation.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserSelectionUiState(
    val users: List<User> = emptyList(),
    val currentUserId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateUserDialog: Boolean = false
)

@HiltViewModel
class UserSelectionViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSelectionUiState())
    val uiState: StateFlow<UserSelectionUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
        loadCurrentUser()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userDao.getAllUsers().collect { users ->
                    _uiState.update {
                        it.copy(
                            users = users,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "ユーザーの読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userSessionManager.currentUserId.collect { userId ->
                _uiState.update { it.copy(currentUserId = userId) }
            }
        }
    }

    /**
     * Select a user and set as current session
     */
    fun selectUser(user: User) {
        viewModelScope.launch {
            try {
                userSessionManager.setCurrentUser(
                    userId = user.id,
                    userName = user.name,
                    userLevel = user.level
                )
                _uiState.update { it.copy(currentUserId = user.id) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "ユーザーの選択に失敗しました: ${e.message}")
                }
            }
        }
    }

    /**
     * Create a new user
     */
    fun createUser(name: String, level: Int = 1, avatarId: Int = 0) {
        viewModelScope.launch {
            try {
                val newUser = User(
                    name = name,
                    level = level,
                    avatarId = avatarId,
                    nativeLanguage = "Korean",
                    studyStartDate = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                val userId = userDao.insertUser(newUser)

                // Auto-select newly created user
                userSessionManager.setCurrentUser(
                    userId = userId,
                    userName = name,
                    userLevel = level
                )

                _uiState.update {
                    it.copy(
                        showCreateUserDialog = false,
                        currentUserId = userId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "ユーザーの作成に失敗しました: ${e.message}")
                }
            }
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                userSessionManager.clearSession()
                _uiState.update { it.copy(currentUserId = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "ログアウトに失敗しました: ${e.message}")
                }
            }
        }
    }

    fun showCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = true) }
    }

    fun hideCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
