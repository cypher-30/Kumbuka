package dev.kumbuka.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.kumbuka.app.data.local.entity.AssessmentMarkEntity
import dev.kumbuka.app.data.local.entity.AssessmentMarkTopicCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentMarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mark: AssessmentMarkEntity)

    @Delete
    suspend fun delete(mark: AssessmentMarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: AssessmentMarkTopicCrossRef)

    @Query("DELETE FROM assessment_mark_topic_cross_ref WHERE assessmentMarkId = :markId")
    suspend fun clearCrossRefsForMark(markId: String)

    @Query("SELECT * FROM assessment_marks ORDER BY date DESC")
    fun observeAll(): Flow<List<AssessmentMarkEntity>>

    @Query("SELECT * FROM assessment_marks WHERE unitId = :unitId ORDER BY date DESC")
    fun observeByUnit(unitId: String): Flow<List<AssessmentMarkEntity>>

    @Query("SELECT topicId FROM assessment_mark_topic_cross_ref WHERE assessmentMarkId = :markId")
    suspend fun getTopicIdsForMark(markId: String): List<String>

    @Query(
        """
        SELECT m.* FROM assessment_marks m
        INNER JOIN assessment_mark_topic_cross_ref x ON x.assessmentMarkId = m.id
        WHERE x.topicId = :topicId
        ORDER BY m.date DESC
        """,
    )
    suspend fun getMarksForTopic(topicId: String): List<AssessmentMarkEntity>
}
