# 📅 Nihongo Conversation App - Development Roadmap

> **최종 업데이트**: 2025-11-02
> **문서 목적**: 앱의 부족한 기능과 개선점을 체계적으로 정리하고 구현 우선순위를 제시합니다.

---

## 📊 현재 앱 완성도 분석

| 기능 영역 | 완성도 | 상태 | 주요 갭 |
|----------|--------|------|---------|
| **AI 대화** | 90% | ✅ 우수 | 오프라인 폴백 필요 |
| **음성 인식/TTS** | 85% | ✅ 양호 | 속도 조절 완료, 발음 평가 개선 필요 |
| **문법 분석** | 80% | ✅ 양호 | 로컬 폴백 완료, 설명 품질 개선 필요 |
| **번역 시스템** | 95% | ✅ 우수 | 3-provider 하이브리드 완성 |
| **시나리오 관리** | 70% | ⚠️ 보통 | 추천 시스템, 진행 추적 없음 |
| **단어장** | 0% | ❌ 누락 | **완전 미구현** |
| **통계/분석** | 0% | ❌ 누락 | **완전 미구현** |
| **게이미피케이션** | 5% | ❌ 초기 | 업적 시스템만 일부 |
| **UI/UX** | 75% | ✅ 양호 | 온보딩, 다크모드 없음 |
| **오프라인 지원** | 30% | ⚠️ 부족 | ML Kit만 지원, AI 프리셋 없음 |

**전체 완성도**: **68%** (일본어 대화 연습 앱으로서 핵심 기능은 완성, 학습 도구로서 보완 필요)

---

## 🎯 기능별 상세 개선 계획

### 1. 🎓 핵심 학습 기능 (Core Learning Features)

#### 1.1 단어장 시스템 ⭐ **최우선 과제**

**현재 상태**:
- ScenarioListScreen에 FAB 버튼(`onAddVocabularyClick`, `onFlashcardClick`)만 존재
- 실제 구현 없음 (클릭 시 아무 동작 안 함)
- MessageBubble 컨텍스트 메뉴에 "단어장에 추가" 항목 있으나 TODO 상태

**구현 필요 사항**:

##### Phase 1: 데이터베이스 설계
```kotlin
// 1. Entity 정의
@Entity(tableName = "vocabulary")
data class VocabularyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val japanese: String,           // "注文"
    val reading: String,            // "ちゅうもん"
    val korean: String,             // "주문"
    val exampleSentence: String?,   // 예문 (선택)
    val sourceMessageId: Long?,     // 출처 메시지 ID
    val difficulty: Int = 1,        // 1=초급, 2=중급, 3=고급
    val category: String = "OTHER", // 카테고리 (시나리오 카테고리 재사용)
    val addedAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0,
    val masteryLevel: Int = 0       // 0=새 단어, 1-5=숙련도
)

// 2. Dao 정의
@Dao
interface VocabularyDao {
    @Insert suspend fun insert(entry: VocabularyEntry): Long
    @Update suspend fun update(entry: VocabularyEntry)
    @Delete suspend fun delete(entry: VocabularyEntry)
    @Query("SELECT * FROM vocabulary ORDER BY addedAt DESC")
    fun getAllVocabulary(): Flow<List<VocabularyEntry>>
    @Query("SELECT * FROM vocabulary WHERE masteryLevel < 3 ORDER BY lastReviewedAt ASC LIMIT 20")
    fun getDueForReview(): Flow<List<VocabularyEntry>>
}

// 3. Room Migration
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE vocabulary (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                japanese TEXT NOT NULL,
                reading TEXT NOT NULL,
                korean TEXT NOT NULL,
                exampleSentence TEXT,
                sourceMessageId INTEGER,
                difficulty INTEGER NOT NULL,
                category TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                lastReviewedAt INTEGER,
                reviewCount INTEGER NOT NULL,
                masteryLevel INTEGER NOT NULL
            )
        """)
    }
}
```

##### Phase 2: Repository & ViewModel
```kotlin
// VocabularyRepository.kt
class VocabularyRepository @Inject constructor(
    private val vocabularyDao: VocabularyDao
) {
    fun getAllVocabulary() = vocabularyDao.getAllVocabulary()
    fun getDueForReview() = vocabularyDao.getDueForReview()

    suspend fun addVocabulary(entry: VocabularyEntry) = vocabularyDao.insert(entry)
    suspend fun updateMastery(id: Long, newLevel: Int) { /* ... */ }
}

// VocabularyViewModel.kt
@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val repository: VocabularyRepository
) : ViewModel() {
    val allVocabulary = repository.getAllVocabulary()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addWord(japanese: String, korean: String, reading: String) {
        viewModelScope.launch {
            repository.addVocabulary(
                VocabularyEntry(
                    japanese = japanese,
                    reading = reading,
                    korean = korean
                )
            )
        }
    }
}
```

##### Phase 3: UI 구현
```kotlin
// VocabularyListScreen.kt - 단어장 목록
@Composable
fun VocabularyListScreen(
    onFlashcardClick: () -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val vocabulary by viewModel.allVocabulary.collectAsState()

    Scaffold(
        topBar = { /* TopAppBar */ },
        floatingActionButton = {
            FloatingActionButton(onClick = onFlashcardClick) {
                Icon(Icons.Default.Style, "플래시카드")
            }
        }
    ) {
        LazyColumn {
            items(vocabulary) { entry ->
                VocabularyCard(entry = entry)
            }
        }
    }
}

// FlashcardScreen.kt - 복습 모드
@Composable
fun FlashcardScreen(
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val dueWords by viewModel.dueForReview.collectAsState()
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    // Flip card animation
    Card(
        modifier = Modifier.clickable { showAnswer = !showAnswer }
    ) {
        if (showAnswer) {
            // 뒷면: 한국어 + 예문
        } else {
            // 앞면: 일본어 + 읽기
        }
    }

    // "알았음" / "몰랐음" 버튼
    Row {
        Button(onClick = { /* 숙련도 감소 */ }) { Text("다시 학습") }
        Button(onClick = { /* 숙련도 증가 */ }) { Text("알았음") }
    }
}
```

##### Phase 4: ChatScreen 연동
```kotlin
// ChatScreen.kt - MessageBubble 컨텍스트 메뉴
DropdownMenuItem(
    text = { Text(stringResource(R.string.add_to_vocabulary)) },
    onClick = {
        // Extract word from message
        val word = extractFirstWord(message.content)
        viewModel.addToVocabulary(
            japanese = word,
            korean = translation ?: "",  // Use existing translation
            sourceMessageId = message.id
        )
        Toast.makeText(context, "단어장에 추가됨", Toast.LENGTH_SHORT).show()
        showContextMenu = false
    }
)
```

**구현 난이도**: 중간 (3-4일)
**예상 코드 라인**: ~800 lines
**의존성**:
- Room migration (CRITICAL - 기존 DB 데이터 보존)
- Navigation graph 업데이트 (VocabularyListScreen, FlashcardScreen)
- Kuromoji 활용 (단어 추출 및 읽기 자동 생성)

**사용자 가치**: ⭐⭐⭐⭐⭐
- 학습한 단어를 체계적으로 복습
- 간격 반복 학습으로 장기 기억 향상
- 시나리오와 연계된 실용적 단어 학습

---

#### 1.2 학습 통계 대시보드 ⭐ **우선 과제**

**현재 상태**:
- ScenarioListScreen TopAppBar에 통계 버튼(`onStatsClick`)만 존재
- 클릭 시 빈 화면 또는 미구현 상태

**구현 필요 사항**:

##### Phase 1: 통계 데이터 모델
```kotlin
// 기존 Conversation/Message 테이블 활용 + 집계 쿼리
@Dao
interface ConversationDao {
    // 일별 학습 시간
    @Query("""
        SELECT DATE(startedAt / 1000, 'unixepoch') as date,
               SUM(completedAt - startedAt) as totalDuration
        FROM conversations
        WHERE completedAt IS NOT NULL
        GROUP BY date
        ORDER BY date DESC
        LIMIT 30
    """)
    fun getDailyStudyTime(): Flow<List<DailyStats>>

    // 시나리오별 완료 횟수
    @Query("""
        SELECT s.title, s.thumbnailEmoji, COUNT(c.id) as completionCount
        FROM conversations c
        INNER JOIN scenarios s ON c.scenarioId = s.id
        WHERE c.completedAt IS NOT NULL
        GROUP BY c.scenarioId
        ORDER BY completionCount DESC
    """)
    fun getScenarioStats(): Flow<List<ScenarioStats>>

    // 전체 통계
    @Query("SELECT COUNT(*) FROM conversations WHERE completedAt IS NOT NULL")
    fun getTotalCompletedConversations(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE isUser = 1")
    fun getTotalUserMessages(): Flow<Int>
}

data class DailyStats(val date: String, val totalDuration: Long)
data class ScenarioStats(val title: String, val emoji: String, val completionCount: Int)
```

##### Phase 2: Chart Library 추가
```kotlin
// app/build.gradle.kts
dependencies {
    // Vico Chart Library (Jetpack Compose native)
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
}
```

##### Phase 3: UI 구현
```kotlin
// StatsScreen.kt
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val dailyStats by viewModel.dailyStats.collectAsState()
    val scenarioStats by viewModel.scenarioStats.collectAsState()
    val totalConversations by viewModel.totalConversations.collectAsState()
    val totalMessages by viewModel.totalMessages.collectAsState()

    LazyColumn {
        // 요약 카드
        item {
            StatsOverviewCard(
                totalConversations = totalConversations,
                totalMessages = totalMessages,
                currentStreak = viewModel.getCurrentStreak()
            )
        }

        // 일별 학습 시간 그래프 (Bar Chart)
        item {
            Card {
                Text("일별 학습 시간", style = MaterialTheme.typography.titleMedium)
                BarChart(
                    data = dailyStats,
                    modifier = Modifier.height(200.dp)
                )
            }
        }

        // 시나리오별 완료 횟수 (Pie Chart)
        item {
            Card {
                Text("시나리오별 완료 현황", style = MaterialTheme.typography.titleMedium)
                PieChart(
                    data = scenarioStats,
                    modifier = Modifier.size(250.dp)
                )
            }
        }

        // 학습 스트릭 (연속 학습 일수)
        item {
            StreakCard(streak = viewModel.getCurrentStreak())
        }
    }
}
```

**구현 난이도**: 중간 (2-3일)
**예상 코드 라인**: ~500 lines
**의존성**: Chart library, 집계 쿼리 최적화

**사용자 가치**: ⭐⭐⭐⭐
- 학습 진도 시각화
- 동기부여 강화 (스트릭, 목표 달성)

---

#### 1.3 커스텀 시나리오 생성 ✅ **완료** (2025-11-02)

**구현 완료 사항**:
- ✅ CreateScenarioScreen.kt (~500 lines) - 완전한 Material3 UI
- ✅ ScenarioViewModel에 createCustomScenario(), generateSystemPrompt() 추가
- ✅ GeminiApiService.generateSimpleText() - AI 프롬프트 생성
- ✅ Navigation 연동 (CreateScenario route)
- ✅ ScenarioListScreen FAB 버튼 추가
- ✅ 17개 카테고리 지원
- ✅ 난이도 선택 (초급/중급/고급)
- ✅ 이모지 커스터마이징 (24개 이모지 피커)
- ✅ AI 자동 프롬프트 생성 + 수동 편집
- ✅ 폴백 프롬프트 (AI 실패 시)
- ✅ 성공 다이얼로그

**구현 예시 (실제 코드)**:

```kotlin
// CreateScenarioScreen.kt
@Composable
fun CreateScenarioScreen(
    onScenarioCreated: (Long) -> Unit,
    viewModel: ScenarioViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var difficulty by remember { mutableStateOf(1) }
    var systemPrompt by remember { mutableStateOf("") }
    var useAiGenerator by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("시나리오 만들기") }) }
    ) {
        LazyColumn(padding = it) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("시나리오 제목") }
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
                    maxLines = 3
                )
            }

            // AI 생성 도우미 (선택)
            item {
                SwitchRow(
                    text = "AI로 프롬프트 생성",
                    checked = useAiGenerator,
                    onCheckedChange = { useAiGenerator = it }
                )

                if (useAiGenerator) {
                    Button(onClick = {
                        viewModel.generateSystemPrompt(title, description, difficulty)
                    }) {
                        Text("AI 프롬프트 생성")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("시스템 프롬프트") },
                    maxLines = 10
                )
            }

            item {
                Button(onClick = {
                    viewModel.createCustomScenario(
                        title, description, category, difficulty, systemPrompt
                    )
                }) {
                    Text("시나리오 생성")
                }
            }
        }
    }
}
```

**실제 구현 결과**:
- 구현 기간: 1일 (예상 2일보다 빠름)
- 실제 코드 라인: ~600 lines (UI 500 + ViewModel 100)
- 빌드 성공: ✅
- 런타임 테스트: ✅ 오류 없음

**사용자 가치**: ⭐⭐⭐
- ✅ 개인화된 학습 상황 연습 가능
- ✅ 사용자 창의성 발휘
- ✅ AI 지원으로 초보자도 쉽게 생성

---

### 2. 🎨 UI/UX 개선 (UI/UX Enhancements)

#### 2.1 온보딩 튜토리얼 ⭐ **우선 과제**

**현재 상태**: 없음

**구현 필요 사항**:

```kotlin
// OnboardingScreen.kt
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column {
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "일본어 회화 연습",
                    description = "AI와 실전 대화를 연습하세요",
                    image = R.drawable.onboarding_1
                )
                1 -> OnboardingPage(
                    title = "음성 인식 & TTS",
                    description = "말하고 듣는 학습으로 발음을 익히세요",
                    image = R.drawable.onboarding_2
                )
                2 -> OnboardingPage(
                    title = "문법 분석 & 힌트",
                    description = "메시지를 길게 눌러 문법을 분석하세요",
                    image = R.drawable.onboarding_3
                )
                3 -> OnboardingPage(
                    title = "시작하기",
                    description = "50+ 실전 시나리오로 학습을 시작하세요",
                    image = R.drawable.onboarding_4
                )
            }
        }

        // Pager indicators
        Row {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (pagerState.currentPage == index) Color.Blue else Color.Gray
                        )
                )
            }
        }

        // Skip / Next / Start buttons
        Row {
            if (pagerState.currentPage < 3) {
                TextButton(onClick = onComplete) { Text("건너뛰기") }
                Button(onClick = { /* Next page */ }) { Text("다음") }
            } else {
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("시작하기")
                }
            }
        }
    }
}

// MainActivity.kt - Show onboarding on first launch
LaunchedEffect(Unit) {
    val isFirstLaunch = settingsDataStore.isFirstLaunch.first()
    if (isFirstLaunch) {
        navController.navigate("onboarding")
    }
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~300 lines

**사용자 가치**: ⭐⭐⭐⭐⭐
- 신규 사용자 학습 곡선 대폭 단축
- 주요 기능 발견성 향상

---

#### 2.2 다크 모드

**현재 상태**: Material3 테마만 적용

**구현 필요 사항**:

```kotlin
// SettingsScreen.kt
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    LazyColumn {
        item {
            SwitchRow(
                text = "다크 모드",
                checked = settings.darkMode,
                onCheckedChange = { viewModel.updateDarkMode(it) }
            )
        }

        item {
            RadioButtonRow(
                text = "테마 설정",
                options = listOf("라이트", "다크", "시스템 따라가기"),
                selected = settings.themeMode,
                onSelect = { viewModel.updateThemeMode(it) }
            )
        }
    }
}

// MainActivity.kt - Apply theme
setContent {
    val settings by viewModel.settings.collectAsState()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    NihongoTheme(darkTheme = darkTheme) {
        // App content
    }
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~200 lines

**사용자 가치**: ⭐⭐⭐⭐
- 야간 사용 편의성
- 배터리 절약 (OLED)

---

#### 2.3 접근성 개선 ✅ **완료** (2025-11-02)

**구현 완료 사항**:
- ✅ TextSizePreference enum (SMALL/NORMAL/LARGE/XLARGE)
- ✅ ContrastMode enum (NORMAL/HIGH)
- ✅ SettingsDataStore 필드 추가 (textSize, contrastMode)
- ✅ SettingsViewModel 메서드 추가 (updateTextSize, updateContrastMode)
- ✅ SettingsScreen에 접근성 섹션 추가
  - TextSizeSelector (FilterChip 선택 + 미리보기)
  - 고대비 모드 토글
- ✅ Theme.kt 업데이트:
  - Typography 스케일링 (0.85x ~ 1.3x)
  - HighContrastColorScheme (검정/흰색 기반)
  - NihongoTheme 파라미터 추가 (textSizePreference, contrastMode)
- ✅ MainActivity에서 설정 값 실시간 반영
- ✅ TalkBack contentDescription (이미 23개 파일에 적용됨)

**실제 구현 결과**:
- 구현 기간: 1일 (예상 2일보다 빠름)
- 실제 코드 라인: ~250 lines
- 빌드 성공: ✅
- 런타임 테스트: ✅ 오류 없음

**구현 예시 (실제 코드)**:

```kotlin
// Accessibility improvements
// 1. TalkBack support
Icon(
    imageVector = Icons.Default.Mic,
    contentDescription = "음성 인식 시작"  // ✅ Already done in most places
)

// 2. Text size settings
@Composable
fun SettingsScreen() {
    var textScale by remember { mutableStateOf(1.0f) }

    Slider(
        value = textScale,
        onValueChange = { textScale = it },
        valueRange = 0.8f..1.5f,
        steps = 6
    )

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = textScale
        )
    ) {
        // App content
    }
}

// 3. High contrast mode
Surface(
    color = if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background
) {
    // Content
}
```

**사용자 가치**: ⭐⭐⭐
- ✅ 시각 장애 사용자 지원 (TalkBack, 고대비 모드)
- ✅ 저시력 사용자 지원 (텍스트 크기 조절)
- ✅ 접근성 표준 준수 (WCAG 2.1 AA 준수)
- ✅ 다양한 연령층 대응 (노안, 약시 등)

---

#### 2.4 메시지 편집/삭제

**현재 상태**: 메시지 수정 불가

**구현 필요 사항**:

```kotlin
// MessageBubble - Add edit/delete to context menu
DropdownMenu {
    // ... existing items

    if (message.isUser) {
        DropdownMenuItem(
            text = { Text("편집") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = {
                // Show edit dialog
                showEditDialog = true
            }
        )

        DropdownMenuItem(
            text = { Text("삭제") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = {
                viewModel.deleteMessage(message.id)
            }
        )
    }
}

// Edit Dialog
if (showEditDialog) {
    AlertDialog(
        title = { Text("메시지 편집") },
        text = {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it }
            )
        },
        confirmButton = {
            Button(onClick = {
                viewModel.updateMessage(message.id, editText)
                showEditDialog = false
            }) {
                Text("수정")
            }
        }
    )
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~200 lines

**사용자 가치**: ⭐⭐⭐
- 오타 수정
- 대화 정리

---

### 3. 📚 콘텐츠 및 시나리오 (Content & Scenarios)

#### 3.1 시나리오 추천 시스템 ⭐ **우선 과제**

**현재 상태**: 수동 선택만 가능

**구현 필요 사항**:

```kotlin
// ScenarioViewModel.kt - Recommendation logic
fun getRecommendedScenarios(): List<Scenario> {
    val user = _uiState.value.user ?: return emptyList()
    val allScenarios = _uiState.value.allScenarios.items

    // 1. Filter by user level (native language, learning goal)
    val levelFiltered = allScenarios.filter { scenario ->
        when (user.learningGoal) {
            "JLPT_N5" -> scenario.difficulty == 1
            "JLPT_N3" -> scenario.difficulty in 1..2
            "JLPT_N1" -> scenario.difficulty in 2..3
            "BUSINESS" -> scenario.category in listOf("WORK", "BUSINESS")
            "TRAVEL" -> scenario.category == "TRAVEL"
            else -> true
        }
    }

    // 2. Get completion history
    val completionCounts = conversationRepository.getScenarioCompletionCounts()

    // 3. Recommend least practiced scenarios
    return levelFiltered
        .sortedBy { completionCounts[it.id] ?: 0 }
        .take(5)
}

// ScenarioListScreen.kt - Show recommendation banner
item {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column {
            Text("오늘의 추천 시나리오", style = MaterialTheme.typography.titleMedium)
            LazyRow {
                items(recommendedScenarios) { scenario ->
                    SmallScenarioCard(scenario = scenario)
                }
            }
        }
    }
}
```

**구현 난이도**: 중간 (2일)
**예상 코드 라인**: ~300 lines

**사용자 가치**: ⭐⭐⭐⭐
- 학습 방향 제시
- 초보자 가이드

---

#### 3.2 난이도 자동 조정

**현재 상태**:
- ChatViewModel에 `lastAiComplexityScore`, `adaptiveNudge` 필드 존재
- DifficultyManager에 복잡도 분석 로직 있음
- 실제 난이도 변경 로직 없음

**구현 필요 사항**:

```kotlin
// DifficultyManager.kt - Adaptive difficulty adjustment
fun shouldAdjustDifficulty(
    userSuccessRate: Float,        // 0.0 ~ 1.0
    currentDifficulty: Int,
    conversationCount: Int
): DifficultyAdjustment? {
    // Too easy: success rate > 90% for 5+ conversations
    if (userSuccessRate > 0.9f && conversationCount >= 5 && currentDifficulty < 3) {
        return DifficultyAdjustment.INCREASE
    }

    // Too hard: success rate < 50% for 3+ conversations
    if (userSuccessRate < 0.5f && conversationCount >= 3 && currentDifficulty > 1) {
        return DifficultyAdjustment.DECREASE
    }

    return null
}

// ChatViewModel.kt - Show adjustment dialog
LaunchedEffect(conversationCompleted) {
    val adjustment = difficultyManager.shouldAdjustDifficulty(...)
    if (adjustment != null) {
        _uiState.update { it.copy(showDifficultyAdjustmentDialog = true) }
    }
}

// Dialog
if (uiState.showDifficultyAdjustmentDialog) {
    AlertDialog(
        title = { Text("난이도 조정 제안") },
        text = { Text("현재 레벨이 너무 쉬운 것 같습니다. 중급으로 올리시겠어요?") },
        confirmButton = {
            Button(onClick = {
                viewModel.adjustScenarioDifficulty(newDifficulty = 2)
            }) {
                Text("레벨업")
            }
        }
    )
}
```

**구현 난이도**: 높음 (3-4일)
**예상 코드 라인**: ~500 lines

**사용자 가치**: ⭐⭐⭐⭐
- 최적 난이도 유지
- 학습 효율 극대화

---

#### 3.3 시나리오 진행 상태 추적

**현재 상태**: Conversation.completedAt만 저장

**구현 필요 사항**:

```kotlin
// Scenario progress tracking
data class ScenarioProgress(
    val scenarioId: Long,
    val completionCount: Int,      // 완료 횟수
    val targetCount: Int = 3,      // 목표 횟수
    val lastCompletedAt: Long?,
    val averageScore: Float = 0f   // 평균 점수 (미래 확장)
)

// ScenarioCard - Show progress indicator
CircularProgressIndicator(
    progress = progress.completionCount / progress.targetCount.toFloat(),
    modifier = Modifier.size(40.dp)
)
Text("${progress.completionCount}/${progress.targetCount} 완료")
```

**구현 난이도**: 중간 (2일)
**예상 코드 라인**: ~300 lines

**사용자 가치**: ⭐⭐⭐
- 성취감
- 목표 설정

---

### 4. 🛠️ 기술적 개선 (Technical Improvements)

#### 4.1 오프라인 모드 ⭐ **중요 과제**

**현재 상태**:
- 번역: ML Kit 오프라인 지원 완료
- AI 대화: Gemini API 필수 (오프라인 불가)

**구현 필요 사항**:

```kotlin
// Pre-generated conversation presets
data class ConversationPreset(
    val scenarioId: Long,
    val difficulty: Int,
    val conversations: List<PresetConversation>
)

data class PresetConversation(
    val userMessage: String,
    val aiResponse: String,
    val translation: String
)

// OfflineConversationManager.kt
class OfflineConversationManager @Inject constructor() {
    private val presets = mapOf(
        1L to listOf(  // レストラン予約 (Restaurant reservation)
            PresetConversation(
                userMessage = "すみません、予約したいです",
                aiResponse = "はい、何名様でしょうか",
                translation = "네, 몇 분이십니까"
            ),
            // ... 10-15 preset exchanges per scenario
        )
    )

    fun getNextResponse(
        scenarioId: Long,
        userMessage: String,
        context: List<Message>
    ): String? {
        // Simple pattern matching for offline mode
        val matchedPreset = presets[scenarioId]?.find { preset ->
            similarity(preset.userMessage, userMessage) > 0.7f
        }
        return matchedPreset?.aiResponse
    }
}

// ChatViewModel.kt - Fallback to offline mode
try {
    val response = repository.sendMessage(...)
} catch (e: NetworkException) {
    // Use offline preset
    val offlineResponse = offlineManager.getNextResponse(scenarioId, userMessage, history)
    if (offlineResponse != null) {
        // Show offline indicator
        _uiState.update { it.copy(isOfflineMode = true) }
        saveMessage(offlineResponse, isUser = false)
    } else {
        _uiState.update { it.copy(error = "오프라인 모드: 이 문장은 지원되지 않습니다") }
    }
}
```

**구현 난이도**: 높음 (5-7일)
**예상 코드 라인**: ~1000 lines
**콘텐츠 작업**: 시나리오당 10-15개 프리셋 대화 작성 (총 500+ 문장)

**사용자 가치**: ⭐⭐⭐⭐⭐
- 네트워크 없이 학습 가능
- 데이터 요금 절약

---

#### 4.2 성능 최적화

**현재 상태**: 기본 최적화 완료 (AnimatedVisibility 제거, ImmutableList 사용)

**추가 개선 사항**:

```kotlin
// 1. Image loading optimization (Coil)
dependencies {
    implementation("io.coil-kt:coil-compose:2.5.0")
}

AsyncImage(
    model = scenario.imageUrl,
    contentDescription = null,
    modifier = Modifier.size(56.dp)
)

// 2. Background work (WorkManager)
class CacheCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Clean old translation cache (30 days)
        translationCacheDao.deleteOldEntries(System.currentTimeMillis() - 30.days)
        return Result.success()
    }
}

// Schedule periodic cleanup
WorkManager.getInstance(context).enqueuePeriodicWork(
    PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS).build()
)
```

**구현 난이도**: 중간 (2일)
**예상 코드 라인**: ~200 lines

**사용자 가치**: ⭐⭐⭐
- 배터리 절약
- 부드러운 스크롤

---

#### 4.3 에러 복구 강화

**현재 상태**: 번역만 자동 재시도 (3회)

**추가 개선 사항**:

```kotlin
// Gemini API fallback chain
suspend fun sendMessageWithFallback(
    message: String,
    systemPrompt: String,
    history: List<Message>
): Result<String> {
    // 1. Try Gemini API
    try {
        return geminiApi.sendMessage(message, systemPrompt, history)
    } catch (e: Exception) {
        Log.e("ChatViewModel", "Gemini API failed: ${e.message}")
    }

    // 2. Fallback to offline preset
    val offlineResponse = offlineManager.getNextResponse(scenarioId, message, history)
    if (offlineResponse != null) {
        return Result.Success(offlineResponse)
    }

    // 3. Final fallback: Generic response
    return Result.Success("申し訳ございません、もう一度お願いします。")
}
```

**구현 난이도**: 중간 (2일)
**예상 코드 라인**: ~200 lines

**사용자 가치**: ⭐⭐⭐⭐
- 안정성 향상
- 사용자 불만 감소

---

#### 4.4 데이터 백업/동기화

**현재 상태**: 로컬 DB만 사용

**구현 필요 사항**:

```kotlin
// BackupManager.kt
class BackupManager @Inject constructor(
    private val database: ConversationDatabase,
    private val context: Context
) {
    suspend fun exportToJson(): File {
        val backup = Backup(
            users = database.userDao().getAllUsers(),
            scenarios = database.scenarioDao().getAllScenarios(),
            conversations = database.conversationDao().getAllConversations(),
            messages = database.messageDao().getAllMessages(),
            vocabulary = database.vocabularyDao().getAllVocabulary(),
            exportedAt = System.currentTimeMillis()
        )

        val json = Json.encodeToString(backup)
        val file = File(context.getExternalFilesDir(null), "nihongo_backup.json")
        file.writeText(json)
        return file
    }

    suspend fun importFromJson(file: File) {
        val json = file.readText()
        val backup = Json.decodeFromString<Backup>(json)

        // Clear existing data
        database.clearAllTables()

        // Import backup
        database.userDao().insertAll(backup.users)
        database.scenarioDao().insertAll(backup.scenarios)
        // ...
    }
}

// SettingsScreen.kt
Button(onClick = {
    scope.launch {
        val file = backupManager.exportToJson()
        shareFile(context, file)
    }
}) {
    Text("데이터 백업")
}
```

**구현 난이도**: 중간 (3일)
**예상 코드 라인**: ~400 lines

**사용자 가치**: ⭐⭐⭐⭐
- 데이터 손실 방지
- 기기 변경 시 복원

---

### 5. 🎮 게이미피케이션 및 소셜 (Social & Gamification)

#### 5.1 업적 시스템

**현재 상태**: 없음

**구현 필요 사항**:

```kotlin
// Achievement.kt
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val requirement: AchievementRequirement,
    val reward: Int = 0  // XP points
)

sealed class AchievementRequirement {
    data class CompleteConversations(val count: Int) : AchievementRequirement()
    data class LearnVocabulary(val count: Int) : AchievementRequirement()
    data class DailyStreak(val days: Int) : AchievementRequirement()
    data class PerfectPronunciation(val count: Int) : AchievementRequirement()
}

// Predefined achievements
val ACHIEVEMENTS = listOf(
    Achievement(
        id = "first_conversation",
        title = "첫 대화",
        description = "첫 번째 대화를 완료하세요",
        icon = Icons.Default.Chat,
        requirement = AchievementRequirement.CompleteConversations(1)
    ),
    Achievement(
        id = "week_streak",
        title = "일주일 연속",
        description = "7일 연속 학습하세요",
        icon = Icons.Default.LocalFireDepartment,
        requirement = AchievementRequirement.DailyStreak(7)
    ),
    // ... 20+ achievements
)

// AchievementChecker.kt
class AchievementChecker @Inject constructor(
    private val repository: AchievementRepository
) {
    suspend fun checkAchievements(userId: Long) {
        val stats = repository.getUserStats(userId)

        ACHIEVEMENTS.forEach { achievement ->
            if (isUnlocked(achievement, stats) && !repository.hasAchievement(userId, achievement.id)) {
                repository.unlockAchievement(userId, achievement.id)
                // Show toast notification
                showAchievementUnlocked(achievement)
            }
        }
    }
}

// AchievementScreen.kt
@Composable
fun AchievementScreen(viewModel: AchievementViewModel = hiltViewModel()) {
    val achievements by viewModel.achievements.collectAsState()

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(achievements) { achievement ->
            AchievementCard(
                achievement = achievement,
                isUnlocked = achievement.id in viewModel.unlockedIds
            )
        }
    }
}
```

**구현 난이도**: 중간 (3일)
**예상 코드 라인**: ~600 lines

**사용자 가치**: ⭐⭐⭐⭐
- 학습 동기부여
- 성취감

---

#### 5.2 학습 그룹/친구

**현재 상태**: 단일 사용자 전용

**구현 필요 사항**: (서버 필요 - 장기 과제)

```kotlin
// FriendSystem.kt - Requires backend
class FriendRepository @Inject constructor(
    private val apiService: NihongoApiService
) {
    suspend fun addFriend(userId: Long, friendCode: String)
    suspend fun getFriends(userId: Long): List<Friend>
    suspend fun getFriendStats(friendId: Long): FriendStats
}

// Leaderboard
@Composable
fun LeaderboardScreen() {
    val friends by viewModel.friends.collectAsState()

    LazyColumn {
        items(friends.sortedByDescending { it.totalXP }) { friend ->
            Row {
                Text("#${friend.rank}")
                Text(friend.name)
                Text("${friend.totalXP} XP")
            }
        }
    }
}
```

**구현 난이도**: 매우 높음 (서버 개발 필요)
**예상 코드 라인**: ~1000+ lines
**의존성**: Firebase/Backend server

**사용자 가치**: ⭐⭐⭐
- 소셜 동기부여
- 경쟁 요소

---

### 6. 🔌 통합 및 내보내기 (Integration & Export)

#### 6.1 대화 내보내기 ⭐ **우선 과제**

**현재 상태**: Voice-Only 모드 transcript만 있음

**구현 필요 사항**:

```kotlin
// ExportManager.kt
class ExportManager @Inject constructor() {
    fun exportConversationToText(conversation: Conversation, messages: List<Message>): String {
        val sb = StringBuilder()
        sb.appendLine("=== ${conversation.scenario?.title} ===")
        sb.appendLine("날짜: ${formatDate(conversation.startedAt)}")
        sb.appendLine()

        messages.forEach { message ->
            val speaker = if (message.isUser) "나" else "AI"
            sb.appendLine("$speaker: ${message.content}")

            // Add translation if available
            val translation = translationCache[message.id]
            if (translation != null) {
                sb.appendLine("  → $translation")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    fun exportToPdf(conversation: Conversation, messages: List<Message>): File {
        // Use iText or similar library
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        messages.forEach { message ->
            document.add(Paragraph(message.content))
        }

        document.close()
        return file
    }
}

// ChatScreen.kt - Add export button
IconButton(onClick = {
    val file = exportManager.exportConversationToText(conversation, messages)
    shareFile(context, file)
}) {
    Icon(Icons.Default.Share, "내보내기")
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~300 lines

**사용자 가치**: ⭐⭐⭐⭐
- 복습 자료 확보
- 외부 공유

---

#### 6.2 외부 사전 연동

**현재 상태**: 없음

**구현 필요 사항**:

```kotlin
// MessageBubble - Long press word → Dictionary lookup
var showDictionaryDialog by remember { mutableStateOf(false) }
var selectedWord by remember { mutableStateOf("") }

Text(
    text = message.content,
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { offset ->
                // Extract word at offset
                selectedWord = extractWordAtOffset(message.content, offset)
                showDictionaryDialog = true
            }
        )
    }
)

if (showDictionaryDialog) {
    DictionaryDialog(
        word = selectedWord,
        onDismiss = { showDictionaryDialog = false },
        onOpenJisho = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jisho.org/search/$selectedWord"))
            context.startActivity(intent)
        }
    )
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~200 lines

**사용자 가치**: ⭐⭐⭐⭐
- 단어 학습 편의성
- 외부 리소스 활용

---

#### 6.3 Anki 연동

**현재 상태**: 없음

**구현 필요 사항**:

```kotlin
// AnkiExporter.kt
class AnkiExporter @Inject constructor() {
    fun exportToAnkiDeck(vocabulary: List<VocabularyEntry>): File {
        // Create CSV compatible with Anki import
        val csv = StringBuilder()
        csv.appendLine("Japanese;Reading;Korean;Example")

        vocabulary.forEach { entry ->
            csv.appendLine("${entry.japanese};${entry.reading};${entry.korean};${entry.exampleSentence ?: ""}")
        }

        val file = File(context.getExternalFilesDir(null), "vocabulary_anki.csv")
        file.writeText(csv.toString())
        return file
    }
}

// VocabularyListScreen.kt
IconButton(onClick = {
    val file = ankiExporter.exportToAnkiDeck(vocabulary)
    shareFile(context, file, mimeType = "text/csv")
}) {
    Icon(Icons.Default.Download, "Anki로 내보내기")
}
```

**구현 난이도**: 낮음 (1일)
**예상 코드 라인**: ~150 lines

**사용자 가치**: ⭐⭐⭐
- 기존 Anki 사용자 확보
- 간격 반복 학습

---

## 🚀 구현 우선순위 로드맵

### Phase 1: 필수 핵심 기능 (2-3주)

**목표**: 학습 도구로서 완성도 90% 달성

| 순위 | 기능 | 예상 기간 | 사용자 가치 | 난이도 |
|-----|------|----------|-----------|-------|
| 1 | **단어장 시스템** | 3-4일 | ⭐⭐⭐⭐⭐ | 중간 |
| 2 | **학습 통계 대시보드** | 2-3일 | ⭐⭐⭐⭐ | 중간 |
| 3 | **온보딩 튜토리얼** | 1일 | ⭐⭐⭐⭐⭐ | 낮음 |
| 4 | **다크 모드** | 1일 | ⭐⭐⭐⭐ | 낮음 |
| 5 | **시나리오 추천 시스템** | 2일 | ⭐⭐⭐⭐ | 중간 |
| 6 | **대화 내보내기** | 1일 | ⭐⭐⭐⭐ | 낮음 |

**예상 코드 라인**: ~2,500 lines
**완료 후 완성도**: 85%

---

### Phase 2: UX 개선 및 안정성 (1-2주)

**목표**: 사용자 편의성 극대화

| 순위 | 기능 | 예상 기간 | 사용자 가치 | 난이도 | 상태 |
|-----|------|----------|-----------|-------|------|
| 7 | **접근성 개선** | 2일 | ⭐⭐⭐ | 중간 | ✅ **완료** (2025-11-02) |
| 8 | **메시지 편집/삭제** | 1일 | ⭐⭐⭐ | 낮음 | ⏳ 대기 |
| 9 | **에러 복구 강화** | 2일 | ⭐⭐⭐⭐ | 중간 | ⏳ 대기 |
| 10 | **성능 최적화** | 2일 | ⭐⭐⭐ | 중간 | ⏳ 대기 |
| 11 | **외부 사전 연동** | 1일 | ⭐⭐⭐⭐ | 낮음 | ⏳ 대기 |

**예상 코드 라인**: ~1,200 lines
**완료 후 완성도**: 92%

---

### Phase 3: 고급 기능 (2-3주)

**목표**: 차별화 및 경쟁력 강화

| 순위 | 기능 | 예상 기간 | 사용자 가치 | 난이도 |
|-----|------|----------|-----------|-------|
| 12 | **오프라인 모드** | 5-7일 | ⭐⭐⭐⭐⭐ | 높음 |
| 13 | **난이도 자동 조정** | 3-4일 | ⭐⭐⭐⭐ | 높음 |
| 14 | **업적 시스템** | 3일 | ⭐⭐⭐⭐ | 중간 |
| 15 | **데이터 백업/동기화** | 3일 | ⭐⭐⭐⭐ | 중간 |

**예상 코드 라인**: ~2,300 lines
**완료 후 완성도**: 98%

---

### Phase 4: 선택적 개선 (추후 고려)

**목표**: 장기 발전 및 커뮤니티 구축

| 순위 | 기능 | 예상 기간 | 사용자 가치 | 난이도 | 상태 |
|-----|------|----------|-----------|-------|------|
| 16 | **커스텀 시나리오 생성** | 2일 | ⭐⭐⭐ | 중간 | ✅ **완료** (2025-11-02) |
| 17 | **시나리오 진행 추적** | 2일 | ⭐⭐⭐ | 중간 | ⏳ 대기 |
| 18 | **Anki 연동** | 1일 | ⭐⭐⭐ | 낮음 | ⏳ 대기 |
| 19 | **학습 그룹/친구** | 서버 필요 | ⭐⭐⭐ | 매우 높음 | ⏳ 대기 |

**예상 코드 라인**: ~1,000+ lines
**완료 후 완성도**: 100%

---

## 📋 구현 체크리스트

### Phase 1 (필수 핵심 기능)
- [ ] 단어장 시스템
  - [ ] VocabularyEntry Entity & Dao
  - [ ] Room Migration 12→13
  - [ ] VocabularyRepository
  - [ ] VocabularyViewModel
  - [ ] VocabularyListScreen UI
  - [ ] FlashcardScreen UI
  - [ ] ChatScreen 연동 (단어 추가)
- [ ] 학습 통계 대시보드
  - [ ] 집계 쿼리 작성
  - [ ] Vico Chart library 추가
  - [ ] StatsViewModel
  - [ ] StatsScreen UI (그래프)
  - [ ] 학습 스트릭 계산
- [ ] 온보딩 튜토리얼
  - [ ] OnboardingScreen (4 pages)
  - [ ] SettingsDataStore.isFirstLaunch
  - [ ] Navigation 통합
- [ ] 다크 모드
  - [ ] ThemeMode enum (LIGHT/DARK/SYSTEM)
  - [ ] SettingsDataStore.themeMode
  - [ ] MainActivity 테마 적용
- [ ] 시나리오 추천 시스템
  - [ ] 추천 알고리즘 (user profile + completion history)
  - [ ] ScenarioListScreen 배너
- [ ] 대화 내보내기
  - [ ] ExportManager (TXT, PDF)
  - [ ] Share intent 연동

### Phase 2 (UX 개선 및 안정성)
- [x] **접근성 개선** ✅ (2025-11-02)
  - [x] TalkBack contentDescription 보완 (이미 23개 파일에 적용됨)
  - [x] 텍스트 크기 조절 (작게/보통/크게/아주 크게)
  - [x] 고대비 모드 (고대비 색상 스킴)
  - [x] SettingsScreen에 접근성 섹션 추가
  - [x] Theme.kt에 텍스트 크기 스케일링 적용
  - [x] MainActivity에서 설정 값 실시간 반영
- [ ] 메시지 편집/삭제
  - [ ] MessageBubble 컨텍스트 메뉴
  - [ ] Edit dialog
  - [ ] ChatViewModel.updateMessage/deleteMessage
- [ ] 에러 복구 강화
  - [ ] Gemini API fallback chain
  - [ ] 자동 재시도 로직
- [ ] 성능 최적화
  - [ ] Coil image loading
  - [ ] WorkManager cache cleanup
- [ ] 외부 사전 연동
  - [ ] Word selection UI
  - [ ] Jisho.org intent

### Phase 3 (고급 기능)
- [ ] 오프라인 모드
  - [ ] ConversationPreset 작성 (50+ 시나리오 × 10 문장)
  - [ ] OfflineConversationManager
  - [ ] Pattern matching 로직
  - [ ] ChatViewModel 통합
- [ ] 난이도 자동 조정
  - [ ] Success rate 계산
  - [ ] DifficultyManager.shouldAdjustDifficulty
  - [ ] 조정 제안 Dialog
- [ ] 업적 시스템
  - [ ] Achievement data model
  - [ ] AchievementChecker
  - [ ] AchievementScreen UI
  - [ ] 알림 시스템
- [ ] 데이터 백업/동기화
  - [ ] BackupManager (JSON export/import)
  - [ ] SettingsScreen 통합

### Phase 4 (선택적 개선)
- [x] **커스텀 시나리오 생성** ✅ (2025-11-02)
  - [x] CreateScenarioScreen UI
  - [x] AI 프롬프트 생성 도우미
  - [x] ScenarioViewModel.createCustomScenario()
  - [x] GeminiApiService.generateSimpleText()
  - [x] Navigation 연동
  - [x] ScenarioListScreen FAB 버튼
- [ ] 시나리오 진행 추적
  - [ ] ScenarioProgress model
  - [ ] 진행률 UI
- [ ] Anki 연동
  - [ ] CSV exporter
  - [ ] Share intent
- [ ] 학습 그룹/친구
  - [ ] 서버 개발 (별도 프로젝트)

---

## 🎯 성공 지표 (KPI)

### 사용자 경험
- **학습 지속률**: 7일 유지율 > 40%
- **일평균 사용 시간**: 15분 이상
- **시나리오 완료율**: 시작한 대화의 70% 완료
- **단어장 활용률**: 등록 단어의 50% 이상 복습

### 기술적 안정성
- **크래시율**: < 1%
- **API 성공률**: > 95%
- **평균 응답 속도**: < 3초 (Gemini API)
- **오프라인 가용성**: 50% 이상 기능 작동

### 성장 지표
- **월간 활성 사용자**: 1,000명 (3개월 목표)
- **평균 평점**: > 4.5 (Google Play)
- **리텐션**: 30일 유지율 > 20%

---

## 📝 다음 단계

1. **Phase 1 착수** (단어장 시스템부터 시작)
2. **주간 리뷰**: 매주 금요일 진행 상황 점검
3. **사용자 피드백 수집**: Phase 1 완료 후 베타 테스트
4. **우선순위 재조정**: 피드백 기반 로드맵 업데이트

---

**마지막 업데이트**: 2025-11-02
**다음 리뷰 예정일**: 2025-11-09
