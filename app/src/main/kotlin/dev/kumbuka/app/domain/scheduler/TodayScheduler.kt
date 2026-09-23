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

/** How many reviews must be completed, rated, and non-deferred before the placeholder predictor will run at all (DESIGN.md §8: "cold start"). */
const val MIN_RATED_REVIEWS_FOR_PLACEHOLDER = 3

enum class SchedulerArmKind { BASELINE, PLACEHOLDER }

data class RecallPrediction(
    val probability: Float,
    val version: String,
)

interface RecallPredictor {
    /** Returns null when this predictor has no eligible history to predict from. */
    fun predictRecall(topic: Topic, history: List<Session>, nowMillis: Long): RecallPrediction?
}

/** The hand-written formula itself - never predicts a recall probability. */
object BaselineRecallPredictor : RecallPredictor {
    override fun predictRecall(topic: Topic, history: List<Session>, nowMillis: Long): RecallPrediction? = null
}

/**
 * A deterministic, hand-set stand-in for the trained model that will
 * eventually load from the separate SemProject_MLEngine repo. This is NOT a
 * trained model - it is a fixed exponential-decay curve seeded only from the
 * student's own last rated after-confidence, so the "model" arm has a real,
 * versioned implementation to log and compare against baseline now, instead
 * of shipping a null slot. Every prediction is tagged with [VERSION] so
 * research output never confuses this for a trained result.
 */
object PlaceholderRecallPredictor : RecallPredictor {
    const val VERSION = "placeholder-v1"
    private const val BASE_HALF_LIFE_DAYS = 4f

    override fun predictRecall(topic: Topic, history: List<Session>, nowMillis: Long): RecallPrediction? {
        val rated = ratedCompletedReviews(history)
        if (rated.size < MIN_RATED_REVIEWS_FOR_PLACEHOLDER) return null
        val latest = rated.maxByOrNull { it.endedAt ?: it.startedAt } ?: return null
        val lastConfidence = latest.confidenceAfter ?: return null
        val referenceMillis = latest.endedAt ?: latest.startedAt
        val elapsedDays = daysBetween(referenceMillis, nowMillis).toFloat()
        // A higher last "after" rating means the student landed on it more
        // solidly, so the hand-set half-life stretches out (forgets slower).
        val halfLifeDays = BASE_HALF_LIFE_DAYS * (1f + lastConfidence.score / 3f)
        val probability = Math.pow(0.5, (elapsedDays / halfLifeDays).toDouble()).toFloat().coerceIn(0f, 1f)
        return RecallPrediction(probability = probability, version = VERSION)
    }
}

/** A completed, non-deferred attempt with both ratings recorded - the only history the placeholder may learn from. */
fun ratedCompletedReviews(history: List<Session>): List<Session> =
    history.filter { it.endedAt != null && !it.wasDeferred && it.confidenceBefore != null && it.confidenceAfter != null }

sealed interface TodayState {
    data object NoUnits : TodayState
    data object NoActiveTopics : TodayState
    data object FreshStart : TodayState
    data object Today : TodayState
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

sealed interface TodayReasonFact {
    data class DueInDays(val days: Int) : TodayReasonFact
    data class LastRated(val confidence: Confidence) : TodayReasonFact
    data class StaleForDays(val days: Int) : TodayReasonFact
    data class DeferredCount(val count: Int) : TodayReasonFact
    data object NoHistory : TodayReasonFact
}

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
    val arm: SchedulerArmKind = SchedulerArmKind.BASELINE,
    val predictedRecall: Float? = null,
    val predictorVersion: String? = null,
    /** "baseline", "placeholder", or "baseline_fallback" when the placeholder arm was requested but had no eligible history. */
    val actualSource: String = "baseline",
    val fallbackReason: String? = null,
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

    fun reasonFacts(): List<TodayReasonFact> = buildList {
        daysUntilDeadline?.let { add(TodayReasonFact.DueInDays(it)) }
        latestConfidence?.let { add(TodayReasonFact.LastRated(it)) }
        if (daysSinceReview > 0) add(TodayReasonFact.StaleForDays(daysSinceReview))
        if (deferralCount > 0) add(TodayReasonFact.DeferredCount(deferralCount))
        if (isEmpty()) add(TodayReasonFact.NoHistory)
    }.take(2)

    private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"
}

fun buildTodayPlan(
    units: List<UnitModel>,
    topics: List<Topic>,
    sessions: List<Session>,
    deadlines: List<Deadline>,
    sessionLengthMinutes: Int = DEFAULT_SESSION_LENGTH_MINUTES,
    nowMillis: Long = System.currentTimeMillis(),
    arm: SchedulerArmKind = SchedulerArmKind.BASELINE,
    predictor: RecallPredictor = BaselineRecallPredictor,
): TodayPlan {
    if (units.isEmpty()) {
        return TodayPlan(
            state = TodayState.NoUnits,
            headline = "Import a Course Pack to start",
            body = "Kumbuka needs imported units before it can rank today's revision list.",
            sessionLengthMinutes = sessionLengthMinutes,
            cards = emptyList(),
        )
    }

    if (topics.isEmpty()) {
        return TodayPlan(
            state = TodayState.NoActiveTopics,
            headline = "No active topics",
            body = "Your units are here, but none of their topics are active for revision right now.",
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
        scoreTopicForArm(topic, history, topicDeadlines, arm, predictor, nowMillis)
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
        state = TodayState.Today,
        headline = "Today",
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
    val daysUntilDeadline = nearestDeadline?.let { daysUntilDeadline(it.date, nowMillis).toLong() }

    val gap = latestConfidence?.asGap() ?: 1f
    val staleness = normalize(daysSinceReview.toFloat(), STALENESS_WINDOW_DAYS)
    val urgency = nearestDeadline?.let { schedulerUrgency(it.date, nowMillis) } ?: 0f
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

/**
 * Scores one topic under the requested arm. BASELINE always runs the
 * published formula unchanged. PLACEHOLDER runs the provisional model
 * formula `0.60*(1-predictedRecall) + 0.30*urgency + 0.10*avoidance` when the
 * predictor has enough eligible history; otherwise it falls back to the
 * exact baseline result for that topic, tagged honestly as
 * "baseline_fallback" with a reason - never a silently-substituted number.
 */
fun scoreTopicForArm(
    topic: Topic,
    history: List<Session>,
    deadlines: List<Deadline>,
    arm: SchedulerArmKind,
    predictor: RecallPredictor,
    nowMillis: Long = System.currentTimeMillis(),
): ScoredTopic {
    val baseline = scoreTopic(topic, history, deadlines, nowMillis)
    if (arm == SchedulerArmKind.BASELINE) return baseline

    val prediction = predictor.predictRecall(topic, history, nowMillis)
    if (prediction == null) {
        val reason = if (ratedCompletedReviews(history).size < MIN_RATED_REVIEWS_FOR_PLACEHOLDER) {
            "cold_start"
        } else {
            "predictor_unavailable"
        }
        return baseline.copy(
            breakdown = baseline.breakdown.copy(
                arm = SchedulerArmKind.PLACEHOLDER,
                actualSource = "baseline_fallback",
                fallbackReason = reason,
            ),
        )
    }

    val score = 0.60f * (1f - prediction.probability) + 0.30f * baseline.breakdown.urgency + 0.10f * baseline.breakdown.avoidance
    return baseline.copy(
        breakdown = baseline.breakdown.copy(
            score = score,
            arm = SchedulerArmKind.PLACEHOLDER,
            predictedRecall = prediction.probability,
            predictorVersion = prediction.version,
            actualSource = "placeholder",
            fallbackReason = null,
        ),
    )
}

data class ArmEvaluation(
    val arm: SchedulerArmKind,
    val requestedArm: SchedulerArmKind,
    /** Every scored topic, ranked - not just the ones that made the cut - so research logging can compare full candidate sets. */
    val rankedTopics: List<ScoredTopic>,
    val selectedTopicIds: Set<String>,
    val cards: List<TodayCard>,
)

/**
 * Evaluates both scheduler arms against one coherent snapshot (DESIGN.md
 * §4: "both arms are live in the same app"). [requestedArm] is whichever arm
 * the student (or the hidden research setting) actually wants displayed;
 * both are still computed and returned so every plan can be logged for
 * comparison regardless of which one is shown.
 */
fun evaluateBothArms(
    units: List<UnitModel>,
    topics: List<Topic>,
    sessions: List<Session>,
    deadlines: List<Deadline>,
    sessionLengthMinutes: Int,
    nowMillis: Long,
    requestedArm: SchedulerArmKind,
    placeholderPredictor: RecallPredictor = PlaceholderRecallPredictor,
): Pair<ArmEvaluation, ArmEvaluation> {
    val baseline = evaluateArm(units, topics, sessions, deadlines, sessionLengthMinutes, nowMillis, SchedulerArmKind.BASELINE, requestedArm, BaselineRecallPredictor)
    val placeholder = evaluateArm(units, topics, sessions, deadlines, sessionLengthMinutes, nowMillis, SchedulerArmKind.PLACEHOLDER, requestedArm, placeholderPredictor)
    return baseline to placeholder
}

private fun evaluateArm(
    units: List<UnitModel>,
    topics: List<Topic>,
    sessions: List<Session>,
    deadlines: List<Deadline>,
    sessionLengthMinutes: Int,
    nowMillis: Long,
    arm: SchedulerArmKind,
    requestedArm: SchedulerArmKind,
    predictor: RecallPredictor,
): ArmEvaluation {
    if (units.isEmpty() || topics.isEmpty()) {
        return ArmEvaluation(arm, requestedArm, emptyList(), emptySet(), emptyList())
    }
    val sessionsByTopic = sessions.groupBy { it.topicId }
    val deadlinesByTopic = deadlines.flatMap { d -> d.topicIds.map { topicId -> topicId to d } }.groupBy({ it.first }, { it.second })
    val unitCodesById = units.associateBy({ it.id }, { it.code })

    val ranked = topics.map { topic ->
        scoreTopicForArm(topic, sessionsByTopic[topic.id].orEmpty(), deadlinesByTopic[topic.id].orEmpty(), arm, predictor, nowMillis)
    }.sortedWith(
        compareByDescending<ScoredTopic> { it.breakdown.score }
            .thenByDescending { it.topic.examWeight }
            .thenBy { it.topic.orderIndex },
    )

    val isFreshStart = sessions.isEmpty() && deadlines.isEmpty()
    val dueTopics = ranked.filter { it.breakdown.score >= DUE_SCORE_THRESHOLD }
    val selected = if (isFreshStart) {
        ranked.take(maxTopicsForSession(sessionLengthMinutes))
    } else {
        dueTopics.take(maxTopicsForSession(sessionLengthMinutes))
    }
    val cards = allocateMinutes(selected, sessionLengthMinutes, unitCodesById)
    return ArmEvaluation(arm, requestedArm, ranked, selected.map { it.topic.id }.toSet(), cards)
}

fun daysUntilDeadline(deadlineMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int =
    ((deadlineMillis - nowMillis) / DAY_MILLIS).toInt().coerceAtLeast(0)

fun schedulerUrgency(deadlineMillis: Long, nowMillis: Long = System.currentTimeMillis()): Float {
    val daysUntil = daysUntilDeadline(deadlineMillis, nowMillis)
    return max(0f, 1f - (daysUntil.toFloat() / URGENCY_WINDOW_DAYS))
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


