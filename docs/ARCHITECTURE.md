# Architecture

Technical architecture and design decisions for NihonGo Conversation.

## Overview

NihonGo Conversation follows **Clean Architecture** principles with **MVVM** pattern, built entirely with modern Android development practices.

## Tech Stack

### Core
- **Language**: Kotlin 1.9.0
- **Build**: Gradle with KSP
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Java**: 17

### UI Layer
- **Jetpack Compose**: 100% declarative UI
- **Material 3**: Design system
- **Compose BOM**: 2024.10.00
- **Navigation**: Compose Navigation
- **Paging 3**: Lazy loading for large lists

### Architecture Components
- **ViewModel**: State management
- **LiveData/Flow**: Reactive data streams
- **Lifecycle**: Lifecycle-aware components
- **DataStore**: Preferences and settings

### Dependency Injection
- **Hilt**: 2.48 (Dagger-based)
- **5 modules**: App, Database, Network, Voice, Translation

### Data Layer
- **Room**: 2.6.1 (SQLite abstraction)
- **Paging 3**: Database pagination
- **Retrofit**: 2.9.0 (REST client)
- **OkHttp**: HTTP client with connection pooling
- **GSON**: JSON serialization

### AI & NLP
- **Gemini SDK**: 0.9.0 (generative AI)
- **Kuromoji**: 0.9.0 (Japanese morphological analysis)
- **ML Kit**: On-device translation

### Voice
- **Android TTS**: Text-to-speech
- **SpeechRecognizer**: Speech-to-text
- Custom VoiceManager for queuing and state

### Charts & Visualization
- **Vico**: 1.13.1 (Charts library)

### Async Processing
- **Coroutines**: 1.7.3
- **Flow**: Reactive streams
- **StateFlow/SharedFlow**: State management

## Project Structure

```
app/src/main/java/com/nihongo/conversation/
├── data/                           # Data Layer
│   ├── local/                      # Local data sources
│   │   ├── dao/                    # Room DAOs (12 DAOs)
│   │   ├── entity/                 # Room entities (15+ entities)
│   │   └── AppDatabase.kt          # Room database definition
│   ├── remote/                     # Remote data sources
│   │   ├── GeminiApiService.kt     # Gemini API client
│   │   ├── MicrosoftTranslator.kt  # Microsoft Translator API
│   │   └── DeepLApiService.kt      # DeepL API client
│   ├── repository/                 # Repository implementations (12 repos)
│   │   ├── ConversationRepository.kt
│   │   ├── TranslationRepository.kt
│   │   ├── ScenarioRepository.kt
│   │   └── ...
│   └── seed/                       # Database seeding
│       └── ScenarioSeeds.kt        # 126 scenarios (2,837 lines)
│
├── domain/                         # Domain Layer
│   ├── model/                      # Business models
│   │   ├── Conversation.kt
│   │   ├── Message.kt
│   │   ├── Scenario.kt
│   │   ├── User.kt
│   │   ├── SentenceCard.kt
│   │   ├── GrammarFeedback.kt
│   │   ├── PronunciationAnalysis.kt
│   │   └── ...
│   ├── analyzer/                   # Analysis components
│   │   ├── PitchAccentAnalyzer.kt
│   │   ├── SpeedRhythmAnalyzer.kt
│   │   └── ProblematicSoundsDetector.kt
│   └── repository/                 # Repository interfaces
│
├── presentation/                   # Presentation Layer
│   ├── chat/                       # Chat feature
│   │   ├── ChatScreen.kt
│   │   ├── ChatViewModel.kt
│   │   ├── MessageBubble.kt
│   │   ├── FeedbackCard.kt
│   │   └── VoiceOnlyComponents.kt
│   ├── scenario/                   # Scenario browser
│   │   ├── ScenarioListScreen.kt
│   │   ├── ScenarioViewModel.kt
│   │   └── CategoryTabs.kt
│   ├── pronunciation/              # Pronunciation analysis
│   │   ├── PronunciationScreen.kt
│   │   ├── PitchVisualization.kt
│   │   └── IntonationVisualizer.kt
│   ├── study/                      # Sentence card practice
│   │   ├── PracticeScreen.kt
│   │   └── SentenceCardSheet.kt
│   ├── flashcard/                  # Flashcard review
│   ├── stats/                      # Statistics dashboard
│   ├── profile/                    # User profile
│   ├── settings/                   # App settings
│   └── ...                         # 35+ screens total
│
└── core/                           # Cross-cutting Concerns
    ├── di/                         # Dependency injection modules
    │   ├── AppModule.kt
    │   ├── DatabaseModule.kt
    │   ├── NetworkModule.kt
    │   ├── VoiceModule.kt
    │   └── TranslationModule.kt
    ├── voice/                      # Voice management
    │   └── VoiceManager.kt
    ├── translation/                # Translation orchestration
    │   └── TranslationManager.kt
    ├── grammar/                    # Grammar analysis
    │   ├── LocalGrammarAnalyzer.kt
    │   └── GeminiGrammarAnalyzer.kt
    ├── cache/                      # Caching layer
    │   ├── ResponseCache.kt
    │   └── TranslationCache.kt
    ├── network/                    # Network monitoring
    │   └── NetworkMonitor.kt
    └── util/                       # Utilities
        ├── DataInitializer.kt      # App initialization
        └── Extensions.kt
```

## Layer Responsibilities

### Data Layer
- **Responsibilities**:
  - Data persistence (Room)
  - Network communication (Retrofit)
  - Data transformation (Entity ↔ Model)
  - Caching strategies
- **Key Components**:
  - DAOs for database access
  - Repository implementations
  - API service interfaces
  - Database seeding

### Domain Layer
- **Responsibilities**:
  - Business logic
  - Domain models (pure Kotlin)
  - Use case definitions
  - Repository contracts
- **Key Components**:
  - Data models without Android dependencies
  - Analyzers for pronunciation/grammar
  - Repository interfaces

### Presentation Layer
- **Responsibilities**:
  - UI rendering (Compose)
  - User interaction handling
  - State management (ViewModel)
  - Navigation
- **Key Components**:
  - Composable screens
  - ViewModels with StateFlow
  - UI state classes

## Database Design

### Room Database (22 Migrations)

#### Core Tables
```sql
-- Conversations
CREATE TABLE conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL,
    scenarioId INTEGER,
    title TEXT NOT NULL,
    status TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY(userId) REFERENCES users(id),
    FOREIGN KEY(scenarioId) REFERENCES scenarios(id)
)

-- Messages
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversationId INTEGER NOT NULL,
    content TEXT NOT NULL,
    isFromUser INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    grammarFeedback TEXT,
    translationKo TEXT,
    FOREIGN KEY(conversationId) REFERENCES conversations(id)
)

-- Scenarios (126+ entries)
CREATE TABLE scenarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    slug TEXT UNIQUE NOT NULL,
    category TEXT NOT NULL,
    titleKo TEXT NOT NULL,
    titleJa TEXT NOT NULL,
    descriptionKo TEXT NOT NULL,
    difficulty INTEGER NOT NULL,
    coreInstruction TEXT NOT NULL,
    promptVersion INTEGER NOT NULL DEFAULT 1,
    isCustom INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL
)

-- Sentence Cards
CREATE TABLE sentence_cards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    messageId INTEGER,
    conversationId INTEGER,
    japanese TEXT NOT NULL,
    korean TEXT NOT NULL,
    grammarPattern TEXT,
    nextReviewDate INTEGER NOT NULL,
    easinessFactor REAL NOT NULL DEFAULT 2.5,
    interval INTEGER NOT NULL DEFAULT 0,
    repetitions INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(messageId) REFERENCES messages(id),
    FOREIGN KEY(conversationId) REFERENCES conversations(id)
)

-- Translation Cache (permanent)
CREATE TABLE translation_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sourceText TEXT NOT NULL,
    translatedText TEXT NOT NULL,
    provider TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    UNIQUE(sourceText, sourceLang, targetLang)
)

-- Users
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    difficultyLevel INTEGER NOT NULL DEFAULT 2,
    avatar TEXT,
    createdAt INTEGER NOT NULL
)
```

#### Indexes (11 optimized)
```sql
-- Composite indexes for common queries
CREATE INDEX idx_messages_conversation_timestamp
ON messages(conversationId, timestamp DESC)

CREATE INDEX idx_conversations_user_updated
ON conversations(userId, updatedAt DESC)

CREATE INDEX idx_sentence_cards_review
ON sentence_cards(nextReviewDate ASC, userId)

CREATE INDEX idx_translation_cache_lookup
ON translation_cache(sourceText, sourceLang, targetLang)

-- Single-column indexes
CREATE INDEX idx_scenarios_category ON scenarios(category)
CREATE INDEX idx_scenarios_difficulty ON scenarios(difficulty)
CREATE INDEX idx_messages_conversation ON messages(conversationId)
```

#### Database Views
```sql
CREATE VIEW conversation_stats AS
SELECT
    c.id,
    c.userId,
    COUNT(m.id) as messageCount,
    MAX(m.timestamp) as lastMessageTime
FROM conversations c
LEFT JOIN messages m ON c.id = m.conversationId
GROUP BY c.id
```

### Migration Strategy

**Upsert Pattern for Scenarios**:
```kotlin
@Transaction
suspend fun upsertBySlug(scenario: Scenario) {
    val existing = getScenarioBySlugSync(scenario.slug)
    when {
        existing == null -> insertScenario(scenario)
        existing.promptVersion < scenario.promptVersion -> {
            updateScenario(scenario.copy(
                id = existing.id,
                createdAt = existing.createdAt
            ))
        }
        else -> { /* skip - same version */ }
    }
}
```

## Scenario Management

### 126 Scenarios Across 16 Categories

| Category | Count | UI Tab |
|----------|-------|--------|
| ENTERTAINMENT | 27 | 🎵 엔터 |
| WORK | 14 | 💼 직장 |
| DAILY_LIFE | 15 | 🏠 일상 |
| TRAVEL | 13 | ✈️ 여행 |
| TECH | 9 | 💻 기술 |
| ESPORTS | 5 | 🎮 게임 |
| JLPT_PRACTICE | 5 | 📖 JLPT |
| CULTURE | 9 | 🎭 기타 |
| HEALTH | 7 | 🎭 기타 |
| FINANCE | 6 | 🎭 기타 |
| STUDY | 5 | 🎭 기타 |
| BUSINESS | 4 | 🎭 기타 |
| HOUSING | 3 | 🎭 기타 |
| ROMANCE | 2 | 🎭 기타 |
| EMERGENCY | 1 | 🎭 기타 |
| DAILY_CONVERSATION | 1 | 🎭 기타 |

### Data Initialization

```kotlin
// DataInitializer.kt - Orchestration
@Singleton
class DataInitializer @Inject constructor(
    private val scenarioDao: ScenarioDao,
    private val scenarioSeeds: ScenarioSeeds,
    private val userDao: UserDao,
    private val cacheInitializer: CacheInitializer
) {
    suspend fun initializeDefaultData() {
        // 1. Create default user
        createDefaultUser()

        // 2. Seed scenarios (upsert 126 scenarios)
        scenarioSeeds.seedAll(scenarioDao)

        // 3. Initialize response cache
        cacheInitializer.initializeCache()
    }
}

// ScenarioSeeds.kt - Data (2,837 lines)
@Singleton
class ScenarioSeeds @Inject constructor() {
    private val scenarios = listOf(
        Scenario(
            slug = "restaurant_ordering",
            category = "DAILY_LIFE",
            titleKo = "레스토랑 주문",
            titleJa = "レストランでの注文",
            difficulty = 1,
            promptVersion = 3,
            coreInstruction = "あなたは日本のレストランの店員です...",
            // ...
        ),
        // ... 126 scenarios
    )

    suspend fun seedAll(scenarioDao: ScenarioDao) {
        scenarios.forEach { scenario ->
            scenarioDao.upsertBySlug(scenario)
        }
    }
}
```

## API Integration

### Gemini 2.5 Flash
```kotlin
interface GeminiApiService {
    // Streaming conversation
    fun sendMessageStream(
        message: String,
        conversationHistory: List<Message>,
        systemPrompt: String
    ): Flow<String>

    // Batch requests (grammar + hints + translation)
    suspend fun batchRequests(
        sentence: String,
        context: ConversationContext,
        requestTypes: Set<BatchRequestType>
    ): BatchResponse
}
```

### Translation Services
```kotlin
interface TranslationRepository {
    suspend fun translate(
        text: String,
        provider: TranslationProvider,
        useCache: Boolean = true,
        fallbackChain: List<TranslationProvider> = emptyList()
    ): TranslationResult
}

// Automatic fallback chain
sealed class TranslationResult {
    data class Success(
        val translatedText: String,
        val provider: TranslationProvider,
        val fromCache: Boolean,
        val elapsed: Long
    ) : TranslationResult()

    data class Error(val message: String) : TranslationResult()
}
```

## State Management

### ViewModel Pattern
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val geminiService: GeminiApiService,
    private val translationRepository: TranslationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI state with immutable collections
    data class ChatUiState(
        val messages: ImmutableList<Message> = ImmutableList.empty(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val userTranslations: ImmutableMap<Long, String> = ImmutableMap.empty(),
        val grammarFeedback: ImmutableMap<Long, ImmutableList<GrammarFeedback>> = ImmutableMap.empty(),
        // ... 30+ state fields
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Events channel
    private val _events = Channel<ChatEvent>()
    val events: Flow<ChatEvent> = _events.receiveAsFlow()
}
```

## Performance Optimizations

### Network Layer
- **GZIP compression**: 70-90% payload reduction
- **Connection pooling**: 50% latency reduction (600ms → 300ms)
- **Request batching**: 61% faster (grammar + hints + translation in one call)
- **Streaming responses**: Low TTFB (~800ms)

### Database Layer
- **11 optimized indexes**: 5-10x faster queries
- **Paging 3**: Lazy loading for large datasets
- **Database views**: Pre-aggregated statistics
- **Transaction batching**: Bulk inserts for seeding

### Caching Layer
- **Response cache**: 99.7% faster (300ms → 1ms)
- **Translation cache**: 95% hit rate, <10ms
- **Permanent cache**: No expiration for translations
- **20 built-in phrases**: Instant offline access

### UI Layer
- **LazyColumn**: Efficient list rendering
- **Compose recomposition optimization**: Only changed items
- **Image loading**: Coil with caching
- **No AnimatedVisibility on messages**: Performance improvement

## Dependency Injection

### Hilt Modules

```kotlin
// AppModule.kt
@InstallIn(SingletonComponent::class)
@Module
object AppModule {
    @Provides @Singleton
    fun provideContext(@ApplicationContext context: Context) = context

    @Provides @Named("GeminiApiKey")
    fun provideGeminiApiKey() = BuildConfig.GEMINI_API_KEY

    @Provides @Named("MicrosoftApiKey")
    fun provideMicrosoftApiKey() = BuildConfig.MICROSOFT_TRANSLATOR_KEY

    @Provides @Named("DeepLApiKey")
    fun provideDeepLApiKey() = BuildConfig.DEEPL_API_KEY
}

// DatabaseModule.kt
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nihongo_database"
        )
        .addMigrations(*ALL_MIGRATIONS)
        .build()
    }

    @Provides fun provideConversationDao(db: AppDatabase) = db.conversationDao()
    @Provides fun provideMessageDao(db: AppDatabase) = db.messageDao()
    // ... 12 DAOs
}

// NetworkModule.kt
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept-Encoding", "gzip")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsofttranslator.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

## Testing Strategy

### Unit Tests
- **ViewModels**: State transitions, business logic
- **Repositories**: Data operations, caching
- **Analyzers**: Pronunciation/grammar analysis

### Integration Tests
- **Database**: Migrations, complex queries
- **API**: Network layer, serialization

### UI Tests
- **Compose**: Screen rendering, user interactions
- **Navigation**: Flow between screens

## Build Configuration

```kotlin
// build.gradle.kts
android {
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // Compose BOM (manages versions)
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // ML Kit
    implementation("com.google.mlkit:translate:17.0.1")

    // Kuromoji (Japanese NLP)
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    // Charts
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
}
```

## Security Considerations

### API Key Management
- Keys stored in `local.properties` (Git ignored)
- Accessed via BuildConfig at compile time
- Never logged or exposed

### ProGuard Rules
```proguard
# Gemini SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
```

## Design Patterns

- **Repository Pattern**: Data abstraction
- **Factory Pattern**: ViewModel creation
- **Observer Pattern**: StateFlow/Flow
- **Singleton Pattern**: Database, API clients
- **Strategy Pattern**: Translation provider selection
- **Builder Pattern**: Complex object construction
- **Adapter Pattern**: Entity ↔ Model mapping

## Future Scalability

### Planned Improvements
- **Offline AI**: Gemini Nano integration
- **Multiplatform**: Kotlin Multiplatform for web/iOS
- **Modularization**: Feature modules for faster builds
- **JSON scenarios**: Migrate from Kotlin to JSON for 200+ scenarios
- **Remote config**: Dynamic scenario updates

### Architecture Evolution
Current Kotlin-based approach works well up to 200 scenarios. Beyond that:
- Move scenarios to `assets/scenarios.json`
- Add `ScenarioLoader.kt` for JSON parsing
- Support multi-language scenario titles
- Enable non-developer content management
