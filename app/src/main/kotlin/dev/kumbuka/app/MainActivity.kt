package dev.kumbuka.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.kumbuka.app.ui.navigation.KbRoute
import dev.kumbuka.app.ui.navigation.KumbukaNavGraph
import dev.kumbuka.app.ui.theme.KumbukaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KumbukaApplication
        setContent {
            val language by app.preferences.language.collectAsState(initial = "en")
            val themeMode by app.preferences.themeMode.collectAsState(initial = "system")
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            // The splash's background is always the dark brand teal regardless of
            // light/dark theme, so it always needs light (white) status bar icons;
            // every other screen follows the resolved theme.
            val isSplashRoute = backStackEntry?.destination?.route == KbRoute.SPLASH
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !(isSplashRoute || darkTheme)
            }

            KumbukaTheme(darkTheme = darkTheme) {
                Localized(languageCode = language) {
                    KumbukaNavGraph(app = app, navController = navController)
                }
            }
        }
    }
}

/**
 * Per-app language override without a full AppCompat LocaleManager setup:
 * re-derives the Context's resources against the chosen locale, so
 * stringResource() picks up values-sw/ where it exists and falls back to
 * values/ (English) for anything not yet translated (DESIGN.md §5).
 *
 * The replacement Context from createConfigurationContext() isn't chained
 * back to the real Activity, so ambient lookups that walk the Context chain
 * (LocalActivityResultRegistryOwner, LocalOnBackPressedDispatcherOwner) fail
 * once LocalContext is overridden - e.g. rememberLauncherForActivityResult()
 * crashes with "No ActivityResultRegistryOwner was provided". Capture the
 * real owners before the override and re-provide them explicitly so screens
 * further down the tree (like the pack file picker) keep working.
 */
@Composable
private fun Localized(languageCode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current)
    val onBackPressedDispatcherOwner = requireNotNull(LocalOnBackPressedDispatcherOwner.current)
    val configuration = remember(languageCode) {
        Configuration(context.resources.configuration).apply {
            setLocale(Locale(languageCode))
        }
    }
    val localizedContext = remember(configuration) { context.createConfigurationContext(configuration) }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        LocalOnBackPressedDispatcherOwner provides onBackPressedDispatcherOwner,
    ) {
        content()
    }
}
