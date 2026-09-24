package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.ui.components.KbSectionHeader
import dev.kumbuka.app.ui.components.KbStatTile
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTonalButton
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.KbUnitBadge
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.components.rememberLocalNow
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun UnitDetailScreen(
    unitId: String,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onExportUnit: (String) -> Unit,
    onShowAssessments: (AssessmentSegment, String) -> Unit,
    onAddMark: (String) -> Unit,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val unitsOrNull by unitRepository.observeAll().collectAsState(initial = null)
    val unit = unitsOrNull?.firstOrNull { it.id == unitId }
    val topics by remember(unitId) { topicRepository.observeByUnit(unitId) }.collectAsState(initial = emptyList())
    val deadlines by remember(unitId) { deadlineRepository.observeByUnit(unitId) }.collectAsState(initial = emptyList())
    val marks by remember(unitId) { assessmentMarkRepository.observeByUnit(unitId) }.collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val today = rememberLocalNow().toLocalDate()
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val active = remember(topics) { topics.filterNot { it.archived }.sortedBy { it.orderIndex } }
    val archived = remember(topics) { topics.filter { it.archived }.sortedBy { it.orderIndex } }
    val lastReview = remember(sessions) { lastReviewByTopic(sessions) }
    val upcoming = remember(deadlines, today) { upcomingPreview(deadlines, today, limit = 3) }
    val reviewed = active.count { it.id in lastReview }

    Scaffold(
        topBar = {
            KbTopBar(
                title = unit?.code ?: stringResource(R.string.unit_detail_title),
                onBack = onBack,
                actions = {
                    if (unit != null) {
                        IconButton(onClick = { onExportUnit(unit.id) }) {
                            Icon(Icons.Outlined.IosShare, contentDescription = stringResource(R.string.unit_export_pack))
                        }
                    }
                },
            )
        },
        containerColor = colors.paper,
    ) { innerPadding ->
        if (unitsOrNull == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return@Scaffold
        }
        if (unit == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.unit_not_found), color = colors.inkMuted, style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "header") {
                Row(Modifier.kbContentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    KbUnitBadge(unitKey = unit.id, unitCode = unit.code, size = 60.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(unit.title, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.unit_detail_pack_version, unit.packVersion ?: 1),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted,
                            )
                            if (unit.isSample) KbStatusPill(stringResource(R.string.units_sample_pill), KbStatus.NEUTRAL)
                        }
                    }
                }
            }
            item(key = "stats") {
                Row(Modifier.kbContentWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KbStatTile(active.size.toString(), stringResource(R.string.unit_detail_stat_topics), Modifier.weight(1f))
                    KbStatTile("$reviewed/${active.size}", stringResource(R.string.unit_detail_stat_reviewed), Modifier.weight(1f))
                    KbStatTile(marks.size.toString(), stringResource(R.string.unit_detail_stat_results), Modifier.weight(1f))
                }
            }

            item(key = "dates-header") {
                KbSectionHeader(
                    title = stringResource(R.string.unit_detail_dates_title),
                    actionLabel = if (deadlines.isNotEmpty()) stringResource(R.string.generic_view_all) else null,
                    onAction = { onShowAssessments(AssessmentSegment.UPCOMING, unit.id) },
                    modifier = Modifier.kbContentWidth(),
                )
            }
            if (upcoming.isEmpty()) {
                item(key = "dates-empty") {
                    KbSurface(Modifier.kbContentWidth()) {
                        Text(stringResource(R.string.unit_detail_dates_empty), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    }
                }
            } else {
                items(upcoming, key = { "date-${it.id}" }) { deadline ->
                    UpcomingRow(
                        deadline = deadline,
                        unitCode = unit.code,
                        days = daysFromToday(deadline.date, today),
                        onClick = null,
                        linkedTopicCount = deadline.topicIds.size,
                        modifier = Modifier.kbContentWidth(),
                    )
                }
            }

            item(key = "results-header") {
                KbSectionHeader(
                    title = stringResource(R.string.unit_detail_results_title),
                    supporting = pluralStringResource(R.plurals.unit_detail_results_count, marks.size, marks.size),
                    actionLabel = if (marks.isNotEmpty()) stringResource(R.string.generic_view_all) else null,
                    onAction = { onShowAssessments(AssessmentSegment.RESULTS, unit.id) },
                    modifier = Modifier.kbContentWidth(),
                )
            }
            item(key = "add-mark") {
                KbTonalButton(
                    text = stringResource(R.string.marks_add_result),
                    onClick = { onAddMark(unit.id) },
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.kbContentWidth(),
                )
            }

            item(key = "topics-header") {
                KbSectionHeader(
                    title = stringResource(R.string.unit_detail_topics_title),
                    supporting = stringResource(R.string.unit_detail_topics_body),
                    modifier = Modifier.kbContentWidth(),
                )
            }
            if (active.isEmpty()) {
                item(key = "topics-empty") {
                    KbSurface(Modifier.kbContentWidth()) {
                        Text(stringResource(R.string.unit_no_topics), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    }
                }
            }
            items(active, key = { "topic-${it.id}" }) { topic ->
                val last = lastReview[topic.id]
                TopicListRow(
                    topic = topic,
                    supporting = if (last != null) {
                        stringResource(R.string.unit_detail_last_reviewed, formatInstantDate(last, locale))
                    } else {
                        stringResource(R.string.unit_detail_not_reviewed)
                    },
                    onClick = { onOpenTopic(topic.id) },
                    modifier = Modifier.kbContentWidth(),
                )
            }
            if (archived.isNotEmpty()) {
                item(key = "archived-toggle") {
                    TextButton(onClick = { showArchived = !showArchived }, modifier = Modifier.kbContentWidth()) {
                        Text(
                            if (showArchived) {
                                stringResource(R.string.unit_detail_hide_archived)
                            } else {
                                pluralStringResource(R.plurals.unit_detail_show_archived, archived.size, archived.size)
                            },
                        )
                    }
                }
                if (showArchived) {
                    items(archived, key = { "archived-${it.id}" }) { topic ->
                        TopicListRow(
                            topic = topic,
                            supporting = stringResource(R.string.unit_detail_archived_label),
                            onClick = { onOpenTopic(topic.id) },
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
            }
        }
    }
}
