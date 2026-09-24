@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbEmptyState
import dev.kumbuka.app.ui.components.KbListRow
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun AssessmentsTabContent(
    units: List<UnitModel>,
    deadlines: List<Deadline>,
    marks: List<AssessmentMark>,
    today: LocalDate,
    segment: AssessmentSegment,
    unitFilter: String?,
    listState: LazyListState,
    onSegmentChange: (AssessmentSegment) -> Unit,
    onUnitFilterChange: (String?) -> Unit,
    onAddMark: () -> Unit,
    onEditMark: (String) -> Unit,
    onDeleteMark: (AssessmentMark) -> Unit,
    onOpenUnits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val effectiveFilter = unitFilter?.takeIf { id -> units.any { it.id == id } }
    val filteredDeadlines = remember(deadlines, effectiveFilter) { deadlines.filter { effectiveFilter == null || it.unitId == effectiveFilter } }
    val sections = remember(filteredDeadlines, today) { groupAssessmentDates(filteredDeadlines, today) }
    val filteredMarks = remember(marks, effectiveFilter) {
        marks.filter { effectiveFilter == null || it.unitId == effectiveFilter }.sortedByDescending { it.date }
    }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "segments") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.kbContentWidth()) {
                AssessmentSegment.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = segment == option,
                        onClick = { onSegmentChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AssessmentSegment.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primaryTint,
                            activeContentColor = colors.onPrimaryTint,
                            inactiveContainerColor = colors.surface,
                            inactiveContentColor = colors.ink,
                            activeBorderColor = colors.outlineStrong,
                            inactiveBorderColor = colors.outlineStrong,
                        ),
                    ) {
                        Text(
                            when (option) {
                                AssessmentSegment.UPCOMING -> stringResource(R.string.assessments_segment_dates)
                                AssessmentSegment.RESULTS -> stringResource(R.string.assessments_segment_results)
                            },
                        )
                    }
                }
            }
        }
        if (units.size > 1) {
            item(key = "filters") {
                Row(
                    modifier = Modifier.kbContentWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = effectiveFilter == null,
                        onClick = { onUnitFilterChange(null) },
                        label = { Text(stringResource(R.string.assessments_all_units)) },
                        colors = chipColors(),
                    )
                    units.sortedBy { it.code }.forEach { unit ->
                        FilterChip(
                            selected = effectiveFilter == unit.id,
                            onClick = { onUnitFilterChange(if (effectiveFilter == unit.id) null else unit.id) },
                            label = { KbUnitLabel(unitKey = unit.id, unitCode = unit.code) },
                            colors = chipColors(),
                        )
                    }
                }
            }
        }

        when (segment) {
            AssessmentSegment.UPCOMING -> {
                if (sections.isEmpty()) {
                    item(key = "dates-empty") {
                        KbEmptyState(
                            icon = Icons.AutoMirrored.Outlined.EventNote,
                            title = stringResource(R.string.assessments_dates_empty_title),
                            body = stringResource(R.string.assessments_dates_empty_body),
                            primaryLabel = stringResource(R.string.home_open_units),
                            onPrimary = onOpenUnits,
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                sections.forEach { section ->
                    item(key = "group-${section.group.name}") {
                        Text(
                            dateGroupLabel(section.group),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            modifier = Modifier.kbContentWidth().padding(top = 8.dp).semantics { heading() },
                        )
                    }
                    items(section.deadlines, key = { "date-${it.id}" }) { deadline ->
                        UpcomingRow(
                            deadline = deadline,
                            unitCode = unitCodes[deadline.unitId] ?: deadline.unitId,
                            days = daysFromToday(deadline.date, today),
                            onClick = null,
                            linkedTopicCount = deadline.topicIds.size,
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                if (sections.any { it.group == AssessmentDateGroup.PAST }) {
                    item(key = "past-note") {
                        Text(
                            stringResource(R.string.assessments_past_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted,
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
            }
            AssessmentSegment.RESULTS -> {
                if (units.isNotEmpty()) {
                    item(key = "add-mark") {
                        KbPrimaryButton(
                            text = stringResource(R.string.marks_add_result),
                            onClick = onAddMark,
                            icon = Icons.Outlined.Add,
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                if (filteredMarks.isEmpty()) {
                    item(key = "marks-empty") {
                        KbEmptyState(
                            icon = Icons.AutoMirrored.Outlined.Grading,
                            title = stringResource(R.string.marks_no_results_yet),
                            body = stringResource(R.string.assessments_results_empty_body),
                            modifier = Modifier.kbContentWidth(),
                        )
                    }
                }
                items(filteredMarks, key = { "mark-${it.id}" }) { mark ->
                    ResultRow(
                        mark = mark,
                        unitCode = unitCodes[mark.unitId] ?: mark.unitId,
                        dateText = formatStoredDate(mark.date, locale),
                        onEdit = { onEditMark(mark.id) },
                        onDelete = { pendingDeleteId = mark.id },
                        modifier = Modifier.kbContentWidth(),
                    )
                }
            }
        }
    }

    val pendingDelete = marks.firstOrNull { it.id == pendingDeleteId }
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.marks_delete_title)) },
            text = { Text(stringResource(R.string.marks_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMark(pendingDelete)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.marks_delete_confirm), color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.marks_delete_cancel)) } },
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LocalKbColors.current.primaryTint,
    selectedLabelColor = LocalKbColors.current.onPrimaryTint,
    containerColor = LocalKbColors.current.surface,
)

@Composable
private fun ResultRow(
    mark: AssessmentMark,
    unitCode: String,
    dateText: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    val percent = if (mark.outOf > 0f) (mark.score / mark.outOf * 100f).roundToInt() else 0
    val status = when {
        percent >= 70 -> KbStatus.SUCCESS
        percent >= 50 -> KbStatus.INFO
        else -> KbStatus.WARNING
    }
    KbListRow(
        onClick = onEdit,
        modifier = modifier,
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.marks_delete_result), tint = colors.inkMuted)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KbUnitLabel(unitKey = mark.unitId, unitCode = unitCode)
            Text("·", color = colors.inkFaint, style = MaterialTheme.typography.labelMedium)
            Text(assessmentKindLabel(mark.kind), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.marks_result_score, formatDecimal(mark.score), formatDecimal(mark.outOf)),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            KbStatusPill("$percent%", status)
        }
        Column {
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
