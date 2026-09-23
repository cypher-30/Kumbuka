package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.kumbuka.app.data.local.entity.SchedulerEvaluationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchedulerEvaluationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SchedulerEvaluationEntity>)

    @Query("SELECT * FROM scheduler_evaluations ORDER BY evaluatedAt DESC")
    fun observeAll(): Flow<List<SchedulerEvaluationEntity>>

    @Query("SELECT * FROM scheduler_evaluations WHERE planId = :planId")
    suspend fun getByPlan(planId: String): List<SchedulerEvaluationEntity>

    @Query("SELECT COUNT(*) FROM scheduler_evaluations WHERE planId = :planId")
    suspend fun countForPlan(planId: String): Int
}
