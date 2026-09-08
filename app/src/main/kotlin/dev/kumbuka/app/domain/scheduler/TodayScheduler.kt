package dev.kumbuka.app.domain.scheduler

import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val DAY_MILLIS = 86_400_000L
const val DEFAULT_SESSION_LENGTH_MINUTES = 60
const val DUE_SCORE_THRESHOLD = 0.35f
private const val STALENESS_WINDOW_DAYS = 14f
private const val URGENCY_WINDOW_DAYS = 21f
private const val AVOIDANCE_WINDOW = 5f

interface RecallPredictor {
    suspend fun predictRecall(topic: Topic, history: List<Session>): RecallPrediction?
}

data class RecallPrediction(
    val probability: Float,
    val source: String,
)

object BaselineRecallPredictor : RecallPredictor {
    override suspend fun predictRecall(topic: Topic, history: List<Session>): RecallPrediction? = null
}

object LearnedRecallPredictor : RecallPredictor {
    override suspend fun predictRecall(topic: Topic, history: List<Session>): RecallPrediction? = null
}

sealed interface TodayState {
    data object NoUnits : TodayState
    data object FreshStart : TodayState
    data object Tonight : TodayState
    data object AllCaughtUp : TodayState
}

data class TodayPlan(
    val state: TodayState,
    val headline: String,
    val body: String,
    val sessionLengthMinutes: Int,
    val cards: List<TodayCard>,
)

data class TodayCard(
    val topicId: String,
    val unitCode: String,
    val title: String,
    val objective: String,
    val minutes: Int,
    val score: Float,
    val breakdown: ScoreBreakdown,
)

data class ScoreBreakdown(
    val gap: Float,
    val staleness: Float,
    val urgency: Float,
    val avoidance: Float,
    val score: Float,
    val latestConfidence: Confidence?,
    val daysSinceReview: Int,
    val daysUntilDeadline: Int?,
    val deferralCount: Int,
) {
    fun plainLanguageSummary(): String = buildString {
        append("Gap contributes ")
        append(percent(gap))
        append("; staleness contributes ")
        append(percent(staleness))
        append("; urgency contributes ")
        append(percent(urgency))
        append("; avoidance contributes ")
        append(percent(avoidance))
        append('.')
    }

    fun formulaSummary(): String = "score = 0.40×gap + 0.20×staleness + 0.30×urgency + 0.10×avoidance"

    private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"
}

fun buildTodayPlan(
    units: List<UnitModel>,
    topics: List<Topic>,
    sessions: List<Session>,
    deadlines: List<Deadline>,
    sessionLengthMinutes: Int = DEFAULT_SESSION_LENGTH_MINUTES,
    nowMillis: Long = System.currentTimeMillis(),
): TodayPlan {
    if (units.isEmpty() || topics.isEmpty()) {
        return TodayPlan(
            state = TodayState.NoUnits,
            headline = "Import a Course Pack to start",
            body = "Kumbuka needs imported units before it can rank tonight's revision list.",
            sessionLengthMinutes = sessionLengthMinutes,
            cards = emptyList(),
        )
    }

    val sessionsByTopic = sessions.groupBy { it.topicId }
    val deadlinesByTopic = deadlines.flatMap { deadline -> deadline.topicIds.map { topicId -> topicId to deadline } }
        .groupBy({ it.first }, { it.second })

    val scoredTopics = topics.map { topic ->
        val history = sessionsByTopic[topic.id].orEmpty()
        val topicDeadlines = deadlinesByTopic[topic.id].orEmpty()
        scoreTopic(topic, history, topicDeadlines, nowMillis)
    }.sortedWith(
        compareByDescending<ScoredTopic> { it.breakdown.score }
            .thenByDescending { it.topic.examWeight }
            .thenBy { it.topic.orderIndex },
    )

    val hasAnyHistory = sessions.isNotEmpty()
    val hasAnyDeadlines = deadlines.isNotEmpty()
    val isFreshStart = !hasAnyHistory && !hasAnyDeadlines
    val dueTopics = scoredTopics.filter { it.breakdown.score >= DUE_SCORE_THRESHOLD }
    val unitCodesById = units.associateBy({ it.id }, { it.code })

    if (isFreshStart) {
        val cards = allocateMinutes(scoredTopics.take(maxTopicsForSession(sessionLengthMinutes)), sessionLengthMinutes, unitCodesById)
        return TodayPlan(
            state = TodayState.FreshStart,
            headline = "Fresh start",
            body = "You have units imported, but no study history yet. Start with the highest-weight topics and let the schedule learn from your first review.",
            sessionLengthMinutes = sessionLengthMinutes,
            cards = cards,
        )
    }

    if (dueTopics.isEmpty()) {
        return TodayPlan(
            state = TodayState.AllCaughtUp,
            headline = "All caught up",
            body = "Nothing is urgent right now. Your recent reviews have pushed every topic below the baseline threshold.",
            sessionLengthMinutes = sessionLengthMinutes,
            cards = emptyList(),
        )
    }

    val cards = allocateMinutes(dueTopics.take(maxTopicsForSession(sessionLengthMinutes)), sessionLengthMinutes, unitCodesById)
    return TodayPlan(
        state = TodayState.Tonight,
        headline = "Tonight",
        body = "These topics scored highest on the baseline formula. Bigger exam-weight topics also get more of the session minutes.",
        sessionLengthMinutes = sessionLengthMinutes,
        cards = cards,
    )
}

data class ScoredTopic(
    val topic: Topic,
    val breakdown: ScoreBreakdown,
)

fun scoreTopic(
    topic: Topic,
    history: List<Session>,
    deadlines: List<Deadline>,
    nowMillis: Long = System.currentTimeMillis(),
): ScoredTopic {
    val latest = history.maxByOrNull { it.startedAt }
    val latestConfidence = latest?.confidenceBefore
    val daysSinceReview = daysBetween(latest?.endedAt ?: latest?.startedAt ?: topic.createdAt, nowMillis)
    val nearestDeadline = deadlines.minByOrNull { it.date }
    val daysUntilDeadline = nearestDeadline?.let { daysBetween(nowMillis, it.date).coerceAtLeast(0) }

    val gap = latestConfidence?.asGap() ?: 1f
    val staleness = normalize(daysSinceReview.toFloat(), STALENESS_WINDOW_DAYS)
    val urgency = nearestDeadline?.let { max(0f, 1f - (daysUntilDeadline!!.toFloat() / URGENCY_WINDOW_DAYS)) } ?: 0f
    val avoidance = normalize(history.count { it.wasDeferred }.toFloat(), AVOIDANCE_WINDOW)
    val score = 0.40f * gap + 0.20f * staleness + 0.30f * urgency + 0.10f * avoidance

    return ScoredTopic(
        topic = topic,
        breakdown = ScoreBreakdown(
            gap = gap,
            staleness = staleness,
            urgency = urgency,
            avoidance = avoidance,
            score = score,
            latestConfidence = latestConfidence,
            daysSinceReview = daysSinceReview.toInt(),
            daysUntilDeadline = daysUntilDeadline?.toInt(),
            deferralCount = history.count { it.wasDeferred },
        ),
    )
}

fun allocateMinutes(
    scoredTopics: List<ScoredTopic>,
    sessionLengthMinutes: Int,
    unitCodesById: Map<String, String>,
): List<TodayCard> {
    if (scoredTopics.isEmpty()) return emptyList()

    val weights = scoredTopics.map { max(0.05f, it.topic.examWeight) }
    val totalWeight = weights.sum().takeIf { it > 0f } ?: 1f
    val exactShares = scoredTopics.mapIndexed { index, item ->
        sessionLengthMinutes * (weights[index] / totalWeight)
    }
    val base = exactShares.map { max(1, it.toInt()) }.toMutableList()
    var remainder = sessionLengthMinutes - base.sum()

    if (remainder > 0) {
        val order = exactShares.mapIndexed { index, share -> index to (share - base[index]) }
            .sortedByDescending { it.second }
            .map { it.first }
        var cursor = 0
        while (remainder > 0) {
            base[order[cursor % order.size]] += 1
            remainder -= 1
            cursor += 1
        }
    } else if (remainder < 0) {
        val order = exactShares.mapIndexed { index, share -> index to (share - base[index]) }
            .sortedBy { it.second }
            .map { it.first }
        var cursor = 0
        while (remainder < 0 && order.isNotEmpty()) {
            val idx = order[cursor % order.size]
            if (base[idx] > 1) {
                base[idx] -= 1
                remainder += 1
            }
            cursor += 1
            if (cursor > 2000) break
        }
    }

    return scoredTopics.mapIndexed { index, item ->
        val unitCode = unitCodesById[item.topic.unitId] ?: item.topic.unitId
        TodayCard(
            topicId = item.topic.id,
            unitCode = unitCode,
            title = item.topic.title,
            objective = item.topic.objective,
            minutes = base[index],
            score = item.breakdown.score,
            breakdown = item.breakdown,
        )
    }
}

private fun maxTopicsForSession(sessionLengthMinutes: Int): Int =
    when {
        sessionLengthMinutes <= 30 -> 2
        sessionLengthMinutes <= 45 -> 3
        sessionLengthMinutes <= 60 -> 4
        else -> 5
    }

private fun daysBetween(startMillis: Long, endMillis: Long): Long =
    abs(endMillis - startMillis) / DAY_MILLIS

private fun normalize(value: Float, window: Float): Float =
    if (window <= 0f) 0f else (value / window).coerceIn(0f, 1f)


