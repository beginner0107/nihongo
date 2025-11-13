# 📋 음성 녹음 및 재생 기능 요구사항 명세서

## 1. 개요

### 1.1 목적
사용자가 일본어 회화 연습 시 자신의 음성을 녹음하고, 나중에 다시 들어볼 수 있는 기능을 제공하여 발음 개선 및 학습 효과를 극대화한다.

### 1.2 범위
- 음성 메시지 자동 녹음 및 저장
- 개별 메시지 재생
- 전체 대화 복습 모드
- 음성 파일 관리 시스템

### 1.3 용어 정의
- **Voice Recording**: 사용자가 STT를 통해 입력한 음성의 원본 파일
- **Voice Playback**: 저장된 음성을 다시 재생하는 기능
- **Conversation Replay**: 전체 대화를 순차적으로 재생하는 복습 모드

## 2. 기능 요구사항

### 2.1 음성 녹음 (FR-001)

#### 2.1.1 자동 녹음
- **FR-001-01**: 사용자가 음성 인식 버튼을 누르면 녹음이 자동으로 시작되어야 한다
- **FR-001-02**: STT 처리와 동시에 백그라운드에서 음성이 저장되어야 한다
- **FR-001-03**: 녹음은 사용자가 말을 멈추거나 최대 60초 후 자동 종료되어야 한다

#### 2.1.2 파일 저장
- **FR-001-04**: 음성 파일은 M4A(AAC) 형식으로 저장되어야 한다
- **FR-001-05**: 파일명은 `voice_[conversationId]_[messageId]_[timestamp].m4a` 형식이어야 한다
- **FR-001-06**: 파일은 앱의 캐시 디렉토리 `/cache/voice/` 하위에 저장되어야 한다

#### 2.1.3 메타데이터
- **FR-001-07**: 각 녹음에 대해 다음 정보가 DB에 저장되어야 한다:
  - 메시지 ID (외래키)
  - 대화 ID
  - 파일 경로
  - 녹음 시간 (밀리초)
  - 파일 크기 (바이트)
  - 녹음 일시
  - 언어 설정 (ja-JP/ko-KR)

### 2.2 음성 재생 (FR-002)

#### 2.2.1 개별 재생
- **FR-002-01**: 음성이 있는 메시지는 마이크 아이콘(🎤)이 표시되어야 한다
- **FR-002-02**: 아이콘 탭 시 음성이 재생되어야 한다
- **FR-002-03**: 재생 중에는 일시정지 아이콘으로 변경되어야 한다

#### 2.2.2 재생 컨트롤
- **FR-002-04**: 재생/일시정지 기능이 제공되어야 한다
- **FR-002-05**: 재생 진행률이 표시되어야 한다 (현재시간/전체시간)
- **FR-002-06**: 음성 재생 속도 조절이 가능해야 한다 (0.5x, 0.75x, 1.0x, 1.25x, 1.5x)

#### 2.2.3 재생 상태 관리
- **FR-002-07**: 한 번에 하나의 음성만 재생되어야 한다
- **FR-002-08**: 새로운 음성 재생 시 기존 재생은 자동 중지되어야 한다
- **FR-002-09**: 화면 전환 시 재생이 중지되어야 한다

### 2.3 대화 복습 모드 (FR-003)

#### 2.3.1 복습 화면
- **FR-003-01**: 대화 종료 후 "복습하기" 버튼이 제공되어야 한다
- **FR-003-02**: 복습 화면에서 전체 대화 내역이 시간순으로 표시되어야 한다
- **FR-003-03**: 각 메시지별로 재생 버튼이 제공되어야 한다

#### 2.3.2 연속 재생
- **FR-003-04**: "전체 재생" 버튼으로 모든 메시지를 순차 재생할 수 있어야 한다
- **FR-003-05**: 사용자 음성은 녹음 파일로, AI 응답은 TTS로 재생되어야 한다
- **FR-003-06**: 메시지 간 1초의 간격을 두고 재생되어야 한다

#### 2.3.3 선택적 재생
- **FR-003-07**: "내 음성만 재생" 옵션이 제공되어야 한다
- **FR-003-08**: "AI 응답만 재생" 옵션이 제공되어야 한다
- **FR-003-09**: 특정 구간 반복 재생이 가능해야 한다

### 2.4 파일 관리 (FR-004)

#### 2.4.1 저장 공간 관리
- **FR-004-01**: 전체 음성 파일 크기가 50MB를 초과하면 경고가 표시되어야 한다
- **FR-004-02**: 100MB 초과 시 오래된 파일부터 자동 삭제되어야 한다
- **FR-004-03**: 30일 이상 된 음성 파일은 자동 삭제되어야 한다

#### 2.4.2 수동 관리
- **FR-004-04**: 사용자가 개별 음성을 삭제할 수 있어야 한다
- **FR-004-05**: 전체 음성 기록을 삭제하는 옵션이 제공되어야 한다
- **FR-004-06**: 중요한 음성을 "보관" 표시할 수 있어야 한다 (자동 삭제 제외)

## 3. 비기능 요구사항

### 3.1 성능 요구사항 (NFR-001)
- **NFR-001-01**: 음성 녹음이 STT 성능에 영향을 주지 않아야 한다
- **NFR-001-02**: 5분 길이의 음성 재생이 100ms 이내에 시작되어야 한다
- **NFR-001-03**: 100개 메시지 목록이 1초 이내에 로드되어야 한다

### 3.2 보안 요구사항 (NFR-002)
- **NFR-002-01**: 음성 파일은 앱 전용 디렉토리에 저장되어야 한다
- **NFR-002-02**: 다른 앱에서 음성 파일에 접근할 수 없어야 한다
- **NFR-002-03**: 앱 삭제 시 모든 음성 파일이 함께 삭제되어야 한다

### 3.3 사용성 요구사항 (NFR-003)
- **NFR-003-01**: 음성 녹음 중임을 시각적으로 명확히 표시해야 한다
- **NFR-003-02**: 재생 실패 시 명확한 에러 메시지가 표시되어야 한다
- **NFR-003-03**: 모든 음성 관련 작업이 3탭 이내에 접근 가능해야 한다

### 3.4 호환성 요구사항 (NFR-004)
- **NFR-004-01**: Android 7.0 (API 24) 이상에서 동작해야 한다
- **NFR-004-02**: 다양한 음성 코덱을 지원하는 기기에서 동작해야 한다
- **NFR-004-03**: 블루투스 헤드셋에서도 정상 재생되어야 한다

## 4. 데이터 요구사항

### 4.1 데이터베이스 스키마

#### 4.1.1 voice_recordings 테이블
```sql
CREATE TABLE voice_recordings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id INTEGER NOT NULL,
    conversation_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    file_size_bytes INTEGER NOT NULL,
    recorded_at INTEGER NOT NULL,
    language TEXT NOT NULL,
    is_bookmarked INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_voice_recordings_conversation ON voice_recordings(conversation_id);
CREATE INDEX idx_voice_recordings_created_at ON voice_recordings(created_at);
```

#### 4.1.2 messages 테이블 수정
```sql
ALTER TABLE messages ADD COLUMN voice_recording_id INTEGER;
ALTER TABLE messages ADD COLUMN input_type TEXT DEFAULT 'text'; -- 'text', 'voice'
```

### 4.2 파일 저장 구조
```
/data/data/com.nihongo.conversation/cache/
└── voice/
    ├── voice_1_101_1731033600000.m4a
    ├── voice_1_102_1731033660000.m4a
    └── metadata/
        ├── voice_1_101_1731033600000.json
        └── voice_1_102_1731033660000.json
```

## 5. UI/UX 요구사항

### 5.1 메시지 버블 UI

#### 5.1.1 음성 메시지 표시
```
┌─────────────────────────────────┐
│ ありがとうございました         │
│                               │
│ 🎤 ━━━●━━━━ 3.2s             │  ← 미니 플레이어
│                               │
│ 14:30                [⋮]      │
└─────────────────────────────────┘
```

#### 5.1.2 재생 중 상태
```
┌─────────────────────────────────┐
│ ありがとうございました         │
│                               │
│ ⏸️ ━━━━━●━━ 1.8s/3.2s        │  ← 진행 중
│                               │
│ 14:30                [⋮]      │
└─────────────────────────────────┘
```

### 5.2 복습 모드 UI

#### 5.2.1 대화 목록
```
📚 대화 복습
━━━━━━━━━━━━━━━━━━━━━━━━━━━
[▶️ 전체 재생] [👤 내 음성만] [🤖 AI만]

┌─────────────────────────────────┐
│ 1. 👤 こんにちは (0:02)    [▶️] │
├─────────────────────────────────┤
│ 2. 🤖 こんにちは！(0:03)   [▶️] │
├─────────────────────────────────┤
│ 3. 👤 元気ですか (0:03)    [▶️] │
└─────────────────────────────────┘

재생 시간: 총 2분 34초
음성 메시지: 8/15개
```

## 6. 상태 전이도

### 6.1 녹음 상태
```
[대기] → [녹음 중] → [처리 중] → [완료]
   ↑        ↓           ↓         ↓
   ←────[에러]←─────────┴─────────┘
```

### 6.2 재생 상태
```
[정지] ⇄ [재생 중] ⇄ [일시정지]
   ↑        ↓           ↓
   ←────[완료]←─────────┘
```

## 7. 에러 처리

### 7.1 녹음 실패
- **E-001**: 마이크 권한 없음 → "마이크 권한을 허용해주세요"
- **E-002**: 저장 공간 부족 → "저장 공간이 부족합니다"
- **E-003**: 코덱 미지원 → "이 기기는 음성 녹음을 지원하지 않습니다"

### 7.2 재생 실패
- **E-004**: 파일 없음 → "음성 파일을 찾을 수 없습니다"
- **E-005**: 파일 손상 → "음성 파일이 손상되었습니다"
- **E-006**: 재생 중 오류 → "재생 중 오류가 발생했습니다"

## 8. 테스트 요구사항

### 8.1 단위 테스트
- 녹음 시작/중지 로직
- 파일 저장 및 메타데이터 생성
- DB CRUD 작업
- 파일 크기 계산 및 관리

### 8.2 통합 테스트
- STT와 녹음 동시 처리
- 전체 대화 재생 흐름
- 파일 자동 삭제 로직
- 메모리 누수 확인

### 8.3 UI 테스트
- 음성 아이콘 표시/숨김
- 재생 컨트롤 상호작용
- 복습 모드 네비게이션
- 에러 메시지 표시

## 9. 구현 우선순위

### Phase 1 (필수 - 1주차)
1. VoiceRecordingManager 구현
2. DB 스키마 및 Migration
3. 기본 녹음/저장 기능
4. 개별 메시지 재생

### Phase 2 (중요 - 2주차)
1. 복습 모드 UI
2. 전체 대화 재생
3. 재생 컨트롤 (속도, 구간)
4. 파일 관리 시스템

### Phase 3 (선택 - 3주차)
1. 음성 품질 선택
2. 북마크 기능
3. 음성 통계
4. 내보내기 기능

## 10. 제약사항

### 10.1 기술적 제약
- MediaRecorder API 사용 (Android 기본)
- 최대 파일 크기: 10MB/파일
- 최대 녹음 시간: 60초/메시지
- 지원 코덱: AAC만 지원

### 10.2 비즈니스 제약
- 클라우드 백업 미지원 (로컬만)
- 음성 공유 기능 미제공 (개인정보)
- 외부 앱 연동 불가

## 11. 승인 기준

### 11.1 기능 완성도
- [ ] 모든 음성 메시지가 자동 저장됨
- [ ] 저장된 음성을 재생할 수 있음
- [ ] 복습 모드에서 전체 대화 재생 가능
- [ ] 파일 자동 관리 동작 확인

### 11.2 품질 기준
- [ ] 녹음이 STT 성능에 영향 없음
- [ ] 재생 시 음질 저하 없음
- [ ] 메모리 누수 없음
- [ ] 크래시 발생률 0.1% 이하

## 12. 향후 확장 계획

### 12.1 단기 (3개월)
- 발음 점수 분석
- 음성 비교 기능
- 쉐도잉 모드

### 12.2 중기 (6개월)
- 클라우드 백업
- 음성 공유 (SNS)
- AI 발음 코칭

### 12.3 장기 (1년)
- 음성 인식 정확도 개선
- 다국어 지원
- 음성 클론 기술

---

## 부록 A: 기술 스택

### A.1 음성 녹음
- **MediaRecorder**: Android 기본 API
- **AudioRecord**: 저수준 API (대안)
- **코덱**: AAC-LC (Low Complexity)
- **샘플링**: 44.1kHz, 16bit, Mono

### A.2 음성 재생
- **MediaPlayer**: 기본 재생기
- **ExoPlayer**: 고급 기능 (대안)
- **AudioTrack**: 저수준 API

### A.3 파일 관리
- **저장소**: Internal Cache Directory
- **형식**: M4A container with AAC
- **메타데이터**: JSON sidecar files

## 부록 B: API 인터페이스

### B.1 VoiceRecordingManager
```kotlin
interface VoiceRecordingManager {
    fun startRecording(conversationId: Long): Flow<RecordingState>
    fun stopRecording(): RecordingResult
    fun pauseRecording()
    fun resumeRecording()
    fun cancelRecording()
    fun getRecordingDuration(): Long
    fun isRecording(): Boolean
}
```

### B.2 VoicePlaybackManager
```kotlin
interface VoicePlaybackManager {
    fun play(recordingId: Long): Flow<PlaybackState>
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(position: Long)
    fun setSpeed(speed: Float)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
}
```

### B.3 VoiceFileManager
```kotlin
interface VoiceFileManager {
    suspend fun saveRecording(data: ByteArray, metadata: VoiceMetadata): VoiceFile
    suspend fun loadRecording(recordingId: Long): VoiceFile?
    suspend fun deleteRecording(recordingId: Long): Boolean
    suspend fun deleteOldRecordings(olderThan: Long): Int
    suspend fun getTotalSize(): Long
    suspend fun cleanup(maxSize: Long): Int
}
```

## 부록 C: 데이터 모델

### C.1 VoiceRecording
```kotlin
data class VoiceRecording(
    val id: Long = 0,
    val messageId: Long,
    val conversationId: Long,
    val filePath: String,
    val fileName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val recordedAt: Long,
    val language: String,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### C.2 RecordingState
```kotlin
sealed class RecordingState {
    object Idle : RecordingState()
    object Preparing : RecordingState()
    data class Recording(val durationMs: Long) : RecordingState()
    object Paused : RecordingState()
    data class Error(val message: String) : RecordingState()
    data class Completed(val recording: VoiceRecording) : RecordingState()
}
```

### C.3 PlaybackState
```kotlin
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    data class Playing(
        val position: Long,
        val duration: Long,
        val speed: Float = 1.0f
    ) : PlaybackState()
    data class Paused(
        val position: Long,
        val duration: Long
    ) : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
```

---

이 요구사항 문서를 바탕으로 구현을 진행하시면 체계적으로 음성 녹음/재생 기능을 개발할 수 있을 것입니다.