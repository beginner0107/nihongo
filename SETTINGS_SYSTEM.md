# Settings System Implementation

## Summary
Successfully implemented comprehensive settings screen with difficulty level control, speech speed adjustment, and user preferences using DataStore for persistence.

## New Files Created (4)

### 1. UserSettings.kt (`domain/model/`)
**Purpose**: Data model for user preferences
```kotlin
data class UserSettings(
    val difficultyLevel: Int = 1,      // 1-3 (初級/中級/上級)
    val speechSpeed: Float = 1.0f,     // 0.5 - 2.0x
    val autoSpeak: Boolean = true,
    val showRomaji: Boolean = true
)
```

### 2. SettingsDataStore.kt (`data/local/`)
**Purpose**: Persistent storage using DataStore Preferences

**Features**:
- Reactive Flow-based preferences
- Automatic persistence
- Type-safe preference keys
- Error handling with IOException catch

**Methods**:
- `userSettings: Flow<UserSettings>` - Reactive settings stream
- `updateDifficultyLevel(Int)` - Update difficulty
- `updateSpeechSpeed(Float)` - Update TTS speed
- `updateAutoSpeak(Boolean)` - Toggle auto-speak
- `updateShowRomaji(Boolean)` - Toggle romaji display

### 3. SettingsViewModel.kt (`presentation/settings/`)
**Purpose**: Settings state management

**Features**:
- Exposes `StateFlow<UserSettings>`
- Delegates updates to DataStore
- Lifecycle-aware with ViewModel scope

### 4. SettingsScreen.kt (`presentation/settings/`)
**Purpose**: Beautiful Material 3 settings UI

**Components**:
- `SettingsScreen` - Main screen with sections
- `SettingsSection` - Reusable section with icon
- `DifficultySlider` - 3-level slider with badges
- `SpeechSpeedSlider` - Float slider (0.5x - 2.0x)
- `SettingsToggle` - Switch with description

## Updated Files (5)

### 1. build.gradle.kts
**Added**:
```kotlin
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

### 2. VoiceManager.kt
**Enhanced**:
- Added `speed` parameter to `speak()` method
- Added `setSpeechSpeed(Float)` method
- Dynamic TTS rate control (0.5x - 2.0x)

### 3. ChatViewModel.kt
**Changes**:
- Injected `SettingsDataStore`
- Added `observeSettings()` to sync with DataStore
- Auto-speak uses settings value
- Speech speed applied to all TTS calls
- `toggleAutoSpeak()` persists to settings

### 4. NihongoNavHost.kt
**Added**:
- `Screen.Settings` route
- Settings composable navigation
- Back navigation support

### 5. ScenarioListScreen.kt
**Added**:
- Settings icon in TopAppBar
- `onSettingsClick` callback parameter

## Settings Features

### 1. Difficulty Level (難易度レベル)
**Control**: Slider with 3 levels
- **初級 (Level 1)**: Beginner - Simple Japanese
- **中級 (Level 2)**: Intermediate - Natural conversations
- **上級 (Level 3)**: Advanced - Complex, formal language

**Visual**:
- Color-coded badges (green/purple/red)
- Large slider with step labels
- Current level displayed

### 2. Speech Speed (音声速度)
**Control**: Continuous slider
- Range: 0.5x - 2.0x
- Steps: 0.1x increments
- Default: 1.0x (normal speed)

**Labels**:
- 遅い (0.5x) - Slow
- 普通 (1.0x) - Normal
- 速い (2.0x) - Fast

**Real-time**: Immediately applied to all TTS

### 3. Auto-Speak (自動読み上げ)
**Control**: Switch toggle
- Default: ON
- Description: "AIの返信を自動的に音声で読み上げます"
- Persists across sessions

### 4. Romaji Display (ローマ字表示)
**Control**: Switch toggle
- Default: ON
- Description: "ヒントにローマ字を表示します"
- For future hint enhancement

## UI Design

```
┌─────────────────────────────────┐
│ ← 設定                          │
├─────────────────────────────────┤
│                                 │
│ 📈 難易度レベル                 │
│ ┌─────────────────────────────┐ │
│ │ 中級 (Intermediate) Level 2 │ │
│ │ ━━━●━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 初級    中級    上級        │ │
│ └─────────────────────────────┘ │
│                                 │
│ ───────────────────────────────│
│                                 │
│ ⚡ 音声速度                     │
│ ┌─────────────────────────────┐ │
│ │ 速度                  1.2x  │ │
│ │ ━━━━━━●━━━━━━━━━━━━━━━━━ │ │
│ │ 遅い  普通  速い            │ │
│ └─────────────────────────────┘ │
│                                 │
│ ───────────────────────────────│
│                                 │
│ 🔊 音声設定                     │
│ ┌─────────────────────────────┐ │
│ │ AI応答の自動読み上げ    ●━ │ │
│ │ AIの返信を自動的に...       │ │
│ └─────────────────────────────┘ │
│                                 │
│ ───────────────────────────────│
│                                 │
│ 🌐 表示設定                     │
│ ┌─────────────────────────────┐ │
│ │ ローマ字表示            ●━ │ │
│ │ ヒントにローマ字を...       │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ℹ️ 設定はすべての会話に     │ │
│ │   適用されます...           │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Navigation Flow

```
ScenarioListScreen
    │
    ├─ ⚙️ Settings Icon (TopAppBar)
    │     ↓
    │  SettingsScreen
    │     ↓ (← back button)
    │  ScenarioListScreen
    │
    └─ Select Scenario → ChatScreen
                          (uses settings)
```

## Data Flow

### Settings Persistence
```
SettingsScreen
    ↓ (user interaction)
SettingsViewModel
    ↓
SettingsDataStore
    ↓
DataStore Preferences (persistent)
    ↓ (Flow)
ChatViewModel (observes)
    ↓
VoiceManager + UI State
```

### Real-Time Updates
```
User changes speed slider in Settings
    ↓
SettingsDataStore.updateSpeechSpeed()
    ↓
DataStore emits new settings
    ↓
ChatViewModel.observeSettings() receives update
    ↓
VoiceManager.setSpeechSpeed() called
    ↓
Next TTS uses new speed
```

## Integration with Existing Features

### Voice System (STT/TTS)
**Speech Speed**:
- Applied to all `speak()` calls
- Affects AI responses, hint playback, message replay
- Range: 0.5x (slow for beginners) to 2.0x (fast for advanced)

**Auto-Speak**:
- Controls automatic AI response playback
- Can be toggled in Settings or ChatScreen
- Synced across both UIs

### Hint System
**Show Romaji Setting**:
- Currently prepared for future enhancement
- Will control romaji visibility in HintDialog
- Default: enabled for beginners

### Difficulty Level
**Current**: Stored in settings
**Future Use**:
- Adjust AI response complexity
- Filter hint suggestions
- Customize scenario difficulty
- Adaptive learning path

## Technical Implementation

### DataStore Preferences
```kotlin
private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings")

val userSettings: Flow<UserSettings> = context.dataStore.data
    .catch { if (it is IOException) emit(emptyPreferences()) }
    .map { preferences ->
        UserSettings(
            difficultyLevel = preferences[DIFFICULTY_LEVEL] ?: 1,
            speechSpeed = preferences[SPEECH_SPEED] ?: 1.0f,
            // ...
        )
    }
```

### State Management
```kotlin
// SettingsViewModel
val userSettings: StateFlow<UserSettings> =
    settingsDataStore.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

// ChatViewModel
private fun observeSettings() {
    viewModelScope.launch {
        settingsDataStore.userSettings.collect { settings ->
            _uiState.update {
                it.copy(
                    autoSpeak = settings.autoSpeak,
                    speechSpeed = settings.speechSpeed
                )
            }
            voiceManager.setSpeechSpeed(settings.speechSpeed)
        }
    }
}
```

### UI Components

**Difficulty Slider**:
```kotlin
Slider(
    value = value.toFloat(),
    onValueChange = { onValueChange(it.roundToInt()) },
    valueRange = 1f..3f,
    steps = 1
)
```

**Speech Speed Slider**:
```kotlin
Slider(
    value = value,
    onValueChange = onValueChange,
    valueRange = 0.5f..2.0f,
    steps = 14  // 0.1x increments
)
```

**Toggle Switch**:
```kotlin
Switch(
    checked = checked,
    onCheckedChange = onCheckedChange
)
```

## Material Design 3

### Color System
- **Primary**: Icons, active states
- **Tertiary**: Beginner (Level 1) badge
- **Secondary**: Intermediate (Level 2) badge
- **Error**: Advanced (Level 3) badge
- **SurfaceVariant**: Section backgrounds
- **PrimaryContainer**: Speed badge background

### Typography
- **TitleMedium (Bold)**: Section headers
- **BodyLarge (Medium)**: Setting labels
- **BodySmall**: Descriptions, slider labels
- **LabelMedium (Bold)**: Badge text

### Layout
- Vertical scroll for settings list
- 24dp spacing between sections
- 12dp internal spacing
- HorizontalDivider between sections
- Card for info message

## Persistence

### Storage Location
```
/data/data/com.nihongo.conversation/files/datastore/settings.preferences_pb
```

### Data Format
Protocol Buffers (binary)

### Lifecycle
- Persists across app restarts
- Survives app updates
- Cleared only on app uninstall

### Migration
- No migration needed (new feature)
- Defaults applied for new users
- Backwards compatible

## Performance

### Optimization
- `SharingStarted.WhileSubscribed(5000)` - Stops collection 5s after no subscribers
- DataStore reads are cached
- Writes are debounced automatically
- Flow-based reactive updates (no polling)

### Memory
- Minimal overhead (~KB)
- Settings cached in StateFlow
- No database queries

## Testing Checklist

- [ ] Open Settings from Scenario screen
- [ ] Adjust difficulty slider (1-3)
- [ ] Adjust speech speed slider (0.5-2.0)
- [ ] Toggle auto-speak on/off
- [ ] Toggle romaji on/off
- [ ] Go back to scenario list
- [ ] Start chat and verify TTS speed
- [ ] Toggle auto-speak in chat
- [ ] Return to settings - verify persistence
- [ ] Restart app - verify settings saved
- [ ] Test with different speeds (slow/fast)

## Future Enhancements

### Difficulty Integration
- [ ] Pass difficulty to AI prompts
- [ ] Adjust vocabulary complexity
- [ ] Modify sentence structure
- [ ] Filter scenario recommendations

### Additional Settings
- [ ] Theme selection (light/dark/auto)
- [ ] Font size adjustment
- [ ] Notification preferences
- [ ] Backup & restore
- [ ] Export conversation history
- [ ] Voice selection (male/female)
- [ ] Pitch control
- [ ] Language preferences
- [ ] Study goals and reminders

### Advanced Features
- [ ] Custom difficulty profiles
- [ ] Per-scenario speed overrides
- [ ] Adaptive difficulty (auto-adjust based on performance)
- [ ] A/B testing for optimal settings
- [ ] Analytics dashboard

## Accessibility

### Features
- Large touch targets (48dp minimum)
- Clear visual hierarchy
- Color + text for all indicators
- Descriptive labels for screen readers
- Keyboard navigation support (future)

### Screen Reader Support
- All icons have contentDescription
- Slider values announced
- Switch states verbalized
- Section headers marked

## Localization

### Current Text (Japanese)
- 設定 (Settings)
- 難易度レベル (Difficulty Level)
- 音声速度 (Speech Speed)
- 音声設定 (Voice Settings)
- 表示設定 (Display Settings)

### Levels
- 初級 (Beginner)
- 中級 (Intermediate)
- 上級 (Advanced)

### Speed Labels
- 遅い (Slow)
- 普通 (Normal)
- 速い (Fast)

## Architecture Impact

### Clean Architecture Preserved
- **Domain**: UserSettings model (pure data)
- **Data**: SettingsDataStore (storage)
- **Presentation**: SettingsScreen, SettingsViewModel (UI logic)

### Dependencies
```
ChatViewModel ──→ SettingsDataStore
                       ↓
                  DataStore Preferences
```

### Separation of Concerns
- Settings UI independent of chat
- DataStore handles persistence
- ViewModels mediate between UI and data
- Settings reactive via Flow

## Project Structure

```
app/src/main/java/com/nihongo/conversation/
├── domain/model/
│   └── UserSettings.kt               ← NEW
├── data/local/
│   └── SettingsDataStore.kt          ← NEW
├── presentation/
│   ├── settings/                     ← NEW DIRECTORY
│   │   ├── SettingsScreen.kt         ← NEW
│   │   └── SettingsViewModel.kt      ← NEW
│   ├── scenario/
│   │   └── ScenarioListScreen.kt     ← UPDATED (settings icon)
│   ├── chat/
│   │   └── ChatViewModel.kt          ← UPDATED (settings integration)
│   └── navigation/
│       └── NihongoNavHost.kt         ← UPDATED (settings route)
└── core/voice/
    └── VoiceManager.kt                ← UPDATED (speech speed)
```

**Total Files**: 30 Kotlin files (4 new, 5 updated)

---

**Status**: ✅ Complete and Ready for Testing

Comprehensive settings system successfully implemented with DataStore persistence!
