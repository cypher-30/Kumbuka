package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.kumbuka.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    // Must be a real UPDATE-or-INSERT, not @Insert(onConflict = REPLACE): REPLACE
    // deletes the conflicting row before re-inserting, which cascades through
    // SessionEntity's ON DELETE CASCADE FK and would silently wipe a student's
    // session history for this topic on every reimport.
    @Upsert
    suspend fun upsert(topic: TopicEntity)

    @Upsert
    suspend fun upsertAll(topics: List<TopicEntity>)

    @Update
    suspend fun update(topic: TopicEntity)

    @Delete
    suspend fun delete(topic: TopicEntity)

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE unitId = :unitId ORDER BY orderIndex ASC")
    fun observeByUnit(unitId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE unitId = :unitId ORDER BY orderIndex ASC")
    suspend fun getByUnitOnce(unitId: String): List<TopicEntity>

    @Query("SELECT * FROM topics")
    fun observeAll(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics")
    suspend fun getAllOnce(): List<TopicEntity>
}
