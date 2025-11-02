# 🗾 일본어 회화 학습 앱 (NihonGo Conversation)

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_24+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-2.5_Flash-purple.svg)](https://ai.google.dev)

AI 기반 일본어 회화 학습을 위한 개인용 Android 애플리케이션

---

## 🌟 주요 기능

### 🤖 AI 대화 시스템
- **Gemini 2.5 Flash** 기반 자연스러운 일본어 대화
- 맥락을 기억하는 AI 파트너
- 실시간 문법 오류 감지 및 피드백
- **126가지 시나리오**: 엔터테인먼트(27), 직장/업무(14), 일상생활(15), 여행(13), 기술/개발(9), 문화(9), 건강/의료(7), 금융/재테크(6), 학습/교육(5), JLPT 연습(5), e스포츠(5), 비즈니스(4), 부동산/주거(3), 연애/관계(2), 긴급상황(1), 일상회화(1)
- **9개 카테고리 탭**: 전체, 엔터, 직장, 일상, 여행, 기술, 게임, JLPT, 기타
- **커스텀 시나리오**: 개인 맞춤 시나리오 추가/삭제 기능

### 🗣️ 음성 및 발음 기능
- **음성 전용 모드**: 텍스트 없이 음성만으로 대화
- **고급 발음 분석**: 피치 액센트, 억양, 리듬, 문제 음소 감지
- **발음 평가 시스템**: 6차원 점수 및 등급 (초심자 → 네이티브)
- 음성 인식(STT) 및 음성 합성(TTS)

### 🌐 3-Provider 번역 시스템
- **Microsoft Translator** (주 번역, 2M chars/월)
- **DeepL API** (폴백, 500K chars/월)
- **ML Kit** (오프라인 폴백)
- 자동 폴백 체인 및 캐싱

### 📚 학습 관리
- **문장 카드 시스템**: 4가지 연습 모드 (읽기/듣기/빈칸/말하기)
- **SM-2 간격 반복 알고리즘**: 최적 복습 스케줄링
- **문법 패턴 학습**: 10+ 문법 패턴 자동 추출
- **학습 통계**: 강점/약점 분석, 7일 추세, 마스터 추적

### ✨ 추가 기능
- 메시지 컨텍스트 메뉴 (복사/읽기/문법 분석/번역)
- 다중 사용자 프로필 지원
- 난이도 조절 (초급/중급/고급)
- 대화 히스토리 및 검색

📖 **[전체 기능 상세 보기 →](docs/FEATURES.md)**

---

## 🚀 빠른 시작

### 필요 사항
- Android Studio Hedgehog (2023.1.1) 이상
- Android SDK 24+
- Kotlin 1.9.0+
- Gemini API 키 (필수)
- Microsoft Translator/DeepL API 키 (선택)

### 설치 방법

1. **저장소 클론**
```bash
git clone https://github.com/yourusername/nihongo-conversation.git
cd nihongo-conversation
```

2. **API 키 설정**

`local.properties` 파일 생성:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
MICROSOFT_TRANSLATOR_KEY=your_microsoft_key_here  # 선택
DEEPL_API_KEY=your_deepl_key_here                 # 선택
```

3. **빌드 및 실행**
```bash
./gradlew assembleDebug
```

또는 Android Studio에서 Run ▶️

📖 **[자세한 설치 가이드 →](docs/DEVELOPMENT.md)**

---

## 🏗️ 아키텍처

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room + Paging 3
- **Network**: Retrofit + OkHttp
- **AI**: Gemini 2.5 Flash API (스트리밍)
- **Voice**: Android SpeechRecognizer + TTS
- **Translation**: Microsoft Translator + DeepL + ML Kit

```
app/
├── data/           # Repository, Database, API
├── domain/         # UseCase, Model
├── presentation/   # UI (Compose), ViewModel
└── core/           # Utilities, DI modules
```

📖 **[아키텍처 상세 보기 →](docs/ARCHITECTURE.md)**

---

## 🌐 API 통합

### Gemini 2.5 Flash API
- AI 대화 생성
- 문법 분석 및 힌트
- 스트리밍 응답 (TTFB 최적화)

### 3-Provider 번역 시스템
1. **Microsoft Translator** (주 번역)
   - 2M chars/월 무료
   - Korea Central 리전
   - 200-400ms 응답 시간

2. **DeepL API** (폴백)
   - 500K chars/월 무료
   - 높은 번역 품질
   - 300-500ms 응답 시간

3. **ML Kit** (오프라인 폴백)
   - 무제한 사용
   - 온디바이스 번역
   - 100-200ms 응답 시간

**자동 폴백 체인**: Microsoft 실패 → DeepL 시도 → ML Kit 대체

📖 **[API 통합 가이드 →](docs/API.md)**

---

## 📱 주요 화면

- **대화 화면**: AI와 실시간 대화, 문법 피드백, 번역
- **시나리오 선택**: 6가지 일반 + 6가지 목표 기반 시나리오
- **문장 카드 복습**: 4가지 연습 모드
- **발음 히스토리**: 발음 진행도 추적 및 약점 분석
- **통계 화면**: 학습 분석 대시보드
- **설정 화면**: 사용자 프로필, 난이도, TTS 속도

---

## 📝 문서

| 문서 | 내용 |
|------|------|
| [FEATURES.md](docs/FEATURES.md) | 전체 기능 상세 설명 |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 프로젝트 구조 및 기술 스택 |
| [API.md](docs/API.md) | Gemini/Microsoft/DeepL API 통합 가이드 |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | 개발 환경 설정 및 빌드 가이드 |
| [CHANGELOG.md](docs/CHANGELOG.md) | 버전별 업데이트 히스토리 |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | 알려진 이슈 및 해결법 |

---

## 🛠️ 개발 명령어

```bash
# 빌드
./gradlew assembleDebug

# 테스트
./gradlew test

# 클린 빌드
./gradlew clean assembleDebug

# 설치 및 실행
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nihongo.conversation/.MainActivity

# 로그 확인
adb logcat -s ChatViewModel:D GrammarAPI:D VoiceManager:D
```

---

## 🐛 문제 해결

자주 발생하는 문제:

1. **TTS가 작동하지 않음**
   - 디바이스 설정 → 언어 및 입력 → 음성 출력 → 일본어 데이터 설치 확인
   - [TTS 문제 해결 가이드 →](docs/TROUBLESHOOTING.md#1-tts-문제)

2. **Room Migration 에러**
   - 앱 삭제 후 재설치: `adb uninstall com.nihongo.conversation`
   - [마이그레이션 가이드 →](docs/TROUBLESHOOTING.md#4-room-migration-에러)

3. **OutOfMemoryError 빌드 에러**
   - `gradle.properties`에서 `org.gradle.jvmargs=-Xmx4096m` 설정
   - [빌드 문제 해결 →](docs/TROUBLESHOOTING.md#5-빌드-문제)

📖 **[전체 문제 해결 가이드 →](docs/TROUBLESHOOTING.md)**

---

## 📊 성능 지표

| 지표 | 값 |
|------|------|
| 첫 번째 AI 응답 (TTFB) | ~800ms |
| 번역 응답 (캐시) | <10ms (95% 히트율) |
| 번역 응답 (Microsoft) | 200-400ms |
| UI 렌더링 (60fps) | 16ms/frame |
| 데이터베이스 쿼리 | <50ms (인덱스 최적화) |
| 메모리 사용량 | ~120MB |

---

## 🎯 로드맵

- [ ] 오프라인 AI 모드 (Gemini Nano)
- [ ] 커스텀 시나리오 생성
- [ ] 음성 채팅방 (다중 사용자)
- [ ] 웹 버전 (Kotlin Multiplatform)
- [ ] iOS 앱

---

## 📄 라이선스

이 프로젝트는 개인 학습용 프로젝트입니다.

---

## 🙋 지원 및 문의

- **이슈 리포트**: [GitHub Issues](https://github.com/yourusername/nihongo-conversation/issues)
- **개발자**: seungjooahn
- **이메일**: your.email@example.com

---

## 🙏 감사의 글

- [Gemini API](https://ai.google.dev) - AI 대화 생성
- [Microsoft Translator](https://azure.microsoft.com/en-us/products/ai-services/ai-translator) - 번역 서비스
- [DeepL](https://www.deepl.com) - 고품질 번역
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 모던 Android UI

---

**⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**
