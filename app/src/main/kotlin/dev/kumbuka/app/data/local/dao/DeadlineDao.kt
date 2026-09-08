package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.kumbuka.app.data.local.entity.DeadlineEntity
import dev.kumbuka.app.data.local.entity.DeadlineTopicCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadlineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deadline: DeadlineEntity)

    @Delete
    suspend fun delete(deadline: DeadlineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: DeadlineTopicCrossRef)

    @Query("DELETE FROM deadline_topic_cross_ref WHERE deadlineId = :deadlineId")
    suspend fun clearCrossRefsForDeadline(deadlineId: String)

    @Query("SELECT * FROM deadlines WHERE unitId = :unitId ORDER BY date ASC")
    fun observeByUnit(unitId: String): Flow<List<DeadlineEntity>>

    @Query("SELECT topicId FROM deadline_topic_cross_ref WHERE deadlineId = :deadlineId")
    suspend fun getTopicIdsForDeadline(deadlineId: String): List<String>

    @Query(
        """
        SELECT d.* FROM deadlines d
        INNER JOIN deadline_topic_cross_ref x ON x.deadlineId = d.id
        WHERE x.topicId = :topicId
        ORDER BY d.date ASC
        """,
    )
    suspend fun getDeadlinesForTopic(topicId: String): List<DeadlineEntity>

    @Query(
        """
        SELECT d.* FROM deadlines d
        INNER JOIN deadline_topic_cross_ref x ON x.deadlineId = d.id
        WHERE x.topicId = :topicId
        ORDER BY d.date ASC LIMIT 1
        """,
    )
    suspend fun getNearestDeadlineForTopic(topicId: String): DeadlineEntity?
}
