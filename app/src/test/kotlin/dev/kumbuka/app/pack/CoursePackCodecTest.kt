package dev.kumbuka.app.pack

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoursePackCodecTest {
    private val now = 1_700_000_000_000L

    @Test
    fun fromDomain_buildsExpectedPackShapeIncludingTopicLinks() {
        val unit = UnitModel(
            id = "u1",
            code = "ICS 3102",
            title = "Software Engineering",
            packId = "pack-se",
            packVersion = 4,
            createdAt = now,
            updatedAt = now,
        )
        val topicB = topic(id = "t-b", order = 1, title = "B topic")
        val topicA = topic(id = "t-a", order = 0, title = "A topic")
        val deadlineLate = deadline(id = "d-late", date = now + 3_000, topicIds = listOf("t-b"))
        val deadlineSoon = deadline(id = "d-soon", date = now + 1_000, topicIds = listOf("t-a", "t-b"))

        val pack = CoursePack.fromDomain(
            unit = unit,
            topics = listOf(topicB, topicA),
            deadlines = listOf(deadlineLate, deadlineSoon),
        )

        assertEquals("pack-se", pack.packId)
        assertEquals(4, pack.packVersion)
        assertEquals("ICS 3102", pack.unit.code)
        assertEquals(listOf("t-a", "t-b"), pack.topics.map { it.id })
        assertEquals(listOf("d-soon", "d-late"), pack.deadlines.map { it.id })
        assertEquals(listOf("t-a", "t-b"), pack.deadlines.first().topicIds)
        assertTrue(pack.topics.first().resourcePointers.contains("Lecture slides"))
    }

    @Test
    fun codec_roundTrip_preservesCoreFields() {
        val pack = CoursePack(
            packId = "pack-1",
            packVersion = 2,
            unit = CoursePackUnit(code = "ICS 2201", title = "AI"),
            topics = listOf(
                CoursePackTopic(
                    id = "t1",
                    title = "Search",
                    objective = "Explain BFS",
                    retrievalPrompt = "Walk through BFS",
                    examWeight = 0.6f,
                    orderIndex = 0,
                    resourcePointers = listOf("Lecture 3", "Chapter 2"),
                ),
            ),
            deadlines = listOf(
                CoursePackDeadline(
                    id = "d1",
                    title = "CAT 1",
                    date = now + 10_000,
                    kind = DeadlineKind.CAT,
                    topicIds = listOf("t1"),
                ),
            ),
        )

        val raw = CoursePackCodec.encode(pack)
        val decoded = CoursePackCodec.decode(raw)

        assertEquals(pack.packId, decoded.packId)
        assertEquals(pack.packVersion, decoded.packVersion)
        assertEquals(pack.unit.code, decoded.unit.code)
        assertEquals(pack.topics.first().retrievalPrompt, decoded.topics.first().retrievalPrompt)
        assertEquals(pack.topics.first().resourcePointers, decoded.topics.first().resourcePointers)
        assertEquals(pack.deadlines.first().topicIds, decoded.deadlines.first().topicIds)
        assertEquals(pack.deadlines.first().kind, decoded.deadlines.first().kind)
    }

    private fun topic(id: String, order: Int, title: String) = Topic(
        id = id,
        unitId = "u1",
        title = title,
        objective = "Objective $id",
        retrievalPrompt = "Prompt $id",
        examWeight = 0.5f,
        orderIndex = order,
        resourcePointers = listOf("Lecture slides"),
        titleEditedLocally = false,
        objectiveEditedLocally = false,
        weightEditedLocally = false,
        createdAt = now,
        updatedAt = now,
    )

    private fun deadline(id: String, date: Long, topicIds: List<String>) = Deadline(
        id = id,
        unitId = "u1",
        title = id,
        date = date,
        kind = DeadlineKind.EXAM,
        topicIds = topicIds,
        updatedAt = now,
    )
}

