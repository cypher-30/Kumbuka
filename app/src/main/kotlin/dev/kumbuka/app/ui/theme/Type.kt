package dev.kumbuka.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.kumbuka.app.R

/** Newsreader for headlines (serif, per Figma), IBM Plex Sans for everything else. */
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
)

val IbmPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

val KumbukaTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.6.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
    ),
)
