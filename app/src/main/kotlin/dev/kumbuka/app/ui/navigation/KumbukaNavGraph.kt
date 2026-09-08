package dev.kumbuka.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.ui.screens.auth.LoginScreen
import dev.kumbuka.app.ui.screens.consent.ConsentScreen
import dev.kumbuka.app.ui.screens.home.HomePlaceholderScreen
import dev.kumbuka.app.ui.screens.onboarding.OnboardingScreen
import dev.kumbuka.app.ui.screens.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object KbRoute {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val CONSENT = "consent"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

/**
 * Entry flow per the group's decision on the login screens: Splash -> Login
 * (real "not wired up yet" note, working guest path) -> Consent (local-only
 * privacy notice) -> Onboarding (1-3) -> Home. Preferences decide whether a
 * returning user skips straight to Home.
 */
@Composable
fun KumbukaNavGraph(preferences: AppPreferences, navController: NavHostController = rememberNavController()) {
    val language by preferences.language.collectAsState(initial = "en")
    val onboardingComplete by preferences.onboardingComplete.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = KbRoute.SPLASH) {
        composable(KbRoute.SPLASH) {
            // Waits for onboardingComplete's real value (not a fixed timer) before
            // deciding where to go, so a slow first DataStore read never races a
            // returning user back into onboarding they already finished.
            LaunchedEffect(onboardingComplete) {
                val resolved = onboardingComplete ?: return@LaunchedEffect
                delay(900) // minimum time to show the brand moment
                val destination = if (resolved) KbRoute.HOME else KbRoute.LOGIN
                navController.navigate(destination) { popUpTo(KbRoute.SPLASH) { inclusive = true } }
            }
            SplashScreen()
        }
        composable(KbRoute.LOGIN) {
            LoginScreen(
                currentLanguage = language,
                onToggleLanguage = {
                    scope.launch { preferences.setLanguage(if (language == "sw") "en" else "sw") }
                },
                onContinueAsGuest = {
                    scope.launch { preferences.setGuest(true) }
                    navController.navigate(KbRoute.CONSENT) { popUpTo(KbRoute.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(KbRoute.CONSENT) {
            ConsentScreen(
                onAccept = {
                    scope.launch { preferences.setConsentAccepted(true) }
                    navController.navigate(KbRoute.ONBOARDING) { popUpTo(KbRoute.CONSENT) { inclusive = true } }
                },
            )
        }
        composable(KbRoute.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    scope.launch { preferences.setOnboardingComplete(true) }
                    navController.navigate(KbRoute.HOME) { popUpTo(KbRoute.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(KbRoute.HOME) {
            HomePlaceholderScreen()
        }
    }
}
