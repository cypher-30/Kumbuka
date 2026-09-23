package dev.kumbuka.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed brand constants - NOT theme-reactive. The splash screen's background
 * and app-mark accent stay this exact dark teal / amber in both light and
 * dark mode (Kumbuka.dc.html's splash artboard doesn't theme-swap), so these
 * are deliberately not part of [KbPalette].
 */
object KbBrand {
    val primary = Color(0xFF005860)
    val accent = Color(0xFFDE9C31)
}

/** Theme-reactive design tokens, resolved per light/dark via [LocalKbColors]. */
data class KbPalette(
    val primary: Color,
    val primaryTint: Color,
    val paper: Color,
    val paper2: Color,
    val surface: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val border: Color,
    val warningTint: Color,
    val accent: Color,
)

/** Figma variables kb/primary, kb/paper (node 1:5551) + the app-mark's amber accent. */
val KbLightPalette = KbPalette(
    primary = Color(0xFF005860),
    primaryTint = Color(0xFFD2EEF1),
    paper = Color(0xFFEFF7F7),
    paper2 = Color(0xFFE2EEEE),
    surface = Color(0xFFF9FDFD),
    ink = Color(0xFF0F1D20),
    inkMuted = Color(0xFF546062),
    inkFaint = Color(0xFF899192),
    border = Color(0xFFD0D9DB),
    warningTint = Color(0xFFFFE6D3),
    accent = Color(0xFFDE9C31),
)

/** Dark-mode tokens, converted from Kumbuka.dc.html's themeVars(dark) oklch values. */
val KbDarkPalette = KbPalette(
    primary = Color(0xFF57B5BF),
    primaryTint = Color(0xFF0B2E32),
    paper = Color(0xFF0C1617),
    paper2 = Color(0xFF141F21),
    surface = Color(0xFF192426),
    ink = Color(0xFFE2E9EB),
    inkMuted = Color(0xFF9CA7A9),
    inkFaint = Color(0xFF6B7375),
    border = Color(0xFF2A383B),
    warningTint = Color(0xFF43260A),
    accent = Color(0xFFE8AA4E),
)

/** Provided by [KumbukaTheme]; defaults to light so previews outside the theme still work. */
val LocalKbColors = staticCompositionLocalOf { KbLightPalette }
