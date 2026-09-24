@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    var savingWrite by remember { mutableStateOf(false) }
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
        when {
            loadedTopic == null -> {
                loadError = context.getString(R.string.session_not_found)
                loading = false
            }
            activeSession == null && loadedTopic.archived -> {
                // A brand-new session must not be started against an archived topic; an
                // already-active session on a topic archived mid-session is preserved below.
                loadError = context.getString(R.string.session_topic_archived)
                topic = loadedTopic
                unit = loadedUnit
                loading = false
            }
            else -> {
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
                loadError = null
                loading = false
            }
        }
    }

    LaunchedEffect(currentSession?.id, currentSession?.endedAt) {
        while (currentSession != null && currentSession.endedAt == null) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            dev.kumbuka.app.ui.components.KbTopBar(title = stringResource(R.string.session_title), onBack = onBack)
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
                                stage = stage,
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
                                        onRateRecall = { stageName = SessionStage.RateBefore.name },
                                        onDefer = { showDeferSheet = true },
                                    )
                                    SessionStage.RateBefore -> RateBeforeStepCard(
                                        selected = beforeSelectionName?.let(Confidence::valueOf),
                                        saving = savingWrite,
                                        onSelect = { beforeSelectionName = it.name },
                                        onContinue = {
                                            val selection = beforeSelectionName?.let(Confidence::valueOf)
                                            if (selection != null && !savingWrite) {
                                                val updated = currentSession.copy(
                                                    confidenceBefore = selection,
                                                    updatedAt = System.currentTimeMillis(),
                                                )
                                                savingWrite = true
                                                scope.launch {
                                                    sessionRepository.upsert(updated)
                                                    session = updated
                                                    stageName = SessionStage.Restudy.name
                                                    savingWrite = false
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
                                        saving = savingWrite,
                                        onSelect = { afterSelectionName = it.name },
                                        onFinish = {
                                            val selection = afterSelectionName?.let(Confidence::valueOf)
                                            if (selection != null && !savingWrite) {
                                                val now = System.currentTimeMillis()
                                                val updated = currentSession.copy(
                                                    confidenceAfter = selection,
                                                    endedAt = now,
                                                    actualSeconds = ((now - currentSession.startedAt) / 1000L).toInt().coerceAtLeast(0),
                                                    updatedAt = now,
                                                )
                                                savingWrite = true
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
                if (savingWrite) return@DeferSessionSheet
                val now = System.currentTimeMillis()
                val updated = currentSession.copy(
                    endedAt = now,
                    actualSeconds = ((now - currentSession.startedAt) / 1000L).toInt().coerceAtLeast(0),
                    wasDeferred = true,
                    updatedAt = now,
                )
                savingWrite = true
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
    stage: SessionStage?,
) {
    val colors = LocalKbColors.current
    val totalMillis = (plannedMinutes.coerceAtLeast(1) * 60_000L)
    val progress = if (session.endedAt != null) 1f else ((totalMillis - remainingMillis).toFloat() / totalMillis).coerceIn(0f, 1f)
    val stages = listOf(
        SessionStage.Recall to stringResource(R.string.session_stage_recall),
        SessionStage.RateBefore to stringResource(R.string.session_stage_rate),
        SessionStage.Restudy to stringResource(R.string.session_stage_restudy),
        SessionStage.RateAfter to stringResource(R.string.session_stage_rate_again),
    )
    val currentIndex = stages.indexOfFirst { it.first == stage }.let { if (it < 0) stages.size else it }
    Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x2)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            dev.kumbuka.app.ui.components.KbUnitLabel(
                unitKey = topic.unitId,
                unitCode = unit?.code ?: stringResource(R.string.session_unit_unknown),
                emphasized = true,
            )
            Text(topic.title, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.primaryTint), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.fillMaxWidth().padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
                Text(
                    formatDuration(remainingMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onPrimaryTint,
                )
                Text(
                    stringResource(R.string.session_timer_caption, plannedMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryTint,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colors.primary,
                    trackColor = colors.surface,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            stages.forEachIndexed { index, (_, label) ->
                val done = index < currentIndex
                val active = index == currentIndex
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                when {
                                    active -> colors.primary
                                    done -> colors.primary.copy(alpha = 0.45f)
                                    else -> colors.border
                                },
                                MaterialTheme.shapes.small,
                            ),
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) colors.ink else colors.inkMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecallStepCard(
    topic: Topic,
    onRateRecall: () -> Unit,
    onDefer: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_recall_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_recall_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            Text(
                text = topic.retrievalPrompt.ifBlank { topic.objective },
                style = MaterialTheme.typography.titleMedium,
                color = LocalKbColors.current.onPrimaryTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(LocalKbColors.current.primaryTint, MaterialTheme.shapes.medium)
                    .padding(KbSpacing.x2),
            )
            KbPrimaryButton(text = stringResource(R.string.session_done_recalling), onClick = onRateRecall)
            KbSecondaryButton(text = stringResource(R.string.session_defer), onClick = onDefer)
        }
    }
}

@Composable
private fun RateBeforeStepCard(
    selected: Confidence?,
    saving: Boolean,
    onSelect: (Confidence) -> Unit,
    onContinue: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_rate_before_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_rate_before_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            ConfidencePicker(selected = selected, enabled = !saving, onSelect = onSelect)
            KbPrimaryButton(text = stringResource(R.string.session_continue_after_rate), onClick = onContinue, enabled = selected != null && !saving)
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
            if (remainingMillis <= 0L) {
                dev.kumbuka.app.ui.components.KbBanner(stringResource(R.string.session_restudy_done), dev.kumbuka.app.ui.components.KbStatus.INFO)
            }
            KbPrimaryButton(text = stringResource(R.string.session_continue_after_restudy), onClick = onContinue)
        }
    }
}

@Composable
private fun RateAfterStepCard(
    selected: Confidence?,
    saving: Boolean,
    onSelect: (Confidence) -> Unit,
    onFinish: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalKbColors.current.surface), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(KbSpacing.x2), verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
            Text(stringResource(R.string.session_rate_after_title), style = MaterialTheme.typography.titleMedium, color = LocalKbColors.current.ink)
            Text(stringResource(R.string.session_rate_after_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
            ConfidencePicker(selected = selected, enabled = !saving, onSelect = onSelect)
            KbPrimaryButton(text = stringResource(R.string.session_finish), onClick = onFinish, enabled = selected != null && !saving)
        }
    }
}

/** Four large, accessible, single-select choices (min 56dp tall) - not small FilterChips. */
@Composable
private fun ConfidencePicker(selected: Confidence?, enabled: Boolean, onSelect: (Confidence) -> Unit) {
    val blankLabel = stringResource(R.string.confidence_blank)
    val shakyLabel = stringResource(R.string.confidence_shaky)
    val okLabel = stringResource(R.string.confidence_ok)
    val solidLabel = stringResource(R.string.confidence_solid)

    Column(verticalArrangement = Arrangement.spacedBy(KbSpacing.x1)) {
        ConfidenceChoice(confidence = Confidence.BLANK, label = blankLabel, selected = selected == Confidence.BLANK, enabled = enabled, onSelect = onSelect)
        ConfidenceChoice(confidence = Confidence.SHAKY, label = shakyLabel, selected = selected == Confidence.SHAKY, enabled = enabled, onSelect = onSelect)
        ConfidenceChoice(confidence = Confidence.OK, label = okLabel, selected = selected == Confidence.OK, enabled = enabled, onSelect = onSelect)
        ConfidenceChoice(confidence = Confidence.SOLID, label = solidLabel, selected = selected == Confidence.SOLID, enabled = enabled, onSelect = onSelect)
    }
}

@Composable
private fun ConfidenceChoice(
    confidence: Confidence,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (Confidence) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) LocalKbColors.current.primary.copy(alpha = 0.12f) else LocalKbColors.current.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) LocalKbColors.current.primary else LocalKbColors.current.border,
                shape = MaterialTheme.shapes.medium,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onSelect(confidence) },
            )
            .padding(horizontal = KbSpacing.x2, vertical = KbSpacing.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) LocalKbColors.current.primary else LocalKbColors.current.ink,
        )
        if (selected) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = LocalKbColors.current.primary)
        }
    }
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




