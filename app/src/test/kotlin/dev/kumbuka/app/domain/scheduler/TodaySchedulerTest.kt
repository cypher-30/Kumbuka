package dev.kumbuka.app.domain.scheduler

import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodaySchedulerTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun scoreTopic_matchesThePublishedFormula() {
        val topic = topic(weight = 0.5f)
        val session = Session(
            id = "s1",
            topicId = topic.id,
            startedAt = now - (14 * day),
            endedAt = now - (14 * day),
            plannedMinutes = 10,
            actualSeconds = 600,
            confidenceBefore = Confidence.SHAKY,
            confidenceAfter = Confidence.OK,
            wasDeferred = true,
            updatedAt = now - (14 * day),
        )
        val deadline = deadline(date = now)

        val scored = scoreTopic(topic, listOf(session), listOf(deadline), now)

        assertEquals(0.7866667f, scored.breakdown.score, 0.0001f)
        assertEquals(0.6666667f, scored.breakdown.gap, 0.0001f)
        assertEquals(1f, scored.breakdown.staleness, 0.0001f)
        assertEquals(1f, scored.breakdown.urgency, 0.0001f)
        assertEquals(0.2f, scored.breakdown.avoidance, 0.0001f)
    }

    @Test
    fun buildTodayPlan_returnsNoUnitsWhenNothingImported() {
        val plan = buildTodayPlan(units = emptyList(), topics = emptyList(), sessions = emptyList(), deadlines = emptyList(), nowMillis = now)

        assertEquals(TodayState.NoUnits, plan.state)
        assertTrue(plan.cards.isEmpty())
    }

    @Test
    fun buildTodayPlan_returnsFreshStartAndAllocatesByExamWeight() {
        val units = listOf(unit())
        val topics = listOf(
            topic(id = "t1", weight = 0.75f, orderIndex = 0),
            topic(id = "t2", weight = 0.25f, orderIndex = 1),
        )

        val plan = buildTodayPlan(units = units, topics = topics, sessions = emptyList(), deadlines = emptyList(), nowMillis = now)

        assertEquals(TodayState.FreshStart, plan.state)
        assertEquals(2, plan.cards.size)
        assertEquals(45, plan.cards.first { it.topicId == "t1" }.minutes)
        assertEquals(15, plan.cards.first { it.topicId == "t2" }.minutes)
        assertEquals(60, plan.cards.sumOf { it.minutes })
    }

    @Test
    fun buildTodayPlan_returnsAllCaughtUpForRecentSolidReview() {
        val units = listOf(unit())
        val topic = topic()
        val recentSession = Session(
            id = "s1",
            topicId = topic.id,
            startedAt = now - day,
            endedAt = now - day,
            plannedMinutes = 10,
            actualSeconds = 600,
            confidenceBefore = Confidence.SOLID,
            confidenceAfter = Confidence.SOLID,
            wasDeferred = false,
            updatedAt = now - day,
        )

        val plan = buildTodayPlan(units = units, topics = listOf(topic), sessions = listOf(recentSession), deadlines = emptyList(), nowMillis = now)

        assertEquals(TodayState.AllCaughtUp, plan.state)
        assertTrue(plan.cards.isEmpty())
    }

    @Test
    fun schedulerUrgency_matchesDeadlineWindow() {
        assertEquals(1f, schedulerUrgency(now, now), 0.0001f)
        assertEquals(0.5f, schedulerUrgency(now + (10 * day), now), 0.05f)
        assertEquals(0f, schedulerUrgency(now + (30 * day), now), 0.0001f)
        assertEquals(0, daysUntilDeadline(now - (2 * day), now))
    }

    private fun unit() = UnitModel(
        id = "u1",
        code = "ICS 3102",
        title = "Software Engineering",
        packId = "pack-1",
        packVersion = 1,
        createdAt = now,
        updatedAt = now,
    )

    private fun topic(
        id: String = "t1",
        weight: Float = 0.5f,
        orderIndex: Int = 0,
    ) = Topic(
        id = id,
        unitId = "u1",
        title = "Graph Colouring",
        objective = "Explain greedy colouring",
        retrievalPrompt = "Explain graph colouring",
        examWeight = weight,
        orderIndex = orderIndex,
        resourcePointers = listOf("Lecture 6 slides"),
        titleEditedLocally = false,
        objectiveEditedLocally = false,
        weightEditedLocally = false,
        createdAt = now,
        updatedAt = now,
    )

    private fun deadline(date: Long) = Deadline(
        id = "d1",
        unitId = "u1",
        title = "CAT 1",
        date = date,
        kind = DeadlineKind.EXAM,
        topicIds = listOf("t1"),
        updatedAt = now,
    )
}




