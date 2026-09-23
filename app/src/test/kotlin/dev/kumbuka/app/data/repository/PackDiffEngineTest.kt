package dev.kumbuka.app.data.repository

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic
import dev.kumbuka.app.pack.CoursePackUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackDiffEngineTest {
    private val now = 1_700_000_000_000L

    @Test
    fun buildDiff_reportsAddUpdateConflictAndRemovedRows() {
        val localTopics = mapOf(
            "t-updated" to topic(id = "t-updated", objective = "Local objective"),
            "t-conflict" to topic(
                id = "t-conflict",
                title = "My Local Title",
                titleEditedLocally = true,
            ),
            "t-removed" to topic(id = "t-removed", title = "Removed local"),
        )
        val localDeadlines = mapOf(
            "d-updated" to deadline(id = "d-updated", date = now + 2000),
            "d-removed" to deadline(id = "d-removed", date = now + 3000),
        )
        val pack = coursePack(
            topics = listOf(
                packTopic(id = "t-added"),
                packTopic(id = "t-updated", objective = "Incoming objective"),
                packTopic(id = "t-conflict", title = "Incoming title"),
            ),
            deadlines = listOf(
                packDeadline(id = "d-added", date = now + 1000),
                packDeadline(id = "d-updated", date = now + 9999),
            ),
        )

        val diff = PackDiffEngine.buildDiff(pack, existingUnitId = "u1", localTopics = localTopics, localDeadlines = localDeadlines)

        assertEquals(1, diff.preview.topicsToAdd)
        assertEquals(1, diff.preview.topicsToUpdate)
        assertEquals(1, diff.preview.topicsWithConflicts)
        assertEquals(1, diff.preview.topicsMissingInPack)
        assertEquals(1, diff.preview.deadlinesToAdd)
        assertEquals(1, diff.preview.deadlinesToUpdate)
        assertEquals(1, diff.preview.deadlinesMissingInPack)
    }

    @Test
    fun mergeTopics_keepsLocalOnConflictByDefault() {
        val local = topic(
            id = "t1",
            title = "My custom title",
            titleEditedLocally = true,
        )
        val pack = coursePack(topics = listOf(packTopic(id = "t1", title = "Incoming title")))

        val merged = PackDiffEngine.mergeTopics(pack, unitId = "u1", localTopics = mapOf(local.id to local), now = now, conflictChoices = emptyMap())

        assertEquals(1, merged.size)
        assertEquals("My custom title", merged[0].title)
        assertTrue(merged[0].titleEditedLocally)
    }

    @Test
    fun mergeTopics_usesPackWhenConflictChoiceSaysSo() {
        val local = topic(
            id = "t1",
            title = "My custom title",
            titleEditedLocally = true,
        )
        val pack = coursePack(topics = listOf(packTopic(id = "t1", title = "Incoming title")))

        val merged = PackDiffEngine.mergeTopics(
            pack = pack,
            unitId = "u1",
            localTopics = mapOf(local.id to local),
            now = now,
            conflictChoices = mapOf("t1" to TopicConflictChoice.USE_PACK),
        )

        assertEquals(1, merged.size)
        assertEquals("Incoming title", merged[0].title)
        assertFalse(merged[0].titleEditedLocally)
    }

    private fun coursePack(
        topics: List<CoursePackTopic>,
        deadlines: List<CoursePackDeadline> = emptyList(),
    ) = CoursePack(
        packId = "pack-1",
        packVersion = 2,
        unit = CoursePackUnit(code = "ICS 3102", title = "Software Engineering"),
        topics = topics,
        deadlines = deadlines,
    )

    private fun packTopic(
        id: String,
        title: String = "Topic $id",
        objective: String = "Objective $id",
        retrievalPrompt: String = "Prompt $id",
        examWeight: Float = 0.5f,
        orderIndex: Int = 0,
    ) = CoursePackTopic(
        id = id,
        title = title,
        objective = objective,
        retrievalPrompt = retrievalPrompt,
        examWeight = examWeight,
        orderIndex = orderIndex,
        resourcePointers = listOf("Lecture slides"),
    )

    private fun topic(
        id: String,
        title: String = "Local $id",
        objective: String = "Local objective $id",
        titleEditedLocally: Boolean = false,
    ) = Topic(
        id = id,
        unitId = "u1",
        title = title,
        objective = objective,
        retrievalPrompt = "Local prompt",
        examWeight = 0.4f,
        orderIndex = 0,
        resourcePointers = listOf("Local notes"),
        titleEditedLocally = titleEditedLocally,
        objectiveEditedLocally = false,
        weightEditedLocally = false,
        createdAt = now,
        updatedAt = now,
    )

    private fun packDeadline(id: String, date: Long) = CoursePackDeadline(
        id = id,
        title = "Deadline $id",
        date = date,
        kind = DeadlineKind.CAT,
        topicIds = listOf("t-updated"),
    )

    private fun deadline(id: String, date: Long) = Deadline(
        id = id,
        unitId = "u1",
        title = "Deadline $id",
        date = date,
        kind = DeadlineKind.CAT,
        topicIds = listOf("t-updated"),
        updatedAt = now,
    )
}

