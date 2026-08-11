package com.michis.player.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.michis.player.domain.repository.ThemePreference

data class MichisPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val accent: Color,
    val primaryText: Color,
    val secondaryText: Color,
)

fun paletteFor(theme: ThemePreference, systemDark: Boolean = false): MichisPalette = when (theme) {
    ThemePreference.SYSTEM -> paletteFor(if (systemDark) ThemePreference.DARK else ThemePreference.LIGHT)
    ThemePreference.DARK -> palette(0xFF111318, 0xFF1A1D24, 0xFF242832, 0xFF8FA9FF, 0xFFE8EAF0, 0xFFB8BDC9)
    ThemePreference.SEPIA -> palette(0xFFF4ECD8, 0xFFE7D9BC, 0xFFFFF8E8, 0xFF79552D, 0xFF3E3124, 0xFF6C5B49)
    ThemePreference.TWILIGHT -> palette(0xFF2F2638, 0xFF3B3046, 0xFF493A54, 0xFFE3A88F, 0xFFF1E2DC, 0xFFCAB7C6)
    ThemePreference.CONSOLE -> palette(0xFF071A0D, 0xFF0C2514, 0xFF12331C, 0xFF78F58B, 0xFFD6F7DC, 0xFF9FD7A9)
    ThemePreference.PAPER -> palette(0xFFFFFCF2, 0xFFF4F0E5, 0xFFFFFFFF, 0xFF665C49, 0xFF302D28, 0xFF68635B)
    ThemePreference.SAND -> palette(0xFFEAD9B8, 0xFFDDC69F, 0xFFF4E5C8, 0xFF76552E, 0xFF3E3328, 0xFF685A49)
    ThemePreference.LAVENDER -> palette(0xFFEDE7F6, 0xFFDED3EC, 0xFFF7F2FC, 0xFF675080, 0xFF332B45, 0xFF655D70)
    ThemePreference.FOREST -> palette(0xFF183229, 0xFF214239, 0xFF2B5045, 0xFF9BC7A0, 0xFFE1EEE4, 0xFFB2C9B8)
    ThemePreference.OCEAN -> palette(0xFF102C3A, 0xFF173B4B, 0xFF205064, 0xFF8ECBE0, 0xFFE0F0F5, 0xFFA9C8D3)
    ThemePreference.GRAPHITE -> palette(0xFF292B2F, 0xFF35383E, 0xFF42464D, 0xFFB9C1CC, 0xFFF0F1F2, 0xFFBEC1C5)
    ThemePreference.MIDNIGHT -> palette(0xFF0B1020, 0xFF121A30, 0xFF1C2742, 0xFF89A7FF, 0xFFE4EAFF, 0xFFADB9DA)
    ThemePreference.SOFT_PINK -> palette(0xFFFFEEF2, 0xFFF8DDE5, 0xFFFFF7F9, 0xFF8B5263, 0xFF4A3038, 0xFF765B64)
    ThemePreference.MINT -> palette(0xFFE7F5EE, 0xFFD5EADF, 0xFFF3FBF7, 0xFF3E735C, 0xFF203B30, 0xFF526C61)
    ThemePreference.LIGHT -> palette(0xFFF7F7F9, 0xFFFFFFFF, 0xFFEEEFF3, 0xFF53699F, 0xFF191B21, 0xFF5D616B)
}

@Composable
fun MichisTheme(
    theme: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colors = paletteFor(theme, isSystemInDarkTheme()).toColorScheme()
    CompositionLocalProvider(LocalMichisSpacing provides MichisSpacing()) {
        MaterialTheme(colorScheme = colors, typography = MichisTypography, shapes = MichisShapes, content = content)
    }
}

private fun palette(
    background: Long,
    surface: Long,
    card: Long,
    accent: Long,
    primaryText: Long,
    secondaryText: Long,
) = MichisPalette(
    background = Color(background),
    surface = Color(surface),
    card = Color(card),
    accent = Color(accent),
    primaryText = Color(primaryText),
    secondaryText = Color(secondaryText),
)

private fun MichisPalette.toColorScheme(): ColorScheme {
    val dark = background.luminance() < 0.42f
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val onAccent = if (accent.luminance() > 0.45f) Color(0xFF151619) else Color.White
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = card,
        onPrimaryContainer = primaryText,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = card,
        onSecondaryContainer = primaryText,
        tertiary = accent,
        onTertiary = onAccent,
        background = background,
        onBackground = primaryText,
        surface = surface,
        onSurface = primaryText,
        surfaceVariant = card,
        onSurfaceVariant = secondaryText,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = card,
        outline = primaryText.copy(alpha = 0.27f),
        outlineVariant = primaryText.copy(alpha = 0.16f),
    )
}

private fun Color.luminance(): Float {
    fun channel(value: Float): Float = if (value <= 0.04045f) value / 12.92f else Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

data class MichisSpacing(
    val extraSmall: androidx.compose.ui.unit.Dp = 4.dp,
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val extraLarge: androidx.compose.ui.unit.Dp = 32.dp,
)
