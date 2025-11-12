package com.nihongo.conversation.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihongo.conversation.core.theme.AppDesignSystem

/**
 * 난이도 배지 컴포넌트
 * Phase 5단계 세분화: 1=입문, 2=초급, 3=중급, 4=고급, 5=최상급
 *
 * - 입문 (1): secondaryContainer (연한 파랑)
 * - 초급 (2): primaryContainer (파랑)
 * - 중급 (3): tertiaryContainer (보라)
 * - 고급 (4): errorContainer (빨강)
 * - 최상급 (5): error (진한 빨강)
 */
@Composable
fun DifficultyBadge(
    difficulty: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = when (difficulty) {
            1 -> MaterialTheme.colorScheme.secondaryContainer      // 입문: 연한 파랑
            2 -> AppDesignSystem.Colors.difficultyBeginner()       // 초급: 파랑
            3 -> AppDesignSystem.Colors.difficultyIntermediate()   // 중급: 보라
            4 -> AppDesignSystem.Colors.difficultyAdvanced()       // 고급: 빨강
            5 -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f) // 최상급: 진한 빨강
            else -> AppDesignSystem.Colors.surfaceVariant()
        }
    ) {
        Text(
            text = when (difficulty) {
                1 -> "입문"
                2 -> "초급"
                3 -> "중급"
                4 -> "고급"
                5 -> "최상급"
                else -> "초급"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
