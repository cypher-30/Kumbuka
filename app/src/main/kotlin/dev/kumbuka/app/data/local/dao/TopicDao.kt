package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.kumbuka.app.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
