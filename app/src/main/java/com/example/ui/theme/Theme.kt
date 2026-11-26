package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AvyaktDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = AbyssBlack,
    primaryContainer = DeepAzure,
    onPrimaryContainer = CrispWhite,
    secondary = LuminousViolet,
    onSecondary = CrispWhite,
    secondaryContainer = DeepAmethyst,
    onSecondaryContainer = NeonOrchid,
    tertiary = NeonOrchid,
    onTertiary = AbyssBlack,
    background = AbyssBlack,
    onBackground = CrispWhite,
    surface = MidnightSurface,
    onSurface = CrispWhite,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = MutedIce,
    surfaceContainerLowest = AbyssBlack,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = ElevatedSlate,
    surfaceContainerHigh = GlassPanel,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonAlert,
    onError = CrispWhite
)

private val AvyaktShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AvyaktTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AvyaktDarkColorScheme,
        typography = Typography,
        shapes = AvyaktShapes,
        content = content
    )
}
