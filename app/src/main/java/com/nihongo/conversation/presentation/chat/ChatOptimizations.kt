package com.nihongo.conversation.presentation.chat

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Optimized animation specs for UI rendering performance
 * Using remember to avoid recreating specs on every recomposition
 */
@Stable
object ChatAnimations {
    /**
     * Animation duration in milliseconds
     * Reduced from 300ms to 200ms for better perceived performance
     */
    private const val ANIMATION_DURATION = 200

    /**
     * Fade in animation spec (reused across components)
     */
    val fadeInSpec: AnimationSpec<Float> = tween(
        durationMillis = ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )

    /**
     * Fade out animation spec
     */
    val fadeOutSpec: AnimationSpec<Float> = tween(
        durationMillis = ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )

    /**
     * Slide animation spec for messages
     */
    val slideSpec: AnimationSpec<Float> = tween(
        durationMillis = ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )

    /**
     * Get optimized enter transition for messages
     * Disables animations on low-end devices
     */
    @Composable
    fun rememberMessageEnterTransition(): androidx.compose.animation.EnterTransition {
        val configuration = LocalConfiguration.current
        val isLowEnd = remember(configuration) {
            configuration.screenWidthDp < 320 ||
            (configuration.screenWidthDp * configuration.screenHeightDp < 500_000)
        }

        return remember(isLowEnd) {
            if (isLowEnd) {
                fadeIn(animationSpec = tween(50)) // Minimal fade on low-end devices
            } else {
                slideInVertically(
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    /**
     * Get optimized exit transition for messages
     */
    @Composable
    fun rememberMessageExitTransition(): androidx.compose.animation.ExitTransition {
        val configuration = LocalConfiguration.current
        val isLowEnd = remember(configuration) {
            configuration.screenWidthDp < 320 ||
            (configuration.screenWidthDp * configuration.screenHeightDp < 500_000)
        }

        return remember(isLowEnd) {
            if (isLowEnd) {
                fadeOut(animationSpec = tween(50))
            } else {
                slideOutVertically(
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }
}

/**
 * Optimized typing indicator with remembered animation spec
 */
@Composable
fun rememberTypingIndicatorSpec() = remember {
    tween<Float>(
        durationMillis = 600,
        delayMillis = 150,
        easing = FastOutSlowInEasing
    )
}
