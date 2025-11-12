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
 * Phase 12: 모든 화면에서 통일된 난이도 색상 사용
 *
 * - 초급 (1): primaryContainer (파랑)
 * - 중급 (2): tertiaryContainer (보라)
 * - 고급 (3): errorContainer (빨강)
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
            1 -> AppDesignSystem.Colors.difficultyBeginner()
            2 -> AppDesignSystem.Colors.difficultyIntermediate()
            3 -> AppDesignSystem.Colors.difficultyAdvanced()
            else -> AppDesignSystem.Colors.surfaceVariant()
        }
    ) {
        Text(
            text = when (difficulty) {
                1 -> "초급"
                2 -> "중급"
                3 -> "고급"
                else -> "초급"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
