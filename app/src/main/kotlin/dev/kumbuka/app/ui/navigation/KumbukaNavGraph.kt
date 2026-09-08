package dev.kumbuka.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.kumbuka.app.KumbukaApplication
import dev.kumbuka.app.ui.screens.auth.LoginScreen
import dev.kumbuka.app.ui.screens.consent.ConsentScreen
import dev.kumbuka.app.ui.screens.home.HomePlaceholderScreen
import dev.kumbuka.app.ui.screens.onboarding.OnboardingScreen
import dev.kumbuka.app.ui.screens.packs.ExportPackScreen
import dev.kumbuka.app.ui.screens.packs.ImportPackScreen
import dev.kumbuka.app.ui.screens.packs.TopicDetailScreen
import dev.kumbuka.app.ui.screens.packs.UnitsListScreen
import dev.kumbuka.app.ui.screens.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object KbRoute {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val CONSENT = "consent"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val UNITS = "units"
    const val IMPORT_PACK = "import-pack"
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val EXPORT_PACK = "export-pack/{unitId}"

    fun topicDetail(topicId: String) = "topic/$topicId"
    fun exportPack(unitId: String) = "export-pack/$unitId"
}

/**
 * Entry flow per the group's decision on the login screens: Splash -> Login
 * (real "not wired up yet" note, working guest path) -> Consent (local-only
 * privacy notice) -> Onboarding (1-3) -> Home. Preferences decide whether a
 * returning user skips straight to Home.
 */
@Composable
fun KumbukaNavGraph(app: KumbukaApplication, navController: NavHostController = rememberNavController()) {
    val preferences = app.preferences
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
            HomePlaceholderScreen(onBrowseUnits = { navController.navigate(KbRoute.UNITS) })
        }
        composable(KbRoute.UNITS) {
            UnitsListScreen(
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                onBack = { navController.popBackStack() },
                onImportPack = { navController.navigate(KbRoute.IMPORT_PACK) },
                onOpenTopic = { topicId -> navController.navigate(KbRoute.topicDetail(topicId)) },
                onExportUnit = { unitId -> navController.navigate(KbRoute.exportPack(unitId)) },
            )
        }
        composable(KbRoute.IMPORT_PACK) {
            ImportPackScreen(
                packRepository = app.packRepository,
                onBack = { navController.popBackStack() },
                onImported = { navController.popBackStack() },
            )
        }
        composable(
            route = KbRoute.TOPIC_DETAIL,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
        ) { entry ->
            TopicDetailScreen(
                topicRepository = app.topicRepository,
                unitRepository = app.unitRepository,
                topicId = requireNotNull(entry.arguments?.getString("topicId")),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = KbRoute.EXPORT_PACK,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType }),
        ) { entry ->
            ExportPackScreen(
                packRepository = app.packRepository,
                unitId = requireNotNull(entry.arguments?.getString("unitId")),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
