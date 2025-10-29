# Grammar Explanation Feature

## Overview

The grammar explanation feature provides interactive, on-demand analysis of Japanese sentences with Korean explanations. Users can long-press any message to see detailed grammar breakdowns, colored syntax highlighting, and contextual examples from their conversation.

## User Flow

```
User long-presses message
    ↓
ChatScreen triggers requestGrammarExplanation()
    ↓
ChatViewModel shows loading state
    ↓
Repository calls Gemini API with sentence + conversation context + user level
    ↓
Gemini returns structured JSON grammar analysis
    ↓
GrammarBottomSheet displays:
    - Highlighted sentence with color-coded components
    - Quick overview explanation
    - Individual grammar component cards
    - Detailed explanation (collapsible)
    - Examples from current conversation
    - Related grammar patterns to study
```

## Architecture

### Data Models

**GrammarComponent** (`domain/model/GrammarExplanation.kt`)
```kotlin
data class GrammarComponent(
    val text: String,           // e.g., "を", "食べます"
    val type: GrammarType,      // PARTICLE, VERB, etc.
    val explanation: String,    // Korean explanation
    val startIndex: Int,        // Position in original text
    val endIndex: Int
)
```

**GrammarType** (8 types with color codes)
```kotlin
enum class GrammarType(val colorCode: String, val label: String) {
    PARTICLE("0xFF2196F3", "조사"),      // Blue
    VERB("0xFF4CAF50", "동사"),          // Green
    ADJECTIVE("0xFFFF9800", "형용사"),   // Orange
    NOUN("0xFF9C27B0", "명사"),          // Purple
    AUXILIARY("0xFFE91E63", "보조동사"),  // Pink
    CONJUNCTION("0xFF00BCD4", "접속사"),  // Cyan
    ADVERB("0xFFFFEB3B", "부사"),        // Yellow
    EXPRESSION("0xFF795548", "표현")     // Brown
}
```

**GrammarExplanation**
```kotlin
data class GrammarExplanation(
    val originalText: String,
    val components: List<GrammarComponent>,
    val overallExplanation: String,     // High-level summary
    val detailedExplanation: String,    // In-depth breakdown
    val examples: List<String>,         // From conversation
    val relatedPatterns: List<String>   // Related grammar to study
)
```

### API Integration

**GeminiApiService.explainGrammar()** (`data/remote/GeminiApiService.kt`)

Sends structured prompt to Gemini requesting JSON response with:
- Overall and detailed explanations in Korean
- Grammar components with precise start/end positions
- Component types (must match GrammarType enum)
- Conversation examples included for context
- Related grammar patterns

**Prompt Structure:**
```
다음 일본어 문장의 문법을 한국어로 쉽게 설명해주세요.
사용자 레벨: [1-3] (1=초급, 2=중급, 3=고급)

문장: [sentence]

JSON 형식:
{
  "overallExplanation": "...",
  "detailedExplanation": "...",
  "components": [
    {
      "text": "...",
      "type": "PARTICLE|VERB|...",
      "explanation": "...",
      "startIndex": 0,
      "endIndex": 1
    }
  ],
  "examples": [...],
  "relatedPatterns": [...]
}
```

**Error Handling:**
- Falls back to empty explanation on API failure
- Handles JSON parsing errors gracefully
- Shows user-friendly error messages

### Repository Layer

**ConversationRepository.explainGrammar()** (`data/repository/ConversationRepository.kt`)

```kotlin
suspend fun explainGrammar(
    sentence: String,
    conversationHistory: List<Message>,
    userLevel: Int
): GrammarExplanation {
    // Extract last 5 AI messages as examples
    val examples = conversationHistory
        .filter { !it.isUser }
        .map { it.content }
        .takeLast(5)

    return geminiApi.explainGrammar(sentence, examples, userLevel)
}
```

### ViewModel Integration

**ChatViewModel** (`presentation/chat/ChatViewModel.kt`)

Added to `ChatUiState`:
```kotlin
val grammarExplanation: GrammarExplanation? = null
val isLoadingGrammar: Boolean = false
val showGrammarSheet: Boolean = false
```

**Key Methods:**

```kotlin
fun requestGrammarExplanation(sentence: String) {
    viewModelScope.launch {
        _uiState.update {
            it.copy(
                isLoadingGrammar = true,
                showGrammarSheet = true,
                grammarExplanation = null
            )
        }

        try {
            val user = _uiState.value.user
            val grammarExplanation = repository.explainGrammar(
                sentence = sentence,
                conversationHistory = _uiState.value.messages,
                userLevel = user?.level ?: 1
            )

            _uiState.update {
                it.copy(
                    grammarExplanation = grammarExplanation,
                    isLoadingGrammar = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingGrammar = false,
                    error = "문법 분석을 가져오는데 실패했습니다"
                )
            }
        }
    }
}

fun dismissGrammarSheet() {
    _uiState.update {
        it.copy(
            showGrammarSheet = false,
            grammarExplanation = null
        )
    }
}
```

### UI Components

#### 1. MessageBubble Long-Press Interaction

**ChatScreen.kt** - Modified MessageBubble

```kotlin
@Composable
fun MessageBubble(
    message: Message,
    onSpeakMessage: (() -> Unit)? = null,
    onLongPress: () -> Unit = {}
) {
    // ...
    Surface(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .combinedClickable(
                onClick = { onSpeakMessage?.invoke() },
                onLongClick = onLongPress
            )
    ) {
        // Message content
    }
}
```

**Interaction:**
- Single tap: Speaks AI message (if enabled)
- Long press: Shows grammar explanation bottom sheet

#### 2. GrammarBottomSheet

**Location:** `presentation/chat/GrammarBottomSheet.kt`

**Features:**

1. **Header**
   - Title with icon
   - Close button

2. **Highlighted Original Text**
   - Color-coded grammar components
   - Background highlighting for each component type
   - Uses AnnotatedString with SpanStyle

3. **Quick Overview**
   - Light bulb icon
   - Brief 1-2 sentence explanation
   - Primary container background

4. **Grammar Components Section**
   - Individual cards for each component
   - Color-coded left border matching grammar type
   - Japanese text + type badge
   - Korean explanation

5. **Detailed Explanation (Collapsible)**
   - Click to expand/collapse
   - In-depth grammar breakdown
   - Secondary container background

6. **Examples from Conversation**
   - Shows last 5 AI messages from conversation
   - Provides context for grammar usage
   - Surface variant background

7. **Related Grammar Patterns**
   - List of related patterns to study
   - Tertiary container background
   - Bullet points

**Component Structure:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarBottomSheet(
    grammarExplanation: GrammarExplanation?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (isLoading) {
            // Loading indicator
        } else if (grammarExplanation != null) {
            GrammarContent(...)
        }
    }
}

@Composable
private fun GrammarContent(...) {
    LazyColumn {
        // Header
        // Highlighted text
        // Quick overview
        // Grammar components
        // Detailed explanation
        // Examples
        // Related patterns
    }
}

@Composable
private fun GrammarComponentCard(component: GrammarComponent) {
    Surface {
        Row {
            // Color indicator bar
            // Japanese text + type badge
            // Korean explanation
        }
    }
}
```

**Syntax Highlighting Algorithm:**

```kotlin
private fun buildHighlightedText(
    originalText: String,
    components: List<GrammarComponent>
): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0

        components.sortedBy { it.startIndex }.forEach { component ->
            // Add unhighlighted text before component
            if (currentIndex < component.startIndex) {
                append(originalText.substring(currentIndex, component.startIndex))
            }

            // Add highlighted component
            withStyle(
                style = SpanStyle(
                    color = getGrammarTypeColor(component.type),
                    fontWeight = FontWeight.Bold,
                    background = getGrammarTypeColor(component.type).copy(alpha = 0.15f)
                )
            ) {
                append(component.text)
            }

            currentIndex = component.endIndex
        }

        // Add remaining text
        if (currentIndex < originalText.length) {
            append(originalText.substring(currentIndex))
        }
    }
}
```

### Color Scheme

| Grammar Type | Color | Hex | Usage |
|--------------|-------|-----|-------|
| PARTICLE | Blue | #2196F3 | は, が, を, に, で, と |
| VERB | Green | #4CAF50 | 食べます, 行く, 見る |
| ADJECTIVE | Orange | #FF9800 | きれい, おいしい, 高い |
| NOUN | Purple | #9C27B0 | 本, 人, 場所 |
| AUXILIARY | Pink | #E91E63 | ます, です, ている |
| CONJUNCTION | Cyan | #00BCD4 | が, けど, から, ので |
| ADVERB | Yellow | #FFEB3B | とても, ゆっくり, よく |
| EXPRESSION | Brown | #795548 | Idiomatic phrases |

## Usage Examples

### Example 1: Beginner Level (Level 1)

**User long-presses:** "このレストランはとても人気です。"

**Grammar Explanation:**

**Overall:** "이 문장은 장소와 상태를 설명하는 기본 문장입니다."

**Components:**
1. **この** (ADJECTIVE) - 지시 형용사 "이"
2. **レストランは** (NOUN + PARTICLE) - 주제를 나타내는 조사 "は"
3. **とても** (ADVERB) - 정도를 나타내는 부사 "매우"
4. **人気** (NOUN) - 명사 "인기"
5. **です** (AUXILIARY) - 정중한 종결어미

**Detailed:** "「この」는 가까운 것을 가리키는 지시 형용사입니다. 「は」는 주제 표시 조사로 '~은/는'에 해당합니다. 「とても」는 정도를 강조하는 부사입니다. 「です」는 정중한 종결 표현으로 초급에서 가장 기본적인 문장 형태입니다."

**Examples from conversation:**
- "駅はどこですか？"
- "予約は必要ですか？"

**Related patterns:**
- "とても + 형용사/명사"
- "~は~です" (기본 서술문)

### Example 2: Intermediate Level (Level 2)

**User long-presses:** "そのレストランは人気があるので、予約した方がいいと思います。"

**Grammar Explanation:**

**Overall:** "이유를 설명하고 조언을 제공하는 복문입니다."

**Components:**
1. **その** (ADJECTIVE) - 지시 형용사 "그"
2. **レストランは** (NOUN + PARTICLE) - 주제 표시
3. **人気がある** (EXPRESSION) - "인기가 있다" 표현
4. **ので** (CONJUNCTION) - 이유를 나타내는 접속사 "~이므로"
5. **予約した** (VERB) - 과거형 동사 "예약하다"
6. **方がいい** (EXPRESSION) - 조언 표현 "~하는 것이 좋다"
7. **と思います** (EXPRESSION) - 의견 표현 "~라고 생각합니다"

**Detailed:** "「ので」는 이유를 나타내는 접속사로 「から」보다 공손한 표현입니다. 「~した方がいい」는 조언할 때 사용하는 패턴입니다. 「と思います」는 자신의 의견을 부드럽게 전달할 때 사용합니다. 전체적으로 이유 제시 → 조언의 논리적 구조를 가진 문장입니다."

**Examples:**
- "時間がないので、急ぎます。"
- "早く行った方がいいですよ。"

**Related patterns:**
- "~ので" (이유)
- "~た方がいい" (조언)
- "~と思う" (의견)

### Example 3: Advanced Level (Level 3)

**User long-presses:** "当該レストランは評判が高く、満席となることが多いため、事前にご予約されることをお勧めいたします。"

**Grammar Explanation:**

**Overall:** "공손한 비즈니스 표현으로 권유를 전달하는 격식 있는 문장입니다."

**Components:**
1. **当該** (NOUN) - 격식 있는 지시어 "해당"
2. **評判が高く** (EXPRESSION) - "평판이 높고" (て형 대신 く형 사용)
3. **満席となる** (EXPRESSION) - "만석이 되다" (격식 표현)
4. **ことが多い** (EXPRESSION) - "~하는 경우가 많다"
5. **ため** (CONJUNCTION) - 격식 있는 이유 표시 "때문에"
6. **事前に** (ADVERB) - "사전에"
7. **ご予約される** (VERB) - 존경어 "예약하시다"
8. **ことをお勧めいたします** (EXPRESSION) - 겸양어 "권해드립니다"

**Detailed:** "이 문장은 고급 비즈니스 일본어의 전형적인 예입니다. 「当該」는 법률/비즈니스 용어입니다. 「高く」는 い형용사의 연결형으로 이유를 나타냅니다. 「ご~される」는 존경어, 「お~いたす」는 겸양어로 최고 수준의 경어 표현입니다. 전체적으로 매우 공손하고 격식 있는 권유 문장입니다."

**Examples:**
- "ご確認いただければ幸いです。"
- "お時間をいただきありがとうございます。"

**Related patterns:**
- "~ため" (격식 있는 이유)
- "ご~される" (존경어)
- "お~いたす" (겸양어)
- "~をお勧めする" (권유 표현)

## Implementation Details

### State Management

**ChatUiState flow:**
```
Initial: showGrammarSheet = false

User long-presses message
    → requestGrammarExplanation()
    → showGrammarSheet = true
    → isLoadingGrammar = true
    → grammarExplanation = null

API call completes
    → isLoadingGrammar = false
    → grammarExplanation = [data]

User dismisses sheet
    → dismissGrammarSheet()
    → showGrammarSheet = false
    → grammarExplanation = null
```

### Performance Considerations

**API Call:**
- Typical response time: 2-4 seconds
- Includes conversation context (5 messages)
- User level parameter affects explanation complexity

**UI Rendering:**
- LazyColumn for efficient scrolling
- Component cards rendered on-demand
- Syntax highlighting calculated once

**Memory:**
- GrammarExplanation stored temporarily in ViewModel
- Cleared on dismiss
- No persistent storage

### Accessibility

1. **Long-press feedback:**
   - Haptic feedback on long press (platform default)
   - Visual indication (sheet appears)

2. **Screen reader support:**
   - All components have contentDescription
   - Grammar type labels in Korean

3. **Color contrast:**
   - All grammar type colors meet WCAG AA standards
   - Background highlighting with 0.15 alpha for readability

## Future Enhancements

### Planned Features

1. **Grammar Pattern Library:**
   - Save interesting patterns for later review
   - Build personal grammar reference
   - Track which patterns have been explained

2. **Practice Mode:**
   - Generate practice sentences using explained patterns
   - Fill-in-the-blank exercises
   - Sentence construction challenges

3. **Difficulty-Aware Explanations:**
   - Adjust explanation complexity based on user level
   - Progressive disclosure (basic → advanced)
   - Beginner explanations include romaji

4. **Visual Grammar Trees:**
   - Sentence structure diagram
   - Show grammatical relationships
   - Interactive tree navigation

5. **Audio Examples:**
   - Play pronunciation for each component
   - Emphasis on particles and verb forms
   - Native speaker recordings

6. **Comparison Mode:**
   - Compare similar grammar patterns
   - Show usage differences
   - Context-appropriate examples

### Potential Improvements

1. **Offline Grammar Database:**
   - Cache common patterns
   - Faster responses for known structures
   - Reduce API calls

2. **Smart Pattern Detection:**
   - Automatically detect grammar patterns in new messages
   - Suggest explanations proactively
   - "You might want to learn about..."

3. **Spaced Repetition Integration:**
   - Track when grammar was explained
   - Surface forgotten patterns
   - Personalized review schedule

4. **Community Contributions:**
   - User-submitted explanations
   - Vote on best explanations
   - Share grammar tips

## Testing

### Test Cases

**TC1: Basic Long-Press Interaction**
- Open chat with AI messages
- Long-press any message
- Verify bottom sheet appears
- Verify loading indicator shows

**TC2: Grammar Component Rendering**
- Request explanation for "これは本です"
- Verify components highlighted in original text
- Verify each component has correct color
- Verify type badges match component types

**TC3: Level-Appropriate Explanations**
- Test with Level 1 user: expect simple Korean
- Test with Level 2 user: expect intermediate terms
- Test with Level 3 user: expect advanced grammar terminology

**TC4: Conversation Context**
- Have 5+ message conversation
- Request explanation
- Verify "Examples" section shows conversation messages
- Verify examples are relevant to grammar pattern

**TC5: Error Handling**
- Simulate API failure
- Verify fallback explanation shown
- Verify user can dismiss error state
- Verify can retry with another message

**TC6: Detailed Explanation Toggle**
- Expand detailed explanation
- Verify full text shows
- Collapse detailed explanation
- Verify text hides

**TC7: Related Patterns**
- Request explanation with related patterns
- Verify patterns show in separate section
- Verify patterns are relevant to explained grammar

**TC8: Dismiss Interaction**
- Open grammar sheet
- Tap outside sheet → should dismiss
- Tap close button → should dismiss
- Verify state cleared after dismiss

### Manual Testing

1. **Visual Inspection:**
   - All colors display correctly
   - Text is readable on all backgrounds
   - Layout adapts to different content lengths
   - No text overflow or truncation

2. **User Flow:**
   - Long-press feels natural
   - Sheet animation is smooth
   - Loading state is clear
   - Navigation is intuitive

3. **Content Quality:**
   - Explanations are accurate
   - Korean is natural and clear
   - Examples are helpful
   - Related patterns are relevant

## Troubleshooting

### Issue: Long-press not working

**Cause:** combinedClickable not properly configured

**Solution:**
- Check MessageBubble has onLongPress parameter
- Verify combinedClickable modifier applied
- Ensure onLongClick callback set

### Issue: Grammar sheet shows but stays loading

**Cause:** API call failing or hanging

**Solution:**
- Check network connectivity
- Verify Gemini API key configured
- Check logcat for API errors
- Ensure timeout handling in place

### Issue: Components not highlighted

**Cause:** startIndex/endIndex mismatch

**Solution:**
- Verify Gemini returns accurate indices
- Check buildHighlightedText algorithm
- Handle edge cases (components at start/end)
- Ensure components sorted by startIndex

### Issue: Korean text garbled

**Cause:** Character encoding issue

**Solution:**
- Verify UTF-8 encoding throughout
- Check JSON parsing preserves Korean characters
- Test with various Korean characters

### Issue: Related patterns empty

**Cause:** Gemini not returning patterns

**Solution:**
- Update prompt to emphasize related patterns
- Provide examples in prompt
- Add fallback related patterns based on detected grammar

## Code Examples

### Custom Grammar Pattern

```kotlin
// Add custom grammar type
enum class GrammarType(val colorCode: String, val label: String) {
    // ... existing types ...
    KEIGO("0xFFFF5722", "경어")  // Deep Orange for honorifics
}
```

### Manual Grammar Explanation

```kotlin
// Create manual explanation for common patterns
fun getManualExplanation(pattern: String): GrammarExplanation? {
    return when (pattern) {
        "~たことがある" -> GrammarExplanation(
            originalText = pattern,
            components = listOf(/* ... */),
            overallExplanation = "경험을 나타내는 표현입니다.",
            detailedExplanation = "과거에 한 번이라도 경험한 적이 있는지 물어볼 때...",
            examples = listOf("日本に行ったことがあります"),
            relatedPatterns = listOf("~たことがない", "~たことがありますか")
        )
        else -> null
    }
}
```

### Batch Grammar Analysis

```kotlin
// Analyze multiple sentences
suspend fun analyzeConversation(messages: List<Message>): List<GrammarExplanation> {
    return messages
        .filter { !it.isUser }
        .map { message ->
            repository.explainGrammar(
                sentence = message.content,
                conversationHistory = messages,
                userLevel = 1
            )
        }
}
```

## Summary

The Grammar Explanation feature provides:

✅ **Interactive Learning:** Long-press any message for instant grammar breakdown
✅ **Visual Highlighting:** Color-coded grammar components with 8 distinct types
✅ **Korean Explanations:** Level-appropriate explanations in user's native language
✅ **Contextual Examples:** Uses actual conversation for relevant examples
✅ **Comprehensive Analysis:** Overall summary + detailed breakdown + related patterns
✅ **Beautiful UI:** Material 3 bottom sheet with collapsible sections
✅ **Gemini-Powered:** AI-generated explanations adapt to sentence complexity

This feature transforms passive conversation practice into active grammar learning, helping users understand the structure and patterns behind each Japanese sentence they encounter.
