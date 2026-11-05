@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.UUID
import kotlinx.coroutines.launch

/** Parses user-typed decimals, accepting a comma decimal separator too. */
internal fun parseDecimalInput(raw: String): Float? = raw.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }

internal fun formatDecimal(value: Float): String =
    if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

/**
 * Full-screen add/edit for an assessment result. Archived topics already
 * linked to a saved mark stay visible and selectable so history is preserved.
 */
@Composable
fun MarkEditorScreen(
    markId: String?,
    initialUnitId: String?,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    onDone: (message: String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalKbColors.current
    val scope = rememberCoroutineScope()
    val unitsOrNull by unitRepository.observeAll().collectAsState(initial = null)
    val marksOrNull by assessmentMarkRepository.observeAll().collectAsState(initial = null)
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val units = unitsOrNull.orEmpty().sortedBy { it.code }
    val existing = marksOrNull?.firstOrNull { it.id == markId }
    val editing = markId != null

    var initialized by rememberSaveable { mutableStateOf(false) }
    var unitId by rememberSaveable { mutableStateOf<String?>(null) }
    var kindName by rememberSaveable { mutableStateOf(AssessmentKind.CAT.name) }
    var scoreInput by rememberSaveable { mutableStateOf("") }
    var outOfInput by rememberSaveable { mutableStateOf("100") }
    var dateInput by rememberSaveable { mutableStateOf(todayIsoDate()) }
    var selectedTopics by rememberSaveable { mutableStateOf(listOf<String>()) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val ready = unitsOrNull != null && marksOrNull != null
    LaunchedEffect(ready) {
        if (!ready || initialized) return@LaunchedEffect
        if (existing != null) {
            unitId = existing.unitId
            kindName = existing.kind.name
            scoreInput = formatDecimal(existing.score)
            outOfInput = formatDecimal(existing.outOf)
            dateInput = formatEpochDateUtc(existing.date)
            selectedTopics = existing.topicIds
        } else {
            unitId = initialUnitId?.takeIf { id -> units.any { it.id == id } } ?: units.firstOrNull()?.id
        }
        initialized = true
    }

    val topicsForUnit = remember(unitId, topics) { topics.filter { it.unitId == unitId }.sortedBy { it.orderIndex } }
    val selectableTopics = remember(topicsForUnit, selectedTopics) {
        topicsForUnit.filter { !it.archived || it.id in selectedTopics }
    }
    val requestBack = { if (dirty) showDiscard = true else onBack() }
    BackHandler(enabled = dirty) { showDiscard = true }

    val savedText = stringResource(R.string.marks_saved_result)
    val updatedText = stringResource(R.string.marks_updated_result)
    val saveFailedText = stringResource(R.string.marks_save_failed)
    val errorUnitText = stringResource(R.string.marks_error_unit)
    val errorNumberText = stringResource(R.string.marks_error_number)
    val errorRangeText = stringResource(R.string.marks_error_range)
    val errorDateText = stringResource(R.string.marks_error_date)
    val errorTopicsText = stringResource(R.string.marks_error_topics)

    val scoreValue = parseDecimalInput(scoreInput)
    val outOfValue = parseDecimalInput(outOfInput)
    val scoreFieldError = scoreInput.isNotBlank() && (scoreValue == null || scoreValue < 0f || (outOfValue != null && scoreValue > outOfValue))
    val outOfFieldError = outOfInput.isNotBlank() && (outOfValue == null || outOfValue <= 0f)

    Scaffold(
        topBar = {
            KbTopBar(
                title = if (editing) stringResource(R.string.marks_edit_title) else stringResource(R.string.marks_add_result),
                onBack = requestBack,
            )
        },
        containerColor = colors.paper,
    ) { innerPadding ->
        if (!ready) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return@Scaffold
        }
        if (editing && existing == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.mark_not_found), color = colors.inkMuted)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.kbContentWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel(stringResource(R.string.marks_unit_title))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    units.forEach { unit ->
                        FilterChip(
                            selected = unitId == unit.id,
                            onClick = {
                                if (unitId != unit.id) {
                                    unitId = unit.id
                                    selectedTopics = selectedTopics.filter { id -> topics.any { it.id == id && it.unitId == unit.id } }
                                    dirty = true
                                }
                            },
                            label = { KbUnitLabel(unitKey = unit.id, unitCode = unit.code) },
                            colors = editorChipColors(),
                        )
                    }
                }

                FieldLabel(stringResource(R.string.marks_kind_title))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssessmentKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kindName == kind.name,
                            onClick = {
                                kindName = kind.name
                                dirty = true
                            },
                            label = { Text(assessmentKindLabel(kind)) },
                            colors = editorChipColors(),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = {
                            scoreInput = it
                            dirty = true
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = scoreFieldError,
                        label = { Text(stringResource(R.string.marks_score_title)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        value = outOfInput,
                        onValueChange = {
                            outOfInput = it
                            dirty = true
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = outOfFieldError,
                        label = { Text(stringResource(R.string.marks_out_of_title)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    )
                }
                if (scoreValue != null && outOfValue != null && outOfValue > 0f && !scoreFieldError) {
                    Text(
                        stringResource(R.string.mark_editor_percent, scoreValue / outOfValue * 100f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                    )
                }

                OutlinedTextField(
                    value = parseIsoDateMillis(dateInput)?.let { formatStoredDate(it, currentLocale()) } ?: dateInput,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    label = { Text(stringResource(R.string.marks_date_title)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.marks_pick_date))
                        }
                    },
                )

                FieldLabel(stringResource(R.string.marks_topics_title))
                if (selectableTopics.isEmpty()) {
                    Text(stringResource(R.string.marks_topics_empty), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectableTopics.forEach { topic ->
                            val label = if (topic.archived) stringResource(R.string.marks_topic_archived_label, topic.title) else topic.title
                            FilterChip(
                                selected = topic.id in selectedTopics,
                                onClick = {
                                    selectedTopics = if (topic.id in selectedTopics) selectedTopics - topic.id else selectedTopics + topic.id
                                    dirty = true
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = editorChipColors(),
                            )
                        }
                    }
                }

                errorText?.let { KbBanner(it, KbStatus.ERROR) }

                KbPrimaryButton(
                    text = if (editing) stringResource(R.string.marks_update_result) else stringResource(R.string.marks_save_result),
                    enabled = !saving,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    onClick = {
                        val dateMillis = parseIsoDateMillis(dateInput)
                        val kind = runCatching { AssessmentKind.valueOf(kindName) }.getOrDefault(AssessmentKind.CAT)
                        val chosenUnit = unitId
                        errorText = when {
                            chosenUnit.isNullOrBlank() -> errorUnitText
                            scoreValue == null || outOfValue == null -> errorNumberText
                            outOfValue <= 0f || scoreValue < 0f || scoreValue > outOfValue -> errorRangeText
                            dateMillis == null -> errorDateText
                            selectedTopics.isEmpty() -> errorTopicsText
                            else -> null
                        }
                        if (errorText == null && chosenUnit != null && scoreValue != null && outOfValue != null && dateMillis != null) {
                            saving = true
                            scope.launch {
                                runCatching {
                                    assessmentMarkRepository.upsert(
                                        AssessmentMark(
                                            id = existing?.id ?: UUID.randomUUID().toString(),
                                            unitId = chosenUnit,
                                            score = scoreValue,
                                            outOf = outOfValue,
                                            kind = kind,
                                            date = dateMillis,
                                            topicIds = selectedTopics,
                                            updatedAt = System.currentTimeMillis(),
                                        ),
                                    )
                                }.onSuccess {
                                    onDone(if (editing) updatedText else savedText)
                                }.onFailure { t ->
                                    errorText = t.message ?: saveFailedText
                                    saving = false
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = parseIsoDateMillis(dateInput) ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        dateInput = formatEpochDateUtc(it)
                        dirty = true
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.generic_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.generic_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.mark_editor_discard_title)) },
            text = { Text(stringResource(R.string.mark_editor_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.author_discard_confirm), color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.author_discard_cancel)) } },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink, modifier = Modifier.semantics { heading() })
}

@Composable
private fun editorChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LocalKbColors.current.primaryTint,
    selectedLabelColor = LocalKbColors.current.onPrimaryTint,
    containerColor = LocalKbColors.current.surface,
)
