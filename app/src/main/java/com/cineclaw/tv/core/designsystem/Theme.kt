package com.cineclaw.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorPalette = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = ObsidianBackground,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyanSecondary,
    onSecondary = ObsidianBackground,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    border = ObsidianBorder
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CineClawTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}
