@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.packs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.repository.PackImportPreview
import dev.kumbuka.app.data.repository.PackRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository
import dev.kumbuka.app.domain.model.Topic
import dev.kumbuka.app.domain.model.Unit as UnitModel
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.pack.CoursePackCodec
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.theme.KbColors
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UnitsListScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    onBack: () -> Unit,
    onImportPack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onExportUnit: (String) -> Unit,
) {
    val units by unitRepository.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.units_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
                actions = {
                    IconButton(onClick = onImportPack) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = stringResource(R.string.import_pack_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = SnackbarHostState()) },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        if (units.isEmpty()) {
            EmptyUnitsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onImportPack = onImportPack,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.units_list_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KbColors.inkMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(units, key = { it.id }) { unit ->
                    UnitCard(
                        unit = unit,
                        topicRepository = topicRepository,
                        onOpenTopic = onOpenTopic,
                        onExportUnit = onExportUnit,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyUnitsState(modifier: Modifier = Modifier, onImportPack: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(KbColors.primaryTint, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, tint = KbColors.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.units_empty_title), style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.units_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = KbColors.inkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        KbPrimaryButton(text = stringResource(R.string.import_pack_cta), onClick = onImportPack)
    }
}

@Composable
private fun UnitCard(
    unit: UnitModel,
    topicRepository: TopicRepository,
    onOpenTopic: (String) -> Unit,
    onExportUnit: (String) -> Unit,
) {
    val topics by topicRepository.observeByUnit(unit.id).collectAsState(initial = emptyList())
    Card(
        colors = CardDefaults.cardColors(containerColor = KbColors.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(unit.code, style = MaterialTheme.typography.labelLarge, color = KbColors.primary)
                Text(unit.title, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
                Text(
                    text = stringResource(
                        R.string.unit_pack_version,
                        unit.packVersion ?: 1,
                        unit.packId ?: stringResource(R.string.unit_pack_id_missing),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = KbColors.inkMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KbSecondaryButton(
                    text = stringResource(R.string.unit_export_pack),
                    onClick = { onExportUnit(unit.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (topics.isEmpty()) {
                Text(
                    text = stringResource(R.string.unit_no_topics),
                    style = MaterialTheme.typography.bodySmall,
                    color = KbColors.inkMuted,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topics.take(4).forEach { topic ->
                        TopicRow(topic = topic, onClick = { onOpenTopic(topic.id) })
                    }
                    if (topics.size > 4) {
                        Text(
                            text = stringResource(R.string.unit_more_topics, topics.size - 4),
                            style = MaterialTheme.typography.bodySmall,
                            color = KbColors.inkFaint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KbColors.paper2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(topic.title, style = MaterialTheme.typography.bodyMedium, color = KbColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = topic.objective,
                style = MaterialTheme.typography.bodySmall,
                color = KbColors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.topic_detail_cta),
            style = MaterialTheme.typography.labelMedium,
            color = KbColors.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun TopicDetailScreen(
    topicRepository: TopicRepository,
    unitRepository: UnitRepository,
    topicId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var topic by remember(topicId) { mutableStateOf<Topic?>(null) }
    var unit by remember(topicId) { mutableStateOf<UnitModel?>(null) }
    var objectiveDraft by remember(topicId) { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(topicId) {
        topic = topicRepository.getById(topicId)
        unit = topic?.let { unitRepository.getById(it.unitId) }
        objectiveDraft = topic?.objective.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.topic_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        val current = topic
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.topic_not_found), color = KbColors.inkMuted)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(unit?.code ?: stringResource(R.string.topic_unit_unknown), style = MaterialTheme.typography.labelLarge, color = KbColors.primary)
                    Text(current.title, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
                    Text(
                        text = stringResource(
                            R.string.topic_weight_label,
                            current.examWeight,
                            if (current.objectiveEditedLocally) stringResource(R.string.topic_edited_by_you) else stringResource(R.string.topic_from_pack),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = KbColors.inkMuted,
                    )
                }
            }

            Text(stringResource(R.string.topic_objective_label), style = MaterialTheme.typography.labelLarge, color = KbColors.inkMuted)
            OutlinedTextField(
                value = objectiveDraft,
                onValueChange = { objectiveDraft = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(14.dp),
                label = { Text(stringResource(R.string.topic_objective_label)) },
            )

            Text(
                text = stringResource(R.string.topic_retrieval_prompt_label),
                style = MaterialTheme.typography.labelLarge,
                color = KbColors.inkMuted,
            )
            Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(14.dp)) {
                Text(
                    text = current.retrievalPrompt.ifBlank { stringResource(R.string.topic_retrieval_prompt_empty) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = KbColors.ink,
                    modifier = Modifier.padding(14.dp),
                )
            }

            if (current.resourcePointers.isNotEmpty()) {
                Text(stringResource(R.string.topic_resources_label), style = MaterialTheme.typography.labelLarge, color = KbColors.inkMuted)
                current.resourcePointers.forEach { pointer ->
                    Text(
                        text = "• $pointer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KbColors.ink,
                    )
                }
            }

            KbPrimaryButton(
                text = stringResource(R.string.topic_save_objective),
                onClick = {
                    if (objectiveDraft != current.objective) {
                        scope.launch {
                            topicRepository.upsert(
                                current.copy(
                                    objective = objectiveDraft,
                                    objectiveEditedLocally = true,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                            topic = topicRepository.getById(topicId)
                            saveMessage = null
                        }
                    }
                },
            )
            saveMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = KbColors.primary)
            }
        }
    }
}

@Composable
fun ImportPackScreen(
    packRepository: PackRepository,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var rawInput by remember { mutableStateOf("") }
    var parsedPack by remember { mutableStateOf<CoursePack?>(null) }
    var preview by remember { mutableStateOf<PackImportPreview?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                rawInput = withContext(Dispatchers.IO) { readTextFromUri(context, uri) }
                parseError = null
                parsedPack = null
                preview = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_pack_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
                actions = {
                    IconButton(onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(R.string.import_pick_file))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.import_pack_step_0),
                    style = MaterialTheme.typography.titleMedium,
                    color = KbColors.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.import_pack_step_0_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KbColors.inkMuted,
                )
            }
            item {
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = {
                        rawInput = it
                        parseError = null
                        parsedPack = null
                        preview = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 10,
                    maxLines = 16,
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(stringResource(R.string.import_paste_label)) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    KbSecondaryButton(
                        text = stringResource(R.string.import_pick_file),
                        onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                    )
                    KbPrimaryButton(
                        text = stringResource(R.string.import_parse),
                        onClick = {
                            scope.launch {
                                try {
                                    busy = true
                                    val pack = packRepository.parse(rawInput)
                                    val packPreview = packRepository.previewImport(pack)
                                    parsedPack = pack
                                    preview = packPreview
                                    parseError = null
                                } catch (t: Throwable) {
                                    parseError = t.message ?: context.getString(R.string.import_parse_failed)
                                    parsedPack = null
                                    preview = null
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = rawInput.isNotBlank() && !busy,
                    )
                }
            }
            parseError?.let {
                item {
                    InfoBanner(text = it, accent = KbColors.accent)
                }
            }
            if (parsedPack != null && preview != null) {
                item {
                    Text(
                        text = stringResource(R.string.import_pack_step_1),
                        style = MaterialTheme.typography.titleMedium,
                        color = KbColors.ink,
                    )
                }
                item {
                    ImportPreviewCard(preview = preview!!, pack = parsedPack!!)
                }
                item {
                    KbPrimaryButton(
                        text = stringResource(R.string.import_apply),
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val result = packRepository.importPack(parsedPack!!)
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.import_complete_message,
                                            result.topicsToAdd,
                                            result.topicsToUpdate,
                                            result.topicsWithConflicts,
                                        ),
                                    )
                                    onImported()
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(preview: PackImportPreview, pack: CoursePack) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(pack.unit.title, style = MaterialTheme.typography.titleLarge, color = KbColors.ink)
            Text(
                text = stringResource(R.string.import_pack_metadata, preview.packId, preview.packVersion),
                style = MaterialTheme.typography.bodySmall,
                color = KbColors.inkMuted,
            )
            CountRow(label = stringResource(R.string.import_added_topics), value = preview.topicsToAdd)
            CountRow(label = stringResource(R.string.import_changed_topics), value = preview.topicsToUpdate)
            CountRow(label = stringResource(R.string.import_conflicted_topics), value = preview.topicsWithConflicts)
            CountRow(label = stringResource(R.string.import_removed_topics), value = preview.topicsMissingInPack)
            CountRow(label = stringResource(R.string.import_added_deadlines), value = preview.deadlinesToAdd)
            CountRow(label = stringResource(R.string.import_changed_deadlines), value = preview.deadlinesToUpdate)
            CountRow(label = stringResource(R.string.import_removed_deadlines), value = preview.deadlinesMissingInPack)
            Text(
                text = stringResource(R.string.import_preview_note),
                style = MaterialTheme.typography.bodySmall,
                color = KbColors.inkFaint,
            )
        }
    }
}

@Composable
private fun CountRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, color = KbColors.ink)
    }
}

@Composable
private fun InfoBanner(text: String, accent: androidx.compose.ui.graphics.Color) {
    Card(colors = CardDefaults.cardColors(containerColor = KbColors.warningTint), shape = RoundedCornerShape(14.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
fun ExportPackScreen(
    packRepository: PackRepository,
    unitId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val exportState by produceState<Result<String>?>(initialValue = null, unitId) {
        value = runCatching {
            val pack = packRepository.exportUnit(unitId)
            packRepository.encode(pack)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_pack_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
            )
        },
        containerColor = KbColors.paper,
    ) { innerPadding ->
        val result = exportState
        when {
            result == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.export_loading), color = KbColors.inkMuted)
            }
            result.isFailure -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(result.exceptionOrNull()?.message ?: stringResource(R.string.export_failed), color = KbColors.accent)
            }
            else -> {
                val raw = result.getOrNull().orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.export_body), style = MaterialTheme.typography.bodyMedium, color = KbColors.inkMuted)
                    OutlinedTextField(
                        value = raw,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                        shape = RoundedCornerShape(14.dp),
                        readOnly = true,
                        label = { Text(stringResource(R.string.export_json_label)) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KbSecondaryButton(
                            text = stringResource(R.string.export_copy),
                            onClick = {
                                clipboard.setText(AnnotatedString(raw))
                                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                                    ClipData.newPlainText(context.getString(R.string.export_json_label), raw),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).readText()
        } ?: error("Unable to open file")
    }
}



