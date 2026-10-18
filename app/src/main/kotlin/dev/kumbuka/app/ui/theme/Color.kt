package dev.kumbuka.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed brand constants - NOT theme-reactive ("Usiku na Jua"). The launch
 * surfaces (native splash, launcher icon) always use Midnight with a Saffron
 * mark, in both light and dark mode. Saffron is reserved for the mark and
 * sun artwork; Clay is the action colour.
 */
object KbBrand {
    val primary = Color(0xFF172238) // Midnight
    val accent = Color(0xFFF0B23E) // Saffron
    val clay = Color(0xFFB4442A)
}

/**
 * Theme-reactive semantic tokens, resolved per light/dark via [LocalKbColors].
 * Every colour has one meaning:
 * - primary (Clay): interactive actions and selection only
 * - brand (Midnight): the hero card, timer card and other expressive surfaces
 * - success / warning / error: feedback and status, always paired with text
 * - accent (Saffron): the sun - decorative highlight and progress fill, never status
 *
 * Token names map 1:1 onto the design prototype's CSS variables:
 * paper=--bg, paper2=--bg2, surface=--sf, inkMuted=--mu, inkFaint=--fa,
 * border=--bd, outlineStrong=--ol, primaryTint=--pt, warningTint=--wac.
 */
data class KbPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryTint: Color,
    val onPrimaryTint: Color,
    val brand: Color,
    val brandTint: Color,
    val paper: Color,
    val paper2: Color,
    val surface: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val border: Color,
    val outlineStrong: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningTint: Color,
    val error: Color,
    val errorContainer: Color,
    val accent: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val scrim: Color,
    val unitMarkers: List<Color>,
    val isDark: Boolean,
)

val KbLightPalette = KbPalette(
    primary = Color(0xFFB4442A),
    onPrimary = Color(0xFFFFFFFF),
    primaryTint = Color(0xFFF7E1D7),
    onPrimaryTint = Color(0xFF74270F),
    brand = Color(0xFF172238),
    brandTint = Color(0xFFE3E7F0),
    paper = Color(0xFFEFE9DD),
    paper2 = Color(0xFFE5DDCE),
    surface = Color(0xFFFBF9F4),
    ink = Color(0xFF1A1C24),
    inkMuted = Color(0xFF5B5E69),
    inkFaint = Color(0xFF6E717C),
    border = Color(0xFFD5CAB7),
    outlineStrong = Color(0xFF8A8478),
    success = Color(0xFF2E6E4E),
    successContainer = Color(0xFFE1EEE5),
    warning = Color(0xFF8A5300),
    warningTint = Color(0xFFFBEBCB),
    error = Color(0xFFA3243B),
    errorContainer = Color(0xFFF9E0E4),
    accent = Color(0xFFF0B23E),
    heroStart = Color(0xFF172238),
    heroEnd = Color(0xFF172238),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFC3CBDB),
    scrim = Color(0x73172238), // rgba(23,34,56,.45)
    unitMarkers = listOf(
        Color(0xFF2E5DA8),
        Color(0xFFB03A6E),
        Color(0xFF0F6C8C),
        Color(0xFF8C5A00),
        Color(0xFF5A6478),
    ),
    isDark = false,
)

val KbDarkPalette = KbPalette(
    primary = Color(0xFFF2906F),
    onPrimary = Color(0xFF3A1206),
    primaryTint = Color(0xFF43231A),
    onPrimaryTint = Color(0xFFFFD9CB),
    brand = Color(0xFF1E2A44),
    brandTint = Color(0xFF26334F),
    paper = Color(0xFF11131A),
    paper2 = Color(0xFF1F222C),
    surface = Color(0xFF191C25),
    ink = Color(0xFFF3EFE8),
    inkMuted = Color(0xFFBAB5AC),
    inkFaint = Color(0xFF9E998F),
    border = Color(0xFF2E313C),
    outlineStrong = Color(0xFF7D7A73),
    success = Color(0xFF7BC79C),
    successContainer = Color(0xFF16301F),
    warning = Color(0xFFF2C46B),
    warningTint = Color(0xFF3A2C10),
    error = Color(0xFFF4A3B1),
    errorContainer = Color(0xFF4A1A24),
    accent = Color(0xFFF0B23E),
    heroStart = Color(0xFF1E2A44),
    heroEnd = Color(0xFF1E2A44),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFC3CBDB),
    scrim = Color(0x99000000), // rgba(0,0,0,.6)
    unitMarkers = listOf(
        Color(0xFF9DB8F0),
        Color(0xFFF2A3C4),
        Color(0xFF7FCBE3),
        Color(0xFFF0C27A),
        Color(0xFFB4BDCC),
    ),
    isDark = true,
)

/**
 * Stable categorical marker for a unit. Derived from the unit's stable id so it
 * never changes between launches; purely an identity cue, always shown next to
 * the unit code - it never encodes urgency, mastery, or performance.
 */
fun KbPalette.unitMarker(unitKey: String): Color =
    unitMarkers[unitMarkerIndex(unitKey, unitMarkers.size)]

fun unitMarkerIndex(unitKey: String, size: Int): Int {
    var hash = 0
    unitKey.forEach { hash = (hash * 31 + it.code) and 0x7FFFFFFF }
    return hash % size
}

/** Provided by [KumbukaTheme]; defaults to light so previews outside the theme still work. */
val LocalKbColors = staticCompositionLocalOf { KbLightPalette }
