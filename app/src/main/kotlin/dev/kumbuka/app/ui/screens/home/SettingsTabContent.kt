package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.Locale

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
    onLanguageSelected: (String) -> Unit,
    onThemeModeSelected: (String) -> Unit,
    onSessionLengthSelected: (Int) -> Unit,
    onSchedulerArmSelected: (String) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minuteOptions = listOf(0, 15, 30, 45)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
                    Text(stringResource(R.string.settings_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = language == "en", onClick = { onLanguageSelected("en") }, label = { Text("English") })
                        FilterChip(selected = language == "sw", onClick = { onLanguageSelected("sw") }, label = { Text("Kiswahili") })
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = themeMode == "system", onClick = { onThemeModeSelected("system") }, label = { Text(stringResource(R.string.settings_theme_system)) })
                        FilterChip(selected = themeMode == "light", onClick = { onThemeModeSelected("light") }, label = { Text(stringResource(R.string.settings_theme_light)) })
                        FilterChip(selected = themeMode == "dark", onClick = { onThemeModeSelected("dark") }, label = { Text(stringResource(R.string.settings_theme_dark)) })
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_session_length_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(30, 45, 60, 90).forEach { option ->
                            FilterChip(selected = sessionLengthMinutes == option, onClick = { onSessionLengthSelected(option) }, label = { Text(stringResource(R.string.settings_minutes_short, option)) })
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_scheduler_arm_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(selected = schedulerArm == "baseline", onClick = { onSchedulerArmSelected("baseline") }, label = { Text(stringResource(R.string.settings_scheduler_baseline)) })
                        FilterChip(selected = schedulerArm == "placeholder", onClick = { onSchedulerArmSelected("placeholder") }, label = { Text(stringResource(R.string.settings_scheduler_learned)) })
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                            Text(stringResource(R.string.reminders_time_summary, formatReminderTime(reminderHour, reminderMinute)), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = onReminderEnabledChanged)
                    }
                    Text(stringResource(R.string.reminders_pick_hour), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(18, 19, 20, 21).forEach { hour ->
                            FilterChip(selected = reminderHour == hour, onClick = { onReminderTimeSelected(hour, reminderMinute) }, label = { Text(String.format(Locale.US, "%02d:00", hour)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        minuteOptions.forEach { minute ->
                            FilterChip(selected = reminderMinute == minute, onClick = { onReminderTimeSelected(reminderHour, minute) }, label = { Text(String.format(Locale.US, ":%02d", minute)) })
                        }
                    }
                    if (reminderPermissionDenied) {
                        Text(stringResource(R.string.reminders_permission_denied), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.accent)
                    }
                }
            }
        }
    }
}

private fun formatReminderTime(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)
