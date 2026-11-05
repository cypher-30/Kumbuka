package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kumbuka.app.domain.model.AssessmentKind

/**
 * DESIGN.md §3: "the only objective signal in the whole system" - a CAT,
 * assignment, or past-paper score, tagged to the topics it covered.
 */
@Entity(
    tableName = "assessment_marks",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("unitId")],
)
data class AssessmentMarkEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val score: Float,
    val outOf: Float,
    val kind: AssessmentKind,
    val date: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "assessment_mark_topic_cross_ref",
    primaryKeys = ["assessmentMarkId", "topicId"],
    foreignKeys = [
        ForeignKey(
            entity = AssessmentMarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentMarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assessmentMarkId"), Index("topicId")],
)
data class AssessmentMarkTopicCrossRef(
    val assessmentMarkId: String,
    val topicId: String,
)
