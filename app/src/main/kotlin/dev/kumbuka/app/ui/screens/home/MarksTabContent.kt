package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.domain.scheduler.daysUntilDeadline
import dev.kumbuka.app.domain.scheduler.schedulerUrgency
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbSpacing
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MarksTabContent(
    units: List<UnitModel>,
    topics: List<Topic>,
    deadlines: List<Deadline>,
    marks: List<AssessmentMark>,
    assessmentMarkRepository: AssessmentMarkRepository,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val savedResultText = stringResource(R.string.exams_saved_result)
    val updatedResultText = stringResource(R.string.exams_updated_result)
    val saveFailedText = stringResource(R.string.exams_save_failed)
    val deletedResultText = stringResource(R.string.exams_deleted_result)
    val deleteFailedText = stringResource(R.string.exams_delete_failed)
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val examDeadlines = remember(deadlines) { deadlines.filter { it.kind == DeadlineKind.EXAM }.sortedBy { it.date } }
    val examMarks = remember(marks) { marks.filter { it.kind == AssessmentKind.EXAM }.sortedByDescending { it.date } }
    val groupedDeadlines = remember(examDeadlines, unitCodes) { buildUnitDeadlineBuckets(examDeadlines, unitCodes) }

    var showEntry by rememberSaveable { mutableStateOf(false) }
    var editingMarkId by rememberSaveable { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var savingEntry by remember { mutableStateOf(false) }
    var pendingDeleteMark by remember { mutableStateOf<AssessmentMark?>(null) }
    val editingMark = remember(editingMarkId, examMarks) { examMarks.firstOrNull { it.id == editingMarkId } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.exams_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
                    Text(stringResource(R.string.exams_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                    KbPrimaryButton(
                        text = if (showEntry) stringResource(R.string.exams_hide_entry) else stringResource(R.string.exams_add_result),
                        onClick = {
                            showEntry = !showEntry
                            if (!showEntry) editingMarkId = null
                        },
                    )
                }
            }
        }
        if (showEntry) {
            item {
                ExamResultEntryCard(
                    units = units,
                    topics = topics,
                    existingMark = editingMark,
                    saving = savingEntry,
                    onSave = { mark ->
                        scope.launch {
                            savingEntry = true
                            feedback = null
                            runCatching { assessmentMarkRepository.upsert(mark) }
                                .onSuccess {
                                    feedback = if (editingMarkId == null) savedResultText else updatedResultText
                                    showEntry = false
                                    editingMarkId = null
                                }
                                .onFailure { t -> feedback = t.message ?: saveFailedText }
                            savingEntry = false
                        }
                    },
                    onCancelEdit = {
                        showEntry = false
                        editingMarkId = null
                    },
                )
            }
        }
        feedback?.let { message ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.warningTint), shape = MaterialTheme.shapes.medium) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.accent, modifier = Modifier.padding(12.dp))
                }
            }
        }
        if (groupedDeadlines.isEmpty()) {
            item { EmptyExamsState(onRecordResult = { showEntry = true }) }
        } else {
            groupedDeadlines.forEach { group ->
                item(key = "unit-${group.unitId}") {
                    Text(group.unitCode, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.primary)
                }
                group.buckets.forEach { bucket ->
                    item(key = "bucket-${group.unitId}-${bucket.bucket.name}") {
                        Text(bucketLabel(bucket.bucket), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    }
                    items(bucket.deadlines, key = { it.id }) { deadline ->
                        ExamDeadlineCard(deadline = deadline, unitCode = group.unitCode, topicTitles = deadline.topicIds.mapNotNull { topicsById[it]?.title })
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.exams_recent_results), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
        }
        if (examMarks.isEmpty()) {
            item { Text(stringResource(R.string.exams_no_results_yet), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted) }
        } else {
            items(examMarks, key = { it.id }) { mark ->
                ExamResultCard(
                    mark = mark,
                    unitCode = unitCodes[mark.unitId] ?: mark.unitId,
                    topicTitles = mark.topicIds.mapNotNull { topicsById[it]?.title },
                    onEdit = {
                        editingMarkId = mark.id
                        showEntry = true
                    },
                    onDelete = { pendingDeleteMark = mark },
                )
            }
        }
    }

    pendingDeleteMark?.let { mark ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMark = null },
            title = { Text(stringResource(R.string.exams_delete_title)) },
            text = { Text(stringResource(R.string.exams_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        savingEntry = true
                        feedback = null
                        runCatching { assessmentMarkRepository.delete(mark) }
                            .onSuccess {
                                feedback = deletedResultText
                                if (editingMarkId == mark.id) {
                                    editingMarkId = null
                                    showEntry = false
                                }
                            }
                            .onFailure { t -> feedback = t.message ?: deleteFailedText }
                        pendingDeleteMark = null
                        savingEntry = false
                    }
                }) { Text(stringResource(R.string.exams_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteMark = null }) { Text(stringResource(R.string.exams_delete_cancel)) } },
        )
    }
}

@Composable
private fun EmptyExamsState(onRecordResult: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.exams_empty_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.exams_empty_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            KbPrimaryButton(text = stringResource(R.string.exams_add_result), onClick = onRecordResult)
        }
    }
}

@Composable
private fun ExamDeadlineCard(deadline: Deadline, unitCode: String, topicTitles: List<String>) {
    val now = System.currentTimeMillis()
    val urgency = schedulerUrgency(deadline.date, now)
    val days = daysUntilDeadline(deadline.date, now)
    val urgencyLabel = when {
        urgency >= 0.75f -> stringResource(R.string.exams_urgency_high)
        urgency >= 0.40f -> stringResource(R.string.exams_urgency_medium)
        else -> stringResource(R.string.exams_urgency_low)
    }

    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = LocalKbColors.current.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(deadline.title, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Text(unitCode, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                }
                AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.exams_urgency_chip, urgencyLabel, (urgency * 100).toInt())) })
            }
            Text(stringResource(R.string.exams_deadline_date, formatEpochDate(deadline.date), days), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.primary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ExamResultCard(mark: AssessmentMark, unitCode: String, topicTitles: List<String>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.exams_result_score, mark.score, mark.outOf), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
            Text("$unitCode • ${formatEpochDate(mark.date)}", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.primary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.exams_edit_result)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.exams_delete_result)) }
            }
        }
    }
}

@Composable
private fun ExamResultEntryCard(
    units: List<UnitModel>,
    topics: List<Topic>,
    existingMark: AssessmentMark?,
    saving: Boolean,
    onSave: (AssessmentMark) -> Unit,
    onCancelEdit: () -> Unit,
) {
    var selectedUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    var scoreInput by rememberSaveable { mutableStateOf("") }
    var outOfInput by rememberSaveable { mutableStateOf("100") }
    var dateInput by rememberSaveable { mutableStateOf(todayIsoDate()) }
    val selectedTopicIds = remember { mutableStateListOf<String>() }
    var errorText by remember { mutableStateOf<String?>(null) }

    val unitTopics = remember(selectedUnitId, topics) { topics.filter { it.unitId == selectedUnitId } }
    val errorUnitText = stringResource(R.string.exams_entry_error_unit)
    val errorNumberText = stringResource(R.string.exams_entry_error_number)
    val errorRangeText = stringResource(R.string.exams_entry_error_range)
    val errorDateText = stringResource(R.string.author_error_deadline_date)
    val errorTopicsText = stringResource(R.string.exams_entry_error_topics)

    LaunchedEffect(existingMark?.id, units) {
        selectedUnitId = existingMark?.unitId ?: units.firstOrNull()?.id
        scoreInput = existingMark?.score?.toString().orEmpty()
        outOfInput = existingMark?.outOf?.toString() ?: "100"
        dateInput = existingMark?.let { formatEpochDateUtc(it.date) } ?: todayIsoDate()
        selectedTopicIds.clear()
        selectedTopicIds.addAll(existingMark?.topicIds.orEmpty())
        errorText = null
    }

    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(if (existingMark == null) stringResource(R.string.exams_entry_title) else stringResource(R.string.exams_edit_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.exams_entry_pick_unit), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { unit ->
                    FilterChip(
                        selected = selectedUnitId == unit.id,
                        onClick = {
                            selectedUnitId = unit.id
                            selectedTopicIds.clear()
                        },
                        label = { Text(unit.code) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = scoreInput, onValueChange = { scoreInput = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.exams_entry_score)) })
                OutlinedTextField(value = outOfInput, onValueChange = { outOfInput = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.exams_entry_out_of)) })
            }
            OutlinedTextField(value = dateInput, onValueChange = { dateInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.exams_entry_date)) }, supportingText = { Text(stringResource(R.string.author_deadline_date_hint)) })
            Text(stringResource(R.string.exams_entry_topics), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            if (unitTopics.isEmpty()) {
                Text(stringResource(R.string.exams_entry_topics_empty), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unitTopics.forEach { topic ->
                        FilterChip(
                            selected = selectedTopicIds.contains(topic.id),
                            onClick = {
                                if (selectedTopicIds.contains(topic.id)) selectedTopicIds.remove(topic.id) else selectedTopicIds.add(topic.id)
                            },
                            label = { Text(topic.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            errorText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.accent) }
            KbPrimaryButton(
                text = if (existingMark == null) stringResource(R.string.exams_save_result) else stringResource(R.string.exams_update_result),
                enabled = !saving,
                onClick = {
                    val unitId = selectedUnitId
                    val score = scoreInput.toFloatOrNull()
                    val outOf = outOfInput.toFloatOrNull()
                    val dateMillis = parseIsoDateMillis(dateInput.trim())
                    when {
                        unitId.isNullOrBlank() -> errorText = errorUnitText
                        score == null || outOf == null -> errorText = errorNumberText
                        outOf <= 0f || score < 0f || score > outOf -> errorText = errorRangeText
                        dateMillis == null -> errorText = errorDateText
                        selectedTopicIds.isEmpty() -> errorText = errorTopicsText
                        else -> {
                            errorText = null
                            onSave(
                                AssessmentMark(
                                    id = existingMark?.id ?: UUID.randomUUID().toString(),
                                    unitId = unitId,
                                    score = score,
                                    outOf = outOf,
                                    kind = AssessmentKind.EXAM,
                                    date = dateMillis,
                                    topicIds = selectedTopicIds.toList(),
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                },
            )
            if (existingMark != null) {
                KbSecondaryButton(text = stringResource(R.string.exams_cancel_edit), onClick = onCancelEdit)
            }
        }
    }
}
