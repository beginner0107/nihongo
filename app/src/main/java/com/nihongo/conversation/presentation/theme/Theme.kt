package com.nihongo.conversation.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF)
)

// High contrast light color scheme
private val HighContrastLightColorScheme = lightColorScheme(
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

// High contrast dark color scheme
private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF)
)

@Composable
fun NihongoTheme(
    darkTheme: Boolean = false,
    textSizePreference: TextSizePreference = TextSizePreference.NORMAL,
    contrastMode: ContrastMode = ContrastMode.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        contrastMode == ContrastMode.HIGH && darkTheme -> HighContrastDarkColorScheme
        contrastMode == ContrastMode.HIGH && !darkTheme -> HighContrastLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
