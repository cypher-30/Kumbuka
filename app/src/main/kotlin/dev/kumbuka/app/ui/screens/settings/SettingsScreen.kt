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
    onBack: () -> Unit,
    onOpenResearch: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalKbColors.current
    val language by preferences.language.collectAsState(initial = "en")
    val themeMode by preferences.themeMode.collectAsState(initial = "system")
    val sessionLengthMinutes by preferences.sessionLengthMinutes.collectAsState(initial = 60)
    val reminderEnabled by preferences.reminderEnabled.collectAsState(initial = false)
    val reminderHour by preferences.reminderHour.collectAsState(initial = 20)
    val reminderMinute by preferences.reminderMinute.collectAsState(initial = 0)
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

    Scaffold(
        topBar = { KbTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
        containerColor = colors.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "study") {
                SettingsGroup(stringResource(R.string.settings_group_study)) {
                    SettingBlock(stringResource(R.string.settings_session_length_title), stringResource(R.string.settings_session_length_body)) {
                        SegmentedChoice(
                            options = listOf(30, 45, 60, 90),
                            selected = sessionLengthMinutes,
                            label = { stringResource(R.string.settings_minutes_short, it) },
                            onSelect = { minutes -> scope.launch { preferences.setSessionLengthMinutes(minutes) } },
                        )
                    }
                    GroupDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleSmall, color = colors.ink)
                            Text(stringResource(R.string.reminders_approximate_caption), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = onReminderToggle,
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                        )
                    }
                    GroupDivider()
                    NavRow(
                        title = stringResource(R.string.settings_pick_time),
                        value = reminderTimeText,
                        onClick = { showTimePicker = true },
                    )
                    if (reminderPermissionDenied) {
                        KbBanner(stringResource(R.string.reminders_permission_denied), KbStatus.ERROR, Modifier.padding(12.dp))
                    }
                }
            }
            item(key = "appearance") {
                SettingsGroup(stringResource(R.string.settings_group_appearance)) {
                    SettingBlock(stringResource(R.string.settings_theme_title), null) {
                        SegmentedChoice(
                            options = listOf("system", "light", "dark"),
                            selected = themeMode,
                            label = {
                                when (it) {
                                    "light" -> stringResource(R.string.settings_theme_light)
                                    "dark" -> stringResource(R.string.settings_theme_dark)
                                    else -> stringResource(R.string.settings_theme_system)
                                }
                            },
                            onSelect = { mode -> scope.launch { preferences.setThemeMode(mode) } },
                        )
                    }
                    GroupDivider()
                    SettingBlock(stringResource(R.string.settings_language_title), null) {
                        SegmentedChoice(
                            options = listOf("en", "sw"),
                            selected = language,
                            label = { if (it == "sw") "Kiswahili" else "English" },
                            onSelect = { code -> scope.launch { preferences.setLanguage(code) } },
                        )
                    }
                }
            }
            item(key = "about") {
                SettingsGroup(stringResource(R.string.settings_about_title)) {
                    NavRow(
                        title = stringResource(R.string.settings_about_version_title),
                        value = stringResource(R.string.settings_about_subtitle, versionName),
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
                    GroupDivider()
                    Text(
                        stringResource(R.string.settings_privacy_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                    GroupDivider()
                    Text(
                        stringResource(R.string.settings_font_credit),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (researchUnlocked) {
                item(key = "research") {
                    SettingsGroup(stringResource(R.string.settings_research_controls_title)) {
                        NavRow(
                            title = stringResource(R.string.research_title),
                            value = stringResource(R.string.research_entry_body),
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
