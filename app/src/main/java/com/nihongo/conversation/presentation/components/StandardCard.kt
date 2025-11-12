package com.nihongo.conversation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nihongo.conversation.core.theme.AppDesignSystem

/**
 * 표준 카드 컴포넌트
 * Phase 12: 모든 화면에서 일관된 Card 스타일 사용
 *
 * - Elevation: 2.dp
 * - Horizontal Padding: 16.dp
 * - Inner Padding: 20.dp
 * - Element Spacing: 12.dp
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDesignSystem.Spacing.cardHorizontalPadding),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDesignSystem.Spacing.cardHorizontalPadding / 8)  // 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDesignSystem.Spacing.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.Spacing.elementSpacing),
            content = content
        )
    }
}
