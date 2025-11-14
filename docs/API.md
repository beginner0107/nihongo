# API Integration

Complete guide for integrating Gemini AI and translation services.

## Overview

NihonGo Conversation uses three main APIs:
1. **Gemini 2.5 Flash** - AI conversation and grammar analysis
2. **Microsoft Translator** - Primary translation (2M chars/month free)
3. **DeepL** - Fallback translation (500K chars/month free)
4. **ML Kit** - Offline translation (unlimited, on-device)

## Gemini 2.5 Flash API

### Get API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key

### Configuration

```properties
# local.properties
GEMINI_API_KEY=your_gemini_api_key_here
```

### Features

#### Streaming Conversation
```kotlin
interface GeminiApiService {
    fun sendMessageStream(
        message: String,
        conversationHistory: List<Message>,
        systemPrompt: String,
        userLevel: Int
    ): Flow<String>
}

// Usage
viewModelScope.launch {
    geminiService.sendMessageStream(
        message = userMessage,
        conversationHistory = recentMessages.takeLast(20),
        systemPrompt = scenarioPrompt,
        userLevel = user.difficultyLevel
    ).collect { token ->
        // Update UI with each token
        updateResponse(token)
    }
}
```

#### Batch Requests
Combine multiple requests into one API call (61% faster):

```kotlin
suspend fun batchRequests(
    sentence: String,
    context: ConversationContext,
    requestTypes: Set<BatchRequestType>
): BatchResponse

enum class BatchRequestType {
    GRAMMAR,      // Grammar analysis
    HINTS,        // Korean translation hints
    TRANSLATION   // Full translation
}

// Usage
val response = geminiService.batchRequests(
    sentence = "これは何ですか",
    context = currentContext,
    requestTypes = setOf(
        BatchRequestType.GRAMMAR,
        BatchRequestType.HINTS,
        BatchRequestType.TRANSLATION
    )
)
```

### Performance Optimizations

| Optimization | Impact |
|--------------|--------|
| GZIP compression | 70-90% payload reduction |
| Connection pooling | 50% latency reduction (600ms → 300ms) |
| Batch requests | 61% faster than separate calls |
| Recent 20 messages only | 60% payload reduction |
| Message truncation (2000 chars) | Prevents quota exhaustion |

### Rate Limits

- **Free tier**: 60 requests/minute
- **Streaming**: Counts as 1 request regardless of length
- **Batch requests**: Counts as 1 request for all operations

### Error Handling

```kotlin
try {
    val response = geminiService.sendMessage(text)
} catch (e: Exception) {
    when {
        e.message?.contains("quota") == true -> {
            // Quota exceeded
            showError("API quota exceeded. Try again later.")
        }
        e.message?.contains("timeout") == true -> {
            // Timeout (8 second limit)
            showError("Request timed out. Please try again.")
        }
        else -> {
            // Other errors
            showError("AI service unavailable: ${e.message}")
        }
    }
}
```

## Translation APIs

### 3-Provider Fallback Chain

```
[User Request]
     ↓
[Cache Check] ──(HIT)──→ [Return Cached] (<10ms)
     ↓ (MISS)
[Microsoft Translator] ──(Success)──→ [Cache & Return] (200-400ms)
     ↓ (Fail/Quota)
[DeepL API] ──(Success)──→ [Cache & Return] (300-600ms)
     ↓ (Fail/Quota)
[ML Kit] ──(Always works)──→ [Cache & Return] (100-200ms)
```

### 1. Microsoft Translator

#### Get API Key

1. Go to [Azure Portal](https://portal.azure.com)
2. Create a resource → Search "Translator"
3. Select **Free F0** tier (2M chars/month)
4. Choose region: **Korea Central** (lowest latency)
5. Copy **Key** and **Region**

#### Configuration

```properties
# local.properties
MICROSOFT_TRANSLATOR_KEY=your_microsoft_key_here
MICROSOFT_TRANSLATOR_REGION=koreacentral
```

#### API Details

**Endpoint**:
```
POST https://api.cognitive.microsofttranslator.com/translate?api-version=3.0
     &from=ja&to=ko
```

**Headers**:
```http
Ocp-Apim-Subscription-Key: {YOUR_KEY}
Ocp-Apim-Subscription-Region: {YOUR_REGION}
Content-Type: application/json
```

**Request Body**:
```json
[
  {
    "text": "こんにちは"
  }
]
```

**Response**:
```json
[
  {
    "translations": [
      {
        "text": "안녕하세요",
        "to": "ko"
      }
    ]
  }
]
```

#### Implementation

```kotlin
interface MicrosoftTranslatorService {
    @POST("translate")
    suspend fun translate(
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String,
        @Header("Ocp-Apim-Subscription-Region") region: String,
        @Query("api-version") apiVersion: String = "3.0",
        @Query("from") from: String = "ja",
        @Query("to") to: String = "ko",
        @Body texts: List<MicrosoftTranslateRequest>
    ): List<MicrosoftTranslateResponse>
}

// Usage
val response = microsoftService.translate(
    subscriptionKey = BuildConfig.MICROSOFT_TRANSLATOR_KEY,
    region = "koreacentral",
    texts = listOf(MicrosoftTranslateRequest("こんにちは"))
)
val translation = response[0].translations[0].text
```

### 2. DeepL API

#### Get API Key

1. Visit [DeepL Pro API](https://www.deepl.com/pro-api)
2. Sign up for **Free** plan (500K chars/month)
3. Verify email
4. Copy API key from dashboard

#### Configuration

```properties
# local.properties
DEEPL_API_KEY=your_deepl_key_here
```

#### API Details

**Endpoint** (Free tier):
```
POST https://api-free.deepl.com/v2/translate
```

**Headers**:
```http
Authorization: DeepL-Auth-Key {YOUR_KEY}
Content-Type: application/json
```

**Request Body**:
```json
{
  "text": ["こんにちは"],
  "source_lang": "JA",
  "target_lang": "KO"
}
```

**Response**:
```json
{
  "translations": [
    {
      "detected_source_language": "JA",
      "text": "안녕하세요"
    }
  ]
}
```

#### Implementation

```kotlin
interface DeepLApiService {
    @POST("v2/translate")
    suspend fun translate(
        @Header("Authorization") authKey: String,
        @Body request: DeepLRequest
    ): DeepLResponse
}

data class DeepLRequest(
    val text: List<String>,
    @SerializedName("source_lang") val sourceLang: String = "JA",
    @SerializedName("target_lang") val targetLang: String = "KO"
)

// Usage
val response = deepLService.translate(
    authKey = "DeepL-Auth-Key ${BuildConfig.DEEPL_API_KEY}",
    request = DeepLRequest(text = listOf("こんにちは"))
)
val translation = response.translations[0].text
```

### 3. ML Kit Translation

#### Setup

No API key required. Model downloads automatically (~30MB).

```kotlin
// Initialize translator
val options = TranslatorOptions.Builder()
    .setSourceLanguage(TranslateLanguage.JAPANESE)
    .setTargetLanguage(TranslateLanguage.KOREAN)
    .build()

val translator = Translation.getClient(options)

// Download model if needed
translator.downloadModelIfNeeded()
    .addOnSuccessListener {
        Log.d("MLKit", "Model ready")
    }
    .addOnFailureListener { exception ->
        Log.e("MLKit", "Model download failed", exception)
    }
```

#### Usage

```kotlin
// Translate text
translator.translate(text)
    .addOnSuccessListener { translatedText ->
        // Use translation
        displayTranslation(translatedText)
    }
    .addOnFailureListener { exception ->
        // Handle error
        Log.e("MLKit", "Translation failed", exception)
    }
```

#### Characteristics

| Feature | Details |
|---------|---------|
| **Cost** | Free, unlimited |
| **Network** | Not required (on-device) |
| **Speed** | 100-200ms |
| **Accuracy** | Good for simple sentences, fair for complex |
| **Model Size** | ~30MB (ja-ko pair) |
| **Privacy** | 100% on-device, no data sent |

## Translation Repository

### Automatic Fallback Implementation

```kotlin
class TranslationRepository @Inject constructor(
    private val microsoftService: MicrosoftTranslatorService,
    private val deepLService: DeepLApiService,
    private val mlKitTranslator: Translator,
    private val translationDao: TranslationDao
) {
    suspend fun translate(
        text: String,
        provider: TranslationProvider = TranslationProvider.MICROSOFT,
        useCache: Boolean = true,
        fallbackChain: List<TranslationProvider> = listOf(
            TranslationProvider.DEEP_L,
            TranslationProvider.ML_KIT
        )
    ): TranslationResult {
        // 1. Check cache first
        if (useCache) {
            val cached = translationDao.getCached(text)
            if (cached != null) {
                return TranslationResult.Success(
                    translatedText = cached.translatedText,
                    provider = TranslationProvider.valueOf(cached.provider),
                    fromCache = true,
                    elapsed = 1L
                )
            }
        }

        // 2. Try primary provider
        val startTime = System.currentTimeMillis()
        var result = tryProvider(provider, text)

        // 3. Try fallback chain
        if (result is TranslationResult.Error) {
            for (fallbackProvider in fallbackChain) {
                result = tryProvider(fallbackProvider, text)
                if (result is TranslationResult.Success) break
            }
        }

        // 4. Cache successful result
        if (result is TranslationResult.Success) {
            translationDao.insert(
                TranslationCacheEntity(
                    sourceText = text,
                    translatedText = result.translatedText,
                    provider = result.provider.name,
                    timestamp = System.currentTimeMillis()
                )
            )
            return result.copy(elapsed = System.currentTimeMillis() - startTime)
        }

        return result
    }

    private suspend fun tryProvider(
        provider: TranslationProvider,
        text: String
    ): TranslationResult {
        return try {
            when (provider) {
                TranslationProvider.MICROSOFT -> {
                    val response = microsoftService.translate(...)
                    TranslationResult.Success(
                        translatedText = response[0].translations[0].text,
                        provider = provider,
                        fromCache = false,
                        elapsed = 0L
                    )
                }
                TranslationProvider.DEEP_L -> {
                    val response = deepLService.translate(...)
                    TranslationResult.Success(
                        translatedText = response.translations[0].text,
                        provider = provider,
                        fromCache = false,
                        elapsed = 0L
                    )
                }
                TranslationProvider.ML_KIT -> {
                    val translation = mlKitTranslator.translate(text).await()
                    TranslationResult.Success(
                        translatedText = translation,
                        provider = provider,
                        fromCache = false,
                        elapsed = 0L
                    )
                }
            }
        } catch (e: Exception) {
            TranslationResult.Error("${provider.name} failed: ${e.message}")
        }
    }
}
```

### Usage in ViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    fun translateMessage(messageId: Long, japaneseText: String) {
        viewModelScope.launch {
            val result = translationRepository.translate(
                text = japaneseText,
                provider = TranslationProvider.MICROSOFT,
                useCache = true,
                fallbackChain = listOf(
                    TranslationProvider.DEEP_L,
                    TranslationProvider.ML_KIT
                )
            )

            when (result) {
                is TranslationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            translations = it.translations.put(
                                messageId,
                                result.translatedText
                            )
                        )
                    }
                    Log.d("Translation", "Provider: ${result.provider}, " +
                          "Cache: ${result.fromCache}, " +
                          "Time: ${result.elapsed}ms")
                }
                is TranslationResult.Error -> {
                    _events.send(ChatEvent.ShowError(result.message))
                }
            }
        }
    }
}
```

## Quota Management

### Monthly Limits

| Provider | Free Limit | Estimated Usage |
|----------|------------|-----------------|
| Microsoft | 2,000,000 chars | 400 days @ 5K chars/day |
| DeepL | 500,000 chars | 100 days @ 5K chars/day |
| ML Kit | Unlimited | Forever |

### Usage Tracking

```kotlin
class QuotaTracker @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val MICROSOFT_CHARS_KEY = intPreferencesKey("microsoft_chars")
    private val DEEPL_CHARS_KEY = intPreferencesKey("deepl_chars")

    suspend fun trackUsage(provider: TranslationProvider, charCount: Int) {
        dataStore.edit { prefs ->
            when (provider) {
                TranslationProvider.MICROSOFT -> {
                    val current = prefs[MICROSOFT_CHARS_KEY] ?: 0
                    prefs[MICROSOFT_CHARS_KEY] = current + charCount
                }
                TranslationProvider.DEEP_L -> {
                    val current = prefs[DEEPL_CHARS_KEY] ?: 0
                    prefs[DEEPL_CHARS_KEY] = current + charCount
                }
                else -> { /* ML Kit is unlimited */ }
            }
        }
    }

    suspend fun getRemainingQuota(provider: TranslationProvider): Int {
        return dataStore.data.map { prefs ->
            when (provider) {
                TranslationProvider.MICROSOFT -> {
                    2_000_000 - (prefs[MICROSOFT_CHARS_KEY] ?: 0)
                }
                TranslationProvider.DEEP_L -> {
                    500_000 - (prefs[DEEPL_CHARS_KEY] ?: 0)
                }
                TranslationProvider.ML_KIT -> Int.MAX_VALUE
            }
        }.first()
    }
}
```

## Caching Strategy

### Translation Cache

**Database Schema**:
```sql
CREATE TABLE translation_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sourceText TEXT NOT NULL,
    translatedText TEXT NOT NULL,
    provider TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    UNIQUE(sourceText, sourceLang, targetLang)
)

CREATE INDEX idx_translation_cache_lookup
ON translation_cache(sourceText, sourceLang, targetLang)
```

**Benefits**:
- **Permanent storage**: Translations never expire
- **Instant retrieval**: <10ms vs 200-600ms API call
- **~95% cache hit rate**: Most phrases repeat
- **Zero API cost**: No charges for cached translations

### Response Cache (Gemini)

Common phrases cached for offline access:

```kotlin
val COMMON_RESPONSES = mapOf(
    "おはよう" to "おはようございます",
    "こんにちは" to "こんにちは",
    "ありがとう" to "どういたしまして",
    // ... 20 built-in phrases
)
```

## Performance Metrics

| Metric | Microsoft | DeepL | ML Kit | Cache |
|--------|-----------|-------|--------|-------|
| **Latency** | 200-400ms | 300-600ms | 100-200ms | <10ms |
| **Success Rate** | 95% | 90% | 100% | 100% |
| **Accuracy** | High | Very High | Medium | N/A |
| **Cost** | Free (2M) | Free (500K) | Free (∞) | Free |
| **Offline** | ❌ | ❌ | ✅ | ✅ |

## Error Handling

### Common Errors

```kotlin
sealed class TranslationError {
    object QuotaExceeded : TranslationError()
    object NetworkError : TranslationError()
    object InvalidApiKey : TranslationError()
    object ModelNotDownloaded : TranslationError()
    data class UnknownError(val message: String) : TranslationError()
}

fun handleTranslationError(error: Exception): TranslationError {
    return when {
        error.message?.contains("quota") == true -> QuotaExceeded
        error.message?.contains("401") == true -> InvalidApiKey
        error.message?.contains("network") == true -> NetworkError
        error is MlKitException -> ModelNotDownloaded
        else -> UnknownError(error.message ?: "Unknown error")
    }
}
```

### Retry Strategy

```kotlin
suspend fun <T> retryWithExponentialBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries - 1) {
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // Last attempt
}

// Usage
val result = retryWithExponentialBackoff {
    microsoftService.translate(text)
}
```

## Security

### API Key Protection

```kotlin
// ✅ CORRECT: Use BuildConfig
class Repository @Inject constructor(
    @Named("GeminiApiKey") private val apiKey: String
) {
    private val service = GeminiService(apiKey)
}

// Hilt Module
@Provides
@Named("GeminiApiKey")
fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY
```

```properties
# local.properties (Git ignored)
GEMINI_API_KEY=actual_key_here
```

```kotlin
// build.gradle.kts
android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\""
        )
    }
}
```

### ProGuard Rules

```proguard
# Gemini SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Retrofit
-keepattributes Signature
-keep class retrofit2.** { *; }

# Don't log API keys
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

## Resources

- [Gemini API Documentation](https://ai.google.dev/docs)
- [Microsoft Translator Documentation](https://docs.microsoft.com/azure/cognitive-services/translator/)
- [DeepL API Documentation](https://www.deepl.com/docs-api)
- [ML Kit Translation Guide](https://developers.google.com/ml-kit/language/translation)
