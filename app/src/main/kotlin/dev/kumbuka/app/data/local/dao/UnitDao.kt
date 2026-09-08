package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.kumbuka.app.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unit: UnitEntity)

    @Update
    suspend fun update(unit: UnitEntity)

    @Delete
    suspend fun delete(unit: UnitEntity)

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: String): UnitEntity?

    @Query("SELECT * FROM units ORDER BY title ASC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE packId = :packId LIMIT 1")
    suspend fun getByPackId(packId: String): UnitEntity?
}
