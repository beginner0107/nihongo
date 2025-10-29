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