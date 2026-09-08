package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.AssessmentMarkDao
import dev.kumbuka.app.data.local.entity.AssessmentMarkTopicCrossRef
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

    suspend fun upsert(mark: AssessmentMark) {
        dao.upsert(mark.toEntity())
        dao.clearCrossRefsForMark(mark.id)
        mark.topicIds.forEach { topicId ->
            dao.insertCrossRef(AssessmentMarkTopicCrossRef(mark.id, topicId))
        }
    }

    suspend fun delete(mark: AssessmentMark) {
        dao.clearCrossRefsForMark(mark.id)
        dao.delete(mark.toEntity())
    }
}
