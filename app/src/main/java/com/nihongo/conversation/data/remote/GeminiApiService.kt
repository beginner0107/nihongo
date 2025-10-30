package com.nihongo.conversation.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nihongo.conversation.BuildConfig
import com.nihongo.conversation.core.network.NetworkMonitor
import com.nihongo.conversation.core.network.OfflineManager
import com.nihongo.conversation.domain.model.GrammarComponent
import com.nihongo.conversation.domain.model.GrammarExplanation
import com.nihongo.conversation.domain.model.GrammarType
import com.nihongo.conversation.domain.model.Hint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import com.google.ai.client.generativeai.type.RequestOptions
import kotlin.time.Duration.Companion.seconds

@Singleton
class GeminiApiService @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val offlineManager: OfflineManager
) {

    // Request options with 10 second timeout
    private val requestOptions = RequestOptions(
        timeout = 10.seconds
    )

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        requestOptions = requestOptions
    )

    companion object {
        // Payload optimization limits
        private const val MAX_HISTORY_MESSAGES = 20  // Last N messages only
        private const val MAX_CONTEXT_LENGTH = 2000  // Characters per message
        private const val MAX_SYSTEM_PROMPT_LENGTH = 500  // Truncate long prompts
    }

    // Response cache for common phrases
    private val responseCache = mutableMapOf<String, String>()
    private val commonGreetings = mapOf(
        "こんにちは" to "こんにちは！今日はいい天気ですね。",
        "おはよう" to "おはようございます！よく眠れましたか？",
        "こんばんは" to "こんばんは！今日はどうでしたか？",
        "ありがとう" to "どういたしまして！",
        "さようなら" to "さようなら！またお会いしましょう。"
    )

    /**
     * Initialize common phrases for offline use
     * Should be called when app starts with network connection
     */
    suspend fun initializeOfflineData() {
        val commonPhrases = listOf(
            OfflineManager.CommonPhrase("こんにちは", "안녕하세요", "greeting"),
            OfflineManager.CommonPhrase("おはようございます", "좋은 아침입니다", "greeting"),
            OfflineManager.CommonPhrase("こんばんは", "안녕하세요 (저녁)", "greeting"),
            OfflineManager.CommonPhrase("ありがとうございます", "감사합니다", "gratitude"),
            OfflineManager.CommonPhrase("すみません", "죄송합니다", "apology"),
            OfflineManager.CommonPhrase("お願いします", "부탁합니다", "request"),
            OfflineManager.CommonPhrase("いただきます", "잘 먹겠습니다", "dining"),
            OfflineManager.CommonPhrase("ごちそうさまでした", "잘 먹었습니다", "dining"),
            OfflineManager.CommonPhrase("はい", "네", "response"),
            OfflineManager.CommonPhrase("いいえ", "아니요", "response"),
            OfflineManager.CommonPhrase("わかりました", "알겠습니다", "understanding"),
            OfflineManager.CommonPhrase("もう一度お願いします", "다시 한 번 부탁합니다", "request"),
            OfflineManager.CommonPhrase("ゆっくり話してください", "천천히 말씀해 주세요", "request"),
            OfflineManager.CommonPhrase("これは何ですか", "이것은 무엇입니까", "question"),
            OfflineManager.CommonPhrase("いくらですか", "얼마입니까", "shopping"),
            OfflineManager.CommonPhrase("トイレはどこですか", "화장실은 어디입니까", "location"),
            OfflineManager.CommonPhrase("助けてください", "도와주세요", "emergency"),
            OfflineManager.CommonPhrase("わかりません", "모르겠습니다", "understanding"),
            OfflineManager.CommonPhrase("大丈夫です", "괜찮습니다", "response"),
            OfflineManager.CommonPhrase("またね", "또 만나요", "farewell")
        )

        offlineManager.storeCommonPhrases(commonPhrases)
    }

    suspend fun sendMessage(
        message: String,
        conversationHistory: List<Pair<String, Boolean>>,
        systemPrompt: String
    ): String {
        return try {
            val chat = model.startChat(
                history = buildHistory(conversationHistory, systemPrompt)
            )
            val response = chat.sendMessage(message)
            val rawText = response.text ?: "エラーが発生しました"
            cleanResponseText(rawText)
        } catch (e: Exception) {
            throw Exception("Failed to get response from Gemini: ${e.message}")
        }
    }

    /**
     * Send message with streaming response (typewriter effect)
     * Returns Flow that emits text chunks as they arrive
     * Includes offline support and payload optimization
     */
    fun sendMessageStream(
        message: String,
        conversationHistory: List<Pair<String, Boolean>>,
        systemPrompt: String
    ): Flow<String> = flow {
        try {
            // Check cache for common greetings (instant response)
            val trimmedMessage = message.trim()
            commonGreetings[trimmedMessage]?.let { cachedResponse ->
                emit(cachedResponse)
                offlineManager.cacheResponse("greeting:$trimmedMessage", cachedResponse)
                return@flow
            }

            // Check offline cache
            val cacheKey = "$message|${conversationHistory.size}"
            offlineManager.getCachedResponse(cacheKey)?.let { cachedResponse ->
                emit(cachedResponse)
                return@flow
            }

            // Check memory cache (faster than DataStore)
            responseCache[cacheKey]?.let { cachedResponse ->
                emit(cachedResponse)
                return@flow
            }

            // Check network availability
            if (!networkMonitor.isCurrentlyOnline()) {
                // Search common phrases for offline response
                val commonPhrase = offlineManager.searchCommonPhrases(trimmedMessage)
                    .firstOrNull()

                if (commonPhrase != null) {
                    emit(commonPhrase.japanese)
                    return@flow
                }

                // Emit offline error message
                emit("オフラインです。インターネット接続を確認してください。")
                return@flow
            }

            // Optimize payload: truncate history and system prompt
            val optimizedHistory = optimizeHistory(conversationHistory)
            val optimizedPrompt = optimizeSystemPrompt(systemPrompt)

            // Stream from Gemini API
            val chat = model.startChat(
                history = buildHistory(optimizedHistory, optimizedPrompt)
            )

            var fullResponse = StringBuilder()
            chat.sendMessageStream(message).collect { chunk ->
                val text = chunk.text ?: ""
                fullResponse.append(text)
                emit(cleanResponseText(text))
            }

            // Cache the full response in both caches
            val finalResponse = fullResponse.toString()
            responseCache[cacheKey] = finalResponse
            offlineManager.cacheResponse(cacheKey, finalResponse)

            // Keep memory cache size manageable
            if (responseCache.size > 50) {
                responseCache.remove(responseCache.keys.first())
            }

        } catch (e: Exception) {
            emit("エラーが発生しました: ${e.message}")
        }
    }

    /**
     * Optimize conversation history to reduce payload size
     * - Keep only last N messages
     * - Truncate long messages
     * - Remove unnecessary whitespace
     */
    private fun optimizeHistory(history: List<Pair<String, Boolean>>): List<Pair<String, Boolean>> {
        return history
            .takeLast(MAX_HISTORY_MESSAGES)  // Keep only recent messages
            .map { (text, isUser) ->
                val truncated = if (text.length > MAX_CONTEXT_LENGTH) {
                    text.take(MAX_CONTEXT_LENGTH) + "..."
                } else {
                    text
                }
                truncated.trim() to isUser
            }
    }

    /**
     * Optimize system prompt by removing redundancy and truncating if too long
     * - Remove extra whitespace
     * - Truncate to maximum length
     * - Keep only essential instructions
     */
    private fun optimizeSystemPrompt(prompt: String): String {
        val optimized = prompt
            .replace(Regex("\\s+"), " ")  // Remove extra whitespace
            .replace(Regex("\\n+"), "\n")  // Remove multiple newlines
            .trim()

        // Truncate if exceeds maximum length
        return if (optimized.length > MAX_SYSTEM_PROMPT_LENGTH) {
            // Keep first N characters (most important instructions usually at start)
            optimized.take(MAX_SYSTEM_PROMPT_LENGTH) + "..."
        } else {
            optimized
        }
    }

    /**
     * Batch multiple API requests to reduce network overhead
     * Useful for fetching hints, grammar, and translation together
     */
    suspend fun batchRequests(
        sentence: String,
        conversationContext: List<String>,
        userLevel: Int,
        requestTypes: Set<BatchRequestType>
    ): BatchResponse {
        // Check if online
        if (!networkMonitor.isCurrentlyOnline()) {
            return BatchResponse(
                grammar = null,
                hints = emptyList(),
                translation = null,
                error = "オフラインです"
            )
        }

        // Build combined prompt for all requests
        val prompts = mutableListOf<String>()
        if (BatchRequestType.GRAMMAR in requestTypes) {
            prompts.add("1. 文法分析: $sentence")
        }
        if (BatchRequestType.HINTS in requestTypes) {
            prompts.add("2. ヒント提案 (3つ)")
        }
        if (BatchRequestType.TRANSLATION in requestTypes) {
            prompts.add("3. 韓国語翻訳: $sentence")
        }

        val batchPrompt = """
            以下のリクエストに対して、JSONで回答してください：
            ${prompts.joinToString("\n")}

            会話コンテキスト: ${conversationContext.takeLast(5).joinToString(" | ")}
            ユーザーレベル: $userLevel

            JSON形式：
            {
              "grammar": { grammar explanation object },
              "hints": [ hint objects ],
              "translation": "korean translation"
            }
        """.trimIndent()

        return try {
            val response = model.generateContent(batchPrompt)
            parseBatchResponse(response.text ?: "{}", sentence, conversationContext)
        } catch (e: Exception) {
            BatchResponse(
                grammar = null,
                hints = emptyList(),
                translation = null,
                error = e.message
            )
        }
    }

    enum class BatchRequestType {
        GRAMMAR,
        HINTS,
        TRANSLATION
    }

    data class BatchResponse(
        val grammar: GrammarExplanation?,
        val hints: List<Hint>,
        val translation: String?,
        val error: String? = null
    )

    private fun parseBatchResponse(
        jsonText: String,
        sentence: String,
        conversationContext: List<String>
    ): BatchResponse {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val obj = JSONObject(cleanJson)

            val grammar = if (obj.has("grammar")) {
                parseGrammarExplanationFromJson(obj.getJSONObject("grammar").toString(), sentence)
            } else null

            val hints = if (obj.has("hints")) {
                parseHintsFromJson(obj.getJSONArray("hints").toString())
            } else emptyList()

            val translation = obj.optString("translation", null)

            BatchResponse(grammar, hints, translation)
        } catch (e: Exception) {
            BatchResponse(null, emptyList(), null, e.message)
        }
    }

    /**
     * Clean AI response text by removing markdown formatting and furigana
     */
    private fun cleanResponseText(text: String): String {
        return text
            // Remove markdown bold (**text** or __text__)
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            // Remove markdown italic (*text* or _text_)
            .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
            .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
            // Remove furigana in parentheses (both full-width and half-width)
            .replace(Regex("（[^）]*）"), "")
            .replace(Regex("\\([^)]*\\)"), "")
    }

    suspend fun generateHints(
        conversationContext: String,
        userLevel: Int
    ): List<Hint> {
        return try {
            val prompt = """
                현재 일본어 회화 상황: $conversationContext
                사용자 레벨: $userLevel

                위 상황에서 사용자가 다음에 말할 수 있는 일본어 표현 3개를 제공하세요.
                각 표현에 대해 다음 JSON 형식으로 응답하세요:

                [
                  {
                    "japanese": "일본어 표현",
                    "korean": "한국어 번역",
                    "romaji": "로마자 표기",
                    "explanation": "사용 상황 설명"
                  }
                ]

                응답은 반드시 JSON 배열만 포함하고, 다른 텍스트는 포함하지 마세요.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim() ?: "[]"

            parseHintsFromJson(jsonText)
        } catch (e: Exception) {
            // Fallback hints on error
            listOf(
                Hint(
                    japanese = "すみません",
                    korean = "죄송합니다 / 실례합니다",
                    romaji = "sumimasen",
                    explanation = "사람을 부르거나 사과할 때 사용"
                ),
                Hint(
                    japanese = "お願いします",
                    korean = "부탁합니다",
                    romaji = "onegaishimasu",
                    explanation = "무언가를 요청할 때 사용"
                ),
                Hint(
                    japanese = "ありがとうございます",
                    korean = "감사합니다",
                    romaji = "arigatou gozaimasu",
                    explanation = "감사를 표현할 때 사용"
                )
            )
        }
    }

    suspend fun explainGrammar(
        sentence: String,
        conversationExamples: List<String>,
        userLevel: Int
    ): GrammarExplanation {
        val prompt = """
            다음 일본어 문장의 문법을 한국어로 쉽게 설명해주세요.
            사용자 레벨: $userLevel (1=초급, 2=중급, 3=고급)

            문장: $sentence

            다음 JSON 형식으로 응답하세요:
            {
              "overallExplanation": "문장 전체의 간단한 설명 (1-2문장)",
              "detailedExplanation": "문법 구조에 대한 상세한 설명",
              "components": [
                {
                  "text": "문법 요소 (예: を, ます)",
                  "type": "PARTICLE|VERB|ADJECTIVE|NOUN|AUXILIARY|CONJUNCTION|ADVERB|EXPRESSION",
                  "explanation": "이 문법 요소에 대한 한국어 설명",
                  "startIndex": 시작위치,
                  "endIndex": 끝위치
                }
              ],
              "examples": [
                ${conversationExamples.joinToString(",\n") { "\"$it\"" }}
              ],
              "relatedPatterns": [
                "관련 문법 패턴 1",
                "관련 문법 패턴 2"
              ]
            }

            주의사항:
            - components는 문장의 모든 주요 문법 요소를 포함해야 합니다
            - startIndex와 endIndex는 실제 문자열 위치를 정확히 지정하세요
            - type은 반드시 제시된 타입 중 하나여야 합니다
            - 사용자 레벨에 맞는 쉬운 설명을 제공하세요
            - examples는 대화 내용에서 관련된 문장들입니다

            응답은 반드시 JSON만 포함하고, 다른 텍스트는 포함하지 마세요.
        """.trimIndent()

        return try {
            // Add timeout to prevent hanging
            kotlinx.coroutines.withTimeout(15000) {
                val response = model.generateContent(prompt)

                // Safely get text
                val jsonText = try {
                    response.text?.trim()
                } catch (e: Exception) {
                    // Response blocked by safety filters
                    return@withTimeout GrammarExplanation(
                        originalText = sentence,
                        components = emptyList(),
                        overallExplanation = "문법 분석 차단됨",
                        detailedExplanation = "콘텐츠가 안전 필터에 의해 차단되었습니다.",
                        examples = conversationExamples,
                        relatedPatterns = emptyList()
                    )
                }

                if (!jsonText.isNullOrEmpty() && jsonText != "{}") {
                    parseGrammarExplanationFromJson(jsonText, sentence, conversationExamples)
                } else {
                    GrammarExplanation(
                        originalText = sentence,
                        components = emptyList(),
                        overallExplanation = "문법 분석 결과 없음",
                        detailedExplanation = "응답이 비어있습니다.",
                        examples = conversationExamples,
                        relatedPatterns = emptyList()
                    )
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            GrammarExplanation(
                originalText = sentence,
                components = emptyList(),
                overallExplanation = "요청 시간 초과",
                detailedExplanation = "15초 내에 응답을 받지 못했습니다. 다시 시도해주세요.",
                examples = conversationExamples,
                relatedPatterns = emptyList()
            )
        } catch (e: Exception) {
            GrammarExplanation(
                originalText = sentence,
                components = emptyList(),
                overallExplanation = "문법 분석 실패",
                detailedExplanation = when {
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "API 한도 초과"
                    e.message?.contains("blocked", ignoreCase = true) == true ||
                    e.message?.contains("SAFETY", ignoreCase = true) == true ->
                        "콘텐츠 차단됨"
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "네트워크 오류"
                    else ->
                        "오류: ${e.message?.take(50) ?: "알 수 없음"}"
                },
                examples = conversationExamples,
                relatedPatterns = emptyList()
            )
        }
    }

    private fun parseGrammarExplanationFromJson(
        jsonText: String,
        originalText: String,
        fallbackExamples: List<String> = emptyList()
    ): GrammarExplanation {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val obj = JSONObject(cleanJson)
            val componentsArray = obj.getJSONArray("components")
            val components = mutableListOf<GrammarComponent>()

            for (i in 0 until componentsArray.length()) {
                val compObj = componentsArray.getJSONObject(i)
                components.add(
                    GrammarComponent(
                        text = compObj.getString("text"),
                        type = GrammarType.valueOf(compObj.getString("type")),
                        explanation = compObj.getString("explanation"),
                        startIndex = compObj.getInt("startIndex"),
                        endIndex = compObj.getInt("endIndex")
                    )
                )
            }

            val examplesArray = obj.optJSONArray("examples") ?: JSONArray()
            val examples = mutableListOf<String>()
            for (i in 0 until examplesArray.length()) {
                examples.add(examplesArray.getString(i))
            }

            val relatedArray = obj.optJSONArray("relatedPatterns") ?: JSONArray()
            val relatedPatterns = mutableListOf<String>()
            for (i in 0 until relatedArray.length()) {
                relatedPatterns.add(relatedArray.getString(i))
            }

            GrammarExplanation(
                originalText = originalText,
                components = components,
                overallExplanation = obj.getString("overallExplanation"),
                detailedExplanation = obj.getString("detailedExplanation"),
                examples = examples,
                relatedPatterns = relatedPatterns
            )
        } catch (e: Exception) {
            GrammarExplanation(
                originalText = originalText,
                components = emptyList(),
                overallExplanation = "문법 분석을 파싱하는 중 오류가 발생했습니다.",
                detailedExplanation = e.message ?: "알 수 없는 오류",
                examples = fallbackExamples,
                relatedPatterns = emptyList()
            )
        }
    }

    private fun parseHintsFromJson(jsonText: String): List<Hint> {
        return try {
            // Remove markdown code blocks if present
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val hints = mutableListOf<Hint>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                hints.add(
                    Hint(
                        japanese = obj.getString("japanese"),
                        korean = obj.getString("korean"),
                        romaji = obj.optString("romaji", null),
                        explanation = obj.optString("explanation", null)
                    )
                )
            }

            hints
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun translateToKorean(japaneseText: String): String {
        // Handle empty text
        if (japaneseText.isBlank()) {
            return "번역할 텍스트가 없습니다"
        }

        // Truncate if too long (Gemini has token limits)
        val truncatedText = if (japaneseText.length > 2000) {
            japaneseText.substring(0, 2000) + "..."
        } else {
            japaneseText
        }

        val prompt = """
            다음 일본어 문장을 자연스러운 한국어로 번역해주세요.
            문장: $truncatedText

            주의사항:
            - 번역문만 출력하세요 (설명이나 다른 텍스트 없이)
            - 자연스러운 한국어 표현을 사용하세요
            - 존댓말로 번역하세요
        """.trimIndent()

        return try {
            // Add timeout to prevent hanging
            kotlinx.coroutines.withTimeout(10000) {
                val response = model.generateContent(prompt)

                // Safely get text
                val translatedText = try {
                    response.text?.trim()
                } catch (e: Exception) {
                    // Response blocked by safety filters
                    return@withTimeout "번역 차단됨"
                }

                if (!translatedText.isNullOrEmpty()) {
                    translatedText
                } else {
                    "번역 결과 없음"
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            "요청 시간 초과 (10초)"
        } catch (e: Exception) {
            when {
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "API 한도 초과"
                e.message?.contains("blocked", ignoreCase = true) == true ->
                    "콘텐츠 차단됨"
                e.message?.contains("SAFETY", ignoreCase = true) == true ->
                    "안전 필터링됨"
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "네트워크 오류"
                else ->
                    "번역 실패: ${e.message?.take(50) ?: "알 수 없음"}"
            }
        }
    }

    /**
     * Analyze user message for grammar errors, unnatural expressions, and suggestions
     * Returns JSON array of feedback items
     */
    suspend fun analyzeGrammarAndStyle(
        userMessage: String,
        conversationContext: List<String>,
        userLevel: Int
    ): String {
        return try {
            val prompt = """
                일본어 학습자의 메시지를 분석하고 피드백을 제공하세요.

                사용자 메시지: $userMessage
                사용자 레벨: $userLevel (1=초급, 2=중급, 3=고급)
                대화 컨텍스트: ${conversationContext.takeLast(3).joinToString(" | ")}

                다음 4가지 측면에서 분석하고, 문제가 있는 경우에만 피드백을 제공하세요:

                1. **문법 오류** (GRAMMAR_ERROR):
                   - 조사, 동사 활용, 문장 구조 등의 문법적 오류
                   - severity: ERROR (틀림), WARNING (어색함), INFO (개선 가능)

                2. **부자연스러운 표현** (UNNATURAL):
                   - 한국어를 직역한 것 같은 표현 (직역체)
                   - 일본인이 사용하지 않는 표현
                   - severity: WARNING

                3. **더 나은 표현** (BETTER_EXPRESSION):
                   - 더 자연스럽거나 적절한 대안 표현
                   - 상황에 더 맞는 표현
                   - severity: INFO

                4. **대화 흐름** (CONVERSATION_FLOW):
                   - 문맥상 어색한 응답
                   - 대화 전략 제안
                   - severity: INFO

                5. **경어 레벨** (POLITENESS_LEVEL):
                   - 존댓말/반말 사용이 상황에 맞지 않음
                   - severity: WARNING

                JSON 형식으로 응답하세요 (문제가 없으면 빈 배열):
                [
                  {
                    "type": "GRAMMAR_ERROR|UNNATURAL|BETTER_EXPRESSION|CONVERSATION_FLOW|POLITENESS_LEVEL",
                    "severity": "ERROR|WARNING|INFO",
                    "correctedText": "수정된 문장 (해당되는 경우)",
                    "explanation": "한국어로 설명 (왜 틀렸는지, 왜 어색한지)",
                    "betterExpression": "더 나은 대안 표현 (해당되는 경우)",
                    "additionalNotes": "추가 설명이나 사용 예시 (선택적)",
                    "grammarPattern": "문법 패턴 이름 (예: 助詞の使い方, 敬語, 時制)"
                  }
                ]

                중요 규칙:
                - 완벽한 문장이면 빈 배열 [] 을 반환하세요
                - 사소한 문제는 무시하세요 (학습에 도움되는 것만)
                - 초급자에게는 관대하게, 고급자에게는 엄격하게
                - explanation은 친절하고 이해하기 쉽게 작성하세요
                - JSON만 출력하고 다른 텍스트는 포함하지 마세요
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim() ?: "[]"
        } catch (e: Exception) {
            "[]" // Return empty array on error
        }
    }

    private fun buildHistory(
        history: List<Pair<String, Boolean>>,
        systemPrompt: String
    ) = buildList {
        add(
            content("model") {
                text(systemPrompt)
            }
        )
        history.forEach { (text, isUser) ->
            add(
                content(if (isUser) "user" else "model") {
                    text(text)
                }
            )
        }
    }
}
