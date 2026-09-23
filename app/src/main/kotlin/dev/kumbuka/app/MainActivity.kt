package dev.kumbuka.app

import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dev.kumbuka.app.ui.navigation.KbRoute
import dev.kumbuka.app.ui.navigation.KumbukaNavGraph
import dev.kumbuka.app.ui.theme.KbBrand
import dev.kumbuka.app.ui.theme.KumbukaTheme
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() - installs the native
        // Android 12+ SplashScreen (androidx compat on 26-30). It owns only
        // the very first frame while startRoute is resolving; once resolved,
        // KumbukaNavGraph's own KbRoute.SPLASH plays the fuller icon/wordmark
        // animation before routing to onboarding/Today (restored by request).
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KumbukaApplication
        cachePendingImportFromIntent(intent, app)

        var startRoute by mutableStateOf<String?>(null)
        // Keeps the native splash on screen only while the real routing
        // decision (onboarding complete or not) is still loading - not to
        // force the icon animation to finish, and not an indefinite hold if
        // the read fails.
        splashScreen.setKeepOnScreenCondition { startRoute == null }
        lifecycleScope.launch {
            startRoute = runCatching { app.preferences.onboardingComplete.first() }
                .onFailure { error -> Log.e(TAG, "Failed to read onboarding preference; starting fresh", error) }
                .map { onboardingComplete -> if (onboardingComplete) KbRoute.HOME else KbRoute.ONBOARDING }
                .getOrDefault(KbRoute.ONBOARDING)
        }

        setContent {
            val language by app.preferences.language.collectAsState(initial = "en")
            val themeMode by app.preferences.themeMode.collectAsState(initial = "system")
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }

            KumbukaTheme(darkTheme = darkTheme) {
                Localized(languageCode = language) {
                    val resolvedRoute = startRoute
                    if (resolvedRoute != null) {
                        KumbukaNavGraph(app = app, postSplashDestination = resolvedRoute)
                    } else {
                        // Covered entirely by the still-visible native splash window;
                        // never itself paints a second logo/intro frame.
                        Box(Modifier.fillMaxSize().background(KbBrand.primary))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        cachePendingImportFromIntent(intent, application as KumbukaApplication)
    }

    private fun cachePendingImportFromIntent(intent: Intent?, app: KumbukaApplication) {
        val importUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        persistReadPermissionIfPossible(importUri, intent.flags)
        app.queuePendingImportUri(importUri.toString())
    }

    private fun persistReadPermissionIfPossible(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        if (takeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { error ->
            Log.d(TAG, "Persistable permission not available for $uri", error)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
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
