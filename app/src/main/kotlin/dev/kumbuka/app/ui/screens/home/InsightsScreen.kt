@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.SchedulerLogRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.analytics.AssessmentInsightPoint
import dev.kumbuka.app.domain.analytics.reduceInsights
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.scheduler.PlaceholderRecallPredictor
import dev.kumbuka.app.ui.theme.KbSpacing
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun InsightsScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    schedulerLogRepository: SchedulerLogRepository,
    onBack: () -> Unit,
) {
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val marks by assessmentMarkRepository.observeAll().collectAsState(initial = emptyList())
    val schedulerLogs by schedulerLogRepository.observeAll().collectAsState(initial = emptyList())
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val report = remember(units, topics, sessions, marks, schedulerLogs) {
        reduceInsights(units = units, topics = topics, sessions = sessions, marks = marks, schedulerLogs = schedulerLogs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(KbSpacing.x2),
            verticalArrangement = Arrangement.spacedBy(KbSpacing.x2),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                        Text(stringResource(R.string.insights_overview_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.insights_overview_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                        Text(stringResource(R.string.insights_sessions_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.insights_sessions_summary, report.sessionInsights.completedCount, report.sessionInsights.nonDeferredCount), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.insights_sessions_measured, report.sessionInsights.measuredMinutes, report.sessionInsights.unmeasuredCount), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                        Text(stringResource(R.string.insights_assessments_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.insights_assessments_summary, report.assessmentInsights.totalAssessmentCount), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.insights_same_day_note), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    }
                }
            }
            if (report.assessmentInsights.points.isEmpty()) {
                item { EmptyInsightsCard(text = stringResource(R.string.insights_not_enough_data)) }
            } else {
                items(report.assessmentInsights.points, key = { it.markId }) { point ->
                    AssessmentPointCard(point = point, unitCode = unitCodes[point.unitId] ?: point.unitId)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                        Text(stringResource(R.string.comparison_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.comparison_no_model), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                        Text(
                            stringResource(
                                R.string.comparison_predictor_state,
                                report.schedulerInsights.actualCounts["baseline"]?.toString() ?: "0",
                                report.schedulerInsights.predictorVersion?.let { "$it (placeholder, not trained)" } ?: "placeholder, not trained",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalKbColors.current.inkMuted,
                        )
                        SchedulerLine(label = stringResource(R.string.insights_requested_baseline), value = (report.schedulerInsights.requestedCounts["baseline"] ?: 0).toString())
                        SchedulerLine(label = stringResource(R.string.insights_requested_placeholder), value = (report.schedulerInsights.requestedCounts["placeholder"] ?: 0).toString())
                        SchedulerLine(label = stringResource(R.string.insights_actual_baseline), value = ((report.schedulerInsights.actualCounts["baseline"] ?: 0) + (report.schedulerInsights.actualCounts["baseline_fallback"] ?: 0)).toString())
                        SchedulerLine(label = stringResource(R.string.insights_actual_placeholder), value = (report.schedulerInsights.actualCounts["placeholder"] ?: 0).toString())
                        SchedulerLine(label = stringResource(R.string.insights_fallbacks), value = report.schedulerInsights.fallbackCount.toString())
                        Text(stringResource(R.string.insights_scheduler_note), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentPointCard(point: AssessmentInsightPoint, unitCode: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text("$unitCode • ${assessmentKindLabel(point.kind)}", style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.insights_mark_score, point.scorePercent), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.primary)
            if (point.averageConfidenceBefore == null) {
                Text(stringResource(R.string.insights_mark_not_enough_data, point.coveredTopicCount, point.totalLinkedTopicCount), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            } else {
                Text(
                    stringResource(R.string.insights_mark_correlation, point.averageConfidenceBefore, point.coveredTopicCount, point.totalLinkedTopicCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKbColors.current.inkMuted,
                )
            }
            Text(formatEpochDate(point.date), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkFaint)
        }
    }
}

@Composable
private fun EmptyInsightsCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted, modifier = Modifier.padding(KbSpacing.x2))
    }
}

@Composable
private fun SchedulerLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.ink)
    }
}

@Composable
private fun assessmentKindLabel(kind: AssessmentKind): String =
    when (kind) {
        AssessmentKind.CAT -> stringResource(R.string.marks_kind_cat)
        AssessmentKind.ASSIGNMENT -> stringResource(R.string.marks_kind_assignment)
        AssessmentKind.PAST_PAPER -> stringResource(R.string.marks_kind_past_paper)
        AssessmentKind.EXAM -> stringResource(R.string.marks_kind_exam)
    }
