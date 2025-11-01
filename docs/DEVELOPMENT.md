# 🚀 개발 가이드

## 필요 사항

- **Android Studio**: Hedgehog (2023.1.1) 이상
- **Android SDK**: API 24 (Android 7.0) 이상
- **Kotlin**: 1.9.0 이상
- **JDK**: 17 이상
- **API 키**:
  - Gemini API ([발급하기](https://makersuite.google.com/app/apikey))
  - Microsoft Translator (선택사항)
  - DeepL API (선택사항)

---

## 설치 방법

### 1. 프로젝트 클론

```bash
git clone https://github.com/yourusername/nihongo-conversation.git
cd nihongo-conversation
```

### 2. API 키 설정

프로젝트 루트에 `local.properties` 파일을 생성하고 API 키를 추가합니다:

```properties
# 필수: Gemini API (AI 대화)
GEMINI_API_KEY=your_gemini_api_key_here

# 선택: Microsoft Translator (번역 - 우선순위 1)
MICROSOFT_TRANSLATOR_KEY=your_microsoft_key_here
MICROSOFT_TRANSLATOR_REGION=koreacentral

# 선택: DeepL API (번역 - 우선순위 2)
DEEPL_API_KEY=your_deepl_key_here
```

**참고**:
- Gemini API 키만 있어도 기본 기능 사용 가능
- 번역 API 키가 없으면 ML Kit 오프라인 번역으로 자동 폴백

### 3. API 키 발급 방법

#### Gemini API (필수)
1. [Google AI Studio](https://makersuite.google.com/app/apikey) 접속
2. Google 계정으로 로그인
3. "Create API Key" 클릭
4. 생성된 키를 복사하여 `local.properties`에 붙여넣기

#### Microsoft Translator (선택)
1. [Azure Portal](https://portal.azure.com) 접속
2. "Create a resource" → "Translator" 검색
3. 무료 플랜 선택 (F0 - 2M chars/month)
4. Region: "Korea Central" 선택
5. Key와 Region을 `local.properties`에 추가

#### DeepL API (선택)
1. [DeepL API](https://www.deepl.com/pro-api) 접속
2. 무료 플랜 가입 (500K chars/month)
3. API 키 발급
4. `local.properties`에 추가

### 4. 빌드 및 실행

#### Android Studio에서 실행

1. Android Studio에서 프로젝트 열기
2. Gradle Sync 완료 대기
3. Run ▶️ 버튼 클릭 (또는 Shift + F10)

#### 명령줄에서 빌드

```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리스 APK 빌드
./gradlew assembleRelease

# APK 설치 및 실행
./gradlew installDebug
```

### 5. 테스트 실행

```bash
# 단위 테스트
./gradlew test

# 계측 테스트 (에뮬레이터/기기 필요)
./gradlew connectedAndroidTest

# 모든 테스트 실행
./gradlew check
```

---

## 빌드 구성

### Gradle 설정

**필수 메모리 설정** (OutOfMemoryError 방지):

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 의존성

주요 라이브러리 버전 (`app/build.gradle.kts`):

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Material Icons
    implementation("androidx.compose.material:material-icons-core:1.7.4")
    implementation("androidx.compose.material:material-icons-extended:1.7.4")

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.1")
}
```

---

## 프로젝트 구조

```
nihongo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nihongo/conversation/
│   │   │   │   ├── data/           # 데이터 레이어
│   │   │   │   ├── domain/         # 비즈니스 로직
│   │   │   │   ├── presentation/   # UI
│   │   │   │   └── core/           # 공통 유틸
│   │   │   └── res/
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties       # Git 제외, API 키 저장
└── README.md
```

---

## 개발 워크플로우

### 1. 새 기능 개발

```bash
# 브랜치 생성
git checkout -b feature/new-feature

# 변경사항 커밋
git add .
git commit -m "Add new feature"

# 푸시
git push origin feature/new-feature

# Pull Request 생성
```

### 2. 코드 스타일

프로젝트는 Kotlin 공식 스타일 가이드를 따릅니다:

```kotlin
// 클래스명: PascalCase
class ChatViewModel

// 함수명: camelCase
fun sendMessage()

// 상수: UPPER_SNAKE_CASE
const val MAX_MESSAGES = 100

// 프로퍼티: camelCase
val isLoading = false
```

### 3. 디버깅

#### Logcat 필터링

```bash
# ChatViewModel 로그만 보기
adb logcat -s ChatViewModel:D

# 에러만 보기
adb logcat *:E

# 여러 태그 동시 보기
adb logcat -s ChatViewModel:D GrammarDebug:D VoiceManager:*
```

#### 유용한 디버그 태그

- `ChatViewModel`: 대화 로직
- `GrammarAPI`: 문법 분석
- `VoiceManager`: STT/TTS
- `TranslationRepository`: 번역
- `NetworkMonitor`: 네트워크 상태

### 4. 데이터베이스 검사

```bash
# 데이터베이스 파일 추출
adb pull /data/data/com.nihongo.conversation/databases/nihongo_database.db

# SQLite 브라우저로 열기
# https://sqlitebrowser.org/
```

---

## 자주 사용하는 명령어

### 앱 재설치 (데이터 초기화)

```bash
# 완전 제거
adb uninstall com.nihongo.conversation

# 재설치
./gradlew installDebug
```

### 캐시 클리어

```bash
# Gradle 캐시 클리어
./gradlew clean

# Build 캐시 클리어
./gradlew cleanBuildCache

# 모든 캐시 클리어
rm -rf ~/.gradle/caches
```

### 의존성 업데이트 확인

```bash
# 오래된 의존성 확인
./gradlew dependencyUpdates
```

---

## 빌드 최적화

### 로컬 빌드 속도 향상

```properties
# gradle.properties
kotlin.incremental=true
kotlin.caching.enabled=true
kapt.incremental.apt=true
kapt.use.worker.api=true
```

### ProGuard 설정

릴리스 빌드 시 코드 난독화:

```proguard
# proguard-rules.pro

# Gemini SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
```

---

## 환경별 설정

### 디버그 vs 릴리스

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## CI/CD 설정 (선택사항)

### GitHub Actions 예시

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew assembleDebug

    - name: Run tests
      run: ./gradlew test
```

---

## 문제 해결

### 빌드 에러

#### OutOfMemoryError

```bash
# gradle.properties 확인
org.gradle.jvmargs=-Xmx4096m
```

#### Kotlin 컴파일 에러

```bash
# 캐시 클리어 후 재빌드
./gradlew clean
./gradlew build
```

#### AAPT 에러

```bash
# Android SDK 업데이트
sdkmanager "build-tools;34.0.0"
```

### 런타임 에러

#### TTS 초기화 실패

- 설정 → 언어 및 입력 → 음성 출력
- 일본어 TTS 데이터 다운로드 확인

#### 네트워크 에러

- `AndroidManifest.xml`에 INTERNET 권한 확인
- API 키가 `local.properties`에 정확히 입력되었는지 확인

#### 데이터베이스 마이그레이션 실패

```bash
# 앱 재설치로 DB 초기화
adb uninstall com.nihongo.conversation
./gradlew installDebug
```

---

## 기여하기

1. 이슈 생성 (버그 리포트 또는 기능 제안)
2. Fork 후 브랜치 생성
3. 변경사항 커밋
4. Pull Request 생성
5. 코드 리뷰 대기

**코드 리뷰 체크리스트**:
- [ ] Kotlin 스타일 가이드 준수
- [ ] 단위 테스트 추가
- [ ] 문서 업데이트
- [ ] 변경사항 설명 (CHANGELOG.md)
- [ ] 빌드 성공 확인

---

## 참고 자료

- [Android 개발자 가이드](https://developer.android.com/)
- [Jetpack Compose 문서](https://developer.android.com/jetpack/compose)
- [Kotlin 문서](https://kotlinlang.org/docs/home.html)
- [Room 데이터베이스](https://developer.android.com/training/data-storage/room)
- [Hilt 의존성 주입](https://developer.android.com/training/dependency-injection/hilt-android)
