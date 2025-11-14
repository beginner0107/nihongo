# Development Guide

Setup instructions and best practices for developing NihonGo Conversation.

## Prerequisites

### Required
- **Android Studio**: Hedgehog (2023.1.1) or higher
- **JDK**: 17
- **Android SDK**: API 24+ (Android 7.0+)
- **Kotlin**: 1.9.0+
- **Gemini API Key**: [Get one here](https://makersuite.google.com/app/apikey)

### Optional
- **Microsoft Translator API Key**: [Azure Portal](https://portal.azure.com)
- **DeepL API Key**: [DeepL Pro API](https://www.deepl.com/pro-api)

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/nihongo-conversation.git
cd nihongo-conversation
```

### 2. Configure API Keys

Create `local.properties` in the project root:

```properties
# Required
GEMINI_API_KEY=your_gemini_api_key_here

# Optional (for translation)
MICROSOFT_TRANSLATOR_KEY=your_microsoft_key_here
MICROSOFT_TRANSLATOR_REGION=koreacentral

# Optional (fallback translation)
DEEPL_API_KEY=your_deepl_key_here
```

**Get API Keys**:

| Service | Free Tier | Get Key |
|---------|-----------|---------|
| Gemini | Free | [Google AI Studio](https://makersuite.google.com/app/apikey) |
| Microsoft Translator | 2M chars/month | [Azure Portal](https://portal.azure.com) |
| DeepL | 500K chars/month | [DeepL API](https://www.deepl.com/pro-api) |

### 3. Configure Gradle

Create or update `gradle.properties`:

```properties
# Performance settings (required for successful build)
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true

# Kotlin optimizations
kotlin.incremental=true
kotlin.caching.enabled=true

# Android build optimizations
android.useAndroidX=true
android.enableJetifier=false
```

### 4. Build Project

#### Using Android Studio (Recommended)
1. Open project in Android Studio
2. Wait for Gradle sync to complete
3. Click **Run** ▶️ or press `Shift + F10`

#### Using Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

## Development Workflow

### Project Structure Quick Reference

```
nihongo/
├── app/
│   ├── src/main/
│   │   ├── java/com/nihongo/conversation/
│   │   │   ├── data/          # Repositories, DAOs, API clients
│   │   │   ├── domain/        # Business logic, models
│   │   │   ├── presentation/  # Compose UI, ViewModels
│   │   │   └── core/          # DI, utilities
│   │   └── res/               # Resources (strings, layouts)
│   ├── build.gradle.kts       # App dependencies
│   └── proguard-rules.pro     # ProGuard configuration
├── build.gradle.kts           # Project-level build config
├── settings.gradle.kts        # Module configuration
├── local.properties           # API keys (Git ignored)
└── gradle.properties          # Build properties
```

### Common Tasks

#### Clean Rebuild
```bash
./gradlew clean assembleDebug
```

#### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

#### Check for Dependency Updates
```bash
./gradlew dependencyUpdates
```

#### Generate APK
```bash
# Debug APK (outputs to app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

## Debugging

### Logcat Filtering

```bash
# View all app logs
adb logcat -s ChatViewModel:D GrammarAPI:D VoiceManager:D

# View only errors
adb logcat *:E

# View specific component
adb logcat -s TranslationRepository:D

# Save logs to file
adb logcat -d > debug_log.txt
```

### Useful Log Tags

| Tag | Component |
|-----|-----------|
| `ChatViewModel` | Chat logic, message handling |
| `GrammarAPI` | Grammar analysis |
| `VoiceManager` | TTS/STT operations |
| `TranslationRepository` | Translation provider chain |
| `NetworkMonitor` | Network connectivity |
| `ScenarioSeeds` | Scenario database seeding |

### Inspect Database

```bash
# Pull database from device
adb pull /data/data/com.nihongo.conversation/databases/nihongo_database.db

# Open with SQLite browser
# Download: https://sqlitebrowser.org/
```

### View App Info

```bash
# Check if app is installed
adb shell pm list packages | grep nihongo

# View package info
adb shell dumpsys package com.nihongo.conversation

# Clear app data
adb shell pm clear com.nihongo.conversation
```

## Code Style

### Kotlin Conventions

```kotlin
// Class names: PascalCase
class ChatViewModel

// Function names: camelCase
fun sendMessage()

// Constants: UPPER_SNAKE_CASE
const val MAX_MESSAGE_LENGTH = 2000

// Properties: camelCase
val isLoading = false

// Nullable types: Always use safe calls
val user = userRepository.getUser()
val name = user?.name ?: "Unknown"
```

### Compose Best Practices

```kotlin
// Composable names: PascalCase
@Composable
fun MessageBubble(message: Message) {
    // ...
}

// State hoisting
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit
) {
    // UI reacts to state, events go up
}

// Remember for expensive operations
val scrollState = rememberScrollState()
```

### Dependency Injection

```kotlin
// Use @Named for multiple instances
@Provides
@Named("GeminiApiKey")
fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

// Inject with @Named
class Repository @Inject constructor(
    @Named("GeminiApiKey") private val apiKey: String
)
```

## Database Migrations

### Creating New Migration

When modifying Room entities:

```kotlin
// 1. Update entity
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: Long,
    val content: String,
    val newField: String = "" // New field with default
)

// 2. Create migration
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add column WITHOUT DEFAULT in SQL
        database.execSQL(
            "ALTER TABLE messages ADD COLUMN newField TEXT NOT NULL DEFAULT ''"
        )
        // Or create new table
        database.execSQL("""
            CREATE TABLE new_table (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
        """)
    }
}

// 3. Add to AppDatabase
@Database(
    entities = [...],
    version = Y // Increment version
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val ALL_MIGRATIONS = arrayOf(
            // ... existing migrations
            MIGRATION_X_Y
        )
    }
}
```

### Migration Best Practices

**Critical Rules**:
- ✅ **Entity defaults**: Use Kotlin defaults, NOT SQL `DEFAULT`
- ✅ **Indexes**: Declare in `@Entity(indices = [...])`, not just in migration
- ✅ **Test migrations**: Always do clean reinstall to verify

**Common Mistakes**:

```kotlin
// ❌ WRONG: DEFAULT in SQL creates schema mismatch
database.execSQL("""
    CREATE TABLE example (
        name TEXT NOT NULL DEFAULT 'default'
    )
""")

// ✅ CORRECT: No DEFAULT in SQL
database.execSQL("""
    CREATE TABLE example (
        name TEXT NOT NULL
    )
""")

// Use Kotlin default instead
@Entity
data class Example(
    val name: String = "default"
)
```

### Testing Migrations

```bash
# Always test with fresh install
adb uninstall com.nihongo.conversation
./gradlew installDebug

# Check logs for migration errors
adb logcat -d | grep "Migration"
```

## Performance Tips

### Build Performance

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
org.gradle.caching=true
kotlin.incremental=true
```

### App Performance

- **Use `remember` for expensive calculations**
- **Avoid unnecessary recompositions**: Use `derivedStateOf`
- **Use `LazyColumn` for long lists**
- **Cache translations and API responses**
- **Index frequently queried database columns**

## Troubleshooting

### Build Issues

#### OutOfMemoryError

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

#### Kotlin Compilation Errors

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

#### Android SDK Issues

```bash
# Update build tools
sdkmanager "build-tools;34.0.0"

# Accept licenses
sdkmanager --licenses
```

### Runtime Issues

#### TTS Not Working
1. Go to device **Settings** → **Language & Input** → **Text-to-Speech**
2. Install Japanese voice data
3. Restart app

#### Database Migration Crash
```bash
# Clean reinstall
adb uninstall com.nihongo.conversation
./gradlew installDebug

# Check error logs
adb logcat -d | grep "IllegalStateException"
```

#### Network Errors
- Verify API keys in `local.properties`
- Check internet connection
- Verify `INTERNET` permission in `AndroidManifest.xml`

## Testing

### Unit Tests

```kotlin
// Example: ViewModel test
@Test
fun `sendMessage updates state correctly`() = runTest {
    val viewModel = ChatViewModel(mockRepository)

    viewModel.sendMessage("Hello")

    assertEquals("Hello", viewModel.uiState.value.lastMessage)
}
```

### Integration Tests

```kotlin
// Example: Database test
@Test
fun `insert and retrieve conversation`() = runTest {
    val conversation = Conversation(
        id = 1,
        title = "Test",
        userId = 1
    )

    dao.insert(conversation)
    val retrieved = dao.getConversation(1)

    assertEquals(conversation, retrieved)
}
```

### Running Tests

```bash
# Unit tests (fast, no device needed)
./gradlew test

# Instrumented tests (requires device)
./gradlew connectedAndroidTest

# Specific test
./gradlew test --tests ChatViewModelTest
```

## CI/CD (Optional)

### GitHub Actions Example

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew assembleDebug

    - name: Run tests
      run: ./gradlew test

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## Release Build

### 1. Generate Keystore

```bash
keytool -genkey -v -keystore release.keystore \
  -alias nihongo -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Configure Signing

Create `keystore.properties`:

```properties
storeFile=release.keystore
storePassword=your_store_password
keyAlias=nihongo
keyPassword=your_key_password
```

Add to `build.gradle.kts`:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

### 3. Build Release APK

```bash
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

## Resources

### Android Development
- [Android Developer Guide](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

### Libraries
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Retrofit](https://square.github.io/retrofit/)
- [Gemini API](https://ai.google.dev/docs)

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m "Add my feature"`
4. Push to branch: `git push origin feature/my-feature`
5. Open Pull Request

### PR Checklist
- [ ] Code follows Kotlin style guide
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Clean build succeeds
- [ ] No new warnings
