# Hint System Implementation (Korean-Japanese Translation)

## Summary
Successfully implemented AI-powered hint system with Korean-to-Japanese translations to help users learn appropriate Japanese expressions during conversations.

## New Files Created

### 1. Hint.kt (`domain/model/`)
**Purpose**: Data models for hints
```kotlin
data class Hint(
    val japanese: String,      // 日本語表現
    val korean: String,        // 한국어 번역
    val romaji: String?,       // ローマ字
    val explanation: String?   // 使用状況説明
)

data class HintRequest(
    val context: String,
    val userLevel: Int
)
```

### 2. HintDialog.kt (`presentation/chat/`)
**Purpose**: Beautiful dialog UI for displaying hints

**Components**:
- `HintDialog`: Main dialog with loading state
- `HintCard`: Individual hint card with:
  - Japanese text (clickable to use)
  - Romaji pronunciation
  - Korean translation with 🇰🇷 flag
  - Explanation in gray box
  - Speaker icon to hear pronunciation

**Features**:
- Loading indicator while fetching hints
- Error state handling
- Scrollable list of hints
- Click card to populate input field
- Click speaker icon to hear Japanese pronunciation

## Updated Files

### GeminiApiService.kt
**Added Methods**:
- `generateHints(conversationContext, userLevel)`: Generate contextual hints using Gemini AI
- `parseHintsFromJson(jsonText)`: Parse JSON response into Hint objects

**Prompt Engineering**:
```kotlin
"""
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
"""
```

**Fallback Hints**: Provides default hints if API fails:
- すみません (sumimasen)
- お願いします (onegaishimasu)
- ありがとうございます (arigatou gozaimasu)

### ConversationRepository.kt
**Added Method**:
- `getHints(conversationHistory, userLevel)`: Get hints based on recent conversation context (last 5 messages)

### ChatViewModel.kt
**Updated ChatUiState**:
```kotlin
data class ChatUiState(
    // ... existing fields ...
    val hints: List<Hint> = emptyList(),
    val isLoadingHints: Boolean = false,
    val showHintDialog: Boolean = false
)
```

**New Methods**:
- `requestHints()`: Fetch hints from Gemini API
- `dismissHintDialog()`: Close hint dialog
- `useHint(hint)`: Populate input field with selected hint

### ChatScreen.kt
**Changes**:
- Added lightbulb icon import
- Added `HintDialog` at the end of Scaffold
- Updated `MessageInput` to include hint button
- Wired up hint callbacks to ViewModel

**New UI**:
```
MessageInput
├── Row
│   ├── VoiceButton
│   ├── OutlinedTextField
│   └── Send Button
└── TextButton (힌트 요청 Korean-Japanese)
```

## Features

### 1. Contextual Hint Generation
- Analyzes last 5 messages in conversation
- Considers user level
- Generates 3 relevant Japanese expressions
- Provides Korean translations and explanations

### 2. AI-Powered Suggestions
- Uses Gemini API for intelligent suggestions
- Adapts to conversation context
- Tailored to scenario and situation

### 3. Multi-Language Support
- **Japanese**: 日本語表現
- **Korean**: 한국어 번역
- **Romaji**: ローマ字 pronunciation guide

### 4. Interactive Hints
- **Click Card**: Populate input field with Japanese text
- **Click Speaker Icon**: Hear Japanese pronunciation (TTS)
- **Visual Feedback**: Material 3 design with clear hierarchy

### 5. Error Handling
- Loading state while fetching
- Fallback hints on API failure
- Graceful error messages

## User Flow

### Requesting Hints
1. User clicks "힌트 요청 (Korean-Japanese)" button
2. System shows loading dialog
3. Gemini generates 3 contextual hints
4. Hints displayed in dialog with:
   - Japanese expression (bold, primary color)
   - Romaji pronunciation
   - Korean translation with flag
   - Explanation in info box

### Using Hints
**Method 1: Click to Use**
1. Click on any hint card
2. Japanese text populates input field
3. Dialog closes
4. User can edit or send directly

**Method 2: Listen and Type**
1. Click speaker icon to hear pronunciation
2. Listen to Japanese TTS
3. Type manually or use voice input
4. Close dialog manually

## UI/UX Design

### HintDialog
```
┌─────────────────────────────┐
│ 💡 힌트               ✕     │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ すみません          🔊  │ │ <- Japanese + Speaker
│ │ sumimasen                │ │ <- Romaji
│ │ ────────────────────     │ │
│ │ 🇰🇷 죄송합니다 / 실례  │ │ <- Korean
│ │ ┌─────────────────────┐ │ │
│ │ │ 사람을 부르거나     │ │ │ <- Explanation
│ │ │ 사과할 때 사용      │ │ │
│ │ └─────────────────────┘ │ │
│ └─────────────────────────┘ │
│ ... 2 more hints ...        │
└─────────────────────────────┘
```

### Hint Button
- Icon: 💡 Lightbulb
- Text: "힌트 요청 (Korean-Japanese)"
- Position: Below message input
- Style: TextButton (subtle, non-intrusive)

## Technical Implementation

### Data Flow
```
User → ChatScreen → ChatViewModel → ConversationRepository
                                  → GeminiApiService
                                  → Parse JSON
                                  → Return Hints
                                  → Update UI State
                                  → Show HintDialog
```

### State Management
```kotlin
// Request hints
_uiState.update {
    it.copy(
        isLoadingHints = true,
        showHintDialog = true
    )
}

// Display hints
_uiState.update {
    it.copy(
        hints = hints,
        isLoadingHints = false
    )
}

// Use hint
_uiState.update {
    it.copy(
        inputText = hint.japanese,
        showHintDialog = false
    )
}
```

### JSON Parsing
- Handles markdown code blocks (```json)
- Safe parsing with try-catch
- Returns empty list on parse error
- Fallback hints on exception

## Example Hints

**Restaurant Scenario**:
```json
[
  {
    "japanese": "ラーメンをお願いします",
    "korean": "라멘 주세요",
    "romaji": "raamen wo onegaishimasu",
    "explanation": "음식을 주문할 때 사용하는 표현"
  },
  {
    "japanese": "いくらですか？",
    "korean": "얼마예요?",
    "romaji": "ikura desu ka",
    "explanation": "가격을 물어볼 때 사용"
  },
  {
    "japanese": "おいしいです",
    "korean": "맛있어요",
    "romaji": "oishii desu",
    "explanation": "음식이 맛있을 때 표현"
  }
]
```

## Integration Points

### Gemini API
- Uses `generateContent()` for hint generation
- Single API call per hint request
- Context-aware prompt engineering

### TTS Integration
- Hints can be spoken via speaker icon
- Reuses existing VoiceManager
- Japanese pronunciation playback

### Voice Input
- Works alongside STT feature
- User can speak or use hints
- Complementary learning methods

## Benefits for Learners

### 1. Learning Support
- Contextual suggestions reduce anxiety
- Korean translations aid understanding
- Romaji helps pronunciation

### 2. Conversation Flow
- Quick access to relevant expressions
- Maintains conversation momentum
- Reduces lookup time

### 3. Progressive Learning
- Adapts to user level
- Reinforces common patterns
- Introduces new vocabulary in context

### 4. Multi-Modal Learning
- Visual (text)
- Auditory (TTS)
- Interactive (click to use)

## Performance

### Optimization
- Lazy hint loading (on demand only)
- Context limited to last 5 messages
- Efficient JSON parsing
- Material 3 animations

### Caching
- No caching (always fresh hints)
- Context changes frequently
- Quick API response (~2-3 seconds)

## Error Handling

### API Failures
```kotlin
try {
    // Generate hints
} catch (e: Exception) {
    // Return fallback hints
    listOf(
        Hint("すみません", "죄송합니다", "sumimasen", ...),
        Hint("お願いします", "부탁합니다", "onegaishimasu", ...),
        Hint("ありがとうございます", "감사합니다", "arigatou gozaimasu", ...)
    )
}
```

### UI States
- **Loading**: Spinner + "힌트를 생성하고 있습니다..."
- **Empty**: "힌트를 가져올 수 없습니다" (error)
- **Success**: Scrollable list of hints

## Testing Checklist

- [ ] Click hint button
- [ ] Verify loading state
- [ ] Verify 3 hints displayed
- [ ] Click hint card to populate input
- [ ] Click speaker icon to hear pronunciation
- [ ] Send message using hint
- [ ] Request hints in different scenarios
- [ ] Test with network error
- [ ] Verify fallback hints
- [ ] Close dialog manually

## Future Enhancements

- [ ] Hint history/favorites
- [ ] User-customizable hints
- [ ] Grammar explanations
- [ ] Difficulty filtering
- [ ] Hint usage statistics
- [ ] Offline hint database
- [ ] Hint categories (greetings, questions, etc.)
- [ ] Save used hints for review

## Files Modified/Created

### New Files (2)
1. `domain/model/Hint.kt`
2. `presentation/chat/HintDialog.kt`

### Modified Files (4)
1. `data/remote/GeminiApiService.kt` (+80 lines)
2. `data/repository/ConversationRepository.kt` (+12 lines)
3. `presentation/chat/ChatViewModel.kt` (+40 lines)
4. `presentation/chat/ChatScreen.kt` (+30 lines)

**Total**: ~250 lines of new code

## Architecture Impact

### Clean Architecture Maintained
- **Domain**: Hint model (data structure)
- **Data**: GeminiApiService, Repository (data access)
- **Presentation**: HintDialog, ChatViewModel (UI logic)

### Dependencies
- No new external dependencies
- Uses existing Gemini SDK
- Uses existing VoiceManager for TTS
- Pure Kotlin/Compose implementation

---

**Status**: ✅ Complete and Ready for Testing

Hint system successfully integrated with Korean-Japanese translations!
