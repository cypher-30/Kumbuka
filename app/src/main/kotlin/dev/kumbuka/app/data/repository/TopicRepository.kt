package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.TopicDao
import dev.kumbuka.app.domain.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TopicRepository(private val topicDao: TopicDao) {
    /** All topics regardless of archive state - for merge/diff/history use. */
    fun observeByUnit(unitId: String): Flow<List<Topic>> =
        topicDao.observeByUnit(unitId).map { list -> list.map { it.toDomain() } }

    /** Active (non-archived) topics only - for study, search, export, and new mark links. */
    fun observeActiveByUnit(unitId: String): Flow<List<Topic>> =
        topicDao.observeByUnit(unitId).map { list -> list.filterNot { it.archived }.map { it.toDomain() } }

    fun observeAll(): Flow<List<Topic>> =
        topicDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<Topic>> =
        topicDao.observeActive().map { list -> list.map { it.toDomain() } }

    suspend fun getByUnitOnce(unitId: String): List<Topic> =
        topicDao.getByUnitOnce(unitId).map { it.toDomain() }

    suspend fun getActiveByUnitOnce(unitId: String): List<Topic> =
        topicDao.getActiveByUnitOnce(unitId).map { it.toDomain() }

    suspend fun getAllOnce(): List<Topic> = topicDao.getAllOnce().map { it.toDomain() }

    suspend fun getById(id: String): Topic? = topicDao.getById(id)?.toDomain()

    suspend fun upsert(topic: Topic) = topicDao.upsert(topic.toEntity())

    suspend fun upsertAll(topics: List<Topic>) = topicDao.upsertAll(topics.map { it.toEntity() })

    suspend fun delete(topic: Topic) = topicDao.delete(topic.toEntity())

    /** Hides a topic from active study/search/export while preserving every session and mark link. */
    suspend fun archive(id: String, now: Long = System.currentTimeMillis()) = topicDao.setArchived(id, true, now)

    /** Restores a previously archived topic in place (e.g. it reappears in a re-imported pack). */
    suspend fun unarchive(id: String, now: Long = System.currentTimeMillis()) = topicDao.setArchived(id, false, now)
}
