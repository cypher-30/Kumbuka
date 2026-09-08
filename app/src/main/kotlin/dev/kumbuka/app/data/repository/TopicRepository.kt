package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.TopicDao
import dev.kumbuka.app.domain.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TopicRepository(private val topicDao: TopicDao) {
    fun observeByUnit(unitId: String): Flow<List<Topic>> =
        topicDao.observeByUnit(unitId).map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Topic>> =
        topicDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getByUnitOnce(unitId: String): List<Topic> =
        topicDao.getByUnitOnce(unitId).map { it.toDomain() }

    suspend fun getAllOnce(): List<Topic> = topicDao.getAllOnce().map { it.toDomain() }

    suspend fun getById(id: String): Topic? = topicDao.getById(id)?.toDomain()

    suspend fun upsert(topic: Topic) = topicDao.upsert(topic.toEntity())

    suspend fun upsertAll(topics: List<Topic>) = topicDao.upsertAll(topics.map { it.toEntity() })

    suspend fun delete(topic: Topic) = topicDao.delete(topic.toEntity())
}
