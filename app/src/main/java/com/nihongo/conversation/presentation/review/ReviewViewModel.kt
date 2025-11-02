package com.nihongo.conversation.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.conversation.core.session.UserSessionManager
import com.nihongo.conversation.core.voice.VoiceManager
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

data class ConversationStats(
    val totalMessages: Int,
    val userMessages: Int,
    val aiMessages: Int,
    val totalWords: Int,
    val duration: Long // in milliseconds
)

data class ConversationWithDetails(
    val conversation: Conversation,
    val messages: List<Message>,
    val scenario: Scenario?,
    val stats: ConversationStats,
    val isExpanded: Boolean = false
)

data class ConversationGroup(
    val dateHeader: String,
    val conversations: List<ConversationWithDetails>
)

data class ReviewUiState(
    val conversationGroups: List<ConversationGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedConversationIds: Set<Long> = emptySet(),
    val conversationToDelete: ConversationWithDetails? = null,
    val deleteSuccess: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val voiceManager: VoiceManager,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get current user ID from session
                val userId = userSessionManager.getCurrentUserIdSync() ?: 1L

                // Load only completed conversations
                repository.getCompletedConversations(userId).collect { conversations ->
                    val conversationDetails = conversations.map { conversation ->
                        val messages = repository.getMessages(conversation.id).first()
                        val scenario = repository.getScenario(conversation.scenarioId).first()
                        val stats = calculateStats(conversation, messages)

                        ConversationWithDetails(
                            conversation = conversation,
                            messages = messages,
                            scenario = scenario,
                            stats = stats,
                            isExpanded = _uiState.value.expandedConversationIds.contains(conversation.id)
                        )
                    }

                    val grouped = groupConversationsByDate(conversationDetails)
                    _uiState.update {
                        it.copy(
                            conversationGroups = grouped,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "대화 기록을 불러올 수 없습니다: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateStats(conversation: Conversation, messages: List<Message>): ConversationStats {
        val userMessages = messages.count { it.isUser }
        val aiMessages = messages.count { !it.isUser }
        val totalWords = messages.sumOf { message ->
            // Count Japanese characters and words
            message.content.replace(Regex("[\\s\\p{Punct}]+"), " ").split(" ").size
        }
        val duration = conversation.updatedAt - conversation.createdAt

        return ConversationStats(
            totalMessages = messages.size,
            userMessages = userMessages,
            aiMessages = aiMessages,
            totalWords = totalWords,
            duration = duration
        )
    }

    private fun groupConversationsByDate(conversations: List<ConversationWithDetails>): List<ConversationGroup> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val headerFormat = SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN)

        // Group by completion date (updatedAt) instead of creation date
        val grouped = conversations.groupBy { conversation ->
            dateFormat.format(Date(conversation.conversation.updatedAt))
        }.map { (dateKey, convos) ->
            val date = dateFormat.parse(dateKey) ?: Date()
            val header = when {
                isToday(date) -> "오늘 완료"
                isYesterday(date) -> "어제 완료"
                else -> headerFormat.format(date) + " 완료"
            }
            ConversationGroup(
                dateHeader = header,
                conversations = convos.sortedByDescending { it.conversation.updatedAt }
            )
        }.sortedByDescending { group ->
            group.conversations.firstOrNull()?.conversation?.updatedAt ?: 0L
        }

        return grouped
    }

    private fun isToday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.time = date
        val checkDate = calendar.get(Calendar.DAY_OF_YEAR)
        return today == checkDate && calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.time = date
        val checkDate = calendar.get(Calendar.DAY_OF_YEAR)
        return yesterday == checkDate && calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    }

    fun toggleConversationExpanded(conversationId: Long) {
        val currentExpanded = _uiState.value.expandedConversationIds
        val newExpanded = if (currentExpanded.contains(conversationId)) {
            currentExpanded - conversationId
        } else {
            currentExpanded + conversationId
        }

        _uiState.update { state ->
            state.copy(
                expandedConversationIds = newExpanded,
                conversationGroups = state.conversationGroups.map { group ->
                    group.copy(
                        conversations = group.conversations.map { conv ->
                            if (conv.conversation.id == conversationId) {
                                conv.copy(isExpanded = newExpanded.contains(conversationId))
                            } else conv
                        }
                    )
                }
            )
        }
    }

    fun playMessage(text: String) {
        voiceManager.speak(text)
    }

    fun extractImportantPhrases(messages: List<Message>): List<String> {
        // Extract unique Japanese phrases from AI responses
        return messages
            .filter { !it.isUser && it.content.isNotBlank() }
            .flatMap { message ->
                // Simple extraction: sentences ending with common Japanese punctuation
                message.content.split("。", "！", "？")
                    .map { it.trim() }
                    .filter { it.length in 5..30 } // Reasonable phrase length
            }
            .distinct()
            .take(5) // Top 5 phrases per conversation
    }

    fun showDeleteConfirmation(conversationDetails: ConversationWithDetails) {
        _uiState.update { it.copy(conversationToDelete = conversationDetails) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(conversationToDelete = null) }
    }

    fun deleteConversation() {
        val toDelete = _uiState.value.conversationToDelete ?: return

        viewModelScope.launch {
            try {
                repository.deleteConversation(toDelete.conversation.id)
                _uiState.update {
                    it.copy(
                        conversationToDelete = null,
                        deleteSuccess = "会話が削除されました"
                    )
                }
                // Clear success message after 2 seconds
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(deleteSuccess = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        conversationToDelete = null,
                        error = "削除に失敗しました: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
