package dev.kumbuka.app.domain.analytics

import dev.kumbuka.app.data.repository.SchedulerEvaluationRecord
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsReducerTest {
    private val day = 86_400_000L
    private val now = 1_700_000_000_000L

    @Test
    fun excludesSameDaySessionsFromAssessmentPairing() {
        val unit = unit()
        val topic = topic()
        val markDate = now
        val sameDaySession = session(topic.id, markDate + 1_000L, Confidence.OK)

        val report = reduceInsights(
            units = listOf(unit),
            topics = listOf(topic),
            sessions = listOf(sameDaySession),
            marks = listOf(mark(markDate, listOf(topic.id))),
            schedulerLogs = emptyList(),
        )

        assertNull(report.assessmentInsights.points.single().averageConfidenceBefore)
    }

    @Test
    fun leavesAverageNullWhenNoLinkedTopicHasPriorRating() {
        val unit = unit()
        val topic = topic()

        val report = reduceInsights(
            units = listOf(unit),
            topics = listOf(topic),
            sessions = listOf(session(topic.id, now - day, null)),
            marks = listOf(mark(now, listOf(topic.id))),
            schedulerLogs = emptyList(),
        )

        val point = report.assessmentInsights.points.single()
        assertNull(point.averageConfidenceBefore)
        assertEquals(0, point.coveredTopicCount)
        assertEquals(1, point.totalLinkedTopicCount)
    }

    @Test
    fun averagesOnlyTopicsWithAvailablePriorRatings() {
        val unit = unit()
        val rated = topic(id = "t1")
        val unrated = topic(id = "t2")

        val report = reduceInsights(
            units = listOf(unit),
            topics = listOf(rated, unrated),
            sessions = listOf(session(rated.id, now - (2 * day), Confidence.SOLID)),
            marks = listOf(mark(now, listOf(rated.id, unrated.id))),
            schedulerLogs = emptyList(),
        )

        val point = report.assessmentInsights.points.single()
        assertEquals(3f, point.averageConfidenceBefore!!, 0.0001f)
        assertEquals(1, point.coveredTopicCount)
        assertEquals(2, point.totalLinkedTopicCount)
    }

    @Test
    fun excludesSampleUnitsTopicsAndMarksByDefault() {
        val sampleUnit = unit(id = "sample", sample = true)
        val sampleTopic = topic(id = "sample-topic", unitId = sampleUnit.id)

        val report = reduceInsights(
            units = listOf(sampleUnit),
            topics = listOf(sampleTopic),
            sessions = listOf(session(sampleTopic.id, now - day, Confidence.OK)),
            marks = listOf(mark(now, listOf(sampleTopic.id), unitId = sampleUnit.id)),
            schedulerLogs = listOf(log(isSample = true)),
        )

        assertEquals(0, report.sessionInsights.completedCount)
        assertEquals(0, report.assessmentInsights.totalAssessmentCount)
        assertEquals(0, report.schedulerInsights.totalSelectedRecommendations)
    }

    @Test
    fun handlesEmptyDatasetsWithoutFabricatingValues() {
        val report = reduceInsights(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(0, report.sessionInsights.completedCount)
        assertEquals(0, report.sessionInsights.measuredMinutes)
        assertEquals(0, report.assessmentInsights.totalAssessmentCount)
        assertEquals(0, report.schedulerInsights.totalSelectedRecommendations)
    }

    @Test
    fun countsSchedulerFallbackRecommendationsSeparately() {
        val report = reduceInsights(
            units = listOf(unit()),
            topics = listOf(topic()),
            sessions = emptyList(),
            marks = emptyList(),
            schedulerLogs = listOf(
                log(requestedArm = "placeholder", actualSource = "baseline_fallback", fallbackReason = "cold_start"),
                log(requestedArm = "baseline", actualSource = "baseline", id = "p2"),
            ),
        )

        assertEquals(2, report.schedulerInsights.totalSelectedRecommendations)
        assertEquals(1, report.schedulerInsights.requestedCounts["placeholder"])
        assertEquals(1, report.schedulerInsights.fallbackCount)
    }

    private fun unit(id: String = "u1", sample: Boolean = false) = Unit(
        id = id,
        code = "ICS 3102",
        title = "Software Engineering",
        packId = "pack-1",
        packVersion = 1,
        createdAt = now,
        updatedAt = now,
        isSample = sample,
    )

    private fun topic(id: String = "t1", unitId: String = "u1") = Topic(
        id = id,
        unitId = unitId,
        title = "Graphs",
        objective = "Explain graphs",
        retrievalPrompt = "Recall the graph definition",
        examWeight = 0.5f,
        orderIndex = 0,
        resourcePointers = emptyList(),
        titleEditedLocally = false,
        objectiveEditedLocally = false,
        weightEditedLocally = false,
        createdAt = now,
        updatedAt = now,
    )

    private fun session(topicId: String, startedAt: Long, confidenceBefore: Confidence?) = Session(
        id = "s-$topicId-$startedAt",
        topicId = topicId,
        startedAt = startedAt,
        endedAt = startedAt + 1_000L,
        plannedMinutes = 30,
        actualSeconds = 900,
        confidenceBefore = confidenceBefore,
        confidenceAfter = Confidence.OK,
        wasDeferred = false,
        updatedAt = startedAt + 1_000L,
    )

    private fun mark(date: Long, topicIds: List<String>, unitId: String = "u1") = AssessmentMark(
        id = "m-$date",
        unitId = unitId,
        score = 80f,
        outOf = 100f,
        kind = AssessmentKind.CAT,
        date = date,
        topicIds = topicIds,
        updatedAt = date,
    )

    private fun log(
        id: String = "p1",
        requestedArm: String = "baseline",
        actualSource: String = "baseline",
        fallbackReason: String? = null,
        isSample: Boolean = false,
    ) = SchedulerEvaluationRecord(
        planId = id,
        evaluatedAt = now,
        topicId = "t1",
        unitId = "u1",
        arm = requestedArm,
        requestedArm = requestedArm,
        actualSource = actualSource,
        fallbackReason = fallbackReason,
        rank = 0,
        selected = true,
        minutes = 30,
        score = 0.5f,
        predictedRecall = null,
        predictorVersion = null,
        isSample = isSample,
    )
}
