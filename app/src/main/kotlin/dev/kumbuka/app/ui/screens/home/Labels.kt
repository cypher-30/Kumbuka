@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.AssessmentKind
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.DeadlineKind
import dev.kumbuka.app.domain.scheduler.DominantPressure
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayReasonFact
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.theme.LocalKbColors
import java.util.Locale

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales.let { if (it.isEmpty) Locale.ROOT else it[0] }

@Composable
fun confidenceLabel(confidence: Confidence): String = when (confidence) {
    Confidence.BLANK -> stringResource(R.string.confidence_blank)
    Confidence.SHAKY -> stringResource(R.string.confidence_shaky)
    Confidence.OK -> stringResource(R.string.confidence_ok)
    Confidence.SOLID -> stringResource(R.string.confidence_solid)
}

@Composable
fun assessmentKindLabel(kind: AssessmentKind): String = when (kind) {
    AssessmentKind.CAT -> stringResource(R.string.marks_kind_cat)
    AssessmentKind.ASSIGNMENT -> stringResource(R.string.marks_kind_assignment)
    AssessmentKind.PAST_PAPER -> stringResource(R.string.marks_kind_past_paper)
    AssessmentKind.EXAM -> stringResource(R.string.marks_kind_exam)
}

@Composable
fun deadlineKindLabel(kind: DeadlineKind): String = when (kind) {
    DeadlineKind.CAT -> stringResource(R.string.marks_kind_cat)
    DeadlineKind.ASSIGNMENT -> stringResource(R.string.marks_kind_assignment)
    DeadlineKind.EXAM -> stringResource(R.string.marks_kind_exam)
}

/** Calendar-relative label: "Today", "Tomorrow", "In 5 days", "Yesterday", "3 days ago". */
@Composable
fun relativeDayLabel(days: Long): String = when {
    days == 0L -> stringResource(R.string.relative_today)
    days == 1L -> stringResource(R.string.relative_tomorrow)
    days == -1L -> stringResource(R.string.relative_yesterday)
    days > 1L -> pluralStringResource(R.plurals.relative_in_days, days.toInt(), days.toInt())
    else -> pluralStringResource(R.plurals.relative_days_ago, (-days).toInt(), (-days).toInt())
}

@Composable
fun dateGroupLabel(group: AssessmentDateGroup): String = when (group) {
    AssessmentDateGroup.TODAY -> stringResource(R.string.date_group_today)
    AssessmentDateGroup.NEXT_7_DAYS -> stringResource(R.string.date_group_next_7)
    AssessmentDateGroup.LATER -> stringResource(R.string.date_group_later)
    AssessmentDateGroup.PAST -> stringResource(R.string.date_group_past)
}

@Composable
fun todayReasonText(card: TodayCard, calendarDueDays: Long? = null): String {
    val pieces = card.breakdown.reasonFacts().mapNotNull { fact ->
        when (fact) {
            is TodayReasonFact.DueInDays -> {
                val days = calendarDueDays ?: fact.days.toLong()
                when {
                    days < 0 -> null
                    days == 0L -> stringResource(R.string.today_reason_due_today)
                    else -> stringResource(R.string.today_reason_due_in_days, days.toInt())
                }
            }
            is TodayReasonFact.LastRated -> stringResource(R.string.today_reason_last_rated, confidenceLabel(fact.confidence))
            is TodayReasonFact.StaleForDays -> stringResource(R.string.today_reason_last_reviewed_days, fact.days)
            is TodayReasonFact.DeferredCount -> stringResource(R.string.today_reason_deferred_count, fact.count)
            TodayReasonFact.NoHistory -> stringResource(R.string.today_reason_no_history)
        }
    }
    return pieces.joinToString(separator = " · ")
}

@Composable
private fun dominantPressureText(pressure: DominantPressure): String = when (pressure) {
    DominantPressure.GAP -> stringResource(R.string.today_why_dominant_gap)
    DominantPressure.STALENESS -> stringResource(R.string.today_why_dominant_staleness)
    DominantPressure.URGENCY -> stringResource(R.string.today_why_dominant_urgency)
    DominantPressure.AVOIDANCE -> stringResource(R.string.today_why_dominant_avoidance)
}

/**
 * Explainable-AI panel for the trained (Learned) arm: shows the model's inputs (right/wrong
 * counts, days since review) and its working (half-life, chance of remembering, the 90% rule).
 * Shows a short fallback note when the Learned arm had to use the baseline formula, and
 * nothing at all on the baseline arm.
 */
@Composable
private fun ModelExplanation(card: TodayCard) {
    val colors = LocalKbColors.current
    val b = card.breakdown
    val trained = b.actualSource == "trained" && b.predictedRecall != null && b.modelHalfLifeDays != null
    val fallback = b.actualSource == "baseline_fallback"
    if (!trained && !fallback) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paper, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.why_model_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.ink)
        if (fallback) {
            Text(stringResource(R.string.why_model_fallback), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            return@Column
        }
        val oneDecimal: (Float) -> String = { java.lang.String.format(java.util.Locale.getDefault(), "%.1f", it) }
        Text(stringResource(R.string.why_model_counts, b.modelRightCount ?: 0, b.modelWrongCount ?: 0), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        Text(stringResource(R.string.why_model_half_life, oneDecimal(b.modelHalfLifeDays ?: 0f)), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        Text(
            stringResource(R.string.why_model_since, oneDecimal(b.modelDaysSinceReview ?: 0f), kotlin.math.round((b.predictedRecall ?: 0f) * 100f).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
        )
        Text(stringResource(R.string.why_model_rule), style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        Text(stringResource(R.string.why_model_source, b.predictorVersion.orEmpty()), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
    }
}

/** Plain-language explanation of a recommendation, with a direct Start action. */
@Composable
fun WhyThisSheet(card: TodayCard, unitKey: String, onDismiss: () -> Unit, onStart: () -> Unit, calendarDueDays: Long? = null) {
    val colors = LocalKbColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        scrimColor = colors.scrim,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = colors.primary)
                Text(stringResource(R.string.today_why_this), style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp, lineHeight = 30.sp), color = colors.ink)
            }
            KbUnitLabel(unitKey = unitKey, unitCode = card.unitCode, emphasized = true)
            Text(card.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.ink)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryTint, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // When the trained model chose this topic and memory (not a deadline) is the main reason, say so.
                val modelLed = card.breakdown.actualSource == "trained" &&
                    card.breakdown.dominantPressure() != DominantPressure.URGENCY &&
                    (card.breakdown.predictedRecall ?: 1f) < dev.kumbuka.app.domain.scheduler.MODEL_TARGET_RECALL
                val headline = if (modelLed) stringResource(R.string.today_why_dominant_model) else dominantPressureText(card.breakdown.dominantPressure())
                Text(headline, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.onPrimaryTint)
                Text(todayReasonText(card, calendarDueDays), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryTint)
            }
            ModelExplanation(card)
            KbPrimaryButton(text = stringResource(R.string.home_start_revision_minutes, card.minutes), onClick = onStart)
            KbSecondaryButton(text = stringResource(R.string.generic_close), onClick = onDismiss)
            Spacer(Modifier.height(16.dp))
        }
    }
}

