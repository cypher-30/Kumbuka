package dev.kumbuka.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed brand constants - NOT theme-reactive. The launch surfaces (native
 * splash, Compose splash, launcher icon) always use this deep indigo with a
 * warm amber mark, in both light and dark mode.
 */
object KbBrand {
    val primary = Color(0xFF3730A3)
    val accent = Color(0xFFFBBF24)
}

/**
 * Theme-reactive semantic tokens ("Campus Workspace"), resolved per light/dark
 * via [LocalKbColors]. Every colour has one meaning:
 * - primary: interactive actions and selection only
 * - brand: restrained expressive accents (Home header, illustrations)
 * - success / warning / error: feedback and status, always paired with text
 * - accent: warm decorative highlight (sun/moon artwork), never status
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
    val unitMarkers: List<Color>,
    val isDark: Boolean,
)

val KbLightPalette = KbPalette(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryTint = Color(0xFFE7E5FF),
    onPrimaryTint = Color(0xFF2E2A8A),
    brand = Color(0xFF6D28D9),
    brandTint = Color(0xFFF1E9FF),
    paper = Color(0xFFF6F7FB),
    paper2 = Color(0xFFECEEF6),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF182033),
    inkMuted = Color(0xFF566176),
    inkFaint = Color(0xFF636C80),
    border = Color(0xFFDADEE9),
    outlineStrong = Color(0xFF7D869A),
    success = Color(0xFF167548),
    successContainer = Color(0xFFDDF4E6),
    warning = Color(0xFF8A4F00),
    warningTint = Color(0xFFFFF0D6),
    error = Color(0xFFB42335),
    errorContainer = Color(0xFFFDE3E6),
    accent = Color(0xFFF59E0B),
    heroStart = Color(0xFF3730A3),
    heroEnd = Color(0xFF6D28D9),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFE4E1FF),
    unitMarkers = listOf(
        Color(0xFF4F46E5),
        Color(0xFF0F766E),
        Color(0xFFBE185D),
        Color(0xFF9A4508),
        Color(0xFF0369A1),
        Color(0xFF7C3AED),
        Color(0xFF166534),
        Color(0xFF3F6212),
    ),
    isDark = false,
)

val KbDarkPalette = KbPalette(
    primary = Color(0xFFB9B3FF),
    onPrimary = Color(0xFF1E1A5C),
    primaryTint = Color(0xFF2E2B63),
    onPrimaryTint = Color(0xFFE2DFFF),
    brand = Color(0xFFC4B5FD),
    brandTint = Color(0xFF30245A),
    paper = Color(0xFF10121B),
    paper2 = Color(0xFF242838),
    surface = Color(0xFF1A1D2B),
    ink = Color(0xFFF1F2FA),
    inkMuted = Color(0xFFB8BFD2),
    inkFaint = Color(0xFF9DA5BA),
    border = Color(0xFF343A4F),
    outlineStrong = Color(0xFF7C849A),
    success = Color(0xFF75D9A2),
    successContainer = Color(0xFF15392A),
    warning = Color(0xFFF5C66A),
    warningTint = Color(0xFF3D2E0E),
    error = Color(0xFFFFB3BA),
    errorContainer = Color(0xFF4A1A22),
    accent = Color(0xFFFBBF24),
    heroStart = Color(0xFF2B2670),
    heroEnd = Color(0xFF4C1D95),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFDCD8FF),
    unitMarkers = listOf(
        Color(0xFFA5A0FF),
        Color(0xFF5EEAD4),
        Color(0xFFF9A8D4),
        Color(0xFFFCD34D),
        Color(0xFF7DD3FC),
        Color(0xFFC4B5FD),
        Color(0xFF86EFAC),
        Color(0xFFBEF264),
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
