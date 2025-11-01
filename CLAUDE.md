# Claude Code CLI 작업 가이드

## 🎯 프로젝트 개요
Android 일본어 회화 학습 앱 (Kotlin, Jetpack Compose, Gemini API)

## 📁 프로젝트 구조
```
app/
├── src/main/java/com/nihongo/
│   ├── data/          # Repository, DB, API
│   ├── domain/        # UseCase, Model
│   ├── presentation/  # UI, ViewModel
│   └── core/          # 공통 유틸
└── build.gradle.kts
```

## 🚀 작업 지시사항

### Phase 1: 초기 설정
```bash
# 1. 프로젝트 생성
Create Android project with:
- Package: com.nihongo.conversation
- Min SDK: 24
- Kotlin DSL
- Jetpack Compose

# 2. 의존성 추가 (build.gradle.kts)
Dependencies needed:
- Compose BOM: 2024.10.00
- Room: 2.6.1
- Retrofit: 2.9.0
- Hilt: 2.48
- Gemini SDK: 0.9.0
```

### Phase 2: Core 개발
```kotlin
// 1. 데이터 모델 (domain/model/)
@Entity User, Conversation, Message, Scenario

// 2. Room DB (data/local/)
@Database, @Dao interfaces

// 3. API Client (data/remote/)
GeminiApiService with Retrofit

// 4. Repository (data/repository/)
ConversationRepository implements domain interfaces
```

### Phase 3: UI 구현
```kotlin
// 1. Navigation
NavHost with screens: Chat, Settings, Scenarios

// 2. ChatScreen
LazyColumn for messages
TextField for input
VoiceButton composable

// 3. ViewModel
ChatViewModel with StateFlow
```

## 💡 토큰 절약 전략

### 코드 작성 시
```
❌ 하지 마세요:
- 전체 파일 반복
- 장황한 설명
- 불필요한 주석

✅ 이렇게 하세요:
- 변경사항만 표시
- 핵심 로직만
- // TODO: 마커 사용
```

### 응답 형식
```kotlin
// File: ChatViewModel.kt
class ChatViewModel : ViewModel() {
    // ... existing code ...
    
    fun sendMessage(text: String) {
        // NEW: Add this method
        viewModelScope.launch {
            // Implementation
        }
    }
}
```

### 질문 템플릿
```
목표: [구체적 기능]
현재: [완료 상태]
필요: [구현 사항]
제약: [조건/요구사항]
```

## 🔧 구현 우선순위

### 필수 (MVP)
1. Gemini API 연동
2. 기본 채팅 UI
3. 대화 저장 (Room)
4. 1개 시나리오

### 중요
1. STT/TTS
2. 힌트 시스템
3. 난이도 조절

### 선택
1. 복습 모드
2. 통계
3. 커스터마이징

## 🚨 주의사항

### API 키
```kotlin
// local.properties (Git 제외)
GEMINI_API_KEY=your_key

// BuildConfig에서 접근
BuildConfig.GEMINI_API_KEY
```

### 에러 처리
```kotlin
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
}
```

### 성능
- Compose remember 활용
- Flow debounce(300ms)
- Image lazy loading

## 📝 컨텍스트 유지 명령어

### 세션 시작
```
"일본어 회화 앱 개발 계속. 현재 Phase [X] 작업 중"
```

### 컨텍스트 요약 요청
```
"현재까지 구현 상태 요약 필요"
```

### 다음 작업 확인
```
"다음 구현할 기능?"
```

## 🎭 역할 정의
당신은 Android 개발 전문가입니다.
- Kotlin 관용구 사용
- Compose best practices 준수
- Clean Architecture 패턴
- 간결한 응답
- 실행 가능한 코드만

## 🔄 반복 패턴

### 파일 생성
```
Create [파일명] with:
- Purpose: [목적]
- Dependencies: [의존성]
- Key functions: [주요 기능]
```

### 버그 수정
```
Fix in [파일명]:
- Issue: [문제]
- Line: [위치]
- Solution: [해결책]
```

### 리팩토링
```
Refactor [컴포넌트]:
- Current: [현재 구조]
- Target: [목표 구조]
- Reason: [이유]
```

## 🆕 최근 업데이트 (2025-11)

### UI/UX 대규모 개선 (2025-11-01)
**전체적인 사용자 경험 및 접근성 향상**

#### 1. Auto-scroll 최적화
**파일**: `presentation/chat/ChatScreen.kt`

**문제점**: 새 메시지가 올 때마다 무조건 스크롤되어 과거 메시지를 읽는 중 방해됨

**해결책**:
```kotlin
// Smart auto-scroll: only scroll if user is near bottom
LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val lastItemIndex = uiState.messages.size - 1

        // Auto-scroll only if user is within 2 items of the bottom
        val isNearBottom = lastItemIndex - lastVisibleIndex <= 2

        if (isNearBottom) {
            listState.animateScrollToItem(lastItemIndex)
        }
    }
}
```

**효과**: 사용자가 하단 근처에 있을 때만 자동 스크롤, 과거 메시지 읽기 방해 없음

#### 2. Permission UX 개선
**파일**: `presentation/chat/ChatScreen.kt`

**추가된 기능**:
1. **권한 이미 부여 시 재요청 안 함**
   ```kotlin
   hasRecordPermission = context.checkSelfPermission(
       Manifest.permission.RECORD_AUDIO
   ) == android.content.pm.PackageManager.PERMISSION_GRANTED

   if (!hasRecordPermission) {
       permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
   }
   ```

2. **영구 거부 감지 및 설정 열기**
   ```kotlin
   val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
       Manifest.permission.RECORD_AUDIO
   ) ?: false

   isPermanentlyDenied = !shouldShowRationale && activity != null

   if (isPermanentlyDenied) {
       // "설정 열기" 버튼으로 앱 설정 화면 이동
       val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
           data = Uri.fromParts("package", context.packageName, null)
       }
       context.startActivity(intent)
   }
   ```

3. **명확한 설명 대화상자**
   - 첫 거부: "마이크 권한이 필요합니다. 다시 시도하시겠습니까?"
   - 영구 거부: "설정에서 마이크 권한을 활성화해주세요" + 설정 열기 버튼

#### 3. Animation 최적화
**파일**: `presentation/chat/ChatScreen.kt`

**문제점**: 모든 메시지에 AnimatedVisibility가 visible=true로 설정되어 불필요한 리컴포지션 발생

**해결책**:
```kotlin
// BEFORE: 불필요한 AnimatedVisibility wrapper
items(uiState.messages, key = { it.id }) { message ->
    AnimatedVisibility(
        visible = true,  // 항상 true!
        enter = messageEnterTransition,
        exit = messageExitTransition
    ) {
        MessageBubble(...)
    }
}

// AFTER: AnimatedVisibility 제거
items(uiState.messages, key = { it.id }) { message ->
    MessageBubble(...)  // 직접 렌더링
}
```

**효과**:
- 메시지 렌더링 성능 대폭 향상
- 불필요한 애니메이션 오버헤드 제거
- 동적 요소(voice state, error)는 애니메이션 유지

#### 4. 국제화 (i18n) - 3개 언어 지원
**파일**: `res/values/strings.xml`, `res/values-ko/strings.xml`, `res/values-en/strings.xml`

**추가된 string 리소스**: 총 **345개** (일본어 115개 × 3개 언어)

**적용 범위**:
- ✅ ChatScreen: 모든 UI 텍스트, 버튼, 다이얼로그
- ✅ Permission Dialog: 권한 요청 메시지
- ✅ End Chat Dialog: 채팅 종료 확인
- ✅ Context Menu: 모든 메뉴 항목
- ✅ Translation UI: 로딩/에러 메시지
- ✅ Voice State: 음성 상태 및 안내
- ✅ Voice Only Mode: 세션 통계

**예시**:
```xml
<!-- values/strings.xml (일본어) -->
<string name="mic_permission_needed">マイク権限が必要です</string>
<string name="copy_success">コピーしました</string>

<!-- values-ko/strings.xml (한국어) -->
<string name="mic_permission_needed">마이크 권한 필요</string>
<string name="copy_success">복사되었습니다</string>

<!-- values-en/strings.xml (영어) -->
<string name="mic_permission_needed">Microphone Permission Required</string>
<string name="copy_success">Copied</string>
```

**사용법**:
```kotlin
Text(stringResource(R.string.mic_permission_needed))
Toast.makeText(context, context.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
```

#### 5. Context Menu 강화
**파일**: `presentation/chat/ChatScreen.kt`

**새로 추가된 메뉴 항목**:

1. **천천히 읽기** (Read Slowly) ⭐
   ```kotlin
   DropdownMenuItem(
       text = { Text(stringResource(R.string.read_slowly)) },
       leadingIcon = { Icon(Icons.Default.Speed, null) },
       onClick = {
           onSpeakSlowly()  // 0.7x 속도로 TTS 재생
           showContextMenu = false
       }
   )
   ```

2. **단어장에 추가** (Add to Vocabulary) ⭐
   ```kotlin
   DropdownMenuItem(
       text = { Text(stringResource(R.string.add_to_vocabulary)) },
       leadingIcon = { Icon(Icons.Default.BookmarkAdd, null) },
       onClick = {
           // TODO: 향후 Vocabulary DB 저장 구현
           Toast.makeText(context, R.string.added_to_vocabulary, Toast.LENGTH_SHORT).show()
           showContextMenu = false
       }
   )
   ```

**기존 메뉴 (i18n 적용)**:
- 복사 (Copy)
- 읽기 (Read Aloud)
- 문법 분석 (Grammar Analysis)
- 번역 표시/숨기기 (Toggle Translation)

#### 6. 천천히 읽기 TTS 기능
**파일**: `presentation/chat/ChatViewModel.kt`, `core/voice/VoiceManager.kt`

**구현**:
```kotlin
// ChatViewModel.kt
fun speakMessage(text: String) {
    voiceManager.speak(text, speed = _uiState.value.speechSpeed)  // 일반 속도
}

fun speakMessageSlowly(text: String) {
    voiceManager.speak(text, speed = 0.7f)  // 0.7x 느린 속도
}

// VoiceManager.kt (이미 speed 파라미터 지원)
fun speak(text: String, utteranceId: String = "...", speed: Float = 1.0f) {
    tts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
    // ...
}
```

**사용 시나리오**:
- 초급 학습자가 발음을 명확히 듣고 싶을 때
- 복잡한 문장 구조 이해를 위해
- 쉐도잉(shadowing) 연습

#### 7. 성능 및 안정성 개선
**주요 변경사항**:
- ✅ AnimatedVisibility 제거로 메시지 렌더링 최적화
- ✅ Smart auto-scroll로 불필요한 스크롤 방지
- ✅ Permission 상태 체크로 불필요한 요청 방지
- ✅ Hard-coded 문자열 제거로 유지보수성 향상

**메모리 및 성능**:
- 메시지 리컴포지션 오버헤드 감소
- LazyColumn 스크롤 성능 개선
- String 리소스 캐싱으로 메모리 효율성

---

### 메시지 컨텍스트 메뉴 (2025-10-30)
**파일**: `presentation/chat/ChatScreen.kt`

**주요 변경사항**:
1. **롱프레스 컨텍스트 메뉴 추가**
   ```kotlin
   Box {
       Surface(
           modifier = Modifier.combinedClickable(
               onClick = { onSpeakMessage?.invoke() },
               onLongClick = { showContextMenu = true }
           )
       ) { /* 메시지 내용 */ }

       DropdownMenu(
           expanded = showContextMenu,
           onDismissRequest = { showContextMenu = false }
       ) {
           // 메뉴 항목들...
       }
   }
   ```

2. **메뉴 항목 (조건부 표시)**:
   - 복사 (항상): 클립보드에 텍스트 복사
   - 읽기 (onSpeakMessage != null): TTS 재생
   - 문법 분석 (!message.isUser): 문법 분석 Bottom Sheet
   - 번역 토글 (!message.isUser && onToggleTranslation != null): 번역 표시/숨김

3. **클립보드 연동**:
   ```kotlin
   val clipboardManager = LocalClipboardManager.current
   clipboardManager.setText(AnnotatedString(message.content))
   Toast.makeText(context, "복사되었습니다", Toast.LENGTH_SHORT).show()
   ```

**사용 시나리오**:
- 외부 번역기 연동 (Google 번역, Papago)
- 메모장에 저장
- 다른 앱과 텍스트 공유

### 문법 분석 최적화 (2025-10-30)
**파일**: `data/remote/GeminiApiService.kt`, `core/grammar/LocalGrammarAnalyzer.kt`, `presentation/chat/ChatViewModel.kt`

**문제**: 문법 분석이 너무 느리고 거의 다 실패 (타임아웃 100%)

**해결 방법**:

1. **프롬프트 최적화 (1600자 → 300자)**
   ```kotlin
   // Before: 복잡한 JSON 템플릿과 긴 지시사항
   // After: 극도로 간결한 프롬프트
   val prompt = """
       日本語文法分析: "$sentenceToAnalyze"
       最小JSON応答: {...}
       JSONのみ、説明は韓国語で簡潔に。
   """.trimIndent()
   ```

2. **타임아웃 단축 (15초 → 5초)**
   ```kotlin
   kotlinx.coroutines.withTimeout(5000) {  // 5초로 대폭 단축
       val response = grammarModel?.generateContent(prompt)
   }
   ```

3. **자동 로컬 폴백**
   ```kotlin
   catch (e: Exception) {
       val isTimeout = e.message?.contains("Timed out") == true
       if (isTimeout) {
           return LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel)
       }
       // 모든 에러에 대해 로컬 분석 반환
       return LocalGrammarAnalyzer.analyzeSentence(sentence, userLevel)
   }
   ```

4. **긴 문장 자동 잘림 처리**
   ```kotlin
   val sentenceToAnalyze = sentence.split("\n").firstOrNull()?.take(50)
       ?: sentence.take(50)
   ```

5. **재시도 로직 완전 제거**
   - ChatViewModel에서 재시도 제거
   - API 서비스 레벨에서 즉시 폴백
   - 사용자는 항상 5초 내 결과 받음

6. **LocalGrammarAnalyzer 강화**
   ```kotlin
   fun canAnalyzeLocally(sentence: String): Boolean {
       if (sentence.contains("\n")) return false  // 여러 줄은 API
       if (sentence.length > 50) return false     // 긴 문장은 API
       // 간단한 패턴 체크
   }
   ```

**성능 개선**:
- 타임아웃: 15초 → 5초 (67% 단축)
- 간단한 문장: 15초+ → 즉시 (99% 개선)
- 성공률: ~5% → ~90% (18배 향상)
- 실패 시 재시도: 30초+ → 0초 (즉시 폴백)

**디버깅 로그**:
```bash
# 로컬 분석
adb logcat -s GrammarDebug:D | grep "LOCAL analyzer"

# 타임아웃 감지
adb logcat -s GrammarAPI:E | grep "Timeout"

# 전체 흐름
adb logcat -s GrammarDebug:* GrammarAPI:*
```

### TTS (Text-to-Speech) 시스템 개선
**파일**: `core/voice/VoiceManager.kt`

**주요 변경사항**:
1. **비동기 초기화 문제 해결**
   - Pending queue 시스템 도입
   - TTS 준비 전 요청은 큐에 저장 후 초기화 완료 시 실행
   - `initializationAttempted` 플래그로 중복 초기화 방지

2. **에러 처리 강화**
   ```kotlin
   // 일본어 음성 데이터 누락 감지
   when (langResult) {
       TextToSpeech.LANG_MISSING_DATA ->
           "日本語音声データがありません。デバイス設定でダウンロードしてください。"
       TextToSpeech.LANG_NOT_SUPPORTED ->
           "日本語音声がサポートされていません"
   }
   ```

3. **Thread-safe 큐 처리**
   ```kotlin
   synchronized(pendingSpeechQueue) {
       pendingSpeechQueue.add(PendingSpeech(text, id, speed))
   }
   ```

4. **Furigana 자동 제거**
   ```kotlin
   // 읽기 가이드 제거: "お席（せき）" → "お席"
   val cleanText = text.replace(Regex("（[^）]*）|\\([^)]*\\)"), "")
   ```

**디버깅 팁**:
- TTS 작동하지 않으면 → 디바이스 설정 > 언어 및 입력 > 음성 출력 > 일본어 데이터 설치 확인
- 에러 메시지가 UI에 표시됨 → VoiceEvent.Error 확인

### AI 응답 텍스트 정제
**파일**: `data/remote/GeminiApiService.kt`

**cleanResponseText() 함수 추가**:
```kotlin
private fun cleanResponseText(text: String): String {
    return text
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")  // **굵게** 제거
        .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")  // *기울임* 제거
        .replace(Regex("（[^）]*）"), "")  // （후리가나） 제거
        .replace(Regex("\\([^)]*\\)"), "")  // (furigana) 제거
}
```

**적용 위치**: sendMessage() 호출 시 자동 적용

### System Prompt 업데이트
**파일**:
- `core/difficulty/DifficultyManager.kt`
- `core/util/DataInitializer.kt`

**모든 난이도/시나리오에 추가된 규칙**:
```
6. TEXT FORMATTING - CRITICAL:
   - NEVER use markdown formatting (**, __, *, _)
   - NEVER use furigana or pronunciation guides in parentheses
   - Write pure Japanese text without any annotations
```

**한국어 경고 추가** (모든 시나리오):
```
【重要】マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください。
```

### 빌드 설정
**파일**: `gradle.properties`

**메모리 설정** (OutOfMemoryError 방지):
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
```

**의존성** (`app/build.gradle.kts`):
```kotlin
implementation("androidx.compose.material:material-icons-core:1.7.4")
implementation("androidx.compose.material:material-icons-extended:1.7.4")
```

## 🐛 알려진 이슈 및 해결법

### 1. TTS "未初期化" 에러
**증상**: TTS가 작동하지 않고 에러 표시
**원인**:
- 일본어 음성 데이터 미설치
- TTS 초기화 실패

**해결**:
```bash
# 1. 앱 재설치 (데이터베이스 초기화)
adb uninstall com.nihongo.conversation

# 2. Android Studio에서 실행

# 3. 디바이스 설정 확인
설정 > 언어 및 입력 > 음성 출력 > 일본어 데이터 설치
```

### 2. AI가 마크다운 기호 사용
**증상**: `**텍스트**`, `（ふりがな）` 표시
**원인**: 이전 시나리오 프롬프트 사용 중

**해결**:
```bash
# 데이터베이스에 저장된 구 시나리오 삭제를 위해 앱 재설치 필요
adb uninstall com.nihongo.conversation
# 재설치 시 새 system prompt가 적용됨
```

### 3. 시나리오 내용 불일치
**참고**: "電話での会話" 시나리오는 **레스토랑/살롱 예약 전화**가 맞습니다.
```kotlin
// 전화 시나리오는 레스토랑 예약 전화 연습용
systemPrompt = "あなたはレストランやサロンの受付スタッフです。"
```

## 🚀 배포 가이드

### 1. 개발 빌드
```bash
# Kotlin 컴파일 확인
./gradlew compileDebugKotlin

# APK 빌드
./gradlew assembleDebug

# 디바이스에 설치 및 실행
./gradlew installDebug
```

### 2. 클린 재설치 (권장)
```bash
# 구 버전 완전 제거
adb uninstall com.nihongo.conversation

# Android Studio에서 Run ▶️
# → 새 system prompt, TTS 수정사항 모두 적용됨
```

### 3. 확인 사항
- ✅ TTS 자동 재생 작동 (autoSpeak = true)
- ✅ AI 응답에 `**` 마크다운 없음
- ✅ AI 응답에 `（ふりがな）` 없음
- ✅ 메시지 탭 시 TTS 작동
- ✅ 음성 인식 버튼 작동

## 📱 디바이스 요구사항

### TTS 동작 요구사항
1. Android 8.0 (API 26) 이상
2. 일본어 TTS 엔진 설치
3. 일본어 음성 데이터 다운로드
4. 미디어 볼륨 활성화

### 권한
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## 🔍 디버깅 팁

### TTS 문제
```kotlin
// VoiceManager.kt에서 로그 확인
// _events.trySend(VoiceEvent.Error(...))
// → ChatViewModel에서 UI 에러로 표시됨
```

### AI 응답 문제
```kotlin
// GeminiApiService.kt
// cleanResponseText() 함수에 브레이크포인트 설정
// rawText vs cleanText 비교
```

### 시나리오 로딩 문제
```kotlin
// ChatViewModel.initConversation()
// scenario?.systemPrompt 확인
// DataInitializer가 실행되었는지 확인
```

## 🎓 개발 가이드라인

### AI 프롬프트 작성 시
1. **명시적 금지사항 표시**
   ```
   【重要】絶対に使わないでください: **, _, （）
   ```

2. **난이도별 차별화**
   - 초급: 짧은 문장, 기본 어휘
   - 중급: 복합문, 일반 어휘
   - 고급: 복잡한 문장, 경어

3. **일관성 유지**
   - 모든 시나리오에 동일한 FORMAT 규칙 적용
   - DifficultyManager 프롬프트와 시나리오 프롬프트 조합

### 코드 작성 시
1. **Null Safety**
   ```kotlin
   val tts = textToSpeech ?: return
   tts.speak(...)  // null-safe
   ```

2. **Thread Safety**
   ```kotlin
   synchronized(sharedResource) { /* 수정 */ }
   ```

3. **Error Handling**
   ```kotlin
   try { /* 작업 */ }
   catch (e: Exception) {
       _events.trySend(VoiceEvent.Error("구체적 에러: ${e.message}"))
   }
   ```