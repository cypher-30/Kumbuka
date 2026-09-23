@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbSpacing
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class MarkFeedback(val success: Boolean, val message: String)

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
    val savedResultText = stringResource(R.string.marks_saved_result)
    val updatedResultText = stringResource(R.string.marks_updated_result)
    val saveFailedText = stringResource(R.string.marks_save_failed)
    val deletedResultText = stringResource(R.string.marks_deleted_result)
    val deleteFailedText = stringResource(R.string.marks_delete_failed)
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val sortedMarks = remember(marks) { marks.sortedByDescending { it.date } }
    val groupedDeadlines = remember(deadlines, unitCodes) { buildUnitDeadlineBuckets(deadlines.sortedBy { it.date }, unitCodes) }

    var showEntry by rememberSaveable { mutableStateOf(false) }
    var editingMarkId by rememberSaveable { mutableStateOf<String?>(null) }
    var savingEntry by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<MarkFeedback?>(null) }
    var pendingDeleteMark by remember { mutableStateOf<AssessmentMark?>(null) }
    val editingMark = remember(editingMarkId, sortedMarks) { sortedMarks.firstOrNull { it.id == editingMarkId } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.marks_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
                    Text(stringResource(R.string.marks_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
                    KbPrimaryButton(
                        text = if (showEntry) stringResource(R.string.marks_hide_entry) else stringResource(R.string.marks_add_result),
                        onClick = {
                            feedback = null
                            showEntry = !showEntry
                            if (!showEntry) editingMarkId = null
                        },
                    )
                }
            }
        }
        feedback?.let { current ->
            item { FeedbackBanner(feedback = current) }
        }
        if (showEntry) {
            item {
                MarkEntryCard(
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
                                    feedback = MarkFeedback(
                                        success = true,
                                        message = if (editingMarkId == null) savedResultText else updatedResultText,
                                    )
                                    showEntry = false
                                    editingMarkId = null
                                }
                                .onFailure { t ->
                                    feedback = MarkFeedback(success = false, message = t.message ?: saveFailedText)
                                }
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
        item {
            Text(stringResource(R.string.marks_deadlines_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
        }
        if (groupedDeadlines.isEmpty()) {
            item { StateCard(text = stringResource(R.string.marks_no_deadlines)) }
        } else {
            groupedDeadlines.forEach { group ->
                item(key = "deadline-unit-${group.unitId}") {
                    Text(group.unitCode, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
                }
                group.buckets.forEach { bucket ->
                    item(key = "deadline-bucket-${group.unitId}-${bucket.bucket.name}") {
                        Text(bucketLabel(bucket.bucket), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                    }
                    items(bucket.deadlines, key = { it.id }) { deadline ->
                        DeadlineCard(deadline = deadline, unitCode = group.unitCode, topicTitles = deadline.topicIds.mapNotNull { topicsById[it]?.title })
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.marks_saved_marks_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
        }
        if (sortedMarks.isEmpty()) {
            item { StateCard(text = stringResource(R.string.marks_no_results_yet)) }
        } else {
            items(sortedMarks, key = { it.id }) { mark ->
                MarkCard(
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
            title = { Text(stringResource(R.string.marks_delete_title)) },
            text = { Text(stringResource(R.string.marks_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { assessmentMarkRepository.delete(mark) }
                            .onSuccess {
                                feedback = MarkFeedback(success = true, message = deletedResultText)
                                if (editingMarkId == mark.id) {
                                    editingMarkId = null
                                    showEntry = false
                                }
                            }
                            .onFailure { t ->
                                feedback = MarkFeedback(success = false, message = t.message ?: deleteFailedText)
                            }
                        pendingDeleteMark = null
                    }
                }) { Text(stringResource(R.string.marks_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteMark = null }) { Text(stringResource(R.string.marks_delete_cancel)) } },
        )
    }
}

@Composable
private fun FeedbackBanner(feedback: MarkFeedback) {
    val icon = if (feedback.success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
    val tint = if (feedback.success) LocalKbColors.current.primary else LocalKbColors.current.accent
    val background = if (feedback.success) LocalKbColors.current.primaryTint else LocalKbColors.current.warningTint
    Card(colors = CardDefaults.cardColors(containerColor = background), shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(feedback.message, style = MaterialTheme.typography.bodySmall, color = tint)
        }
    }
}

@Composable
private fun StateCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun DeadlineCard(deadline: Deadline, unitCode: String, topicTitles: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = LocalKbColors.current.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(deadline.title, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Text("$unitCode • ${deadlineKindLabel(deadline.kind)}", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                }
            }
            Text(stringResource(R.string.marks_deadline_date, formatEpochDate(deadline.date)), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.primary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MarkCard(mark: AssessmentMark, unitCode: String, topicTitles: List<String>, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(assessmentKindLabel(mark.kind), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                    Text("$unitCode • ${formatEpochDate(mark.date)}", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                }
                Text(stringResource(R.string.marks_result_score, mark.score, mark.outOf), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
            }
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.marks_edit_result))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.marks_delete_result))
                }
            }
        }
    }
}

@Composable
private fun MarkEntryCard(
    units: List<UnitModel>,
    topics: List<Topic>,
    existingMark: AssessmentMark?,
    saving: Boolean,
    onSave: (AssessmentMark) -> Unit,
    onCancelEdit: () -> Unit,
) {
    val editing = existingMark != null
    var selectedUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedKindName by rememberSaveable { mutableStateOf(AssessmentKind.CAT.name) }
    var scoreInput by rememberSaveable { mutableStateOf("") }
    var outOfInput by rememberSaveable { mutableStateOf("100") }
    var dateInput by rememberSaveable { mutableStateOf(todayIsoDate()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedTopicIds = remember { mutableStateListOf<String>() }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingMark?.id, editing) {
        selectedUnitId = existingMark?.unitId ?: units.firstOrNull()?.id
        selectedKindName = existingMark?.kind?.name ?: AssessmentKind.CAT.name
        scoreInput = existingMark?.score?.toString().orEmpty()
        outOfInput = existingMark?.outOf?.toString() ?: "100"
        dateInput = existingMark?.let { formatEpochDateUtc(it.date) } ?: todayIsoDate()
        selectedTopicIds.clear()
        selectedTopicIds.addAll(existingMark?.topicIds.orEmpty())
        errorText = null
    }

    val topicsForUnit = remember(selectedUnitId, topics) { topics.filter { it.unitId == selectedUnitId }.sortedBy { it.orderIndex } }
    val selectableTopics = remember(topicsForUnit, selectedTopicIds.toList()) {
        val activeTopics = topicsForUnit.filterNot { it.archived }
        val linkedArchived = topicsForUnit.filter { it.archived && it.id in selectedTopicIds }
        (activeTopics + linkedArchived).distinctBy { it.id }
    }
    val errorUnitText = stringResource(R.string.marks_error_unit)
    val errorNumberText = stringResource(R.string.marks_error_number)
    val errorRangeText = stringResource(R.string.marks_error_range)
    val errorDateText = stringResource(R.string.marks_error_date)
    val errorTopicsText = stringResource(R.string.marks_error_topics)

    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(if (editing) stringResource(R.string.marks_edit_title) else stringResource(R.string.marks_entry_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)

            Text(stringResource(R.string.marks_kind_title), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssessmentKind.entries.forEach { kind ->
                    FilterChip(selected = selectedKindName == kind.name, onClick = { selectedKindName = kind.name }, label = { Text(assessmentKindLabel(kind)) })
                }
            }

            Text(stringResource(R.string.marks_unit_title), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { unit ->
                    FilterChip(
                        selected = selectedUnitId == unit.id,
                        onClick = {
                            selectedUnitId = unit.id
                            selectedTopicIds.removeAll { topicId -> topics.none { it.id == topicId && it.unitId == unit.id && it.archived } }
                            selectedTopicIds.removeAll { topicId -> topics.any { it.id == topicId && it.unitId != unit.id } }
                        },
                        label = { Text(unit.code) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = scoreInput, onValueChange = { scoreInput = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.marks_score_title)) })
                OutlinedTextField(value = outOfInput, onValueChange = { outOfInput = it }, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.marks_out_of_title)) })
            }

            OutlinedTextField(
                value = dateInput,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.marks_date_title)) },
                supportingText = { Text(stringResource(R.string.author_deadline_date_hint)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.marks_pick_date))
                    }
                },
            )

            Text(stringResource(R.string.marks_topics_title), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
            if (selectableTopics.isEmpty()) {
                Text(stringResource(R.string.marks_topics_empty), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableTopics.forEach { topic ->
                        val topicLabel = if (topic.archived) {
                            stringResource(R.string.marks_topic_archived_label, topic.title)
                        } else {
                            topic.title
                        }
                        FilterChip(
                            selected = selectedTopicIds.contains(topic.id),
                            onClick = {
                                if (selectedTopicIds.contains(topic.id)) selectedTopicIds.remove(topic.id) else selectedTopicIds.add(topic.id)
                            },
                            label = { Text(topicLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }

            errorText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.accent)
            }

            KbPrimaryButton(
                text = if (editing) stringResource(R.string.marks_update_result) else stringResource(R.string.marks_save_result),
                enabled = !saving,
                onClick = {
                    val unitId = selectedUnitId
                    val score = scoreInput.toFloatOrNull()
                    val outOf = outOfInput.toFloatOrNull()
                    val dateMillis = parseIsoDateMillis(dateInput)
                    val kind = runCatching { AssessmentKind.valueOf(selectedKindName) }.getOrDefault(AssessmentKind.CAT)
                    when {
                        unitId.isNullOrBlank() -> errorText = errorUnitText
                        score == null || outOf == null || !score.isFinite() || !outOf.isFinite() -> errorText = errorNumberText
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
                                    kind = kind,
                                    date = dateMillis,
                                    topicIds = selectedTopicIds.toList(),
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                },
            )
            if (editing) {
                KbSecondaryButton(text = stringResource(R.string.marks_cancel_edit), onClick = onCancelEdit)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = parseIsoDateMillis(dateInput) ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        dateInput = formatEpochDateUtc(selected)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.generic_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.generic_back)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
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

@Composable
private fun deadlineKindLabel(kind: DeadlineKind): String =
    when (kind) {
        DeadlineKind.CAT -> stringResource(R.string.marks_kind_cat)
        DeadlineKind.ASSIGNMENT -> stringResource(R.string.marks_kind_assignment)
        DeadlineKind.EXAM -> stringResource(R.string.marks_kind_exam)
    }
