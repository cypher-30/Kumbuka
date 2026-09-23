@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Confidence
import dev.kumbuka.app.domain.model.Session
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.LocalKbColors
import dev.kumbuka.app.ui.theme.KbSpacing
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SessionMaxWidth = 760.dp

private enum class SessionStage {
    Recall,
    RateBefore,
    Restudy,
    RateAfter,
}

@Composable
fun SessionFlowScreen(
    topicRepository: TopicRepository,
    unitRepository: UnitRepository,
    sessionRepository: SessionRepository,
    topicId: String,
    plannedMinutes: Int,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var topic by remember { mutableStateOf<Topic?>(null) }
    var unit by remember { mutableStateOf<UnitModel?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var stageName by rememberSaveable(topicId) { mutableStateOf(SessionStage.Recall.name) }
    var beforeSelectionName by rememberSaveable(topicId) { mutableStateOf<String?>(null) }
    var afterSelectionName by rememberSaveable(topicId) { mutableStateOf<String?>(null) }
    var showDeferSheet by rememberSaveable(topicId) { mutableStateOf(false) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val deferSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentTopic = topic
    val currentUnit = unit
    val currentSession = session
    val effectivePlannedMinutes = currentSession?.plannedMinutes ?: plannedMinutes
    val remainingMillis = currentSession?.let {
        sessionRemainingMillis(it.startedAt, effectivePlannedMinutes, nowMillis)
    } ?: 0L
    val stage = when {
        currentSession == null -> null
        currentSession.endedAt != null -> null
        currentSession.confidenceBefore == null -> {
            if (stageName == SessionStage.RateBefore.name) SessionStage.RateBefore else SessionStage.Recall
        }
        currentSession.confidenceAfter == null -> {
            if (stageName == SessionStage.RateAfter.name || remainingMillis <= 0L) SessionStage.RateAfter else SessionStage.Restudy
        }
        else -> null
    }

    LaunchedEffect(topicId, plannedMinutes) {
        loading = true
        loadError = null
        val loadedTopic = topicRepository.getById(topicId)
        val loadedUnit = loadedTopic?.let { unitRepository.getById(it.unitId) }
        val activeSession = sessionRepository.getLatestActiveForTopic(topicId)
        val current = activeSession ?: Session(
            id = UUID.randomUUID().toString(),
            topicId = topicId,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            plannedMinutes = plannedMinutes,
            actualSeconds = 0,
            confidenceBefore = null,
            confidenceAfter = null,
            wasDeferred = false,
            updatedAt = System.currentTimeMillis(),
        )
        if (activeSession == null) {
            sessionRepository.upsert(current)
        }

        topic = loadedTopic
        unit = loadedUnit
        session = current
        beforeSelectionName = current.confidenceBefore?.name
        afterSelectionName = current.confidenceAfter?.name
        stageName = when {
            current.confidenceBefore == null -> SessionStage.Recall.name
            current.confidenceAfter == null && sessionRemainingMillis(current.startedAt, current.plannedMinutes, System.currentTimeMillis()) > 0L -> SessionStage.Restudy.name
            current.confidenceAfter == null -> SessionStage.RateAfter.name
            else -> SessionStage.Restudy.name
        }
        loadError = if (loadedTopic == null) context.getString(R.string.session_not_found) else null
        loading = false
    }

    LaunchedEffect(currentSession?.id, currentSession?.endedAt) {
        while (currentSession != null && currentSession.endedAt == null) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.session_loading), color = LocalKbColors.current.inkMuted)
                }
            }
            loadError != null || currentTopic == null || currentSession == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(KbSpacing.x3),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(loadError ?: stringResource(R.string.session_not_found), color = LocalKbColors.current.inkMuted)
                    KbSecondaryButton(text = stringResource(R.string.generic_back), onClick = onBack, modifier = Modifier.padding(top = KbSpacing.x2))
                }
            }
            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    val compact = maxWidth < 360.dp
                    val horizontalInset = if (compact) KbSpacing.x1 else KbSpacing.x2
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = SessionMaxWidth)
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = horizontalInset, vertical = KbSpacing.x2),
                        verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
                    ) {
                        item {
                            SessionHeaderCard(
                                topic = currentTopic,
                                unit = currentUnit,
                                session = currentSession,
                                plannedMinutes = effectivePlannedMinutes,
                                remainingMillis = remainingMillis,
                            )
                        }

                        item {
                            AnimatedContent(
                                targetState = stage,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220)) +
                                        slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 8 })) togetherWith
                                        (fadeOut(animationSpec = tween(160)) +
                                            slideOutVertically(animationSpec = tween(160), targetOffsetY = { -it / 8 }))
                                },
                                label = "sessionStageTransition",
                            ) { stageTarget ->
                                when (stageTarget) {
                                    SessionStage.Recall -> RecallStepCard(
                                        topic = currentTopic,
                                        remainingMillis = remainingMillis,
                                        onRateRecall = { stageName = SessionStage.RateBefore.name },
                                        onDefer = { showDeferSheet = true },
                                    )
                                    SessionStage.RateBefore -> RateBeforeStepCard(
                                        selected = beforeSelectionName?.let(Confidence::valueOf),
                                        onSelect = { beforeSelectionName = it.name },
                                        onContinue = {
                                            val selection = beforeSelectionName?.let(Confidence::valueOf)
                                            if (selection != null) {
                                                val updated = currentSession.copy(
                                                    confidenceBefore = selection,
                                                    updatedAt = System.currentTimeMillis(),
                                                )
                                                scope.launch {
                                                    sessionRepository.upsert(updated)
                                                    session = updated
                                                    stageName = SessionStage.Restudy.name
                                                }
                                            }
                                        },
                                    )
                                    SessionStage.Restudy -> RestudyStepCard(
                                        remainingMillis = remainingMillis,
                                        onContinue = { stageName = SessionStage.RateAfter.name },
                                    )
                                    SessionStage.RateAfter -> RateAfterStepCard(
                                        selected = afterSelectionName?.let(Confidence::valueOf),
                                        onSelect = { afterSelectionName = it.name },
                                        onFinish = {
                                            val selection = afterSelectionName?.let(Confidence::valueOf)
                                            if (selection != null) {
                                                val now = System.currentTimeMillis()
                                                val updated = currentSession.copy(
                                                    confidenceAfter = selection,
                                                    endedAt = now,
                                                    actualSeconds = ((now - currentSession.startedAt) / 1000L).toInt().coerceAtLeast(0),
                                                    updatedAt = now,
                                                )
                                                scope.launch {
                                                    sessionRepository.upsert(updated)
                                                    session = updated
                                                    onFinished()
                                                }
                                            }
                                        },
                                    )
                                    null -> Card(
                                        colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface),
                                        shape = MaterialTheme.shapes.large,
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(KbSpacing.x2),
                                            verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
                                        ) {
                                            Text(
                                                stringResource(R.string.session_complete_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = LocalKbColors.current.ink,
                                            )
                                            Text(
                                                stringResource(R.string.session_complete_body),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = LocalKbColors.current.inkMuted,
                                            )
                                            KbPrimaryButton(text = stringResource(R.string.generic_back), onClick = onFinished)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeferSheet && currentSession != null) {
        DeferSessionSheet(
            sheetState = deferSheetState,
            onDismiss = { showDeferSheet = false },
            onConfirm = {
                val now = System.currentTimeMillis()
                val updated = currentSession.copy(
                    endedAt = now,
                    actualSeconds = ((now - currentSession.startedAt) / 1000L).toInt().coerceAtLeast(0),
                    wasDeferred = true,
                    updatedAt = now,
                )
                scope.launch {
                    sessionRepository.upsert(updated)
                    session = updated
                    showDeferSheet = false
                    onFinished()
                }
            },
        )
    }
}

@Composable
private fun DeferSessionSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalKbColors.current.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KbSpacing.x3, vertical = KbSpacing.x1)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = LocalKbColors.current.primary)
                Text(stringResource(R.string.session_defer_title), style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
            }
            Text(stringResource(R.string.session_defer_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            KbPrimaryButton(text = stringResource(R.string.session_defer_confirm), onClick = onConfirm)
            KbSecondaryButton(text = stringResource(R.string.session_defer_cancel), onClick = onDismiss)
        }
    }
}

@Composable
private fun SessionHeaderCard(
    topic: Topic,
    unit: UnitModel?,
    session: Session,
    plannedMinutes: Int,
    remainingMillis: Long,
) {
    val totalMillis = (plannedMinutes.coerceAtLeast(1) * 60_000L)
    val progress = if (session.endedAt != null) 1f else ((totalMillis - remainingMillis).toFloat() / totalMillis).coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x1 / 2)) {
                Text(unit?.code ?: stringResource(R.string.session_unit_unknown), style = MaterialTheme.typography.labelLarge, color = LocalKbColors.current.primary)
                Text(topic.title, style = MaterialTheme.typography.titleLarge, color = LocalKbColors.current.ink)
                Text(topic.objective, style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(KbSpacing.x1), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.session_planned_minutes, plannedMinutes), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.session_time_remaining, formatDuration(remainingMillis)), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
            }
        }
    }
}

@Composable
private fun RecallStepCard(
    topic: Topic,
    remainingMillis: Long,
    onRateRecall: () -> Unit,
    onDefer: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_recall_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_recall_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            Text(
                text = topic.retrievalPrompt.ifBlank { topic.objective },
                style = MaterialTheme.typography.bodyMedium,
                color = LocalKbColors.current.ink,
            )
            Text(
                text = stringResource(R.string.session_time_remaining, formatDuration(remainingMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = LocalKbColors.current.primary,
            )
            KbPrimaryButton(text = stringResource(R.string.session_done_recalling), onClick = onRateRecall)
            KbSecondaryButton(text = stringResource(R.string.session_defer), onClick = onDefer)
        }
    }
}

@Composable
private fun RateBeforeStepCard(
    selected: Confidence?,
    onSelect: (Confidence) -> Unit,
    onContinue: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_rate_before_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_rate_before_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            ConfidencePicker(selected = selected, onSelect = onSelect)
            KbPrimaryButton(text = stringResource(R.string.session_continue_after_rate), onClick = onContinue, enabled = selected != null)
        }
    }
}

@Composable
private fun RestudyStepCard(
    remainingMillis: Long,
    onContinue: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_restudy_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_restudy_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            Text(
                text = if (remainingMillis > 0L) {
                    stringResource(R.string.session_time_remaining, formatDuration(remainingMillis))
                } else {
                    stringResource(R.string.session_restudy_done)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = LocalKbColors.current.primary,
            )
            KbPrimaryButton(text = stringResource(R.string.session_continue_after_restudy), onClick = onContinue)
        }
    }
}

@Composable
private fun RateAfterStepCard(
    selected: Confidence?,
    onSelect: (Confidence) -> Unit,
    onFinish: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_rate_after_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_rate_after_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            ConfidencePicker(selected = selected, onSelect = onSelect)
            KbPrimaryButton(text = stringResource(R.string.session_finish), onClick = onFinish, enabled = selected != null)
        }
    }
}

@Composable
private fun ConfidencePicker(selected: Confidence?, onSelect: (Confidence) -> Unit) {
    val blankLabel = stringResource(R.string.confidence_blank)
    val shakyLabel = stringResource(R.string.confidence_shaky)
    val okLabel = stringResource(R.string.confidence_ok)
    val solidLabel = stringResource(R.string.confidence_solid)

    BoxWithConstraints {
        val compact = maxWidth < 340.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                ConfidenceChip(confidence = Confidence.BLANK, label = blankLabel, selected = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
                ConfidenceChip(confidence = Confidence.SHAKY, label = shakyLabel, selected = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
                ConfidenceChip(confidence = Confidence.OK, label = okLabel, selected = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
                ConfidenceChip(confidence = Confidence.SOLID, label = solidLabel, selected = selected, onSelect = onSelect, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(KbSpacing.x1), modifier = Modifier.fillMaxWidth()) {
                    ConfidenceChip(confidence = Confidence.BLANK, label = blankLabel, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f))
                    ConfidenceChip(confidence = Confidence.SHAKY, label = shakyLabel, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(KbSpacing.x1), modifier = Modifier.fillMaxWidth()) {
                    ConfidenceChip(confidence = Confidence.OK, label = okLabel, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f))
                    ConfidenceChip(confidence = Confidence.SOLID, label = solidLabel, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ConfidenceChip(
    confidence: Confidence,
    label: String,
    selected: Confidence?,
    onSelect: (Confidence) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected == confidence,
        onClick = { onSelect(confidence) },
        label = { Text(label) },
        modifier = modifier,
    )
}

private fun sessionRemainingMillis(startedAt: Long, plannedMinutes: Int, nowMillis: Long): Long =
    (startedAt + (plannedMinutes.coerceAtLeast(1) * 60_000L) - nowMillis).coerceAtLeast(0L)

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        String.format("%dm %02ds", minutes, seconds)
    } else {
        String.format("%ds", seconds)
    }
}




