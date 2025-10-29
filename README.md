# 🗾 일본어 회화 학습 앱 (NihonGo Conversation)

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_24+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-2.5_Flash-purple.svg)](https://ai.google.dev)

AI 기반 일본어 회화 학습을 위한 개인용 Android 애플리케이션

## 🌟 주요 기능

- 🤖 **AI 대화 파트너**: Gemini 2.5 Flash를 활용한 자연스러운 일본어 대화
- 💭 **맥락 기억**: 이전 대화를 기억하고 관계를 이어가는 친구 같은 AI
- 💡 **AI 힌트 시스템**: 한국어-일본어 번역 힌트, 로마자 표기, 문맥 기반 제안
- 📖 **문법 설명 기능**: 메시지 길게 누르기로 즉시 문법 분석, 색상별 구문 강조, 캐싱으로 즉시 재로딩
- 🌐 **메시지별 번역**: 각 AI 메시지마다 한국어 번역 버튼, 선택적 번역 확인
- 💾 **대화 관리**: 대화 종료 버튼, 히스토리 자동 저장, 새 대화 시작 기능
- 🎭 **6가지 시나리오**: 레스토랑, 쇼핑, 호텔, 친구 만들기, 전화 대화, 병원 (초급/중급/상급)
- 🎙️ **음성 지원**: STT로 일본어 음성 인식, TTS로 AI 응답 자동 재생, 재시도 메커니즘
- ⚙️ **설정 시스템**: 난이도 조절 (1-3), 음성 속도 (0.5x-2.0x), 자동 읽기, 로마자 표시
- 🎯 **난이도 조절**: JLPT 레벨별 AI 응답 (N5-N4/N3-N2/N1), 어휘 복잡도 분석, 별점 표시
- 📊 **학습 통계**: 일일/주간/월간 진도, 연속 학습일 추적, 시나리오별 진행률, 차트 시각화
- 🔥 **복습 모드**: 완료된 대화 재생, 날짜별 그룹화, 중요 문구 추출, 메시지 재생
- 👤 **사용자 프로필**: 아바타 선택, 학습 목표, 개인화된 AI 응답
- ✨ **세련된 UI**: Material 3 디자인, 타이핑 인디케이터, 부드러운 애니메이션, 메시지 타임스탬프

## 🚀 빠른 시작

### 필요 사항
- Android Studio Hedgehog (2023.1.1) 이상
- Android SDK 24 이상
- Kotlin 1.9.0 이상
- Gemini API 키 ([발급하기](https://makersuite.google.com/app/apikey))

### 설치 방법

1. **프로젝트 클론**
```bash
git clone https://github.com/yourusername/nihongo-conversation.git
cd nihongo-conversation
```

2. **API 키 설정**
```properties
# local.properties 파일 생성
GEMINI_API_KEY=your_api_key_here
```

3. **빌드 및 실행**
```bash
./gradlew assembleDebug
# 또는 Android Studio에서 직접 실행
```

## 🏗️ 아키텍처

```
app/
├── src/main/java/com/nihongo/conversation/
│   ├── data/                 # 데이터 레이어
│   │   ├── local/            # Room DB
│   │   ├── remote/           # API 클라이언트
│   │   └── repository/       # Repository 구현
│   ├── domain/               # 도메인 레이어
│   │   ├── model/            # 데이터 모델
│   │   ├── usecase/          # 비즈니스 로직
│   │   └── repository/       # Repository 인터페이스
│   ├── presentation/         # 프레젠테이션 레이어
│   │   ├── ui/               # Compose UI
│   │   ├── viewmodel/        # ViewModels
│   │   └── theme/            # 테마 설정
│   └── core/                 # 공통 유틸리티
│       ├── di/               # Dependency Injection
│       └── utils/            # 헬퍼 함수
└── build.gradle.kts
```

## 🛠️ 기술 스택

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room (SQLite)
- **Persistence**: DataStore Preferences
- **Network**: Retrofit + OkHttp
- **Async**: Coroutines + Flow
- **AI**: Gemini 2.5 Flash API
- **Voice**: Android SpeechRecognizer (STT) + TextToSpeech (TTS)

## 📱 주요 화면

### 대화 화면 (ChatScreen)
- 💬 **채팅 인터페이스**: 비대칭 라운드 모서리 메시지 버블, 타임스탬프
- 🎙️ **음성 입력**: 펄스 애니메이션 마이크 버튼, 실시간 음성 인식
- 🔊 **자동 음성 재생**: AI 응답 자동 읽기, 메시지 클릭으로 재생
- 💡 **AI 힌트 다이얼로그**: 한국어-일본어 번역, 로마자, 설명
- 📖 **문법 설명**: 메시지 길게 누르기로 즉시 문법 분석 (아래 참조)
- ⭐ **복잡도 표시**: AI 메시지에 1-5 별점으로 어휘 난이도 표시
- ⌨️ **스마트 입력**: 엔터키로 전송, 타이핑 인디케이터
- ✨ **부드러운 애니메이션**: Slide-in/fade-in 메시지, 에러 표시

### 문법 설명 기능 (GrammarBottomSheet)
- 🖱️ **즉시 분석**: 메시지 길게 누르기로 문법 분석 시작
- 🎨 **색상 구문 강조**: 8가지 문법 요소별 색상 코딩
  - 🔵 조사 (は, が, を, に, で, と)
  - 🟢 동사 (食べます, 行く, 見る)
  - 🟠 형용사 (きれい, おいしい, 高い)
  - 🟣 명사 (本, 人, 場所)
  - 🔴 보조동사 (ます, です, ている)
  - 🔷 접속사 (が, けど, から, ので)
  - 🟡 부사 (とても, ゆっくり, よく)
  - 🟤 표현 (관용구, 패턴)
- 💡 **간단 설명**: 문장 전체의 1-2줄 요약
- 📝 **구성요소 분석**: 각 문법 요소의 한국어 설명 카드
- 📖 **상세 설명**: 펼쳐보기로 심화 문법 해설
- 💬 **대화 예시**: 현재 대화에서 유사한 문장 추출
- 🎯 **관련 패턴**: 함께 공부하면 좋은 문법 패턴 제안
- 🎓 **레벨별 설명**: 사용자 레벨에 맞춘 설명 난이도

### 시나리오 선택 (ScenarioListScreen)
- 🎭 **6가지 시나리오**: 레스토랑, 쇼핑, 호텔, 친구, 전화, 병원
- 🏷️ **난이도 배지**: 초급(초록)/중급(보라)/상급(빨강)
- 🎨 **아이콘 디자인**: 각 시나리오별 커스텀 아이콘
- ⚙️ **설정 버튼**: TopAppBar에서 빠른 접근

### 설정 화면 (SettingsScreen)
- 📈 **난이도 레벨**: 1-3단계 슬라이더 (초급/중급/상급)
- ⚡ **음성 속도**: 0.5x-2.0x 조절 (0.1x 단위)
- 🔊 **자동 읽기 토글**: AI 응답 자동 음성 재생 on/off
- 🌐 **로마자 표시 토글**: 힌트 로마자 표시 제어
- 💾 **자동 저장**: DataStore로 모든 설정 영구 저장

### 복습 화면 (ReviewScreen)
- 📅 **날짜별 그룹**: 오늘/어제/특정 날짜로 대화 그룹화
- 🎭 **시나리오 표시**: 난이도 배지와 시나리오 정보
- 📖 **확장 가능 카드**: 탭으로 전체 대화 보기
- 🔊 **메시지 재생**: AI 메시지 TTS 재생
- ⭐ **중요 문구**: 자동 추출된 핵심 일본어 표현 (최대 5개)
- ✨ **부드러운 애니메이션**: 확장/축소 전환 효과

### 통계 화면 (StatsScreen)
- 🔥 **연속 학습일**: 현재 연속 기록과 최고 기록 표시
- 📊 **막대 차트**: 일일 학습 시간 (분 단위)
- 📈 **선 차트**: 일일 메시지 수 추세
- 🥧 **파이 차트**: 시나리오별 완료율 분포
- 📅 **주간/월간 뷰**: 필터 칩으로 기간 선택
- 💯 **총계 통계**: 전체 대화 수, 메시지 수, 학습 시간
- 🎨 **Canvas API 차트**: 커스텀 그래픽 시각화

## ✨ 최신 업데이트 (ChatScreen Polish)

### 타이핑 인디케이터 (`TypingIndicator.kt`)
```kotlin
// AI가 메시지를 생성 중일 때 표시되는 애니메이션
- 3개의 점이 순차적으로 크기 변화 (0.5f → 1.0f)
- 각 점마다 150ms 지연으로 자연스러운 파동 효과
- SecondaryContainer 배경의 둥근 말풍선 안에 표시
```

### 메시지 애니메이션
```kotlin
AnimatedVisibility(
    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
    exit = slideOutVertically() + fadeOut()
)
// 새 메시지가 아래에서 슬라이드 업되며 페이드 인
```

### 메시지 버블 디자인 개선
- **비대칭 모서리**: 사용자 메시지는 오른쪽 하단, AI 메시지는 왼쪽 하단이 뾰족 (꼬리 효과)
- **타임스탬프**: HH:mm 형식으로 각 메시지에 표시
- **Tonal Elevation**: 1dp 입체감으로 깊이 추가
- **최대 너비**: 280dp로 제한하여 가독성 확보
- **색상 대비**: onPrimaryContainer/onSecondaryContainer로 명확한 텍스트

### 에러 표시 개선
```kotlin
// 애니메이션 에러 컨테이너
- ErrorOutline 아이콘 + 에러 메시지
- ErrorContainer 배경색으로 시각적 구분
- Slide-in/fade-in 애니메이션으로 부드러운 표시
```

### 키보드 입력 UX
```kotlin
OutlinedTextField(
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
    keyboardActions = KeyboardActions(onSend = { onSend() })
)
// 키보드 엔터키(전송 버튼)로 바로 메시지 전송 가능
```

### 스페이싱 최적화
- 메시지 간격: 12dp (이전 8dp에서 증가)
- LazyColumn 컨텐츠 패딩: 16dp
- 자동 스크롤: 새 메시지 추가 시 애니메이션 스크롤

## 🆕 최신 업데이트 (2025-10-29) - 대화 관리 및 번역 기능 강화

### ✨ 새로운 기능

**1. 대화 종료 및 새 대화 시작**
- ✅ **"チャット終了" 버튼**: 메시지가 있을 때 TopAppBar에 체크마크(✓) 버튼 표시
- ✅ **확인 다이얼로그**: 실수 방지를 위한 확인 절차
- ✅ **대화 히스토리 자동 저장**: 종료된 대화는 복습 모드에서 확인 가능
- ✅ **깨끗한 시작**: 종료 후 동일 시나리오에서 새 대화 시작

**2. 개선된 번역 기능**
- ✅ **메시지별 번역 버튼**: 각 AI 메시지마다 "한국어 번역" 버튼 제공
- ✅ **토글 방식**: 원하는 메시지만 선택적으로 번역 확인
- ✅ **번역 캐싱**: 한 번 번역된 메시지는 즉시 표시
- ✅ **간결한 UI**: 버튼 텍스트가 "번역 숨기기"로 변경

**3. 문법 설명 캐싱**
- ✅ **즉시 로딩**: 같은 문장을 다시 길게 누르면 캐시에서 즉시 표시
- ✅ **스크롤 안정성**: 문법 시트가 더 이상 스크롤로 사라지지 않음
- ✅ **성능 향상**: API 호출 최소화로 데이터 절약

**4. 대화 이력 보존**
- ✅ **자동 복원**: 복습 화면에서 돌아와도 대화 내용 유지
- ✅ **시나리오별 관리**: 각 시나리오마다 독립적인 대화 세션
- ✅ **스마트 저장**: 실제로 메시지를 보낼 때만 대화 생성 (빈 대화 방지)

**5. TTS 안정성 개선**
- ✅ **재시도 메커니즘**: 초기화 실패 시 자동 재시도
- ✅ **시나리오 전환 안정화**: 시나리오를 바꿔도 TTS가 계속 작동
- ✅ **더 나은 에러 메시지**: 문제 발생 시 구체적인 해결 방법 제시

### 🗄️ 데이터베이스 개선

**Schema Version 2 마이그레이션**:
```sql
ALTER TABLE conversations ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0
```
- 활성 대화(isCompleted = 0)와 종료된 대화(isCompleted = 1) 구분
- 기존 데이터 자동 마이그레이션으로 데이터 손실 없음
- 복습 모드에서 종료된 대화만 표시

### 💡 사용 방법

**대화 종료하기**:
1. 채팅 중 우측 상단의 체크마크(✓) 버튼 클릭
2. 확인 다이얼로그에서 "終了" 선택
3. 대화가 히스토리에 저장되고 새 대화 시작

**번역 보기**:
1. AI 메시지 하단의 "한국어 번역" 버튼 클릭
2. 번역이 메시지 아래에 표시됨
3. "번역 숨기기" 버튼으로 다시 숨기기

**문법 설명 빠르게 보기**:
1. AI 메시지 길게 누르기
2. 두 번째부터는 캐시에서 즉시 로딩!

---

## 🆕 이전 업데이트 (2025-10) - TTS 및 텍스트 정제

### TTS (Text-to-Speech) 시스템 대폭 개선

**문제 해결**:
- ✅ **"TTS未初期化" 에러 완전 해결**: 비동기 초기화 문제 수정
- ✅ **마크다운 기호 제거**: AI 응답에서 `**텍스트**`, `*이탤릭*` 자동 제거
- ✅ **후리가나 자동 제거**: `お席（せき）` → `お席` 으로 정제
- ✅ **에러 메시지 개선**: 구체적인 해결 방법 제시

**새로운 기능**:
1. **Pending Queue 시스템**
   - TTS가 준비되기 전 음성 요청을 큐에 저장
   - 초기화 완료 시 자동으로 재생
   - 앱 시작 직후 AI 응답도 정상 재생

2. **일본어 음성 데이터 감지**
   - 디바이스에 일본어 TTS 데이터가 없으면 자동 감지
   - 설치 방법을 포함한 친절한 에러 메시지 표시
   - `설정 > 언어 및 입력 > 음성 출력 > 일본어 다운로드`

3. **텍스트 자동 정제**
   ```kotlin
   // AI 응답 예시 (자동 정제됨):
   // Before: "**冷たい（つめたい）**飲み物（のみもの）"
   // After:  "冷たい飲み物"
   ```

4. **Thread-Safe 처리**
   - 동시성 문제 해결로 안정성 향상
   - 멀티스레드 환경에서도 안전한 TTS 동작

**시스템 프롬프트 업데이트**:
- 모든 시나리오에 텍스트 포맷 규칙 추가
- AI가 마크다운과 후리가나를 사용하지 않도록 명시적 지시
- 3단계 난이도 레벨 모두 적용

### 빌드 설정 개선

**메모리 설정** (`gradle.properties`):
```properties
# OutOfMemoryError 방지
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
```

**의존성 업데이트** (`app/build.gradle.kts`):
```kotlin
// Material Icons 라이브러리
implementation("androidx.compose.material:material-icons-core:1.7.4")
implementation("androidx.compose.material:material-icons-extended:1.7.4")
```

## 🐛 문제 해결 가이드

### TTS가 작동하지 않을 때

**증상**: 음성이 재생되지 않거나 에러 메시지 표시

**해결 방법**:
1. **앱 완전 재설치** (데이터베이스 초기화)
   ```bash
   adb uninstall com.nihongo.conversation
   # Android Studio에서 다시 실행
   ```

2. **일본어 음성 데이터 설치 확인**
   - 디바이스 설정 → 언어 및 입력
   - 음성 출력 → TTS 엔진 설정
   - 일본어 음성 데이터 다운로드

3. **볼륨 확인**
   - 미디어 볼륨이 켜져 있는지 확인
   - 무음 모드 해제

4. **자동 음성 재생 설정**
   - 설정 화면에서 "자동 읽기" 토글 확인
   - 오른쪽 상단 스피커 아이콘 확인

### AI가 이상한 기호를 표시할 때

**증상**: `**텍스트**` 또는 `（ふりがな）` 표시

**원인**: 데이터베이스에 저장된 이전 시스템 프롬프트 사용 중

**해결 방법**:
```bash
# 앱 재설치로 새 프롬프트 적용
adb uninstall com.nihongo.conversation
# Android Studio에서 다시 실행
```

재설치 후 자동으로:
- ✅ 새로운 시스템 프롬프트 적용
- ✅ AI 응답 텍스트 정제 기능 활성화
- ✅ TTS 후리가나 제거 기능 활성화

### 시나리오 내용 확인

**"電話での会話" 시나리오**:
- 이 시나리오는 **레스토랑/살롱 예약 전화 연습**용입니다
- AI가 "레스토랑입니다"라고 응답하는 것이 정상입니다
- 사용자는 전화로 예약하는 역할을 연습합니다

**시나리오 구성**:
1. レストランでの注文 - 레스토랑에서 직접 주문
2. 買い物 - 쇼핑
3. ホテルでのチェックイン - 호텔 체크인
4. 友達を作る - 친구 만들기
5. **電話での会話** - 전화로 예약하기 (레스토랑/살롱)
6. 病院で - 병원 방문

## 🔧 개발 가이드

### Claude Code CLI 사용법

1. **세션 시작**
```bash
claude-code "Continue 일본어 회화 앱 development"
```

2. **컨텍스트 유지**
```bash
# 체크포인트 저장
.claude/session_manager.sh save

# 체크포인트 복원
.claude/session_manager.sh restore [checkpoint_id]
```

3. **효율적인 작업**
- Sonnet 사용: UI 구현, 테스트, 버그 수정
- Opus 사용: 아키텍처 설계, 복잡한 로직

### 빌드 설정

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.nihongo.conversation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

## 📊 개발 로드맵

### ✅ Phase 1: MVP (완료)
- [x] 프로젝트 설정 (Kotlin + Compose + Hilt)
- [x] Gemini API 연동 (2.5 Flash)
- [x] 기본 채팅 UI (Material 3)
- [x] Room 데이터베이스 (대화/메시지 저장)
- [x] Navigation 구조

### ✅ Phase 2: 핵심 기능 (완료)
- [x] STT/TTS 통합 (일본어 음성 인식/재생)
- [x] VoiceManager (음성 상태 관리)
- [x] AI 힌트 시스템 (문맥 기반 제안)
- [x] 한국어-일본어 번역 다이얼로그
- [x] 6가지 실생활 시나리오
- [x] 시나리오 선택 화면
- [x] 설정 시스템 (DataStore)
- [x] 난이도 조절 (1-3 레벨)
- [x] 음성 속도 제어 (0.5x-2.0x)
- [x] ChatScreen UX 폴리싱 (애니메이션, 타이핑 인디케이터)

### ✅ Phase 3: 고급 기능 (완료)
- [x] 복습 모드 (저장된 대화 재생, 날짜 그룹화, 중요 문구)
- [x] 학습 통계 (연속 학습일, 차트, 주간/월간 뷰)
- [x] 사용자 프로필 시스템 (아바타, 학습 목표, 개인화)
- [x] 난이도별 AI 응답 조정 (JLPT N5-N1, 어휘 복잡도 분석)
- [x] 문법 설명 기능 (색상 구문 강조, 한국어 설명, 대화 예시)
- [x] 문법 캐싱 (즉시 재로딩)
- [x] 메시지별 한국어 번역 버튼
- [x] 대화 종료 및 히스토리 관리
- [x] TTS 재시도 메커니즘
- [x] 대화 이력 보존 (네비게이션 시)
- [x] 스마트 대화 생성 (빈 대화 방지)

### 📅 Phase 4: 추가 기능 (계획)
- [ ] 발음 평가 (STT 정확도 분석)
- [ ] 플래시카드 생성 (중요 문구에서)
- [ ] 퀴즈 모드
- [ ] 목표 설정 및 알림
- [ ] 소셜 공유 기능

## 🧪 테스트

```bash
# 단위 테스트
./gradlew test

# UI 테스트
./gradlew connectedAndroidTest

# 특정 테스트 실행
./gradlew test --tests "*.ChatViewModelTest"
```

## 📂 주요 파일 구조

### Domain Layer (`domain/model/`)
- **User.kt**: 사용자 엔티티 (Room @Entity, avatarId, learningGoal, level)
- **Scenario.kt**: 시나리오 템플릿 (제목, 설명, 난이도, 시스템 프롬프트)
- **Conversation.kt**: 대화 세션 (userId, scenarioId, isCompleted)
- **Message.kt**: 개별 메시지 (content, isUser, timestamp, complexityScore)
- **Hint.kt**: AI 힌트 (japanese, korean, romaji, explanation)
- **GrammarExplanation.kt**: 문법 분석 (components, overallExplanation, examples, relatedPatterns)
- **GrammarComponent.kt**: 문법 요소 (text, type, explanation, startIndex, endIndex)
- **GrammarType.kt**: 문법 타입 enum (PARTICLE, VERB, ADJECTIVE, etc.)
- **UserSettings.kt**: 사용자 설정 (difficulty, speechSpeed, autoSpeak, showRomaji)

### Data Layer
#### Local (`data/local/`)
- **NihongoDatabase.kt**: Room 데이터베이스 (4개 DAO, Schema v2)
  - `MIGRATION_1_2`: isCompleted 컬럼 추가 마이그레이션
- **UserDao.kt, ScenarioDao.kt, ConversationDao.kt, MessageDao.kt**: 데이터 접근 인터페이스
  - `getLatestActiveConversationByUserAndScenario()`: 활성 대화 조회
  - `getCompletedConversationsByUserAndScenario()`: 완료된 대화 조회
- **SettingsDataStore.kt**: DataStore Preferences 관리
- **DataInitializer.kt**: 6가지 기본 시나리오 초기화

#### Remote (`data/remote/`)
- **GeminiApiService.kt**: Gemini 2.5 Flash API 클라이언트
  - `sendMessage()`: AI 대화 생성
  - `generateHints()`: 문맥 기반 힌트 생성 (JSON 파싱)

#### Repository (`data/repository/`)
- **ConversationRepository.kt**: 통합 데이터 관리
  - Room DB + Gemini API 통합
  - Flow 기반 리액티브 데이터
  - `getOrCreateConversation()`: 대화 세션 복원 또는 생성
  - `completeConversation()`: 대화 종료 및 히스토리 저장
  - `translateToKorean()`: 일본어 → 한국어 번역
- **StatsRepository.kt**: 학습 통계 계산
  - 일일/주간/월간 통계
  - 연속 학습일 추적
  - 시나리오별 진행률
  - 학습 시간 추정

### Presentation Layer
#### Chat (`presentation/chat/`)
- **ChatScreen.kt**: 메인 채팅 UI (360+ lines)
  - ChatScreen, MessageBubble, MessageInput composables
  - AnimatedVisibility, 타임스탬프, 에러 표시
  - 대화 종료 확인 다이얼로그
  - 메시지별 번역 버튼
- **ChatViewModel.kt**: 채팅 상태 관리
  - 메시지 전송/수신, 음성 이벤트, 힌트 요청
  - Settings 관찰 및 VoiceManager 연동
  - `confirmEndChat()`: 대화 종료 및 상태 초기화
  - `toggleMessageTranslation()`: 메시지별 번역 토글
  - `requestGrammarExplanation()`: 문법 설명 (캐싱 지원)
- **TypingIndicator.kt**: 3-dot 펄스 애니메이션
- **VoiceButton.kt**: 마이크 버튼 + 펄스 효과
- **HintDialog.kt**: 힌트 카드 리스트 다이얼로그
- **VoiceStateIndicator.kt**: 음성 상태 표시

#### Scenario (`presentation/scenario/`)
- **ScenarioListScreen.kt**: 시나리오 선택 화면
  - ScenarioCard, DifficultyBadge, 아이콘 매핑
- **ScenarioViewModel.kt**: 시나리오 리스트 관리

#### Settings (`presentation/settings/`)
- **SettingsScreen.kt**: 설정 UI
  - DifficultySlider, SpeechSpeedSlider, SettingsToggle
  - 섹션별 레이아웃 (Material 3)
- **SettingsViewModel.kt**: 설정 상태 관리 (DataStore 연동)

#### Review (`presentation/review/`)
- **ReviewScreen.kt**: 복습 모드 UI (480+ lines)
  - 날짜별 대화 그룹화
  - 확장 가능 대화 카드
  - 중요 문구 추출 및 재생
- **ReviewViewModel.kt**: 복습 상태 관리
  - 대화 로딩 및 그룹화
  - 중요 문구 추출 로직
  - TTS 재생 제어

#### Stats (`presentation/stats/`)
- **StatsScreen.kt**: 통계 대시보드 UI (450+ lines)
  - 연속 학습일 카드
  - 총계 통계 (회화/메시지/시간)
  - 주간/월간 뷰 토글
- **StatsViewModel.kt**: 통계 상태 관리
  - StatsRepository 연동
  - 기간별 데이터 필터링
- **Charts.kt**: 차트 컴포넌트 (320+ lines)
  - BarChart (막대 차트)
  - LineChart (선 차트)
  - PieChart (파이 차트)
  - ChartLegend, StatCard

#### Navigation (`presentation/navigation/`)
- **NihongoNavHost.kt**: Navigation Compose 라우팅
  - ScenarioList (시작) → Chat / Settings / Stats / Review
- **Screen.kt**: 라우트 정의

### Core Layer (`core/`)
#### DI (`core/di/`)
- **DatabaseModule.kt**: Room DB Hilt 제공
- **AppModule.kt**: Context, Gemini API Hilt 제공
- **VoiceModule.kt**: VoiceManager Singleton 제공

#### Voice (`core/voice/`)
- **VoiceManager.kt**: STT/TTS 통합 관리
  - Android SpeechRecognizer (일본어 ja-JP)
  - TextToSpeech (속도 제어 0.5x-2.0x, 재시도 메커니즘)
  - Pending queue 시스템 (비동기 초기화 문제 해결)
  - StateFlow 기반 상태 관리
  - `retryTtsInitialization()`: TTS 재초기화 함수
- **VoiceState.kt**: Idle, Listening, Speaking 상태
- **VoiceEvent.kt**: RecognitionResult, Error, SpeakingComplete 이벤트

#### Util (`core/util/`)
- **Result.kt**: Success/Error/Loading sealed class

### Application (`NihongoApp.kt`)
- Hilt Application 진입점
- DataInitializer로 기본 시나리오 삽입

**총 파일 수**: 40+ Kotlin 파일 (Review 모드 +2, Stats 대시보드 +4, 기존 30+)

## 🔑 핵심 구현 포인트

### 1. Gemini API 통합 + 텍스트 정제
```kotlin
// GeminiApiService.kt
val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash",
    apiKey = BuildConfig.GEMINI_API_KEY
)

// 대화 히스토리를 포함한 컨텍스트 전달
val chat = generativeModel.startChat(history = conversationHistory)
val response = chat.sendMessage(userMessage)

// 🆕 응답 텍스트 자동 정제
private fun cleanResponseText(text: String): String {
    return text
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")  // **굵게** 제거
        .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")  // *기울임* 제거
        .replace(Regex("（[^）]*）"), "")  // （후리가나） 제거
        .replace(Regex("\\([^)]*\\)"), "")  // (furigana) 제거
}
```

### 2. 리액티브 Settings 동기화
```kotlin
// ChatViewModel에서 Settings 관찰
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

### 3. 음성 인식/재생 상태 관리 + TTS 개선
```kotlin
// VoiceManager.kt - StateFlow 기반 상태 관리
private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
val state: StateFlow<VoiceState> = _state.asStateFlow()

// 🆕 Pending Queue 시스템 (비동기 초기화 문제 해결)
private val pendingSpeechQueue = mutableListOf<PendingSpeech>()

fun speak(text: String, speed: Float = 1.0f) {
    // 후리가나 자동 제거
    val cleanText = text.replace(Regex("（[^）]*）|\\([^)]*\\)"), "").trim()

    if (!isTtsInitialized) {
        // TTS 준비 전 - 큐에 저장
        synchronized(pendingSpeechQueue) {
            pendingSpeechQueue.add(PendingSpeech(cleanText, utteranceId, speed))
        }
        return
    }

    // TTS 준비 완료 - 즉시 재생
    textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
}

// 초기화 완료 시 큐 처리
private fun initializeTts() {
    textToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            // 일본어 설정 및 준비 완료
            isTtsInitialized = true

            // 🆕 대기 중인 음성 재생
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.forEach { pending ->
                    textToSpeech?.speak(pending.text, ...)
                }
                pendingSpeechQueue.clear()
            }
        }
    }
}

// UI에서 상태 구독
val voiceState by viewModel.voiceState.collectAsState()
```

### 4. AI 힌트 생성 (JSON 파싱)
```kotlin
// GeminiApiService.kt
suspend fun generateHints(conversationHistory: List<Message>): List<Hint> {
    val prompt = """
    Based on this conversation, suggest 3 helpful Japanese phrases...
    Return ONLY a JSON array with this exact format:
    [{"japanese": "...", "korean": "...", "romaji": "...", "explanation": "..."}]
    """

    val response = generativeModel.generateContent(prompt)
    // JSON 파싱 with fallback hints
}
```

### 5. Compose 애니메이션 최적화
```kotlin
// TypingIndicator.kt - remember로 애니메이션 인스턴스 재사용
val infiniteTransition = rememberInfiniteTransition(label = "typing")

// ChatScreen.kt - 키 기반 아이템 추적으로 재조합 최소화
items(items = uiState.messages, key = { it.id }) { message ->
    AnimatedVisibility(...)
}
```

### 6. DataStore Preferences 패턴
```kotlin
// SettingsDataStore.kt
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

### 7. Room 관계형 쿼리
```kotlin
// ConversationDao.kt
@Query("""
    SELECT * FROM conversations
    WHERE userId = :userId
    ORDER BY createdAt DESC
""")
fun getConversationsByUser(userId: Long): Flow<List<Conversation>>

// MessageDao.kt - Foreign Key 관계
@Entity(
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
```

## 🤝 기여하기

개인 프로젝트이지만 피드백과 제안은 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

MIT License - 자유롭게 사용하세요

## 📮 연락처

- Email: your.email@example.com
- Project Link: [https://github.com/yourusername/nihongo-conversation](https://github.com/yourusername/nihongo-conversation)

## 🙏 감사의 말

- Google Gemini Team - 강력한 AI API 제공
- Android Jetpack Team - 현대적인 Android 개발 도구
- 일본어 학습 커뮤니티 - 피드백과 아이디어

---

**Note**: 이 앱은 개인 학습용으로 개발되었습니다. 상업적 사용 시 Gemini API 라이선스를 확인하세요.