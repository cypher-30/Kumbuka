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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.kumbuka.app.KumbukaApplication
import dev.kumbuka.app.ui.screens.home.HomeScreen
import dev.kumbuka.app.ui.screens.home.InsightsScreen
import dev.kumbuka.app.ui.screens.onboarding.OnboardingScreen
import dev.kumbuka.app.ui.screens.session.SessionFlowScreen
import dev.kumbuka.app.ui.screens.packs.PackAuthoringScreen
import dev.kumbuka.app.ui.screens.packs.ExportPackScreen
import dev.kumbuka.app.ui.screens.packs.ImportPackScreen
import dev.kumbuka.app.ui.screens.packs.TopicDetailScreen
import dev.kumbuka.app.ui.screens.splash.SplashAnimationDurationMs
import dev.kumbuka.app.ui.screens.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object KbRoute {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val INSIGHTS = "insights"
    const val IMPORT_PACK = "import-pack"
    const val AUTHOR_PACK = "author-pack"
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val EXPORT_PACK = "export-pack/{unitId}"
    const val SESSION = "session/{topicId}/{plannedMinutes}"

    fun topicDetail(topicId: String) = "topic/$topicId"
    fun exportPack(unitId: String) = "export-pack/$unitId"
    fun session(topicId: String, plannedMinutes: Int) = "session/$topicId/$plannedMinutes"
}

/**
 * Entry flow: MainActivity resolves the real onboardingComplete value while
 * the native splash icon is still on screen, then hands this graph a
 * [postSplashDestination] of either [KbRoute.ONBOARDING] (fresh) or
 * [KbRoute.HOME] (returning). The graph always starts at [KbRoute.SPLASH],
 * which plays the fuller icon/wordmark/tagline choreography once before
 * navigating on to that resolved destination - the native SplashScreen API
 * still owns the very first frame, this is a deliberate second, brief,
 * app-authored beat on top of it. Login was removed entirely - there is no
 * account, no backend, and nothing to sign into.
 */
@Composable
fun KumbukaNavGraph(
    app: KumbukaApplication,
    postSplashDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val preferences = app.preferences
    val scope = rememberCoroutineScope()
    val pendingImportUri = app.pendingImportUri
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, pendingImportUri) {
        if (pendingImportUri == null || currentRoute == null) return@LaunchedEffect
        if (currentRoute == KbRoute.SPLASH || currentRoute == KbRoute.ONBOARDING) return@LaunchedEffect
        if (currentRoute != KbRoute.IMPORT_PACK) {
            navController.navigate(KbRoute.IMPORT_PACK) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = KbRoute.SPLASH) {
        composable(KbRoute.SPLASH) {
            SplashScreen()
            LaunchedEffect(postSplashDestination) {
                delay(SplashAnimationDurationMs)
                navController.navigate(postSplashDestination) {
                    popUpTo(KbRoute.SPLASH) { inclusive = true }
                }
            }
        }
        composable(KbRoute.ONBOARDING) {
            val language by preferences.language.collectAsState(initial = "en")
            OnboardingScreen(
                currentLanguage = language,
                onLanguageSelected = { code ->
                    scope.launch { preferences.setLanguage(code) }
                },
                onSave = { runCatching { preferences.completeOnboarding() } },
                onFinished = {
                    navController.navigate(KbRoute.HOME) { popUpTo(KbRoute.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(KbRoute.HOME) {
            HomeScreen(
                preferences = app.preferences,
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                deadlineRepository = app.deadlineRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                schedulerLogRepository = app.schedulerLogRepository,
                onImportPack = { navController.navigate(KbRoute.IMPORT_PACK) },
                onCreateUnit = { navController.navigate(KbRoute.AUTHOR_PACK) },
                onOpenTopic = { topicId -> navController.navigate(KbRoute.topicDetail(topicId)) },
                onExportUnit = { unitId -> navController.navigate(KbRoute.exportPack(unitId)) },
                onStartSession = { topicId, plannedMinutes -> navController.navigate(KbRoute.session(topicId, plannedMinutes)) },
                onOpenInsights = { navController.navigate(KbRoute.INSIGHTS) },
            )
        }
        composable(KbRoute.INSIGHTS) {
            InsightsScreen(
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                schedulerLogRepository = app.schedulerLogRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(KbRoute.AUTHOR_PACK) {
            PackAuthoringScreen(
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                deadlineRepository = app.deadlineRepository,
                onBack = { navController.popBackStack() },
                onSaved = { unitId -> navController.navigate(KbRoute.exportPack(unitId)) },
            )
        }
        composable(KbRoute.IMPORT_PACK) {
            ImportPackScreen(
                packRepository = app.packRepository,
                pendingImportUri = pendingImportUri,
                onPendingImportUriHandled = { handledUri -> app.clearPendingImportUri(handledUri) },
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
        composable(
            route = KbRoute.SESSION,
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("plannedMinutes") { type = NavType.IntType },
            ),
        ) { entry ->
            SessionFlowScreen(
                topicRepository = app.topicRepository,
                unitRepository = app.unitRepository,
                sessionRepository = app.sessionRepository,
                topicId = requireNotNull(entry.arguments?.getString("topicId")),
                plannedMinutes = entry.arguments?.getInt("plannedMinutes") ?: 60,
                onBack = { navController.popBackStack() },
                onFinished = {
                    navController.popBackStack(KbRoute.HOME, inclusive = false)
                },
            )
        }
    }
}
