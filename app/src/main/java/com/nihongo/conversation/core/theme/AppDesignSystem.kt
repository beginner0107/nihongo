package com.nihongo.conversation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 앱 전체 디자인 시스템 정의
 * Phase 12: 디자인 통일성 개선을 위한 공통 색상 및 간격 규칙
 */
object AppDesignSystem {

    /**
     * 색상 팔레트
     */
    object Colors {
        // Primary actions & highlights
        @Composable
        fun primaryChip() = MaterialTheme.colorScheme.primaryContainer

        @Composable
        fun secondaryChip() = MaterialTheme.colorScheme.secondaryContainer

        @Composable
        fun tertiaryChip() = MaterialTheme.colorScheme.tertiaryContainer

        // Difficulty levels (통일!)
        @Composable
        fun difficultyBeginner() = MaterialTheme.colorScheme.primaryContainer  // 초급: 파랑

        @Composable
        fun difficultyIntermediate() = MaterialTheme.colorScheme.tertiaryContainer  // 중급: 보라

        @Composable
        fun difficultyAdvanced() = MaterialTheme.colorScheme.errorContainer  // 고급: 빨강

        // Status colors
        val success = Color(0xFF4CAF50)      // 완료/달성
        val warning = Color(0xFFFFB300)      // 진행 중
        val gold = Color(0xFFFFD700)         // 보상/포인트

        // Backgrounds
        @Composable
        fun surfaceVariant() = MaterialTheme.colorScheme.surfaceVariant
    }

    /**
     * Spacing 시스템
     */
    object Spacing {
        val cardHorizontalPadding: Dp = 16.dp
        val cardInnerPadding: Dp = 20.dp
        val elementSpacing: Dp = 12.dp
        val sectionSpacing: Dp = 16.dp
    }
}
