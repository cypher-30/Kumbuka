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
    /**
     * Set when a topic is removed from a re-imported pack and the student
     * chooses not to keep studying it. Never physically deleted - archiving
     * hides it from active scheduling/search/export while preserving every
     * session and assessment-mark link (DESIGN.md §7: re-import "never
     * touches a student's existing confidence or session history").
     * Re-importing the same topic id un-archives it in place.
     */
    val archived: Boolean = false,
)
