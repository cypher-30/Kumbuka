package dev.kumbuka.app.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.SchedulerLogRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.analytics.reduceInsights
import dev.kumbuka.app.domain.scheduler.PlaceholderRecallPredictor
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch

/** Hidden research diagnostics: scheduler arm choice and logged-selection counts. Not a student feature. */
@Composable
fun ResearchDiagnosticsScreen(
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    schedulerLogRepository: SchedulerLogRepository,
    onBack: () -> Unit,
) {
    val colors = LocalKbColors.current
    val scope = rememberCoroutineScope()
    val schedulerArm by preferences.schedulerArm.collectAsState(initial = "baseline")
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val marks by assessmentMarkRepository.observeAll().collectAsState(initial = emptyList())
    val logs by schedulerLogRepository.observeAll().collectAsState(initial = emptyList())
    val report = remember(units, topics, sessions, marks, logs) { reduceInsights(units, topics, sessions, marks, logs) }
    val latest = remember(logs) { logs.filter { it.selected }.maxByOrNull { it.evaluatedAt } }
    val scheduler = report.schedulerInsights

    Scaffold(
        topBar = { KbTopBar(title = stringResource(R.string.research_title), onBack = onBack) },
        containerColor = colors.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "notice") {
                KbBanner(stringResource(R.string.settings_research_controls_body), KbStatus.INFO, Modifier.kbContentWidth())
            }
            item(key = "arm") {
                SettingsGroup(stringResource(R.string.settings_scheduler_arm_title)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SegmentedChoice(
                            options = listOf("baseline", "placeholder"),
                            selected = schedulerArm,
                            label = {
                                if (it == "placeholder") stringResource(R.string.settings_scheduler_learned) else stringResource(R.string.settings_scheduler_baseline)
                            },
                            onSelect = { arm -> scope.launch { preferences.setSchedulerArm(arm) } },
                        )
                        Text(stringResource(R.string.comparison_no_model), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    }
                }
            }
            item(key = "latest") {
                SettingsGroup(stringResource(R.string.research_latest_title)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticLine(stringResource(R.string.settings_research_actual_source), latest?.actualSource ?: stringResource(R.string.settings_research_no_data))
                        DiagnosticLine(stringResource(R.string.settings_research_fallback_state), fallbackLabel(latest?.fallbackReason))
                        DiagnosticLine(
                            stringResource(R.string.settings_research_predictor),
                            stringResource(R.string.research_predictor_value, PlaceholderRecallPredictor.VERSION),
                        )
                    }
                }
            }
            item(key = "counts") {
                SettingsGroup(stringResource(R.string.comparison_title)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticLine(stringResource(R.string.insights_requested_baseline), (scheduler.requestedCounts["baseline"] ?: 0).toString())
                        DiagnosticLine(stringResource(R.string.insights_requested_placeholder), (scheduler.requestedCounts["placeholder"] ?: 0).toString())
                        DiagnosticLine(
                            stringResource(R.string.insights_actual_baseline),
                            ((scheduler.actualCounts["baseline"] ?: 0) + (scheduler.actualCounts["baseline_fallback"] ?: 0)).toString(),
                        )
                        DiagnosticLine(stringResource(R.string.insights_actual_placeholder), (scheduler.actualCounts["placeholder"] ?: 0).toString())
                        DiagnosticLine(stringResource(R.string.insights_fallbacks), scheduler.fallbackCount.toString())
                        Text(stringResource(R.string.insights_scheduler_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.ink)
    }
}

@Composable
private fun fallbackLabel(reason: String?): String = when (reason) {
    null -> stringResource(R.string.settings_research_fallback_none)
    "cold_start" -> stringResource(R.string.settings_research_fallback_cold_start)
    "predictor_unavailable" -> stringResource(R.string.settings_research_fallback_predictor_unavailable)
    else -> reason
}
