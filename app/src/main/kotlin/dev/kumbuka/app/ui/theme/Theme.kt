package dev.kumbuka.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = KbColors.primary,
    onPrimary = KbColors.surface,
    primaryContainer = KbColors.primaryTint,
    onPrimaryContainer = KbColors.primary,
    background = KbColors.paper,
    onBackground = KbColors.ink,
    surface = KbColors.surface,
    onSurface = KbColors.ink,
    surfaceVariant = KbColors.paper2,
    onSurfaceVariant = KbColors.inkMuted,
    outline = KbColors.border,
    error = KbColors.accent,
)

// Dark palette not specified in Figma yet; derived by keeping the same
// relationships (dark ground, light ink, same primary) until a real dark
// spec exists.
private val DarkColors = darkColorScheme(
    primary = KbColors.primaryTint,
    onPrimary = KbColors.ink,
    primaryContainer = KbColors.primary,
    onPrimaryContainer = KbColors.primaryTint,
    background = Color(0xFF0F1D20),
    onBackground = KbColors.paper,
    surface = Color(0xFF16282B),
    onSurface = KbColors.paper,
    surfaceVariant = Color(0xFF223235),
    onSurfaceVariant = KbColors.border,
    outline = KbColors.inkMuted,
    error = KbColors.accent,
)

@Composable
fun KumbukaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = KumbukaTypography,
        content = content,
    )
}
