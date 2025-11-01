# 🗾 Presentation 패키지 일본어 하드코딩 감사 보고서

**생성일**: 2025-11-01  
**목적**: 다국어 지원을 위해 strings.xml로 이동해야 할 하드코딩된 일본어 텍스트 식별

---

## 📊 요약

- **총 파일 수**: 26개
- **카테고리**: Error Messages, UI Labels, Button Text, Placeholders
- **우선순위**: 높음 (다국어 지원 필수)

---

## 📁 파일별 일본어 텍스트 목록

### 1. ViewModel - Error Messages

#### AddVocabularyViewModel.kt
```kotlin
Line 161: "単語を入力してください"          // Please enter a word
Line 168: "意味を入力してください"          // Please enter meaning
Line 202: "この単語は既に追加されています"   // This word is already added
Line 209: "保存に失敗しました"              // Save failed
```

#### UserSelectionViewModel.kt
```kotlin
Line 52:  "ユーザーの読み込みに失敗しました"    // Failed to load users
Line 81:  "ユーザーの選択に失敗しました"        // Failed to select user
Line 118: "ユーザーの作成に失敗しました"        // Failed to create user
Line 134: "ログアウトに失敗しました"            // Logout failed
```

#### StatsViewModel.kt
```kotlin
Line 75:  "統計データの読み込みに失敗しました"  // Failed to load stats data
```

#### ConversationHistoryViewModel.kt
```kotlin
Line 59:  "履歴の読み込みに失敗しました"        // Failed to load history
Line 114: "会話の削除に失敗しました"            // Failed to delete conversation
```

#### PronunciationHistoryViewModel.kt
```kotlin
Line 84:  "履歴の読み込みに失敗しました"        // Failed to load history
Line 104: "データの削除に失敗しました"          // Failed to delete data
```

#### FlashcardStatsViewModel.kt
```kotlin
Line 70:  "統計データの読み込みに失敗しました"  // Failed to load stats
```

#### FlashcardReviewViewModel.kt
```kotlin
Line 63:  "復習カードの読み込みに失敗しました"  // Failed to load review cards
```

#### ProfileViewModel.kt
```kotlin
Line 110: "プロフィールの更新に失敗しました"    // Failed to update profile
Line 146: "アバターの更新に失敗しました"        // Failed to update avatar
```

---

### 2. Screen - UI Text & Labels

#### ReviewScreen.kt
```kotlin
Line 45:  "復習モード"                          // Review mode
Line 49:  "過去の会話を復習しましょう"          // Let's review past conversations
Line 59:  "戻る"                                // Back
Line 97:  "エラーが発生しました"                // An error occurred
Line 121: "まだ会話がありません"                // No conversations yet
Line 126: "シナリオから会話を始めましょう！"    // Start a conversation from scenarios!
Line 224: "会話"                                // Conversation
Line 229: "完了: "                              // Completed: 
Line 246: "閉じる" / "開く"                     // Close / Open
Line 290: "分"                                  // minutes
Line 309: "~{n}語"                             // ~N words
Line 357: "重要フレーズ"                        // Important phrases
Line 427: "再生"                                // Play
Line 480: "初級"                                // Beginner
Line 481: "中級"                                // Intermediate
Line 482: "上級"                                // Advanced
```

#### ConversationHistoryScreen.kt
```kotlin
Line 37:  "会話履歴"                            // Conversation history
Line 41:  "{n}件の会話"                         // N conversations
Line 51:  "戻る"                                // Back
Line 74:  "シナリオや会話内容を検索..."        // Search scenarios or conversation content...
Line 86:  "クリア"                              // Clear
Line 107: "すべて"                              // All
Line 118: "進行中"                              // In progress
Line 129: "完了"                                // Completed
Line 144: "シナリオ"                            // Scenario
Line 227: "条件に一致する会話が見つかりません"  // No conversations match the criteria
Line 229: "会話履歴がありません"                // No conversation history
Line 236: "シナリオから会話を始めましょう！"    // Start a conversation from scenarios!
Line 278: "シナリオで絞り込み"                  // Filter by scenario
Line 283: "すべてのシナリオ"                    // All scenarios
Line 314: "閉じる"                              // Close
Line 331: "会話を削除"                          // Delete conversation
Line 332: "この会話を削除してもよろしいですか？" // Are you sure you want to delete?
Line 340: "削除"                                // Delete
Line 345: "キャンセル"                          // Cancel
Line 469: "{n}件"                               // N items
Line 516: "再開" / "続ける"                     // Resume / Continue
```

#### PitchAccentVisualization.kt
```kotlin
Line 40:  "ピッチアクセント分析"                // Pitch accent analysis
Line 76:  "平板" / "平板（下がらない）"         // Heiban (flat)
Line 77:  "頭高" / "頭高（最初で下がる）"       // Atamadaka (initial drop)
Line 78:  "中高" / "中高（中間で下がる）"       // Nakadaka (middle drop)
Line 79:  "尾高" / "尾高（最後で下がる）"       // Odaka (final drop)
Line 132: "モーラ別ピッチ"                      // Pitch by mora
Line 227: "ピッチ曲線"                          // Pitch curve
Line 349: "ピッチパターン"                      // Pitch pattern
Line 360: "あなた:"                             // You:
Line 384: "正解:"                               // Correct:
Line 416: "信頼度:"                             // Confidence:
Line 454: "完璧です！ネイティブと同じピッチです。"  // Perfect! Same pitch as native
Line 461: "もう少しです。正解のパターンを練習しましょう。" // Almost there. Practice the correct pattern
```

#### IntonationVisualizer.kt
```kotlin
Line 40:  "イントネーション分析"                // Intonation analysis
Line 78:  "平叙文" / "文末が下がる"             // Statement / End falls
Line 84:  "疑問文" / "文末が上がる"             // Question / End rises
Line 90:  "感嘆文" / "強い感情"                 // Exclamation / Strong emotion
Line 96:  "命令文" / "急激に下がる"             // Command / Sharp drop
Line 159: "イントネーション曲線"                // Intonation curve
Line 282: "文末上昇"                            // Final rise
Line 287: "文末下降"                            // Final fall
Line 408: "改善のヒント"                        // Improvement hints
Line 439: "完璧です！平叙文のイントネーションが自然です。"  // Perfect! Natural statement intonation
Line 440: "素晴らしい！疑問文の上昇イントネーションが正確です。"  // Excellent! Accurate question rising intonation
Line 451: "文末をもう少し下げてみましょう。平叙文は下降調です。"  // Try lowering the end more. Statements fall
Line 452: "文末を上げて疑問の気持ちを表現しましょう。"  // Raise the end to express questioning
Line 453: "感情をもっと込めて、音程の変化を大きくしましょう。"  // Put more emotion, make pitch changes bigger
Line 454: "もっと強く、急激に下げてみましょう。"  // Try dropping more sharply and strongly
```

#### FeedbackCard.kt
```kotlin
Line 149: "より良い表現:"                       // Better expression:
Line 177: "詳細を隠す" / "詳細を見る"           // Hide details / See details
Line 216: "確認済み"                            // Acknowledged
Line 228: "適用"                                // Apply
Line 243: "間違い"                              // Error
Line 244: "注意"                                // Warning
Line 245: "情報"                                // Info
Line 304: "文法"                                // Grammar
Line 305: "不自然な表現"                        // Unnatural expression
Line 306: "より良い表現"                        // Better expression
Line 307: "会話の流れ"                          // Conversation flow
Line 308: "敬語レベル"                          // Politeness level
```

#### AddVocabularyScreen.kt
```kotlin
Line 31:  "単語を追加しました！"                // Word added!
```

#### UserSelectionScreen.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### ChatScreen.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### PronunciationPracticeSheet.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### VoiceOnlyComponents.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### FlashcardStatsScreen.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### SentenceCardPracticeSheet.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### PronunciationHistoryScreen.kt
```kotlin
// (많은 UI 텍스트 포함 - 별도 파일 참조)
```

#### Charts.kt
```kotlin
Line 35:  "分"                                  // minutes (label)
```

#### VoiceButton.kt
```kotlin
// (UI 텍스트 포함 - 별도 파일 참조)
```

#### DifficultyIndicator.kt
```kotlin
// (난이도 레벨 텍스트 포함)
```

#### Avatars.kt
```kotlin
// (아바타 관련 텍스트)
```

---

## 📋 카테고리별 분류

### 🔴 높은 우선순위 (Error Messages & User Feedback)
- ViewModel error messages (8개 파일, ~20개 메시지)
- Toast/Snackbar messages
- Validation messages

### 🟡 중간 우선순위 (UI Labels & Navigation)
- Screen titles
- Tab labels
- Button text
- Navigation labels

### 🟢 낮은 우선순위 (Content Labels)
- Statistical labels (분, 語, 件)
- Content descriptions
- Placeholder text

---

## 🔧 권장 수정 방법

### 1. strings.xml 생성
```xml
<!-- res/values/strings.xml (한국어) -->
<resources>
    <!-- Error Messages -->
    <string name="error_word_required">단어를 입력해주세요</string>
    <string name="error_meaning_required">의미를 입력해주세요</string>
    <string name="error_word_duplicate">이 단어는 이미 추가되어 있습니다</string>
    <string name="error_save_failed">저장에 실패했습니다</string>
    
    <!-- UI Labels -->
    <string name="review_mode">복습 모드</string>
    <string name="conversation_history">대화 기록</string>
    <string name="beginner">초급</string>
    <string name="intermediate">중급</string>
    <string name="advanced">고급</string>
    
    <!-- ... -->
</resources>
```

```xml
<!-- res/values-ja/strings.xml (일본어) -->
<resources>
    <!-- Error Messages -->
    <string name="error_word_required">単語を入力してください</string>
    <string name="error_meaning_required">意味を入力してください</string>
    <string name="error_word_duplicate">この単語は既に追加されています</string>
    <string name="error_save_failed">保存に失敗しました</string>
    
    <!-- UI Labels -->
    <string name="review_mode">復習モード</string>
    <string name="conversation_history">会話履歴</string>
    <string name="beginner">初級</string>
    <string name="intermediate">中級</string>
    <string name="advanced">高級</string>
    
    <!-- ... -->
</resources>
```

### 2. Compose에서 사용
```kotlin
// Before
Text("単語を入力してください")

// After
Text(stringResource(R.string.error_word_required))
```

### 3. ViewModel에서 사용
```kotlin
// Before
error = "保存に失敗しました: ${e.message}"

// After (Context 필요)
@Inject constructor(
    @ApplicationContext private val context: Context
) {
    error = context.getString(R.string.error_save_failed_with_reason, e.message)
}
```

---

## 📌 다음 단계

1. **Phase 1**: ViewModel error messages 이동 (우선순위 높음)
2. **Phase 2**: Screen UI labels 이동
3. **Phase 3**: Content descriptions & accessibility
4. **Phase 4**: 영어(en), 중국어(zh) 번역 추가

---

**참고**: 이 작업은 별도 세션에서 진행하는 것을 권장합니다.
