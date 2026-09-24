package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import dev.kumbuka.app.ui.components.KbNavTab

enum class AssessmentSegment { UPCOMING, RESULTS }

/**
 * Cross-destination workspace state, hoisted above the nav graph so secondary
 * screens (unit detail, mark editor) can hand the user back to a specific tab,
 * segment and unit filter, and leave a one-shot confirmation message.
 */
@Stable
class WorkspaceShellState(
    tab: KbNavTab = KbNavTab.HOME,
    segment: AssessmentSegment = AssessmentSegment.UPCOMING,
    unitFilter: String? = null,
) {
    var tab by mutableStateOf(tab)
    var segment by mutableStateOf(segment)
    var unitFilter by mutableStateOf(unitFilter)

    /** One-shot snackbar text shown by the shell, then cleared. Not persisted. */
    var pendingMessage by mutableStateOf<String?>(null)

    fun showAssessments(segment: AssessmentSegment, unitId: String? = null) {
        this.segment = segment
        this.unitFilter = unitId
        this.tab = KbNavTab.ASSESSMENTS
    }

    companion object {
        val Saver: Saver<WorkspaceShellState, Any> = Saver(
            save = { listOf(it.tab.name, it.segment.name, it.unitFilter.orEmpty()) },
            restore = { raw ->
                @Suppress("UNCHECKED_CAST")
                val parts = raw as List<String>
                WorkspaceShellState(
                    tab = runCatching { KbNavTab.valueOf(parts[0]) }.getOrDefault(KbNavTab.HOME),
                    segment = runCatching { AssessmentSegment.valueOf(parts[1]) }.getOrDefault(AssessmentSegment.UPCOMING),
                    unitFilter = parts[2].ifBlank { null },
                )
            },
        )
    }
}

@Composable
fun rememberWorkspaceShellState(): WorkspaceShellState =
    rememberSaveable(saver = WorkspaceShellState.Saver) { WorkspaceShellState() }
