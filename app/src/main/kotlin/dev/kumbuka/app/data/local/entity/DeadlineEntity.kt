package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kumbuka.app.domain.model.DeadlineKind

@Entity(
    tableName = "deadlines",
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
data class DeadlineEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val date: Long,
    val kind: DeadlineKind,
    val updatedAt: Long,
)

/** A deadline covers one or more topics (DESIGN.md §2: "deadlines, tied to the topics they cover"). */
@Entity(
    tableName = "deadline_topic_cross_ref",
    primaryKeys = ["deadlineId", "topicId"],
    foreignKeys = [
        ForeignKey(
            entity = DeadlineEntity::class,
            parentColumns = ["id"],
            childColumns = ["deadlineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deadlineId"), Index("topicId")],
)
data class DeadlineTopicCrossRef(
    val deadlineId: String,
    val topicId: String,
)
