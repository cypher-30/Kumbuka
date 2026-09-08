package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.SessionDao
import dev.kumbuka.app.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(private val sessionDao: SessionDao) {
    fun observeByTopic(topicId: String): Flow<List<Session>> =
        sessionDao.observeByTopic(topicId).map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getByTopicOnce(topicId: String): List<Session> =
        sessionDao.getByTopicOnce(topicId).map { it.toDomain() }

    suspend fun getLatestForTopic(topicId: String): Session? =
        sessionDao.getLatestForTopic(topicId)?.toDomain()

    suspend fun countDeferralsForTopic(topicId: String): Int =
        sessionDao.countDeferralsForTopic(topicId)

    suspend fun upsert(session: Session) = sessionDao.upsert(session.toEntity())
}
