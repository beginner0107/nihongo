package com.nihongo.conversation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihongo.conversation.data.local.entity.TranslationCacheEntity

/**
 * Translation Cache DAO
 *
 * 캐싱 전략:
 * 1. 번역 요청 전 캐시 확인
 * 2. 캐시 히트: 즉시 반환 (API 호출 없음)
 * 3. 캐시 미스: API 호출 후 결과 캐싱
 * 4. 30일 이상 된 캐시 자동 정리
 */
@Dao
interface TranslationCacheDao {

    /**
     * Get cached translation
     *
     * @param sourceText Original text
     * @param sourceLang Source language code
     * @param targetLang Target language code
     * @return Cached translation or null
     */
    @Query("""
        SELECT * FROM translation_cache
        WHERE sourceText = :sourceText
        AND sourceLang = :sourceLang
        AND targetLang = :targetLang
        LIMIT 1
    """)
    suspend fun getTranslation(
        sourceText: String,
        sourceLang: String = "ja",
        targetLang: String = "ko"
    ): TranslationCacheEntity?

    /**
     * Cache translation result
     *
     * OnConflictStrategy.REPLACE: 동일한 sourceText가 있으면 덮어쓰기
     * (최신 번역으로 업데이트)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheTranslation(translation: TranslationCacheEntity)

    /**
     * Clean old cache entries (older than 30 days)
     *
     * @param cutoffTime Unix timestamp (현재 시간 - 30일)
     * @return Number of deleted entries
     */
    @Query("DELETE FROM translation_cache WHERE timestamp < :cutoffTime")
    suspend fun cleanOldCache(cutoffTime: Long): Int

    /**
     * Get total cache count
     *
     * @return Total number of cached translations
     */
    @Query("SELECT COUNT(*) FROM translation_cache")
    suspend fun getCacheCount(): Int

    /**
     * Clear all cache (for testing or manual cleanup)
     */
    @Query("DELETE FROM translation_cache")
    suspend fun clearAllCache()
}
