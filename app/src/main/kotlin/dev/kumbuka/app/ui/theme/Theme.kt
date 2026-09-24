package dev.kumbuka.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private fun KbPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryTint,
        onPrimaryContainer = onPrimaryTint,
        secondary = brand,
        onSecondary = if (isDark) paper else surface,
        secondaryContainer = primaryTint,
        onSecondaryContainer = onPrimaryTint,
        tertiary = brand,
        onTertiary = if (isDark) paper else surface,
        tertiaryContainer = brandTint,
        onTertiaryContainer = ink,
        background = paper,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = paper2,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = paper2,
        surfaceBright = surface,
        surfaceDim = paper2,
        outline = outlineStrong,
        outlineVariant = border,
        error = error,
        onError = if (isDark) paper else surface,
        errorContainer = errorContainer,
        onErrorContainer = error,
        inverseSurface = ink,
        inverseOnSurface = paper,
        inversePrimary = if (isDark) KbLightPalette.primary else KbDarkPalette.primary,
        scrim = base.scrim,
    )
}

/** Concentric rounded geometry: controls 12dp, rows/cards 16-20dp, sheets/hero 28dp. */
private val KumbukaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KumbukaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val kbPalette = if (darkTheme) KbDarkPalette else KbLightPalette
    CompositionLocalProvider(LocalKbColors provides kbPalette) {
        MaterialTheme(
            colorScheme = kbPalette.toColorScheme(),
            typography = KumbukaTypography,
            shapes = KumbukaShapes,
            content = content,
        )
    }
}
