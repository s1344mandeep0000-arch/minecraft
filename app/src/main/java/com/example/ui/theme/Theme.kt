package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VoxelDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = TextPrimary,
    secondary = AmethystSecondary,
    onSecondary = ObsidianBg,
    secondaryContainer = AmethystContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = DiamondAccent,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = VoxelCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = VoxelCard,
    surfaceContainerHigh = VoxelElevated,
    outline = BorderSubtle,
    outlineVariant = BorderEmerald,
    error = RedstoneError,
    onError = ObsidianBg,
    errorContainer = RedstoneContainer,
    onErrorContainer = RedstoneError
)

private val VoxelShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun VoxelStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoxelDarkColorScheme,
        typography = Typography,
        shapes = VoxelShapes,
        content = content
    )
}
