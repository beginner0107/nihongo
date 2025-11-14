# Project: Nihongo Conversation

## Overview
Android Japanese conversation learning app powered by AI (Gemini), featuring voice interaction, grammar analysis, and spaced repetition learning.

## Tech Stack
- **UI**: Jetpack Compose, Material 3
- **Architecture**: MVVM + Clean Architecture (data/domain/presentation)
- **Database**: Room 2.6.1, Paging 3
- **Dependency Injection**: Hilt 2.48
- **AI/Translation**: Gemini SDK 0.9.0, Microsoft Translator, DeepL, ML Kit
- **Voice**: Android TTS/STT, SpeechRecognizer
- **Charts**: Vico 1.13.1
- **NLP**: Kuromoji 0.9.0 (Japanese morphological analysis)
- **Async**: Coroutines 1.7.3, Flow
- **Build**: Kotlin 1.5.14, Java 17, KSP

## Architecture
```
app/src/main/java/com/nihongo/conversation/
├── data/                    # Repository, Room DB, API clients
│   ├── local/              # Room entities, DAOs, caching
│   ├── remote/             # Gemini, Microsoft, DeepL APIs
│   ├── repository/         # 12 repositories (ConversationRepository, etc.)
│   └── seed/               # Database seeding
├── domain/                  # Business logic, models
│   ├── model/              # 15+ data models (Scenario, User, Message, etc.)
│   └── analyzer/           # Pronunciation, pitch accent analyzers
├── presentation/           # Compose screens, ViewModels
│   ├── chat/               # Core chat feature
│   ├── scenario/           # Scenario browser with search/filter
│   ├── profile/            # User profile management
│   ├── stats/              # Analytics dashboard
│   ├── vocabulary/         # Flashcard system
│   └── [35 screens total]
└── core/                   # Cross-cutting concerns
    ├── di/                 # Hilt modules (5 modules)
    ├── voice/              # TTS/STT (VoiceManager)
    ├── translation/        # 3-provider translation system
    ├── grammar/            # Grammar analysis (Local + Gemini)
    ├── difficulty/         # Difficulty management
    ├── cache/              # Response & translation caching
    └── util/               # DataInitializer (DB seeding)
```

## Key Commands
```bash
# Development
./gradlew compileDebugKotlin    # Compile Kotlin
./gradlew assembleDebug         # Build APK
./gradlew installDebug          # Install to device

# Clean reinstall (recommended for DB changes)
adb uninstall com.nihongo.conversation
# Then run from Android Studio

# Debugging
adb logcat -s ChatViewModel:D GrammarDebug:D
adb logcat -d | grep "Migration didn't properly handle"
```

## API Configuration
```properties
# local.properties (Git ignored)
GEMINI_API_KEY=your_key
MICROSOFT_TRANSLATOR_KEY=your_key
DEEPL_API_KEY=your_key

# build.gradle.kts auto-generates BuildConfig fields
```

## Coding Standards

### Dependency Injection
```kotlin
// ✅ Use @Named for multiple instances
@Provides @Named("MicrosoftApiKey") fun provideMicrosoftApiKey(): String
@Provides @Named("DeepLApiKey") fun provideDeepLApiKey(): String

class Repository @Inject constructor(
    @Named("MicrosoftApiKey") private val apiKey: String
)
```

### Room Migrations
```kotlin
// ⚠️ CRITICAL: Entity must exactly match Migration SQL

// ✅ Correct
@Entity(
    tableName = "example",
    indices = [Index(value = ["name"])]  // Declare in Entity
)
data class Example(val name: String = "default")  // Kotlin default OK

// Migration
database.execSQL("CREATE TABLE example (name TEXT NOT NULL)")  // NO DEFAULT
database.execSQL("CREATE INDEX IF NOT EXISTS index_example_name ON example(name)")

// ❌ Wrong: DEFAULT in SQL causes schema mismatch
database.execSQL("CREATE TABLE example (name TEXT NOT NULL DEFAULT 'default')")
```

### Thread Safety
```kotlin
// ✅ Synchronized access for shared resources
synchronized(pendingSpeechQueue) {
    pendingSpeechQueue.add(item)
}
```

### Null Safety
```kotlin
// ✅ Safe unwrapping
val tts = textToSpeech ?: return
tts.speak(text)
```

### Error Handling
```kotlin
// ✅ Specific error messages
try { /* operation */ }
catch (e: Exception) {
    _events.trySend(VoiceEvent.Error("TTS initialization failed: ${e.message}"))
}
```

## Critical Don'ts
- ❌ **Room**: DEFAULT values in Migration SQL (use Kotlin defaults)
- ❌ **Room**: Missing `@Index` in Entity when Migration creates index
- ❌ **DI**: Providing same type without `@Named` annotation
- ❌ **UI**: Hardcoded strings (use `stringResource(R.string.*)`)
- ❌ **AI**: Markdown in responses (enforce in system prompts)
- ❌ **Voice**: Blocking main thread for TTS operations
- ❌ **API**: Exposing API keys in code (use BuildConfig)

## Core Features

### 3-Provider Translation System
**Automatic fallback chain**: Cache → Microsoft (2M chars/month) → DeepL (500K) → ML Kit (offline)

```kotlin
val result = translationRepository.translate(
    text = japaneseText,
    provider = TranslationProvider.MICROSOFT,  // Default
    useCache = true,
    fallbackChain = listOf(DEEP_L, ML_KIT)
)
```

**Quota Management**:
- Microsoft: 2M chars/month (primary)
- DeepL: 500K chars/month (high accuracy)
- ML Kit: Unlimited (offline fallback)
- Cache: 30-day TTL, ~95% hit rate

### Scenario System
- 126+ predefined scenarios across 16 categories
- Custom scenario creation/deletion
- Search (title/description/category) + difficulty filter
- Favorites system
- 5-level difficulty: 입문/초급/중급/고급/최상급

### Grammar Analysis
- **Fast**: Local Kuromoji analyzer (instant)
- **Smart**: Gemini API with 5s timeout + auto-fallback
- **Patterns**: 10+ grammar patterns extraction
- **Success rate**: ~90%

### Voice Features
- **TTS**: Japanese voice with speed control (0.5x - 2.0x)
- **STT**: Real-time speech recognition
- **Voice-only mode**: No text UI
- **Pronunciation analysis**: Pitch, rhythm, problematic sounds

### Spaced Repetition
- **SM-2 algorithm** implementation
- **4 practice modes**: Read/Listen/Fill/Speak
- Flashcard interface with review scheduling

## Common Issues & Quick Fixes

### TTS Not Working
**Symptom**: "未初期化" error or no sound
**Fix**:
1. Device Settings → Language & Input → Text-to-Speech → Install Japanese voice data
2. Clean reinstall: `adb uninstall com.nihongo.conversation`

### Migration Crash
**Symptom**: `IllegalStateException: Migration didn't properly handle`
**Cause**: Entity definition ≠ Migration SQL schema
**Fix**:
1. Check `Expected` vs `Found` in logcat
2. Verify `@Index` annotations in Entity
3. Remove DEFAULT from Migration SQL
4. Test with clean reinstall

**Debug**:
```bash
adb logcat -d | grep -A 20 "Migration didn't properly handle"
```

### AI Markdown in Responses
**Symptom**: `**text**`, `（ふりがな）` in chat
**Cause**: Old system prompts in DB
**Fix**: Clean reinstall to apply new prompts

### Translation Quota Exceeded
**Symptom**: Translation fails
**Fix**: Automatic fallback to DeepL → ML Kit (no action needed)

### Grammar Analysis Timeout
**Symptom**: Takes >5 seconds
**Fix**: Automatic fallback to LocalGrammarAnalyzer (already implemented)

## Development Guidelines

### AI System Prompts
```kotlin
// Always include formatting rules
"""
【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。

TEXT FORMATTING - CRITICAL:
- NEVER use markdown formatting (**, __, *, _)
- NEVER use furigana or pronunciation guides
- Write pure Japanese text without any annotations
"""
```

### Difficulty Levels
- **입문 (1)**: 5-10 words, present tense, basic vocabulary
- **초급 (2)**: 10-15 words, polite form, common expressions
- **중급 (3)**: Complex sentences, casual/polite mix
- **고급 (4)**: Keigo, nuanced expressions
- **최상급 (5)**: Native-level idioms

### Component Reuse
```kotlin
// ✅ Use shared components for consistency
DifficultyBadge(difficulty = level)  // Single source of truth

// ❌ Don't hardcode difficulty display
when (difficulty) { 1 -> "초급" ... }  // Maintenance nightmare
```

### Database Seeding
- **DataInitializer** runs on app first launch
- Populates 126 scenarios automatically
- Don't delete/modify `core/util/DataInitializer.kt` without migration

## Performance Notes
- **Message rendering**: No AnimatedVisibility wrapper (removed for performance)
- **Auto-scroll**: Smart detection (only scroll if user near bottom)
- **Translation cache**: ~95% hit rate → minimal API calls
- **Grammar analysis**: 5s timeout with instant local fallback

## Recent Breaking Changes (Last 3 Months)
See [CHANGELOG.md](CHANGELOG.md) for detailed history.

### Nov 2025
- **DifficultyBadge unification**: Use `DifficultyBadge` component everywhere
- **3-provider translation**: Microsoft primary, DeepL/ML Kit fallback
- **Scenario search/filter**: Real-time search + difficulty filters
- **i18n support**: Japanese/Korean/English UI strings

### Oct 2025
- **Grammar timeout**: Reduced to 5s with local fallback
- **Context menu**: Long-press messages for copy/TTS/grammar
- **TTS pending queue**: Async initialization with queuing

## Build Configuration
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
```

```kotlin
// build.gradle.kts
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}

dependencies {
    // Compose BOM manages versions
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.material3:material3")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

## Device Requirements
- **Min SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Permissions**: INTERNET, RECORD_AUDIO
- **TTS**: Japanese voice data required
- **Storage**: ~50MB for app + database

## Testing Checklist
After making changes:
- [ ] Clean reinstall: `adb uninstall` → run
- [ ] TTS auto-play works
- [ ] No markdown (`**`, `*`) in AI responses
- [ ] No furigana `（）` in AI responses
- [ ] Voice recognition works
- [ ] Translation works (cache → Microsoft → DeepL → ML Kit)
- [ ] Grammar analysis responds within 5s
- [ ] Search/filter scenarios works
- [ ] Migration succeeds (check logcat)

## Debugging Tips
```bash
# Grammar analysis
adb logcat -s GrammarDebug:D GrammarAPI:E

# TTS issues
adb logcat -s VoiceManager:D

# Translation provider chain
adb logcat -s TranslationRepository:D

# Room migrations
adb logcat -d | grep "Migration"

# Crash logs
adb logcat -d | grep -i "exception\|error" | tail -50
```

## Token Optimization (for Claude interactions)
```
✅ Do:
- Reference file:line (e.g., ChatViewModel.kt:142)
- Show only changed code sections
- Use // ... existing code ... placeholders
- Ask specific questions with context

❌ Don't:
- Request full file rewrites
- Ask for verbose explanations
- Generate extensive documentation
- Repeat unchanged code
```

## Support
- **Issues**: https://github.com/anthropics/claude-code/issues
- **Docs**: https://docs.claude.com/en/docs/claude-code/
- **Help**: Type `/help` in Claude Code CLI
