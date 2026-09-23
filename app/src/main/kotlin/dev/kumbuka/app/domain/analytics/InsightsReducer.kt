package dev.kumbuka.app.domain.analytics

import dev.kumbuka.app.data.repository.SchedulerEvaluationRecord
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit
import java.time.Instant
import java.time.ZoneOffset

private const val SECONDS_PER_MINUTE = 60

data class SessionInsights(
    val completedCount: Int,
    val nonDeferredCount: Int,
    val measuredMinutes: Int,
    val unmeasuredCount: Int,
)

data class AssessmentInsightPoint(
    val markId: String,
    val unitId: String,
    val kind: AssessmentKind,
    val date: Long,
    val scorePercent: Float,
    val averageConfidenceBefore: Float?,
    val coveredTopicCount: Int,
    val totalLinkedTopicCount: Int,
)

data class AssessmentInsights(
    val totalAssessmentCount: Int,
    val points: List<AssessmentInsightPoint>,
)

data class SchedulerInsights(
    val totalSelectedRecommendations: Int,
    val requestedCounts: Map<String, Int>,
    val actualCounts: Map<String, Int>,
    val fallbackCount: Int,
    val predictorVersion: String?,
)

data class InsightsReport(
    val sessionInsights: SessionInsights,
    val assessmentInsights: AssessmentInsights,
    val schedulerInsights: SchedulerInsights,
)

fun reduceInsights(
    units: List<Unit>,
    topics: List<Topic>,
    sessions: List<Session>,
    marks: List<AssessmentMark>,
    schedulerLogs: List<SchedulerEvaluationRecord>,
): InsightsReport {
    val sampleUnitIds = units.filter { it.isSample }.map { it.id }.toSet()
    val topicById = topics.associateBy { it.id }
    val sampleTopicIds = topics.filter { it.unitId in sampleUnitIds }.map { it.id }.toSet()

    val filteredSessions = sessions.filter { session ->
        topicById[session.topicId]?.unitId?.let { it !in sampleUnitIds } ?: true
    }
    val completedSessions = filteredSessions.filter { it.endedAt != null }
    val sessionInsights = SessionInsights(
        completedCount = completedSessions.size,
        nonDeferredCount = completedSessions.count { !it.wasDeferred },
        measuredMinutes = completedSessions.filter { it.actualSeconds > 0 }.sumOf { it.actualSeconds } / SECONDS_PER_MINUTE,
        unmeasuredCount = completedSessions.count { it.actualSeconds <= 0 },
    )

    val filteredMarks = marks.filter { mark ->
        mark.unitId !in sampleUnitIds && mark.topicIds.none { it in sampleTopicIds }
    }
    val assessmentPoints = filteredMarks.map { mark ->
        val markDate = utcDate(mark.date)
        val linkedTopicIds = mark.topicIds.filterNot { it in sampleTopicIds }
        val ratings = linkedTopicIds.mapNotNull { topicId ->
            completedSessions
                .filter { it.topicId == topicId && it.confidenceBefore != null && utcDate(it.startedAt).isBefore(markDate) }
                .maxByOrNull { it.startedAt }
                ?.confidenceBefore
                ?.score
                ?.toFloat()
        }
        AssessmentInsightPoint(
            markId = mark.id,
            unitId = mark.unitId,
            kind = mark.kind,
            date = mark.date,
            scorePercent = if (mark.outOf > 0f) (mark.score / mark.outOf) * 100f else 0f,
            averageConfidenceBefore = ratings.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
            coveredTopicCount = ratings.size,
            totalLinkedTopicCount = linkedTopicIds.size,
        )
    }.sortedByDescending { it.date }
    val assessmentInsights = AssessmentInsights(totalAssessmentCount = filteredMarks.size, points = assessmentPoints)

    val selectedLogs = schedulerLogs.filter { it.selected && !it.isSample }
    val schedulerInsights = SchedulerInsights(
        totalSelectedRecommendations = selectedLogs.size,
        requestedCounts = selectedLogs.groupingBy { it.requestedArm }.eachCount(),
        actualCounts = selectedLogs.groupingBy { it.actualSource }.eachCount(),
        fallbackCount = selectedLogs.count { it.actualSource == "baseline_fallback" || it.fallbackReason != null },
        predictorVersion = selectedLogs.mapNotNull { it.predictorVersion }.firstOrNull(),
    )

    return InsightsReport(
        sessionInsights = sessionInsights,
        assessmentInsights = assessmentInsights,
        schedulerInsights = schedulerInsights,
    )
}

private fun utcDate(epochMillis: Long) = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
