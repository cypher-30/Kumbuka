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
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
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
    val language by preferences.language.collectAsState(initial = "en")
    val themeMode by preferences.themeMode.collectAsState(initial = "system")
    val sessionLengthMinutes by preferences.sessionLengthMinutes.collectAsState(initial = 60)
    val schedulerArm by preferences.schedulerArm.collectAsState(initial = "baseline")
    val reminderEnabled by preferences.reminderEnabled.collectAsState(initial = false)
    val reminderHour by preferences.reminderHour.collectAsState(initial = 20)
    val reminderMinute by preferences.reminderMinute.collectAsState(initial = 0)
    val plan = remember(units, topics, sessions, deadlines, sessionLengthMinutes) {
        buildTodayPlan(units = units, topics = topics, sessions = sessions, deadlines = deadlines, sessionLengthMinutes = sessionLengthMinutes)
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
                onStartSession = onStartSession,
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
                onLanguageSelected = { code -> scope.launch { preferences.setLanguage(code) } },
                onThemeModeSelected = { mode -> scope.launch { preferences.setThemeMode(mode) } },
                onSessionLengthSelected = { minutes -> scope.launch { preferences.setSessionLengthMinutes(minutes) } },
                onSchedulerArmSelected = { arm -> scope.launch { preferences.setSchedulerArm(arm) } },
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
