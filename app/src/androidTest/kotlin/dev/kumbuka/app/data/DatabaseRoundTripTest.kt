package dev.kumbuka.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kumbuka.app.data.local.KumbukaDatabase
import dev.kumbuka.app.data.local.entity.DeadlineEntity
import dev.kumbuka.app.data.local.entity.DeadlineTopicCrossRef
import dev.kumbuka.app.data.local.entity.SessionEntity
import dev.kumbuka.app.data.local.entity.TopicEntity
import dev.kumbuka.app.data.local.entity.UnitEntity
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.DeadlineKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseRoundTripTest {
    private lateinit var db: KumbukaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, KumbukaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun unitTopicSession_roundTripsThroughConverters() = runBlocking {
        val now = System.currentTimeMillis()
        db.unitDao().upsert(
            UnitEntity("u1", "ICS 3102", "Software Eng", null, null, now, now),
        )
        db.topicDao().upsert(
            TopicEntity(
                id = "t1",
                unitId = "u1",
                title = "Graph Colouring",
                objective = "Explain greedy colouring",
                retrievalPrompt = "Explain graph colouring",
                examWeight = 0.15f,
                orderIndex = 0,
                resourcePointers = listOf("Lecture 6 slides", "Textbook ch. 9"),
                titleEditedLocally = false,
                objectiveEditedLocally = false,
                weightEditedLocally = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        db.sessionDao().upsert(
            SessionEntity(
                id = "s1",
                topicId = "t1",
                startedAt = now,
                endedAt = now + 480_000,
                plannedMinutes = 8,
                actualSeconds = 480,
                confidenceBefore = Confidence.SHAKY,
                confidenceAfter = Confidence.OK,
                wasDeferred = false,
                updatedAt = now,
            ),
        )

        val topic = db.topicDao().getById("t1")
        assertEquals(listOf("Lecture 6 slides", "Textbook ch. 9"), topic?.resourcePointers)

        val session = db.sessionDao().getLatestForTopic("t1")
        assertEquals(Confidence.SHAKY, session?.confidenceBefore)
        assertEquals(Confidence.OK, session?.confidenceAfter)
    }

    @Test
    fun deletingUnit_cascadesToTopicsAndSessions() = runBlocking {
        val now = System.currentTimeMillis()
        val unit = UnitEntity("u1", "ICS 3102", "Software Eng", null, null, now, now)
        db.unitDao().upsert(unit)
        db.topicDao().upsert(
            TopicEntity("t1", "u1", "Topic", "Obj", "Prompt", 0.1f, 0, emptyList(), false, false, false, now, now),
        )
        db.sessionDao().upsert(
            SessionEntity("s1", "t1", now, null, 8, 0, null, null, false, now),
        )

        db.unitDao().delete(unit)

        assertTrue(db.topicDao().getByUnitOnce("u1").isEmpty())
        assertNull(db.sessionDao().getLatestForTopic("t1"))
    }

    @Test
    fun deadline_linksToMultipleTopicsViaCrossRef() = runBlocking {
        val now = System.currentTimeMillis()
        db.unitDao().upsert(UnitEntity("u1", "ICS 3102", "Software Eng", null, null, now, now))
        db.topicDao().upsertAll(
            listOf(
                TopicEntity("t1", "u1", "Topic 1", "", "", 0.1f, 0, emptyList(), false, false, false, now, now),
                TopicEntity("t2", "u1", "Topic 2", "", "", 0.1f, 1, emptyList(), false, false, false, now, now),
            ),
        )
        db.deadlineDao().upsert(DeadlineEntity("d1", "u1", "CAT 1", now, DeadlineKind.CAT, now))
        db.deadlineDao().insertCrossRef(DeadlineTopicCrossRef("d1", "t1"))
        db.deadlineDao().insertCrossRef(DeadlineTopicCrossRef("d1", "t2"))

        val topicIds = db.deadlineDao().getTopicIdsForDeadline("d1")
        assertEquals(setOf("t1", "t2"), topicIds.toSet())

        val nearest = db.deadlineDao().getNearestDeadlineForTopic("t1")
        assertEquals("d1", nearest?.id)
    }

    @Test
    fun unitFlow_emitsAfterUpsert() = runBlocking {
        val now = System.currentTimeMillis()
        db.unitDao().upsert(UnitEntity("u1", "ICS 3102", "Software Eng", null, null, now, now))
        val units = db.unitDao().observeAll().first()
        assertEquals(1, units.size)
        assertEquals("ICS 3102", units.first().code)
    }
}
