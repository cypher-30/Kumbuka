package dev.kumbuka.app.data.bootstrap

import dev.kumbuka.app.data.repository.AssessmentMarkRepository
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * Developer tool: fills the app with roughly three months of believable study
 * history (units, topics, reviews with improving confidence, deferrals, dates
 * and marks) so every screen and graph can be reviewed with real-looking data.
 *
 * Only reachable from the hidden research screen of a debuggable build. Ids are
 * stable ("demo-..."), so running it twice updates rows in place instead of
 * duplicating them, and it never deletes anything.
 */
object DemoHistorySeeder {
    private data class UnitSpec(val code: String, val title: String, val topics: List<TopicSpec>)
    private data class TopicSpec(val title: String, val objective: String, val prompt: String, val weight: Float)

    private val specs = listOf(
        UnitSpec(
            "BIT 2101", "Database Systems",
            listOf(
                TopicSpec("Relational model and keys", "Define relations, keys and integrity constraints.", "What makes a candidate key different from a primary key?", 0.15f),
                TopicSpec("ER modelling", "Turn a requirements paragraph into an ER diagram.", "How do you resolve a many-to-many relationship?", 0.20f),
                TopicSpec("SQL joins and subqueries", "Write multi-table queries with joins and subqueries.", "When does a LEFT JOIN return NULLs?", 0.25f),
                TopicSpec("Normalisation to 3NF", "Decompose a table to third normal form.", "What anomaly does 2NF remove?", 0.20f),
                TopicSpec("Transactions and ACID", "Explain ACID and isolation levels.", "What is a dirty read?", 0.12f),
                TopicSpec("Indexing and query plans", "Choose indexes and read an execution plan.", "Why can an index slow down writes?", 0.08f),
            ),
        ),
        UnitSpec(
            "ICS 2201", "Computer Networks",
            listOf(
                TopicSpec("OSI and TCP/IP layers", "Map protocols to layers.", "Which layer handles routing?", 0.15f),
                TopicSpec("IP addressing and subnetting", "Subnet a network from a requirement.", "How many hosts fit in a /27?", 0.25f),
                TopicSpec("TCP vs UDP", "Compare reliability, ordering and overhead.", "Why does DNS usually use UDP?", 0.15f),
                TopicSpec("Routing basics", "Explain distance-vector vs link-state routing.", "What problem does split horizon solve?", 0.20f),
                TopicSpec("HTTP and DNS", "Trace a page request end to end.", "What happens between typing a URL and the first byte?", 0.15f),
                TopicSpec("Network security basics", "Describe firewalls, TLS and common attacks.", "What does the TLS handshake establish?", 0.10f),
            ),
        ),
        UnitSpec(
            "MATH 201", "Linear Algebra",
            listOf(
                TopicSpec("Systems of linear equations", "Solve systems by Gaussian elimination.", "When does a system have no solution?", 0.18f),
                TopicSpec("Matrix operations", "Multiply, transpose and invert matrices.", "When is a matrix invertible?", 0.17f),
                TopicSpec("Determinants", "Compute determinants and interpret them.", "What does a zero determinant imply?", 0.15f),
                TopicSpec("Vector spaces and bases", "Decide independence, span and basis.", "How do you test linear independence?", 0.20f),
                TopicSpec("Eigenvalues and eigenvectors", "Find eigenpairs and diagonalise.", "What is the characteristic polynomial for?", 0.20f),
                TopicSpec("Orthogonality", "Use projections and Gram-Schmidt.", "What does the projection of b onto a represent?", 0.10f),
            ),
        ),
        UnitSpec(
            "STA 210", "Probability and Statistics",
            listOf(
                TopicSpec("Probability axioms", "Apply the axioms and counting rules.", "How do you get P(A or B)?", 0.12f),
                TopicSpec("Conditional probability and Bayes", "Use Bayes theorem in a worked problem.", "Why is P(A|B) not P(B|A)?", 0.22f),
                TopicSpec("Random variables", "Work with discrete and continuous variables.", "What is the difference between PMF and PDF?", 0.16f),
                TopicSpec("Common distributions", "Pick and apply binomial, Poisson and normal.", "When is Poisson a good model?", 0.20f),
                TopicSpec("Sampling and estimation", "Build and interpret confidence intervals.", "What does a 95% interval actually mean?", 0.18f),
                TopicSpec("Hypothesis testing", "Run a t-test and interpret the p-value.", "What does a p-value not tell you?", 0.12f),
            ),
        ),
        UnitSpec(
            "COM 101", "Communication Skills",
            listOf(
                TopicSpec("Academic writing structure", "Plan an essay with thesis and evidence.", "What does a good topic sentence do?", 0.20f),
                TopicSpec("Referencing and citation", "Cite sources in the required style.", "When must you cite a paraphrase?", 0.20f),
                TopicSpec("Report writing", "Structure a technical report.", "What goes in an executive summary?", 0.20f),
                TopicSpec("Presentation skills", "Design and deliver a short talk.", "How do you open a talk?", 0.20f),
                TopicSpec("Listening and note-taking", "Take structured notes from a lecture.", "What is the Cornell method?", 0.20f),
            ),
        ),
    )

    /** Days relative to today: (unit index, title, kind, offset). Negative = already past. */
    private val dates = listOf(
        Triple(0, "CAT 1: Modelling and SQL", DeadlineKind.CAT) to -58,
        Triple(0, "Assignment: Design a clinic database", DeadlineKind.ASSIGNMENT) to -30,
        Triple(0, "CAT 2: Normalisation and transactions", DeadlineKind.CAT) to 4,
        Triple(0, "End Semester Exam", DeadlineKind.EXAM) to 38,
        Triple(1, "CAT 1: Layers and addressing", DeadlineKind.CAT) to -49,
        Triple(1, "Lab: Subnetting practical", DeadlineKind.ASSIGNMENT) to -21,
        Triple(1, "CAT 2: Routing and transport", DeadlineKind.CAT) to 9,
        Triple(1, "End Semester Exam", DeadlineKind.EXAM) to 41,
        Triple(2, "CAT 1: Systems and matrices", DeadlineKind.CAT) to -44,
        Triple(2, "Assignment 2: Eigen problems", DeadlineKind.ASSIGNMENT) to 2,
        Triple(2, "End Semester Exam", DeadlineKind.EXAM) to 44,
        Triple(3, "CAT 1: Probability and Bayes", DeadlineKind.CAT) to -37,
        Triple(3, "CAT 2: Distributions", DeadlineKind.CAT) to 13,
        Triple(3, "End Semester Exam", DeadlineKind.EXAM) to 46,
        Triple(4, "Report submission", DeadlineKind.ASSIGNMENT) to 6,
        Triple(4, "Group presentation", DeadlineKind.ASSIGNMENT) to 18,
    )

    suspend fun seed(
        units: UnitRepository,
        topics: TopicRepository,
        sessions: SessionRepository,
        deadlines: DeadlineRepository,
        marks: AssessmentMarkRepository,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        // Re-running replaces the previous demo reviews instead of stacking on top of them.
        sessions.deleteDemoSessions("demo-s-")
        val random = Random(2026)
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val utcDay = { offset: Int -> today.plusDays(offset.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }

        val unitIds = mutableListOf<String>()
        val topicIdsByUnit = mutableListOf<List<String>>()
        specs.forEachIndexed { unitIndex, spec ->
            val unitId = "demo-unit-$unitIndex"
            unitIds += unitId
            units.upsert(Unit(unitId, spec.code, spec.title, null, null, now - 95 * dayMs, now))
            val ids = spec.topics.mapIndexed { index, t ->
                val id = "demo-topic-$unitIndex-$index"
                topics.upsert(
                    Topic(
                        id = id, unitId = unitId, title = t.title, objective = t.objective, retrievalPrompt = t.prompt,
                        examWeight = t.weight, orderIndex = index + 1,
                        resourcePointers = listOf("Lecture ${index + 1} slides", "Tutorial sheet ${index + 1}"),
                        titleEditedLocally = false, objectiveEditedLocally = false, weightEditedLocally = false,
                        createdAt = now - 95 * dayMs, updatedAt = now,
                    ),
                )
                id
            }
            topicIdsByUnit += ids
        }

        // Reviews: each topic is revisited on an expanding schedule over ~90 days,
        // confidence trending up with noise, a few deferrals, evening-heavy times.
        var counter = 0
        specs.forEachIndexed { unitIndex, spec ->
            spec.topics.forEachIndexed { topicIndex, _ ->
                // The last topics of the newest unit are left unreviewed so coverage looks realistic.
                if ((unitIndex == 4 && topicIndex >= 3) || (unitIndex == 3 && topicIndex == 5) || (unitIndex == 2 && topicIndex == 5)) return@forEachIndexed
                // Some topics have gone stale (last reviewed weeks ago) so the queue has real work.
                val stopDay = if ((topicIndex + unitIndex) % 3 == 0) 14 + random.nextInt(0, 26) else 0
                val topicId = topicIdsByUnit[unitIndex][topicIndex]
                // Stagger: later topics and later units were started later, so coverage is uneven.
                var day = 88 - unitIndex * 6 - topicIndex * 4
                var gap = 2 + random.nextInt(3)
                var level = random.nextInt(0, 2)
                while (day > stopDay) {
                    val hour = listOf(7, 13, 17, 19, 20, 21, 22).random(random)
                    val started = startOfToday - day * dayMs + hour * 3_600_000L + random.nextInt(0, 3_000_000)
                    val deferred = random.nextInt(100) < 9
                    val planned = listOf(12, 15, 16, 20).random(random)
                    if (deferred) {
                        sessions.upsert(Session("demo-s-${counter++}", topicId, started, started + 20_000, planned, 20, null, null, true, started))
                    } else {
                        val before = Confidence.entries[level.coerceIn(0, 3)]
                        level = (level + if (random.nextInt(100) < 40) 1 else 0).coerceAtMost(3)
                        if (random.nextInt(100) < 20) level = (level - 1).coerceAtLeast(0)
                        val after = Confidence.entries[(level + if (random.nextInt(100) < 25) 1 else 0).coerceIn(0, 3)]
                        val seconds = (planned * 60 * (0.7 + random.nextDouble() * 0.5)).toInt()
                        sessions.upsert(
                            Session("demo-s-${counter++}", topicId, started, started + seconds * 1000L, planned, seconds, before, after, false, started + seconds * 1000L),
                        )
                    }
                    day -= gap
                    gap = (gap + 1 + random.nextInt(3)).coerceAtMost(14)
                }
            }
        }

        dates.forEachIndexed { index, (head, offset) ->
            val (unitIndex, title, kind) = head
            val linked = topicIdsByUnit[unitIndex].take(if (kind == DeadlineKind.EXAM) 6 else 3 + index % 2)
            deadlines.upsert(Deadline("demo-d-$index", unitIds[unitIndex], title, utcDay(offset), kind, linked, now))
        }

        val markRows = listOf(
            Triple(0, AssessmentKind.CAT, 21.0f to 30.0f) to -58,
            Triple(0, AssessmentKind.ASSIGNMENT, 13.5f to 20.0f) to -30,
            Triple(1, AssessmentKind.CAT, 18.0f to 30.0f) to -49,
            Triple(1, AssessmentKind.ASSIGNMENT, 16.0f to 20.0f) to -21,
            Triple(2, AssessmentKind.CAT, 14.0f to 30.0f) to -44,
            Triple(2, AssessmentKind.PAST_PAPER, 52.0f to 100.0f) to -25,
            Triple(3, AssessmentKind.CAT, 24.5f to 30.0f) to -37,
            Triple(3, AssessmentKind.PAST_PAPER, 68.0f to 100.0f) to -12,
            Triple(4, AssessmentKind.ASSIGNMENT, 17.0f to 20.0f) to -40,
        )
        markRows.forEachIndexed { index, (head, offset) ->
            val (unitIndex, kind, scores) = head
            marks.upsert(
                AssessmentMark(
                    "demo-m-$index", unitIds[unitIndex], scores.first, scores.second, kind, utcDay(offset),
                    topicIdsByUnit[unitIndex].take(3), now,
                ),
            )
        }
    }
}
