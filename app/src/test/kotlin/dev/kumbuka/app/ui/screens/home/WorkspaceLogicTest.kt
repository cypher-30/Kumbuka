package dev.kumbuka.app.ui.screens.home

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.ui.theme.unitMarkerIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class WorkspaceLogicTest {
    private val today = LocalDate.of(2026, 9, 10)

    private fun utcMillis(date: LocalDate) = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun deadline(id: String, date: LocalDate) =
        Deadline(id, "u1", id, utcMillis(date), DeadlineKind.CAT, emptyList(), 0L)

    private fun session(id: String, topicId: String, endedAt: LocalDateTime?, deferred: Boolean = false, seconds: Int = 600) =
        Session(
            id = id,
            topicId = topicId,
            startedAt = 0L,
            endedAt = endedAt?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
            plannedMinutes = 10,
            actualSeconds = seconds,
            confidenceBefore = null,
            confidenceAfter = null,
            wasDeferred = deferred,
            updatedAt = 0L,
        )

    @Test
    fun greetingPeriodsSwitchAtBoundaries() {
        assertEquals(GreetingPeriod.EVENING, greetingPeriodFor(4))
        assertEquals(GreetingPeriod.MORNING, greetingPeriodFor(5))
        assertEquals(GreetingPeriod.MORNING, greetingPeriodFor(11))
        assertEquals(GreetingPeriod.AFTERNOON, greetingPeriodFor(12))
        assertEquals(GreetingPeriod.AFTERNOON, greetingPeriodFor(16))
        assertEquals(GreetingPeriod.EVENING, greetingPeriodFor(17))
        assertEquals(GreetingPeriod.EVENING, greetingPeriodFor(23))
    }

    @Test
    fun nextGreetingBoundaryIncludesMidnight() {
        assertEquals(today.atTime(5, 0), nextGreetingBoundary(today.atTime(4, 59)))
        assertEquals(today.atTime(12, 0), nextGreetingBoundary(today.atTime(5, 0)))
        assertEquals(today.atTime(17, 0), nextGreetingBoundary(today.atTime(12, 0)))
        assertEquals(today.plusDays(1).atStartOfDay(), nextGreetingBoundary(today.atTime(17, 0)))
        assertEquals(today.plusDays(1).atStartOfDay(), nextGreetingBoundary(today.atTime(23, 59)))
    }

    @Test
    fun groupsDatesByCalendarDistance() {
        val sections = groupAssessmentDates(
            listOf(
                deadline("past2", today.minusDays(3)),
                deadline("later", today.plusDays(8)),
                deadline("week7", today.plusDays(7)),
                deadline("today", today),
                deadline("past1", today.minusDays(1)),
                deadline("tomorrow", today.plusDays(1)),
            ),
            today,
        )
        assertEquals(
            listOf(AssessmentDateGroup.TODAY, AssessmentDateGroup.NEXT_7_DAYS, AssessmentDateGroup.LATER, AssessmentDateGroup.PAST),
            sections.map { it.group },
        )
        assertEquals(listOf("tomorrow", "week7"), sections[1].deadlines.map { it.id })
        assertEquals(listOf("past1", "past2"), sections[3].deadlines.map { it.id })
    }

    @Test
    fun emptyGroupsAreOmitted() {
        val sections = groupAssessmentDates(listOf(deadline("later", today.plusDays(30))), today)
        assertEquals(listOf(AssessmentDateGroup.LATER), sections.map { it.group })
    }

    @Test
    fun upcomingPreviewSkipsPastAndLimits() {
        val preview = upcomingPreview(
            listOf(
                deadline("past", today.minusDays(1)),
                deadline("d4", today.plusDays(4)),
                deadline("d0", today),
                deadline("d2", today.plusDays(2)),
                deadline("d9", today.plusDays(9)),
            ),
            today,
        )
        assertEquals(listOf("d0", "d2", "d4"), preview.map { it.id })
    }

    @Test
    fun calendarDueDaysUsesEarliestLinkedDeadline() {
        val deadlines = listOf(
            deadline("a", today.plusDays(7)).copy(topicIds = listOf("t1")),
            deadline("b", today.plusDays(3)).copy(topicIds = listOf("t1", "t2")),
            deadline("c", today.plusDays(1)).copy(topicIds = listOf("t3")),
        )
        assertEquals(3L, calendarDueDays("t1", deadlines, today))
        assertEquals(3L, calendarDueDays("t2", deadlines, today))
        assertNull(calendarDueDays("t4", deadlines, today))
    }

    @Test
    fun recentActivityExcludesDeferredAndSampleSessions() {
        val sessions = listOf(
            session("a", "t1", today.atTime(9, 0)),
            session("b", "t1", today.atTime(10, 0), seconds = 1200),
            session("c", "t1", today.atTime(11, 0), deferred = true),
            session("d", "sample", today.atTime(12, 0)),
            session("e", "t1", today.minusDays(2).atTime(8, 0)),
            session("f", "t1", today.minusDays(9).atTime(8, 0)),
            session("g", "t1", null),
        )
        val activity = recentActivity(sessions, today, ZoneOffset.UTC, excludedTopicIds = setOf("sample"))
        assertEquals(7, activity.size)
        assertEquals(today.minusDays(6), activity.first().date)
        assertEquals(2, activity.last().completedReviews)
        assertEquals(30, activity.last().measuredMinutes)
        assertEquals(1, activity[4].completedReviews)
        assertEquals(3, activity.sumOf { it.completedReviews })
    }

    @Test
    fun lastReviewIgnoresDeferredSessions() {
        val latest = today.atTime(10, 0)
        val result = lastReviewByTopic(
            listOf(
                session("a", "t1", today.atTime(9, 0)),
                session("b", "t1", latest),
                session("c", "t1", today.atTime(11, 0), deferred = true),
                session("d", "t2", today.atTime(11, 0), deferred = true),
            ),
        )
        assertEquals(latest.toInstant(ZoneOffset.UTC).toEpochMilli(), result["t1"])
        assertNull(result["t2"])
    }

    @Test
    fun storedDatesRoundTripAsUtcCalendarDays() {
        val millis = parseIsoDateMillis("2026-11-13")!!
        assertEquals("2026-11-13", formatEpochDateUtc(millis))
        assertEquals(LocalDate.of(2026, 11, 13), storedCalendarDate(millis))
        assertNull(parseIsoDateMillis("13/11/2026"))
    }

    @Test
    fun decimalInputAcceptsCommas() {
        assertEquals(12.5f, parseDecimalInput(" 12,5 "))
        assertEquals(30f, parseDecimalInput("30"))
        assertNull(parseDecimalInput("abc"))
        assertNull(parseDecimalInput("NaN"))
    }

    @Test
    fun unitBadgeTextPrefersCourseNumber() {
        assertEquals("3102", dev.kumbuka.app.ui.components.unitBadgeText("ICS 3102"))
        assertEquals("320X", dev.kumbuka.app.ui.components.unitBadgeText("HED 320X"))
        assertEquals("210", dev.kumbuka.app.ui.components.unitBadgeText("CS 210"))
        assertEquals("BIOL", dev.kumbuka.app.ui.components.unitBadgeText("Biology"))
        assertEquals("?", dev.kumbuka.app.ui.components.unitBadgeText("?"))
    }

    @Test
    fun unitMarkerIndexIsStableAndInRange() {
        assertEquals(unitMarkerIndex("unit-ics-2201", 8), unitMarkerIndex("unit-ics-2201", 8))
        listOf("", "a", "unit-zz", "x".repeat(200)).forEach {
            assertTrue(unitMarkerIndex(it, 8) in 0 until 8)
        }
    }
}
