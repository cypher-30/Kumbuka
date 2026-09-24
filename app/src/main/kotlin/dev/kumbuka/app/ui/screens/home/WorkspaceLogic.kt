package dev.kumbuka.app.ui.screens.home

import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * Pure presentation logic for the Campus Workspace screens. Kept free of
 * Compose so it is unit-tested directly. None of this touches scheduler maths:
 * calendar labels here are deliberately independent of TodayScheduler's
 * clamped elapsed-day urgency helper.
 */

private val isoFormatterUtc: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

/** ISO date of a stored date-only value (marks/deadlines are stored at UTC midnight). */
fun formatEpochDateUtc(epochMillis: Long): String = isoFormatterUtc.format(Instant.ofEpochMilli(epochMillis))
fun todayIsoDate(): String = LocalDate.now().toString()

fun parseIsoDateMillis(raw: String): Long? = runCatching {
    LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}.getOrNull()

/** Calendar day of a stored date-only value (authoring/marks store UTC midnight). */
fun storedCalendarDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()

/** Localized medium date, e.g. "24 Sept 2026" / "24 Sep 2026". */
fun formatStoredDate(epochMillis: Long, locale: Locale): String =
    storedCalendarDate(epochMillis).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** Localized date for an instant in the device zone (sessions store real instants). */
fun formatInstantDate(epochMillis: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

fun shortMonth(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("MMM", locale)).trimEnd('.')

// ---------------------------------------------------------------------------
// Greeting
// ---------------------------------------------------------------------------

enum class GreetingPeriod { MORNING, AFTERNOON, EVENING }

/** Morning 05:00-11:59, afternoon 12:00-16:59, evening otherwise (device-local time). */
fun greetingPeriodFor(hour: Int): GreetingPeriod = when (hour) {
    in 5..11 -> GreetingPeriod.MORNING
    in 12..16 -> GreetingPeriod.AFTERNOON
    else -> GreetingPeriod.EVENING
}

/** Next instant at which the greeting or the displayed date can change (05:00, 12:00, 17:00, midnight). */
fun nextGreetingBoundary(now: LocalDateTime): LocalDateTime {
    val date = now.toLocalDate()
    val candidates = listOf(
        date.atTime(5, 0),
        date.atTime(12, 0),
        date.atTime(17, 0),
        date.plusDays(1).atStartOfDay(),
    )
    return candidates.first { it.isAfter(now) }
}

// ---------------------------------------------------------------------------
// Assessment dates
// ---------------------------------------------------------------------------

enum class AssessmentDateGroup { TODAY, NEXT_7_DAYS, LATER, PAST }

/** Signed calendar-day distance from [today] to the stored date (negative = in the past). */
fun daysFromToday(epochMillis: Long, today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, storedCalendarDate(epochMillis))

fun assessmentDateGroup(epochMillis: Long, today: LocalDate): AssessmentDateGroup {
    val days = daysFromToday(epochMillis, today)
    return when {
        days < 0 -> AssessmentDateGroup.PAST
        days == 0L -> AssessmentDateGroup.TODAY
        days <= 7 -> AssessmentDateGroup.NEXT_7_DAYS
        else -> AssessmentDateGroup.LATER
    }
}

data class AssessmentDateSection(val group: AssessmentDateGroup, val deadlines: List<Deadline>)

/**
 * Date-first grouping across all units. Future groups are soonest-first; the
 * Past group is most-recent-first and always last. Past dates are *not*
 * "overdue": the model has no submission/completion state.
 */
fun groupAssessmentDates(deadlines: List<Deadline>, today: LocalDate): List<AssessmentDateSection> {
    val grouped = deadlines.groupBy { assessmentDateGroup(it.date, today) }
    return listOf(
        AssessmentDateGroup.TODAY,
        AssessmentDateGroup.NEXT_7_DAYS,
        AssessmentDateGroup.LATER,
        AssessmentDateGroup.PAST,
    ).mapNotNull { group ->
        val rows = grouped[group].orEmpty()
        if (rows.isEmpty()) {
            null
        } else {
            val sorted = if (group == AssessmentDateGroup.PAST) rows.sortedByDescending { it.date } else rows.sortedBy { it.date }
            AssessmentDateSection(group, sorted)
        }
    }
}

/**
 * Calendar-day distance to the nearest assessment linked to [topicId], chosen
 * the same way the scheduler picks it (earliest linked date). Display only:
 * the scheduler keeps its own elapsed-time urgency maths.
 */
fun calendarDueDays(topicId: String, deadlines: List<Deadline>, today: LocalDate): Long? =
    deadlines.filter { topicId in it.topicIds }.minByOrNull { it.date }?.let { daysFromToday(it.date, today) }

/** Nearest dated assessments from today onwards, for the Home "Coming up" preview. */
fun upcomingPreview(deadlines: List<Deadline>, today: LocalDate, limit: Int = 3): List<Deadline> =
    deadlines.filter { daysFromToday(it.date, today) >= 0 }.sortedBy { it.date }.take(limit)

// ---------------------------------------------------------------------------
// Study activity
// ---------------------------------------------------------------------------

data class DayActivity(val date: LocalDate, val completedReviews: Int, val measuredMinutes: Int)

/**
 * Finished (non-deferred) reviews per local day for the last [days] days,
 * oldest first, including empty days. [excludedTopicIds] removes sample-pack
 * sessions, matching InsightsReducer's sample exclusion.
 */
fun recentActivity(
    sessions: List<Session>,
    today: LocalDate,
    zone: ZoneId,
    days: Int = 7,
    excludedTopicIds: Set<String> = emptySet(),
): List<DayActivity> {
    val start = today.minusDays((days - 1).toLong())
    val finished = sessions.filter { it.endedAt != null && !it.wasDeferred && it.topicId !in excludedTopicIds }
    val byDay = finished.groupBy { Instant.ofEpochMilli(it.endedAt!!).atZone(zone).toLocalDate() }
    return (0 until days).map { offset ->
        val date = start.plusDays(offset.toLong())
        val rows = byDay[date].orEmpty()
        DayActivity(date, rows.size, rows.sumOf { it.actualSeconds.coerceAtLeast(0) } / 60)
    }
}

/** Most recent finished, non-deferred review instant per topic. */
fun lastReviewByTopic(sessions: List<Session>): Map<String, Long> =
    sessions.filter { it.endedAt != null && !it.wasDeferred }
        .groupBy { it.topicId }
        .mapValues { (_, rows) -> rows.maxOf { it.endedAt!! } }
