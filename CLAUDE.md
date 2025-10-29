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

## 🆕 최근 업데이트 (2025-10)

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