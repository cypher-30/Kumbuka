@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.kumbuka.app.ui.screens.packs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.kumbuka.app.R
import dev.kumbuka.app.data.bootstrap.DefaultContentSeeder
import dev.kumbuka.app.data.repository.PackChangeKind
import dev.kumbuka.app.data.repository.PackDiff
import dev.kumbuka.app.data.repository.PackImportResolution
import dev.kumbuka.app.data.repository.PackImportPreview
import dev.kumbuka.app.data.repository.PackRepository
import dev.kumbuka.app.data.repository.TopicConflictChoice
import dev.kumbuka.app.pack.CoursePack
import dev.kumbuka.app.ui.components.KbBanner
import dev.kumbuka.app.ui.components.KbPrimaryButton
import dev.kumbuka.app.ui.components.KbSecondaryButton
import dev.kumbuka.app.ui.components.KbStatus
import dev.kumbuka.app.ui.components.KbSurface
import dev.kumbuka.app.ui.components.KbTopBar
import dev.kumbuka.app.ui.theme.LocalKbColors
import dev.kumbuka.app.ui.theme.KbSpacing
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// .coursepack has no registered MIME type, so providers report it as octet-stream (or nothing).
// Non-pack files are rejected by the parser with a readable error.
private val CoursePackMimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")

@Composable
fun ImportPackScreen(
    packRepository: PackRepository,
    pendingImportUri: String?,
    onPendingImportUriHandled: (String) -> Unit,
    onBack: () -> Unit,
    onImported: (message: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var rawInput by remember { mutableStateOf("") }
    var showPasteInput by rememberSaveable { mutableStateOf(false) }
    var parsedPack by remember { mutableStateOf<CoursePack?>(null) }
    var diff by remember { mutableStateOf<PackDiff?>(null) }
    var resolution by remember { mutableStateOf(PackImportResolution()) }
    var showDiff by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var queuedPendingImportUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetReviewState() {
        parsedPack = null
        diff = null
        resolution = PackImportResolution()
        showDiff = false
    }

    fun defaultResolutionFor(packDiff: PackDiff): PackImportResolution = PackImportResolution(
        topicConflictChoices = packDiff.topicDiffs
            .filter { it.kind == PackChangeKind.CONFLICTED }
            .associate { it.topicId to TopicConflictChoice.KEEP_LOCAL },
    )

    fun needsReview(packDiff: PackDiff): Boolean {
        val preview = packDiff.preview
        return packDiff.existingUnitId != null &&
            (
                preview.topicsWithConflicts > 0 ||
                    preview.topicsMissingInPack > 0 ||
                    preview.deadlinesMissingInPack > 0
                )
    }

    fun importSuccessMessage(preview: PackImportPreview): String {
        val hasChanges = preview.topicsToAdd > 0 ||
            preview.topicsToUpdate > 0 ||
            preview.deadlinesToAdd > 0 ||
            preview.deadlinesToUpdate > 0 ||
            preview.topicsMissingInPack > 0 ||
            preview.deadlinesMissingInPack > 0
        return if (hasChanges) {
            context.getString(
                R.string.import_complete_message,
                preview.topicsToAdd,
                preview.topicsToUpdate,
                preview.topicsWithConflicts,
            )
        } else {
            context.getString(R.string.import_no_changes_message)
        }
    }

    suspend fun applyPack(pack: CoursePack, resolution: PackImportResolution) {
        val result = packRepository.importPack(pack, resolution)
        onImported(importSuccessMessage(result))
    }

    suspend fun parseAndRoute(raw: String) {
        val pack = packRepository.parse(raw)
        val packDiff = packRepository.buildDiff(pack)
        val defaultResolution = defaultResolutionFor(packDiff)
        rawInput = raw
        parsedPack = pack
        diff = packDiff
        resolution = defaultResolution
        parseError = null
        if (needsReview(packDiff)) {
            showDiff = true
        } else {
            applyPack(pack, defaultResolution)
        }
    }

    suspend fun importFromUri(uri: Uri) {
        persistReadPermissionIfPossible(context, uri)
        val raw = readTextFromUri(context, uri)
        showPasteInput = false
        parseAndRoute(raw)
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        scope.launch {
            if (uri == null) {
                parseError = context.getString(R.string.import_pick_cancelled)
                snackbarHostState.showSnackbar(parseError!!)
                return@launch
            }
            busy = true
            try {
                importFromUri(uri)
            } catch (error: SecurityException) {
                resetReviewState()
                parseError = context.getString(R.string.import_file_permission_denied)
            } catch (error: IOException) {
                resetReviewState()
                parseError = context.getString(R.string.import_file_read_failed)
            } catch (error: Throwable) {
                resetReviewState()
                parseError = error.message ?: context.getString(R.string.import_parse_failed)
            } finally {
                busy = false
            }
        }
    }

    BackHandler(enabled = showDiff && !busy) {
        showDiff = false
    }

    LaunchedEffect(pendingImportUri) {
        val uriString = pendingImportUri ?: return@LaunchedEffect
        queuedPendingImportUri = uriString
        onPendingImportUriHandled(uriString)
    }

    LaunchedEffect(queuedPendingImportUri, busy) {
        if (busy) return@LaunchedEffect
        val uriString = queuedPendingImportUri ?: return@LaunchedEffect
        queuedPendingImportUri = null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
        if (uri == null) {
            parseError = context.getString(R.string.import_file_read_failed)
            return@LaunchedEffect
        }
        busy = true
        try {
            importFromUri(uri)
        } catch (error: SecurityException) {
            resetReviewState()
            parseError = context.getString(R.string.import_file_permission_denied)
        } catch (error: IOException) {
            resetReviewState()
            parseError = context.getString(R.string.import_file_read_failed)
        } catch (error: Throwable) {
            resetReviewState()
            parseError = error.message ?: context.getString(R.string.import_parse_failed)
        } finally {
            busy = false
        }
    }

    Scaffold(
        topBar = {
            KbTopBar(
                title = stringResource(if (showDiff) R.string.diff_title else R.string.import_pack_title),
                onBack = { if (!busy) { if (showDiff) showDiff = false else onBack() } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        if (showDiff && parsedPack != null && diff != null) {
            PackDiffScreen(
                diff = diff!!,
                resolution = resolution,
                onConflictChoice = { topicId, choice ->
                    resolution = resolution.copy(
                        topicConflictChoices = resolution.topicConflictChoices.toMutableMap().apply {
                            this[topicId] = choice
                        },
                    )
                },
                onTopicRemovalChoice = { topicId, choice ->
                    resolution = resolution.copy(
                        topicRemovalChoices = resolution.topicRemovalChoices.toMutableMap().apply {
                            this[topicId] = choice
                        },
                    )
                },
                onDeadlineRemovalChoice = { deadlineId, choice ->
                    resolution = resolution.copy(
                        deadlineRemovalChoices = resolution.deadlineRemovalChoices.toMutableMap().apply {
                            this[deadlineId] = choice
                        },
                    )
                },
                onBack = { if (!busy) showDiff = false },
                onApply = {
                    scope.launch {
                        busy = true
                        try {
                            applyPack(parsedPack!!, resolution)
                        } catch (error: Throwable) {
                            parseError = error.message ?: context.getString(R.string.import_apply_failed)
                            snackbarHostState.showSnackbar(parseError!!)
                        } finally {
                            busy = false
                        }
                    }
                },
                busy = busy,
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(KbSpacing.x2),
            verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
        ) {
            item {
                Text(
                    text = stringResource(R.string.import_pack_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalKbColors.current.inkMuted,
                )
            }
            item {
                KbPrimaryButton(
                    icon = Icons.Outlined.FolderOpen,
                    text = stringResource(R.string.import_choose_file),
                    onClick = {
                        parseError = null
                        openDocument.launch(CoursePackMimeTypes)
                    },
                    enabled = !busy,
                )
            }
            item {
                TextButton(
                    onClick = {
                        showPasteInput = !showPasteInput
                        parseError = null
                        if (!showPasteInput) {
                            resetReviewState()
                        }
                    },
                    enabled = !busy,
                ) {
                    Text(
                        text = stringResource(
                            if (showPasteInput) R.string.import_hide_paste else R.string.import_show_paste,
                        ),
                    )
                }
            }
            parseError?.let {
                item {
                    KbBanner(text = it, status = KbStatus.ERROR)
                }
            }
            if (showPasteInput) {
                item {
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = {
                            rawInput = it
                            parseError = null
                            resetReviewState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 10,
                        maxLines = 16,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(R.string.import_paste_label)) },
                    )
                }
                item {
                    KbPrimaryButton(
                        text = stringResource(R.string.import_parse),
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    parseAndRoute(rawInput)
                                } catch (error: Throwable) {
                                    resetReviewState()
                                    parseError = error.message ?: context.getString(R.string.import_parse_failed)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = rawInput.isNotBlank() && !busy,
                    )
                }
            }
            item {
                KbSurface(modifier = Modifier.padding(top = KbSpacing.x2)) {
                    run {
                        Text(stringResource(R.string.import_try_sample_title), style = MaterialTheme.typography.titleSmall, color = LocalKbColors.current.ink)
                        Text(stringResource(R.string.import_try_sample_body), style = MaterialTheme.typography.bodySmall, color = LocalKbColors.current.inkMuted)
                        KbSecondaryButton(
                            text = stringResource(R.string.import_try_sample_cta),
                            onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        DefaultContentSeeder.seedSamplePacks(packRepository)
                                        onImported(context.getString(R.string.import_try_sample_done))
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
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
            KbTopBar(title = stringResource(R.string.export_pack_title), onBack = onBack)
        },
        containerColor = LocalKbColors.current.paper,
    ) { innerPadding ->
        val result = exportState
        when {
            result == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.export_loading), color = LocalKbColors.current.inkMuted)
            }
            result.isFailure -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                KbBanner(text = result.exceptionOrNull()?.message ?: stringResource(R.string.export_failed), status = KbStatus.ERROR, modifier = Modifier.padding(KbSpacing.x2))
            }
            else -> {
                val raw = result.getOrNull().orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(KbSpacing.x2)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(KbSpacing.x1),
                ) {
                    Text(stringResource(R.string.export_body), style = MaterialTheme.typography.bodyMedium, color = LocalKbColors.current.inkMuted)
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
        } ?: throw IOException("Unable to open file")
    }
}

private fun persistReadPermissionIfPossible(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
