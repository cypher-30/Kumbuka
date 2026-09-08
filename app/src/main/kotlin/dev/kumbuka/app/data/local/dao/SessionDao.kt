package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.kumbuka.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE topicId = :topicId ORDER BY startedAt DESC")
    fun observeByTopic(topicId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE topicId = :topicId ORDER BY startedAt DESC")
    suspend fun getByTopicOnce(topicId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE topicId = :topicId ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestForTopic(topicId: String): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions WHERE topicId = :topicId AND wasDeferred = 1")
    suspend fun countDeferralsForTopic(topicId: String): Int

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>
}
