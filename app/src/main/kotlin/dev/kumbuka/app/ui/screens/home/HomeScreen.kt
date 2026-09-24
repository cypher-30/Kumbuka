package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.kumbuka.app.R
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SchedulerLogRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.domain.scheduler.PlaceholderRecallPredictor
import dev.kumbuka.app.domain.scheduler.SchedulerArmKind
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.domain.scheduler.evaluateBothArms
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.components.KbSettingsAction
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.kbNavTabLabel
import dev.kumbuka.app.ui.components.rememberLocalNow
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Main workspace shell: Home, Units, Assessments and Progress share one
 * scaffold and bottom bar. Settings, unit detail and the mark editor are
 * separate routes reached from here. [shell] is hoisted by the nav graph so
 * those routes can return the user to a particular tab/segment/filter.
 */
@Composable
fun HomeScreen(
    shell: WorkspaceShellState,
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    schedulerLogRepository: SchedulerLogRepository,
    onImportPack: () -> Unit,
    onCreateUnit: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUnit: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onEditMark: (markId: String?, unitId: String?) -> Unit,
    onStartSession: (String, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val unitsOrNull by unitRepository.observeAll().collectAsState(initial = null)
    val units: List<UnitModel> = unitsOrNull.orEmpty()
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val deadlines by deadlineRepository.observeAll().collectAsState(initial = emptyList())
    val marks by assessmentMarkRepository.observeAll().collectAsState(initial = emptyList())
    val schedulerLogs by schedulerLogRepository.observeAll().collectAsState(initial = emptyList())
    val sessionLengthMinutes by preferences.sessionLengthMinutes.collectAsState(initial = 60)
    val schedulerArm by preferences.schedulerArm.collectAsState(initial = "baseline")
    val now = rememberLocalNow()
    val today = now.toLocalDate()

    val activeTopics = remember(topics) { topics.filterNot { it.archived } }
    // Samples are for exploring: once the student has a unit of their own, Home plans only from
    // their real units. Samples stay browsable in Units and Assessments.
    val planUnits = remember(units) { units.filterNot { it.isSample }.ifEmpty { units } }
    val planUnitIds = remember(planUnits) { planUnits.map { it.id }.toSet() }
    val planTopics = remember(activeTopics, planUnitIds) { activeTopics.filter { it.unitId in planUnitIds } }
    val planDeadlines = remember(deadlines, planUnitIds) { deadlines.filter { it.unitId in planUnitIds } }
    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val requestedArm = remember(schedulerArm) {
        if (schedulerArm == "placeholder") SchedulerArmKind.PLACEHOLDER else SchedulerArmKind.BASELINE
    }
    // `now` is a key so the plan re-ranks when the day rolls over or the app resumes.
    val plan = remember(planUnits, planTopics, sessions, planDeadlines, sessionLengthMinutes, requestedArm, now) {
        buildTodayPlan(
            units = planUnits,
            topics = planTopics,
            sessions = sessions,
            deadlines = planDeadlines,
            sessionLengthMinutes = sessionLengthMinutes,
            arm = requestedArm,
            predictor = PlaceholderRecallPredictor,
        )
    }
    val unitIdByTopicId = remember(topics) { topics.associateBy({ it.id }, { it.unitId }) }
    val sampleTopicIds = remember(units, topics) {
        val sampleUnitIds = units.filter { it.isSample }.map { it.id }.toSet()
        topics.filter { it.unitId in sampleUnitIds }.map { it.id }.toSet()
    }
    val recentReviews = remember(sessions, today, sampleTopicIds) {
        recentActivity(sessions, today, ZoneId.systemDefault(), 7, sampleTopicIds).sumOf { it.completedReviews }
    }

    val homeList = rememberLazyListState()
    val unitsList = rememberLazyListState()
    val assessmentsList = rememberLazyListState()
    val progressList = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.marks_deleted_result)
    val deleteFailedText = stringResource(R.string.marks_delete_failed)

    LaunchedEffect(shell.pendingMessage) {
        val message = shell.pendingMessage ?: return@LaunchedEffect
        shell.pendingMessage = null
        snackbarHostState.showSnackbar(message)
    }

    BackHandler(enabled = shell.tab != KbNavTab.HOME) { shell.tab = KbNavTab.HOME }

    val startScheduledSession: (TodayCard) -> Unit = { card ->
        scope.launch {
            val nowMillis = System.currentTimeMillis()
            val (baseline, placeholder) = evaluateBothArms(
                units = planUnits,
                topics = planTopics,
                sessions = sessions,
                deadlines = planDeadlines,
                sessionLengthMinutes = sessionLengthMinutes,
                nowMillis = nowMillis,
                requestedArm = requestedArm,
                placeholderPredictor = PlaceholderRecallPredictor,
            )
            val planId = schedulerLogRepository.logPlan(
                baseline = baseline,
                placeholder = placeholder,
                unitIdByTopicId = unitIdByTopicId,
                sampleTopicIds = sampleTopicIds,
                evaluatedAt = nowMillis,
            )
            val activeSession = sessionRepository.getLatestActiveForTopic(card.topicId)
            if (activeSession == null) {
                sessionRepository.upsert(
                    Session(
                        id = UUID.randomUUID().toString(),
                        topicId = card.topicId,
                        startedAt = nowMillis,
                        endedAt = null,
                        plannedMinutes = card.minutes,
                        actualSeconds = 0,
                        confidenceBefore = null,
                        confidenceAfter = null,
                        wasDeferred = false,
                        updatedAt = nowMillis,
                        sourcePlanId = planId,
                        sourceArm = card.breakdown.actualSource,
                    ),
                )
            } else if (activeSession.sourcePlanId == null || activeSession.sourceArm == null) {
                sessionRepository.upsert(
                    activeSession.copy(
                        plannedMinutes = activeSession.plannedMinutes.takeIf { it > 0 } ?: card.minutes,
                        updatedAt = nowMillis,
                        sourcePlanId = activeSession.sourcePlanId ?: planId,
                        sourceArm = activeSession.sourceArm ?: card.breakdown.actualSource,
                    ),
                )
            }
            onStartSession(card.topicId, card.minutes)
        }
    }

    Scaffold(
        topBar = {
            if (shell.tab != KbNavTab.HOME) {
                KbTopBar(
                    title = kbNavTabLabel(shell.tab),
                    actions = {
                        if (shell.tab == KbNavTab.UNITS) AddUnitMenu(onImportPack = onImportPack, onCreateUnit = onCreateUnit)
                        KbSettingsAction(onOpenSettings = onOpenSettings)
                    },
                )
            }
        },
        bottomBar = { KbBottomNavBar(active = shell.tab, onSelect = { shell.tab = it }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (shell.tab) {
            KbNavTab.HOME -> HomeTabContent(
                now = now,
                loaded = unitsOrNull != null,
                plan = plan,
                topicsById = topicsById,
                unitCodes = unitCodes,
                deadlines = planDeadlines,
                recentReviews = recentReviews,
                listState = homeList,
                onStart = startScheduledSession,
                onOpenSettings = onOpenSettings,
                onImportPack = onImportPack,
                onCreateUnit = onCreateUnit,
                onOpenUnits = { shell.tab = KbNavTab.UNITS },
                onOpenAssessments = { shell.showAssessments(AssessmentSegment.UPCOMING) },
                onOpenProgress = { shell.tab = KbNavTab.PROGRESS },
                modifier = contentModifier,
            )
            KbNavTab.UNITS -> UnitsTabContent(
                units = units,
                activeTopics = activeTopics,
                deadlines = deadlines,
                today = today,
                listState = unitsList,
                onOpenUnit = onOpenUnit,
                onOpenTopic = onOpenTopic,
                onImportPack = onImportPack,
                onCreateUnit = onCreateUnit,
                modifier = contentModifier,
            )
            KbNavTab.ASSESSMENTS -> AssessmentsTabContent(
                units = units,
                deadlines = deadlines,
                marks = marks,
                today = today,
                segment = shell.segment,
                unitFilter = shell.unitFilter,
                listState = assessmentsList,
                onSegmentChange = { shell.segment = it },
                onUnitFilterChange = { shell.unitFilter = it },
                onAddMark = { onEditMark(null, shell.unitFilter) },
                onEditMark = { markId -> onEditMark(markId, null) },
                onDeleteMark = { mark ->
                    scope.launch {
                        runCatching { assessmentMarkRepository.delete(mark) }
                            .onSuccess { shell.pendingMessage = deletedText }
                            .onFailure { t -> shell.pendingMessage = t.message ?: deleteFailedText }
                    }
                },
                onOpenUnits = { shell.tab = KbNavTab.UNITS },
                modifier = contentModifier,
            )
            KbNavTab.PROGRESS -> ProgressTabContent(
                units = units,
                topics = topics,
                sessions = sessions,
                marks = marks,
                schedulerLogs = schedulerLogs,
                today = today,
                listState = progressList,
                onStartStudying = { shell.tab = KbNavTab.HOME },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun AddUnitMenu(onImportPack: () -> Unit, onCreateUnit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.units_add_unit))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_pack_title)) },
                leadingIcon = { Icon(Icons.Outlined.UploadFile, contentDescription = null) },
                onClick = {
                    expanded = false
                    onImportPack()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.author_start_cta)) },
                leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreateUnit()
                },
            )
        }
    }
}
