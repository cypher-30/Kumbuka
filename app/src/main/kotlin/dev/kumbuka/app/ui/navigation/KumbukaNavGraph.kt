package dev.kumbuka.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.kumbuka.app.data.reminders.ReminderScheduler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.kumbuka.app.KumbukaApplication
import dev.kumbuka.app.R
import dev.kumbuka.app.data.bootstrap.DefaultContentSeeder
import dev.kumbuka.app.ui.screens.home.HomeScreen
import dev.kumbuka.app.ui.screens.home.MarkEditorScreen
import dev.kumbuka.app.ui.screens.home.UnitDetailScreen
import dev.kumbuka.app.ui.screens.home.rememberWorkspaceShellState
import dev.kumbuka.app.ui.screens.settings.ResearchDiagnosticsScreen
import dev.kumbuka.app.ui.screens.settings.SettingsScreen
import dev.kumbuka.app.ui.screens.onboarding.FirstUnitChoice
import dev.kumbuka.app.ui.screens.onboarding.OnboardingScreen
import dev.kumbuka.app.ui.screens.session.SessionFlowScreen
import dev.kumbuka.app.ui.screens.packs.CreateUnitScreen
import dev.kumbuka.app.ui.screens.packs.PasteOutlineScreen
import dev.kumbuka.app.ui.screens.packs.ExportPackScreen
import dev.kumbuka.app.ui.screens.packs.ImportPackScreen
import dev.kumbuka.app.ui.screens.packs.TopicDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.ui.screens.session.NextTopic

object KbRoute {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val RESEARCH = "research"
    const val IMPORT_PACK = "import-pack"
    const val AUTHOR_PACK = "author-pack"
    const val PASTE_OUTLINE = "paste-outline"
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val UNIT_DETAIL = "unit/{unitId}"
    const val MARK_EDITOR = "mark-edit?markId={markId}&unitId={unitId}"
    const val EXPORT_PACK = "export-pack/{unitId}"
    const val SESSION = "session/{topicId}/{plannedMinutes}"

    fun topicDetail(topicId: String) = "topic/$topicId"
    fun unitDetail(unitId: String) = "unit/$unitId"
    fun markEditor(markId: String?, unitId: String?) = buildString {
        append("mark-edit")
        val params = listOfNotNull(markId?.let { "markId=$it" }, unitId?.let { "unitId=$it" })
        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
    }
    fun exportPack(unitId: String) = "export-pack/$unitId"
    fun session(topicId: String, plannedMinutes: Int) = "session/$topicId/$plannedMinutes"
}

/**
 * Entry flow: MainActivity resolves the real onboardingComplete value while
 * the native animated splash icon is still on screen (the single launch beat -
 * there is no Compose splash route), then hands this graph a
 * [postSplashDestination] of either [KbRoute.ONBOARDING] (fresh) or
 * [KbRoute.HOME] (returning), which is the graph's start destination. Login
 * was removed entirely - there is no account, no backend, and nothing to
 * sign into.
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
    val shell = rememberWorkspaceShellState()
    val sampleAddedText = stringResource(R.string.import_try_sample_done)
    val saveFailedText = stringResource(R.string.onboarding_save_failed)

    LaunchedEffect(currentRoute, pendingImportUri) {
        if (pendingImportUri == null || currentRoute == null) return@LaunchedEffect
        if (currentRoute == KbRoute.ONBOARDING) return@LaunchedEffect
        if (currentRoute != KbRoute.IMPORT_PACK) {
            navController.navigate(KbRoute.IMPORT_PACK) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = postSplashDestination) {
        composable(KbRoute.ONBOARDING) {
            val language by preferences.language.collectAsState(initial = "en")
            OnboardingScreen(
                currentLanguage = language,
                onLanguageSelected = { code ->
                    scope.launch { preferences.setLanguage(code) }
                },
                onSaveConsent = { participate -> runCatching { preferences.completeOnboarding(participate) } },
                onChoose = { choice ->
                    navController.navigate(KbRoute.HOME) { popUpTo(KbRoute.ONBOARDING) { inclusive = true } }
                    when (choice) {
                        FirstUnitChoice.IMPORT -> navController.navigate(KbRoute.IMPORT_PACK)
                        FirstUnitChoice.CREATE -> navController.navigate(KbRoute.AUTHOR_PACK)
                        FirstUnitChoice.SAMPLE -> scope.launch {
                            runCatching { DefaultContentSeeder.seedSamplePacks(app.packRepository) }
                                .onSuccess { shell.pendingMessage = sampleAddedText }
                                .onFailure { shell.pendingMessage = it.message ?: saveFailedText }
                        }
                        FirstUnitChoice.SKIP -> Unit
                    }
                },
            )
        }
        composable(KbRoute.HOME) {
            HomeScreen(
                shell = shell,
                preferences = app.preferences,
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                deadlineRepository = app.deadlineRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                schedulerLogRepository = app.schedulerLogRepository,
                onImportPack = { navController.navigate(KbRoute.IMPORT_PACK) },
                onCreateUnit = { navController.navigate(KbRoute.AUTHOR_PACK) },
                onPasteOutline = { navController.navigate(KbRoute.PASTE_OUTLINE) },
                onTrySample = {
                    scope.launch {
                        runCatching { DefaultContentSeeder.seedSamplePacks(app.packRepository) }
                            .onSuccess { shell.pendingMessage = sampleAddedText }
                            .onFailure { shell.pendingMessage = it.message ?: saveFailedText }
                    }
                },
                onOpenTopic = { topicId -> navController.navigate(KbRoute.topicDetail(topicId)) },
                onOpenUnit = { unitId -> navController.navigate(KbRoute.unitDetail(unitId)) },
                onExportUnit = { unitId -> navController.navigate(KbRoute.exportPack(unitId)) },
                onOpenSettings = { navController.navigate(KbRoute.SETTINGS) { launchSingleTop = true } },
                onEditMark = { markId, unitId -> navController.navigate(KbRoute.markEditor(markId, unitId)) },
                onStartSession = { topicId, plannedMinutes -> navController.navigate(KbRoute.session(topicId, plannedMinutes)) },
            )
        }
        composable(KbRoute.SETTINGS) {
            SettingsScreen(
                preferences = app.preferences,
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                onBack = { navController.popBackStack() },
                onOpenResearch = { navController.navigate(KbRoute.RESEARCH) },
            )
        }
        composable(KbRoute.RESEARCH) {
            ResearchDiagnosticsScreen(
                preferences = app.preferences,
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                schedulerLogRepository = app.schedulerLogRepository,
                deadlineRepository = app.deadlineRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = KbRoute.UNIT_DETAIL,
            arguments = listOf(navArgument("unitId") { type = NavType.StringType }),
        ) { entry ->
            UnitDetailScreen(
                unitId = requireNotNull(entry.arguments?.getString("unitId")),
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                sessionRepository = app.sessionRepository,
                deadlineRepository = app.deadlineRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                onBack = { navController.popBackStack() },
                onOpenTopic = { topicId -> navController.navigate(KbRoute.topicDetail(topicId)) },
                onExportUnit = { unitId -> navController.navigate(KbRoute.exportPack(unitId)) },
                onShowAssessments = { segment, unitId ->
                    shell.showAssessments(segment, unitId)
                    navController.popBackStack(KbRoute.HOME, inclusive = false)
                },
                onAddMark = { unitId -> navController.navigate(KbRoute.markEditor(null, unitId)) },
            )
        }
        composable(
            route = KbRoute.MARK_EDITOR,
            arguments = listOf(
                navArgument("markId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("unitId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            MarkEditorScreen(
                markId = entry.arguments?.getString("markId"),
                initialUnitId = entry.arguments?.getString("unitId"),
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                assessmentMarkRepository = app.assessmentMarkRepository,
                onDone = { message ->
                    shell.pendingMessage = message
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(KbRoute.AUTHOR_PACK) {
            CreateUnitScreen(
                packRepository = app.packRepository,
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                deadlineRepository = app.deadlineRepository,
                onBack = { navController.popBackStack() },
                onSaved = { unitId, openShare ->
                    navController.popBackStack()
                    navController.navigate(if (openShare) KbRoute.exportPack(unitId) else KbRoute.unitDetail(unitId))
                },
            )
        }
        composable(KbRoute.PASTE_OUTLINE) {
            val createdText = stringResource(R.string.paste_created)
            PasteOutlineScreen(
                unitRepository = app.unitRepository,
                topicRepository = app.topicRepository,
                deadlineRepository = app.deadlineRepository,
                onBack = { navController.popBackStack() },
                onCreated = { unitId, unitCode ->
                    shell.pendingMessage = createdText.replace("%1\$s", unitCode)
                    navController.popBackStack()
                    navController.navigate(KbRoute.unitDetail(unitId))
                },
            )
        }
        composable(KbRoute.IMPORT_PACK) {
            ImportPackScreen(
                packRepository = app.packRepository,
                pendingImportUri = pendingImportUri,
                onPendingImportUriHandled = { handledUri -> app.clearPendingImportUri(handledUri) },
                onBack = { navController.popBackStack() },
                onImported = { message ->
                    shell.pendingMessage = message
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = KbRoute.TOPIC_DETAIL,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
        ) { entry ->
            val sessionLength by preferences.sessionLengthMinutes.collectAsState(initial = 60)
            TopicDetailScreen(
                topicRepository = app.topicRepository,
                unitRepository = app.unitRepository,
                sessionRepository = app.sessionRepository,
                topicId = requireNotNull(entry.arguments?.getString("topicId")),
                sessionLengthMinutes = sessionLength,
                onStartSession = { topicId, minutes -> navController.navigate(KbRoute.session(topicId, minutes)) },
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
            val sessionTopicId = requireNotNull(entry.arguments?.getString("topicId"))
            val reminderEnabled by preferences.reminderEnabled.collectAsState(initial = true)
            val context = LocalContext.current
            val enableReminder: () -> Unit = {
                scope.launch {
                    val hour = preferences.reminderHour.first()
                    val minute = preferences.reminderMinute.first()
                    preferences.setReminderEnabled(true)
                    ReminderScheduler.scheduleDaily(context.applicationContext, hour, minute)
                }
            }
            val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) enableReminder()
            }
            SessionFlowScreen(
                topicRepository = app.topicRepository,
                unitRepository = app.unitRepository,
                sessionRepository = app.sessionRepository,
                topicId = sessionTopicId,
                plannedMinutes = entry.arguments?.getInt("plannedMinutes") ?: 60,
                reminderEnabled = reminderEnabled,
                loadNextTopic = { nextTopicAfter(app, sessionTopicId) },
                onBack = { navController.popBackStack() },
                onFinished = {
                    navController.popBackStack(KbRoute.HOME, inclusive = false)
                },
                onStartNext = { nextId ->
                    shell.pendingStartTopicId = nextId
                    navController.popBackStack(KbRoute.HOME, inclusive = false)
                },
                onEnableReminder = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else enableReminder()
                },
            )
        }
    }
}

/**
 * What Today would recommend next once [justFinishedTopicId] is done. Mirrors the
 * Home plan's inputs (the student's own units, falling back to samples) so the
 * session Done screen and Today agree.
 */
private suspend fun nextTopicAfter(app: KumbukaApplication, justFinishedTopicId: String): NextTopic? {
    val allUnits = app.unitRepository.observeAll().first()
    val planUnits = allUnits.filterNot { it.isSample }.ifEmpty { allUnits }
    val unitIds = planUnits.map { it.id }.toSet()
    val topics = app.topicRepository.getAllOnce().filter { !it.archived && it.unitId in unitIds }
    val sessions = app.sessionRepository.observeAll().first()
    val deadlines = app.deadlineRepository.observeAll().first().filter { it.unitId in unitIds }
    val length = app.preferences.sessionLengthMinutes.first()
    val plan = buildTodayPlan(planUnits, topics, sessions, deadlines, length)
    val card = plan.cards.firstOrNull { it.topicId != justFinishedTopicId } ?: return null
    val unitKey = topics.firstOrNull { it.id == card.topicId }?.unitId ?: card.unitCode
    return NextTopic(card.topicId, card.title, unitKey, card.unitCode, card.minutes)
}
