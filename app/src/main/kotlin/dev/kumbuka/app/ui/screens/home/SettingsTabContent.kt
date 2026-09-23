@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.scheduler.PlaceholderRecallPredictor
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.Calendar

@Composable
fun SettingsTabContent(
    language: String,
    themeMode: String,
    sessionLengthMinutes: Int,
    schedulerArm: String,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    reminderPermissionDenied: Boolean,
    latestSchedulerActualSource: String?,
    latestSchedulerFallbackReason: String?,
    onLanguageSelected: (String) -> Unit,
    onThemeModeSelected: (String) -> Unit,
    onSessionLengthSelected: (Int) -> Unit,
    onSchedulerArmSelected: (String) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderTimeSelected: (Int, Int) -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val reminderTimeText = remember(reminderHour, reminderMinute) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
        }.time.let { DateFormat.getTimeFormat(context).format(it) }
    }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showResearchControls by rememberSaveable { mutableStateOf(false) }
    var aboutTapCount by rememberSaveable { mutableStateOf(0) }
    var lastAboutTapAt by rememberSaveable { mutableLongStateOf(0L) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column {
                    SettingsSection(title = stringResource(R.string.settings_language_title)) {
                        OptionChipRow {
                            FilterChip(selected = language == "en", onClick = { onLanguageSelected("en") }, label = { Text("English") })
                            FilterChip(selected = language == "sw", onClick = { onLanguageSelected("sw") }, label = { Text("Kiswahili") })
                        }
                    }
                    SettingsDivider()
                    SettingsSection(title = stringResource(R.string.settings_theme_title)) {
                        OptionChipRow {
                            FilterChip(selected = themeMode == "system", onClick = { onThemeModeSelected("system") }, label = { Text(stringResource(R.string.settings_theme_system)) })
                            FilterChip(selected = themeMode == "light", onClick = { onThemeModeSelected("light") }, label = { Text(stringResource(R.string.settings_theme_light)) })
                            FilterChip(selected = themeMode == "dark", onClick = { onThemeModeSelected("dark") }, label = { Text(stringResource(R.string.settings_theme_dark)) })
                        }
                    }
                    SettingsDivider()
                    SettingsSection(title = stringResource(R.string.settings_session_length_title)) {
                        OptionChipRow {
                            listOf(30, 45, 60, 90).forEach { option ->
                                FilterChip(selected = sessionLengthMinutes == option, onClick = { onSessionLengthSelected(option) }, label = { Text(stringResource(R.string.settings_minutes_short, option)) })
                            }
                        }
                    }
                    SettingsDivider()
                    ReminderSection(
                        reminderEnabled = reminderEnabled,
                        formattedTime = reminderTimeText,
                        reminderPermissionDenied = reminderPermissionDenied,
                        onReminderEnabledChanged = onReminderEnabledChanged,
                        onShowTimePicker = { showTimePicker = true },
                    )
                    SettingsDivider()
                    SettingsClickableRow(
                        title = stringResource(R.string.insights_title),
                        subtitle = stringResource(R.string.settings_insights_subtitle),
                        onClick = onOpenInsights,
                    )
                    SettingsDivider()
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_about_title),
                        subtitle = stringResource(R.string.settings_about_subtitle, "0.1.0"),
                        onClick = {
                            val now = System.currentTimeMillis()
                            aboutTapCount = if (now - lastAboutTapAt <= 1500L) aboutTapCount + 1 else 1
                            lastAboutTapAt = now
                            if (aboutTapCount >= 7) {
                                showResearchControls = true
                                aboutTapCount = 0
                            }
                        },
                    )
                    if (showResearchControls) {
                        SettingsDivider()
                        ResearchControlsSection(
                            schedulerArm = schedulerArm,
                            latestSchedulerActualSource = latestSchedulerActualSource,
                            latestSchedulerFallbackReason = latestSchedulerFallbackReason,
                            onSchedulerArmSelected = onSchedulerArmSelected,
                            onClose = { showResearchControls = false },
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = reminderHour, initialMinute = reminderMinute, is24Hour = is24Hour)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_reminder_time_picker_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onReminderTimeSelected(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.generic_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.marks_delete_cancel)) }
            },
        )
    }
}

@Composable
private fun ReminderSection(
    reminderEnabled: Boolean,
    formattedTime: String,
    reminderPermissionDenied: Boolean,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.reminders_title)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.reminders_time_summary, formattedTime), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink)
                Text(stringResource(R.string.reminders_approximate_caption), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            }
            Switch(checked = reminderEnabled, onCheckedChange = onReminderEnabledChanged)
        }
        TextButton(onClick = onShowTimePicker) {
            Text(stringResource(R.string.settings_pick_time))
        }
        if (reminderPermissionDenied) {
            Text(stringResource(R.string.reminders_permission_denied), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.accent)
        }
    }
}

@Composable
private fun ResearchControlsSection(
    schedulerArm: String,
    latestSchedulerActualSource: String?,
    latestSchedulerFallbackReason: String?,
    onSchedulerArmSelected: (String) -> Unit,
    onClose: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_research_controls_title)) {
        Text(stringResource(R.string.settings_research_controls_body), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
        ResearchLine(stringResource(R.string.settings_research_requested_arm), if (schedulerArm == "placeholder") stringResource(R.string.settings_scheduler_learned) else stringResource(R.string.settings_scheduler_baseline))
        ResearchLine(stringResource(R.string.settings_research_actual_source), latestSchedulerActualSource ?: stringResource(R.string.settings_research_no_data))
        ResearchLine(
            stringResource(R.string.settings_research_predictor),
            stringResource(R.string.comparison_predictor_state, "baseline", "${PlaceholderRecallPredictor.VERSION} (placeholder, not trained)"),
        )
        ResearchLine(stringResource(R.string.settings_research_fallback_state), fallbackLabel(latestSchedulerFallbackReason))
        OptionChipRow {
            FilterChip(selected = schedulerArm == "baseline", onClick = { onSchedulerArmSelected("baseline") }, label = { Text(stringResource(R.string.settings_scheduler_baseline)) })
            FilterChip(selected = schedulerArm == "placeholder", onClick = { onSchedulerArmSelected("placeholder") }, label = { Text(stringResource(R.string.settings_scheduler_learned)) })
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.settings_research_close))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
        content()
    })
}

@Composable
private fun SettingsClickableRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = LocalKbColors.current.border)
}

@Composable
private fun OptionChipRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()), content = content)
}

@Composable
private fun ResearchLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.ink)
    }
}

@Composable
private fun fallbackLabel(reason: String?): String = when (reason) {
    null -> stringResource(R.string.settings_research_fallback_none)
    "cold_start" -> stringResource(R.string.settings_research_fallback_cold_start)
    "predictor_unavailable" -> stringResource(R.string.settings_research_fallback_predictor_unavailable)
    else -> reason
}
