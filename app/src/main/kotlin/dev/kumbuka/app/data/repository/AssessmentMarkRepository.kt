package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.AssessmentMarkDao
import dev.kumbuka.app.domain.model.AssessmentMark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssessmentMarkRepository(private val dao: AssessmentMarkDao) {
    fun observeAll(): Flow<List<AssessmentMark>> =
        dao.observeAll().map { list ->
            list.map { entity -> entity.toDomain(dao.getTopicIdsForMark(entity.id)) }
        }

    fun observeByUnit(unitId: String): Flow<List<AssessmentMark>> =
        dao.observeByUnit(unitId).map { list ->
            list.map { entity -> entity.toDomain(dao.getTopicIdsForMark(entity.id)) }
        }

    suspend fun upsert(mark: AssessmentMark) =
        dao.upsertWithTopics(mark.toEntity(), mark.topicIds)

    suspend fun delete(mark: AssessmentMark) =
        dao.deleteWithTopics(mark.toEntity())
}
