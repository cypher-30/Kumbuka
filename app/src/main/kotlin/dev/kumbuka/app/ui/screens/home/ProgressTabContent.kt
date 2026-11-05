package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.SchedulerEvaluationRecord
import dev.kumbuka.app.domain.analytics.reduceInsights
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbEmptyState
import dev.kumbuka.app.ui.components.KbListRow
import dev.kumbuka.app.ui.components.KbSectionHeader
import dev.kumbuka.app.ui.components.KbStatTile
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import dev.kumbuka.app.ui.theme.unitMarker
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle

@Composable
fun ProgressTabContent(
    units: List<UnitModel>,
    topics: List<Topic>,
    sessions: List<Session>,
    marks: List<AssessmentMark>,
    schedulerLogs: List<SchedulerEvaluationRecord>,
    today: LocalDate,
    listState: LazyListState,
    onStartStudying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val report = remember(units, topics, sessions, marks, schedulerLogs) { reduceInsights(units, topics, sessions, marks, schedulerLogs) }
    val sampleUnitIds = remember(units) { units.filter { it.isSample }.map { it.id }.toSet() }
    val ownUnits = remember(units) { units.filterNot { it.isSample }.sortedBy { it.code } }
    val ownActiveTopics = remember(topics, sampleUnitIds) { topics.filter { !it.archived && it.unitId !in sampleUnitIds } }
    val sampleTopicIds = remember(topics, sampleUnitIds) { topics.filter { it.unitId in sampleUnitIds }.map { it.id }.toSet() }
    val activity = remember(sessions, today, sampleTopicIds) { recentActivity(sessions, today, ZoneId.systemDefault(), 7, sampleTopicIds) }
    val lastReview = remember(sessions) { lastReviewByTopic(sessions) }
    val reviewedCount = ownActiveTopics.count { it.id in lastReview }
    val latestRatings = remember(sessions, ownActiveTopics) {
        val ids = ownActiveTopics.map { it.id }.toSet()
        sessions.filter { it.endedAt != null && !it.wasDeferred && it.topicId in ids }
            .groupBy { it.topicId }
            .mapNotNull { (_, rows) -> rows.maxBy { it.endedAt!! }.let { it.confidenceAfter ?: it.confidenceBefore } }
    }
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val hasAnyData = report.sessionInsights.completedCount > 0 || report.assessmentInsights.totalAssessmentCount > 0

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "tiles") {
            Column(Modifier.kbContentWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KbStatTile(report.sessionInsights.nonDeferredCount.toString(), stringResource(R.string.progress_tile_reviews), Modifier.weight(1f))
                    KbStatTile(report.sessionInsights.measuredMinutes.toString(), stringResource(R.string.progress_tile_minutes), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KbStatTile("$reviewedCount/${ownActiveTopics.size}", stringResource(R.string.progress_tile_topics), Modifier.weight(1f))
                    KbStatTile(report.assessmentInsights.totalAssessmentCount.toString(), stringResource(R.string.progress_tile_results), Modifier.weight(1f))
                }
            }
        }
        if (!hasAnyData) {
            item(key = "empty") {
                KbEmptyState(
                    icon = Icons.Outlined.Insights,
                    title = stringResource(R.string.progress_empty_title),
                    body = stringResource(if (ownUnits.isEmpty() && sampleUnitIds.isNotEmpty()) R.string.progress_empty_sample_only else R.string.progress_empty),
                    primaryLabel = stringResource(R.string.progress_go_home),
                    onPrimary = onStartStudying,
                    modifier = Modifier.kbContentWidth(),
                )
            }
        }

        item(key = "week-header") { KbSectionHeader(stringResource(R.string.progress_weekly_title), Modifier.kbContentWidth()) }
        item(key = "week") {
            val maxCount = (activity.maxOfOrNull { it.completedReviews } ?: 0).coerceAtLeast(1)
            val total = activity.sumOf { it.completedReviews }
            val summary = activity.joinToString(", ") { day ->
                "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${day.completedReviews}"
            }
            KbSurface(modifier = Modifier.kbContentWidth()) {
                Text(stringResource(R.string.progress_week_total, total), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(top = 8.dp)
                        .clearAndSetSemantics { contentDescription = summary },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    activity.forEach { day ->
                        val isToday = day.date == today
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(day.completedReviews.toString(), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                            Box(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .height((6 + 64 * day.completedReviews / maxCount).dp)
                                    .background(if (isToday) colors.primary else colors.primary.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
                            )
                            Text(
                                day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) colors.ink else colors.inkMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        if (ownUnits.isNotEmpty()) {
            item(key = "coverage-header") {
                KbSectionHeader(stringResource(R.string.progress_coverage_title), Modifier.kbContentWidth(), supporting = stringResource(R.string.progress_coverage_body))
            }
            items(ownUnits, key = { "coverage-${it.id}" }) { unit ->
                val unitTopics = ownActiveTopics.filter { it.unitId == unit.id }
                val reviewed = unitTopics.count { it.id in lastReview }
                val fraction = if (unitTopics.isEmpty()) 0f else reviewed.toFloat() / unitTopics.size
                KbSurface(modifier = Modifier.kbContentWidth(), contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KbUnitLabel(unitKey = unit.id, unitCode = unit.code, emphasized = true, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.progress_coverage_value, reviewed, unitTopics.size), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = colors.unitMarker(unit.id),
                        trackColor = colors.paper2,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }

        if (latestRatings.isNotEmpty()) {
            item(key = "confidence-header") {
                KbSectionHeader(stringResource(R.string.progress_confidence_title), Modifier.kbContentWidth(), supporting = stringResource(R.string.progress_confidence_body))
            }
            item(key = "confidence") {
                val counts = Confidence.entries.associateWith { level -> latestRatings.count { it == level } }
                val palette = mapOf(
                    Confidence.BLANK to colors.error,
                    Confidence.SHAKY to colors.warning,
                    Confidence.OK to colors.primary,
                    Confidence.SOLID to colors.success,
                )
                KbSurface(modifier = Modifier.kbContentWidth()) {
                    Row(Modifier.fillMaxWidth().height(14.dp).background(colors.paper2, CircleShape)) {
                        Confidence.entries.forEach { level ->
                            val count = counts[level] ?: 0
                            if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(palette.getValue(level)))
                        }
                    }
                    Confidence.entries.forEach { level ->
                        LegendRow(color = palette.getValue(level), label = confidenceLabel(level), value = (counts[level] ?: 0).toString())
                    }
                }
            }
        }

        if (report.assessmentInsights.points.isNotEmpty()) {
            item(key = "marks-header") {
                KbSectionHeader(stringResource(R.string.progress_marks_title), Modifier.kbContentWidth(), supporting = stringResource(R.string.insights_same_day_note))
            }
            items(report.assessmentInsights.points, key = { "point-${it.markId}" }) { point ->
                KbListRow(modifier = Modifier.kbContentWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KbUnitLabel(unitKey = point.unitId, unitCode = unitCodes[point.unitId] ?: point.unitId)
                        Text("· ${assessmentKindLabel(point.kind)} · ${formatStoredDate(point.date, locale)}", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(stringResource(R.string.insights_mark_score, point.scorePercent), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    val average = point.averageConfidenceBefore
                    Text(
                        if (average == null) {
                            stringResource(R.string.insights_mark_not_enough_data, point.coveredTopicCount, point.totalLinkedTopicCount)
                        } else {
                            stringResource(R.string.insights_mark_correlation, average, point.coveredTopicCount, point.totalLinkedTopicCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }
            }
        }

        item(key = "privacy") {
            Text(
                stringResource(R.string.insights_overview_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                modifier = Modifier.kbContentWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.ink)
    }
}
