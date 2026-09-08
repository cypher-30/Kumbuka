package dev.kumbuka.app.domain.model

/**
 * Domain-layer models, decoupled from the Room-annotated entities in
 * data.local.entity. The repository layer maps between the two so the
 * scheduler, pack import/export, and UI never depend on Room directly.
 *
 * Every id is a stable UUID string and every row carries updatedAt -
 * never an autoincrement id - so a pack re-import can update a row in
 * place without breaking a student's existing history (DESIGN.md §7).
 */

data class Unit(
    val id: String,
    val code: String,
    val title: String,
    val packId: String?,
    val packVersion: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Topic(
    val id: String,
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

data class Session(
    val id: String,
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

data class Deadline(
    val id: String,
    val unitId: String,
    val title: String,
    val date: Long,
    val kind: DeadlineKind,
    val topicIds: List<String>,
    val updatedAt: Long,
)

data class AssessmentMark(
    val id: String,
    val unitId: String,
    val score: Float,
    val outOf: Float,
    val kind: AssessmentKind,
    val date: Long,
    val topicIds: List<String>,
    val updatedAt: Long,
)
