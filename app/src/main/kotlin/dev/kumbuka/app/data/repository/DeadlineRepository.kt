package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.DeadlineDao
import dev.kumbuka.app.data.local.entity.DeadlineTopicCrossRef
import dev.kumbuka.app.domain.model.Deadline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeadlineRepository(private val deadlineDao: DeadlineDao) {
    fun observeByUnit(unitId: String): Flow<List<Deadline>> =
        deadlineDao.observeByUnit(unitId).map { list ->
            list.map { entity -> entity.toDomain(deadlineDao.getTopicIdsForDeadline(entity.id)) }
        }

    suspend fun getNearestForTopic(topicId: String): Deadline? =
        deadlineDao.getNearestDeadlineForTopic(topicId)?.let { entity ->
            entity.toDomain(deadlineDao.getTopicIdsForDeadline(entity.id))
        }

    suspend fun upsert(deadline: Deadline) {
        deadlineDao.upsert(deadline.toEntity())
        deadlineDao.clearCrossRefsForDeadline(deadline.id)
        deadline.topicIds.forEach { topicId ->
            deadlineDao.insertCrossRef(DeadlineTopicCrossRef(deadline.id, topicId))
        }
    }

    suspend fun delete(deadline: Deadline) = deadlineDao.delete(deadline.toEntity())
}
