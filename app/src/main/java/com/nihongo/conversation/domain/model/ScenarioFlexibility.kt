package com.nihongo.conversation.domain.model

/**
 * 시나리오의 AI 성격 유연성 정의
 * 시나리오 유형에 따라 AI 성격 선택 가능 여부를 결정
 */
enum class ScenarioFlexibility {
    /**
     * 역할 고정 - 성격 선택 불가
     * 예: 편의점 점원, 호텔 프론트, 병원 접수 등 서비스업
     */
    FIXED,

    /**
     * 성격 선택 가능
     * 예: 친구, 동료, 언어 교환 파트너 등 개인적 관계
     */
    FLEXIBLE;

    companion object {
        fun fromString(value: String): ScenarioFlexibility {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                FIXED // 기본값
            }
        }
    }
}