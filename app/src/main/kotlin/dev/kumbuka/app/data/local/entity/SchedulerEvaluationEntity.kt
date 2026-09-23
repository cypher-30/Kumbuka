package dev.kumbuka.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (plan, topic, arm): the research spine from DESIGN.md §4. Both
 * the baseline arm and the model arm are evaluated and logged for every Today
 * plan, regardless of which arm is actually shown to the student, so the two
 * can be compared later. `actualSource` records what really produced the
 * score ("baseline", "placeholder", or "baseline_fallback" when the
 * placeholder had no eligible history) - never claim a trained model ran
 * when only the hand-set placeholder did.
 */
@Entity(
    tableName = "scheduler_evaluations",
    indices = [Index("planId"), Index("topicId"), Index("evaluatedAt")],
)
data class SchedulerEvaluationEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val evaluatedAt: Long,
    val topicId: String,
    val unitId: String,
    val arm: String,
    val requestedArm: String,
    val actualSource: String,
    val fallbackReason: String?,
    val rank: Int,
    val selected: Boolean,
    val minutes: Int,
    val score: Float,
    val predictedRecall: Float?,
    val predictorVersion: String?,
    val isSample: Boolean,
)
