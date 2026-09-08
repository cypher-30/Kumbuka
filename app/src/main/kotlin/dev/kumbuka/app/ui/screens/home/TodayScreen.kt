@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.scheduler.TodayCard
import dev.kumbuka.app.domain.scheduler.TodayPlan
import dev.kumbuka.app.domain.scheduler.TodayState
import dev.kumbuka.app.domain.scheduler.buildTodayPlan
import dev.kumbuka.app.ui.components.KbBottomNavBar
import dev.kumbuka.app.ui.components.KbNavTab
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbColors

@Composable
fun TodayScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
) {
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())
    val topics by topicRepository.observeAll().collectAsState(initial = emptyList())
    val sessions by sessionRepository.observeAll().collectAsState(initial = emptyList())
    val deadlines by deadlineRepository.observeAll().collectAsState(initial = emptyList())
    val plan = remember(units, topics, sessions, deadlines) {
        buildTodayPlan(
            units = units,
            topics = topics,
            sessions = sessions,
            deadlines = deadlines,
        )
    }
    var activeTab by remember { mutableStateOf(KbNavTab.TONIGHT) }
    var activeWhyCard by remember { mutableStateOf<TodayCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.headline) },
                actions = {
                    TextButton(onClick = onBrowseUnits) {
                        Text(stringResource(R.string.home_browse_units))
                    }
                },
            )
        },
        bottomBar = { KbBottomNavBar(active = activeTab, onSelect = { activeTab = it }) },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(plan = plan, onBrowseUnits = onBrowseUnits, onImportPack = onImportPack)
            }

            when (plan.state) {
                TodayState.NoUnits -> item { NoUnitsState(onBrowseUnits = onBrowseUnits, onImportPack = onImportPack) }
                TodayState.FreshStart -> {
                    item { StatePreamble(text = plan.body) }
                    items(plan.cards, key = { it.topicId }) { card ->
                        TodayTopicCard(card = card, onWhyThis = { activeWhyCard = card })
                    }
                    item { BrowseUnitsPrompt(onBrowseUnits) }
                }
                TodayState.Tonight -> {
                    item { StatePreamble(text = plan.body) }
                    items(plan.cards, key = { it.topicId }) { card ->
                        TodayTopicCard(card = card, onWhyThis = { activeWhyCard = card })
                    }
                }
                TodayState.AllCaughtUp -> item { AllCaughtUpState(onBrowseUnits = onBrowseUnits) }
            }
        }
    }

    activeWhyCard?.let { card ->
        WhyThisDialog(
            card = card,
            onDismiss = { activeWhyCard = null },
        )
    }
}

@Composable
private fun SummaryCard(plan: TodayPlan, onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(plan.headline, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(plan.body, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            AssistChip(
                onClick = onBrowseUnits,
                label = { Text("${plan.sessionLengthMinutes} min baseline session") },
            )
            KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun StatePreamble(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
}

@Composable
private fun NoUnitsState(onBrowseUnits: () -> Unit, onImportPack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.units_empty_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(stringResource(R.string.today_no_units_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
                KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun BrowseUnitsPrompt(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_keep_going_title), style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
            Text(stringResource(R.string.today_keep_going_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun AllCaughtUpState(onBrowseUnits: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.today_all_caught_up_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(stringResource(R.string.today_all_caught_up_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
            KbSecondaryButton(text = stringResource(R.string.home_browse_units), onClick = onBrowseUnits)
        }
    }
}

@Composable
private fun TodayTopicCard(card: TodayCard, onWhyThis: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(card.unitCode, style = MaterialTheme.typography.labelLarge, color = KbColors.primary)
                    Text(card.title, style = MaterialTheme.typography.titleMedium, color = KbColors.ink)
                }
                AssistChip(onClick = onWhyThis, label = { Text("${card.minutes} min") })
            }
            Text(
                text = card.objective,
                style = MaterialTheme.typography.bodyMedium,
                color = KbColors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onWhyThis, label = { Text(stringResource(R.string.today_why_this)) }, leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) })
                Text(
                    text = stringResource(R.string.today_score_label, card.score),
                    style = MaterialTheme.typography.bodySmall,
                    color = KbColors.inkMuted,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun WhyThisDialog(card: TodayCard, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.today_why_this)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(card.breakdown.formulaSummary(), style = MaterialTheme.typography.bodyMedium, color = KbColors.ink)
                Text(card.breakdown.plainLanguageSummary(), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
                BreakdownLine(label = stringResource(R.string.today_gap), value = "${(card.breakdown.gap * 100).toInt()}%")
                BreakdownLine(label = stringResource(R.string.today_staleness), value = "${(card.breakdown.staleness * 100).toInt()}%")
                BreakdownLine(label = stringResource(R.string.today_urgency), value = "${(card.breakdown.urgency * 100).toInt()}%")
                BreakdownLine(label = stringResource(R.string.today_avoidance), value = "${(card.breakdown.avoidance * 100).toInt()}%")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_ok))
            }
        },
    )
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KbColors.inkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = KbColors.ink)
    }
}






