package dev.kumbuka.app.data.repository

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic

object PackDiffEngine {
    fun buildDiff(
        pack: CoursePack,
        existingUnitId: String?,
        localTopics: Map<String, Topic>,
        localDeadlines: Map<String, Deadline>,
    ): PackDiff {
        val topicDiffs = buildTopicDiffs(pack, localTopics)
        val deadlineDiffs = buildDeadlineDiffs(pack, localDeadlines)
        return PackDiff(
            packId = pack.packId,
            packVersion = pack.packVersion,
            unitTitle = pack.unit.title,
            existingUnitId = existingUnitId,
            topicDiffs = topicDiffs,
            deadlineDiffs = deadlineDiffs,
        )
    }

    fun mergeTopics(
        pack: CoursePack,
        unitId: String,
        localTopics: Map<String, Topic>,
        now: Long,
        conflictChoices: Map<String, TopicConflictChoice>,
    ): List<Topic> = pack.topics.map { imported ->
        val local = localTopics[imported.id]
        if (local == null) {
            Topic(
                id = imported.id,
                unitId = unitId,
                title = imported.title,
                objective = imported.objective,
                retrievalPrompt = imported.retrievalPrompt,
                examWeight = imported.examWeight,
                orderIndex = imported.orderIndex,
                resourcePointers = imported.resourcePointers,
                titleEditedLocally = false,
                objectiveEditedLocally = false,
                weightEditedLocally = false,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            val usePack = conflictChoices[imported.id] == TopicConflictChoice.USE_PACK
            val titleConflict = local.titleEditedLocally && local.title != imported.title
            val objectiveConflict = local.objectiveEditedLocally && local.objective != imported.objective
            val weightConflict = local.weightEditedLocally && local.examWeight != imported.examWeight

            local.copy(
                unitId = unitId,
                title = if (titleConflict && !usePack) local.title else imported.title,
                objective = if (objectiveConflict && !usePack) local.objective else imported.objective,
                retrievalPrompt = imported.retrievalPrompt,
                examWeight = if (weightConflict && !usePack) local.examWeight else imported.examWeight,
                orderIndex = imported.orderIndex,
                resourcePointers = imported.resourcePointers,
                titleEditedLocally = if (titleConflict && usePack) false else local.titleEditedLocally,
                objectiveEditedLocally = if (objectiveConflict && usePack) false else local.objectiveEditedLocally,
                weightEditedLocally = if (weightConflict && usePack) false else local.weightEditedLocally,
                updatedAt = now,
            )
        }
    }

    fun mergeDeadlines(
        pack: CoursePack,
        unitId: String,
        localDeadlines: Map<String, Deadline>,
        now: Long,
    ): List<Deadline> = pack.deadlines.map { imported ->
        val local = localDeadlines[imported.id]
        if (local == null) {
            Deadline(
                id = imported.id,
                unitId = unitId,
                title = imported.title,
                date = imported.date,
                kind = imported.kind,
                topicIds = imported.topicIds,
                updatedAt = now,
            )
        } else {
            local.copy(
                unitId = unitId,
                title = imported.title,
                date = imported.date,
                kind = imported.kind,
                topicIds = imported.topicIds,
                updatedAt = now,
            )
        }
    }

    private fun buildTopicDiffs(pack: CoursePack, localTopics: Map<String, Topic>): List<PackTopicDiff> {
        val packIds = pack.topics.map { it.id }.toSet()
        return buildList {
            for (imported in pack.topics) {
                val local = localTopics[imported.id]
                when {
                    local == null -> add(PackTopicDiff(imported.id, PackChangeKind.ADDED, null, imported))
                    else -> {
                        val changedFields = topicChangedFields(local, imported)
                        val conflictedFields = topicConflictedFields(local, imported)
                        when {
                            conflictedFields.isNotEmpty() -> add(PackTopicDiff(imported.id, PackChangeKind.CONFLICTED, local, imported, changedFields, conflictedFields))
                            changedFields.isNotEmpty() -> add(PackTopicDiff(imported.id, PackChangeKind.UPDATED, local, imported, changedFields, emptySet()))
                        }
                    }
                }
            }
            localTopics.values.filter { it.id !in packIds }.forEach { local ->
                add(PackTopicDiff(local.id, PackChangeKind.REMOVED, local, null))
            }
        }
    }

    private fun buildDeadlineDiffs(pack: CoursePack, localDeadlines: Map<String, Deadline>): List<PackDeadlineDiff> {
        val packIds = pack.deadlines.map { it.id }.toSet()
        return buildList {
            for (imported in pack.deadlines) {
                val local = localDeadlines[imported.id]
                when {
                    local == null -> add(PackDeadlineDiff(imported.id, PackChangeKind.ADDED, null, imported))
                    hasDeadlineChanges(local, imported) -> add(PackDeadlineDiff(imported.id, PackChangeKind.UPDATED, local, imported, deadlineChangedFields(local, imported)))
                }
            }
            localDeadlines.values.filter { it.id !in packIds }.forEach { local ->
                add(PackDeadlineDiff(local.id, PackChangeKind.REMOVED, local, null))
            }
        }
    }

    private fun topicChangedFields(local: Topic, imported: CoursePackTopic): Set<TopicField> = buildSet {
        if (local.title != imported.title) add(TopicField.TITLE)
        if (local.objective != imported.objective) add(TopicField.OBJECTIVE)
        if (local.retrievalPrompt != imported.retrievalPrompt) add(TopicField.RETRIEVAL_PROMPT)
        if (local.examWeight != imported.examWeight) add(TopicField.EXAM_WEIGHT)
        if (local.orderIndex != imported.orderIndex) add(TopicField.ORDER_INDEX)
        if (local.resourcePointers != imported.resourcePointers) add(TopicField.RESOURCE_POINTERS)
    }

    private fun topicConflictedFields(local: Topic, imported: CoursePackTopic): Set<TopicField> = buildSet {
        if (local.titleEditedLocally && local.title != imported.title) add(TopicField.TITLE)
        if (local.objectiveEditedLocally && local.objective != imported.objective) add(TopicField.OBJECTIVE)
        if (local.weightEditedLocally && local.examWeight != imported.examWeight) add(TopicField.EXAM_WEIGHT)
    }

    private fun hasDeadlineChanges(local: Deadline, imported: CoursePackDeadline): Boolean =
        local.title != imported.title ||
            local.date != imported.date ||
            local.kind != imported.kind ||
            local.topicIds != imported.topicIds

    private fun deadlineChangedFields(local: Deadline, imported: CoursePackDeadline): Set<String> = buildSet {
        if (local.title != imported.title) add("title")
        if (local.date != imported.date) add("date")
        if (local.kind != imported.kind) add("kind")
        if (local.topicIds != imported.topicIds) add("topicIds")
    }
}

