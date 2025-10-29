# Dynamic AI Difficulty Adjustment System

## Overview

The difficulty system dynamically adjusts AI responses based on the user's Japanese proficiency level (JLPT N5-N1), providing appropriate vocabulary, grammar, and sentence complexity. Each AI response is analyzed for vocabulary complexity and displayed with visual indicators.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   ChatViewModel                     │
│  - Retrieves user difficulty level                  │
│  - Generates enhanced prompts with difficulty rules │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│              DifficultyManager                      │
│  - Provides difficulty-specific prompts             │
│  - Analyzes vocabulary complexity                   │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│          ConversationRepository                     │
│  - Analyzes AI response complexity                  │
│  - Stores complexity score in Message entity        │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────┐
│               ChatScreen UI                         │
│  - Displays complexity indicators on AI messages    │
└─────────────────────────────────────────────────────┘
```

## Components

### 1. DifficultyLevel Enum

```kotlin
enum class DifficultyLevel(val value: Int) {
    BEGINNER(1),      // 初級: JLPT N5-N4
    INTERMEDIATE(2),  // 中級: JLPT N3-N2
    ADVANCED(3)       // 上級: JLPT N1
}
```

Maps user proficiency levels (1-3) to JLPT level ranges.

### 2. VocabularyComplexity Enum

```kotlin
enum class VocabularyComplexity {
    BASIC,        // 1 star: N5-N4 common daily words
    COMMON,       // 2 stars: N4-N3 frequently used
    INTERMEDIATE, // 3 stars: N3-N2 standard vocabulary
    ADVANCED,     // 4 stars: N2-N1 specialized/formal
    EXPERT        // 5 stars: N1+ rare/technical/literary
}
```

Represents the actual complexity of AI-generated responses.

### 3. DifficultyManager

**Location:** `core/difficulty/DifficultyManager.kt`

**Key Methods:**

```kotlin
// Generate difficulty-specific prompt enhancements
fun getDifficultyPrompt(level: DifficultyLevel): String

// Analyze vocabulary complexity of Japanese text
fun analyzeVocabularyComplexity(text: String): VocabularyComplexity

// Convert complexity to 1-5 integer score
fun getComplexityScore(complexity: VocabularyComplexity): Int
```

**Heuristic Analysis Algorithm:**

1. **Kanji Density**: Calculate ratio of kanji characters to total characters
2. **Keigo Detection**: Check for honorific patterns (ございます, いらっしゃる, etc.)
3. **Advanced Grammar**: Detect complex patterns (ざるを得ない, に他ならない, etc.)
4. **Formal Endings**: Identify business/academic language markers

**Complexity Scoring:**

| Condition | Complexity |
|-----------|-----------|
| Keigo + Advanced Grammar | EXPERT (5) |
| Kanji ratio > 40% + Formal endings | ADVANCED (4) |
| Kanji ratio > 30% OR Advanced grammar | INTERMEDIATE (3) |
| Kanji ratio > 15% | COMMON (2) |
| Low kanji density | BASIC (1) |

## Difficulty Level Specifications

### Level 1: Beginner (初級 - JLPT N5-N4)

**Target Audience:** Users just starting to learn Japanese

**Vocabulary:**
- Basic, common words only (N5-N4 level)
- Prefer hiragana or simple kanji
- Examples: 食べる, 行く, 見る, きれい, おいしい

**Grammar:**
- Simple sentence structures (subject-object-verb)
- Present/past tense only
- Common particles: は, が, を, に, で, と
- です/ます form exclusively
- NO causative, passive, or conditional forms

**Sentence Length:**
- 5-10 words maximum
- One idea per sentence
- Frequent use of particles for clarity

**Example Transformations:**
```
❌ Complex: "そのレストランはとても人気があるので、予約した方がいいと思います。"
✅ Beginner: "そのレストランは人気です。予約をしてください。"
```

### Level 2: Intermediate (中級 - JLPT N3-N2)

**Target Audience:** Users with conversational ability

**Vocabulary:**
- Common to intermediate vocabulary (N3-N2 level)
- Mix of kanji and kana appropriately
- Common compound words
- Examples: 準備する, 残念, 素晴らしい, 〜によって

**Grammar:**
- Compound sentences with connectors (が, けど, から, ので)
- Conditional forms (たら, ば, なら)
- Potential, passive, causative forms
- Te-form combinations (〜ている, 〜てみる, 〜てあげる)
- Common patterns (〜ようだ, 〜そうだ, 〜らしい)

**Sentence Length:**
- 10-20 words
- Multiple clauses connected naturally
- Varied sentence structures

**Example Transformations:**
```
❌ Too simple: "レストランは人気です。予約をしてください。"
❌ Too complex: "当該レストランにおかれましては、満席となることが多く..."
✅ Intermediate: "そのレストランは人気があるので、予約した方がいいと思いますよ。"
```

### Level 3: Advanced (上級 - JLPT N1)

**Target Audience:** Near-native or advanced learners

**Vocabulary:**
- Advanced vocabulary including specialized terms
- Literary expressions and formal language
- Kanji compounds and Sino-Japanese words
- Examples: 考慮する, 顕著, 〜に際して, 〜をもって

**Grammar:**
- Complex sentence structures with multiple clauses
- Advanced patterns (〜ざるを得ない, 〜に他ならない)
- Keigo (honorific/humble/polite language)
- Literary forms and written language
- Passive-causative combinations

**Sentence Length:**
- 20+ words
- Multiple nested clauses
- Complex logical relationships

**Cultural Elements:**
- Idiomatic expressions and proverbs (ことわざ)
- Four-character idioms (四字熟語)
- Cultural references
- Nuanced indirect communication

**Example Transformations:**
```
❌ Too simple: "そのレストランは人気があるので、予約した方がいいと思いますよ。"
✅ Advanced: "当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。"
✅ Formal: "そちらのレストランにおかれましては、大変人気がございますので、あらかじめご予約を頂戴できればと存じます。"
```

## Integration Flow

### 1. Prompt Enhancement (ChatViewModel)

```kotlin
fun sendMessage() {
    // Get user's difficulty level from profile
    val user = _uiState.value.user
    val difficultyLevel = DifficultyLevel.fromInt(user?.level ?: 1)

    // Get difficulty-specific guidelines
    val difficultyPrompt = difficultyManager.getDifficultyPrompt(difficultyLevel)

    // Combine: scenario prompt + personalized prefix + difficulty rules
    val enhancedPrompt = scenario.systemPrompt +
                         personalizedPrefix +
                         difficultyPrompt

    // Send to Gemini API with enhanced prompt
    repository.sendMessage(
        conversationId = currentConversationId,
        userMessage = message,
        conversationHistory = _uiState.value.messages,
        systemPrompt = enhancedPrompt
    )
}
```

### 2. Complexity Analysis (ConversationRepository)

```kotlin
suspend fun sendMessage(...): Flow<Result<Message>> = flow {
    // Get AI response from Gemini API
    val aiResponse = geminiApi.sendMessage(...)

    // Analyze vocabulary complexity
    val complexity = difficultyManager.analyzeVocabularyComplexity(aiResponse)
    val complexityScore = difficultyManager.getComplexityScore(complexity)

    // Save with complexity score
    val aiMsg = Message(
        conversationId = conversationId,
        content = aiResponse,
        isUser = false,
        complexityScore = complexityScore  // 1-5
    )
    messageDao.insertMessage(aiMsg)
}
```

### 3. Visual Display (ChatScreen)

```kotlin
@Composable
fun MessageBubble(message: Message) {
    // ... message content ...

    // Show difficulty indicator for AI messages
    if (!message.isUser && message.complexityScore > 0) {
        CompactDifficultyIndicator(
            complexityScore = message.complexityScore
        )
    }

    // ... timestamp ...
}
```

## UI Components

### CompactDifficultyIndicator

**Location:** `presentation/chat/DifficultyIndicator.kt`

Displays color-coded stars (1-5) indicating vocabulary complexity:

| Score | Label | Color | Stars |
|-------|-------|-------|-------|
| 1 | 基本 (Basic) | Green (#4CAF50) | ⭐ |
| 2 | 一般 (Common) | Light Green (#8BC34A) | ⭐⭐ |
| 3 | 中級 (Intermediate) | Amber (#FFC107) | ⭐⭐⭐ |
| 4 | 上級 (Advanced) | Orange (#FF9800) | ⭐⭐⭐⭐ |
| 5 | 専門 (Expert) | Red (#F44336) | ⭐⭐⭐⭐⭐ |

**Visual Design:**
- 10dp star icons
- Displays below message content, above timestamp
- Only shown for AI messages with complexity score > 0
- Compact layout (stars only, no labels)

### DifficultyIndicator (Full Version)

Full indicator with school icon, stars, and label. Currently not used in UI but available for future features like detailed analysis screens.

### ComplexityExplanation

Provides detailed explanation of complexity levels:
- Level description (e.g., "中級 (N3-N2レベル)")
- Details about vocabulary and grammar used
- Can be used in help dialogs or settings

## Database Schema

### Message Entity Update

```kotlin
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val hasError: Boolean = false,
    val complexityScore: Int = 0  // NEW: 0 = not analyzed, 1-5 = complexity
)
```

**Backward Compatibility:**
- Default value of 0 means "not analyzed"
- Existing messages automatically get score 0
- No migration required

## Testing Scenarios

### Test Case 1: Beginner Level (Level 1)

**Setup:**
1. Create user profile with level = 1
2. Start conversation in "Restaurant Reservation" scenario

**Expected Behavior:**
- AI uses simple vocabulary (N5-N4 words)
- Short sentences (5-10 words)
- です/ます form only
- Complexity scores should be 1-2 stars (green/light green)

**Example Exchange:**
```
User: "レストランを予約したいです。"
AI: "はい、わかりました。何人ですか？" [1 star - 基本]
AI: "いつ行きますか？" [1 star - 基本]
```

### Test Case 2: Intermediate Level (Level 2)

**Setup:**
1. Create user profile with level = 2
2. Start conversation in "Shopping" scenario

**Expected Behavior:**
- AI uses intermediate vocabulary (N3-N2 words)
- Compound sentences with connectors
- Mix of polite and casual forms
- Complexity scores should be 2-3 stars (light green/amber)

**Example Exchange:**
```
User: "このシャツはいくらですか？"
AI: "そのシャツは3000円ですが、今セール中なので20%オフになりますよ。" [3 stars - 中級]
```

### Test Case 3: Advanced Level (Level 3)

**Setup:**
1. Create user profile with level = 3
2. Start conversation in "Business Meeting" scenario

**Expected Behavior:**
- AI uses advanced vocabulary and keigo
- Complex sentence structures
- Literary expressions and idioms
- Complexity scores should be 4-5 stars (orange/red)

**Example Exchange:**
```
User: "会議の日程について相談したいのですが。"
AI: "承知いたしました。ご都合の良い日時をお知らせいただければ、こちらで調整させていただきます。" [4-5 stars - 上級/専門]
```

### Test Case 4: Complexity Analysis Accuracy

**Test Input Texts:**

```kotlin
// Should be BASIC (1 star)
val basic = "今日は晴れです。公園に行きます。"

// Should be COMMON (2 stars)
val common = "明日は友達と映画を見に行く予定です。"

// Should be INTERMEDIATE (3 stars)
val intermediate = "このレストランは評判がいいので、予約した方がいいと思います。"

// Should be ADVANCED (4 stars)
val advanced = "当該事項につきまして、慎重に検討させていただきます。"

// Should be EXPERT (5 stars)
val expert = "ご多忙のところ恐縮でございますが、何卒ご検討のほどよろしくお願い申し上げます。"
```

## Performance Considerations

### Complexity Analysis

- **Time Complexity:** O(n) where n = text length
- **Pattern Matching:** Limited set of patterns (~50 total)
- **Impact:** Minimal overhead (<10ms for typical responses)

### Memory Usage

- **Prompt Storage:** ~2KB per difficulty level (cached in memory)
- **Analysis State:** No persistent state required
- **Message Storage:** +4 bytes per message (Int field)

## Future Enhancements

### Planned Features

1. **Adaptive Difficulty:**
   - Automatically adjust level based on user performance
   - Track comprehension metrics (response time, error rate)
   - Suggest level changes

2. **Vocabulary Highlighting:**
   - Highlight advanced words in messages
   - Tap to see definition and JLPT level
   - Save difficult words for review

3. **Granular Analysis:**
   - Separate scores for vocabulary, grammar, complexity
   - Show breakdown in detailed view
   - Track improvement over time

4. **Custom Difficulty Profiles:**
   - Allow users to fine-tune preferences
   - Focus on specific areas (grammar vs vocabulary)
   - Create custom JLPT target levels

### Potential Improvements

1. **ML-Based Analysis:**
   - Replace heuristics with trained model
   - Use JLPT vocabulary database
   - More accurate complexity scoring

2. **Real-time Difficulty Adjustment:**
   - Adjust within conversation based on understanding
   - Offer "simpler" or "more advanced" alternatives
   - Dynamic prompt modification

3. **Context-Aware Prompts:**
   - Combine difficulty with scenario context
   - Cultural appropriateness rules
   - Situational formality guidelines

## References

### JLPT Level Descriptions

- **N5 (Beginner):** Basic Japanese, 100 kanji, 800 words
- **N4 (Elementary):** Simple conversations, 300 kanji, 1500 words
- **N3 (Intermediate):** Daily situations, 650 kanji, 3750 words
- **N2 (Pre-Advanced):** Natural conversations, 1000 kanji, 6000 words
- **N1 (Advanced):** Complex texts, 2000+ kanji, 10000+ words

### Keigo Categories

1. **Sonkeigo (尊敬語):** Respectful language for others' actions
   - Examples: いらっしゃる, おっしゃる, なさる

2. **Kenjougo (謙譲語):** Humble language for own actions
   - Examples: いたす, 申し上げる, 存じる

3. **Teineigo (丁寧語):** Polite language
   - Examples: です, ます, ございます

## Troubleshooting

### Issue: All messages show same complexity

**Cause:** DifficultyManager not injected or complexity analysis not called

**Solution:** Verify ConversationRepository is calling `analyzeVocabularyComplexity()` after AI response

### Issue: Complexity scores don't match difficulty level

**Expected:** This is normal behavior
- AI **target level** (prompt instructions) ≠ **actual complexity** (analysis result)
- Gemini API may not perfectly follow difficulty guidelines
- Some topics naturally require more complex vocabulary

**Mitigation:** Complexity tracking helps identify when AI deviates from target

### Issue: No stars shown on messages

**Cause:** `complexityScore = 0` (not analyzed) or UI integration missing

**Solution:**
1. Check Message entity has complexityScore field
2. Verify ConversationRepository stores scores
3. Confirm ChatScreen shows CompactDifficultyIndicator

## Code Examples

### Custom Difficulty Prompt

```kotlin
// Add domain-specific difficulty rules
fun getCustomPrompt(level: DifficultyLevel, domain: String): String {
    val basePrompt = getDifficultyPrompt(level)
    val domainRules = when (domain) {
        "medical" -> "\nUse medical terminology appropriately for this level."
        "business" -> "\nUse business keigo and formal expressions."
        else -> ""
    }
    return basePrompt + domainRules
}
```

### Batch Complexity Analysis

```kotlin
// Analyze multiple messages
suspend fun analyzeConversationComplexity(conversationId: Long) {
    val messages = messageDao.getMessagesByConversation(conversationId)

    messages.filter { !it.isUser && it.complexityScore == 0 }.forEach { msg ->
        val complexity = difficultyManager.analyzeVocabularyComplexity(msg.content)
        val score = difficultyManager.getComplexityScore(complexity)

        messageDao.updateComplexityScore(msg.id, score)
    }
}
```

### Difficulty Statistics

```kotlin
// Calculate average difficulty per conversation
fun getAverageComplexity(messages: List<Message>): Float {
    val aiMessages = messages.filter { !it.isUser && it.complexityScore > 0 }
    return if (aiMessages.isNotEmpty()) {
        aiMessages.map { it.complexityScore }.average().toFloat()
    } else 0f
}
```

## Summary

The dynamic difficulty adjustment system provides:

✅ **Personalized Learning:** AI adapts to user's JLPT level
✅ **Visual Feedback:** Color-coded complexity indicators
✅ **Comprehensive Guidelines:** Detailed prompts for each level
✅ **Automatic Analysis:** Heuristic-based complexity scoring
✅ **Seamless Integration:** Works with existing chat flow
✅ **Future-Ready:** Extensible for advanced features

This system ensures learners receive appropriately challenging content while tracking actual complexity of AI responses.
