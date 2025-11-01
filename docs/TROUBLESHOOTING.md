# 🐛 문제 해결 가이드

## TTS (Text-to-Speech) 문제

### 1. TTS가 작동하지 않을 때

**증상**: 음성이 재생되지 않거나 "未初期化" 에러 메시지 표시

**원인**:
- 일본어 음성 데이터 미설치
- TTS 초기화 실패
- 미디어 볼륨이 꺼져 있음

**해결 방법**:

#### 방법 1: 앱 완전 재설치 (권장)
```bash
# 데이터베이스 초기화를 위한 앱 제거
adb uninstall com.nihongo.conversation

# Android Studio에서 다시 실행
```

#### 방법 2: 일본어 음성 데이터 설치 확인
1. 디바이스 **설정** 앱 열기
2. **언어 및 입력** → **음성 출력**
3. **TTS 엔진 설정** 선택
4. **일본어 음성 데이터 다운로드** 확인

#### 방법 3: 볼륨 및 설정 확인
- 미디어 볼륨이 켜져 있는지 확인
- 무음 모드 해제
- 앱 내 설정 화면에서 "자동 읽기" 토글 확인
- 채팅 화면 오른쪽 상단 스피커 아이콘 확인

---

## AI 응답 문제

### 2. AI가 이상한 기호를 표시할 때

**증상**:
- `**텍스트**` (볼드 마크다운)
- `*텍스트*` (이탤릭 마크다운)
- `（ふりがな）` (후리가나 괄호)

**원인**: 데이터베이스에 저장된 이전 시스템 프롬프트 사용 중

**해결 방법**:
```bash
# 앱 재설치로 새 프롬프트 적용
adb uninstall com.nihongo.conversation

# Android Studio에서 다시 실행
```

**재설치 후 자동 적용**:
- ✅ 새로운 시스템 프롬프트 (마크다운 금지 규칙)
- ✅ AI 응답 텍스트 정제 기능 (`cleanResponseText()`)
- ✅ TTS 후리가나 제거 기능

---

## 시나리오 문제

### 3. 시나리오 내용 불일치

**"電話での会話" 시나리오 관련 오해**

**증상**: AI가 "레스토랑입니다"라고 응답하는데 이게 맞나요?

**답변**: 네, 정상입니다!

**설명**:
- "電話での会話" 시나리오는 **전화로 레스토랑/살롱 예약하기** 연습용입니다
- AI는 레스토랑/살롱 직원 역할을 맡습니다
- 사용자는 고객 역할로 전화로 예약하는 상황을 연습합니다

**시나리오 구성**:
1. レストランでの注文 - 레스토랑에서 직접 주문
2. 買い物 - 쇼핑
3. ホテルでのチェックイン - 호텔 체크인
4. 友達を作る - 친구 만들기
5. **電話での会話** - 전화로 예약하기 (레스토랑/살롱)
6. 病院で - 병원 방문

**시스템 프롬프트 예시**:
```kotlin
systemPrompt = "あなたはレストランやサロンの受付スタッフです。"
// "당신은 레스토랑이나 살롱의 접수 직원입니다"
```

---

## 데이터베이스 문제

### 4. Room Migration 스키마 불일치 크래시 ⚠️

**증상**: 앱 실행 시 즉시 크래시, logcat에 다음 에러:
```
FATAL EXCEPTION: main
java.lang.IllegalStateException: Migration didn't properly handle: [테이블명]
Expected: TableInfo{...}
Found: TableInfo{...}
```

**원인**: Room Entity 정의와 Migration SQL 스키마가 일치하지 않음

**흔한 실수들**:

#### 실수 1: DEFAULT 값 불일치
```kotlin
// ❌ 잘못된 예
@Entity(tableName = "example")
data class Example(
    val name: String = "default"  // Entity에는 default가 있는데
)

// Migration에서 DEFAULT 지정
database.execSQL("""
    CREATE TABLE example (
        name TEXT NOT NULL DEFAULT 'default'  // ← 이러면 스키마 불일치!
    )
""")

// ✅ 올바른 예
database.execSQL("""
    CREATE TABLE example (
        name TEXT NOT NULL  // DEFAULT 제거
    )
""")
```

#### 실수 2: 인덱스 누락
```kotlin
// ❌ 잘못된 예
@Entity(tableName = "example")  // indices 없음
data class Example(...)

// Migration에서 인덱스 생성
database.execSQL("CREATE INDEX idx_name ON example(name)")  // ← 불일치!

// ✅ 올바른 예
@Entity(
    tableName = "example",
    indices = [Index(value = ["name"])]  // Entity에 명시
)
data class Example(...)

// Migration
database.execSQL("CREATE INDEX IF NOT EXISTS index_example_name ON example(name)")
```

#### 실수 3: 컬럼 순서 차이
일반적으로는 괜찮지만, 주의가 필요합니다.

**실제 사례 - Translation Cache (2025-11-01)**:

**문제 상황**:
```kotlin
// Entity 정의
@Entity(tableName = "translation_cache")  // ← indices 없음!
data class TranslationCacheEntity(
    @PrimaryKey val sourceText: String,
    val sourceLang: String = "ja",  // ← default 있음
    val targetLang: String = "ko"
)

// Migration
database.execSQL("""
    CREATE TABLE translation_cache (
        sourceText TEXT NOT NULL PRIMARY KEY,
        sourceLang TEXT NOT NULL DEFAULT 'ja',  // ← DEFAULT 추가됨
        targetLang TEXT NOT NULL DEFAULT 'ko'
    )
""")
database.execSQL("CREATE INDEX ... ON translation_cache(provider)")  // ← Entity에 없음!
```

**해결 방법**:
```kotlin
// 1. Entity에 indices 추가
@Entity(
    tableName = "translation_cache",
    indices = [
        Index(value = ["provider"]),
        Index(value = ["timestamp"])
    ]
)
data class TranslationCacheEntity(
    @PrimaryKey val sourceText: String,
    val sourceLang: String = "ja",  // default는 괜찮음 (Kotlin 레벨)
    val targetLang: String = "ko"
)

// 2. Migration에서 DEFAULT 제거
database.execSQL("""
    CREATE TABLE translation_cache (
        sourceText TEXT NOT NULL PRIMARY KEY,
        sourceLang TEXT NOT NULL,  // DEFAULT 제거
        targetLang TEXT NOT NULL   // DEFAULT 제거
    )
""")
database.execSQL("CREATE INDEX IF NOT EXISTS index_translation_cache_provider ON translation_cache(provider)")
database.execSQL("CREATE INDEX IF NOT EXISTS index_translation_cache_timestamp ON translation_cache(timestamp)")
```

**디버깅 방법**:
```bash
# 1. 크래시 로그 확인
adb logcat -d | grep -A 20 "Migration didn't properly handle"

# 2. Expected vs Found 비교
# - Expected: Entity에서 정의한 스키마
# - Found: Migration으로 실제 생성된 스키마
# - 차이점을 찾아서 수정

# 3. 완전 재설치로 테스트
adb uninstall com.nihongo.conversation
./gradlew installDebug
```

**예방 방법**:
- ✅ Entity 수정 시 반드시 Migration도 함께 확인
- ✅ `@Index`, `foreignKeys` 등은 Entity에 명시
- ✅ Migration SQL에는 DEFAULT 사용 자제 (Kotlin default로 처리)
- ✅ Migration 작성 후 즉시 클린 재설치로 테스트
- ✅ Room Schema Export 활성화 (`exportSchema = true`)하여 자동 검증

**핵심 원칙**:
> **Entity 정의 = Migration SQL 결과**
>
> Room이 기대하는 스키마와 실제 DB 스키마가 1:1로 일치해야 함!

---

## 빌드 문제

### 5. OutOfMemoryError

**증상**: Gradle 빌드 중 메모리 부족 에러

**해결 방법**:

`gradle.properties` 파일에 다음 설정 추가:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 6. Kotlin 컴파일 에러

**증상**: "Could not resolve all files for configuration" 에러

**해결 방법**:
```bash
# 캐시 클리어 후 재빌드
./gradlew clean
./gradlew build --refresh-dependencies
```

### 7. AAPT (Android Asset Packaging Tool) 에러

**증상**: "AAPT: error: resource not found" 또는 리소스 처리 실패

**해결 방법**:
```bash
# Android SDK 빌드 도구 업데이트
sdkmanager "build-tools;34.0.0"

# 프로젝트 클린 및 재빌드
./gradlew clean build
```

---

## 네트워크 문제

### 8. API 호출 실패

**증상**: "Network error" 또는 "Failed to connect to api.google.dev"

**원인**:
- 인터넷 연결 문제
- API 키 오류
- 방화벽/프록시 차단

**해결 방법**:

#### 1. API 키 확인
```properties
# local.properties 확인
GEMINI_API_KEY=your_actual_key_here
```

#### 2. 인터넷 연결 확인
```bash
# PC에서 API 테스트
curl -H "x-goog-api-key: YOUR_KEY" \
  https://generativelanguage.googleapis.com/v1beta/models

# 200 OK 응답이 와야 정상
```

#### 3. AndroidManifest.xml 권한 확인
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

#### 4. 네트워크 보안 설정 (필요 시)
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

---

## 번역 문제

### 9. 번역이 작동하지 않음

**증상**: "번역 실패" 또는 빈 번역 결과

**원인**:
- 모든 번역 API 키가 설정되지 않음
- ML Kit 모델 미다운로드
- 네트워크 연결 끊김

**해결 방법**:

#### ML Kit 오프라인 번역으로 폴백
ML Kit은 API 키 없이 작동하며, 앱 최초 실행 시 자동으로 모델을 다운로드합니다 (~30MB).

**수동 다운로드 확인**:
```kotlin
// 앱 코드에서 (자동 실행됨)
val translator = Translation.getClient(options)
translator.downloadModelIfNeeded()
    .addOnSuccessListener { Log.d("MLKit", "Model downloaded") }
    .addOnFailureListener { e -> Log.e("MLKit", "Download failed", e) }
```

#### 번역 API 키 추가 (선택사항)
```properties
# local.properties
MICROSOFT_TRANSLATOR_KEY=your_key
DEEPL_API_KEY=your_key
```

---

## 권한 문제

### 10. 음성 인식 권한 거부

**증상**: "Permission denied" 또는 마이크 접근 불가

**해결 방법**:

1. **앱 설정에서 권한 확인**
   - 설정 → 앱 → NihonGo Conversation
   - 권한 → 마이크 → 허용

2. **런타임 권한 요청 (자동)**
   - 앱에서 음성 입력 버튼 클릭 시 자동 요청
   - "허용" 선택

3. **AndroidManifest.xml 확인**
   ```xml
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   ```

---

## 성능 문제

### 11. 앱이 느리거나 버벅임

**증상**: UI가 버벅이거나 응답이 느림

**해결 방법**:

#### 1. 캐시 클리어
```bash
# 앱 데이터 클리어
adb shell pm clear com.nihongo.conversation

# 또는 재설치
adb uninstall com.nihongo.conversation
./gradlew installDebug
```

#### 2. 데이터베이스 최적화 (자동)
앱은 자동으로 다음을 수행합니다:
- 인덱스 생성 (11개)
- 오래된 캐시 정리
- 쿼리 최적화

#### 3. 메모리 확인
```bash
# 앱 메모리 사용량 확인
adb shell dumpsys meminfo com.nihongo.conversation
```

### 12. 배터리 소모가 심함

**원인**:
- TTS가 계속 실행 중
- 네트워크 연결 풀링
- 백그라운드 작업

**해결 방법**:
- 대화 종료 후 앱 완전 종료
- 자동 음성 재생 끄기
- 백그라운드 제한 설정 (OS 레벨)

---

## 데이터 문제

### 13. 대화 기록이 사라짐

**원인**:
- 앱 재설치 (데이터 삭제됨)
- 스토리지 부족으로 자동 정리
- 데이터베이스 손상

**해결 방법**:

#### 데이터베이스 백업 (개발자용)
```bash
# 데이터베이스 추출
adb pull /data/data/com.nihongo.conversation/databases/nihongo_database.db ~/backup/

# 복원
adb push ~/backup/nihongo_database.db /data/data/com.nihongo.conversation/databases/
```

**참고**: 일반 사용자는 현재 백업 기능이 없습니다. (향후 업데이트 예정)

---

## 로그 수집

### 디버깅용 로그 추출

```bash
# 전체 로그
adb logcat -d > full_log.txt

# 앱 관련 로그만
adb logcat -d | grep "com.nihongo" > app_log.txt

# 에러만
adb logcat -d *:E > error_log.txt

# 특정 태그
adb logcat -d -s ChatViewModel:* GrammarAPI:* VoiceManager:* > debug_log.txt
```

---

## 자주 묻는 질문 (FAQ)

### Q1: Gemini API 키가 없으면 앱을 사용할 수 없나요?
**A**: 네, Gemini API는 필수입니다. AI 대화 기능의 핵심이기 때문입니다. 무료로 발급받을 수 있습니다: [Google AI Studio](https://makersuite.google.com/app/apikey)

### Q2: 번역 API 키도 필요한가요?
**A**: 아니요, 선택사항입니다. ML Kit 오프라인 번역이 자동으로 사용됩니다.

### Q3: 오프라인에서도 사용할 수 있나요?
**A**: 부분적으로 가능합니다:
- ✅ ML Kit 오프라인 번역
- ✅ 20개 공통 구문 캐시
- ✅ 최근 대화 복습
- ❌ 새로운 AI 대화 (Gemini API 필요)

### Q4: 데이터를 다른 기기로 옮길 수 있나요?
**A**: 현재는 지원하지 않습니다. (향후 업데이트 예정)

### Q5: 앱이 너무 많은 데이터를 사용하나요?
**A**: 아니요. 최적화되어 있습니다:
- 100개 메시지 기준 약 0.14MB (GZIP 압축)
- 캐싱으로 중복 요청 방지
- 오프라인 폴백

---

## 지원

### 추가 도움이 필요하신가요?

- **GitHub Issues**: [프로젝트 이슈 페이지](https://github.com/yourusername/nihongo-conversation/issues)
- **로그 첨부**: 문제 보고 시 logcat 로그를 함께 첨부해주세요
- **재현 단계**: 문제가 발생하는 정확한 단계를 설명해주세요

**이슈 템플릿**:
```markdown
## 문제 설명
[간단한 설명]

## 재현 단계
1. [단계 1]
2. [단계 2]
3. [단계 3]

## 예상 동작
[정상적으로 작동해야 하는 방식]

## 실제 동작
[실제로 발생하는 동작]

## 환경
- Android 버전: [예: 13]
- 기기 모델: [예: Pixel 7]
- 앱 버전: [예: 1.0.0]

## 로그
[logcat 로그 붙여넣기]
```
