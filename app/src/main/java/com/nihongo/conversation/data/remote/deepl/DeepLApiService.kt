package com.nihongo.conversation.data.remote.deepl

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepL API Service Interface
 *
 * API Documentation: https://www.deepl.com/docs-api/translate-text/
 *
 * DeepL API Free 제약사항 (2025-11-01 기준):
 * - 한 달에 500,000자까지 번역
 * - 최대 2개의 API 키 생성 및 관리
 * - 용어집 1개 (일부 언어만)
 */
interface DeepLApiService {

    /**
     * Translate text from Japanese to Korean
     *
     * @param authorization DeepL-Auth-Key {API_KEY}
     * @param request Translation request with text array
     * @return Translation response with translated text
     */
    @POST("v2/translate")
    suspend fun translate(
        @Header("Authorization") authorization: String,
        @Body request: DeepLRequest
    ): DeepLResponse
}
