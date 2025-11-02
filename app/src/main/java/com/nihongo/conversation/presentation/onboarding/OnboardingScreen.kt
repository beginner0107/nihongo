package com.nihongo.conversation.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            title = "일본어 회화 연습",
            description = "AI와 실전 대화를 연습하세요\n50개 이상의 실제 상황 시나리오로\n자연스러운 일본어 회화를 익혀보세요",
            icon = Icons.Default.Chat,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
        ),
        OnboardingPage(
            title = "음성 인식 & TTS",
            description = "말하고 듣는 학습으로 발음을 익히세요\n음성 인식으로 대화하고\nTTS로 정확한 발음을 들어보세요",
            icon = Icons.Default.Mic,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        OnboardingPage(
            title = "문법 분석 & 힌트",
            description = "메시지를 길게 눌러 문법을 분석하세요\n문장 구조, 품사, 한국어 번역을\n실시간으로 확인할 수 있습니다",
            icon = Icons.Default.School,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        OnboardingPage(
            title = "단어장 & 통계",
            description = "학습한 단어를 복습하고\n학습 진행 상황을 확인하세요\n플래시카드로 효과적인 복습이 가능합니다",
            icon = Icons.Default.Analytics,
            backgroundColor = MaterialTheme.colorScheme.errorContainer
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    OnboardingPageContent(page = pages[page])
                }

                // Page Indicators
                Row(
                    modifier = Modifier
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width = animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width.value)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                // Navigation Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button (show only on first 3 pages)
                    AnimatedVisibility(
                        visible = pagerState.currentPage < pages.size - 1,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TextButton(onClick = onComplete) {
                            Text("건너뛰기")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next/Start button
                    if (pagerState.currentPage < pages.size - 1) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        ) {
                            Text("다음")
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            Text("시작하기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Container
        Surface(
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp),
            shape = CircleShape,
            color = page.backgroundColor
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
        )
    }
}
