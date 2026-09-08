package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
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
data class TopicEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val objective: String,
    val retrievalPrompt: String,
    val examWeight: Float,
    val orderIndex: Int,
    val resourcePointers: List<String>,
    val titleEditedLocally: Boolean,
    val objectiveEditedLocally: Boolean,
    val weightEditedLocally: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
