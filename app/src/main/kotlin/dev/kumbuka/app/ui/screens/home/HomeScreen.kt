package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import dev.kumbuka.app.ui.screens.packs.TopicDetailScreen
import dev.kumbuka.app.ui.components.KbNavRail
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExtendedFloatingActionButton
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
    onPasteOutline: () -> Unit,
    onTrySample: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUnit: (String) -> Unit,
    onExportUnit: (String) -> Unit,
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
    // Samples and personal goals never feed the research log.
    val researchExcludedTopicIds = remember(units, topics) {
        val excludedUnitIds = units.filter { it.isSample || it.isPersonal }.map { it.id }.toSet()
        topics.filter { it.unitId in excludedUnitIds }.map { it.id }.toSet()
    }
    val doneToday = remember(sessions, today, planTopics) {
        completedToday(sessions, today, ZoneId.systemDefault(), planTopics.map { it.id }.toSet())
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

    BackHandler(enabled = shell.tab != KbNavTab.TODAY) { shell.tab = KbNavTab.TODAY }

    val startScheduledSessionRef = remember { arrayOfNulls<(TodayCard) -> Unit>(1) }
    LaunchedEffect(shell.pendingStartTopicId, plan) {
        val wanted = shell.pendingStartTopicId ?: return@LaunchedEffect
        val card = plan.cards.firstOrNull { it.topicId == wanted } ?: return@LaunchedEffect
        shell.pendingStartTopicId = null
        startScheduledSessionRef[0]?.invoke(card)
    }

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
                sampleTopicIds = researchExcludedTopicIds,
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
    startScheduledSessionRef[0] = startScheduledSession

    val colors = LocalKbColors.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val wide = widthDp >= 600
    val twoPane = widthDp >= 840 && (shell.tab == KbNavTab.TODAY || shell.tab == KbNavTab.UNITS)

    var addSheetOpen by rememberSaveable { mutableStateOf(false) }
    // Two-pane detail selection. Today shows one topic; Units keeps a small stack of
    // "unit:<id>" / "topic:<id>" entries so a unit's topic can open and go back.
    var todayTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    var unitPaneStack by rememberSaveable { mutableStateOf(ArrayList<String>()) }

    val openTopic: (String) -> Unit = if (twoPane) {
        { id ->
            if (shell.tab == KbNavTab.TODAY) todayTopicId = id else unitPaneStack = ArrayList(unitPaneStack + "topic:$id")
        }
    } else {
        onOpenTopic
    }
    val openUnit: (String) -> Unit = if (twoPane) {
        { id -> unitPaneStack = arrayListOf("unit:$id") }
    } else {
        onOpenUnit
    }

    val tabContent: @Composable (Modifier) -> Unit = { contentModifier ->
        when (shell.tab) {
            KbNavTab.TODAY -> HomeTabContent(
                now = now,
                loaded = unitsOrNull != null,
                plan = plan,
                topicsById = topicsById,
                unitCodes = unitCodes,
                deadlines = planDeadlines,
                doneToday = doneToday,
                selectedTopicId = if (twoPane) todayTopicId ?: plan.cards.firstOrNull()?.topicId else null,
                listState = homeList,
                onStart = startScheduledSession,
                onOpenTopic = openTopic,
                onImportPack = onImportPack,
                onCreateUnit = onCreateUnit,
                onTrySample = onTrySample,
                onOpenUnits = { shell.tab = KbNavTab.UNITS },
                onOpenAssessments = { shell.showAssessments(AssessmentSegment.UPCOMING) },
                modifier = contentModifier,
            )
            KbNavTab.UNITS -> UnitsTabContent(
                units = units,
                activeTopics = activeTopics,
                deadlines = deadlines,
                reviewedTopicIds = remember(sessions) { lastReviewByTopic(sessions).keys },
                today = today,
                listState = unitsList,
                onOpenUnit = openUnit,
                onOpenTopic = openTopic,
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
                onStartStudying = { shell.tab = KbNavTab.TODAY },
                modifier = contentModifier,
            )
        }
    }

    val shellTopBar: @Composable () -> Unit = {
        val activeList = when (shell.tab) {
            KbNavTab.TODAY -> homeList
            KbNavTab.UNITS -> unitsList
            KbNavTab.ASSESSMENTS -> assessmentsList
            KbNavTab.PROGRESS -> progressList
        }
        KbTopBar(
            title = kbNavTabLabel(shell.tab),
            scrolled = activeList.canScrollBackward,
            actions = {
                if (shell.tab == KbNavTab.TODAY) {
                    IconButton(onClick = { shell.tab = KbNavTab.UNITS }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.nav_search))
                    }
                }
                if (shell.tab == KbNavTab.UNITS) {
                    IconButton(onClick = { addSheetOpen = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.units_add_unit))
                    }
                }
                // Wide windows carry Settings in the rail; phones keep it top-right on every tab.
                if (!wide) KbSettingsAction(onOpenSettings = onOpenSettings)
            },
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        if (wide) {
            KbNavRail(active = shell.tab, onSelect = { shell.tab = it }, onOpenSettings = onOpenSettings)
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { if (!twoPane) shellTopBar() },
            bottomBar = { if (!wide) KbBottomNavBar(active = shell.tab, onSelect = { shell.tab = it }) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (shell.tab == KbNavTab.ASSESSMENTS && units.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { onEditMark(null, shell.unitFilter) },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.marks_add_result), fontWeight = FontWeight.Bold) },
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            },
            containerColor = colors.paper,
        ) { innerPadding ->
            if (twoPane) {
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Column(modifier = Modifier.width(TwoPaneListWidth).fillMaxHeight()) {
                        shellTopBar()
                        Box(modifier = Modifier.weight(1f)) { tabContent(Modifier) }
                    }
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val paneRef = if (shell.tab == KbNavTab.TODAY) {
                            (todayTopicId ?: plan.cards.firstOrNull()?.topicId)?.let { "topic:$it" }
                        } else {
                            unitPaneStack.lastOrNull()
                                ?: (units.firstOrNull { !it.isSample } ?: units.firstOrNull())?.let { "unit:${it.id}" }
                        }
                        val canGoBack = shell.tab == KbNavTab.UNITS && unitPaneStack.size > 1
                        val goBack = { unitPaneStack = ArrayList(unitPaneStack.dropLast(1)) }
                        val kind = paneRef?.substringBefore(':')
                        val id = paneRef?.substringAfter(':')
                        when {
                            paneRef != null && kind == "topic" && id != null -> key(paneRef) {
                                TopicDetailScreen(
                                    topicRepository = topicRepository,
                                    unitRepository = unitRepository,
                                    sessionRepository = sessionRepository,
                                    topicId = id,
                                    sessionLengthMinutes = sessionLengthMinutes,
                                    onStartSession = onStartSession,
                                    onBack = goBack,
                                    showBack = canGoBack,
                                )
                            }
                            paneRef != null && kind == "unit" && id != null -> key(paneRef) {
                                UnitDetailScreen(
                                    unitId = id,
                                    unitRepository = unitRepository,
                                    topicRepository = topicRepository,
                                    sessionRepository = sessionRepository,
                                    deadlineRepository = deadlineRepository,
                                    assessmentMarkRepository = assessmentMarkRepository,
                                    onBack = goBack,
                                    onOpenTopic = openTopic,
                                    onExportUnit = onExportUnit,
                                    onShowAssessments = { segment, unitId -> shell.showAssessments(segment, unitId) },
                                    onAddMark = { unitId -> onEditMark(null, unitId) },
                                    showBack = canGoBack,
                                )
                            }
                        }
                    }
                }
            } else {
                tabContent(Modifier.padding(innerPadding))
            }
        }
    }

    if (addSheetOpen) {
        AddUnitSheet(
            onDismiss = { addSheetOpen = false },
            onImportPack = {
                addSheetOpen = false
                onImportPack()
            },
            onCreateUnit = {
                addSheetOpen = false
                onCreateUnit()
            },
            onPasteOutline = {
                addSheetOpen = false
                onPasteOutline()
            },
        )
    }
}

private val TwoPaneListWidth = 440.dp

/** "Add unit" bottom sheet: the single entry point for bringing a unit into the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUnitSheet(onDismiss: () -> Unit, onImportPack: () -> Unit, onCreateUnit: () -> Unit, onPasteOutline: () -> Unit) {
    val colors = LocalKbColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.units_add_unit),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                color = colors.ink,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            AddUnitOption(Icons.Outlined.UploadFile, stringResource(R.string.import_pack_title), onImportPack, divider = true)
            AddUnitOption(Icons.Outlined.ContentPaste, stringResource(R.string.paste_title), onPasteOutline, divider = true)
            AddUnitOption(Icons.Outlined.EditNote, stringResource(R.string.author_start_cta), onCreateUnit, divider = true)
        }
    }
}

@Composable
private fun AddUnitOption(icon: ImageVector, title: String, onClick: () -> Unit, divider: Boolean) {
    val colors = LocalKbColors.current
    Column {
        if (divider) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryTint),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = colors.onPrimaryTint) }
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
        }
    }
}
