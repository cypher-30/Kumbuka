package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kumbuka.app.domain.model.Confidence

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val confidenceBefore: Confidence?,
    val confidenceAfter: Confidence?,
    val wasDeferred: Boolean,
    val updatedAt: Long,
)
