@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.packs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.PackRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic
import dev.kumbuka.app.pack.CoursePackUnit
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.KbSegmentedControl
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.screens.home.formatEpochDateUtc
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch
import java.util.UUID

private enum class Weight(val value: Float, val label: Int) {
    LOW(0.1f, R.string.create_weight_low),
    MEDIUM(0.2f, R.string.create_weight_medium),
    HIGH(0.35f, R.string.create_weight_high),
}

private class TopicField(title: String = "", weight: Weight = Weight.MEDIUM) {
    var title by mutableStateOf(title)
    var weight by mutableStateOf(weight)
}

private class DateField(title: String = "", kind: DeadlineKind = DeadlineKind.CAT, millis: Long? = null) {
    var title by mutableStateOf(title)
    var kind by mutableStateOf(kind)
    var millis by mutableStateOf(millis)
}

/**
 * One screen for making a unit by hand: a Course (code, topics with an exam
 * weight, dates, optional share) or a Personal goal (a title and objectives).
 * Personal goals are flagged so they never reach research logs or exports.
 */
@Composable
fun CreateUnitScreen(
    packRepository: PackRepository,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    deadlineRepository: DeadlineRepository,
    onBack: () -> Unit,
    onSaved: (unitId: String, openShare: Boolean) -> Unit,
) {
    val colors = LocalKbColors.current
    val scope = rememberCoroutineScope()
    var goalMode by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(false) }
    val topics = remember { mutableStateListOf(TopicField()) }
    val objectives = remember { mutableStateListOf(TopicField()) }
    val dates = remember { mutableStateListOf<DateField>() }
    var datePickerFor by remember { mutableStateOf<DateField?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    val activeItems = if (goalMode) objectives else topics
    val dirty = title.isNotBlank() || code.isNotBlank() || activeItems.any { it.title.isNotBlank() } || dates.isNotEmpty()
    val valid = title.isNotBlank() && activeItems.any { it.title.isNotBlank() } && (goalMode || code.isNotBlank())
    val leave = { if (dirty && !saving) showDiscard = true else onBack() }
    BackHandler(onBack = leave)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = colors.surface,
        focusedContainerColor = colors.surface,
        unfocusedBorderColor = colors.outlineStrong,
    )

    Scaffold(
        topBar = { KbTopBar(title = stringResource(R.string.author_start_cta), onBack = leave) },
        containerColor = colors.paper,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KbSegmentedControl(
                options = listOf(stringResource(R.string.create_mode_course), stringResource(R.string.create_mode_goal)),
                selectedIndex = if (goalMode) 1 else 0,
                onSelect = { goalMode = it == 1 },
                modifier = Modifier.kbContentWidth(),
            )
            if (goalMode) {
                KbBanner(stringResource(R.string.create_goal_note), KbStatus.INFO, Modifier.kbContentWidth())
            }

            Column(Modifier.kbContentWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!goalMode) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(R.string.paste_code_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(if (goalMode) R.string.create_goal_title_label else R.string.paste_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
            }

            SectionTitle(stringResource(if (goalMode) R.string.create_objectives_title else R.string.create_topics_title))
            activeItems.forEachIndexed { index, field ->
                KbSurface(modifier = Modifier.kbContentWidth(), contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = field.title,
                            onValueChange = { field.title = it },
                            label = { Text(stringResource(if (goalMode) R.string.create_objective_hint else R.string.create_topic_hint, index + 1)) },
                            modifier = Modifier.weight(1f),
                            colors = fieldColors,
                        )
                        if (activeItems.size > 1) {
                            IconButton(onClick = { activeItems.removeAt(index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.create_remove))
                            }
                        }
                    }
                    if (!goalMode) {
                        Text(stringResource(R.string.create_weight_label), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Weight.entries.forEach { weight ->
                                FilterChip(
                                    selected = field.weight == weight,
                                    onClick = { field.weight = weight },
                                    label = { Text(stringResource(weight.label)) },
                                    colors = chipColors(),
                                )
                            }
                        }
                    }
                }
            }
            KbSecondaryButton(
                text = stringResource(if (goalMode) R.string.create_add_objective else R.string.create_add_topic),
                onClick = { activeItems += TopicField() },
                icon = Icons.Outlined.Add,
                modifier = Modifier.kbContentWidth(),
            )

            SectionTitle(stringResource(R.string.create_dates_title))
            dates.forEachIndexed { index, date ->
                KbSurface(modifier = Modifier.kbContentWidth(), contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = date.title,
                            onValueChange = { date.title = it },
                            label = { Text(stringResource(R.string.create_date_title_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = fieldColors,
                        )
                        IconButton(onClick = { dates.removeAt(index) }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.create_remove))
                        }
                    }
                    if (!goalMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DeadlineKind.entries.forEach { kind ->
                                FilterChip(
                                    selected = date.kind == kind,
                                    onClick = { date.kind = kind },
                                    label = { Text(kindLabel(kind)) },
                                    colors = chipColors(),
                                )
                            }
                        }
                    }
                    KbSecondaryButton(
                        text = date.millis?.let { formatEpochDateUtc(it) } ?: stringResource(R.string.create_date_pick),
                        onClick = { datePickerFor = date },
                        icon = Icons.Outlined.Event,
                    )
                }
            }
            if (!goalMode || dates.isEmpty()) {
                KbSecondaryButton(
                    text = stringResource(R.string.create_add_date),
                    onClick = { dates += DateField(kind = if (goalMode) DeadlineKind.ASSIGNMENT else DeadlineKind.CAT) },
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.kbContentWidth(),
                )
            }

            if (!goalMode) {
                KbSurface(modifier = Modifier.kbContentWidth(), contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.create_share_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                            Text(stringResource(R.string.create_share_body), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
                        }
                        Switch(
                            checked = share,
                            onCheckedChange = { share = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                        )
                    }
                }
            }

            if (error) KbBanner(stringResource(R.string.onboarding_save_failed), KbStatus.ERROR, Modifier.kbContentWidth())
            KbPrimaryButton(
                text = stringResource(R.string.create_save),
                enabled = valid && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        error = false
                        val unitId = runCatching {
                            saveUnit(
                                goal = goalMode, code = code.trim(), title = title.trim(),
                                topics = activeItems.filter { it.title.isNotBlank() }.map { it.title.trim() to it.weight.value },
                                dates = dates.filter { it.title.isNotBlank() && it.millis != null }.map { Triple(it.title.trim(), it.kind, it.millis!!) },
                                packRepository = packRepository, unitRepository = unitRepository,
                                topicRepository = topicRepository, deadlineRepository = deadlineRepository,
                            )
                        }.onFailure { error = true }.getOrNull()
                        saving = false
                        if (unitId != null) onSaved(unitId, share && !goalMode)
                    }
                },
                modifier = Modifier.kbContentWidth(),
            )
        }
    }

    datePickerFor?.let { target ->
        val state = rememberDatePickerState(initialSelectedDateMillis = target.millis)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    target.millis = state.selectedDateMillis
                    datePickerFor = null
                }) { Text(stringResource(R.string.generic_ok)) }
            },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text(stringResource(R.string.generic_cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.create_discard_title)) },
            text = { Text(stringResource(R.string.create_discard_body)) },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.create_discard_confirm), color = colors.error) } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.author_discard_cancel)) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = LocalKbColors.current.ink,
        modifier = Modifier.kbContentWidth().padding(top = 6.dp),
    )
}

@Composable
private fun kindLabel(kind: DeadlineKind): String = stringResource(
    when (kind) {
        DeadlineKind.CAT -> R.string.create_kind_cat
        DeadlineKind.ASSIGNMENT -> R.string.create_kind_assignment
        DeadlineKind.EXAM -> R.string.create_kind_exam
    },
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LocalKbColors.current.primaryTint,
    selectedLabelColor = LocalKbColors.current.onPrimaryTint,
)

private suspend fun saveUnit(
    goal: Boolean,
    code: String,
    title: String,
    topics: List<Pair<String, Float>>,
    dates: List<Triple<String, DeadlineKind, Long>>,
    packRepository: PackRepository,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    deadlineRepository: DeadlineRepository,
): String {
    val now = System.currentTimeMillis()
    if (goal) {
        val unitId = "unit-" + UUID.randomUUID()
        unitRepository.upsert(UnitModel(unitId, "GOAL", title, null, null, now, now, isPersonal = true))
        val saved = topics.mapIndexed { index, (name, _) ->
            Topic(
                id = "topic-" + UUID.randomUUID(), unitId = unitId, title = name, objective = "", retrievalPrompt = "",
                examWeight = 1f / topics.size, orderIndex = index + 1, resourcePointers = emptyList(),
                titleEditedLocally = false, objectiveEditedLocally = false, weightEditedLocally = false,
                createdAt = now, updatedAt = now,
            )
        }
        topicRepository.upsertAll(saved)
        dates.forEach { (dateTitle, kind, millis) ->
            deadlineRepository.upsert(Deadline("deadline-" + UUID.randomUUID(), unitId, dateTitle, millis, kind, saved.map { it.id }, now))
        }
        return unitId
    }
    val topicIds = topics.map { "topic-" + UUID.randomUUID() }
    val slug = code.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    val packId = (if (slug.isBlank()) "pack" else slug) + "-" + UUID.randomUUID().toString().take(8)
    val pack = CoursePack(
        packId = packId,
        packVersion = 1,
        unit = CoursePackUnit(code, title),
        topics = topics.mapIndexed { index, (name, weight) ->
            CoursePackTopic(topicIds[index], name, "", "", weight, index + 1)
        },
        deadlines = dates.map { (dateTitle, kind, millis) ->
            CoursePackDeadline("deadline-" + UUID.randomUUID(), dateTitle, millis, kind, topicIds)
        },
    )
    packRepository.importPack(pack)
    return requireNotNull(unitRepository.getByPackId(packId)).id
}
