package com.nihongo.conversation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Translation Cache Entity
 *
 * 목적: DeepL API의 월 50만자 한도를 효율적으로 사용하기 위한 캐싱
 *
 * 전략:
 * - 동일한 문장은 한 번만 번역 후 캐시에 저장
 * - 30일 이상 된 캐시는 자동 삭제
 * - ML Kit 번역도 캐싱하여 성능 향상
 */
@Entity(tableName = "translation_cache")
data class TranslationCacheEntity(
    @PrimaryKey
    val sourceText: String,

    val translatedText: String,

    // "mlkit" or "deepl"
    val provider: String,

    // 캐시 생성 시간 (Unix timestamp)
    val timestamp: Long = System.currentTimeMillis(),

    // 소스 언어 코드 (예: "ja")
    val sourceLang: String = "ja",

    // 타겟 언어 코드 (예: "ko")
    val targetLang: String = "ko"
)
