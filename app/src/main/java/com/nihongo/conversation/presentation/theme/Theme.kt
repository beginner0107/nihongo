package com.nihongo.conversation.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.nihongo.conversation.domain.model.ContrastMode
import com.nihongo.conversation.domain.model.TextSizePreference

// CompositionLocal for text size scale
val LocalTextSizeScale = compositionLocalOf { 1.0f }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D)
)

// High contrast color scheme
private val HighContrastColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFF000000)
)

@Composable
fun NihongoTheme(
    textSizePreference: TextSizePreference = TextSizePreference.NORMAL,
    contrastMode: ContrastMode = ContrastMode.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when (contrastMode) {
        ContrastMode.NORMAL -> LightColorScheme
        ContrastMode.HIGH -> HighContrastColorScheme
    }

    val textScale = textSizePreference.scale

    CompositionLocalProvider(LocalTextSizeScale provides textScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography().scaledTypography(textScale),
            content = content
        )
    }
}

// Extension function to scale typography
private fun Typography.scaledTypography(scale: Float): Typography {
    return Typography(
        displayLarge = displayLarge.scale(scale),
        displayMedium = displayMedium.scale(scale),
        displaySmall = displaySmall.scale(scale),
        headlineLarge = headlineLarge.scale(scale),
        headlineMedium = headlineMedium.scale(scale),
        headlineSmall = headlineSmall.scale(scale),
        titleLarge = titleLarge.scale(scale),
        titleMedium = titleMedium.scale(scale),
        titleSmall = titleSmall.scale(scale),
        bodyLarge = bodyLarge.scale(scale),
        bodyMedium = bodyMedium.scale(scale),
        bodySmall = bodySmall.scale(scale),
        labelLarge = labelLarge.scale(scale),
        labelMedium = labelMedium.scale(scale),
        labelSmall = labelSmall.scale(scale)
    )
}

private fun TextStyle.scale(scale: Float): TextStyle {
    return copy(fontSize = fontSize * scale)
}
