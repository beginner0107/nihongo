# Review Mode Implementation

## Summary
Implemented comprehensive review mode for reviewing past Japanese conversations with playback, date grouping, and important phrase extraction.

## New Files Created (2)

### 1. ReviewViewModel.kt (`presentation/review/`)
**Purpose**: Manage review mode state and data loading

**Features**:
- Load all user conversations from Room database
- Group conversations by date (today/yesterday/specific dates)
- Expand/collapse conversation details
- Extract important Japanese phrases (5 per conversation)
- Message playback via VoiceManager

**Data Models**:
```kotlin
data class ConversationWithDetails(
    val conversation: Conversation,
    val messages: List<Message>,
    val scenario: Scenario?,
    val isExpanded: Boolean = false
)

data class ConversationGroup(
    val dateHeader: String,           // "오늘", "어제", "2025년 10월 29일 (화)"
    val conversations: List<ConversationWithDetails>
)

data class ReviewUiState(
    val conversationGroups: List<ConversationGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedConversationIds: Set<Long> = emptySet()
)
```

**Key Methods**:
- `loadConversations()` - Fetch user conversations with messages and scenarios
- `groupConversationsByDate()` - Group by date with Korean labels
- `toggleConversationExpanded(id)` - Expand/collapse conversation
- `playMessage(text)` - TTS playback
- `extractImportantPhrases(messages)` - Extract 5 key Japanese phrases

### 2. ReviewScreen.kt (`presentation/review/`)
**Purpose**: Beautiful Material 3 review UI

**Components**:
- `ReviewScreen` - Main screen with TopAppBar and conversation list
- `DateHeader` - Date divider with horizontal lines
- `ConversationCard` - Expandable card with conversation details
- `ReviewMessageBubble` - Compact message bubble with play button
- `ImportantPhraseChip` - Highlighted phrase with TTS playback
- `DifficultyBadge` - Reusable scenario difficulty badge

**UI Features**:
- 📅 **Date Grouping**: Conversations grouped as "오늘", "어제", or "YYYY년 MM월 DD일 (E)"
- 🎭 **Scenario Badges**: Difficulty level (초급/중급/상급) with color coding
- 🎙️ **Message Playback**: Tap volume icon to play AI messages
- ⭐ **Important Phrases**: Top 5 Japanese phrases extracted per conversation
- ✨ **Smooth Animations**: Expand/collapse with fade + slide animations
- 💬 **Message Count**: Shows total messages when collapsed

## Updated Files (2)

### 1. NihongoNavHost.kt
**Added**:
- `Screen.Review` route object
- Review composable with navigation
- `onReviewClick` callback to ChatScreen

**Route Structure**:
```
ScenarioList (start)
    ├─ Settings
    ├─ Review  ← NEW
    └─ Chat
         └─ Review  ← Can navigate from chat
```

### 2. ChatScreen.kt
**Added**:
- `onReviewClick: () -> Unit` parameter
- Review button (HistoryEdu icon) in TopAppBar actions
- Icon import for HistoryEdu

**App Bar Layout**:
```
┌─────────────────────────────────┐
│ ← Chat Title       📚 🔊        │
│                    ↑  ↑         │
│                Review Auto-speak│
└─────────────────────────────────┘
```

## UI Design

```
┌─────────────────────────────────┐
│ ← 復習モード                    │
│   過去の会話を復習しましょう    │
├─────────────────────────────────┤
│                                 │
│ ━━━━━━ 오늘 ━━━━━━━━━━━━━━━  │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 💬 レストランでの注文       │ │
│ │    14:32              初級  │ │
│ │ ─────────────────────────  │ │
│ │ 📨 12 メッセージ            │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━━━ 어제 ━━━━━━━━━━━━━━━  │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 💬 買い物                   │ │
│ │    18:45              初級 ▼│ │
│ │ ─────────────────────────  │ │
│ │   [User bubble]             │ │
│ │             [AI bubble] 🔊  │ │
│ │   [User bubble]             │ │
│ │             [AI bubble] 🔊  │ │
│ │ ─────────────────────────  │ │
│ │ 💡 重要フレーズ             │ │
│ │ ⭐ いらっしゃいませ      🔊 │ │
│ │ ⭐ これをください        🔊 │ │
│ │ ⭐ ありがとうございます  🔊 │ │
│ └─────────────────────────────┘ │
│                                 │
│ ━━━━ 2025年 10月 28일 (월) ━━│
│ ...                             │
└─────────────────────────────────┘
```

## Features Breakdown

### 1. Date Grouping
**Implementation**:
- Uses `SimpleDateFormat` with Korean locale
- Special handling for today/yesterday
- Format: "YYYY년 MM월 DD일 (E)" for other dates
- Conversations sorted by most recent first

**Example Headers**:
```kotlin
"오늘"                      // Today
"어제"                      // Yesterday
"2025년 10월 28일 (월)"     // Specific date with day of week
```

### 2. Expandable Conversation Cards
**Collapsed State**:
- Scenario title + time
- Difficulty badge
- Message count
- Expand/collapse icon

**Expanded State**:
- All messages with user/AI bubbles
- Play button (🔊) on AI messages
- Important phrases section
- Smooth expand/collapse animation

### 3. Important Phrases Extraction
**Algorithm**:
```kotlin
fun extractImportantPhrases(messages: List<Message>): List<String> {
    return messages
        .filter { !it.isUser && it.content.isNotBlank() }  // AI messages only
        .flatMap { message ->
            message.content.split("。", "！", "？")         // Split by Japanese punctuation
                .map { it.trim() }
                .filter { it.length in 5..30 }             // Reasonable phrase length
        }
        .distinct()
        .take(5)                                           // Top 5 phrases
}
```

**Why This Works**:
- Extracts complete sentences from AI responses
- Filters by reasonable length (5-30 chars)
- Removes duplicates
- Focuses on AI's Japanese teaching

### 4. Message Playback
**Features**:
- Volume icon (🔊) on each AI message bubble
- Tap to play using VoiceManager TTS
- Respects current speech speed settings
- Same voice as chat mode

**UI Placement**:
```
┌────────────────────────┐
│ こんにちは！       🔊 │  ← AI bubble with play button
└────────────────────────┘
```

### 5. Color-Coded Difficulty Badges
**Reused Component** from ScenarioListScreen:
- **初級** (Level 1): Tertiary color (green tint)
- **中級** (Level 2): Secondary color (purple tint)
- **上級** (Level 3): Error color (red tint)

## Navigation Flow

```
ScenarioListScreen
    │
    └─ Select Scenario → ChatScreen
                            │
                            ├─ 📚 Review Button
                            │     ↓
                            │  ReviewScreen
                            │     │
                            │     └─ Expand Conversation
                            │          ├─ Play Messages
                            │          └─ Play Phrases
                            │
                            └─ ← Back → ScenarioListScreen
```

## Data Flow

### Loading Conversations
```
ReviewViewModel.init()
    ↓
loadConversations()
    ↓
repository.getUserConversations(userId)
    ↓
For each conversation:
    ├─ repository.getMessages(conversationId)
    └─ repository.getScenario(scenarioId)
    ↓
groupConversationsByDate()
    ↓
Update UI State
    ↓
ReviewScreen renders conversation list
```

### Playing Messages
```
User taps 🔊 on AI message
    ↓
onPlayMessage(message.content)
    ↓
ReviewViewModel.playMessage(text)
    ↓
VoiceManager.speak(text)
    ↓
TTS plays Japanese audio
```

## Integration with Existing Systems

### Room Database
**Uses Existing DAOs**:
- `ConversationDao.getConversationsByUser(userId)`
- `MessageDao.getMessagesByConversation(conversationId)`
- `ScenarioDao.getScenarioById(scenarioId)`

**No Schema Changes**: Works with existing database structure

### VoiceManager
**TTS Integration**:
- Same VoiceManager instance injected via Hilt
- Respects current speech speed from settings
- Uses Japanese (ja-JP) TTS voice

### Navigation System
**Clean Integration**:
- New `Screen.Review` route added
- Accessible from ChatScreen via TopAppBar
- Standard back navigation to previous screen

## Material Design 3 Elements

### Color Scheme
- **PrimaryContainer**: TopAppBar background (review mode)
- **OnPrimaryContainer**: TopAppBar text
- **PrimaryContainer (0.7 alpha)**: User message bubbles
- **SecondaryContainer (0.7 alpha)**: AI message bubbles
- **TertiaryContainer**: Important phrase chips
- **Tertiary**: Important phrases icon and title
- **SurfaceVariant**: Card backgrounds
- **Primary**: Date header text and dividers

### Typography
- **TitleLarge**: "復習モード" main title
- **BodySmall**: Subtitle and descriptions
- **TitleMedium**: Scenario titles
- **LabelLarge (Bold)**: Date headers
- **TitleSmall (Bold)**: "重要フレーズ" section title
- **BodyMedium**: Message content and phrases

### Components Used
- `Scaffold` + `TopAppBar`
- `LazyColumn` for scrolling list
- `Card` with elevation for conversations
- `Surface` for message bubbles and chips
- `IconButton` for playback controls
- `HorizontalDivider` for sections
- `AnimatedVisibility` for expand/collapse

## Performance Optimizations

### Lazy Loading
```kotlin
LazyColumn {
    uiState.conversationGroups.forEach { group ->
        item(key = "header_${group.dateHeader}") { ... }
        items(items = group.conversations, key = { it.conversation.id }) { ... }
    }
}
```
- Only visible conversations rendered
- Stable keys for efficient recomposition
- Smooth scrolling even with many conversations

### Efficient State Management
```kotlin
private val _uiState = MutableStateFlow(ReviewUiState())
val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
```
- Single StateFlow for all UI state
- Minimal recompositions
- Efficient expand/collapse tracking with Set<Long>

### Important Phrases Caching
- Calculated once per conversation
- Passed as parameter to avoid recalculation
- Distinct() removes duplicates efficiently

## Empty States

### No Conversations
```
         🎓
   まだ会話がありません
シナリオから会話を始めましょう！
```
- Friendly icon (HistoryEdu)
- Clear message in Japanese
- Guidance to start a conversation

### Loading State
```
         ⏳
     (Loading spinner)
```

### Error State
```
         ⚠️
  大話記録を不러올 수 없습니다
        (error message)
```

## Accessibility

### Features
- All icons have `contentDescription`
- Large touch targets (48dp IconButtons)
- Clear visual hierarchy
- Color + text for difficulty levels
- Screen reader friendly

### Keyboard Navigation
- Expandable cards via tap/click
- Play buttons accessible
- Back button in TopAppBar

## Localization

### Mixed Language Strategy
**Japanese** (UI Labels):
- 復習モード (Review Mode)
- 過去の会話を復習しましょう (Let's review past conversations)
- まだ会話がありません (No conversations yet)
- メッセージ (messages)
- 重要フレーズ (Important phrases)

**Korean** (Date Headers):
- 오늘 (Today)
- 어제 (Yesterday)
- 2025년 10월 29일 (화) (Date format)

**Why Mixed**:
- User is Korean learning Japanese
- Japanese UI helps immersion
- Korean dates for clarity

## Testing Checklist

### Basic Functionality
- [ ] Open review mode from ChatScreen
- [ ] View grouped conversations by date
- [ ] Expand/collapse conversation cards
- [ ] Play AI messages via volume icon
- [ ] Play important phrases
- [ ] Navigate back to chat
- [ ] Handle empty state (no conversations)
- [ ] Handle loading state
- [ ] Handle error state

### Data Accuracy
- [ ] Correct date grouping (today/yesterday/dates)
- [ ] Messages ordered correctly (newest first)
- [ ] Scenario info displays correctly
- [ ] Difficulty badges match scenarios
- [ ] Important phrases extracted properly
- [ ] Message count accurate

### UX Polish
- [ ] Smooth expand/collapse animations
- [ ] TTS playback at correct speed
- [ ] Scroll performance with many conversations
- [ ] Cards layout properly
- [ ] Icons and colors correct
- [ ] Text readable and aligned

## Future Enhancements

### Analytics
- [ ] Track most reviewed conversations
- [ ] Identify commonly practiced scenarios
- [ ] Measure learning progress over time

### Enhanced Phrase Extraction
- [ ] Use AI to extract grammatically important phrases
- [ ] Categorize by JLPT level
- [ ] Add translations for important phrases
- [ ] Romaji pronunciation guide

### Search & Filter
- [ ] Search conversations by keyword
- [ ] Filter by scenario type
- [ ] Filter by difficulty level
- [ ] Filter by date range

### Export & Share
- [ ] Export conversation as PDF
- [ ] Share important phrases
- [ ] Create flashcards from phrases
- [ ] Bookmark favorite conversations

### Study Tools
- [ ] Quiz mode from past conversations
- [ ] Spaced repetition for important phrases
- [ ] Progress tracking per scenario
- [ ] Achievement badges

### Advanced Playback
- [ ] Play entire conversation sequentially
- [ ] Adjust playback speed per message
- [ ] Record user voice and compare
- [ ] Slow-motion mode for beginners

## Architecture Impact

### Clean Architecture Preserved
- **Presentation**: ReviewScreen, ReviewViewModel (UI logic)
- **Data**: ConversationRepository (data access)
- **Domain**: Existing models reused (Conversation, Message, Scenario)

### Dependencies
```
ReviewViewModel ──→ ConversationRepository ──→ Room DAOs
      ↓
  VoiceManager (TTS)
```

### Separation of Concerns
- ReviewViewModel handles business logic
- ReviewScreen handles UI only
- Repository abstracts data source
- VoiceManager handles TTS independently

## Project Structure

```
app/src/main/java/com/nihongo/conversation/
├── presentation/
│   ├── review/                      ← NEW DIRECTORY
│   │   ├── ReviewScreen.kt          ← NEW (480+ lines)
│   │   └── ReviewViewModel.kt       ← NEW (160+ lines)
│   ├── chat/
│   │   └── ChatScreen.kt            ← UPDATED (review button)
│   └── navigation/
│       └── NihongoNavHost.kt        ← UPDATED (review route)
└── data/repository/
    └── ConversationRepository.kt    ← No changes (uses existing methods)
```

**Total New Code**: 640+ lines
**Files Created**: 2
**Files Modified**: 2

---

## Summary

✅ **Complete Review Mode Implementation**

**Key Achievements**:
1. 📚 Comprehensive conversation review UI
2. 📅 Smart date grouping with Korean labels
3. 🎭 Expandable conversation cards with animations
4. 🔊 Message playback for all AI responses
5. ⭐ Automatic important phrase extraction
6. 🎨 Beautiful Material 3 design
7. 🔄 Seamless integration with existing architecture
8. ♿ Accessible and user-friendly
9. 🚀 Performant lazy loading
10. 🌐 Mixed Japanese/Korean localization

**User Experience**:
- Review past conversations organized by date
- Expand to see full message history
- Play AI messages to practice listening
- Highlight and practice important Japanese phrases
- Navigate easily from chat mode
- Beautiful, polished UI with smooth animations

**Technical Excellence**:
- Clean architecture maintained
- Reused existing Room database queries
- Efficient StateFlow state management
- Lazy loading for performance
- Material 3 design system
- No breaking changes to existing code

The review mode is now **ready for testing** and provides a complete, polished learning experience! 🎉
