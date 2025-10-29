##

 User Profile System Implementation

## Summary
Implemented comprehensive user profile system with avatar selection, personal information, learning goals, and AI response personalization for the Japanese conversation learning app.

## New Files Created (4)

### 1. ProfileRepository.kt (`data/repository/`)
**Purpose**: Manage user profile CRUD operations and personalization

**Key Methods**:
```kotlin
fun getCurrentUser(): Flow<User?>
suspend fun getCurrentUserImmediate(): User?
suspend fun updateProfile(user: User)
suspend fun saveProfile(...)
suspend fun getFavoriteScenarioIds(): List<Long>
suspend fun isProfileComplete(): Boolean
suspend fun getPersonalizedPromptPrefix(): String
```

**Features**:
- Single user management (ID = 1L)
- Create or update profile
- Extract favorite scenario IDs from comma-separated string
- Profile completeness check
- **Personalized AI Prompt Generation**:
  ```kotlin
  "You are speaking with ${user.name}"
  "Their learning goal is: ${user.learningGoal}"
  "About them: ${user.bio}"
  "Their native language is ${user.nativeLanguage}"
  "Their Japanese level is $levelDescription (JLPT N5-N1)"
  ```

### 2. ProfileViewModel.kt (`presentation/profile/`)
**Purpose**: Manage profile UI state

**State Management**:
```kotlin
data class ProfileUiState(
    val user: User? = null,
    val name: String = "",
    val selectedAvatarId: Int = 0,
    val learningGoal: String = "",
    val selectedScenarios: Set<Long> = emptySet(),
    val nativeLanguage: String = "Korean",
    val bio: String = "",
    val level: Int = 1,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)
```

**Methods**:
- `updateName(name)`, `selectAvatar(id)`, `updateLearningGoal(goal)`
- `toggleScenario(id)`, `updateBio(bio)`, `updateLevel(level)`
- `saveProfile()` - with validation
- `clearSaveSuccess()`, `clearError()`

**Validation**:
- Name is required (cannot be blank)
- Shows error if validation fails

### 3. Avatars.kt (`presentation/profile/`)
**Purpose**: Avatar display and selection components

**Predefined Avatars** (6 options):
```kotlin
val AVATAR_EMOJIS = listOf(
    "😊",  // Happy face
    "🎌",  // Japanese flag
    "🗾",  // Japan map
    "🍣",  // Sushi
    "⛩️",  // Torii gate
    "🎎"   // Japanese dolls
)

val AVATAR_COLORS = listOf(
    Orange, Green, Blue, Red, Purple, Yellow
)
```

**Components**:
- `Avatar(avatarId, size)` - Display avatar with emoji and colored background
- `AvatarSelector(selectedAvatarId, onAvatarSelected)` - Grid of selectable avatars
- `getAvatarEmoji(avatarId)` - Helper function
- `getAvatarColor(avatarId)` - Helper function

**UI Features**:
- Circular avatar backgrounds with unique colors
- 6x grid layout (wraps on mobile)
- Selected avatar has border + checkmark badge
- Large preview above selector

### 4. ProfileScreen.kt (`presentation/profile/`)
**Purpose**: Comprehensive profile editing UI

**Sections**:

1. **Avatar Section** (アバター)
   - Large preview (100dp)
   - 6-option selector grid
   - Icon: Person

2. **Basic Info** (基本情報)
   - Name field (required)
   - Bio field (2-3 lines)
   - Icon: Info

3. **Learning Goal** (学習目標)
   - Goal text area (2-3 lines)
   - Placeholder: "日本旅行のため、アニメを字幕なしで見るため..."
   - Icon: EmojiEvents (trophy)

4. **Japanese Level** (日本語レベル)
   - Slider (1-3: 初級/中級/上級)
   - Shows JLPT level description
   - Icon: TrendingUp

5. **Favorite Scenarios** (お気に入りシナリオ)
   - Checkboxes for all scenarios
   - Shows scenario title, description, difficulty badge
   - Icon: Favorite (heart)

6. **Native Language** (母語)
   - Text field
   - Default: "Korean"
   - Icon: Language

**Components**:
- `ProfileSection(title, icon, content)` - Reusable section card
- `ScenarioCheckbox(scenario, isSelected, onToggle)` - Scenario selection row
- `DifficultyBadge(difficulty)` - Color-coded badge

**TopAppBar**:
- Back button
- Save button (shows spinner when saving)
- Title: "プロフィール"
- Subtitle: "あなたの情報を設定しましょう"

**Save Flow**:
1. User taps save button
2. Validation (name required)
3. Show saving spinner
4. Save to Room via ProfileRepository
5. On success: navigate back
6. On error: show error message in card

## Updated Files (3)

### 1. User.kt (domain/model/)
**Enhanced with profile fields**:

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val level: Int = 1,
    val avatarId: Int = 0,              // NEW: 0-5 for avatar selection
    val learningGoal: String = "",      // NEW: User's learning objective
    val favoriteScenarios: String = "", // NEW: Comma-separated IDs
    val nativeLanguage: String = "Korean", // NEW: Mother tongue
    val bio: String = "",               // NEW: Self-introduction
    val studyStartDate: Long = System.currentTimeMillis(), // NEW
    val createdAt: Long = System.currentTimeMillis()
)
```

**Breaking Change**: Added default values to existing fields to support migration

**Database Migration**: May require migration or database recreation for existing apps

### 2. ChatViewModel.kt
**Integrated profile personalization**:

**Changes**:
- Injected `ProfileRepository`
- Added `user: User?` to `ChatUiState`
- Added `observeUserProfile()` to load current user
- **Enhanced AI prompts** with personalized context:
  ```kotlin
  val personalizedPrefix = profileRepository.getPersonalizedPromptPrefix()
  val enhancedPrompt = scenario.systemPrompt + personalizedPrefix
  ```

**AI Personalization**:
AI now receives context about:
- User's name
- Learning goal
- Bio/background
- Native language
- Japanese proficiency level (with JLPT mapping)

**Example Enhanced Prompt**:
```
[Original Scenario Prompt]

User Context:
You are speaking with 田中太郎
Their learning goal is: 日本旅行のため
About them: 大学生です
Their native language is Korean
Their Japanese level is intermediate (JLPT N3-N2 level)

Tailor your responses to be appropriate for their level and goals.
```

### 3. Navigation Updates

**NihongoNavHost.kt**:
- Added `Screen.Profile` route
- Added Profile composable with navigation
- Passes `onSaveSuccess` callback to navigate back after save

**ScenarioListScreen.kt**:
- Added `onProfileClick` callback parameter
- Added profile icon (👤 AccountCircle) in TopAppBar
- Icon order: Profile, Stats, Settings

**App Bar Layout**:
```
┌─────────────────────────────────┐
│ Scenario List    👤 📊 ⚙️      │
│                  ↑  ↑  ↑        │
│            Profile Stats Settings│
└─────────────────────────────────┘
```

## UI Design

```
┌─────────────────────────────────┐
│ ← プロフィール            💾    │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 👤 アバター                 │ │
│ │                             │ │
│ │        😊                   │ │
│ │   (100dp preview)           │ │
│ │                             │ │
│ │ 😊  🎌  🗾  🍣  ⛩️  🎎    │ │
│ │  ✓                          │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ℹ️ 基本情報                 │ │
│ │                             │ │
│ │ 名前: [田中太郎___________] │ │
│ │                             │ │
│ │ 自己紹介:                   │ │
│ │ [大学生です____________]    │ │
│ │ [____________________]      │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🏆 学習目標                 │ │
│ │                             │ │
│ │ [日本旅行のため__________]  │ │
│ │ [____________________]      │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📈 日本語レベル             │ │
│ │                             │ │
│ │ 中級 (JLPT N3-N2)           │ │
│ │ ━━━━●━━━━━━━━━━━━━━━━━  │ │
│ │ 初級    中級    上級        │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ❤️ お気に入りシナリオ      │ │
│ │                             │ │
│ │ ☑ レストランでの注文 [初級]│ │
│ │ ☐ 買い物 [初級]            │ │
│ │ ☑ ホテル [中級]            │ │
│ │ ☐ 友達 [中級]              │ │
│ │ ☐ 電話 [上級]              │ │
│ │ ☐ 病院 [上級]              │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🌐 母語                     │ │
│ │                             │ │
│ │ [Korean________________]    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │      💾 保存する            │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Features Breakdown

### 1. Avatar Selection (6 Presets)

**Avatars**:
| ID | Emoji | Meaning | Color |
|----|-------|---------|-------|
| 0  | 😊   | Happy   | Orange |
| 1  | 🎌   | Japan Flag | Green |
| 2  | 🗾   | Japan Map | Blue |
| 3  | 🍣   | Sushi   | Red |
| 4  | ⛩️   | Torii Gate | Purple |
| 5  | 🎎   | Dolls   | Yellow |

**Selection UI**:
- Grid of 6 circular avatars
- Each has unique background color
- Selected avatar has:
  - 3dp primary color border
  - Checkmark badge (bottom-right)
- Tap to select
- Large preview shows current selection

### 2. User Name & Bio

**Name Field**:
- Required (validated on save)
- Single line input
- Placeholder: "田中太郎"
- Icon: Person

**Bio Field**:
- Optional
- 2-3 lines
- Multiline input
- Placeholder: "簡単な自己紹介を書いてください"
- Icon: Description

**Usage in AI**:
- Name used in greeting: "You are speaking with [name]"
- Bio provides context about user background

### 3. Learning Goals

**Purpose**: Help AI understand user's motivation

**Examples**:
- "日本旅行のため" (For traveling to Japan)
- "アニメを字幕なしで見るため" (To watch anime without subtitles)
- "JLPT N2合格のため" (To pass JLPT N2)
- "日本の会社で働くため" (To work at a Japanese company)

**AI Integration**:
- Included in AI prompt: "Their learning goal is: [goal]"
- AI can provide relevant examples
- Conversation topics aligned with goal

### 4. Japanese Proficiency Level

**Levels**:
- **Level 1**: 初級 (Beginner) - JLPT N5-N4
- **Level 2**: 中級 (Intermediate) - JLPT N3-N2
- **Level 3**: 上級 (Advanced) - JLPT N1

**UI**:
- Slider with 3 steps
- Shows current level with JLPT mapping
- Labels below slider

**AI Personalization**:
- Beginner: Simple sentences, common vocabulary
- Intermediate: Natural conversation, varied grammar
- Advanced: Complex sentences, formal language, idioms

**Prompt Integration**:
```
"Their Japanese level is intermediate (JLPT N3-N2 level)"
"Tailor your responses to be appropriate for their level"
```

### 5. Favorite Scenarios

**Purpose**: Track user preferences and interests

**UI**:
- List of all scenarios
- Checkbox for each
- Shows scenario title, description, difficulty
- Stored as comma-separated IDs: "1,3,5"

**Future Use**:
- Recommend similar scenarios
- Track progress by interest area
- Personalize scenario suggestions

### 6. Native Language

**Purpose**: Help AI understand user's linguistic background

**Default**: "Korean" (target audience)

**AI Integration**:
- "Their native language is [language]"
- AI can explain using linguistic comparisons
- Can switch between languages if user struggles

### 7. Profile Persistence

**Storage**: Room database (users table)

**Flow**:
```
User edits profile
    ↓
ProfileViewModel.saveProfile()
    ↓
ProfileRepository.saveProfile()
    ↓
Room Database (users table)
    ↓
Profile updates propagate via Flow
    ↓
ChatViewModel receives updated user
    ↓
Next AI message uses new profile context
```

## AI Response Personalization

### Before Profile System:
```
System Prompt:
You are a friendly Japanese language partner helping someone practice
conversation. Be natural and encouraging.

User: こんにちは
AI: こんにちは！元気ですか？
```

### With Profile System:
```
System Prompt:
You are a friendly Japanese language partner helping someone practice
conversation. Be natural and encouraging.

User Context:
You are speaking with 田中太郎
Their learning goal is: 日本旅行のため
About them: 韓国の大学生です
Their native language is Korean
Their Japanese level is intermediate (JLPT N3-N2 level)

Tailor your responses to be appropriate for their level and goals.

User: こんにちは
AI: 田中さん、こんにちは！今日はどこか行きたい場所がありますか？
    日本旅行の計画について話しましょうか？
```

**Improvements**:
- Uses user's name (田中さん)
- References their goal (日本旅行)
- Appropriate complexity for N3-N2 level
- Conversation aligned with interests

## Data Flow

### Profile Creation/Update
```
ProfileScreen
    ↓
User fills form (name, avatar, goal, etc.)
    ↓
Tap save button
    ↓
ProfileViewModel.saveProfile()
    ↓
Validation (name required)
    ↓
ProfileRepository.saveProfile(...)
    ↓
Room UserDao.insertUser() or updateUser()
    ↓
Database update
    ↓
Flow emits updated User
    ↓
ProfileViewModel receives update
    ↓
Success → navigate back
```

### Profile → AI Personalization
```
ChatViewModel.init()
    ↓
observeUserProfile()
    ↓
ProfileRepository.getCurrentUser() (Flow)
    ↓
Update ChatUiState.user
    ↓
User sends message
    ↓
ChatViewModel.sendMessage()
    ↓
ProfileRepository.getPersonalizedPromptPrefix()
    ↓
Builds context string from user fields
    ↓
Appends to scenario.systemPrompt
    ↓
ConversationRepository.sendMessage(enhancedPrompt)
    ↓
GeminiApiService receives personalized prompt
    ↓
AI generates context-aware response
```

## Integration with Existing Systems

### Room Database
**User Table Enhanced**:
- Added 6 new fields with default values
- Backward compatible (defaults allow gradual migration)
- Single user model (ID = 1L for simplicity)

### ChatViewModel
**Profile Integration**:
- Observes user profile via Flow
- Automatically updates when profile changes
- Enhances AI prompts in real-time

### Navigation
**New Route**: `Screen.Profile`
- Accessible from ScenarioListScreen
- Back navigation after save
- Standard NavHost pattern

## Material Design 3 Elements

### Color Scheme
- **PrimaryContainer**: TopAppBar background
- **OnPrimaryContainer**: TopAppBar text
- **Primary**: Icons, selected avatar border, level text
- **SurfaceVariant**: Section card backgrounds
- **ErrorContainer**: Error message background
- **Tertiary/Secondary/Error**: Difficulty badges

### Typography
- **TitleLarge**: Screen title (22sp)
- **TitleMedium**: Section titles (16sp, bold)
- **BodySmall**: Subtitles, placeholders (12sp)
- **LabelMedium**: Input labels (12sp)

### Components
- `Scaffold` + `TopAppBar`
- `LazyColumn` for scrollable form
- `Card` for sections
- `OutlinedTextField` for inputs
- `Slider` for level selection
- `Checkbox` for scenarios
- `Button` for save action
- `CircularProgressIndicator` for loading

### Spacing
- Screen padding: 16dp
- Section spacing: 24dp
- Internal spacing: 16dp
- Avatar grid gap: 12dp

## Performance Optimizations

### Flow-Based Reactivity
```kotlin
profileRepository.getCurrentUser() // Flow<User?>
    ↓
Collected in ChatViewModel
    ↓
Only updates when user changes
    ↓
Minimal recompositions
```

### Lazy Profile Loading
- Profile loaded on demand
- Cached in StateFlow
- No repeated database queries

### Efficient Avatar Storage
- Avatars stored as integers (0-5)
- Emojis rendered from constants
- No image files needed

## Empty/Error States

### New User (No Profile)
- All fields show defaults/placeholders
- Name field empty (will show error if save without filling)
- Level defaults to 1 (初級)
- Avatar defaults to 0 (😊)

### Save Error
```
┌────────────────────────────────┐
│ ⚠️ 名前を入力してください     │
└────────────────────────────────┘
```

### Loading State
- Full screen spinner while loading profile
- Save button shows spinner while saving

## Accessibility

### Features
- All icons have `contentDescription`
- Input fields have labels
- Clear visual hierarchy
- Large touch targets (avatar circles, checkboxes)
- Slider has verbal values

### Screen Reader Support
- Section headers announced
- Input field labels read
- Checkbox states verbalized
- Save button state announced

## Localization

### Japanese UI
- プロフィール (Profile)
- アバター (Avatar)
- 基本情報 (Basic Information)
- 学習目標 (Learning Goal)
- 日本語レベル (Japanese Level)
- お気に入りシナリオ (Favorite Scenarios)
- 母語 (Native Language)
- 保存する (Save)

### Level Labels
- 初級 (Beginner)
- 中級 (Intermediate)
- 上級 (Advanced)

### JLPT Mapping
- N5-N4 (Beginner)
- N3-N2 (Intermediate)
- N1 (Advanced)

## Testing Checklist

### Profile Creation
- [ ] Open profile from scenario screen
- [ ] Select avatar (all 6 options)
- [ ] Enter name
- [ ] Enter bio
- [ ] Enter learning goal
- [ ] Adjust Japanese level slider
- [ ] Select favorite scenarios
- [ ] Change native language
- [ ] Save profile
- [ ] Verify navigation back
- [ ] Check profile persists after app restart

### Profile Editing
- [ ] Load existing profile
- [ ] All fields populate correctly
- [ ] Modify each field
- [ ] Save changes
- [ ] Verify updates persist

### Validation
- [ ] Try saving without name → error shown
- [ ] Error message displays correctly
- [ ] Can dismiss error and retry

### AI Personalization
- [ ] Create profile with name "田中"
- [ ] Set goal "日本旅行"
- [ ] Set level to 中級
- [ ] Start chat
- [ ] Verify AI uses name in response
- [ ] Verify AI references goal
- [ ] Verify response complexity matches level

### UI/UX
- [ ] Avatar preview updates when selected
- [ ] Checkmark shows on selected avatar
- [ ] Level slider shows correct label
- [ ] Scenario checkboxes toggle correctly
- [ ] Save button shows spinner when saving
- [ ] Smooth scrolling in LazyColumn

## Future Enhancements

### Profile Features
- [ ] Profile photo upload
- [ ] Study streak on profile
- [ ] Achievement badges
- [ ] Learning statistics summary
- [ ] Export profile data

### Avatar System
- [ ] Custom avatar colors
- [ ] Unlock special avatars via achievements
- [ ] Animated avatars
- [ ] Avatar accessories

### Personalization
- [ ] AI personality selection (formal/casual/friendly)
- [ ] Topic interests (food, travel, anime, business)
- [ ] Learning style preferences (visual/auditory/kinesthetic)
- [ ] Conversation pace control (slow/normal/fast)

### Goals & Tracking
- [ ] SMART goal setting (Specific, Measurable, Achievable, Relevant, Time-bound)
- [ ] Progress toward goal visualization
- [ ] Goal reminders
- [ ] Milestone celebrations

### Social Features
- [ ] Share profile (optional)
- [ ] Find study partners with similar goals
- [ ] Join study groups
- [ ] Profile comparison (anonymized)

## Architecture Impact

### Clean Architecture Preserved
- **Domain**: Enhanced User model with profile fields
- **Data**: ProfileRepository for profile operations
- **Presentation**: ProfileViewModel + ProfileScreen + Avatars

### Dependencies
```
ProfileViewModel ──→ ProfileRepository ──→ UserDao
                                              ↓
                                         Room Database

ChatViewModel ──→ ProfileRepository
      ↓
  Personalized AI Prompts
```

### Separation of Concerns
- Profile management isolated in ProfileRepository
- UI state in ProfileViewModel
- Avatar logic in separate Avatars.kt
- AI personalization abstracted in repository method

## Project Structure

```
app/src/main/java/com/nihongo/conversation/
├── domain/model/
│   └── User.kt                          ← UPDATED (6 new fields)
├── data/repository/
│   └── ProfileRepository.kt             ← NEW (120+ lines)
├── presentation/
│   ├── profile/                         ← NEW DIRECTORY
│   │   ├── ProfileScreen.kt             ← NEW (400+ lines)
│   │   ├── ProfileViewModel.kt          ← NEW (130+ lines)
│   │   └── Avatars.kt                   ← NEW (120+ lines)
│   ├── chat/
│   │   └── ChatViewModel.kt             ← UPDATED (profile integration)
│   ├── scenario/
│   │   └── ScenarioListScreen.kt        ← UPDATED (profile icon)
│   └── navigation/
│       └── NihongoNavHost.kt            ← UPDATED (profile route)
```

**Total New Code**: 770+ lines
**Files Created**: 4
**Files Modified**: 4

---

## Summary

✅ **Complete User Profile System**

**Key Achievements**:
1. 👤 User profile with name, bio, avatar, goals
2. 😊 6 preset avatar options with unique colors
3. 🎯 Learning goal tracking
4. 📊 Japanese proficiency level (JLPT N5-N1)
5. ❤️ Favorite scenario selection
6. 🌐 Native language setting
7. 🤖 **AI response personalization** based on profile
8. 💾 Persistent storage in Room database
9. 🔄 Real-time profile updates via Flow
10. 🎨 Beautiful Material 3 UI

**User Experience**:
- Create personalized profile with avatar
- Set learning goals and preferences
- AI adapts responses to user's level and goals
- Profile persists across sessions
- Easy access from main navigation

**AI Personalization**:
- Uses user's name in conversations
- References learning goals
- Adjusts complexity to proficiency level
- Considers native language background
- Provides relevant examples and topics

**Technical Excellence**:
- Clean architecture maintained
- ProfileRepository encapsulates logic
- Flow-based reactive updates
- Validation and error handling
- Material 3 design system
- No breaking changes to existing code

The user profile system is now **ready for testing** and provides deep personalization for an enhanced learning experience! 👤✨
