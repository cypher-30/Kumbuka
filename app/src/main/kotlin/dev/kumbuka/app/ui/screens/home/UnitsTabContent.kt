package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbEmptyState
import dev.kumbuka.app.ui.components.KbListRow
import dev.kumbuka.app.ui.components.KbSectionHeader
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbUnitBadge
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.LocalDate

@Composable
fun UnitsTabContent(
    units: List<UnitModel>,
    activeTopics: List<Topic>,
    deadlines: List<Deadline>,
    today: LocalDate,
    listState: LazyListState,
    onOpenUnit: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onImportPack: () -> Unit,
    onCreateUnit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKbColors.current
    var query by rememberSaveable { mutableStateOf("") }
    val unitsById = remember(units) { units.associateBy { it.id } }
    val topicsByUnit = remember(activeTopics) { activeTopics.groupBy { it.unitId } }
    val upcomingByUnit = remember(deadlines, today) {
        deadlines.filter { daysFromToday(it.date, today) >= 0 }.groupBy { it.unitId }
    }
    val trimmed = query.trim().lowercase()
    val results = remember(trimmed, activeTopics, unitsById) {
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            activeTopics.filter { topic ->
                val unit = unitsById[topic.unitId]
                listOf(topic.title, topic.objective, topic.retrievalPrompt, unit?.code.orEmpty(), unit?.title.orEmpty())
                    .any { it.lowercase().contains(trimmed) }
            }
        }
    }
    val resultGroups = remember(results, units) {
        units.mapNotNull { unit ->
            results.filter { it.unitId == unit.id }.sortedBy { it.orderIndex }.takeIf { it.isNotEmpty() }?.let { unit to it }
        }
    }
    val (ownUnits, sampleUnits) = remember(units) { units.sortedBy { it.code }.partition { !it.isSample } }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (units.isEmpty()) {
            item(key = "empty") {
                KbEmptyState(
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.units_empty_title),
                    body = stringResource(R.string.units_empty_body),
                    primaryLabel = stringResource(R.string.import_pack_cta),
                    onPrimary = onImportPack,
                    secondaryLabel = stringResource(R.string.author_start_cta),
                    onSecondary = onCreateUnit,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            return@LazyColumn
        }

        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.kbContentWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surface,
                    focusedContainerColor = colors.surface,
                    unfocusedBorderColor = colors.border,
                ),
            )
        }

        if (trimmed.isNotBlank()) {
            item(key = "results-count") {
                Text(
                    if (results.isEmpty()) stringResource(R.string.search_no_results) else pluralStringResource(R.plurals.search_results_count, results.size, results.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            resultGroups.forEach { (unit, topics) ->
                item(key = "result-unit-${unit.id}") {
                    KbUnitLabel(unitKey = unit.id, unitCode = "${unit.code} · ${unit.title}", emphasized = true, modifier = Modifier.kbContentWidth().padding(top = 6.dp))
                }
                items(topics, key = { "result-${it.id}" }) { topic ->
                    TopicListRow(topic = topic, supporting = topic.objective, onClick = { onOpenTopic(topic.id) }, modifier = Modifier.kbContentWidth())
                }
            }
            return@LazyColumn
        }

        item(key = "own-header") {
            KbSectionHeader(
                title = stringResource(R.string.units_your_units),
                supporting = pluralStringResource(R.plurals.units_count, ownUnits.size, ownUnits.size),
                modifier = Modifier.kbContentWidth(),
            )
        }
        if (ownUnits.isEmpty()) {
            item(key = "own-empty") {
                KbSurface(modifier = Modifier.kbContentWidth()) {
                    Text(stringResource(R.string.units_only_samples), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
            }
        }
        items(ownUnits, key = { "unit-${it.id}" }) { unit ->
            UnitLibraryRow(
                unit = unit,
                topicCount = topicsByUnit[unit.id].orEmpty().size,
                upcomingCount = upcomingByUnit[unit.id].orEmpty().size,
                onClick = { onOpenUnit(unit.id) },
                modifier = Modifier.kbContentWidth(),
            )
        }
        if (sampleUnits.isNotEmpty()) {
            item(key = "sample-header") {
                KbSectionHeader(
                    title = stringResource(R.string.units_samples_title),
                    supporting = stringResource(R.string.units_samples_body),
                    modifier = Modifier.kbContentWidth(),
                )
            }
            items(sampleUnits, key = { "unit-${it.id}" }) { unit ->
                UnitLibraryRow(
                    unit = unit,
                    topicCount = topicsByUnit[unit.id].orEmpty().size,
                    upcomingCount = upcomingByUnit[unit.id].orEmpty().size,
                    onClick = { onOpenUnit(unit.id) },
                    modifier = Modifier.kbContentWidth(),
                )
            }
        }
    }
}

@Composable
private fun UnitLibraryRow(unit: UnitModel, topicCount: Int, upcomingCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKbColors.current
    KbListRow(
        onClick = onClick,
        modifier = modifier,
        leading = { KbUnitBadge(unitKey = unit.id, unitCode = unit.code) },
        trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.inkMuted) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(unit.code, style = MaterialTheme.typography.labelLarge, color = colors.inkMuted)
            if (unit.isSample) KbStatusPill(stringResource(R.string.units_sample_pill), KbStatus.NEUTRAL)
        }
        Text(unit.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            pluralStringResource(R.plurals.unit_topics_count, topicCount, topicCount) + " · " +
                pluralStringResource(R.plurals.unit_upcoming_count, upcomingCount, upcomingCount),
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted,
        )
    }
}

@Composable
fun TopicListRow(
    topic: Topic,
    supporting: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
) {
    val colors = LocalKbColors.current
    KbListRow(
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingText != null) {
                    Text(trailingText, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.inkMuted)
            }
        },
    ) {
        Text(topic.title, style = MaterialTheme.typography.titleSmall, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!supporting.isNullOrBlank()) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
