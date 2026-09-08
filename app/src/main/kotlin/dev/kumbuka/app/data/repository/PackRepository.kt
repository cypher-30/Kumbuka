package dev.kumbuka.app.data.repository

import androidx.room.withTransaction
import dev.kumbuka.app.data.local.KumbukaDatabase
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackCodec
import java.util.UUID
import kotlinx.coroutines.flow.first

data class PackImportPreview(
    val packId: String,
    val packVersion: Int,
    val unitTitle: String,
    val existingUnitId: String?,
    val topicsToAdd: Int,
    val topicsToUpdate: Int,
    val topicsWithConflicts: Int,
    val topicsMissingInPack: Int,
    val deadlinesToAdd: Int,
    val deadlinesToUpdate: Int,
    val deadlinesMissingInPack: Int,
)

class PackRepository(
    private val database: KumbukaDatabase,
    private val unitRepository: UnitRepository,
    private val topicRepository: TopicRepository,
    private val deadlineRepository: DeadlineRepository,
) {
    suspend fun parse(raw: String): CoursePack = CoursePackCodec.decode(raw)

    fun encode(pack: CoursePack): String = CoursePackCodec.encode(pack)

    suspend fun exportUnit(unitId: String): CoursePack {
        val unit = requireNotNull(unitRepository.getById(unitId)) { "Unknown unit: $unitId" }
        val topics = topicRepository.getByUnitOnce(unitId)
        val deadlines = deadlineRepository.observeByUnit(unitId).first()
        return CoursePack.fromDomain(unit, topics, deadlines)
    }

    suspend fun previewImport(pack: CoursePack): PackImportPreview {
        val existingUnit = unitRepository.getByPackId(pack.packId)
        val localTopics = existingUnit?.let { topicRepository.getByUnitOnce(it.id) }.orEmpty().associateBy { it.id }
        val localDeadlines = existingUnit?.let { deadlineRepository.observeByUnit(it.id).first() }.orEmpty().associateBy { it.id }

        val topicStats = summarizeTopics(pack, localTopics)
        val deadlineStats = summarizeDeadlines(pack, localDeadlines)
        return PackImportPreview(
            packId = pack.packId,
            packVersion = pack.packVersion,
            unitTitle = pack.unit.title,
            existingUnitId = existingUnit?.id,
            topicsToAdd = topicStats.added,
            topicsToUpdate = topicStats.updated,
            topicsWithConflicts = topicStats.conflicts,
            topicsMissingInPack = topicStats.removed,
            deadlinesToAdd = deadlineStats.added,
            deadlinesToUpdate = deadlineStats.updated,
            deadlinesMissingInPack = deadlineStats.removed,
        )
    }

    suspend fun importPack(pack: CoursePack): PackImportPreview = database.withTransaction {
        val existingUnit = unitRepository.getByPackId(pack.packId)
        val now = System.currentTimeMillis()
        val unitId = existingUnit?.id ?: UUID.randomUUID().toString()
        val unit = (existingUnit ?: UnitModel(
            id = unitId,
            code = pack.unit.code,
            title = pack.unit.title,
            packId = pack.packId,
            packVersion = pack.packVersion,
            createdAt = now,
            updatedAt = now,
        )).copy(
            id = unitId,
            code = pack.unit.code,
            title = pack.unit.title,
            packId = pack.packId,
            packVersion = pack.packVersion,
            updatedAt = now,
        )
        unitRepository.upsert(unit)

        val localTopics = topicRepository.getByUnitOnce(unitId).associateBy { it.id }
        val mergedTopics = mergeTopics(pack, unitId, localTopics, now)
        mergedTopics.forEach { topicRepository.upsert(it) }

        val localDeadlines = deadlineRepository.observeByUnit(unitId).first().associateBy { it.id }
        val mergedDeadlines = mergeDeadlines(pack, unitId, localDeadlines, now)
        mergedDeadlines.forEach { deadlineRepository.upsert(it) }

        val preview = previewFromSnapshots(pack, existingUnit?.id, localTopics, localDeadlines)
        preview
    }

    private data class MergeStats(val added: Int, val updated: Int, val conflicts: Int, val removed: Int)

    private fun summarizeTopics(pack: CoursePack, localTopics: Map<String, Topic>): MergeStats {
        val packIds = pack.topics.map { it.id }.toSet()
        var added = 0
        var updated = 0
        var conflicts = 0
        for (topic in pack.topics) {
            val local = localTopics[topic.id]
            when {
                local == null -> added += 1
                hasTopicConflict(local, topic) -> conflicts += 1
                hasTopicChanges(local, topic) -> updated += 1
            }
        }
        val removed = localTopics.keys.count { it !in packIds }
        return MergeStats(added, updated, conflicts, removed)
    }

    private fun summarizeDeadlines(pack: CoursePack, localDeadlines: Map<String, Deadline>): MergeStats {
        val packIds = pack.deadlines.map { it.id }.toSet()
        var added = 0
        var updated = 0
        for (deadline in pack.deadlines) {
            val local = localDeadlines[deadline.id]
            when {
                local == null -> added += 1
                hasDeadlineChanges(local, deadline) -> updated += 1
            }
        }
        val removed = localDeadlines.keys.count { it !in packIds }
        return MergeStats(added, updated, 0, removed)
    }

    private fun previewFromSnapshots(
        pack: CoursePack,
        existingUnitId: String?,
        localTopics: Map<String, Topic>,
        localDeadlines: Map<String, Deadline>,
    ): PackImportPreview {
        val topicStats = summarizeTopics(pack, localTopics)
        val deadlineStats = summarizeDeadlines(pack, localDeadlines)
        return PackImportPreview(
            packId = pack.packId,
            packVersion = pack.packVersion,
            unitTitle = pack.unit.title,
            existingUnitId = existingUnitId,
            topicsToAdd = topicStats.added,
            topicsToUpdate = topicStats.updated,
            topicsWithConflicts = topicStats.conflicts,
            topicsMissingInPack = topicStats.removed,
            deadlinesToAdd = deadlineStats.added,
            deadlinesToUpdate = deadlineStats.updated,
            deadlinesMissingInPack = deadlineStats.removed,
        )
    }

    private fun mergeTopics(
        pack: CoursePack,
        unitId: String,
        localTopics: Map<String, Topic>,
        now: Long,
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
            local.copy(
                unitId = unitId,
                title = if (local.titleEditedLocally && local.title != imported.title) local.title else imported.title,
                objective = if (local.objectiveEditedLocally && local.objective != imported.objective) local.objective else imported.objective,
                retrievalPrompt = imported.retrievalPrompt,
                examWeight = if (local.weightEditedLocally && local.examWeight != imported.examWeight) local.examWeight else imported.examWeight,
                orderIndex = imported.orderIndex,
                resourcePointers = imported.resourcePointers,
                updatedAt = now,
            )
        }
    }

    private fun mergeDeadlines(
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

    private fun hasTopicConflict(local: Topic, imported: dev.kumbuka.app.pack.CoursePackTopic): Boolean =
        (local.titleEditedLocally && local.title != imported.title) ||
            (local.objectiveEditedLocally && local.objective != imported.objective) ||
            (local.weightEditedLocally && local.examWeight != imported.examWeight)

    private fun hasTopicChanges(local: Topic, imported: dev.kumbuka.app.pack.CoursePackTopic): Boolean =
        local.title != imported.title ||
            local.objective != imported.objective ||
            local.retrievalPrompt != imported.retrievalPrompt ||
            local.examWeight != imported.examWeight ||
            local.orderIndex != imported.orderIndex ||
            local.resourcePointers != imported.resourcePointers

    private fun hasDeadlineChanges(local: Deadline, imported: CoursePackDeadline): Boolean =
        local.title != imported.title ||
            local.date != imported.date ||
            local.kind != imported.kind ||
            local.topicIds != imported.topicIds
}



