package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.Composable
import dev.kumbuka.app.data.repository.DeadlineRepository
import dev.kumbuka.app.data.repository.SessionRepository
import dev.kumbuka.app.data.repository.TopicRepository
import dev.kumbuka.app.data.repository.UnitRepository

/**
 * Kept as the navigation entry point; Phase 4 turns the old placeholder into
 * the real Today screen while preserving the existing route name.
 */
@Composable
fun HomePlaceholderScreen(
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
) {
    TodayScreen(
        unitRepository = unitRepository,
        topicRepository = topicRepository,
        sessionRepository = sessionRepository,
        deadlineRepository = deadlineRepository,
        onBrowseUnits = onBrowseUnits,
        onImportPack = onImportPack,
    )
}
