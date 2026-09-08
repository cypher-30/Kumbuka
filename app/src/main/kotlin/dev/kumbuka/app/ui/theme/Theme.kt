package dev.kumbuka.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KumbukaGreen = Color(0xFF1B4332)
private val KumbukaGreenLight = Color(0xFF52796F)

private val LightColors = lightColorScheme(
    primary = KumbukaGreen,
    secondary = KumbukaGreenLight,
)

private val DarkColors = darkColorScheme(
    primary = KumbukaGreenLight,
    secondary = KumbukaGreen,
)

@Composable
fun KumbukaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
