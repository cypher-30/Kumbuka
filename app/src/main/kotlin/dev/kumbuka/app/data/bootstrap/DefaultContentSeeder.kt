package dev.kumbuka.app.data.bootstrap

import dev.kumbuka.app.data.repository.PackRepository
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackDeadline
import dev.kumbuka.app.pack.CoursePackTopic
import dev.kumbuka.app.pack.CoursePackUnit

object DefaultContentSeeder {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    /**
     * Explicit opt-in only - never called automatically on launch. A first
     * run must show real empty states, not fabricated sample data
     * (deliverable: "no fake sample data").
     */
    suspend fun seedSamplePacks(
        packRepository: PackRepository,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        buildSamplePacks(nowMillis).forEach { pack ->
            packRepository.importPack(pack, isSample = true)
        }
    }

    private fun buildSamplePacks(nowMillis: Long): List<CoursePack> = listOf(
        CoursePack(
            packId = "kumbuka.sample.cs210",
            packVersion = 1,
            unit = CoursePackUnit(code = "CS 210", title = "Data Structures"),
            topics = listOf(
                CoursePackTopic(
                    id = "kumbuka.sample.cs210.t.arrays",
                    title = "Arrays and Lists",
                    objective = "Choose the right sequence structure and analyze time complexity.",
                    retrievalPrompt = "When should you use a linked list instead of a dynamic array?",
                    examWeight = 0.35f,
                    orderIndex = 1,
                    resourcePointers = listOf("CLRS Ch. 10", "Lecture 3 slides"),
                ),
                CoursePackTopic(
                    id = "kumbuka.sample.cs210.t.stacks",
                    title = "Stacks and Queues",
                    objective = "Implement stack and queue operations and apply them to real problems.",
                    retrievalPrompt = "What operations are O(1) for both stack and queue?",
                    examWeight = 0.3f,
                    orderIndex = 2,
                    resourcePointers = listOf("Lab 2", "Practice set A"),
                ),
                CoursePackTopic(
                    id = "kumbuka.sample.cs210.t.trees",
                    title = "Trees and Traversals",
                    objective = "Explain traversal strategies and choose balanced tree structures.",
                    retrievalPrompt = "How do preorder and postorder traversals differ in use cases?",
                    examWeight = 0.35f,
                    orderIndex = 3,
                    resourcePointers = listOf("CLRS Ch. 12", "Traversal drills"),
                ),
            ),
            deadlines = listOf(
                CoursePackDeadline(
                    id = "kumbuka.sample.cs210.d.assignment1",
                    title = "Assignment 1: Implement Queue Variants",
                    date = nowMillis + 10 * DAY_MILLIS,
                    kind = DeadlineKind.ASSIGNMENT,
                    topicIds = listOf(
                        "kumbuka.sample.cs210.t.arrays",
                        "kumbuka.sample.cs210.t.stacks",
                    ),
                ),
                CoursePackDeadline(
                    id = "kumbuka.sample.cs210.d.cat1",
                    title = "CAT 1",
                    date = nowMillis + 20 * DAY_MILLIS,
                    kind = DeadlineKind.CAT,
                    topicIds = listOf(
                        "kumbuka.sample.cs210.t.arrays",
                        "kumbuka.sample.cs210.t.stacks",
                        "kumbuka.sample.cs210.t.trees",
                    ),
                ),
            ),
        ),
        CoursePack(
            packId = "kumbuka.sample.math115",
            packVersion = 1,
            unit = CoursePackUnit(code = "MATH 115", title = "Discrete Mathematics"),
            topics = listOf(
                CoursePackTopic(
                    id = "kumbuka.sample.math115.t.logic",
                    title = "Propositional Logic",
                    objective = "Translate statements into logical form and prove equivalence.",
                    retrievalPrompt = "What is the contrapositive of p -> q?",
                    examWeight = 0.34f,
                    orderIndex = 1,
                    resourcePointers = listOf("Textbook Sec. 1.1", "Tutorial sheet 1"),
                ),
                CoursePackTopic(
                    id = "kumbuka.sample.math115.t.proofs",
                    title = "Proof Techniques",
                    objective = "Use direct proof, contradiction, and induction correctly.",
                    retrievalPrompt = "When is proof by contradiction more natural than direct proof?",
                    examWeight = 0.33f,
                    orderIndex = 2,
                    resourcePointers = listOf("Lecture 5 notes", "Induction drills"),
                ),
                CoursePackTopic(
                    id = "kumbuka.sample.math115.t.graphs",
                    title = "Graph Basics",
                    objective = "Model relationships as graphs and compute key properties.",
                    retrievalPrompt = "Define degree sequence and explain what it reveals.",
                    examWeight = 0.33f,
                    orderIndex = 3,
                    resourcePointers = listOf("Textbook Sec. 8.1", "Graph worksheet"),
                ),
            ),
            deadlines = listOf(
                CoursePackDeadline(
                    id = "kumbuka.sample.math115.d.quiz1",
                    title = "Logic Quiz",
                    date = nowMillis + 7 * DAY_MILLIS,
                    kind = DeadlineKind.CAT,
                    topicIds = listOf("kumbuka.sample.math115.t.logic"),
                ),
                CoursePackDeadline(
                    id = "kumbuka.sample.math115.d.final",
                    title = "End Semester Exam",
                    date = nowMillis + 45 * DAY_MILLIS,
                    kind = DeadlineKind.EXAM,
                    topicIds = listOf(
                        "kumbuka.sample.math115.t.logic",
                        "kumbuka.sample.math115.t.proofs",
                        "kumbuka.sample.math115.t.graphs",
                    ),
                ),
            ),
        ),
    )
}

