package dev.kumbuka.app.data.repository

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic

enum class PackChangeKind {
    ADDED,
    UPDATED,
    CONFLICTED,
    REMOVED,
}

enum class TopicConflictChoice {
    KEEP_LOCAL,
    USE_PACK,
}

enum class RemovalChoice {
    KEEP,
    DELETE,
}

enum class TopicField {
    TITLE,
    OBJECTIVE,
    RETRIEVAL_PROMPT,
    EXAM_WEIGHT,
    ORDER_INDEX,
    RESOURCE_POINTERS,
}

data class PackImportResolution(
    val topicConflictChoices: Map<String, TopicConflictChoice> = emptyMap(),
    val topicRemovalChoices: Map<String, RemovalChoice> = emptyMap(),
    val deadlineRemovalChoices: Map<String, RemovalChoice> = emptyMap(),
)

data class PackTopicDiff(
    val topicId: String,
    val kind: PackChangeKind,
    val local: Topic?,
    val imported: CoursePackTopic?,
    val changedFields: Set<TopicField> = emptySet(),
    val conflictedFields: Set<TopicField> = emptySet(),
) {
    val title: String
        get() = imported?.title ?: local?.title.orEmpty()
}

data class PackDeadlineDiff(
    val deadlineId: String,
    val kind: PackChangeKind,
    val local: Deadline?,
    val imported: CoursePackDeadline?,
    val changedFields: Set<String> = emptySet(),
)

data class PackDiff(
    val packId: String,
    val packVersion: Int,
    val unitTitle: String,
    val existingUnitId: String?,
    val topicDiffs: List<PackTopicDiff>,
    val deadlineDiffs: List<PackDeadlineDiff>,
) {
    val preview: PackImportPreview = PackImportPreview(
        packId = packId,
        packVersion = packVersion,
        unitTitle = unitTitle,
        existingUnitId = existingUnitId,
        topicsToAdd = topicDiffs.count { it.kind == PackChangeKind.ADDED },
        topicsToUpdate = topicDiffs.count { it.kind == PackChangeKind.UPDATED },
        topicsWithConflicts = topicDiffs.count { it.kind == PackChangeKind.CONFLICTED },
        topicsMissingInPack = topicDiffs.count { it.kind == PackChangeKind.REMOVED },
        deadlinesToAdd = deadlineDiffs.count { it.kind == PackChangeKind.ADDED },
        deadlinesToUpdate = deadlineDiffs.count { it.kind == PackChangeKind.UPDATED },
        deadlinesMissingInPack = deadlineDiffs.count { it.kind == PackChangeKind.REMOVED },
    )
}

