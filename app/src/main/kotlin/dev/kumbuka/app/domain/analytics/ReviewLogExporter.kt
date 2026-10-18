package dev.kumbuka.app.domain.analytics

import dev.kumbuka.app.domain.model.AssessmentMark
import dev.kumbuka.app.domain.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Builds the research review log as CSV. It contains only what the study needs:
 * review sessions (timing, planned and actual length, confidence before/after,
 * deferrals, scheduler arm) and assessment marks (kind, score, out-of, date).
 *
 * It deliberately never includes unit codes or titles, topic titles, objectives,
 * notes, or anything that identifies the student - rows carry opaque ids only.
 * Sample-pack topics are excluded by the caller via [excludedTopicIds] and
 * [excludedUnitIds], matching how Progress ignores them.
 */
object ReviewLogExporter {
    const val HEADER = "record_type,id,unit_id,topic_id,kind,started_at_utc,ended_at_utc," +
        "planned_minutes,actual_seconds,confidence_before,confidence_after,deferred,scheduler_arm," +
        "score,out_of,date"

    fun build(
        sessions: List<Session>,
        marks: List<AssessmentMark>,
        unitIdByTopic: Map<String, String>,
        excludedTopicIds: Set<String> = emptySet(),
        excludedUnitIds: Set<String> = emptySet(),
    ): String {
        val rows = ArrayList<String>()
        rows += HEADER
        sessions
            .filter { it.topicId !in excludedTopicIds }
            .sortedBy { it.startedAt }
            .forEach { s ->
                rows += csvRow(
                    "session", s.id, unitIdByTopic[s.topicId].orEmpty(), s.topicId, "",
                    iso(s.startedAt), s.endedAt?.let(::iso).orEmpty(),
                    s.plannedMinutes.toString(), s.actualSeconds.toString(),
                    s.confidenceBefore?.score?.toString().orEmpty(), s.confidenceAfter?.score?.toString().orEmpty(),
                    if (s.wasDeferred) "1" else "0", s.sourceArm.orEmpty(), "", "", "",
                )
            }
        marks
            .filter { it.unitId !in excludedUnitIds }
            .sortedBy { it.date }
            .forEach { m ->
                rows += csvRow(
                    "mark", m.id, m.unitId, m.topicIds.joinToString(";"), m.kind.name,
                    "", "", "", "", "", "", "", "",
                    trimNumber(m.score), trimNumber(m.outOf), LocalDate.ofInstant(Instant.ofEpochMilli(m.date), ZoneOffset.UTC).toString(),
                )
            }
        return rows.joinToString("\r\n", postfix = "\r\n")
    }

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    private fun trimNumber(value: Float): String =
        if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

    private fun csvRow(vararg fields: String): String = fields.joinToString(",") { escape(it) }

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
