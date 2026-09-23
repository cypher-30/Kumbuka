package dev.kumbuka.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.screens.packs.UnitsListBody
import dev.kumbuka.app.ui.theme.LocalKbColors

@Composable
fun UnitsTabContent(
    units: List<UnitModel>,
    topics: List<Topic>,
    topicRepository: TopicRepository,
    sessionLengthMinutes: Int,
    onImportPack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onExportUnit: (String) -> Unit,
    onStartSession: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val unitCodes = remember(units) { units.associateBy({ it.id }, { it.code }) }
    val trimmed = query.trim().lowercase()
    val results = remember(trimmed, topics, unitCodes) {
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            topics.filter { topic ->
                listOf(topic.title, topic.objective, topic.retrievalPrompt, unitCodes[topic.unitId].orEmpty())
                    .joinToString(" ")
                    .lowercase()
                    .contains(trimmed)
            }
        }
    }

    UnitsListBody(
        units = units,
        topicRepository = topicRepository,
        onImportPack = onImportPack,
        onOpenTopic = onOpenTopic,
        onExportUnit = onExportUnit,
        modifier = modifier,
        headerContent = {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(dev.kumbuka.app.ui.theme.KbSpacing.x1)) {
                        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.search_query_label)) },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        )
                        Text(
                            when {
                                trimmed.isBlank() -> stringResource(R.string.search_hint)
                                results.isEmpty() -> stringResource(R.string.search_no_results)
                                else -> stringResource(R.string.search_results_count, results.size)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalKbColors.current.inkMuted,
                        )
                    }
                }
            }
            if (results.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.search_results_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                }
                items(results, key = { it.id }) { topic ->
                    SearchResultCard(
                        topic = topic,
                        unitCode = unitCodes[topic.unitId] ?: topic.unitId,
                        sessionLengthMinutes = sessionLengthMinutes,
                        onOpenTopic = onOpenTopic,
                        onStartSession = onStartSession,
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchResultCard(
    topic: Topic,
    unitCode: String,
    sessionLengthMinutes: Int,
    onOpenTopic: (String) -> Unit,
    onStartSession: (String, Int) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(dev.kumbuka.app.ui.theme.KbSpacing.x1)) {
            Text(unitCode, style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
            Text(topic.title, style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(topic.objective, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (topic.retrievalPrompt.isNotBlank()) {
                Text(topic.retrievalPrompt, style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            KbPrimaryButton(text = stringResource(R.string.search_start_session, sessionLengthMinutes), onClick = { onStartSession(topic.id, sessionLengthMinutes) })
            androidx.compose.material3.TextButton(onClick = { onOpenTopic(topic.id) }) {
                Text(stringResource(R.string.topic_detail_cta))
            }
        }
    }
}
