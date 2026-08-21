package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleekColorScheme = darkColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekMint,
    onTertiary = SleekOnMint,
    tertiaryContainer = SleekMintContainer,
    onTertiaryContainer = SleekMint,
    error = SleekCoral,
    onError = SleekOnCoral,
    errorContainer = SleekCoralContainer,
    onErrorContainer = SleekCoral,
    background = SleekBg,
    onBackground = TextPrimary,
    surface = SleekSurface,
    onSurface = TextPrimary,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SleekOutline,
    outlineVariant = SleekBorder
)

@Composable
fun MobileCraftTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekColorScheme,
        typography = Typography,
        content = content
    )
}
