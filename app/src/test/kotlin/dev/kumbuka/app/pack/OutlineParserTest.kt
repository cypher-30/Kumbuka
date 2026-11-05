package dev.kumbuka.app.pack

import dev.kumbuka.app.domain.model.DeadlineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class OutlineParserTest {
    private val sample = """
        ICS 3108 SERVER ADMINISTRATION
        Course outline, weeks 1-12 (from class rep)
        1. Introduction to server operating systems
        2. User & group management
        3. File permissions & ACLs
        4. DNS & DHCP configuration
        5. Web servers: Apache and Nginx
        6. Backup & recovery strategies
        CAT 1 - 6th Nov (topics 1-3)
        CAT 2 - 27th Nov (topics 4-6)
        Ref: Lecture notes, Nemeth ch. 6
    """.trimIndent()

    @Test
    fun parsesTheDesignSampleOutline() {
        val parsed = ParsedOutlineAssert(OutlineParser.parse(sample))
        parsed.code("ICS 3108")
        parsed.title("Server Administration")
        assertEquals(6, parsed.value.topics.size)
        assertEquals("Introduction to server operating systems", parsed.value.topics.first())
        assertEquals(2, parsed.value.dates.size)
        assertEquals(listOf("Lecture notes", "Nemeth ch. 6"), parsed.value.references)
        assertEquals(listOf("Course outline, weeks 1-12 (from class rep)"), parsed.value.skipped)
        assertTrue(parsed.value.isUsable)
    }

    @Test
    fun datesCarryTitleDayMonthAndTopicRange() {
        val dates = OutlineParser.parse(sample).dates
        assertEquals("CAT 1", dates[0].title)
        assertEquals(6, dates[0].day)
        assertEquals(Month.NOVEMBER, dates[0].month)
        assertEquals(1..3, dates[0].topicRange)
        assertEquals(4..6, dates[1].topicRange)
        assertEquals(DeadlineKind.CAT, dates[0].kind)
    }

    @Test
    fun dateKindIsInferredFromTheTitle() {
        val parsed = OutlineParser.parse("End Semester Exam - 12 Dec\nAssignment 2 - 3rd Oct\nLab practical - 4 Oct")
        assertEquals(listOf(DeadlineKind.EXAM, DeadlineKind.ASSIGNMENT, DeadlineKind.ASSIGNMENT), parsed.dates.map { it.kind })
    }

    @Test
    fun bulletStylesAllBecomeTopics() {
        val parsed = OutlineParser.parse("- Alpha\n* Beta\n• Gamma\n4) Delta")
        assertEquals(listOf("Alpha", "Beta", "Gamma", "Delta"), parsed.topics)
    }

    @Test
    fun textWithNoTopicsIsNotUsable() {
        val parsed = OutlineParser.parse("just some words\nand more words")
        assertFalse(parsed.isUsable)
        assertEquals(2, parsed.skipped.size)
        assertEquals("", parsed.code)
    }

    @Test
    fun onlyTheFirstCodeLineNamesTheUnit() {
        val parsed = OutlineParser.parse("MAT 201 Linear algebra\nCS 210 Data structures\n1. Vectors")
        assertEquals("MAT 201", parsed.code)
        assertEquals("Linear Algebra", parsed.title)
        assertEquals(listOf("CS 210 Data structures"), parsed.skipped)
    }

    @Test
    fun resolveUsesNextOccurrenceOnOrAfterToday() {
        val date = OutlineDate("CAT", 6, Month.NOVEMBER, null)
        assertEquals(LocalDate.of(2026, 11, 6), date.resolve(LocalDate.of(2026, 10, 16)))
        assertEquals(LocalDate.of(2027, 11, 6), date.resolve(LocalDate.of(2026, 11, 7)))
        assertEquals(LocalDate.of(2026, 11, 6), date.resolve(LocalDate.of(2026, 11, 6)))
        val leap = OutlineDate("CAT", 29, Month.FEBRUARY, null)
        assertEquals(LocalDate.of(2027, 2, 28), leap.resolve(LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun emptyTopicRangeIsNull() {
        assertNull(OutlineParser.parse("CAT 1 - 6 Nov").dates.single().topicRange)
    }

    private class ParsedOutlineAssert(val value: ParsedOutline) {
        fun code(expected: String) = assertEquals(expected, value.code)
        fun title(expected: String) = assertEquals(expected, value.title)
    }
}
