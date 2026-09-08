package dev.kumbuka.app.data.repository

import androidx.room.withTransaction
import dev.kumbuka.app.data.local.KumbukaDatabase
import dev.kumbuka.app.domain.model.Unit as UnitModel
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

    suspend fun buildDiff(pack: CoursePack): PackDiff {
        val existingUnit = unitRepository.getByPackId(pack.packId)
        val localTopics = existingUnit?.let { topicRepository.getByUnitOnce(it.id) }.orEmpty().associateBy { it.id }
        val localDeadlines = existingUnit?.let { deadlineRepository.observeByUnit(it.id).first() }.orEmpty().associateBy { it.id }
        return PackDiffEngine.buildDiff(pack, existingUnit?.id, localTopics, localDeadlines)
    }

    suspend fun previewImport(pack: CoursePack): PackImportPreview = buildDiff(pack).preview

    suspend fun importPack(pack: CoursePack, resolution: PackImportResolution = PackImportResolution()): PackImportPreview = database.withTransaction {
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
        val mergedTopics = PackDiffEngine.mergeTopics(pack, unitId, localTopics, now, resolution.topicConflictChoices)
        mergedTopics.forEach { topicRepository.upsert(it) }

        val localDeadlines = deadlineRepository.observeByUnit(unitId).first().associateBy { it.id }
        val mergedDeadlines = PackDiffEngine.mergeDeadlines(pack, unitId, localDeadlines, now)
        mergedDeadlines.forEach { deadlineRepository.upsert(it) }

        val packTopicIds = pack.topics.map { it.id }.toSet()
        localTopics.values
            .filter { it.id !in packTopicIds && resolution.topicRemovalChoices[it.id] == RemovalChoice.DELETE }
            .forEach { topicRepository.delete(it) }

        val packDeadlineIds = pack.deadlines.map { it.id }.toSet()
        localDeadlines.values
            .filter { it.id !in packDeadlineIds && resolution.deadlineRemovalChoices[it.id] == RemovalChoice.DELETE }
            .forEach { deadlineRepository.delete(it) }

        PackDiffEngine.buildDiff(pack, existingUnit?.id, localTopics, localDeadlines).preview
    }
}



