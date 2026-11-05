package dev.kumbuka.app.domain.analytics

import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewLogExporterTest {
    private fun session(id: String, topic: String, started: Long, deferred: Boolean = false) = Session(
        id = id, topicId = topic, startedAt = started, endedAt = if (deferred) null else started + 60_000,
        plannedMinutes = 15, actualSeconds = 60, confidenceBefore = if (deferred) null else Confidence.SHAKY,
        confidenceAfter = if (deferred) null else Confidence.OK, wasDeferred = deferred, updatedAt = started,
        sourceArm = "baseline",
    )

    private val mark = AssessmentMark(
        id = "m1", unitId = "u1", score = 17.5f, outOf = 20f, kind = AssessmentKind.CAT,
        date = 1_700_000_000_000L, topicIds = listOf("t1", "t2"), updatedAt = 0L,
    )

    @Test
    fun headerAndRowsHaveTheSameColumnCount() {
        val csv = ReviewLogExporter.build(listOf(session("s1", "t1", 1000L)), listOf(mark), mapOf("t1" to "u1"))
        val lines = csv.trimEnd().split("\r\n")
        val columns = lines.first().split(",").size
        assertEquals(3, lines.size)
        lines.forEach { assertEquals(columns, it.split(",").size) }
    }

    @Test
    fun sessionRowCarriesRatingsAndDeferralButNoNames() {
        val csv = ReviewLogExporter.build(listOf(session("s1", "t1", 0L), session("s2", "t1", 5000L, deferred = true)), emptyList(), mapOf("t1" to "u1"))
        val rows = csv.trimEnd().split("\r\n").drop(1)
        assertEquals("session,s1,u1,t1,,1970-01-01T00:00:00Z,1970-01-01T00:01:00Z,15,60,1,2,0,baseline,,,", rows[0])
        assertTrue(rows[1].contains(",1,baseline"))
        assertFalse(csv.contains("title", ignoreCase = true))
    }

    @Test
    fun marksRowUsesUtcDateAndTrimsWholeNumbers() {
        val csv = ReviewLogExporter.build(emptyList(), listOf(mark), emptyMap())
        val row = csv.trimEnd().split("\r\n")[1]
        assertTrue(row.startsWith("mark,m1,u1,t1;t2,CAT,"))
        assertTrue(row.endsWith(",17.5,20,2023-11-14"))
    }

    @Test
    fun sampleTopicsAndUnitsAreExcluded() {
        val csv = ReviewLogExporter.build(
            listOf(session("keep", "t1", 1L), session("drop", "sample", 2L)),
            listOf(mark, mark.copy(id = "m2", unitId = "sampleUnit")),
            mapOf("t1" to "u1"),
            excludedTopicIds = setOf("sample"),
            excludedUnitIds = setOf("sampleUnit"),
        )
        assertTrue(csv.contains("keep"))
        assertFalse(csv.contains("drop"))
        assertTrue(csv.contains("m1"))
        assertFalse(csv.contains("m2"))
    }

    @Test
    fun fieldsWithCommasAreQuoted() {
        val csv = ReviewLogExporter.build(emptyList(), listOf(mark.copy(id = "a,b")), emptyMap())
        assertTrue(csv.contains("\"a,b\""))
    }
}
