package dev.kumbuka.app.ui.screens.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Deadline
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.pack.OutlineParser
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTonalButton
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.components.kbContentWidth
import dev.kumbuka.app.ui.theme.LocalKbColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private const val SampleOutline = """ICS 3108 SERVER ADMINISTRATION
Course outline, weeks 1-12 (from class rep)
1. Introduction to server operating systems
2. User & group management
3. File permissions & ACLs
4. DNS & DHCP configuration
5. Web servers: Apache and Nginx
6. Backup & recovery strategies
CAT 1 - 6th Nov (topics 1-3)
CAT 2 - 27th Nov (topics 4-6)
Ref: Lecture notes, Nemeth ch. 6"""

/**
 * Create a unit by pasting a course outline. The parser's result is previewed
 * live (nothing is dropped silently: unmatched lines are listed) and the
 * student can fix the unit code and title before anything is saved.
 */
@Composable
fun PasteOutlineScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    deadlineRepository: DeadlineRepository,
    onBack: () -> Unit,
    onCreated: (unitId: String, unitCode: String) -> Unit,
) {
    val colors = LocalKbColors.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var codeOverride by rememberSaveable { mutableStateOf<String?>(null) }
    var titleOverride by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val parsed = remember(text) { OutlineParser.parse(text) }
    val code = codeOverride ?: parsed.code
    val title = titleOverride ?: parsed.title
    val canCreate = parsed.isUsable && code.isNotBlank() && title.isNotBlank() && !saving
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = colors.surface,
        focusedContainerColor = colors.surface,
        unfocusedBorderColor = colors.outlineStrong,
    )

    Scaffold(
        topBar = { KbTopBar(title = stringResource(R.string.paste_title), onBack = onBack) },
        containerColor = colors.paper,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.paste_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                modifier = Modifier.kbContentWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    codeOverride = null
                    titleOverride = null
                },
                modifier = Modifier.kbContentWidth().heightIn(min = 200.dp),
                placeholder = { Text(stringResource(R.string.paste_placeholder)) },
                shape = MaterialTheme.shapes.large,
                colors = fieldColors,
            )
            if (text.isBlank()) {
                KbTonalButton(
                    text = stringResource(R.string.paste_use_sample),
                    onClick = { text = SampleOutline },
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.kbContentWidth(),
                )
            }
            if (text.isNotBlank()) {
                if (!parsed.isUsable) {
                    KbBanner(stringResource(R.string.paste_nothing_found), KbStatus.WARNING, Modifier.kbContentWidth())
                } else {
                    KbSurface(modifier = Modifier.kbContentWidth()) {
                        Text(
                            pluralStringResource(R.plurals.paste_found_topics, parsed.topics.size, parsed.topics.size) + " · " +
                                pluralStringResource(R.plurals.paste_found_dates, parsed.dates.size, parsed.dates.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.ink,
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { codeOverride = it },
                            label = { Text(stringResource(R.string.paste_code_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { titleOverride = it },
                            label = { Text(stringResource(R.string.paste_title_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                        )
                        parsed.topics.forEachIndexed { index, topic ->
                            Text("${index + 1}. $topic", style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                        }
                    }
                    if (parsed.skipped.isNotEmpty()) {
                        KbBanner(
                            pluralStringResource(R.plurals.paste_skipped, parsed.skipped.size, parsed.skipped.size, parsed.skipped.joinToString(" / ")),
                            KbStatus.INFO,
                            Modifier.kbContentWidth(),
                        )
                    }
                }
            }
            if (error) KbBanner(stringResource(R.string.onboarding_save_failed), KbStatus.ERROR, Modifier.kbContentWidth())
            KbPrimaryButton(
                text = stringResource(R.string.paste_create),
                enabled = canCreate,
                onClick = {
                    scope.launch {
                        saving = true
                        error = false
                        val unitId = runCatching {
                            createUnitFromOutline(parsed, code.trim(), title.trim(), unitRepository, topicRepository, deadlineRepository)
                        }.onFailure { error = true }.getOrNull()
                        saving = false
                        if (unitId != null) onCreated(unitId, code.trim())
                    }
                },
                modifier = Modifier.kbContentWidth(),
            )
        }
    }
}

private suspend fun createUnitFromOutline(
    parsed: dev.kumbuka.app.pack.ParsedOutline,
    code: String,
    title: String,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    deadlineRepository: DeadlineRepository,
): String {
    val now = System.currentTimeMillis()
    val unitId = "unit-" + UUID.randomUUID()
    unitRepository.upsert(UnitModel(unitId, code, title, packId = null, packVersion = null, createdAt = now, updatedAt = now))
    val weight = 1f / parsed.topics.size
    val topics = parsed.topics.mapIndexed { index, topicTitle ->
        Topic(
            id = "topic-" + UUID.randomUUID(),
            unitId = unitId,
            title = topicTitle,
            objective = "",
            retrievalPrompt = "",
            examWeight = weight,
            orderIndex = index + 1,
            resourcePointers = parsed.references,
            titleEditedLocally = false,
            objectiveEditedLocally = false,
            weightEditedLocally = false,
            createdAt = now,
            updatedAt = now,
        )
    }
    topicRepository.upsertAll(topics)
    val today = LocalDate.now()
    parsed.dates.forEach { outlineDate ->
        val linked = outlineDate.topicRange
            ?.mapNotNull { number -> topics.getOrNull(number - 1)?.id }
            ?: emptyList()
        deadlineRepository.upsert(
            Deadline(
                id = "deadline-" + UUID.randomUUID(),
                unitId = unitId,
                title = outlineDate.title,
                date = outlineDate.resolve(today).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                kind = outlineDate.kind,
                topicIds = linked,
                updatedAt = now,
            ),
        )
    }
    return unitId
}
