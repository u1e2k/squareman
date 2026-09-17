package com.retro.squareman.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PSPAccentBlue,
    secondary = RetroFocusAmber,
    tertiary = RetroFocusGreen,
    background = RetroDarkBg,
    surface = RetroPanelBg,
    onPrimary = RetroDarkBg,
    onSecondary = RetroDarkBg,
    onTertiary = RetroDarkBg,
    onBackground = RetroTextPrimary,
    onSurface = RetroTextPrimary
)

@Composable
fun SquaremanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = RetroTypography,
        content = content
    )
}
