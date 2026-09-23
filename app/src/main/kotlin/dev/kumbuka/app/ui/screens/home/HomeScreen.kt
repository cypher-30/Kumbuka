@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dev.kumbuka.app.R
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.reminders.ReminderScheduler
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SchedulerLogRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.scheduler.PlaceholderRecallPredictor
import dev.kumbuka.app.domain.scheduler.SchedulerArmKind
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.domain.scheduler.evaluateBothArms
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    schedulerLogRepository: SchedulerLogRepository,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onCreateUnit: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onExportUnit: (String) -> Unit,
    onStartSession: (String, Int) -> Unit,
    onOpenInsights: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val deadlines by deadlineRepository.observeAll().collectAsState(initial = emptyList())
    val marks by assessmentMarkRepository.observeAll().collectAsState(initial = emptyList())
    val schedulerLogs by schedulerLogRepository.observeAll().collectAsState(initial = emptyList())
    val language by preferences.language.collectAsState(initial = "en")
    val themeMode by preferences.themeMode.collectAsState(initial = "system")
    val sessionLengthMinutes by preferences.sessionLengthMinutes.collectAsState(initial = 60)
    val schedulerArm by preferences.schedulerArm.collectAsState(initial = "baseline")
    val reminderEnabled by preferences.reminderEnabled.collectAsState(initial = false)
    val reminderHour by preferences.reminderHour.collectAsState(initial = 20)
    val reminderMinute by preferences.reminderMinute.collectAsState(initial = 0)
    val requestedArm = remember(schedulerArm) {
        if (schedulerArm == "placeholder") SchedulerArmKind.PLACEHOLDER else SchedulerArmKind.BASELINE
    }
    val plan = remember(units, topics, sessions, deadlines, sessionLengthMinutes, requestedArm) {
        buildTodayPlan(
            units = units,
            topics = topics,
            sessions = sessions,
            deadlines = deadlines,
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
    val latestSelectedLog = remember(schedulerLogs) {
        schedulerLogs.filter { it.selected }.maxByOrNull { it.evaluatedAt }
    }

    var activeTabName by rememberSaveable { mutableStateOf(KbNavTab.TODAY.name) }
    var pendingReminderEnable by remember { mutableStateOf(false) }
    var reminderPermissionDenied by remember { mutableStateOf(false) }
    val activeTab = remember(activeTabName) { KbNavTab.valueOf(activeTabName) }
    val reminderPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingReminderEnable) {
            scope.launch {
                preferences.setReminderEnabled(true)
                ReminderScheduler.scheduleDaily(context.applicationContext, reminderHour, reminderMinute)
            }
            reminderPermissionDenied = false
        } else if (pendingReminderEnable) {
            reminderPermissionDenied = true
        }
        pendingReminderEnable = false
    }
    val startScheduledSession: (dev.kumbuka.app.domain.scheduler.TodayCard) -> Unit = { card ->
        scope.launch {
            val now = System.currentTimeMillis()
            val (baseline, placeholder) = evaluateBothArms(
                units = units,
                topics = topics,
                sessions = sessions,
                deadlines = deadlines,
                sessionLengthMinutes = sessionLengthMinutes,
                nowMillis = now,
                requestedArm = requestedArm,
                placeholderPredictor = PlaceholderRecallPredictor,
            )
            val planId = schedulerLogRepository.logPlan(
                baseline = baseline,
                placeholder = placeholder,
                unitIdByTopicId = unitIdByTopicId,
                sampleTopicIds = sampleTopicIds,
                evaluatedAt = now,
            )
            val activeSession = sessionRepository.getLatestActiveForTopic(card.topicId)
            if (activeSession == null) {
                sessionRepository.upsert(
                    Session(
                        id = UUID.randomUUID().toString(),
                        topicId = card.topicId,
                        startedAt = now,
                        endedAt = null,
                        plannedMinutes = card.minutes,
                        actualSeconds = 0,
                        confidenceBefore = null,
                        confidenceAfter = null,
                        wasDeferred = false,
                        updatedAt = now,
                        sourcePlanId = planId,
                        sourceArm = card.breakdown.actualSource,
                    ),
                )
            } else if (activeSession.sourcePlanId == null || activeSession.sourceArm == null) {
                sessionRepository.upsert(
                    activeSession.copy(
                        plannedMinutes = activeSession.plannedMinutes.takeIf { it > 0 } ?: card.minutes,
                        updatedAt = now,
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
            TopAppBar(
                title = {
                    Text(
                        when (activeTab) {
                            KbNavTab.TODAY -> stringResource(R.string.nav_today)
                            KbNavTab.UNITS -> stringResource(R.string.nav_units)
                            KbNavTab.MARKS -> stringResource(R.string.nav_marks)
                            KbNavTab.SETTINGS -> stringResource(R.string.nav_settings)
                        },
                    )
                },
                actions = {
                    if (activeTab == KbNavTab.UNITS) {
                        IconButton(onClick = onCreateUnit) {
                            Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.author_start_cta))
                        }
                        IconButton(onClick = onImportPack) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = stringResource(R.string.import_pack_title))
                        }
                    }
                },
            )
        },
        bottomBar = { KbBottomNavBar(active = activeTab, onSelect = { activeTabName = it.name }) },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        when (activeTab) {
            KbNavTab.TODAY -> TodayTabContent(
                plan = plan,
                onBrowseUnits = { activeTabName = KbNavTab.UNITS.name },
                onImportPack = onImportPack,
                onStartSession = startScheduledSession,
                onOpenInsights = onOpenInsights,
                modifier = Modifier.padding(innerPadding),
            )
            KbNavTab.UNITS -> UnitsTabContent(
                units = units,
                topics = topics,
                topicRepository = topicRepository,
                sessionLengthMinutes = sessionLengthMinutes,
                onImportPack = onImportPack,
                onOpenTopic = onOpenTopic,
                onExportUnit = onExportUnit,
                onStartSession = onStartSession,
                modifier = Modifier.padding(innerPadding),
            )
            KbNavTab.MARKS -> MarksTabContent(
                units = units,
                topics = topics,
                deadlines = deadlines,
                marks = marks,
                assessmentMarkRepository = assessmentMarkRepository,
                scope = scope,
                modifier = Modifier.padding(innerPadding),
            )
            KbNavTab.SETTINGS -> SettingsTabContent(
                language = language,
                themeMode = themeMode,
                sessionLengthMinutes = sessionLengthMinutes,
                schedulerArm = schedulerArm,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderPermissionDenied = reminderPermissionDenied,
                latestSchedulerActualSource = latestSelectedLog?.actualSource,
                latestSchedulerFallbackReason = latestSelectedLog?.fallbackReason,
                onLanguageSelected = { code -> scope.launch { preferences.setLanguage(code) } },
                onThemeModeSelected = { mode -> scope.launch { preferences.setThemeMode(mode) } },
                onSessionLengthSelected = { minutes -> scope.launch { preferences.setSessionLengthMinutes(minutes) } },
                onSchedulerArmSelected = { arm -> scope.launch { preferences.setSchedulerArm(arm) } },
                onOpenInsights = onOpenInsights,
                onReminderEnabledChanged = { enabled ->
                    if (!enabled) {
                        scope.launch {
                            preferences.setReminderEnabled(false)
                            ReminderScheduler.cancel(context.applicationContext)
                        }
                        reminderPermissionDenied = false
                    } else {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            pendingReminderEnable = true
                            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scope.launch {
                                preferences.setReminderEnabled(true)
                                ReminderScheduler.scheduleDaily(context.applicationContext, reminderHour, reminderMinute)
                            }
                            reminderPermissionDenied = false
                        }
                    }
                },
                onReminderTimeSelected = { hour, minute ->
                    scope.launch {
                        preferences.setReminderTime(hour, minute)
                        if (reminderEnabled) {
                            ReminderScheduler.scheduleDaily(context.applicationContext, hour, minute)
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
