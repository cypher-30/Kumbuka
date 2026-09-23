package dev.kumbuka.app.data.repository

import dev.kumbuka.app.data.local.dao.SchedulerEvaluationDao
import dev.kumbuka.app.data.local.entity.SchedulerEvaluationEntity
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.scheduler.ArmEvaluation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class SchedulerEvaluationRecord(
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

private fun SchedulerEvaluationEntity.toDomain() = SchedulerEvaluationRecord(
    planId, evaluatedAt, topicId, unitId, arm, requestedArm, actualSource, fallbackReason,
    rank, selected, minutes, score, predictedRecall, predictorVersion, isSample,
)

/**
 * Persists both-arm Today evaluations for the research comparison in
 * DESIGN.md §4. Every call writes one row per (topic, arm) inside a single
 * transactional insert so a plan is either fully logged or not logged at
 * all - there is no partial write that could look like a completed
 * evaluation.
 */
class SchedulerLogRepository(private val dao: SchedulerEvaluationDao) {
    fun observeAll(): Flow<List<SchedulerEvaluationRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun logPlan(
        baseline: ArmEvaluation,
        placeholder: ArmEvaluation,
        unitIdByTopicId: Map<String, String>,
        sampleTopicIds: Set<String>,
        evaluatedAt: Long,
    ): String {
        val planId = UUID.randomUUID().toString()
        val rows = buildList {
            listOf(baseline, placeholder).forEach { evaluation ->
                evaluation.rankedTopics.forEachIndexed { index, scored ->
                    val topicId = scored.topic.id
                    add(
                        SchedulerEvaluationEntity(
                            id = "$planId:$topicId:${evaluation.arm.name}",
                            planId = planId,
                            evaluatedAt = evaluatedAt,
                            topicId = topicId,
                            unitId = unitIdByTopicId[topicId] ?: scored.topic.unitId,
                            arm = evaluation.arm.name.lowercase(),
                            requestedArm = evaluation.requestedArm.name.lowercase(),
                            actualSource = scored.breakdown.actualSource,
                            fallbackReason = scored.breakdown.fallbackReason,
                            rank = index,
                            selected = topicId in evaluation.selectedTopicIds,
                            minutes = evaluation.cards.firstOrNull { it.topicId == topicId }?.minutes ?: 0,
                            score = scored.breakdown.score,
                            predictedRecall = scored.breakdown.predictedRecall,
                            predictorVersion = scored.breakdown.predictorVersion,
                            isSample = topicId in sampleTopicIds,
                        ),
                    )
                }
            }
        }
        if (rows.isNotEmpty()) {
            dao.insertAll(rows)
        }
        return planId
    }
}
