@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayPlan
import dev.kumbuka.app.domain.scheduler.TodayState
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun TodayTabContent(
    plan: TodayPlan,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onStartSession: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeWhyCard by remember { mutableStateOf<TodayCard?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (plan.state != TodayState.NoUnits) {
            item { SummaryCard(plan = plan, onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
        }
        when (plan.state) {
            TodayState.NoUnits -> item { NoUnitsState(onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
            TodayState.FreshStart -> {
                item { StatePreamble(text = plan.body) }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicCard(card = card, onWhyThis = { activeWhyCard = card }, onStartSession = onStartSession)
                }
                item { BrowseUnitsPrompt(onBrowseUnits) }
            }
            TodayState.Tonight -> {
                item { StatePreamble(text = plan.body) }
                items(plan.cards, key = { it.topicId }) { card ->
                    TodayTopicCard(card = card, onWhyThis = { activeWhyCard = card }, onStartSession = onStartSession)
                }
            }
            TodayState.AllCaughtUp -> item { AllCaughtUpState(onBrowseUnits = onBrowseUnits) }
        }
    }

    activeWhyCard?.let { card ->
        WhyThisSheet(
            card = card,
            onDismiss = { activeWhyCard = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        )
    }
}

@Composable
private fun SummaryCard(plan: TodayPlan, onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(plan.headline, style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            Text(plan.body, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            AssistChip(onClick = onBrowseUnits, label = { Text(stringResource(R.string.today_session_length, plan.sessionLengthMinutes)) })
            KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun StatePreamble(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
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
private fun BrowseUnitsPrompt(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_keep_going_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.today_keep_going_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun AllCaughtUpState(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_all_caught_up_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.today_all_caught_up_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun TodayTopicCard(card: TodayCard, onWhyThis: () -> Unit, onStartSession: (String, Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(card.unitCode, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
                    Text(card.title, style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                }
                AssistChip(onClick = onWhyThis, label = { Text("${card.minutes} min") })
            }
            Text(card.objective, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onWhyThis, label = { Text(stringResource(R.string.today_why_this)) }, leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) })
                Text(
                    text = stringResource(R.string.today_score_label, card.score),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKbColors.current.inkMuted,
                    fontWeight = FontWeight.Medium,
                )
            }
            KbPrimaryButton(text = stringResource(R.string.today_start_session), onClick = { onStartSession(card.topicId, card.minutes) })
        }
    }
}

@Composable
private fun WhyThisSheet(card: TodayCard, onDismiss: () -> Unit, sheetState: androidx.compose.material3.SheetState) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LocalKbColors.current.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = LocalKbColors.current.primary)
                Text(stringResource(R.string.today_why_this), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            }
            Text(card.breakdown.formulaSummary(), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.ink)
            Text(card.breakdown.plainLanguageSummary(), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            BreakdownLine(label = stringResource(R.string.today_gap), value = "${(card.breakdown.gap * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_staleness), value = "${(card.breakdown.staleness * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_urgency), value = "${(card.breakdown.urgency * 100).toInt()}%")
            BreakdownLine(label = stringResource(R.string.today_avoidance), value = "${(card.breakdown.avoidance * 100).toInt()}%")
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.generic_ok))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.ink)
    }
}
