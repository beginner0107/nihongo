package com.nihongo.conversation.data.remote.microsoft

import com.google.gson.annotations.SerializedName

/**
 * Microsoft Translator Text API Models
 *
 * API Documentation: https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-translate
 *
 * Free Tier (F0):
 * - 2,000,000 characters/month
 * - No credit card required
 * - Korea Central region
 */

/**
 * Translation request body
 *
 * IMPORTANT: Microsoft API expects uppercase "Text" field
 */
data class MicrosoftTranslateRequest(
    @SerializedName("Text")  // Microsoft API uses uppercase!
    val text: String
)

/**
 * Translation response
 *
 * Microsoft returns an array of objects, each containing a "translations" array.
 * Example response:
 * [
 *   {
 *     "translations": [
 *       {
 *         "text": "번역된 텍스트",
 *         "to": "ko"
 *       }
 *     ]
 *   }
 * ]
 */
data class MicrosoftTranslateResponse(
    @SerializedName("translations")
    val translations: List<Translation>
) {
    data class Translation(
        @SerializedName("text")  // Response uses lowercase
        val text: String,

        @SerializedName("to")
        val to: String
    )
}

/**
 * Error response from Microsoft Translator
 */
data class MicrosoftErrorResponse(
    @SerializedName("error")
    val error: ErrorDetail
) {
    data class ErrorDetail(
        @SerializedName("code")
        val code: String,

        @SerializedName("message")
        val message: String
    )
}
