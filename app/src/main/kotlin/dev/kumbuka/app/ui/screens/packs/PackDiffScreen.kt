@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.PackChangeKind
import dev.kumbuka.app.data.repository.PackDeadlineDiff
import dev.kumbuka.app.data.repository.PackDiff
import dev.kumbuka.app.data.repository.PackImportResolution
import dev.kumbuka.app.data.repository.PackTopicDiff
import dev.kumbuka.app.data.repository.RemovalChoice
import dev.kumbuka.app.data.repository.TopicConflictChoice
import dev.kumbuka.app.data.repository.TopicField
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbColors

@Composable
fun PackDiffScreen(
    diff: PackDiff,
    resolution: PackImportResolution,
    onConflictChoice: (topicId: String, choice: TopicConflictChoice) -> Unit,
    onTopicRemovalChoice: (topicId: String, choice: RemovalChoice) -> Unit,
    onDeadlineRemovalChoice: (deadlineId: String, choice: RemovalChoice) -> Unit,
    onBack: () -> Unit,
    onApply: () -> Unit,
    busy: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ImportSummaryCard(diff)
        }
        item {
            InfoBanner(text = bannerText(diff), accent = KbColors.primary)
        }
        if (diff.topicDiffs.isEmpty() && diff.deadlineDiffs.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = stringResource(R.string.diff_no_changes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KbColors.inkMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        if (diff.topicDiffs.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.import_changed_topics))
            }
            items(diff.topicDiffs, key = { "topic-${it.topicId}-${it.kind}" }) { topicDiff ->
                TopicDiffCard(
                    diff = topicDiff,
                    selectedChoice = resolution.topicConflictChoices[topicDiff.topicId] ?: TopicConflictChoice.KEEP_LOCAL,
                    onConflictChoice = onConflictChoice,
                    selectedRemovalChoice = resolution.topicRemovalChoices[topicDiff.topicId] ?: RemovalChoice.KEEP,
                    onRemovalChoice = onTopicRemovalChoice,
                )
            }
        }
        if (diff.deadlineDiffs.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.import_changed_deadlines))
            }
            items(diff.deadlineDiffs, key = { "deadline-${it.deadlineId}-${it.kind}" }) { deadlineDiff ->
                DeadlineDiffCard(
                    diff = deadlineDiff,
                    selectedRemovalChoice = resolution.deadlineRemovalChoices[deadlineDiff.deadlineId] ?: RemovalChoice.KEEP,
                    onRemovalChoice = onDeadlineRemovalChoice,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KbSecondaryButton(
                    text = stringResource(R.string.diff_back_to_preview),
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
                KbPrimaryButton(
                    text = stringResource(R.string.diff_apply),
                    onClick = onApply,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ImportSummaryCard(diff: PackDiff) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(diff.unitTitle, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(
                text = stringResource(R.string.import_pack_metadata, diff.packId, diff.packVersion),
                style = MaterialTheme.typography.bodySmall,
                color = KbColors.inkMuted,
            )
            CountRow(label = stringResource(R.string.import_added_topics), value = diff.preview.topicsToAdd)
            CountRow(label = stringResource(R.string.import_changed_topics), value = diff.preview.topicsToUpdate)
            CountRow(label = stringResource(R.string.import_conflicted_topics), value = diff.preview.topicsWithConflicts)
            CountRow(label = stringResource(R.string.import_removed_topics), value = diff.preview.topicsMissingInPack)
            CountRow(label = stringResource(R.string.import_added_deadlines), value = diff.preview.deadlinesToAdd)
            CountRow(label = stringResource(R.string.import_changed_deadlines), value = diff.preview.deadlinesToUpdate)
            CountRow(label = stringResource(R.string.import_removed_deadlines), value = diff.preview.deadlinesMissingInPack)
        }
    }
}

@Composable
private fun CountRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, color = KbColors.ink)
    }
}

@Composable
private fun InfoBanner(text: String, accent: androidx.compose.ui.graphics.Color) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.warningTint), shape = RoundedCornerShape(14.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
}

@Composable
private fun TopicDiffCard(
    diff: PackTopicDiff,
    selectedChoice: TopicConflictChoice,
    onConflictChoice: (topicId: String, choice: TopicConflictChoice) -> Unit,
    selectedRemovalChoice: RemovalChoice,
    onRemovalChoice: (topicId: String, choice: RemovalChoice) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(diff.title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
                AssistChip(onClick = {}, enabled = false, label = { Text(diff.kind.topicLabel()) })
            }
            when (diff.kind) {
                PackChangeKind.ADDED -> {
                    Text(stringResource(R.string.diff_pack_value), style = MaterialTheme.typography.labelMedium, color = KbColors.primary)
                    diff.imported?.let { Text(topicSnapshot(it), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted) }
                }
                PackChangeKind.UPDATED -> {
                    FieldTags(diff.changedFields)
                    ValueRows(
                        local = diff.local?.let(::topicSnapshot).orEmpty(),
                        pack = diff.imported?.let(::topicSnapshot).orEmpty(),
                    )
                }
                PackChangeKind.CONFLICTED -> {
                    FieldTags(diff.changedFields)
                    Text(stringResource(R.string.diff_conflict_note), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                    ValueRows(
                        local = diff.local?.let(::topicSnapshot).orEmpty(),
                        pack = diff.imported?.let(::topicSnapshot).orEmpty(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = selectedChoice == TopicConflictChoice.KEEP_LOCAL,
                            onClick = { onConflictChoice(diff.topicId, TopicConflictChoice.KEEP_LOCAL) },
                            label = { Text(stringResource(R.string.diff_keep_local)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = selectedChoice == TopicConflictChoice.USE_PACK,
                            onClick = { onConflictChoice(diff.topicId, TopicConflictChoice.USE_PACK) },
                            label = { Text(stringResource(R.string.diff_use_pack)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                PackChangeKind.REMOVED -> {
                    Text(stringResource(R.string.diff_removed_note), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                    Text(diff.local?.let(::topicSnapshot).orEmpty(), style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = selectedRemovalChoice == RemovalChoice.KEEP,
                            onClick = { onRemovalChoice(diff.topicId, RemovalChoice.KEEP) },
                            label = { Text(stringResource(R.string.diff_keep_row)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = selectedRemovalChoice == RemovalChoice.DELETE,
                            onClick = { onRemovalChoice(diff.topicId, RemovalChoice.DELETE) },
                            label = { Text(stringResource(R.string.diff_delete_row)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineDiffCard(
    diff: PackDeadlineDiff,
    selectedRemovalChoice: RemovalChoice,
    onRemovalChoice: (deadlineId: String, choice: RemovalChoice) -> Unit,
) {
    val title = diff.imported?.title ?: diff.local?.title.orEmpty()
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
                AssistChip(onClick = {}, enabled = false, label = { Text(diff.kind.deadlineLabel()) })
            }
            when (diff.kind) {
                PackChangeKind.ADDED -> {
                    Text(diff.imported?.let(::deadlineSnapshot).orEmpty(), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                }
                PackChangeKind.UPDATED -> {
                    DeadlineFieldTags(diff.changedFields.map { deadlineFieldLabel(it) }.toSet())
                    ValueRows(
                        local = diff.local?.let(::deadlineSnapshot).orEmpty(),
                        pack = diff.imported?.let(::deadlineSnapshot).orEmpty(),
                    )
                }
                PackChangeKind.REMOVED -> {
                    Text(stringResource(R.string.diff_removed_note), style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
                    Text(diff.local?.let(::deadlineSnapshot).orEmpty(), style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = selectedRemovalChoice == RemovalChoice.KEEP,
                            onClick = { onRemovalChoice(diff.deadlineId, RemovalChoice.KEEP) },
                            label = { Text(stringResource(R.string.diff_keep_row)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = selectedRemovalChoice == RemovalChoice.DELETE,
                            onClick = { onRemovalChoice(diff.deadlineId, RemovalChoice.DELETE) },
                            label = { Text(stringResource(R.string.diff_delete_row)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                PackChangeKind.CONFLICTED -> Unit
            }
        }
    }
}

@Composable
private fun FieldTags(fields: Set<TopicField>) {
    if (fields.isEmpty()) return
    Text(stringResource(R.string.diff_changed_fields), style = MaterialTheme.typography.labelMedium, color = KbColors.inkMuted)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        fields.take(3).forEach { field ->
            AssistChip(onClick = {}, enabled = false, label = { Text(field.label()) })
        }
    }
}

@Composable
private fun DeadlineFieldTags(fields: Set<String>) {
    if (fields.isEmpty()) return
    Text(stringResource(R.string.diff_changed_fields), style = MaterialTheme.typography.labelMedium, color = KbColors.inkMuted)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        fields.take(3).forEach { field ->
            AssistChip(onClick = {}, enabled = false, label = { Text(field) })
        }
    }
}

@Composable
private fun ValueRows(local: String, pack: String) {
    Text(stringResource(R.string.diff_local_value), style = MaterialTheme.typography.labelMedium, color = KbColors.inkMuted)
    Text(local, style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
    Text(stringResource(R.string.diff_pack_value), style = MaterialTheme.typography.labelMedium, color = KbColors.primary, fontWeight = FontWeight.Medium)
    Text(pack, style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
}

@Composable
private fun TopicField.label(): String = when (this) {
    TopicField.TITLE -> stringResource(R.string.diff_field_title)
    TopicField.OBJECTIVE -> stringResource(R.string.diff_field_objective)
    TopicField.RETRIEVAL_PROMPT -> stringResource(R.string.diff_field_retrieval_prompt)
    TopicField.EXAM_WEIGHT -> stringResource(R.string.diff_field_exam_weight)
    TopicField.ORDER_INDEX -> stringResource(R.string.diff_field_order_index)
    TopicField.RESOURCE_POINTERS -> stringResource(R.string.diff_field_resource_pointers)
}

@Composable
private fun PackChangeKind.topicLabel(): String = when (this) {
    PackChangeKind.ADDED -> stringResource(R.string.import_added_topics)
    PackChangeKind.UPDATED -> stringResource(R.string.import_changed_topics)
    PackChangeKind.CONFLICTED -> stringResource(R.string.import_conflicted_topics)
    PackChangeKind.REMOVED -> stringResource(R.string.import_removed_topics)
}

@Composable
private fun PackChangeKind.deadlineLabel(): String = when (this) {
    PackChangeKind.ADDED -> stringResource(R.string.import_added_deadlines)
    PackChangeKind.UPDATED -> stringResource(R.string.import_changed_deadlines)
    PackChangeKind.CONFLICTED -> stringResource(R.string.import_conflicted_topics)
    PackChangeKind.REMOVED -> stringResource(R.string.import_removed_deadlines)
}

@Composable
private fun deadlineFieldLabel(field: String): String = when (field) {
    "title" -> stringResource(R.string.diff_field_title)
    "date" -> stringResource(R.string.diff_field_date)
    "kind" -> stringResource(R.string.diff_field_kind)
    "topicIds" -> stringResource(R.string.diff_field_topic_ids)
    else -> field
}

private fun topicSnapshot(topic: dev.kumbuka.app.pack.CoursePackTopic): String =
    "${topic.title} | w=${topic.examWeight} | order=${topic.orderIndex} | ${topic.objective.take(80)}"

private fun topicSnapshot(topic: dev.kumbuka.app.domain.model.Topic): String =
    "${topic.title} | w=${topic.examWeight} | order=${topic.orderIndex} | ${topic.objective.take(80)}"

private fun deadlineSnapshot(deadline: dev.kumbuka.app.pack.CoursePackDeadline): String =
    "${deadline.kind} @ ${deadline.date} | topics=${deadline.topicIds.joinToString()}"

private fun deadlineSnapshot(deadline: dev.kumbuka.app.domain.model.Deadline): String =
    "${deadline.kind} @ ${deadline.date} | topics=${deadline.topicIds.joinToString()}"

@Composable
private fun bannerText(diff: PackDiff): String {
    val preview = diff.preview
    return "${preview.topicsToAdd} new, ${preview.topicsToUpdate} changed, ${preview.topicsWithConflicts} conflicts, ${preview.topicsMissingInPack} missing topics" +
        " | ${preview.deadlinesToAdd} new and ${preview.deadlinesToUpdate} changed deadlines"
}




