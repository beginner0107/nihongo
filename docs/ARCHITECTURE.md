# 🏗️ 아키텍처

```
app/
├── src/main/java/com/nihongo/conversation/
│   ├── data/                 # 데이터 레이어
│   │   ├── local/            # Room DB
│   │   ├── remote/           # API 클라이언트
│   │   └── repository/       # Repository 구현
│   ├── domain/               # 도메인 레이어
│   │   ├── model/            # 데이터 모델
│   │   │   ├── SentenceCard.kt           # 문장 카드 (NEW!)
│   │   │   ├── EnhancedPronunciation.kt  # 향상된 발음 (NEW!)
│   │   │   ├── GrammarFeedback.kt        # 문법 피드백 (NEW!)
│   │   │   ├── ScenarioGoal.kt           # 시나리오 목표 (NEW!)
│   │   │   └── VoiceOnlyMode.kt          # 음성 전용 (NEW!)
│   │   ├── analyzer/         # 음성/발음 분석기 (NEW!)
│   │   │   ├── PitchAccentAnalyzer.kt
│   │   │   ├── SpeedRhythmAnalyzer.kt
│   │   │   └── ProblematicSoundsDetector.kt
│   │   ├── usecase/          # 비즈니스 로직
│   │   └── repository/       # Repository 인터페이스
│   ├── presentation/         # 프레젠테이션 레이어
│   │   ├── chat/             # 대화 화면
│   │   │   ├── VoiceOnlyComponents.kt    # 음성 전용 UI (NEW!)
│   │   │   └── FeedbackCard.kt           # 피드백 카드 (NEW!)
│   │   ├── pronunciation/    # 발음 분석 UI (NEW!)
│   │   │   ├── PitchAccentVisualization.kt
│   │   │   └── IntonationVisualizer.kt
│   │   ├── study/            # 학습 UI (NEW!)
│   │   │   └── SentenceCardPracticeSheet.kt
│   │   ├── flashcard/        # 플래시카드 복습 및 통계
│   │   ├── vocabulary/       # 커스텀 단어 추가
│   │   ├── user/             # 유저 선택/관리
│   │   ├── scenario/         # 시나리오 목록
│   │   ├── stats/            # 통계 화면
│   │   ├── review/           # 복습 화면
│   │   └── theme/            # 테마 설정
│   └── core/                 # 공통 유틸리티
│       ├── di/               # Dependency Injection
│       ├── session/          # 세션 관리 (UserSessionManager)
│       ├── network/          # 네트워크 모니터링/오프라인
│       ├── voice/            # STT/TTS
│       └── util/             # 헬퍼 함수
└── build.gradle.kts
```

## 🛠️ 기술 스택

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room (SQLite) + Paging 3
  - 11개 최적화 인덱스 (복합 인덱스 포함)
  - 데이터베이스 뷰 (conversation_stats)
  - 스트리밍 쿼리 최적화
- **Persistence**: DataStore Preferences (Settings, User Session, Offline Cache)
- **Network**: Retrofit + OkHttp
- **Async**: Coroutines + Flow
- **AI**: Gemini 2.5 Flash API (스트리밍 지원)
- **Voice**: Android SpeechRecognizer (STT) + TextToSpeech (TTS)
- **Performance**:
  - Response caching (common phrases)
  - Lazy loading (Paging 3)
  - Database indexing (5-10x faster queries)
