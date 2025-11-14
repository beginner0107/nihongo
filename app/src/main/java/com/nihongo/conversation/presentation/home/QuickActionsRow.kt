package com.nihongo.conversation.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Quick Actions Row - Four primary actions with cute emoji buttons
 *
 * Cute, colorful design for quick access
 * Height: ~80dp
 *
 * Actions:
 * - ▶ 이어하기 (Resume last conversation)
 * - 🎲 랜덤 (Random scenario)
 * - 📜 히스토리 (Conversation history)
 * - 📋 전체 (View all scenarios)
 */
@Composable
fun QuickActionsRow(
    onResume: () -> Unit,
    onRandom: () -> Unit,
    onViewAll: () -> Unit,
    onHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: Resume + Random
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resume button
            FilledTonalButton(
                onClick = onResume,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFE5F3FF),
                    contentColor = Color(0xFF1976D2)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "이어하기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Random button
            FilledTonalButton(
                onClick = onRandom,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFFFF9E5),
                    contentColor = Color(0xFFF57C00)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "랜덤",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Second row: History + View All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // History button
            FilledTonalButton(
                onClick = onHistory,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFFFE5EC),
                    contentColor = Color(0xFFD81B60)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "히스토리",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // View All button
            FilledTonalButton(
                onClick = onViewAll,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFE5FFE5),
                    contentColor = Color(0xFF388E3C)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "전체",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
