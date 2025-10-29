# 🗾 일본어 회화 학습 앱 (NihonGo Conversation)

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_24+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-2.5_Flash-purple.svg)](https://ai.google.dev)

AI 기반 일본어 회화 학습을 위한 개인용 Android 애플리케이션

## 🌟 주요 기능

- 🤖 **AI 대화 파트너**: Gemini 2.5 Flash를 활용한 자연스러운 일본어 대화
- 💭 **맥락 기억**: 이전 대화를 기억하고 관계를 이어가는 친구 같은 AI
- 💡 **학습 지원**: 힌트 시스템, 문장 완성 도우미
- 🎭 **시나리오 모드**: 실생활 상황별 대화 연습
- 🎙️ **음성 지원**: STT/TTS로 실제 대화처럼 연습
- 📊 **학습 통계**: 진도 추적 및 복습 시스템

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

- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Network**: Retrofit + OkHttp
- **Async**: Coroutines + Flow
- **AI**: Gemini 2.5 Flash API
- **Voice**: Google Speech-to-Text/Text-to-Speech

## 📱 주요 화면

### 대화 화면
- 채팅 인터페이스
- 음성 입력/출력
- 힌트 시스템
- 난이도 조절

### 시나리오 선택
- 상황별 대화 템플릿
- 난이도 표시
- 학습 목표 설정

### 학습 통계
- 일일/주간/월간 통계
- 학습 스트릭
- 실력 향상 그래프

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
- [x] 프로젝트 설정
- [x] Gemini API 연동
- [x] 기본 채팅 UI
- [x] 대화 저장

### 🚧 Phase 2: 핵심 기능 (진행중)
- [ ] STT/TTS 통합
- [ ] 힌트 시스템
- [ ] 난이도 조절
- [ ] 시나리오 추가

### 📅 Phase 3: 고급 기능
- [ ] 복습 모드
- [ ] 학습 통계
- [ ] AI 성격 커스터마이징

## 🧪 테스트

```bash
# 단위 테스트
./gradlew test

# UI 테스트
./gradlew connectedAndroidTest

# 특정 테스트 실행
./gradlew test --tests "*.ChatViewModelTest"
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