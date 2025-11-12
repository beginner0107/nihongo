package com.nihongo.conversation.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Quick Actions Row - Three primary actions for scenario access
 *
 * Phase 11 - Option C Component
 * Height: ~100dp
 *
 * Actions:
 * - 이어하기 (Resume last conversation)
 * - 랜덤 (Random scenario)
 * - 전체 (View all scenarios)
 */
@Composable
fun QuickActionsRow(
    onResume: () -> Unit,
    onRandom: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "빠른 액션",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = onResume,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "이어하기",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "이어하기",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            ElevatedButton(
                onClick = onRandom,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "랜덤",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "랜덤",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            OutlinedButton(
                onClick = onViewAll,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "전체",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "전체",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
