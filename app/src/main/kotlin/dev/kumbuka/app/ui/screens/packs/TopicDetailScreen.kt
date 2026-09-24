package dev.kumbuka.app.ui.screens.packs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbStatusPill
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.KbUnitLabel
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.screens.home.confidenceLabel
import dev.kumbuka.app.ui.screens.home.currentLocale
import dev.kumbuka.app.ui.screens.home.formatInstantDate
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch

/** Read-first topic page: what to know, how to test yourself, where to look, then Start revision. */
@Composable
fun TopicDetailScreen(
    topicRepository: TopicRepository,
    unitRepository: UnitRepository,
    sessionRepository: SessionRepository,
    topicId: String,
    sessionLengthMinutes: Int,
    onStartSession: (String, Int) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalKbColors.current
    val locale = currentLocale()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.topic_saved_objective)
    var loaded by remember(topicId) { mutableStateOf(false) }
    var topic by remember(topicId) { mutableStateOf<Topic?>(null) }
    var unit by remember(topicId) { mutableStateOf<UnitModel?>(null) }
    var editing by rememberSaveable(topicId) { mutableStateOf(false) }
    var draft by rememberSaveable(topicId) { mutableStateOf("") }
    var showDiscard by rememberSaveable(topicId) { mutableStateOf(false) }
    val sessions by remember(topicId) { sessionRepository.observeByTopic(topicId) }.collectAsState(initial = emptyList())
    val lastFinished = remember(sessions) { sessions.filter { it.endedAt != null && !it.wasDeferred }.maxByOrNull { it.endedAt!! } }

    LaunchedEffect(topicId) {
        topic = topicRepository.getById(topicId)
        unit = topic?.let { unitRepository.getById(it.unitId) }
        loaded = true
    }

    val current = topic
    val dirty = editing && current != null && draft != current.objective
    val exitEdit = {
        editing = false
        showDiscard = false
    }
    BackHandler(enabled = editing) { if (dirty) showDiscard = true else exitEdit() }

    Scaffold(
        topBar = {
            KbTopBar(
                title = unit?.code ?: stringResource(R.string.topic_detail_title),
                onBack = { if (dirty) showDiscard = true else if (editing) exitEdit() else onBack() },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = colors.paper,
    ) { innerPadding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return@Scaffold
        }
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.topic_not_found), color = colors.inkMuted)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.kbContentWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KbUnitLabel(unitKey = current.unitId, unitCode = unit?.let { "${it.code} · ${it.title}" } ?: stringResource(R.string.topic_unit_unknown), emphasized = true)
                    if (current.archived) KbStatusPill(stringResource(R.string.unit_detail_archived_label), KbStatus.NEUTRAL)
                }
                Text(current.title, style = MaterialTheme.typography.headlineSmall, color = colors.ink, modifier = Modifier.semantics { heading() })
                Text(
                    if (lastFinished != null) {
                        val rating = lastFinished.confidenceAfter ?: lastFinished.confidenceBefore
                        val date = formatInstantDate(lastFinished.endedAt!!, locale)
                        if (rating != null) stringResource(R.string.topic_last_review_rated, date, confidenceLabel(rating)) else stringResource(R.string.unit_detail_last_reviewed, date)
                    } else {
                        stringResource(R.string.unit_detail_not_reviewed)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )

                if (current.archived) {
                    KbBanner(stringResource(R.string.session_topic_archived), KbStatus.NEUTRAL)
                } else {
                    KbPrimaryButton(
                        text = stringResource(R.string.home_start_revision_minutes, sessionLengthMinutes),
                        onClick = { onStartSession(current.id, sessionLengthMinutes) },
                        icon = Icons.Outlined.PlayArrow,
                    )
                }

                KbSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.topic_objective_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        KbStatusPill(
                            if (current.objectiveEditedLocally) stringResource(R.string.topic_edited_by_you) else stringResource(R.string.topic_from_pack),
                            if (current.objectiveEditedLocally) KbStatus.INFO else KbStatus.NEUTRAL,
                        )
                    }
                    if (editing) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 10,
                            label = { Text(stringResource(R.string.topic_objective_label)) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KbSecondaryButton(
                                text = stringResource(R.string.generic_cancel),
                                onClick = { if (dirty) showDiscard = true else exitEdit() },
                                modifier = Modifier.weight(1f),
                            )
                            KbPrimaryButton(
                                text = stringResource(R.string.topic_save_objective),
                                enabled = dirty && draft.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        topicRepository.upsert(
                                            current.copy(objective = draft.trim(), objectiveEditedLocally = true, updatedAt = System.currentTimeMillis()),
                                        )
                                        topic = topicRepository.getById(topicId)
                                        editing = false
                                        snackbar.showSnackbar(savedText)
                                    }
                                },
                            )
                        }
                    } else {
                        Text(current.objective.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                        TextButton(onClick = {
                            draft = current.objective
                            editing = true
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(stringResource(R.string.topic_edit_objective))
                        }
                    }
                }

                KbSurface(color = colors.primaryTint, borderColor = null) {
                    Text(stringResource(R.string.topic_retrieval_prompt_label), style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryTint, modifier = Modifier.semantics { heading() })
                    Text(
                        current.retrievalPrompt.ifBlank { stringResource(R.string.topic_retrieval_prompt_empty) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.ink,
                    )
                }

                if (current.resourcePointers.isNotEmpty()) {
                    KbSurface {
                        Text(stringResource(R.string.topic_resources_label), style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.semantics { heading() })
                        current.resourcePointers.forEach { pointer ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.padding(top = 2.dp))
                                Text(pointer, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.topic_weight_line, current.examWeight),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.topic_discard_title)) },
            text = { Text(stringResource(R.string.topic_discard_body)) },
            confirmButton = { TextButton(onClick = exitEdit) { Text(stringResource(R.string.author_discard_confirm), color = colors.error) } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.author_discard_cancel)) } },
        )
    }
}
