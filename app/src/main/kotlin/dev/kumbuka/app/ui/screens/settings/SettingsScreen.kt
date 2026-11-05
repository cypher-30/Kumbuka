@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.kumbuka.app.R
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.analytics.ReviewLogExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.kumbuka.app.ui.components.AppMarkGlyph
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.reminders.ReminderScheduler
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    onBack: () -> Unit,
    onOpenResearch: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalKbColors.current
    val language by preferences.language.collectAsState(initial = "en")
    val themeMode by preferences.themeMode.collectAsState(initial = "light")
    val sessionLengthMinutes by preferences.sessionLengthMinutes.collectAsState(initial = 60)
    val reminderEnabled by preferences.reminderEnabled.collectAsState(initial = false)
    val reminderHour by preferences.reminderHour.collectAsState(initial = 20)
    val reminderMinute by preferences.reminderMinute.collectAsState(initial = 0)
    val studyParticipation by preferences.studyParticipation.collectAsState(initial = false)
    val snackbarHostState = remember { SnackbarHostState() }
    var showParticipationDialog by rememberSaveable { mutableStateOf(false) }
    val exportedText = stringResource(R.string.settings_export_done)
    val exportFailedText = stringResource(R.string.settings_export_failed)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val outcome = runCatching {
                    val units = unitRepository.observeAll().first()
                    val topics = topicRepository.observeAll().first()
                    val sampleUnitIds = units.filter { it.isSample || it.isPersonal }.map { it.id }.toSet()
                    val sampleTopicIds = topics.filter { it.unitId in sampleUnitIds }.map { it.id }.toSet()
                    val csv = ReviewLogExporter.build(
                        sessions = sessionRepository.observeAll().first(),
                        marks = assessmentMarkRepository.observeAll().first(),
                        unitIdByTopic = topics.associate { it.id to it.unitId },
                        excludedTopicIds = sampleTopicIds,
                        excludedUnitIds = sampleUnitIds,
                    )
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                    }
                }
                snackbarHostState.showSnackbar(if (outcome.isSuccess) exportedText else exportFailedText)
            }
        }
    }
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }

    var pendingReminderEnable by remember { mutableStateOf(false) }
    var reminderPermissionDenied by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var researchUnlocked by rememberSaveable { mutableStateOf(false) }
    var aboutTapCount by rememberSaveable { mutableIntStateOf(0) }
    var lastAboutTapAt by rememberSaveable { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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
    val onReminderToggle: (Boolean) -> Unit = { enabled ->
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
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scope.launch {
                    preferences.setReminderEnabled(true)
                    ReminderScheduler.scheduleDaily(context.applicationContext, reminderHour, reminderMinute)
                }
                reminderPermissionDenied = false
            }
        }
    }
    val reminderTimeText = remember(reminderHour, reminderMinute) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
        }.time.let { DateFormat.getTimeFormat(context).format(it) }
    }

    val themeLabel: @Composable (String) -> String = {
        when (it) {
            "light" -> stringResource(R.string.settings_theme_light)
            "dark" -> stringResource(R.string.settings_theme_dark)
            else -> stringResource(R.string.settings_theme_system)
        }
    }
    val languageLabel: (String) -> String = { if (it == "sw") "Kiswahili" else "English" }
    var openPicker by rememberSaveable { mutableStateOf<String?>(null) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    var creditsOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { KbTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "intro") {
                Row(
                    modifier = Modifier
                        .kbContentWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.brand)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppMarkGlyph(size = 40.dp, color = colors.accent)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Kumbuka", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.onHero)
                        Text(
                            stringResource(R.string.settings_privacy_body),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = colors.onHeroMuted,
                        )
                    }
                }
            }
            item(key = "study") {
                SettingsGroup(stringResource(R.string.settings_group_study)) {
                    ValueRow(
                        icon = Icons.Outlined.Timer,
                        title = stringResource(R.string.settings_session_length_title),
                        value = stringResource(R.string.settings_minutes_short, sessionLengthMinutes),
                        onClick = { openPicker = "length" },
                    )
                    GroupDivider()
                    ValueRow(
                        icon = Icons.Outlined.NotificationsNone,
                        title = stringResource(R.string.reminders_title),
                        value = if (reminderEnabled) reminderTimeText else null,
                        supporting = stringResource(R.string.reminders_approximate_caption),
                        onClick = { onReminderToggle(!reminderEnabled) },
                        trailing = {
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = onReminderToggle,
                                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                            )
                        },
                    )
                    if (reminderEnabled) {
                        GroupDivider()
                        ValueRow(
                            icon = Icons.Outlined.Schedule,
                            title = stringResource(R.string.settings_pick_time),
                            value = reminderTimeText,
                            onClick = { showTimePicker = true },
                        )
                    }
                    if (reminderPermissionDenied) {
                        KbBanner(stringResource(R.string.reminders_permission_denied), KbStatus.ERROR, Modifier.padding(12.dp))
                    }
                }
            }
            item(key = "appearance") {
                SettingsGroup(stringResource(R.string.settings_group_appearance)) {
                    ValueRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        value = themeLabel(themeMode),
                        onClick = { openPicker = "theme" },
                    )
                    GroupDivider()
                    ValueRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.settings_language_title),
                        value = languageLabel(language),
                        onClick = { openPicker = "language" },
                    )
                }
            }
            item(key = "data") {
                SettingsGroup(stringResource(R.string.settings_group_data)) {
                    ValueRow(
                        icon = Icons.Outlined.Science,
                        title = stringResource(R.string.settings_research_title),
                        value = stringResource(if (studyParticipation) R.string.settings_research_on else R.string.settings_research_off),
                        onClick = { showParticipationDialog = true },
                    )
                    GroupDivider()
                    ValueRow(
                        icon = Icons.Outlined.FileDownload,
                        title = stringResource(R.string.settings_export_title),
                        value = null,
                        supporting = stringResource(if (studyParticipation) R.string.settings_export_hint else R.string.settings_export_join_first),
                        onClick = { if (studyParticipation) exportLauncher.launch("kumbuka-review-log.csv") else showParticipationDialog = true },
                    )
                }
            }
            item(key = "about") {
                SettingsGroup(stringResource(R.string.settings_about_title)) {
                    ValueRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.settings_privacy_title),
                        value = null,
                        onClick = { privacyOpen = true },
                    )
                    GroupDivider()
                    ValueRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = stringResource(R.string.settings_credits_title),
                        value = null,
                        onClick = { creditsOpen = true },
                    )
                    GroupDivider()
                    ValueRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about_version_title),
                        value = versionName,
                        showChevron = false,
                        onClick = {
                            val now = System.currentTimeMillis()
                            aboutTapCount = if (now - lastAboutTapAt <= 1500L) aboutTapCount + 1 else 1
                            lastAboutTapAt = now
                            if (aboutTapCount >= 7) {
                                researchUnlocked = true
                                aboutTapCount = 0
                            }
                        },
                    )
                }
            }
            if (researchUnlocked) {
                item(key = "research") {
                    SettingsGroup(stringResource(R.string.settings_research_controls_title)) {
                        ValueRow(
                            icon = Icons.Outlined.Science,
                            title = stringResource(R.string.research_title),
                            value = null,
                            supporting = stringResource(R.string.research_entry_body),
                            onClick = onOpenResearch,
                        )
                        GroupDivider()
                        TextButton(onClick = { researchUnlocked = false }, modifier = Modifier.padding(8.dp)) {
                            Text(stringResource(R.string.settings_research_close))
                        }
                    }
                }
            }
        }
    }

    when (openPicker) {
        "length" -> ChoiceSheet(
            title = stringResource(R.string.settings_session_length_title),
            supporting = stringResource(R.string.settings_session_length_body),
            options = listOf(30, 45, 60, 90),
            selected = sessionLengthMinutes,
            label = { stringResource(R.string.settings_minutes_short, it) },
            onSelect = { minutes -> scope.launch { preferences.setSessionLengthMinutes(minutes) } },
            onDismiss = { openPicker = null },
        )
        "theme" -> ChoiceSheet(
            title = stringResource(R.string.settings_theme_title),
            supporting = stringResource(R.string.settings_theme_system_hint),
            options = listOf("system", "light", "dark"),
            selected = themeMode,
            label = { themeLabel(it) },
            onSelect = { mode -> scope.launch { preferences.setThemeMode(mode) } },
            onDismiss = { openPicker = null },
        )
        "language" -> ChoiceSheet(
            title = stringResource(R.string.settings_language_title),
            supporting = null,
            options = listOf("en", "sw"),
            selected = language,
            label = { languageLabel(it) },
            onSelect = { code -> scope.launch { preferences.setLanguage(code) } },
            onDismiss = { openPicker = null },
        )
    }
    if (showParticipationDialog) {
        AlertDialog(
            onDismissRequest = { showParticipationDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = colors.surface,
            title = {
                Text(stringResource(if (studyParticipation) R.string.settings_research_withdraw_title else R.string.settings_research_join_title))
            },
            text = {
                Text(stringResource(if (studyParticipation) R.string.settings_research_withdraw_body else R.string.settings_research_join_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { preferences.setStudyParticipation(!studyParticipation) }
                    showParticipationDialog = false
                }) {
                    Text(stringResource(if (studyParticipation) R.string.settings_research_withdraw_confirm else R.string.settings_research_join_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showParticipationDialog = false }) { Text(stringResource(R.string.generic_cancel)) } },
        )
    }
    if (privacyOpen) {
        InfoSheet(stringResource(R.string.settings_privacy_title), stringResource(R.string.settings_privacy_body)) { privacyOpen = false }
    }
    if (creditsOpen) {
        InfoSheet(stringResource(R.string.settings_credits_title), stringResource(R.string.settings_font_credit)) { creditsOpen = false }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_reminder_time_picker_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    scope.launch {
                        preferences.setReminderTime(hour, minute)
                        if (reminderEnabled) ReminderScheduler.scheduleDaily(context.applicationContext, hour, minute)
                    }
                    showTimePicker = false
                }) { Text(stringResource(R.string.generic_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.generic_cancel)) } },
        )
    }
}

@Composable
internal fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKbColors.current
    Column(Modifier.kbContentWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
        KbSurface(contentPadding = 0.dp) {
            Column(content = content)
        }
    }
}

@Composable
internal fun GroupDivider() {
    HorizontalDivider(color = LocalKbColors.current.border, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingBlock(title: String, supporting: String?, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKbColors.current
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
        if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        content()
    }
}

@Composable
internal fun NavRow(title: String, value: String?, onClick: () -> Unit, showChevron: Boolean = true) {
    val colors = LocalKbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
            if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
        if (showChevron) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.inkMuted)
    }
}

@Composable
internal fun <T> SegmentedChoice(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val colors = LocalKbColors.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.primaryTint,
                    activeContentColor = colors.onPrimaryTint,
                    inactiveContainerColor = colors.surface,
                    inactiveContentColor = colors.ink,
                    activeBorderColor = colors.outlineStrong,
                    inactiveBorderColor = colors.outlineStrong,
                ),
                icon = {},
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

/** One settings row: icon tile, title (+ optional supporting line), current value and a chevron or custom trailing control. */
@Composable
internal fun ValueRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
    supporting: String? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalKbColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(colors.primaryTint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimaryTint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp), color = colors.inkMuted)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, maxLines = 1)
            if (showChevron) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.inkMuted)
        }
    }
}

/** Single-choice picker in a bottom sheet; picking applies immediately and closes the sheet. */
@Composable
private fun <T> ChoiceSheet(
    title: String,
    supporting: String?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalKbColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = colors.scrim,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.semantics { heading() })
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            Column(Modifier.padding(top = 8.dp)) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.primaryTint else Color.Transparent)
                            .clickable(role = Role.RadioButton) {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = if (isSelected) colors.onPrimaryTint else colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onPrimaryTint)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSheet(title: String, body: String, onDismiss: () -> Unit) {
    val colors = LocalKbColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = colors.scrim,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.semantics { heading() })
            Text(body, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp), color = colors.inkMuted)
        }
    }
}
