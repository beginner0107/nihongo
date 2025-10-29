package com.nihongo.conversation.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.data.repository.ConversationRepository
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.Scenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ConversationItem(
    val conversation: Conversation,
    val scenario: Scenario?,
    val messageCount: Int,
    val lastMessage: String?,
    val duration: Long // milliseconds
)

enum class ConversationFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

data class ConversationHistoryUiState(
    val conversations: List<ConversationItem> = emptyList(),
    val filteredConversations: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: ConversationFilter = ConversationFilter.ALL,
    val selectedScenarioId: Long? = null, // null = all scenarios
    val availableScenarios: List<Scenario> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val conversationToDelete: Conversation? = null
)

@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationHistoryUiState())
    val uiState: StateFlow<ConversationHistoryUiState> = _uiState.asStateFlow()

    private val userId = 1L // TODO: Get from user session

    init {
        loadConversations()
        loadScenarios()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                repository.getUserConversations(userId).collect { conversations ->
                    val conversationItems = conversations.map { conversation ->
                        val messages = repository.getMessages(conversation.id).first()
                        val scenario = repository.getScenario(conversation.scenarioId).first()

                        ConversationItem(
                            conversation = conversation,
                            scenario = scenario,
                            messageCount = messages.size,
                            lastMessage = messages.lastOrNull()?.content,
                            duration = conversation.updatedAt - conversation.createdAt
                        )
                    }

                    _uiState.update {
                        it.copy(
                            conversations = conversationItems,
                            filteredConversations = applyFilters(
                                conversationItems,
                                it.searchQuery,
                                it.selectedFilter,
                                it.selectedScenarioId
                            ),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "会話履歴の読み込みに失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadScenarios() {
        viewModelScope.launch {
            repository.getAllScenarios().collect { scenarios ->
                _uiState.update { it.copy(availableScenarios = scenarios) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredConversations = applyFilters(
                    it.conversations,
                    query,
                    it.selectedFilter,
                    it.selectedScenarioId
                )
            )
        }
    }

    fun onFilterChange(filter: ConversationFilter) {
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                filteredConversations = applyFilters(
                    it.conversations,
                    it.searchQuery,
                    filter,
                    it.selectedScenarioId
                )
            )
        }
    }

    fun onScenarioFilterChange(scenarioId: Long?) {
        _uiState.update {
            it.copy(
                selectedScenarioId = scenarioId,
                filteredConversations = applyFilters(
                    it.conversations,
                    it.searchQuery,
                    it.selectedFilter,
                    scenarioId
                )
            )
        }
    }

    private fun applyFilters(
        conversations: List<ConversationItem>,
        query: String,
        filter: ConversationFilter,
        scenarioId: Long?
    ): List<ConversationItem> {
        var filtered = conversations

        // Apply completion status filter
        filtered = when (filter) {
            ConversationFilter.ALL -> filtered
            ConversationFilter.ACTIVE -> filtered.filter { !it.conversation.isCompleted }
            ConversationFilter.COMPLETED -> filtered.filter { it.conversation.isCompleted }
        }

        // Apply scenario filter
        if (scenarioId != null) {
            filtered = filtered.filter { it.conversation.scenarioId == scenarioId }
        }

        // Apply search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { item ->
                item.scenario?.title?.lowercase()?.contains(lowerQuery) == true ||
                item.lastMessage?.lowercase()?.contains(lowerQuery) == true
            }
        }

        // Sort by most recent first
        return filtered.sortedByDescending { it.conversation.updatedAt }
    }


    fun showDeleteDialog(conversation: Conversation) {
        _uiState.update {
            it.copy(
                showDeleteDialog = true,
                conversationToDelete = conversation
            )
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                conversationToDelete = null
            )
        }
    }

    fun deleteConversation() {
        viewModelScope.launch {
            val conversation = _uiState.value.conversationToDelete ?: return@launch

            try {
                repository.completeConversation(conversation.id)
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        conversationToDelete = null
                    )
                }
                // Reload to reflect changes
                loadConversations()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "会話の削除に失敗しました: ${e.message}",
                        showDeleteDialog = false,
                        conversationToDelete = null
                    )
                }
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
        val dateYear = calendar.get(Calendar.YEAR)

        return when {
            dateYear == todayYear && dateDay == today -> {
                "今日 ${SimpleDateFormat("HH:mm", Locale.JAPANESE).format(Date(timestamp))}"
            }
            dateYear == todayYear && dateDay == today - 1 -> {
                "昨日 ${SimpleDateFormat("HH:mm", Locale.JAPANESE).format(Date(timestamp))}"
            }
            dateYear == todayYear -> {
                SimpleDateFormat("MM月dd日 HH:mm", Locale.JAPANESE).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("yyyy年MM月dd日", Locale.JAPANESE).format(Date(timestamp))
            }
        }
    }

    fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / (60 * 1000)
        return when {
            minutes < 1 -> "1分未満"
            minutes < 60 -> "${minutes}分"
            else -> "${minutes / 60}時間${minutes % 60}分"
        }
    }
}
