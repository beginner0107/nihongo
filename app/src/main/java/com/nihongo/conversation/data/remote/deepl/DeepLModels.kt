package com.nihongo.conversation.data.remote.deepl

import com.google.gson.annotations.SerializedName

/**
 * DeepL API Request
 * https://www.deepl.com/docs-api/translate-text/
 */
data class DeepLRequest(
    @SerializedName("text")
    val text: List<String>,

    @SerializedName("source_lang")
    val sourceLang: String = "JA",

    @SerializedName("target_lang")
    val targetLang: String = "KO"
)

/**
 * DeepL API Response
 */
data class DeepLResponse(
    @SerializedName("translations")
    val translations: List<Translation>
)

data class Translation(
    @SerializedName("detected_source_language")
    val detectedSourceLanguage: String,

    @SerializedName("text")
    val text: String
)

/**
 * DeepL API Error Response
 */
data class DeepLErrorResponse(
    @SerializedName("message")
    val message: String
)

/**
 * Translation Provider enum
 */
enum class TranslationProvider {
    ML_KIT,      // 오프라인, 빠름, 무료
    MICROSOFT,   // 온라인, 빠름, 월 200만자 무료
    DEEP_L       // 온라인, 정확, 월 50만자 무료
}
