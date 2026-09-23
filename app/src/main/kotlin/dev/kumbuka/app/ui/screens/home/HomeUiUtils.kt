package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.scheduler.daysUntilDeadline
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
private val dateFormatterUtc: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

fun formatEpochDate(epochMillis: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMillis))
fun formatEpochDateUtc(epochMillis: Long): String = dateFormatterUtc.format(Instant.ofEpochMilli(epochMillis))
fun todayIsoDate(): String = LocalDate.now().toString()

fun parseIsoDateMillis(raw: String): Long? = runCatching {
    LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}.getOrNull()

enum class MarkDateBucket { TODAY, THIS_WEEK, THIS_MONTH, LATER }

data class UnitDeadlineBucket(
    val unitId: String,
    val unitCode: String,
    val buckets: List<DateBucketGroup>,
)

data class DateBucketGroup(
    val bucket: MarkDateBucket,
    val deadlines: List<Deadline>,
)

fun buildUnitDeadlineBuckets(
    deadlines: List<Deadline>,
    unitCodes: Map<String, String>,
    nowMillis: Long = System.currentTimeMillis(),
): List<UnitDeadlineBucket> {
    val ordered = listOf(MarkDateBucket.TODAY, MarkDateBucket.THIS_WEEK, MarkDateBucket.THIS_MONTH, MarkDateBucket.LATER)
    return deadlines
        .groupBy { it.unitId }
        .toList()
        .sortedBy { (unitId, _) -> unitCodes[unitId] ?: unitId }
        .map { (unitId, rows) ->
            val grouped = rows.groupBy { deadline ->
                when (daysUntilDeadline(deadline.date, nowMillis)) {
                    0 -> MarkDateBucket.TODAY
                    in 1..7 -> MarkDateBucket.THIS_WEEK
                    in 8..30 -> MarkDateBucket.THIS_MONTH
                    else -> MarkDateBucket.LATER
                }
            }
            UnitDeadlineBucket(
                unitId = unitId,
                unitCode = unitCodes[unitId] ?: unitId,
                buckets = ordered.mapNotNull { bucket ->
                    grouped[bucket]?.takeIf { it.isNotEmpty() }?.let { DateBucketGroup(bucket, it.sortedBy { d -> d.date }) }
                },
            )
        }
}

@Composable
fun bucketLabel(bucket: MarkDateBucket): String =
    when (bucket) {
        MarkDateBucket.TODAY -> stringResource(R.string.exams_bucket_today)
        MarkDateBucket.THIS_WEEK -> stringResource(R.string.exams_bucket_this_week)
        MarkDateBucket.THIS_MONTH -> stringResource(R.string.exams_bucket_this_month)
        MarkDateBucket.LATER -> stringResource(R.string.exams_bucket_later)
    }
