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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayPlan
import dev.kumbuka.app.domain.scheduler.TodayState
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.domain.scheduler.daysUntilDeadline
import dev.kumbuka.app.domain.scheduler.schedulerUrgency
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onStartSession: (String, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val deadlines by deadlineRepository.observeAll().collectAsState(initial = emptyList())
    val marks by assessmentMarkRepository.observeAll().collectAsState(initial = emptyList())
    val plan = remember(units, topics, sessions, deadlines) {
        buildTodayPlan(units = units, topics = topics, sessions = sessions, deadlines = deadlines)
    }

    val savedResultText = stringResource(R.string.exams_saved_result)
    val updatedResultText = stringResource(R.string.exams_updated_result)
    val saveFailedText = stringResource(R.string.exams_save_failed)
    val deletedResultText = stringResource(R.string.exams_deleted_result)
    val deleteFailedText = stringResource(R.string.exams_delete_failed)

    var activeTab by remember { mutableStateOf(KbNavTab.TONIGHT) }
    var activeWhyCard by remember { mutableStateOf<TodayCard?>(null) }
    var showExamEntry by rememberSaveable { mutableStateOf(false) }
    var editingMarkId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteMark by remember { mutableStateOf<AssessmentMark?>(null) }
    var savingExamResult by remember { mutableStateOf(false) }
    var examFormFeedback by remember { mutableStateOf<String?>(null) }
    val whySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hideEntry = {
        showExamEntry = false
        editingMarkId = null
    }

    val toggleEntry = {
        showExamEntry = !showExamEntry
        if (!showExamEntry) editingMarkId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (activeTab) {
                            KbNavTab.TONIGHT -> plan.headline
                            KbNavTab.EXAMS -> stringResource(R.string.exams_title)
                            KbNavTab.PROGRESS -> stringResource(R.string.nav_progress)
                            KbNavTab.SETTINGS -> stringResource(R.string.nav_settings)
                        },
                    )
                },
                actions = {
                    when (activeTab) {
                        KbNavTab.TONIGHT -> TextButton(onClick = onBrowseUnits) { Text(stringResource(R.string.home_browse_units)) }
                        KbNavTab.EXAMS -> TextButton(onClick = toggleEntry) {
                            Text(if (showExamEntry) stringResource(R.string.exams_hide_entry) else stringResource(R.string.exams_add_result))
                        }
                        else -> Unit
                    }
                },
            )
        },
        bottomBar = { KbBottomNavBar(active = activeTab, onSelect = { activeTab = it }) },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        when (activeTab) {
            KbNavTab.TONIGHT -> TonightTabContent(
                innerPadding = innerPadding,
                plan = plan,
                onBrowseUnits = onBrowseUnits,
                onImportPack = onImportPack,
                onStartSession = onStartSession,
                onWhyThis = { activeWhyCard = it },
            )

            KbNavTab.EXAMS -> ExamsTabContent(
                innerPadding = innerPadding,
                units = units,
                topics = topics,
                deadlines = deadlines,
                marks = marks,
                showEntry = showExamEntry,
                editingMarkId = editingMarkId,
                savingEntry = savingExamResult,
                feedback = examFormFeedback,
                onBrowseUnits = onBrowseUnits,
                onToggleEntry = toggleEntry,
                onCancelEdit = hideEntry,
                onEditMark = { mark ->
                    editingMarkId = mark.id
                    showExamEntry = true
                },
                onDeleteMark = { mark -> pendingDeleteMark = mark },
                onSaveMark = { mark ->
                    scope.launch {
                        savingExamResult = true
                        examFormFeedback = null
                        runCatching { assessmentMarkRepository.upsert(mark) }
                            .onSuccess {
                                examFormFeedback = if (editingMarkId == null) savedResultText else updatedResultText
                                hideEntry()
                            }
                            .onFailure { t -> examFormFeedback = t.message ?: saveFailedText }
                        savingExamResult = false
                    }
                },
            )

            KbNavTab.PROGRESS -> PlaceholderTab(
                innerPadding = innerPadding,
                title = stringResource(R.string.home_progress_placeholder_title),
                body = stringResource(R.string.home_progress_placeholder_body),
            )

            KbNavTab.SETTINGS -> PlaceholderTab(
                innerPadding = innerPadding,
                title = stringResource(R.string.home_settings_placeholder_title),
                body = stringResource(R.string.home_settings_placeholder_body),
            )
        }
    }

    activeWhyCard?.let { card ->
        WhyThisSheet(card = card, onDismiss = { activeWhyCard = null }, sheetState = whySheetState)
    }

    pendingDeleteMark?.let { mark ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMark = null },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text(stringResource(R.string.exams_delete_title)) },
            text = { Text(stringResource(R.string.exams_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        savingExamResult = true
                        examFormFeedback = null
                        runCatching { assessmentMarkRepository.delete(mark) }
                            .onSuccess {
                                examFormFeedback = deletedResultText
                                if (editingMarkId == mark.id) hideEntry()
                                pendingDeleteMark = null
                            }
                            .onFailure { t ->
                                examFormFeedback = t.message ?: deleteFailedText
                                pendingDeleteMark = null
                            }
                        savingExamResult = false
                    }
                }) { Text(stringResource(R.string.exams_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteMark = null }) { Text(stringResource(R.string.exams_delete_cancel)) } },
        )
    }
}

@Composable
private fun TonightTabContent(
    innerPadding: PaddingValues,
    plan: TodayPlan,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onStartSession: (String, Int) -> Unit,
    onWhyThis: (TodayCard) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (plan.state != TodayState.NoUnits) {
            item { SummaryCard(plan = plan, onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
        }
        when (plan.state) {
            TodayState.NoUnits -> item { NoUnitsState(onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
            TodayState.FreshStart -> {
                item { StatePreamble(text = plan.body) }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicCard(card = card, onWhyThis = { onWhyThis(card) }, onStartSession = onStartSession)
                }
                item { BrowseUnitsPrompt(onBrowseUnits) }
            }
            TodayState.Tonight -> {
                item { StatePreamble(text = plan.body) }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicCard(card = card, onWhyThis = { onWhyThis(card) }, onStartSession = onStartSession)
                }
            }
            TodayState.AllCaughtUp -> item { AllCaughtUpState(onBrowseUnits = onBrowseUnits) }
        }
    }
}

@Composable
private fun ExamsTabContent(
    innerPadding: PaddingValues,
    units: List<UnitModel>,
    topics: List<Topic>,
    deadlines: List<Deadline>,
    marks: List<AssessmentMark>,
    showEntry: Boolean,
    editingMarkId: String?,
    savingEntry: Boolean,
    feedback: String?,
    onBrowseUnits: () -> Unit,
    onToggleEntry: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditMark: (AssessmentMark) -> Unit,
    onDeleteMark: (AssessmentMark) -> Unit,
    onSaveMark: (AssessmentMark) -> Unit,
) {
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val examDeadlines = remember(deadlines) { deadlines.filter { it.kind == DeadlineKind.EXAM }.sortedBy { it.date } }
    val examMarks = remember(marks) { marks.filter { it.kind == AssessmentKind.EXAM }.sortedByDescending { it.date } }
    val editingMark = remember(editingMarkId, examMarks) { examMarks.firstOrNull { it.id == editingMarkId } }
    val groupedDeadlines = remember(examDeadlines, unitCodes) { buildUnitDeadlineBuckets(examDeadlines, unitCodes) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.exams_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
                    Text(stringResource(R.string.exams_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KbPrimaryButton(text = stringResource(R.string.exams_add_result), onClick = onToggleEntry, modifier = Modifier.weight(1f))
                        KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits, modifier = Modifier.weight(1f))
                    }
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
                    onSave = onSaveMark,
                    onCancelEdit = onCancelEdit,
                )
            }
        }

        feedback?.let { message ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KbColors.warningTint), shape = RoundedCornerShape(14.dp)) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = KbColors.accent, modifier = Modifier.padding(12.dp))
                }
            }
        }

        if (groupedDeadlines.isEmpty()) {
            item { EmptyExamsState(onRecordResult = onToggleEntry, onBrowseUnits = onBrowseUnits) }
        } else {
            groupedDeadlines.forEach { group ->
                item(key = "unit-${group.unitId}") {
                    Text(group.unitCode, style = MaterialTheme.typography.titleSmall, color = KbColors.primary)
                }
                group.buckets.forEach { bucket ->
                    item(key = "bucket-${group.unitId}-${bucket.bucket.name}") {
                        Text(bucketLabel(bucket.bucket), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                    }
                    items(bucket.deadlines, key = { it.id }) { deadline ->
                        ExamDeadlineCard(
                            deadline = deadline,
                            unitCode = group.unitCode,
                            topicTitles = deadline.topicIds.mapNotNull { topicsById[it]?.title },
                        )
                    }
                }
            }
        }

        item {
            Text(stringResource(R.string.exams_recent_results), style = MaterialTheme.typography.titleSmall, color = KbColors.ink)
        }
        if (examMarks.isEmpty()) {
            item {
                Text(stringResource(R.string.exams_no_results_yet), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
            }
        } else {
            items(examMarks.take(8), key = { it.id }) { mark ->
                ExamResultCard(
                    mark = mark,
                    unitCode = unitCodes[mark.unitId] ?: mark.unitId,
                    topicTitles = mark.topicIds.mapNotNull { topicsById[it]?.title },
                    onEdit = { onEditMark(mark) },
                    onDelete = { onDeleteMark(mark) },
                )
            }
        }
    }
}

@Composable
private fun EmptyExamsState(onRecordResult: () -> Unit, onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.exams_empty_title), style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
            Text(stringResource(R.string.exams_empty_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbPrimaryButton(text = stringResource(R.string.exams_add_result), onClick = onRecordResult)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
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

    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = KbColors.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(deadline.title, style = MaterialTheme.typography.titleSmall, color = KbColors.ink)
                    Text(unitCode, style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.exams_urgency_chip, urgencyLabel, (urgency * 100).toInt())) },
                )
            }
            Text(stringResource(R.string.exams_deadline_date, formatEpochDate(deadline.date), days), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = KbColors.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ExamResultCard(
    mark: AssessmentMark,
    unitCode: String,
    topicTitles: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.exams_result_score, mark.score, mark.outOf), style = MaterialTheme.typography.titleSmall, color = KbColors.ink)
            Text("$unitCode • ${formatEpochDate(mark.date)}", style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
            if (topicTitles.isNotEmpty()) {
                Text(topicTitles.joinToString(), style = MaterialTheme.typography.bodySmall, color = KbColors.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (existingMark == null) stringResource(R.string.exams_entry_title) else stringResource(R.string.exams_edit_title),
                style = MaterialTheme.typography.titleMedium,
                color = KbColors.ink,
            )
            Text(stringResource(R.string.exams_entry_pick_unit), style = MaterialTheme.typography.labelLarge, color = KbColors.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { scoreInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.exams_entry_score)) },
                )
                OutlinedTextField(
                    value = outOfInput,
                    onValueChange = { outOfInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.exams_entry_out_of)) },
                )
            }
            OutlinedTextField(
                value = dateInput,
                onValueChange = { dateInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.exams_entry_date)) },
                supportingText = { Text(stringResource(R.string.author_deadline_date_hint)) },
            )
            Text(stringResource(R.string.exams_entry_topics), style = MaterialTheme.typography.labelLarge, color = KbColors.inkMuted)
            if (unitTopics.isEmpty()) {
                Text(stringResource(R.string.exams_entry_topics_empty), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
            } else {
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

            errorText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = KbColors.accent) }

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
                            scoreInput = ""
                            outOfInput = "100"
                            dateInput = todayIsoDate()
                            selectedTopicIds.clear()
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

@Composable
private fun PlaceholderTab(innerPadding: PaddingValues, title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            }
        }
    }
}

@Composable
private fun SummaryCard(plan: TodayPlan, onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(plan.headline, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(plan.body, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            AssistChip(onClick = onBrowseUnits, label = { Text(stringResource(R.string.today_session_length, plan.sessionLengthMinutes)) })
            KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun StatePreamble(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
}

@Composable
private fun NoUnitsState(onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.units_empty_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(stringResource(R.string.today_no_units_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun BrowseUnitsPrompt(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_keep_going_title), style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
            Text(stringResource(R.string.today_keep_going_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun AllCaughtUpState(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_all_caught_up_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(stringResource(R.string.today_all_caught_up_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun TodayTopicCard(card: TodayCard, onWhyThis: () -> Unit, onStartSession: (String, Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(card.unitCode, style = MaterialTheme.typography.labelLarge, color = KbColors.primary)
                    Text(card.title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
                }
                AssistChip(onClick = onWhyThis, label = { Text("${card.minutes} min") })
            }
            Text(card.objective, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onWhyThis, label = { Text(stringResource(R.string.today_why_this)) }, leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) })
                Text(
                    text = stringResource(R.string.today_score_label, card.score),
                    style = MaterialTheme.typography.bodySmall,
                    color = KbColors.inkMuted,
                    fontWeight = FontWeight.Medium,
                )
            }
            KbPrimaryButton(text = stringResource(R.string.today_start_session), onClick = { onStartSession(card.topicId, card.minutes) })
        }
    }
}

@Composable
private fun WhyThisSheet(card: TodayCard, onDismiss: () -> Unit, sheetState: androidx.compose.material3.SheetState) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = KbColors.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = KbColors.primary)
                Text(stringResource(R.string.today_why_this), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            }
            Text(card.breakdown.formulaSummary(), style = MaterialTheme.typography.bodyMedium, color = KbColors.ink)
            Text(card.breakdown.plainLanguageSummary(), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            BreakdownLine(label = stringResource(R.string.today_gap), value = "${(card.breakdown.gap * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_staleness), value = "${(card.breakdown.staleness * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_urgency), value = "${(card.breakdown.urgency * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_avoidance), value = "${(card.breakdown.avoidance * 100).toInt()}%")
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.generic_ok))
            }
        }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
private val dateFormatterUtc: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

private fun formatEpochDate(epochMillis: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMillis))
private fun formatEpochDateUtc(epochMillis: Long): String = dateFormatterUtc.format(Instant.ofEpochMilli(epochMillis))
private fun todayIsoDate(): String = LocalDate.now().toString()

private fun parseIsoDateMillis(raw: String): Long? = runCatching {
    LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}.getOrNull()

private enum class ExamDateBucket { TODAY, THIS_WEEK, THIS_MONTH, LATER }

private data class UnitDeadlineBucket(
    val unitId: String,
    val unitCode: String,
    val buckets: List<DateBucketGroup>,
)

private data class DateBucketGroup(
    val bucket: ExamDateBucket,
    val deadlines: List<Deadline>,
)

private fun buildUnitDeadlineBuckets(
    deadlines: List<Deadline>,
    unitCodes: Map<String, String>,
    nowMillis: Long = System.currentTimeMillis(),
): List<UnitDeadlineBucket> {
    val ordered = listOf(ExamDateBucket.TODAY, ExamDateBucket.THIS_WEEK, ExamDateBucket.THIS_MONTH, ExamDateBucket.LATER)
    return deadlines
        .groupBy { it.unitId }
        .toList()
        .sortedBy { (unitId, _) -> unitCodes[unitId] ?: unitId }
        .map { (unitId, rows) ->
            val grouped = rows.groupBy { deadline ->
                when (val days = daysUntilDeadline(deadline.date, nowMillis)) {
                    0 -> ExamDateBucket.TODAY
                    in 1..7 -> ExamDateBucket.THIS_WEEK
                    in 8..30 -> ExamDateBucket.THIS_MONTH
                    else -> ExamDateBucket.LATER
                }
            }
            UnitDeadlineBucket(
                unitId = unitId,
                unitCode = unitCodes[unitId] ?: unitId,
                buckets = ordered.mapNotNull { bucket ->
                    grouped[bucket]?.takeIf { it.isNotEmpty() }?.let { DateBucketGroup(bucket, it.sortedBy { d -> d.date }) }
                },
            )
        }
}

@Composable
private fun bucketLabel(bucket: ExamDateBucket): String =
    when (bucket) {
        ExamDateBucket.TODAY -> stringResource(R.string.exams_bucket_today)
        ExamDateBucket.THIS_WEEK -> stringResource(R.string.exams_bucket_this_week)
        ExamDateBucket.THIS_MONTH -> stringResource(R.string.exams_bucket_this_month)
        ExamDateBucket.LATER -> stringResource(R.string.exams_bucket_later)
    }

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
    }
}

