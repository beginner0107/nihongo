# 🗾 일본어 회화 학습 앱 (NihonGo Conversation)

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_24+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-2.5_Flash-purple.svg)](https://ai.google.dev)

AI 기반 일본어 회화 학습을 위한 개인용 Android 애플리케이션

## 🌟 주요 기능

- 🤖 **AI 대화 파트너**: Gemini 2.5 Flash를 활용한 자연스러운 일본어 대화
- 💭 **맥락 기억**: 이전 대화를 기억하고 관계를 이어가는 친구 같은 AI
- 💡 **AI 힌트 시스템**: 한국어-일본어 번역 힌트, 로마자 표기, 문맥 기반 제안
- 🎭 **6가지 시나리오**: 레스토랑, 쇼핑, 호텔, 친구 만들기, 전화 대화, 병원 (초급/중급/상급)
- 🎙️ **음성 지원**: STT로 일본어 음성 인식, TTS로 AI 응답 자동 재생
- ⚙️ **설정 시스템**: 난이도 조절 (1-3), 음성 속도 (0.5x-2.0x), 자동 읽기, 로마자 표시
- 📊 **학습 통계**: 일일/주간/월간 진도, 연속 학습일 추적, 시나리오별 진행률, 차트 시각화
- 🔥 **복습 모드**: 과거 대화 재생, 날짜별 그룹화, 중요 문구 추출, 메시지 재생
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
- ⌨️ **스마트 입력**: 엔터키로 전송, 타이핑 인디케이터
- ✨ **부드러운 애니메이션**: Slide-in/fade-in 메시지, 에러 표시

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

### 🚧 Phase 3: 고급 기능 (진행중)
- [x] 복습 모드 (저장된 대화 재생, 날짜 그룹화, 중요 문구)
- [x] 학습 통계 (연속 학습일, 차트, 주간/월간 뷰)
- [ ] 사용자 프로필 시스템
- [ ] 난이도별 AI 응답 조정
- [ ] 문법 설명 기능
- [ ] 발음 평가 (STT 정확도 분석)

### 📅 Phase 4: 추가 기능 (계획)
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
- **User.kt**: 사용자 엔티티 (Room @Entity)
- **Scenario.kt**: 시나리오 템플릿 (제목, 설명, 난이도, 시스템 프롬프트)
- **Conversation.kt**: 대화 세션 (userId, scenarioId)
- **Message.kt**: 개별 메시지 (content, isUser, timestamp)
- **Hint.kt**: AI 힌트 (japanese, korean, romaji, explanation)
- **UserSettings.kt**: 사용자 설정 (difficulty, speechSpeed, autoSpeak, showRomaji)

### Data Layer
#### Local (`data/local/`)
- **NihongoDatabase.kt**: Room 데이터베이스 (4개 DAO)
- **UserDao.kt, ScenarioDao.kt, ConversationDao.kt, MessageDao.kt**: 데이터 접근 인터페이스
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
- **ChatViewModel.kt**: 채팅 상태 관리
  - 메시지 전송/수신, 음성 이벤트, 힌트 요청
  - Settings 관찰 및 VoiceManager 연동
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
  - TextToSpeech (속도 제어 0.5x-2.0x)
  - StateFlow 기반 상태 관리
- **VoiceState.kt**: Idle, Listening, Speaking 상태
- **VoiceEvent.kt**: RecognitionResult, Error, SpeakingComplete 이벤트

#### Util (`core/util/`)
- **Result.kt**: Success/Error/Loading sealed class

### Application (`NihongoApp.kt`)
- Hilt Application 진입점
- DataInitializer로 기본 시나리오 삽입

**총 파일 수**: 40+ Kotlin 파일 (Review 모드 +2, Stats 대시보드 +4, 기존 30+)

## 🔑 핵심 구현 포인트

### 1. Gemini API 통합
```kotlin
// GeminiApiService.kt
val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash-latest",
    apiKey = BuildConfig.GEMINI_API_KEY
)

// 대화 히스토리를 포함한 컨텍스트 전달
val chat = generativeModel.startChat(history = conversationHistory)
val response = chat.sendMessage(userMessage)
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

### 3. 음성 인식/재생 상태 관리
```kotlin
// VoiceManager.kt - StateFlow 기반 상태 관리
private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
val state: StateFlow<VoiceState> = _state.asStateFlow()

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