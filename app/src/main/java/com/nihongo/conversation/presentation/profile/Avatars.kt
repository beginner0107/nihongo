package com.nihongo.conversation.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Predefined avatar emojis
val AVATAR_EMOJIS = listOf(
    "😊", // Happy
    "🎌", // Japanese flag
    "🗾", // Japan map
    "🍣", // Sushi
    "⛩️", // Torii gate
    "🎎"  // Japanese dolls
)

val AVATAR_COLORS = listOf(
    Color(0xFFFFB74D), // Orange
    Color(0xFF81C784), // Green
    Color(0xFF64B5F6), // Blue
    Color(0xFFE57373), // Red
    Color(0xFFBA68C8), // Purple
    Color(0xFFFFD54F)  // Yellow
)

/**
 * Avatar display component
 */
@Composable
fun Avatar(
    avatarId: Int,
    size: Int = 64,
    modifier: Modifier = Modifier
) {
    val emoji = AVATAR_EMOJIS.getOrElse(avatarId) { AVATAR_EMOJIS[0] }
    val backgroundColor = AVATAR_COLORS.getOrElse(avatarId) { AVATAR_COLORS[0] }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size * 0.6).sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Avatar selector grid
 */
@Composable
fun AvatarSelector(
    selectedAvatarId: Int,
    onAvatarSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "아바타",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AVATAR_EMOJIS.forEachIndexed { index, emoji ->
                val isSelected = selectedAvatarId == index
                val backgroundColor = AVATAR_COLORS.getOrElse(index) { AVATAR_COLORS[0] }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onAvatarSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get avatar emoji by ID
 */
fun getAvatarEmoji(avatarId: Int): String {
    return AVATAR_EMOJIS.getOrElse(avatarId) { AVATAR_EMOJIS[0] }
}

/**
 * Get avatar color by ID
 */
fun getAvatarColor(avatarId: Int): Color {
    return AVATAR_COLORS.getOrElse(avatarId) { AVATAR_COLORS[0] }
}
