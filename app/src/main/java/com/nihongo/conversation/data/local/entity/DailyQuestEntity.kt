package com.nihongo.conversation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_quests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["expiresAt"]),
        Index(value = ["isCompleted"])
    ]
)
data class DailyQuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,  // "편의점에서 대화 완료하기"
    val description: String,
    val questType: String,  // MESSAGE_COUNT, SCENARIO_COMPLETE, etc.
    val targetValue: Int,  // 10 메시지, 1 시나리오
    val currentValue: Int = 0,
    val rewardPoints: Int,  // 30 포인트
    val expiresAt: Long,  // 자정 (Epoch milliseconds)
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Quest types available in the system
 */
enum class QuestType {
    MESSAGE_COUNT,        // 10개 메시지 보내기
    SCENARIO_COMPLETE,    // 시나리오 1개 완료
    VOICE_ONLY_SESSION,   // 음성 전용 모드로 대화
    VOCABULARY_REVIEW,    // 플래시카드 20개 복습
    PRONUNCIATION_PRACTICE, // 발음 연습 5회
    GRAMMAR_ANALYSIS,     // 문법 분석 3회 사용
    CONVERSATION_LENGTH,  // 15분 이상 대화
    NEW_SCENARIO          // 새로운 시나리오 시작
}
