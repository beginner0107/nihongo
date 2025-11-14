# NihonGo Conversation

AI-powered Japanese conversation learning app for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-2.5%20Flash-purple.svg)](https://ai.google.dev)

## Features

- **AI Conversation Partner** - Natural Japanese dialogue powered by Gemini 2.5 Flash
- **126+ Scenarios** - Entertainment, work, travel, JLPT practice, and custom scenarios
- **Voice-Only Mode** - Practice conversation without text distractions
- **Advanced Pronunciation Analysis** - Pitch accent, intonation, rhythm, and problematic sounds detection
- **Smart Translation** - 3-provider system (Microsoft Translator, DeepL, ML Kit) with automatic fallback
- **Spaced Repetition** - SM-2 algorithm with 4 practice modes (read, listen, fill-in, speak)
- **Grammar Feedback** - Real-time error detection and natural expression suggestions
- **Multi-User Support** - Separate profiles with personalized difficulty levels

## Quick Start

### Requirements

- Android Studio Hedgehog (2023.1.1) or higher
- Android SDK 24+
- Gemini API key ([Get one here](https://makersuite.google.com/app/apikey))

### Installation

```bash
git clone https://github.com/yourusername/nihongo-conversation.git
cd nihongo-conversation
```

Create `local.properties` file:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
# Optional translation providers
MICROSOFT_TRANSLATOR_KEY=your_microsoft_key_here
DEEPL_API_KEY=your_deepl_key_here
```

Build and run:

```bash
./gradlew assembleDebug
```

Or run directly from Android Studio.

## Architecture

Built with modern Android development practices:

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room + Paging 3
- **AI**: Gemini 2.5 Flash (streaming)
- **Voice**: Android TTS/STT
- **Translation**: Multi-provider fallback chain

```
app/
├── data/           # Repositories, Room DB, API clients
├── domain/         # Business logic, models
├── presentation/   # Compose UI, ViewModels
└── core/           # DI, utilities
```

## Documentation

- [Features Guide](docs/FEATURES.md) - Detailed feature overview
- [Architecture](docs/ARCHITECTURE.md) - Technical architecture and design decisions
- [Development](docs/DEVELOPMENT.md) - Setup and build instructions
- [API Integration](docs/API.md) - Gemini, Microsoft, DeepL setup
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions
- [Changelog](docs/CHANGELOG.md) - Version history

## Key Technologies

### Gemini 2.5 Flash API
- Streaming responses for low latency
- Context-aware conversations
- Grammar analysis and hints

### 3-Provider Translation System
1. **Microsoft Translator** (Primary) - 2M chars/month free
2. **DeepL** (Fallback) - 500K chars/month free
3. **ML Kit** (Offline) - Unlimited, on-device

Automatic fallback chain ensures translation always works, even offline.

## Performance

| Metric | Value |
|--------|-------|
| First AI response | ~800ms |
| Translation (cached) | <10ms (95% hit rate) |
| Translation (API) | 200-600ms |
| UI rendering | 60fps |
| Database queries | <50ms |
| Memory usage | ~120MB |

## Development

```bash
# Build
./gradlew assembleDebug

# Test
./gradlew test

# Clean build
./gradlew clean assembleDebug

# Install and run
./gradlew installDebug
adb shell am start -n com.nihongo.conversation/.MainActivity
```

See [DEVELOPMENT.md](docs/DEVELOPMENT.md) for detailed setup instructions.

## Troubleshooting

Common issues:

- **TTS not working**: Install Japanese voice data in device settings
- **Migration errors**: Clean reinstall with `adb uninstall com.nihongo.conversation`
- **Build errors**: Set `org.gradle.jvmargs=-Xmx4096m` in `gradle.properties`

See [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for complete guide.

## Roadmap

- [ ] Offline AI mode (Gemini Nano)
- [ ] Web version (Kotlin Multiplatform)
- [ ] iOS app
- [ ] Voice chat rooms
- [ ] Custom scenario creator

## License

Personal learning project.

## Acknowledgments

- [Gemini API](https://ai.google.dev) - AI conversation
- [Microsoft Translator](https://azure.microsoft.com/products/ai-services/ai-translator) - Translation
- [DeepL](https://www.deepl.com) - High-quality translation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI
