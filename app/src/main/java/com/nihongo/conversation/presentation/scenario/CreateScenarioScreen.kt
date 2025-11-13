package com.nihongo.conversation.presentation.scenario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 커스텀 시나리오 생성 화면
 * 사용자가 자신만의 시나리오를 만들 수 있습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScenarioScreen(
    onBackClick: () -> Unit,
    onScenarioCreated: () -> Unit,
    viewModel: ScenarioViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var difficulty by remember { mutableIntStateOf(1) }
    var emoji by remember { mutableStateOf("💬") }
    var systemPrompt by remember { mutableStateOf("") }
    var useAiGenerator by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isGeneratingPrompt by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            "DAILY_LIFE" to "🏠 일상 생활",
            "WORK" to "💼 직장/업무",
            "TRAVEL" to "✈️ 여행",
            "ENTERTAINMENT" to "🎵 엔터테인먼트",
            "ESPORTS" to "🎮 e스포츠",
            "TECH" to "💻 기술/개발",
            "FINANCE" to "💰 금융/재테크",
            "CULTURE" to "🎭 문화",
            "HOUSING" to "🏢 부동산/주거",
            "HEALTH" to "🏥 건강/의료",
            "STUDY" to "📚 학습/교육",
            "DAILY_CONVERSATION" to "💬 일상 회화",
            "JLPT_PRACTICE" to "📖 JLPT 연습",
            "BUSINESS" to "🤝 비즈니스",
            "ROMANCE" to "💕 연애/관계",
            "EMERGENCY" to "🚨 긴급 상황",
            "OTHER" to "📚 기타"
        )
    }

    val commonEmojis = remember {
        listOf(
            "💬", "📝", "🎯", "🎓", "💼", "🏠", "✈️", "🍽️",
            "🎵", "🎮", "💻", "🏥", "🏦", "🎭", "💕", "🚨",
            "📚", "🗣️", "👥", "🌟", "🎉", "🔥", "💡", "🎨"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("커스텀 시나리오 만들기") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    // 생성 버튼
                    TextButton(
                        onClick = {
                            viewModel.createCustomScenario(
                                title = title,
                                description = description,
                                category = category,
                                difficulty = difficulty,
                                emoji = emoji,
                                systemPrompt = systemPrompt.ifBlank {
                                    // 기본 프롬프트 생성
                                    generateDefaultPrompt(title, description, difficulty)
                                }
                            )
                            showSuccessDialog = true
                        },
                        enabled = title.isNotBlank() && description.isNotBlank()
                    ) {
                        Text("생성")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 제목 입력
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("시나리오 제목 *") },
                    placeholder = { Text("예: 카페에서 주문하기") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null)
                    }
                )
            }

            // 설명 입력
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 *") },
                    placeholder = { Text("예: 카페에서 음료와 디저트를 주문하는 상황을 연습합니다") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    }
                )
            }

            // 카테고리 선택
            item {
                ExposedDropdownMenuBox(
                    expanded = showCategoryMenu,
                    onExpandedChange = { showCategoryMenu = it }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.first == category }?.second ?: "📚 기타",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("카테고리") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        categories.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    category = key
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 난이도 선택
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "난이도",
                        style = MaterialTheme.typography.labelLarge
                    )

                    // Phase 5단계: 난이도 선택 (2줄로 표시)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 첫 번째 줄: 입문, 초급, 중급
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                1 to "입문",
                                2 to "초급",
                                3 to "중급"
                            ).forEach { (level, label) ->
                                FilterChip(
                                    selected = difficulty == level,
                                    onClick = { difficulty = level },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = if (difficulty == level) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                        // 두 번째 줄: 고급, 최상급
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                4 to "고급",
                                5 to "최상급"
                            ).forEach { (level, label) ->
                                FilterChip(
                                    selected = difficulty == level,
                                    onClick = { difficulty = level },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = if (difficulty == level) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                            // 빈 공간 채우기 (3칸 중 2칸만 사용)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 이모지 선택
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "아이콘 (이모지)",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedButton(
                        onClick = { showEmojiPicker = !showEmojiPicker },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("이모지 선택")
                    }

                    if (showEmojiPicker) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                commonEmojis.chunked(8).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        row.forEach { emojiOption ->
                                            IconButton(
                                                onClick = {
                                                    emoji = emojiOption
                                                    showEmojiPicker = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = emojiOption,
                                                    style = MaterialTheme.typography.headlineSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI 프롬프트 생성 도우미 (선택)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AI로 프롬프트 생성",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "제목과 설명을 바탕으로 AI가 시스템 프롬프트를 작성합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useAiGenerator,
                            onCheckedChange = { useAiGenerator = it }
                        )
                    }

                    if (useAiGenerator) {
                        Button(
                            onClick = {
                                isGeneratingPrompt = true
                                viewModel.generateSystemPrompt(
                                    title = title,
                                    description = description,
                                    difficulty = difficulty,
                                    onGenerated = { generated ->
                                        systemPrompt = generated
                                        isGeneratingPrompt = false
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = title.isNotBlank() && description.isNotBlank() && !isGeneratingPrompt
                        ) {
                            if (isGeneratingPrompt) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("생성 중...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("AI 프롬프트 생성")
                            }
                        }
                    }
                }
            }

            // 시스템 프롬프트 입력
            item {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("시스템 프롬프트 (선택)") },
                    placeholder = {
                        Text("AI 역할과 대화 규칙을 작성하세요.\n비워두면 기본 프롬프트가 사용됩니다.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 15,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    supportingText = {
                        Text(
                            "예: あなたはカフェの店員です。お客様の注文を丁寧に受けてください。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }

            // 도움말 카드
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "시나리오 작성 팁",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            "• 제목: 구체적이고 명확하게 (예: 병원 예약하기)\n" +
                                    "• 설명: 어떤 상황인지 자세히 설명\n" +
                                    "• 난이도: 사용할 어휘와 문법 수준\n" +
                                    "• 프롬프트: AI 역할과 대화 스타일 정의",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // 생성 완료 다이얼로그
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onScenarioCreated()
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("시나리오 생성 완료!")
            },
            text = {
                Text("새로운 커스텀 시나리오가 생성되었습니다.\n시나리오 목록에서 확인하세요.")
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onScenarioCreated()
                }) {
                    Text("확인")
                }
            }
        )
    }
}

/**
 * 기본 시스템 프롬프트 생성 (Phase 5단계 지원)
 */
private fun generateDefaultPrompt(title: String, description: String, difficulty: Int): String {
    val difficultyText = when (difficulty) {
        1 -> "入門レベル: 1-2文（合計10語以内）、超簡単な表現を使って"
        2 -> "初級レベル: 1-2文（5-12語）、簡単な表現を使って"
        3 -> "中級レベル: 2-3文（10-15語）、自然な表現を使って"
        4 -> "上級レベル: 2-4文（15-20語）、丁寧で正確な表現を使って"
        5 -> "最上級レベル: 3-5文（20-30語）、高度で専門的な表現を使って"
        else -> "自然な表現を使って"
    }

    return """
        あなたは「$title」のシナリオでユーザーと会話します。

        状況: $description

        ${difficultyText}、自然な日本語で応答してください。

        【重要】
        - マークダウン記号（**、_など）や読み仮名（例：お席（せき））を絶対に使わないでください
        - 自然な会話の流れを作る（モデル提示+質問など）
        - 1ターンにつき指定された文数と語数を守る
        - ユーザーの返答を待つ
    """.trimIndent()
}
