@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.packs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.launch

private enum class AuthorStep { UnitBasics, TopicsAndDeadlines, Review }

private data class TopicDraft(
    val title: String,
    val objective: String,
    val retrievalPrompt: String,
    val examWeight: Float,
    val resourcePointers: List<String>,
)

private data class DeadlineDraft(
    val title: String,
    val dateInput: String,
    val dateMillis: Long,
    val kind: DeadlineKind,
    val topicIndexes: Set<Int>,
)

@Composable
fun PackAuthoringScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    deadlineRepository: DeadlineRepository,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val errorWeightText = stringResource(R.string.author_error_weight)
    val errorWeightRangeText = stringResource(R.string.author_error_weight_range)
    val errorDuplicateTopicText = stringResource(R.string.author_error_topic_duplicate)
    val errorDateText = stringResource(R.string.author_error_deadline_date)
    val errorDeadlineTopicsText = stringResource(R.string.author_error_deadline_topics)
    val saveFailedText = stringResource(R.string.author_save_failed)

    var step by remember { mutableStateOf(AuthorStep.UnitBasics) }
    var unitCode by remember { mutableStateOf("") }
    var unitTitle by remember { mutableStateOf("") }
    var packVersion by remember { mutableStateOf("1") }

    var topicTitle by remember { mutableStateOf("") }
    var topicObjective by remember { mutableStateOf("") }
    var topicPrompt by remember { mutableStateOf("") }
    var topicWeight by remember { mutableStateOf("0.20") }
    var pointerDraft by remember { mutableStateOf("") }
    val pointerChoices = remember { mutableStateListOf<String>() }
    val topics = remember { mutableStateListOf<TopicDraft>() }

    var deadlineTitle by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var deadlineKind by remember { mutableStateOf(DeadlineKind.CAT) }
    val deadlineTopicIndexes = remember { mutableStateListOf<Int>() }
    val deadlines = remember { mutableStateListOf<DeadlineDraft>() }

    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val hasDraftContent = unitCode.isNotBlank() || unitTitle.isNotBlank() || topics.isNotEmpty() || deadlines.isNotEmpty()
    val handleBackPress = {
        when (step) {
            AuthorStep.UnitBasics -> if (hasDraftContent) showDiscardConfirm = true else onBack()
            AuthorStep.TopicsAndDeadlines -> step = AuthorStep.UnitBasics
            AuthorStep.Review -> step = AuthorStep.TopicsAndDeadlines
        }
    }

    BackHandler(onBack = handleBackPress)

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.author_discard_title)) },
            text = { Text(stringResource(R.string.author_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.author_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text(stringResource(R.string.author_discard_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (step) {
                            AuthorStep.UnitBasics -> stringResource(R.string.author_step_1_title)
                            AuthorStep.TopicsAndDeadlines -> stringResource(R.string.author_step_2_title)
                            AuthorStep.Review -> stringResource(R.string.author_step_3_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                AuthorStep.UnitBasics -> {
                    item {
                        StepInfoCard(
                            title = stringResource(R.string.author_step_1_title),
                            body = stringResource(R.string.author_step_1_body),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = unitCode,
                            onValueChange = { unitCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_unit_code)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = unitTitle,
                            onValueChange = { unitTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_unit_title)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = packVersion,
                            onValueChange = { packVersion = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_pack_version)) },
                        )
                    }
                    item {
                        KbPrimaryButton(
                            text = stringResource(R.string.author_continue_step_2),
                            onClick = {
                                errorText = null
                                step = AuthorStep.TopicsAndDeadlines
                            },
                            enabled = unitCode.isNotBlank() && unitTitle.isNotBlank(),
                        )
                    }
                }

                AuthorStep.TopicsAndDeadlines -> {
                    item {
                        StepInfoCard(
                            title = stringResource(R.string.author_step_2_title),
                            body = stringResource(R.string.author_step_2_body),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = topicTitle,
                            onValueChange = { topicTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_topic_title)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = topicObjective,
                            onValueChange = { topicObjective = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text(stringResource(R.string.author_topic_objective)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = topicPrompt,
                            onValueChange = { topicPrompt = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_topic_prompt)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = topicWeight,
                            onValueChange = { topicWeight = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_topic_weight)) },
                        )
                    }
                    item {
                        AddChoiceCard(
                            pointerDraft = pointerDraft,
                            onPointerDraftChange = { pointerDraft = it },
                            choices = pointerChoices,
                            onAddChoice = {
                                if (pointerDraft.isNotBlank()) {
                                    pointerChoices += pointerDraft.trim()
                                    pointerDraft = ""
                                }
                            },
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            KbSecondaryButton(
                                text = stringResource(R.string.author_add_topic_clear),
                                onClick = {
                                    topicTitle = ""
                                    topicObjective = ""
                                    topicPrompt = ""
                                    topicWeight = "0.20"
                                    pointerChoices.clear()
                                    pointerDraft = ""
                                },
                                modifier = Modifier.weight(1f),
                            )
                            KbPrimaryButton(
                                text = stringResource(R.string.author_add_topic),
                                onClick = {
                                    val weight = topicWeight.toFloatOrNull()
                                    val normalizedTitle = topicTitle.trim().lowercase()
                                    val duplicate = topics.any { it.title.trim().lowercase() == normalizedTitle }
                                    when {
                                        weight == null -> errorText = errorWeightText
                                        weight < 0f || weight > 1f -> errorText = errorWeightRangeText
                                        duplicate -> errorText = errorDuplicateTopicText
                                        else -> {
                                            topics += TopicDraft(
                                                title = topicTitle.trim(),
                                                objective = topicObjective.trim(),
                                                retrievalPrompt = topicPrompt.trim().ifBlank { topicObjective.trim() },
                                                examWeight = weight,
                                                resourcePointers = pointerChoices.toList(),
                                            )
                                            topicTitle = ""
                                            topicObjective = ""
                                            topicPrompt = ""
                                            topicWeight = "0.20"
                                            pointerChoices.clear()
                                            pointerDraft = ""
                                            errorText = null
                                        }
                                    }
                                },
                                enabled = topicTitle.isNotBlank() && topicObjective.isNotBlank() && topicWeight.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (topics.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.author_topics_added, topics.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = LocalKbColors.current.ink,
                            )
                        }
                        itemsIndexed(topics) { index, topic ->
                            TopicDraftCard(index = index + 1, topic = topic)
                        }
                    }

                    item {
                        StepInfoCard(
                            title = stringResource(R.string.author_deadline_title),
                            body = stringResource(R.string.author_deadline_body),
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = deadlineTitle,
                            onValueChange = { deadlineTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_deadline_name)) },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = deadlineDate,
                            onValueChange = { deadlineDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.author_deadline_date)) },
                            supportingText = { Text(stringResource(R.string.author_deadline_date_hint)) },
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            DeadlineKind.entries.forEach { kind ->
                                FilterChip(
                                    selected = deadlineKind == kind,
                                    onClick = { deadlineKind = kind },
                                    label = { Text(kind.name) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.author_deadline_topics), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.inkMuted)
                                if (topics.isEmpty()) {
                                    Text(stringResource(R.string.author_deadline_topics_empty), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                                } else {
                                    topics.forEachIndexed { index, topic ->
                                        FilterChip(
                                            selected = deadlineTopicIndexes.contains(index),
                                            onClick = {
                                                if (deadlineTopicIndexes.contains(index)) {
                                                    deadlineTopicIndexes.remove(index)
                                                } else {
                                                    deadlineTopicIndexes.add(index)
                                                }
                                            },
                                            label = { Text(topic.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            KbSecondaryButton(
                                text = stringResource(R.string.author_add_deadline_clear),
                                onClick = {
                                    deadlineTitle = ""
                                    deadlineDate = ""
                                    deadlineKind = DeadlineKind.CAT
                                    deadlineTopicIndexes.clear()
                                },
                                modifier = Modifier.weight(1f),
                            )
                            KbPrimaryButton(
                                text = stringResource(R.string.author_add_deadline),
                                onClick = {
                                    val parsedDate = parseDeadlineDateMillis(deadlineDate.trim())
                                    when {
                                        parsedDate == null -> errorText = errorDateText
                                        deadlineTopicIndexes.isEmpty() -> errorText = errorDeadlineTopicsText
                                        else -> {
                                            deadlines += DeadlineDraft(
                                                title = deadlineTitle.trim(),
                                                dateInput = deadlineDate.trim(),
                                                dateMillis = parsedDate,
                                                kind = deadlineKind,
                                                topicIndexes = deadlineTopicIndexes.toSet(),
                                            )
                                            deadlineTitle = ""
                                            deadlineDate = ""
                                            deadlineKind = DeadlineKind.CAT
                                            deadlineTopicIndexes.clear()
                                            errorText = null
                                        }
                                    }
                                },
                                enabled = deadlineTitle.isNotBlank() && deadlineDate.isNotBlank() && topics.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (deadlines.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.author_deadlines_added, deadlines.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = LocalKbColors.current.ink,
                            )
                        }
                        itemsIndexed(deadlines) { index, deadline ->
                            DeadlineDraftCard(index = index + 1, deadline = deadline, topics = topics)
                        }
                    }
                    item {
                        KbPrimaryButton(
                            text = stringResource(R.string.author_continue_step_3),
                            onClick = {
                                errorText = null
                                step = AuthorStep.Review
                            },
                            enabled = topics.isNotEmpty(),
                        )
                    }
                }

                AuthorStep.Review -> {
                    item {
                        StepInfoCard(
                            title = stringResource(R.string.author_step_3_title),
                            body = stringResource(R.string.author_step_3_body),
                        )
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(unitCode.uppercase(), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
                                Text(unitTitle, style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
                                Text(
                                    stringResource(R.string.author_review_counts, topics.size, deadlines.size, packVersion.ifBlank { "1" }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalKbColors.current.inkMuted,
                                )
                            }
                        }
                    }
                    itemsIndexed(topics) { index, topic ->
                        TopicDraftCard(index = index + 1, topic = topic)
                    }
                    if (deadlines.isNotEmpty()) {
                        itemsIndexed(deadlines) { index, deadline ->
                            DeadlineDraftCard(index = index + 1, deadline = deadline, topics = topics)
                        }
                    }
                    item {
                        KbPrimaryButton(
                            text = stringResource(R.string.author_save_and_export),
                            onClick = {
                                busy = true
                                errorText = null
                                scope.launch {
                                    try {
                                        val now = System.currentTimeMillis()
                                        val unitId = UUID.randomUUID().toString()
                                        val normalizedCode = unitCode.trim().uppercase()
                                        val version = packVersion.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        val unit = UnitModel(
                                            id = unitId,
                                            code = normalizedCode,
                                            title = unitTitle.trim(),
                                            packId = buildPackId(normalizedCode),
                                            packVersion = version,
                                            createdAt = now,
                                            updatedAt = now,
                                        )
                                        val topicRows = topics.mapIndexed { index, draft ->
                                            Topic(
                                                id = UUID.randomUUID().toString(),
                                                unitId = unitId,
                                                title = draft.title,
                                                objective = draft.objective,
                                                retrievalPrompt = draft.retrievalPrompt,
                                                examWeight = draft.examWeight,
                                                orderIndex = index,
                                                resourcePointers = draft.resourcePointers,
                                                titleEditedLocally = false,
                                                objectiveEditedLocally = false,
                                                weightEditedLocally = false,
                                                createdAt = now,
                                                updatedAt = now,
                                            )
                                        }
                                        val deadlineRows = deadlines.map { draft ->
                                            Deadline(
                                                id = UUID.randomUUID().toString(),
                                                unitId = unitId,
                                                title = draft.title,
                                                date = draft.dateMillis,
                                                kind = draft.kind,
                                                topicIds = draft.topicIndexes.mapNotNull { idx -> topicRows.getOrNull(idx)?.id },
                                                updatedAt = now,
                                            )
                                        }
                                        unitRepository.upsert(unit)
                                        topicRepository.upsertAll(topicRows)
                                        deadlineRows.forEach { deadlineRepository.upsert(it) }
                                        onSaved(unitId)
                                    } catch (t: Throwable) {
                                        errorText = t.message ?: saveFailedText
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                        )
                    }
                }
            }

            errorText?.let {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.warningTint), shape = RoundedCornerShape(14.dp)) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalKbColors.current.accent,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepInfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
        }
    }
}

@Composable
private fun AddChoiceCard(
    pointerDraft: String,
    onPointerDraftChange: (String) -> Unit,
    choices: List<String>,
    onAddChoice: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.author_add_choice_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.author_add_choice_body), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            OutlinedTextField(
                value = pointerDraft,
                onValueChange = onPointerDraftChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.author_pointer_label)) },
            )
            KbSecondaryButton(text = stringResource(R.string.author_add_choice_button), onClick = onAddChoice)
            if (choices.isNotEmpty()) {
                choices.forEach { choice ->
                    Text("• $choice", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.ink)
                }
            }
        }
    }
}

@Composable
private fun TopicDraftCard(index: Int, topic: TopicDraft) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$index. ${topic.title}", style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
            Text(topic.objective, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Weight ${topic.examWeight}", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.primary)
            if (topic.resourcePointers.isNotEmpty()) {
                Text(topic.resourcePointers.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.ink)
            }
        }
    }
}

@Composable
private fun DeadlineDraftCard(index: Int, deadline: DeadlineDraft, topics: List<TopicDraft>) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$index. ${deadline.title}", style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
            Text("${deadline.kind} • ${deadline.dateInput}", style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            val linked = deadline.topicIndexes.mapNotNull { idx -> topics.getOrNull(idx)?.title }
            Text(linked.joinToString(), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun buildPackId(unitCode: String): String {
    val slug = unitCode.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    val suffix = UUID.randomUUID().toString().take(8)
    return if (slug.isBlank()) "pack-$suffix" else "$slug-$suffix"
}

private fun parseDeadlineDateMillis(raw: String): Long? = runCatching {
    val date = LocalDate.parse(raw)
    date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}.getOrNull()

