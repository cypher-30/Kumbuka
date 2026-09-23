package dev.kumbuka.app.ui.screens.home

import androidx.compose.runtime.Composable
import dev.kumbuka.app.data.prefs.AppPreferences
import dev.kumbuka.app.data.repository.AssessmentMarkRepository
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
    preferences: AppPreferences,
    unitRepository: UnitRepository,
    topicRepository: TopicRepository,
    sessionRepository: SessionRepository,
    deadlineRepository: DeadlineRepository,
    assessmentMarkRepository: AssessmentMarkRepository,
    onBrowseUnits: () -> Unit,
    onImportPack: () -> Unit,
    onStartSession: (String, Int) -> Unit,
) {
    TodayScreen(
        preferences,
        unitRepository,
        topicRepository,
        sessionRepository,
        deadlineRepository,
        assessmentMarkRepository,
        onBrowseUnits,
        onImportPack,
        onStartSession,
    )
}
