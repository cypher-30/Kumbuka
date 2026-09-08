package dev.kumbuka.app.pack

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CoursePack(
    val formatVersion: Int = 1,
    val packId: String,
    val packVersion: Int,
    val unit: CoursePackUnit,
    val topics: List<CoursePackTopic>,
    val deadlines: List<CoursePackDeadline> = emptyList(),
) {
    companion object {
        fun fromDomain(unit: UnitModel, topics: List<Topic>, deadlines: List<Deadline>): CoursePack {
            val sortedTopics = topics.sortedBy { it.orderIndex }
            val sortedDeadlines = deadlines.sortedBy { it.date }
            return CoursePack(
                packId = unit.packId ?: unit.id,
                packVersion = unit.packVersion ?: 1,
                unit = CoursePackUnit(code = unit.code, title = unit.title),
                topics = sortedTopics.map { topic ->
                    CoursePackTopic(
                        id = topic.id,
                        title = topic.title,
                        objective = topic.objective,
                        retrievalPrompt = topic.retrievalPrompt,
                        examWeight = topic.examWeight,
                        orderIndex = topic.orderIndex,
                        resourcePointers = topic.resourcePointers,
                    )
                },
                deadlines = sortedDeadlines.map { deadline ->
                    CoursePackDeadline(
                        id = deadline.id,
                        title = deadline.title,
                        date = deadline.date,
                        kind = deadline.kind,
                        topicIds = deadline.topicIds,
                    )
                },
            )
        }
    }
}

@Serializable
data class CoursePackUnit(
    val code: String,
    val title: String,
)

@Serializable
data class CoursePackTopic(
    val id: String,
    val title: String,
    val objective: String,
    val retrievalPrompt: String,
    val examWeight: Float,
    val orderIndex: Int,
    val resourcePointers: List<String> = emptyList(),
)

@Serializable
data class CoursePackDeadline(
    val id: String,
    val title: String,
    val date: Long,
    val kind: DeadlineKind,
    val topicIds: List<String> = emptyList(),
)

object CoursePackCodec {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(pack: CoursePack): String = json.encodeToString(pack)

    fun decode(raw: String): CoursePack = json.decodeFromString(raw.trim().trimStart('\uFEFF'))
}


