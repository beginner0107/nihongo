package com.nihongo.conversation.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
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
import java.util.concurrent.ConcurrentHashMap

@Singleton
class GeminiApiService @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val offlineManager: OfflineManager,
    private val memoryManager: com.nihongo.conversation.core.memory.MemoryManager  // Phase 6A
) {

    // Request options with 10 second timeout
    private val requestOptions = RequestOptions(
        timeout = 10.seconds
    )

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Layer 1: Generation config with stop sequences to prevent English explanations
    // This is the most powerful proactive approach - stops AI immediately when trying to generate English
    // Note: Gemini API allows maximum 5 stop sequences
    private val conversationConfig = generationConfig {
        temperature = 0.7f
        topK = 40
        topP = 0.9f
        stopSequences = listOf(
            " which",     // "おいしい" which is... (most common pattern)
            " is a",      // "common" is a... (second most common)
            " that",      // "すぐに" that means...
            "(Polite",    // (Polite and clear...) meta commentary
            "THINK:"      // Internal thinking (covers both INK: and THINK:)
        )
    }

    private val model: GenerativeModel? by lazy {
        if (apiKey.isBlank()) {
            null
        } else {
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                requestOptions = requestOptions,
                generationConfig = conversationConfig
            )
        }
    }

    // Separate model for grammar analysis with longer timeout
    private val grammarModel: GenerativeModel? by lazy {
        if (apiKey.isBlank()) {
            null
        } else {
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                requestOptions = RequestOptions(timeout = 15.seconds),
                generationConfig = conversationConfig  // Apply same config to prevent explanations
            )
        }
    }

    private val missingApiKeyMessage =
        "Gemini APIキーが設定されていません。local.properties に GEMINI_API_KEY を追加してください。"

    companion object {
        // Payload optimization limits
        private const val MAX_HISTORY_MESSAGES = 20  // Last N messages only
        private const val MAX_CONTEXT_LENGTH = 2000  // Characters per message
        private const val MAX_SYSTEM_PROMPT_LENGTH = 500  // Truncate long prompts
    }

    // Response cache for common phrases
    private val responseCache = ConcurrentHashMap<String, String>()
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
        val activeModel = model
            ?: return missingApiKeyMessage

        return try {
            val chat = activeModel.startChat(
                history = buildHistory(conversationHistory, systemPrompt)
            )
            val response = chat.sendMessage(message)
            val rawText = response.text ?: "エラーが発生しました"
            cleanResponseText(rawText)
        } catch (e: Exception) {
            throw Exception("Failed to get response from Gemini", e)
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
        val activeModel = model
        if (activeModel == null) {
            emit(missingApiKeyMessage)
            return@flow
        }

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
                emit(cleanResponseText(cachedResponse))
                return@flow
            }

            // Check memory cache (faster than DataStore)
            responseCache[cacheKey]?.let { cachedResponse ->
                emit(cleanResponseText(cachedResponse))
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
            val chat = activeModel.startChat(
                history = buildHistory(optimizedHistory, optimizedPrompt)
            )

            var fullResponse = StringBuilder()
            chat.sendMessageStream(message).collect { chunk ->
                val text = chunk.text ?: ""
                fullResponse.append(text)
                emit(cleanResponseText(text))
            }

            // Cache the full response (cleaned) in both caches
            val finalResponse = fullResponse.toString()
            val finalCleaned = cleanResponseText(finalResponse)
            responseCache[cacheKey] = finalCleaned
            offlineManager.cacheResponse(cacheKey, finalCleaned)

            // Keep memory cache size manageable
            if (responseCache.size > 50) {
                val firstKey = responseCache.keys.firstOrNull()
                if (firstKey != null) responseCache.remove(firstKey)
            }

        } catch (e: Exception) {
            // Re-throw exception so repository can handle it properly
            throw e
        }
    }

    /**
     * Phase 6A: Optimize conversation history with dynamic limits based on memory pressure
     * - Keep only last N messages (adjusted by memory level)
     * - Truncate long messages
     * - Remove unnecessary whitespace
     */
    private fun optimizeHistory(history: List<Pair<String, Boolean>>): List<Pair<String, Boolean>> {
        // Phase 6A: Dynamic limit based on memory pressure
        val limit = when (memoryManager.memoryLevel.value) {
            com.nihongo.conversation.core.memory.MemoryManager.MemoryLevel.CRITICAL ->
                MAX_HISTORY_MESSAGES / 2  // 10 messages
            com.nihongo.conversation.core.memory.MemoryManager.MemoryLevel.LOW ->
                (MAX_HISTORY_MESSAGES * 0.7).toInt()  // 14 messages
            com.nihongo.conversation.core.memory.MemoryManager.MemoryLevel.NORMAL ->
                MAX_HISTORY_MESSAGES  // 20 messages
        }

        return history
            .takeLast(limit)  // Keep only recent messages
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
            // First collapse multiple newlines but preserve line breaks
            .replace(Regex("\\n+"), "\n")
            // Then collapse runs of spaces/tabs etc. (not newlines)
            .replace(Regex("[ \\t\\u000B\\f\\r]+"), " ")
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
            // Fail fast if API key/model unavailable
            val activeModel = model
            if (activeModel == null) {
                return BatchResponse(
                    grammar = null,
                    hints = emptyList(),
                    translation = null,
                    error = missingApiKeyMessage
                )
            }

            val response = kotlinx.coroutines.withTimeout(10000) {
                activeModel.generateContent(batchPrompt)
            }
            parseBatchResponse(response?.text ?: "{}", sentence, conversationContext)
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
     * Clean AI response text by removing markdown formatting, furigana, and internal reasoning
     * AGGRESSIVE filtering for long conversations where reasoning might slip through
     */
    private fun cleanResponseText(text: String): String {
        var cleaned = text

        // Step 0: AGGRESSIVE - Remove common reasoning keywords and their surrounding text
        val reasoningPatterns = listOf(
            // Thinking markers (INK:, THINK:, etc.)
            Regex("(?i)^\\s*INK:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*THINK:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*THINKING:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*THOUGHT:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*REASON:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*REASONING:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*ANALYSIS:\\s*.*?$", RegexOption.MULTILINE),
            Regex("(?i)^\\s*INTERNAL:\\s*.*?$", RegexOption.MULTILINE),
            // Multi-line thinking blocks
            Regex("(?i)THINK[\\s\\S]*?(?=[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]|$)", RegexOption.MULTILINE),
            // English thinking patterns
            Regex("(?i)^.*?I should.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?I could.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?I will.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?I can.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?Let me.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?Let's.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?This is.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?That's.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?It's.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?Since.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?The user.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?They.*?said.*?$", RegexOption.MULTILINE),
            Regex("(?i)^.*?In this.*?$", RegexOption.MULTILINE),
            // Remove numbered reasoning (1., 2., etc.)
            Regex("^\\d+\\.\\s*[A-Za-z].*?$", RegexOption.MULTILINE),
            // Remove "Response:", "Answer:", etc.
            Regex("(?i)^(Response|Answer|Reply):\\s*", RegexOption.MULTILINE)
        )

        reasoningPatterns.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "")
        }

        // Step 1: Remove internal reasoning blocks (THINK, English explanations, etc.)
        // Find first line that contains Japanese characters
        val japaneseCharPattern = Regex("[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]")
        val lines = cleaned.lines()

        // Find the first line with Japanese content
        val firstJapaneseLineIndex = lines.indexOfFirst { line ->
            japaneseCharPattern.containsMatchIn(line) &&
            // Exclude lines that are primarily English/thinking
            !line.trim().matches(Regex("^[A-Za-z\\s:,\\.!?\"'()\\[\\]{}0-9]+$"))
        }

        // If found, take from that line onwards
        if (firstJapaneseLineIndex >= 0) {
            cleaned = lines.drop(firstJapaneseLineIndex).joinToString("\n")
        }

        // Step 2: Remove remaining English-only lines that might be reasoning
        cleaned = cleaned.lines()
            .filterNot { line ->
                val trimmed = line.trim()
                // Remove lines that are pure English (likely reasoning/thinking)
                trimmed.isNotEmpty() &&
                trimmed.matches(Regex("^[A-Za-z\\s:,\\.!?\"'()\\[\\]{}0-9-]+$")) &&
                !japaneseCharPattern.containsMatchIn(trimmed)
            }
            .joinToString("\n")

        // Step 3: Remove markdown formatting and furigana
        return cleaned
            // Remove markdown bold (**text** or __text__)
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            // Remove markdown italic (*text* or _text_)
            .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
            .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
            // Remove furigana in parentheses (both full-width and half-width)
            .replace(Regex("（[^）]*）"), "")
            .replace(Regex("\\([^)]*\\)"), "")
            // Clean up multiple blank lines
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    suspend fun generateHints(
        conversationContext: String,
        userLevel: Int,
        scenarioPrompt: String = "",
        aiLastMessage: String = ""
    ): List<Hint> {
        return try {
            val prompt = """
                【시나리오 상황】
                $scenarioPrompt

                【최근 대화 내역】
                $conversationContext

                【AI의 마지막 발화】
                "$aiLastMessage"

                【사용자 레벨】$userLevel (1=초급 N5-N4, 2=중급 N3-N2, 3=고급 N1)

                【요청사항】
                AI가 "$aiLastMessage"라고 말했습니다.
                사용자가 이 질문/발화에 자연스럽게 응답할 수 있는 일본어 표현 3개를 제공하세요.

                **중요 규칙**:
                1. AI의 질문 의도에 직접 답하는 표현만 제공 (일반적인 인사말 금지)
                2. 시나리오 상황에 맞는 표현만 제공
                3. 사용자 레벨에 맞는 문법/어휘 사용
                4. 짧고 실용적인 표현 우선 (1-10단어)

                [
                  {
                    "japanese": "일본어 표현",
                    "korean": "한국어 의미",
                    "romaji": "로마자 발음",
                    "explanation": "이 표현을 쓰는 상황 (한 줄)"
                  }
                ]

                JSON만 출력하세요. 다른 설명 금지.
            """.trimIndent()

            val response = model?.generateContent(prompt)
            val jsonText = response?.text?.trim() ?: "[]"

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

    /**
     * Explain Japanese grammar using Kotori morphological analyzer
     *
     * Plan A Implementation: Replaced Gemini API with Kotori for:
     * - 50x faster analysis (5000ms+ → <100ms)
     * - 100% offline capability
     * - Accurate morphological analysis (MeCab IPADIC)
     * - Zero Gemini API quota usage for grammar analysis
     *
     * @param sentence Japanese sentence to analyze
     * @param conversationExamples Unused (kept for API compatibility)
     * @param userLevel User JLPT level (1=N5/N4, 2=N3/N2, 3=N1)
     * @return GrammarExplanation with morphological components
     */
    suspend fun explainGrammar(
        sentence: String,
        conversationExamples: List<String>,
        userLevel: Int
    ): GrammarExplanation {
        android.util.Log.d("GrammarAPI", "=== Kotori Grammar Analysis START ===")
        android.util.Log.d("GrammarAPI", "Sentence: '$sentence'")
        android.util.Log.d("GrammarAPI", "User level: $userLevel")

        // Use Kuromoji for fast, accurate morphological analysis
        return com.nihongo.conversation.core.grammar.KuromojiGrammarAnalyzer.analyzeSentence(
            sentence = sentence,
            userLevel = userLevel
        )
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
                        type = parseGrammarType(compObj.getString("type")),
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
        val startTime = System.currentTimeMillis()
        android.util.Log.d("Translation", "=== Translation Request Start ===")
        android.util.Log.d("Translation", "Input text: '$japaneseText'")
        android.util.Log.d("Translation", "Input length: ${japaneseText.length}")

        // Handle empty text
        if (japaneseText.isBlank()) {
            android.util.Log.w("Translation", "Empty input text")
            throw IllegalArgumentException("번역할 텍스트가 없습니다")
        }

        // Truncate if too long (Gemini has token limits)
        val truncatedText = if (japaneseText.length > 2000) {
            android.util.Log.w("Translation", "Text truncated from ${japaneseText.length} to 2000 chars")
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

        android.util.Log.d("Translation", "Prompt length: ${prompt.length}")

        return try {
            // Add timeout to prevent hanging
            kotlinx.coroutines.withTimeout(10000) {
                android.util.Log.d("Translation", "Calling Gemini API...")
                val response = model?.generateContent(prompt)
                android.util.Log.d("Translation", "API response received")

                // Safely get text
                val translatedText = try {
                    response?.text?.trim()
                } catch (e: Exception) {
                    // Response blocked by safety filters
                    android.util.Log.e("Translation", "Response blocked by safety filter", e)
                    throw TranslationException("번역 차단됨 (안전 필터)", cause = e)
                }

                if (!translatedText.isNullOrEmpty()) {
                    val elapsed = System.currentTimeMillis() - startTime
                    android.util.Log.d("Translation", "=== Translation Success ===")
                    android.util.Log.d("Translation", "Output: '$translatedText'")
                    android.util.Log.d("Translation", "Output length: ${translatedText.length}")
                    android.util.Log.d("Translation", "Elapsed time: ${elapsed}ms")
                    translatedText
                } else {
                    android.util.Log.e("Translation", "Empty response from API")
                    throw TranslationException("번역 결과 없음 (빈 응답)")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.e("Translation", "Timeout after ${elapsed}ms", e)
            throw TranslationException("요청 시간 초과 (10초)", cause = e)
        } catch (e: TranslationException) {
            // Re-throw our custom exceptions
            throw e
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.e("Translation", "Translation failed after ${elapsed}ms", e)
            android.util.Log.e("Translation", "Error type: ${e.javaClass.simpleName}")
            android.util.Log.e("Translation", "Error message: ${e.message}")

            val errorMessage = when {
                e.message?.contains("quota", ignoreCase = true) == true -> {
                    android.util.Log.e("Translation", "Quota exceeded")
                    "API 한도 초과"
                }
                e.message?.contains("blocked", ignoreCase = true) == true -> {
                    android.util.Log.e("Translation", "Content blocked")
                    "콘텐츠 차단됨"
                }
                e.message?.contains("SAFETY", ignoreCase = true) == true -> {
                    android.util.Log.e("Translation", "Safety filter triggered")
                    "안전 필터링됨"
                }
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> {
                    android.util.Log.e("Translation", "Network error")
                    "네트워크 오류"
                }
                e.message?.contains("rate limit", ignoreCase = true) == true -> {
                    android.util.Log.e("Translation", "Rate limit exceeded")
                    "요청 제한 초과 (잠시 후 다시 시도)"
                }
                else -> {
                    android.util.Log.e("Translation", "Unknown error: ${e.message}")
                    "번역 실패: ${e.message?.take(50) ?: "알 수 없음"}"
                }
            }

            throw TranslationException(errorMessage, cause = e)
        }
    }

    /**
     * Custom exception for translation errors
     */
    class TranslationException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    /**
     * Analyze user message for grammar errors, unnatural expressions, and suggestions
     * Returns JSON array of feedback items
     */
    suspend fun analyzeGrammarAndStyle(
        userMessage: String,
        conversationContext: List<String>,
        userLevel: Int
    ): String {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("GrammarAnalysis", "=== Starting Grammar Analysis ===")
        android.util.Log.d("GrammarAnalysis", "Message: $userMessage")
        android.util.Log.d("GrammarAnalysis", "Level: $userLevel")

        return try {
            // Simplified prompt - 15 lines instead of 40
            val prompt = """
                日本語学習者のメッセージを簡潔に分析してください。

                メッセージ: $userMessage
                レベル: ${if (userLevel == 1) "初級" else if (userLevel == 2) "中級" else "上級"}

                重要な問題のみJSON配列で返してください:
                [{"type":"GRAMMAR_ERROR","severity":"ERROR","explanation":"틀린 이유","correctedText":"올바른 문장"}]

                問題なければ空配列を返す: []

                チェック項目:
                1. 文法エラー(助詞、動詞活用)
                2. 不自然な表現
                3. 敬語の間違い

                JSONのみ出力、説明は韓国語で簡潔に。
            """.trimIndent()

            android.util.Log.d("GrammarAnalysis", "Sending request to Gemini...")

            // Generate content with the prompt using grammar model (15s timeout)
            val response = grammarModel?.generateContent(prompt)

            val responseText = response?.text?.trim() ?: "[]"
            val elapsed = System.currentTimeMillis() - startTime

            android.util.Log.d("GrammarAnalysis", "Response received in ${elapsed}ms")
            android.util.Log.d("GrammarAnalysis", "Response length: ${responseText.length}")

            // Clean response
            val cleanedResponse = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Validate JSON
            try {
                org.json.JSONArray(cleanedResponse)
                cleanedResponse
            } catch (e: Exception) {
                android.util.Log.e("GrammarAnalysis", "Invalid JSON, returning empty array")
                "[]"
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            android.util.Log.e("GrammarAnalysis", "Failed after ${elapsed}ms: ${e.message}", e)

            when {
                e.message?.contains("DEADLINE_EXCEEDED") == true -> {
                    android.util.Log.e("GrammarAnalysis", "Timeout error")
                }
                e.message?.contains("SAFETY") == true -> {
                    android.util.Log.e("GrammarAnalysis", "Safety filter blocked")
                }
                else -> {
                    android.util.Log.e("GrammarAnalysis", "Unknown error: ${e.javaClass.simpleName}")
                }
            }
            "[]" // Return empty array on error
        }
    }

    private fun buildHistory(
        history: List<Pair<String, Boolean>>,
        systemPrompt: String
    ) = buildList {
        // Add initial instruction as a user message (acts as guidance)
        add(
            content("user") {
                text(systemPrompt)
            }
        )

        // Short reminder for long conversations (acts as guidance)
        val reminderPrompt = """
            REMINDER: Output ONLY Japanese dialogue. NO English, NO thinking, NO explanations.
            絶対厳守：日本語の会話文のみ。英語禁止、思考過程禁止。
        """.trimIndent()

        // Add history with periodic reminders to prevent rule drift
        history.forEachIndexed { index, (text, isUser) ->
            add(
                content(if (isUser) "user" else "model") {
                    text(text)
                }
            )

            // Every 8 messages, inject a reminder (but not too close to the end)
            if (index > 0 && index % 8 == 0 && index < history.size - 2) {
                add(
                    content("user") {
                        text(reminderPrompt)
                    }
                )
            }
        }
    }

    /**
     * Generate simple text response using Gemini API
     * Used for generic text generation tasks (e.g., prompt generation)
     */
    suspend fun generateSimpleText(prompt: String): String {
        val activeModel = model
            ?: throw IllegalStateException("Gemini model not initialized")

        return try {
            val response = activeModel.generateContent(prompt)
            response.text ?: throw Exception("Empty response from Gemini API")
        } catch (e: Exception) {
            android.util.Log.e("GeminiApiService", "Error generating text: ${e.message}", e)
            throw e
        }
    }
}

private fun parseGrammarType(typeStr: String?): GrammarType {
    val normalized = typeStr?.trim()?.uppercase() ?: return GrammarType.EXPRESSION
    return GrammarType.values().firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        ?: GrammarType.EXPRESSION
}
