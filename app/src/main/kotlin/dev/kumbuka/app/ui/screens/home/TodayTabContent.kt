@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.scheduler.DominantPressure
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayPlan
import dev.kumbuka.app.domain.scheduler.TodayReasonFact
import dev.kumbuka.app.domain.scheduler.TodayState
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun TodayTabContent(
    plan: TodayPlan,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onStartSession: (TodayCard) -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeWhyCard by remember { mutableStateOf<TodayCard?>(null) }
    val hasDueToday = remember(plan.cards) { plan.cards.any { it.breakdown.daysUntilDeadline == 0 } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (plan.state != TodayState.NoUnits) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onOpenInsights) {
                        Icon(Icons.Outlined.Insights, contentDescription = null)
                        Spacer(Modifier.height(0.dp))
                        Text(stringResource(R.string.insights_title))
                    }
                }
            }
        }

        when (plan.state) {
            TodayState.NoUnits -> item { NoUnitsState(onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
            TodayState.NoActiveTopics -> item { StateMessageCard(text = stringResource(R.string.today_no_active_topics_body)) }
            TodayState.FreshStart -> {
                item { StateMessageCard(text = stringResource(R.string.today_fresh_start_body)) }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicRow(card = card, onWhyThis = { activeWhyCard = card }, onStartSession = { onStartSession(card) })
                }
            }
            TodayState.Today -> {
                item {
                    StateMessageCard(
                        text = if (hasDueToday) {
                            stringResource(R.string.today_due_today_body)
                        } else {
                            stringResource(R.string.today_ranked_body, plan.sessionLengthMinutes)
                        },
                    )
                }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicRow(card = card, onWhyThis = { activeWhyCard = card }, onStartSession = { onStartSession(card) })
                }
            }
            TodayState.AllCaughtUp -> item { StateMessageCard(text = stringResource(R.string.today_all_caught_up_body)) }
        }
    }

    activeWhyCard?.let { card ->
        WhyThisSheet(card = card, onDismiss = { activeWhyCard = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true))
    }
}

@Composable
private fun StateMessageCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalKbColors.current.inkMuted,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun NoUnitsState(onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.units_empty_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.today_no_units_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun TodayTopicRow(card: TodayCard, onWhyThis: () -> Unit, onStartSession: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.clickable(onClick = onStartSession),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.unitCode, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
                    Text(card.title, style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                }
                AssistChip(onClick = onStartSession, label = { Text(stringResource(R.string.today_minutes_short, card.minutes)) })
            }
            Text(todayReasonText(card), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onWhyThis, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Text(stringResource(R.string.today_why_this))
            }
        }
    }
}

@Composable
private fun WhyThisSheet(card: TodayCard, onDismiss: () -> Unit, sheetState: androidx.compose.material3.SheetState) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LocalKbColors.current.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = LocalKbColors.current.primary)
                Text(stringResource(R.string.today_why_this), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            }
            Text(todayReasonText(card), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink)
            Text(dominantPressureText(card.breakdown.dominantPressure()), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.generic_ok))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun dominantPressureText(pressure: DominantPressure): String = when (pressure) {
    DominantPressure.GAP -> stringResource(R.string.today_why_dominant_gap)
    DominantPressure.STALENESS -> stringResource(R.string.today_why_dominant_staleness)
    DominantPressure.URGENCY -> stringResource(R.string.today_why_dominant_urgency)
    DominantPressure.AVOIDANCE -> stringResource(R.string.today_why_dominant_avoidance)
}

@Composable
private fun todayReasonText(card: TodayCard): String {
    val pieces = card.breakdown.reasonFacts().map { fact ->
        when (fact) {
            is TodayReasonFact.DueInDays -> if (fact.days == 0) {
                stringResource(R.string.today_reason_due_today)
            } else {
                stringResource(R.string.today_reason_due_in_days, fact.days)
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
private fun confidenceLabel(confidence: Confidence): String =
    when (confidence) {
        Confidence.BLANK -> stringResource(R.string.confidence_blank)
        Confidence.SHAKY -> stringResource(R.string.confidence_shaky)
        Confidence.OK -> stringResource(R.string.confidence_ok)
        Confidence.SOLID -> stringResource(R.string.confidence_solid)
    }
