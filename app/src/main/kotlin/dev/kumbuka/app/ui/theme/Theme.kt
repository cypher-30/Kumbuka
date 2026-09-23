package dev.kumbuka.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = KbLightPalette.primary,
    onPrimary = KbLightPalette.surface,
    primaryContainer = KbLightPalette.primaryTint,
    onPrimaryContainer = KbLightPalette.primary,
    secondary = KbLightPalette.primary,
    onSecondary = KbLightPalette.surface,
    secondaryContainer = KbLightPalette.primaryTint,
    onSecondaryContainer = KbLightPalette.primary,
    tertiary = KbLightPalette.accent,
    onTertiary = KbLightPalette.surface,
    background = KbLightPalette.paper,
    onBackground = KbLightPalette.ink,
    surface = KbLightPalette.surface,
    onSurface = KbLightPalette.ink,
    surfaceVariant = KbLightPalette.paper2,
    onSurfaceVariant = KbLightPalette.inkMuted,
    outline = KbLightPalette.border,
    error = KbLightPalette.accent,
)

private val DarkColors = darkColorScheme(
    primary = KbDarkPalette.primary,
    onPrimary = KbDarkPalette.paper,
    primaryContainer = KbDarkPalette.primaryTint,
    onPrimaryContainer = KbDarkPalette.primary,
    secondary = KbDarkPalette.primary,
    onSecondary = KbDarkPalette.paper,
    secondaryContainer = KbDarkPalette.primaryTint,
    onSecondaryContainer = KbDarkPalette.primary,
    tertiary = KbDarkPalette.accent,
    onTertiary = KbDarkPalette.paper,
    background = KbDarkPalette.paper,
    onBackground = KbDarkPalette.ink,
    surface = KbDarkPalette.surface,
    onSurface = KbDarkPalette.ink,
    surfaceVariant = KbDarkPalette.paper2,
    onSurfaceVariant = KbDarkPalette.inkMuted,
    outline = KbDarkPalette.border,
    error = KbDarkPalette.accent,
)

private val KumbukaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun KumbukaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val kbPalette = if (darkTheme) KbDarkPalette else KbLightPalette
    CompositionLocalProvider(LocalKbColors provides kbPalette) {
        MaterialTheme(
            colorScheme = colors,
            typography = KumbukaTypography,
            shapes = KumbukaShapes,
            content = content,
        )
    }
}
