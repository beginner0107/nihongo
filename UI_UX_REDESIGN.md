# UI/UX 대규모 개편 계획서

> **목표**: 게임화, 데이터 시각화, 소셜 기능을 통한 학습 동기 부여 및 사용자 경험 혁신

## 🎉 진행 상황 (2025-11-12 업데이트)

### 완료된 Phase
- ✅ **Phase 2: 퀘스트/미션 시스템** (100% 완료)
  - Part 1: Backend 구현 (Database, Repository, Domain Models)
  - Part 2: UI 구현 (QuestCard, QuestSection, HomeScreen 통합)
  - Part 3: 자동 진행률 추적 (ChatViewModel 통합, 자동 완료 감지)

### 진행 중인 Phase
- 🚧 **Phase 1: 홈 대시보드 혁신** (부분 완료)
  - ✅ TodayLearningCard (진행률 바, 동기부여 메시지)
  - ✅ RecommendedScenariosSection (카드 크기 확대)
  - ✅ RecentScenariosSection (최근 학습 시나리오)
  - ⏳ LearningProgressCard (대기 중)
  - ⏳ StreakCard (대기 중)

### 다음 예정 Phase
- 📅 Phase 6: 하단 네비게이션 바 (HomeScreen 완성 후)
- 📅 Phase 3: 리더보드 & Achievement
- 📅 Phase 4: 학습 통계 고도화

---

## 📊 참고 디자인 분석

### 벤치마크 앱 특징
1. **대시보드 중심 설계**: 학습 진행률을 한눈에 파악 (78%, 완료/예정 수치화)
2. **게임화 요소**: 퀘스트, 포인트, 레벨, 리더보드
3. **데이터 시각화**: 곡선 그래프, 바 차트, 히트맵
4. **마이크로 애니메이션**: 부드러운 전환, 인터랙티브 피드백
5. **카드 UI**: 둥근 모서리, 그라데이션, 색상 코딩
6. **소셜 요소**: 리더보드, Achievement, 경쟁 시스템

---

## 🎯 개편 목표

### Before (현재 상태)
- ScenarioListScreen = 단순 시나리오 목록
- 학습 동기 부여 요소 부족
- 진행 상황 파악 어려움 (Stats 화면에만 존재)
- 정적인 UI, 애니메이션 최소
- 소셜/경쟁 요소 없음

### After (목표 상태)
- **홈 대시보드**: 학습 진행률, 스트릭, 일일 목표를 상단에 배치
- **퀘스트 시스템**: 일일 미션으로 학습 동기 부여 (30포인트 보상)
- **리더보드**: 주간 순위 경쟁으로 지속성 향상
- **애니메이션**: 부드러운 전환, 포인트 획득 효과
- **하단 네비게이션**: 주요 화면 빠른 접근 (홈/학습/복습/통계/프로필)

---

## 📂 Phase별 구현 계획

### Phase 1: 홈 대시보드 혁신 🏠 **최우선**
**예상 시간**: 6시간
**우선순위**: 🔥 Critical

#### 목표
ScenarioListScreen을 단순 목록에서 학습 대시보드로 전환

#### 구현 내용

##### 1.1 학습 진행률 요약 카드
**파일**: `presentation/dashboard/LearningProgressCard.kt` (신규)

```kotlin
@Composable
fun LearningProgressCard(
    completedCount: Int,
    inProgressCount: Int,
    favoriteCount: Int,
    averageProgress: Float,  // 0.0 ~ 1.0
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 메트릭 박스들
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricBox(
                        icon = Icons.Default.CheckCircle,
                        count = completedCount,
                        label = "완료",
                        color = Color(0xFF4CAF50)  // Green
                    )

                    MetricBox(
                        icon = Icons.Default.PlayCircle,
                        count = inProgressCount,
                        label = "진행 중",
                        color = Color(0xFF2196F3)  // Blue
                    )
                }

                MetricBox(
                    icon = Icons.Default.Star,
                    count = favoriteCount,
                    label = "즐겨찾기",
                    color = Color(0xFFFFD700)  // Gold
                )
            }

            // 우측: 진행률 원형 차트
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = averageProgress,
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = "${(averageProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Column {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

##### 1.2 학습 스트릭 카드
**파일**: `presentation/dashboard/StreakCard.kt` (신규)

```kotlin
@Composable
fun StreakCard(
    currentStreak: Int,
    bestStreak: Int,
    lastStudyDate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "연속 학습",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "마지막 학습: $lastStudyDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "스트릭",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFFFF6B35)  // Orange flame color
                )
            }

            // 현재 스트릭 (불 이모지)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥".repeat(currentStreak.coerceAtMost(10)),
                    fontSize = 24.sp
                )
            }

            // 통계
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentStreak일",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "현재",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$bestStreak일",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "최고 기록",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
```

##### 1.3 일일 목표 카드
**파일**: `presentation/dashboard/DailyGoalCard.kt` (신규)

```kotlin
@Composable
fun DailyGoalCard(
    currentCount: Int,
    targetCount: Int,
    remainingHours: Int,
    remainingMinutes: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentCount.toFloat() / targetCount).coerceAtMost(1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "오늘의 목표",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$currentCount / $targetCount 메시지",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // 진행률 바
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (progress >= 1f) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.primary,
            )

            // 남은 시간
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "시간",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "남은 시간: ${remainingHours}시간 ${remainingMinutes}분",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 완료 시 표시
                if (progress >= 1f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "완료",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "목표 달성!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
```

##### 1.4 ScenarioListScreen 통합
**파일**: `presentation/scenario/ScenarioListScreen.kt` (수정)

```kotlin
// 기존 LazyColumn에 대시보드 카드들 추가
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState
) {
    // 1. 학습 진행률 카드
    item {
        LearningProgressCard(
            completedCount = uiState.completedCount,
            inProgressCount = uiState.inProgressCount,
            favoriteCount = uiState.favoriteCount,
            averageProgress = uiState.averageProgress
        )
    }

    // 2. 스트릭 카드
    item {
        StreakCard(
            currentStreak = uiState.currentStreak,
            bestStreak = uiState.bestStreak,
            lastStudyDate = uiState.lastStudyDate
        )
    }

    // 3. 일일 목표 카드
    item {
        DailyGoalCard(
            currentCount = uiState.todayMessageCount,
            targetCount = uiState.dailyGoal,
            remainingHours = uiState.remainingHours,
            remainingMinutes = uiState.remainingMinutes
        )
    }

    // 4. 퀘스트 섹션 (Phase 2에서 추가)
    // ...

    // 5. 섹션 헤더
    item {
        Text(
            text = "시나리오",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
    }

    // 6. 기존 시나리오 목록
    items(uiState.scenarios, key = { it.id }) { scenario ->
        ScenarioCard(...)
    }
}
```

##### 1.5 ViewModel 업데이트
**파일**: `presentation/scenario/ScenarioViewModel.kt` (수정)

```kotlin
data class ScenarioUiState(
    // 기존 필드들...
    val scenarios: List<Scenario> = emptyList(),
    val searchQuery: String = "",
    val selectedDifficulties: Set<Int> = emptySet(),

    // 새로 추가: 대시보드 데이터
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val favoriteCount: Int = 0,
    val averageProgress: Float = 0f,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastStudyDate: String = "",
    val todayMessageCount: Int = 0,
    val dailyGoal: Int = 10,
    val remainingHours: Int = 0,
    val remainingMinutes: Int = 0
)

class ScenarioViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // 완료/진행 중/즐겨찾기 계산
            val allScenarios = repository.getAllScenarios().first()
            val completedScenarios = repository.getCompletedScenarios(currentUserId).first()
            val inProgressScenarios = repository.getInProgressScenarios(currentUserId).first()
            val favoriteScenarios = allScenarios.filter { it.isFavorite }

            // 평균 진행률 계산
            val totalProgress = completedScenarios.size.toFloat() / allScenarios.size

            // 스트릭 계산
            val streak = calculateStreak()

            // 오늘 메시지 수
            val todayMessages = repository.getTodayMessageCount(currentUserId).first()

            // 남은 시간 (자정까지)
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val duration = Duration.between(now, midnight)

            _uiState.update {
                it.copy(
                    completedCount = completedScenarios.size,
                    inProgressCount = inProgressScenarios.size,
                    favoriteCount = favoriteScenarios.size,
                    averageProgress = totalProgress,
                    currentStreak = streak.current,
                    bestStreak = streak.best,
                    lastStudyDate = streak.lastStudyDate,
                    todayMessageCount = todayMessages,
                    remainingHours = duration.toHours().toInt(),
                    remainingMinutes = (duration.toMinutes() % 60).toInt()
                )
            }
        }
    }

    private suspend fun calculateStreak(): StreakData {
        // TODO: Repository에 학습 기록 조회 메서드 추가
        // 연속 학습 일수 계산 로직
        return StreakData(current = 7, best = 15, lastStudyDate = "오늘 15:23")
    }
}

data class StreakData(
    val current: Int,
    val best: Int,
    val lastStudyDate: String
)
```

##### 1.6 Repository 업데이트
**파일**: `data/repository/ConversationRepository.kt` (수정)

```kotlin
interface ConversationRepository {
    // 기존 메서드들...

    // 새로 추가: 대시보드 통계
    fun getCompletedScenarios(userId: Long): Flow<List<Scenario>>
    fun getInProgressScenarios(userId: Long): Flow<List<Scenario>>
    fun getTodayMessageCount(userId: Long): Flow<Int>
    fun getStudyStreak(userId: Long): Flow<StreakData>
}

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val scenarioDao: ScenarioDao
) : ConversationRepository {

    override fun getCompletedScenarios(userId: Long): Flow<List<Scenario>> {
        // Conversation 테이블에서 isCompleted = true인 시나리오 조회
        return conversationDao.getCompletedConversations(userId)
            .map { conversations ->
                conversations.mapNotNull { conversation ->
                    scenarioDao.getScenarioById(conversation.scenarioId)
                }.distinctBy { it.id }
            }
    }

    override fun getInProgressScenarios(userId: Long): Flow<List<Scenario>> {
        return conversationDao.getInProgressConversations(userId)
            .map { conversations ->
                conversations.mapNotNull { conversation ->
                    scenarioDao.getScenarioById(conversation.scenarioId)
                }.distinctBy { it.id }
            }
    }

    override fun getTodayMessageCount(userId: Long): Flow<Int> {
        val startOfDay = LocalDate.now().atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return messageDao.getMessageCountSince(userId, startOfDay)
    }

    override fun getStudyStreak(userId: Long): Flow<StreakData> {
        // TODO: 학습 기록 테이블 필요 (study_log)
        // 매일 학습 여부를 기록하고 연속 일수 계산
        return flow {
            emit(StreakData(current = 0, best = 0, lastStudyDate = ""))
        }
    }
}
```

##### 1.7 DAO 업데이트
**파일**: `data/local/dao/ConversationDao.kt` (수정)

```kotlin
@Dao
interface ConversationDao {
    // 기존 메서드들...

    @Query("""
        SELECT * FROM conversations
        WHERE userId = :userId AND isCompleted = 1
        ORDER BY updatedAt DESC
    """)
    fun getCompletedConversations(userId: Long): Flow<List<ConversationEntity>>

    @Query("""
        SELECT * FROM conversations
        WHERE userId = :userId AND isCompleted = 0
        ORDER BY updatedAt DESC
    """)
    fun getInProgressConversations(userId: Long): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    // 기존 메서드들...

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE conversationId IN (
            SELECT id FROM conversations WHERE userId = :userId
        ) AND timestamp >= :startTimestamp
    """)
    fun getMessageCountSince(userId: Long, startTimestamp: Long): Flow<Int>
}
```

#### 완료 조건
- [ ] LearningProgressCard 컴포넌트 작동
- [ ] StreakCard 컴포넌트 작동
- [ ] DailyGoalCard 컴포넌트 작동
- [ ] ScenarioListScreen에 대시보드 카드 통합
- [ ] ViewModel에서 대시보드 데이터 로드
- [ ] Repository/DAO에 통계 쿼리 추가
- [ ] 실시간 데이터 업데이트 확인

---

### Phase 2: 퀘스트/미션 시스템 🎮 **최우선**
**예상 시간**: 8시간
**우선순위**: 🔥 Critical

#### 목표
일일 퀘스트로 학습 동기 부여 (게임화)

#### 구현 내용

##### 2.1 Database Schema
**파일**: `data/local/entity/DailyQuestEntity.kt` (신규)

```kotlin
@Entity(
    tableName = "daily_quests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["expiresAt"]),
        Index(value = ["isCompleted"])
    ]
)
data class DailyQuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,  // "편의점에서 대화 완료하기"
    val description: String,
    val questType: String,  // MESSAGE_COUNT, SCENARIO_COMPLETE, etc.
    val targetValue: Int,  // 10 메시지, 1 시나리오
    val currentValue: Int = 0,
    val rewardPoints: Int,  // 30 포인트
    val expiresAt: Long,  // 자정 (Epoch milliseconds)
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class QuestType {
    MESSAGE_COUNT,        // 10개 메시지 보내기
    SCENARIO_COMPLETE,    // 시나리오 1개 완료
    VOICE_ONLY_SESSION,   // 음성 전용 모드로 대화
    VOCABULARY_REVIEW,    // 플래시카드 20개 복습
    PRONUNCIATION_PRACTICE, // 발음 연습 5회
    GRAMMAR_ANALYSIS,     // 문법 분석 3회 사용
    CONVERSATION_LENGTH,  // 15분 이상 대화
    NEW_SCENARIO          // 새로운 시나리오 시작
}
```

**파일**: `data/local/entity/UserPointsEntity.kt` (신규)

```kotlin
@Entity(tableName = "user_points")
data class UserPointsEntity(
    @PrimaryKey val userId: Long,
    val totalPoints: Int = 0,
    val todayPoints: Int = 0,
    val weeklyPoints: Int = 0,
    val level: Int = 1,  // 레벨 (100포인트 = 1레벨)
    val weeklyRank: Int? = null,
    val lastResetDate: Long = System.currentTimeMillis()
)
```

##### 2.2 DAO
**파일**: `data/local/dao/DailyQuestDao.kt` (신규)

```kotlin
@Dao
interface DailyQuestDao {
    @Query("SELECT * FROM daily_quests WHERE userId = :userId AND expiresAt > :now ORDER BY isCompleted ASC, rewardPoints DESC")
    fun getActiveQuests(userId: Long, now: Long = System.currentTimeMillis()): Flow<List<DailyQuestEntity>>

    @Query("SELECT * FROM daily_quests WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC LIMIT 10")
    fun getCompletedQuests(userId: Long): Flow<List<DailyQuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: DailyQuestEntity): Long

    @Update
    suspend fun updateQuest(quest: DailyQuestEntity)

    @Query("UPDATE daily_quests SET currentValue = :value WHERE id = :questId")
    suspend fun updateQuestProgress(questId: Long, value: Int)

    @Query("UPDATE daily_quests SET isCompleted = 1, completedAt = :completedAt WHERE id = :questId")
    suspend fun completeQuest(questId: Long, completedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_quests WHERE expiresAt < :now")
    suspend fun deleteExpiredQuests(now: Long = System.currentTimeMillis())
}

@Dao
interface UserPointsDao {
    @Query("SELECT * FROM user_points WHERE userId = :userId")
    fun getUserPoints(userId: Long): Flow<UserPointsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPoints(userPoints: UserPointsEntity)

    @Query("UPDATE user_points SET totalPoints = totalPoints + :points, todayPoints = todayPoints + :points, weeklyPoints = weeklyPoints + :points WHERE userId = :userId")
    suspend fun addPoints(userId: Long, points: Int)

    @Query("UPDATE user_points SET level = :level WHERE userId = :userId")
    suspend fun updateLevel(userId: Long, level: Int)

    @Query("UPDATE user_points SET todayPoints = 0, lastResetDate = :resetDate WHERE userId = :userId")
    suspend fun resetDailyPoints(userId: Long, resetDate: Long)

    @Query("UPDATE user_points SET weeklyPoints = 0 WHERE userId = :userId")
    suspend fun resetWeeklyPoints(userId: Long)
}
```

##### 2.3 Domain Model
**파일**: `domain/model/Quest.kt` (신규)

```kotlin
data class Quest(
    val id: Long,
    val title: String,
    val description: String,
    val type: QuestType,
    val targetValue: Int,
    val currentValue: Int,
    val rewardPoints: Int,
    val expiresAt: Long,
    val isCompleted: Boolean,
    val progress: Float = currentValue.toFloat() / targetValue
)

data class UserPoints(
    val userId: Long,
    val totalPoints: Int,
    val todayPoints: Int,
    val weeklyPoints: Int,
    val level: Int,
    val pointsToNextLevel: Int,
    val weeklyRank: Int?
)
```

##### 2.4 Repository
**파일**: `data/repository/QuestRepository.kt` (신규)

```kotlin
interface QuestRepository {
    fun getActiveQuests(userId: Long): Flow<List<Quest>>
    fun getUserPoints(userId: Long): Flow<UserPoints?>
    suspend fun generateDailyQuests(userId: Long)
    suspend fun updateQuestProgress(questId: Long, value: Int)
    suspend fun completeQuest(questId: Long): Int  // Returns reward points
    suspend fun addPoints(userId: Long, points: Int)
}

class QuestRepositoryImpl @Inject constructor(
    private val questDao: DailyQuestDao,
    private val pointsDao: UserPointsDao
) : QuestRepository {

    override fun getActiveQuests(userId: Long): Flow<List<Quest>> {
        return questDao.getActiveQuests(userId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override fun getUserPoints(userId: Long): Flow<UserPoints?> {
        return pointsDao.getUserPoints(userId)
            .map { entity ->
                entity?.toDomainModel()
            }
    }

    override suspend fun generateDailyQuests(userId: Long) {
        // 자정 (다음날 00:00:00)
        val midnight = LocalDate.now().plusDays(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 퀘스트 템플릿
        val questTemplates = listOf(
            DailyQuestEntity(
                userId = userId,
                title = "대화 연습하기",
                description = "AI와 10개 메시지 주고받기",
                questType = QuestType.MESSAGE_COUNT.name,
                targetValue = 10,
                rewardPoints = 30,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "시나리오 완주",
                description = "시나리오 1개 완료하기",
                questType = QuestType.SCENARIO_COMPLETE.name,
                targetValue = 1,
                rewardPoints = 50,
                expiresAt = midnight
            ),
            DailyQuestEntity(
                userId = userId,
                title = "음성 전용 대화",
                description = "음성만으로 대화 완료",
                questType = QuestType.VOICE_ONLY_SESSION.name,
                targetValue = 1,
                rewardPoints = 40,
                expiresAt = midnight
            )
        )

        // 랜덤하게 3개 선택
        questTemplates.shuffled().take(3).forEach { quest ->
            questDao.insertQuest(quest)
        }
    }

    override suspend fun updateQuestProgress(questId: Long, value: Int) {
        questDao.updateQuestProgress(questId, value)

        // 목표 달성 시 자동 완료
        val quest = questDao.getActiveQuests(0).first().find { it.id == questId }
        if (quest != null && value >= quest.targetValue) {
            completeQuest(questId)
        }
    }

    override suspend fun completeQuest(questId: Long): Int {
        val quest = questDao.getActiveQuests(0).first().find { it.id == questId }
        if (quest != null && !quest.isCompleted) {
            questDao.completeQuest(questId)
            addPoints(quest.userId, quest.rewardPoints)
            return quest.rewardPoints
        }
        return 0
    }

    override suspend fun addPoints(userId: Long, points: Int) {
        pointsDao.addPoints(userId, points)

        // 레벨업 체크
        val userPoints = pointsDao.getUserPoints(userId).first()
        if (userPoints != null) {
            val newLevel = (userPoints.totalPoints / 100) + 1
            if (newLevel > userPoints.level) {
                pointsDao.updateLevel(userId, newLevel)
            }
        }
    }
}

// Extension functions
private fun DailyQuestEntity.toDomainModel() = Quest(
    id = id,
    title = title,
    description = description,
    type = QuestType.valueOf(questType),
    targetValue = targetValue,
    currentValue = currentValue,
    rewardPoints = rewardPoints,
    expiresAt = expiresAt,
    isCompleted = isCompleted
)

private fun UserPointsEntity.toDomainModel() = UserPoints(
    userId = userId,
    totalPoints = totalPoints,
    todayPoints = todayPoints,
    weeklyPoints = weeklyPoints,
    level = level,
    pointsToNextLevel = (level * 100) - (totalPoints % 100),
    weeklyRank = weeklyRank
)
```

##### 2.5 UI Components
**파일**: `presentation/quest/QuestCard.kt` (신규)

```kotlin
@Composable
fun QuestCard(
    quest: Quest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측: 아이콘 + 내용
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 퀘스트 타입 아이콘
                Icon(
                    imageVector = when(quest.type) {
                        QuestType.MESSAGE_COUNT -> Icons.Default.Message
                        QuestType.SCENARIO_COMPLETE -> Icons.Default.CheckCircle
                        QuestType.VOICE_ONLY_SESSION -> Icons.Default.Mic
                        QuestType.VOCABULARY_REVIEW -> Icons.Default.Book
                        QuestType.PRONUNCIATION_PRACTICE -> Icons.Default.RecordVoiceOver
                        QuestType.GRAMMAR_ANALYSIS -> Icons.Default.Analytics
                        QuestType.CONVERSATION_LENGTH -> Icons.Default.Timer
                        QuestType.NEW_SCENARIO -> Icons.Default.AddCircle
                    },
                    contentDescription = quest.type.name,
                    modifier = Modifier.size(40.dp),
                    tint = if (quest.isCompleted) Color(0xFFFFD700)
                           else MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 진행률 바
                    LinearProgressIndicator(
                        progress = { quest.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (quest.isCompleted) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text = "${quest.currentValue} / ${quest.targetValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 우측: 보상
            Box(
                modifier = Modifier
                    .background(
                        if (quest.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (quest.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "완료",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "포인트",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (quest.isCompleted) "완료!"
                               else "${quest.rewardPoints}P",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (quest.isCompleted) Color(0xFF4CAF50)
                                else Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}
```

**파일**: `presentation/quest/QuestSection.kt` (신규)

```kotlin
@Composable
fun QuestSection(
    quests: List<Quest>,
    userPoints: UserPoints?,
    onQuestClick: (Quest) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "오늘의 퀘스트",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (userPoints != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Lv.${userPoints.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${userPoints.totalPoints}P",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(onClick = { onViewAllClick() }) {
                Text("전체 보기")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 퀘스트 카드들 (최대 3개)
        quests.take(3).forEach { quest ->
            QuestCard(
                quest = quest,
                onClick = { onQuestClick(quest) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
```

##### 2.6 ViewModel
**파일**: `presentation/quest/QuestViewModel.kt` (신규)

```kotlin
@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val currentUserId = userPreferences.currentUserId

    val quests = questRepository.getActiveQuests(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPoints = questRepository.getUserPoints(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        checkAndGenerateQuests()
    }

    private fun checkAndGenerateQuests() {
        viewModelScope.launch {
            val activeQuests = questRepository.getActiveQuests(currentUserId).first()
            if (activeQuests.isEmpty()) {
                questRepository.generateDailyQuests(currentUserId)
            }
        }
    }

    fun updateQuestProgress(questId: Long, value: Int) {
        viewModelScope.launch {
            questRepository.updateQuestProgress(questId, value)
        }
    }

    fun completeQuest(questId: Long) {
        viewModelScope.launch {
            val points = questRepository.completeQuest(questId)
            if (points > 0) {
                _events.send(QuestEvent.QuestCompleted(points))
            }
        }
    }

    private val _events = Channel<QuestEvent>()
    val events = _events.receiveAsFlow()
}

sealed class QuestEvent {
    data class QuestCompleted(val points: Int) : QuestEvent()
}
```

##### 2.7 ChatViewModel 통합 (자동 진행률 업데이트)
**파일**: `presentation/chat/ChatViewModel.kt` (수정)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // ...
    private val questRepository: QuestRepository
) : ViewModel() {

    fun sendMessage(text: String) {
        viewModelScope.launch {
            // 기존 메시지 전송 로직...

            // 퀘스트 진행률 업데이트
            updateQuestProgress()
        }
    }

    private suspend fun updateQuestProgress() {
        val activeQuests = questRepository.getActiveQuests(currentUserId).first()

        activeQuests.forEach { quest ->
            when (quest.type) {
                QuestType.MESSAGE_COUNT -> {
                    val newValue = quest.currentValue + 1
                    questRepository.updateQuestProgress(quest.id, newValue)
                }
                QuestType.CONVERSATION_LENGTH -> {
                    // 대화 시간 계산
                    val conversationDuration = calculateDuration()
                    if (conversationDuration >= 15) {
                        questRepository.updateQuestProgress(quest.id, 1)
                    }
                }
                // 다른 퀘스트 타입 처리...
                else -> {}
            }
        }
    }
}
```

##### 2.8 ScenarioListScreen 통합
**파일**: `presentation/scenario/ScenarioListScreen.kt` (수정)

```kotlin
@Composable
fun ScenarioListScreen(
    // ...
    questViewModel: QuestViewModel = hiltViewModel()
) {
    val quests by questViewModel.quests.collectAsState()
    val userPoints by questViewModel.userPoints.collectAsState()

    LazyColumn {
        // ... 대시보드 카드들 ...

        // 퀘스트 섹션 추가
        item {
            QuestSection(
                quests = quests,
                userPoints = userPoints,
                onQuestClick = { quest ->
                    // 퀘스트 상세 또는 관련 화면으로 이동
                },
                onViewAllClick = {
                    navController.navigate("quest_list")
                }
            )
        }

        // ... 시나리오 목록 ...
    }
}
```

##### 2.9 Database Migration
**파일**: `data/local/NihongoDatabase.kt` (수정)

```kotlin
@Database(
    entities = [
        // 기존 엔티티들...
        DailyQuestEntity::class,
        UserPointsEntity::class
    ],
    version = 13,  // 12 → 13
    exportSchema = true
)
abstract class NihongoDatabase : RoomDatabase() {
    // ...
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun userPointsDao(): UserPointsDao
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // daily_quests 테이블 생성
        database.execSQL("""
            CREATE TABLE daily_quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                questType TEXT NOT NULL,
                targetValue INTEGER NOT NULL,
                currentValue INTEGER NOT NULL DEFAULT 0,
                rewardPoints INTEGER NOT NULL,
                expiresAt INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                createdAt INTEGER NOT NULL
            )
        """)

        database.execSQL("CREATE INDEX index_daily_quests_userId ON daily_quests(userId)")
        database.execSQL("CREATE INDEX index_daily_quests_expiresAt ON daily_quests(expiresAt)")
        database.execSQL("CREATE INDEX index_daily_quests_isCompleted ON daily_quests(isCompleted)")

        // user_points 테이블 생성
        database.execSQL("""
            CREATE TABLE user_points (
                userId INTEGER PRIMARY KEY NOT NULL,
                totalPoints INTEGER NOT NULL DEFAULT 0,
                todayPoints INTEGER NOT NULL DEFAULT 0,
                weeklyPoints INTEGER NOT NULL DEFAULT 0,
                level INTEGER NOT NULL DEFAULT 1,
                weeklyRank INTEGER,
                lastResetDate INTEGER NOT NULL
            )
        """)
    }
}
```

##### 2.10 포인트 획득 애니메이션
**파일**: `presentation/quest/PointsEarnedAnimation.kt` (신규)

```kotlin
@Composable
fun PointsEarnedAnimation(
    points: Int,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(2000)
        isVisible = false
        delay(300)
        onComplete()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut() + scaleOut() + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFFFD700)
                )

                Text(
                    text = "+$points 포인트",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "퀘스트 완료!",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ChatScreen에서 사용
@Composable
fun ChatScreen(
    // ...
    questViewModel: QuestViewModel = hiltViewModel()
) {
    var showPointsAnimation by remember { mutableStateOf(false) }
    var earnedPoints by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        questViewModel.events.collect { event ->
            when (event) {
                is QuestEvent.QuestCompleted -> {
                    earnedPoints = event.points
                    showPointsAnimation = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 기존 채팅 UI...

        // 포인트 획득 애니메이션
        if (showPointsAnimation) {
            PointsEarnedAnimation(
                points = earnedPoints,
                onComplete = { showPointsAnimation = false }
            )
        }
    }
}
```

#### 완료 조건
- [x] DailyQuestEntity, UserPointsEntity 테이블 생성 ✅ (2025-11-12)
- [x] QuestRepository, QuestViewModel 구현 ✅ (2025-11-12)
- [x] QuestCard, QuestSection UI 작동 ✅ (2025-11-12)
- [x] HomeScreen에 퀘스트 섹션 통합 ✅ (2025-11-12)
- [x] ChatViewModel에서 자동 진행률 업데이트 ✅ (2025-11-12)
- [x] 퀘스트 완료 시 포인트 자동 지급 ✅ (2025-11-12)
- [x] 레벨업 시스템 작동 ✅ (2025-11-12)
- [x] 만료된 퀘스트 자동 삭제 ✅ (2025-11-12)
- [x] 퀘스트 완료 자동 감지 및 다이얼로그 표시 ✅ (2025-11-12)
- [ ] 포인트 획득 애니메이션 작동 (Phase 7에서 구현 예정)

#### 구현 상세 (2025-11-12 완료)

##### Phase 2 Part 1: Backend 구현 ✅
**커밋**: `3581c6d` - "feat: Implement Quest/Mission System UI (Phase 2 Part 2)"

- **Database Schema (Migration 15→16)**:
  - `daily_quests` 테이블 (12개 컬럼, 3개 인덱스)
  - `user_points` 테이블 (7개 컬럼)
  - QuestType enum: MESSAGE_COUNT, SCENARIO_COMPLETE, VOICE_ONLY_SESSION, VOCABULARY_REVIEW, PRONUNCIATION_PRACTICE, GRAMMAR_ANALYSIS, NEW_SCENARIO

- **Repository Layer**:
  - `QuestRepository`: 퀘스트 CRUD, 진행률 업데이트, 포인트 관리
  - `incrementQuestProgressByType()`: 타입별 자동 업데이트 헬퍼 메서드
  - 자동 완료 로직: `currentValue >= targetValue` 시 자동 완료
  - 레벨업 로직: 100포인트 = 1레벨

- **Domain Models**:
  - `Quest`: 진행률 계산 (`progress = currentValue / targetValue`)
  - `UserPoints`: 다음 레벨까지 포인트 계산

##### Phase 2 Part 2: UI 구현 ✅
**커밋**: `3581c6d` - "feat: Implement Quest/Mission System UI (Phase 2 Part 2)"

- **QuestCard.kt**:
  - 타입별 아이콘 (Message, CheckCircle, Mic 등)
  - 진행률 바 (LinearProgressIndicator 6dp height)
  - 보상 배지 (완료: 초록색, 미완료: 골드)
  - `currentValue / targetValue` 진행 상태 표시

- **QuestSection.kt**:
  - 헤더: "오늘의 퀘스트" + 레벨/포인트 표시
  - 상위 3개 퀘스트만 표시
  - 전체 보기 버튼 (향후 확장)

- **QuestViewModel.kt**:
  - `StateFlow<List<Quest>>`: 실시간 퀘스트 목록
  - `StateFlow<UserPoints?>`: 실시간 포인트/레벨
  - 자동 퀘스트 생성 (일일 3개, 매일 자정 갱신)
  - 퀘스트 완료 다이얼로그 상태 관리

- **HomeScreen.kt 통합**:
  - QuestSection 추가 (Today's Learning과 Recommended Scenarios 사이)
  - QuestCompletedDialog 표시
  - QuestViewModel 주입

##### Phase 2 Part 3: 자동 진행률 추적 ✅
**커밋**: `710cd9c` - "feat: Add automatic quest progress tracking (Phase 2 Part 3)"

- **ChatViewModel.kt**:
  - `QuestRepository` 주입
  - **MESSAGE_COUNT 추적**: `sendJapaneseMessage()` 완료 시 자동 +1
  - **VOICE_ONLY_SESSION 추적**: `endVoiceOnlyMode()` 완료 시 자동 +1
  - 백그라운드 코루틴으로 비동기 업데이트 (UI 블로킹 없음)

- **QuestViewModel.kt 자동 완료 감지**:
  - `previousQuestCompletionState` 맵으로 이전 상태 추적
  - `isCompleted` 전환 감지 (false → true)
  - 자동으로 `showQuestCompletedDialog = true` 설정
  - 재시작 시 중복 표시 방지

- **퀘스트 흐름**:
  1. 사용자 액션 (메시지 전송/음성 세션 종료)
  2. `questRepository.incrementQuestProgressByType()` 호출
  3. Repository에서 자동 완료 체크 및 포인트 지급
  4. ViewModel에서 완료 감지 → 다이얼로그 표시
  5. 사용자가 축하 메시지 확인

#### 향후 확장 가능성

**추가 퀘스트 타입 구현 예정**:
- [ ] PRONUNCIATION_PRACTICE: `checkPronunciation()` 추적
- [ ] GRAMMAR_ANALYSIS: `requestGrammarExplanation()` 추적
- [ ] VOCABULARY_REVIEW: 플래시카드 복습 추적
- [ ] SCENARIO_COMPLETE: `completeConversation()` 추적
- [ ] NEW_SCENARIO: `initConversation()` 추적

**UI/UX 개선 예정**:
- [ ] 포인트 획득 애니메이션 (Phase 7)
- [ ] 레벨업 축하 애니메이션
- [ ] 퀘스트 히스토리 화면
- [ ] 주간 퀘스트 챌린지

---

### Phase 3: 리더보드 & Achievement 🏆
**예상 시간**: 5시간
**우선순위**: ⭐ Important

#### 구현 내용
- 주간 리더보드 화면 (상위 3명 강조 디자인)
- Achievement 시스템 (배지, 도전과제)
- 프로필 화면에 배지 갤러리

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 4: 학습 통계 고도화 📈
**예상 시간**: 6시간
**우선순위**: ⭐ Important

#### 구현 내용
- 월별 진행률 그래프 (곡선형, Bezier curve)
- 주간 학습량 바 차트
- 학습 시간 통계 (이번 주 vs 지난 주)
- 히트맵 캘린더 (GitHub 스타일)

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 5: 채팅 화면 개선 💬
**예상 시간**: 7시간
**우선순위**: ⭐ Important

#### 구현 내용
- 메시지 날짜/시간별 그룹핑
- Swipe-to-Reply 제스처
- 메시지 반응 (이모지)
- 메시지 전달 상태 표시 (전송 중, 전송 완료, 실패)

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 6: 하단 네비게이션 바 🧭 **최우선**
**예상 시간**: 3시간
**우선순위**: 🔥 Critical

#### 목표
주요 화면에 빠르게 접근할 수 있는 하단 네비게이션 추가

#### 구현 내용

##### 6.1 NavigationBar UI
**파일**: `presentation/navigation/BottomNavigationBar.kt` (신규)

```kotlin
@Composable
fun NihongoBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        // 1. 홈 (대시보드)
        NavigationBarItem(
            selected = currentRoute == Screen.ScenarioList.route,
            onClick = { onNavigate(Screen.ScenarioList.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "홈"
                )
            },
            label = { Text("홈") }
        )

        // 2. 학습 (마지막 대화 재개)
        NavigationBarItem(
            selected = currentRoute.startsWith("chat"),
            onClick = { onNavigate("chat/resume") },  // 특별한 라우트
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "학습"
                )
            },
            label = { Text("학습") }
        )

        // 3. 복습 (플래시카드)
        NavigationBarItem(
            selected = currentRoute == Screen.FlashcardReview.route,
            onClick = { onNavigate(Screen.FlashcardReview.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "복습"
                )
            },
            label = { Text("복습") }
        )

        // 4. 통계
        NavigationBarItem(
            selected = currentRoute == Screen.Stats.route,
            onClick = { onNavigate(Screen.Stats.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "통계"
                )
            },
            label = { Text("통계") }
        )

        // 5. 프로필
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필"
                )
            },
            label = { Text("프로필") }
        )
    }
}
```

##### 6.2 MainActivity 통합
**파일**: `MainActivity.kt` (수정)

```kotlin
@Composable
fun NihongoApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 하단 네비게이션을 숨겨야 하는 화면들
    val hideBottomBarRoutes = setOf(
        Screen.Onboarding.route,
        Screen.UserSelection.route,
        // Chat 화면도 숨김 (전체 화면 필요)
    )

    val shouldShowBottomBar = currentRoute !in hideBottomBarRoutes &&
        !currentRoute.orEmpty().startsWith("chat/")

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NihongoBottomNavigationBar(
                    currentRoute = currentRoute.orEmpty(),
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // 백스택 최적화
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NihongoNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

##### 6.3 "마지막 대화 재개" 로직
**파일**: `presentation/navigation/NihongoNavHost.kt` (수정)

```kotlin
composable("chat/resume") {
    // ViewModel에서 마지막 대화 조회
    val viewModel: ResumeViewModel = hiltViewModel()
    val lastConversation by viewModel.lastConversation.collectAsState()

    LaunchedEffect(lastConversation) {
        if (lastConversation != null) {
            val conv = lastConversation!!
            navController.navigate("chat/${conv.userId}/${conv.scenarioId}") {
                popUpTo("chat/resume") { inclusive = true }
            }
        } else {
            // 대화 기록이 없으면 시나리오 선택 화면으로
            navController.navigate(Screen.ScenarioList.route) {
                popUpTo("chat/resume") { inclusive = true }
            }
        }
    }

    // 로딩 화면
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

**파일**: `presentation/navigation/ResumeViewModel.kt` (신규)

```kotlin
@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val lastConversation = repository.getLastConversation(userPreferences.currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

// Repository에 메서드 추가
interface ConversationRepository {
    fun getLastConversation(userId: Long): Flow<Conversation?>
}

class ConversationRepositoryImpl {
    override fun getLastConversation(userId: Long): Flow<Conversation?> {
        return conversationDao.getLastConversation(userId)
            .map { it?.toDomainModel() }
    }
}

@Dao
interface ConversationDao {
    @Query("""
        SELECT * FROM conversations
        WHERE userId = :userId
        ORDER BY updatedAt DESC
        LIMIT 1
    """)
    fun getLastConversation(userId: Long): Flow<ConversationEntity?>
}
```

#### 완료 조건
- [ ] NihongoBottomNavigationBar 컴포넌트 작동
- [ ] MainActivity Scaffold에 bottomBar 통합
- [ ] 화면별 bottomBar 표시/숨김 로직 작동
- [ ] "학습" 탭에서 마지막 대화 재개 기능 작동
- [ ] 네비게이션 백스택 최적화 (saveState, restoreState)
- [ ] 선택된 탭 하이라이트 작동

---

### Phase 7: 마이크로 애니메이션 ✨
**예상 시간**: 4시간
**우선순위**: ✅ Optional

#### 구현 내용
- 카드 press 효과 (scale 애니메이션)
- 진행률 바 애니메이션 (부드러운 증가)
- FAB 확장/축소 애니메이션
- 페이지 전환 애니메이션

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 8: 다크 모드 최적화 🌙
**예상 시간**: 3시간
**우선순위**: ✅ Optional

#### 구현 내용
- 커스텀 다크 컬러 팔레트 (순수 검정 피함)
- 다크 모드 전용 그라데이션
- 메시지 버블 색상 조정
- 대비 개선

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 9: 온보딩 개선 🚀
**예상 시간**: 4시간
**우선순위**: ✅ Optional

#### 구현 내용
- Skip 버튼 추가
- 진행 표시 강화 (도트 → 바)
- 인터랙티브 애니메이션
- 타이핑 효과

(세부 구현 내용은 실제 작업 시 작성)

---

### Phase 10: Pull-to-Refresh & 스켈레톤 로딩 ⏳
**예상 시간**: 3시간
**우선순위**: ✅ Optional

#### 구현 내용
- Pull-to-Refresh (Material 3)
- 스켈레톤 로딩 (shimmer 효과)
- 모든 리스트 화면에 적용

(세부 구현 내용은 실제 작업 시 작성)

---

## 📅 구현 일정 (제안)

### Week 1: 핵심 기능 (Phase 1, 2, 6)
- **Day 1-2**: Phase 1 (홈 대시보드)
- **Day 3-4**: Phase 2 (퀘스트 시스템)
- **Day 5**: Phase 6 (하단 네비게이션)

### Week 2: 중요 기능 (Phase 3, 4, 5)
- **Day 1**: Phase 3 (리더보드 & Achievement)
- **Day 2-3**: Phase 4 (학습 통계 고도화)
- **Day 4-5**: Phase 5 (채팅 화면 개선)

### Week 3: 선택 기능 & 마무리 (Phase 7, 8, 9, 10)
- **Day 1**: Phase 7 (마이크로 애니메이션)
- **Day 2**: Phase 8 (다크 모드 최적화)
- **Day 3**: Phase 9 (온보딩 개선)
- **Day 4**: Phase 10 (Pull-to-Refresh & 스켈레톤)
- **Day 5**: 통합 테스트 & 버그 수정

---

## 🎯 성공 지표

### 사용자 경험 개선
- [ ] 학습 진행률을 홈 화면에서 즉시 확인 가능
- [ ] 일일 퀘스트로 학습 동기 부여 (완료율 목표: 70%)
- [ ] 하단 네비게이션으로 주요 화면 접근 시간 50% 단축
- [ ] 애니메이션으로 인터랙션 만족도 향상

### 기술적 성과
- [ ] 모든 Phase 1-6 기능 작동 (필수)
- [ ] 데이터베이스 마이그레이션 성공 (12 → 13)
- [ ] 성능 저하 없음 (60fps 유지)
- [ ] 메모리 사용량 10% 이내 증가

### 학습 지속성
- [ ] 일일 활성 사용자(DAU) 30% 증가
- [ ] 평균 세션 시간 20% 증가
- [ ] 7일 리텐션 40% → 60% 개선
- [ ] 퀘스트 완료율 70% 이상

---

## 📝 개발 가이드

### 코딩 규칙
1. **Composable 함수명**: PascalCase (예: `LearningProgressCard`)
2. **State 변수**: camelCase with `by` delegation (예: `val quests by viewModel.quests.collectAsState()`)
3. **ViewModel**: `@HiltViewModel` + `@Inject constructor`
4. **Repository**: Interface + Impl 분리
5. **DAO 쿼리**: Multi-line raw string with proper indentation

### 애니메이션 가이드
- **Duration**: 200-300ms (빠른 피드백), 500-1000ms (큰 변화)
- **Easing**: `EaseOutCubic` (일반), `Spring` (바운스 효과)
- **AnimatedVisibility**: `fadeIn + scaleIn` (등장), `fadeOut + slideOut` (퇴장)
- **LaunchedEffect**: 애니메이션 트리거용, `delay()` 후 상태 변경

### Database 가이드
- **Migration**: 반드시 테스트 후 커밋
- **Index**: 자주 조회하는 컬럼에 추가 (`userId`, `timestamp`, `isCompleted`)
- **Cascade**: Foreign Key 설정 시 `onDelete = CASCADE` 고려
- **Default 값**: Entity에는 Kotlin default, SQL에는 DEFAULT 사용 자제

### 성능 최적화
- **LazyColumn**: `key` 파라미터 필수
- **remember**: 계산 비용이 높은 값은 `rememberSaveable`
- **derivedStateOf**: 복잡한 계산을 캐싱
- **Flow.stateIn**: ViewModel에서 StateFlow로 변환 시 사용

---

## 🐛 예상 이슈 & 해결법

### Issue 1: Database Migration 실패
**증상**: 앱 크래시, `IllegalStateException: Migration didn't properly handle`

**해결**:
1. Entity 정의와 Migration SQL이 정확히 일치하는지 확인
2. 인덱스를 Entity `@Index`에 명시
3. DEFAULT 값은 Kotlin에서 처리, SQL에서 사용 자제
4. 완전 재설치로 테스트: `adb uninstall com.nihongo.conversation`

### Issue 2: 퀘스트 진행률이 업데이트되지 않음
**원인**: ChatViewModel에서 `questRepository.updateQuestProgress()` 호출 누락

**해결**:
```kotlin
fun sendMessage(text: String) {
    viewModelScope.launch {
        // 메시지 전송 로직...

        // ✅ 퀘스트 업데이트 추가
        updateQuestProgress()
    }
}
```

### Issue 3: 하단 네비게이션이 특정 화면에서 표시됨
**원인**: `shouldShowBottomBar` 조건에 화면 추가 누락

**해결**:
```kotlin
val hideBottomBarRoutes = setOf(
    Screen.Onboarding.route,
    Screen.UserSelection.route,
    // ✅ 추가 필요한 화면
    Screen.CreateScenario.route
)
```

### Issue 4: 포인트 획득 애니메이션이 멈춤
**원인**: `onComplete` 콜백 미실행

**해결**:
```kotlin
LaunchedEffect(Unit) {
    isVisible = true
    delay(2000)
    isVisible = false
    delay(300)  // fadeOut 완료 대기
    onComplete()  // ✅ 반드시 호출
}
```

---

## 📚 참고 자료

### Material 3 Components
- [NavigationBar](https://m3.material.io/components/navigation-bar)
- [Linear Progress Indicator](https://m3.material.io/components/progress-indicators)
- [Cards](https://m3.material.io/components/cards)

### Jetpack Compose
- [AnimatedVisibility](https://developer.android.com/jetpack/compose/animation)
- [LazyColumn Performance](https://developer.android.com/jetpack/compose/lists)
- [State Management](https://developer.android.com/jetpack/compose/state)

### Room Database
- [Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [DAO Best Practices](https://developer.android.com/training/data-storage/room/accessing-data)

---

## ✅ Checklist (Phase 1-2-6 완료 후)

### 기능 테스트
- [ ] 대시보드 카드들이 실시간으로 업데이트됨
- [ ] 스트릭이 정확히 계산됨 (매일 자정 리셋)
- [ ] 일일 목표 진행률이 정확함
- [ ] 퀘스트가 자동 생성됨 (매일 자정)
- [ ] 퀘스트 완료 시 포인트 지급 및 애니메이션 표시
- [ ] 레벨업 시 알림
- [ ] 하단 네비게이션으로 모든 주요 화면 이동 가능
- [ ] "학습" 탭에서 마지막 대화 재개

### 성능 테스트
- [ ] 홈 화면 로딩 시간 < 1초
- [ ] 스크롤 60fps 유지
- [ ] 메모리 사용량 증가 < 10%
- [ ] 배터리 소모 정상

### UI/UX 테스트
- [ ] 다크 모드에서 모든 카드 가독성 확인
- [ ] 태블릿에서 레이아웃 정상 표시
- [ ] 긴 텍스트 처리 (ellipsis)
- [ ] 애니메이션 부드러움

### 버그 확인
- [ ] 데이터베이스 마이그레이션 성공
- [ ] 앱 재시작 시 데이터 유지
- [ ] 권한 거부 시 정상 처리
- [ ] 네트워크 오류 시 Fallback

---

이 문서는 **살아있는 문서**입니다. 각 Phase를 구현하면서 발견한 이슈, 개선 사항, 새로운 아이디어를 계속 업데이트하세요.
