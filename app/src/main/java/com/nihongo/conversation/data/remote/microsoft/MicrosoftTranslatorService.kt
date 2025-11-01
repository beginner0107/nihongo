package com.nihongo.conversation.data.remote.microsoft

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Microsoft Translator Text API Service
 *
 * Endpoint: https://api.cognitive.microsofttranslator.com/
 * Region: Korea Central
 *
 * Free Tier Limits (2025-11-01):
 * - 2,000,000 characters per month
 * - 2,000,000 characters per hour
 * - ~33,300 characters per minute
 *
 * API Key Header: Ocp-Apim-Subscription-Key
 * Region Header: Ocp-Apim-Subscription-Region (koreacentral)
 */
interface MicrosoftTranslatorService {

    /**
     * Translate text from Japanese to Korean
     *
     * @param subscriptionKey Microsoft Translator API key
     * @param region Azure region (e.g., "koreacentral")
     * @param apiVersion API version (default: "3.0")
     * @param from Source language code (default: "ja")
     * @param to Target language code (default: "ko")
     * @param texts List of texts to translate (API supports batch)
     * @return List of translation responses
     */
    @POST("translate")
    suspend fun translate(
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String,
        @Header("Ocp-Apim-Subscription-Region") region: String = "koreacentral",
        @Query("api-version") apiVersion: String = "3.0",
        @Query("from") from: String = "ja",
        @Query("to") to: String = "ko",
        @Body texts: List<MicrosoftTranslateRequest>
    ): List<MicrosoftTranslateResponse>
}
