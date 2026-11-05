package dev.kumbuka.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local-only app state: onboarding/consent progress, language, and which
 * scheduler arm is active. DataStore, not Room - this is app config, not
 * revision data. Nothing here ever leaves the device.
 */
class AppPreferences(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val CONSENT_ACCEPTED = booleanPreferencesKey("consent_accepted")
        val LANGUAGE = stringPreferencesKey("language")
        val SESSION_LENGTH_MINUTES = intPreferencesKey("session_length_minutes")
        val SCHEDULER_ARM = stringPreferencesKey("scheduler_arm")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STUDY_PARTICIPATION = booleanPreferencesKey("study_participation")
    }

    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val consentAccepted: Flow<Boolean> =
        dataStore.data.map { it[Keys.CONSENT_ACCEPTED] ?: false }

    val language: Flow<String> =
        dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    val sessionLengthMinutes: Flow<Int> =
        dataStore.data.map { it[Keys.SESSION_LENGTH_MINUTES] ?: 60 }

    /** "baseline" (default, always shown) or "placeholder" - a hidden research setting; never surfaced in ordinary Settings. */
    val schedulerArm: Flow<String> =
        dataStore.data.map { prefs ->
            when (prefs[Keys.SCHEDULER_ARM]) {
                "learned" -> "placeholder" // migrate a pre-rename stored value
                null -> "baseline"
                else -> prefs[Keys.SCHEDULER_ARM]!!
            }
        }

    val reminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }

    val reminderHour: Flow<Int> =
        dataStore.data.map { (it[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23) }

    val reminderMinute: Flow<Int> =
        dataStore.data.map { (it[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59) }

    /** Opt-in to the research study. Off until the student explicitly agrees; changeable any time in Settings. */
    val studyParticipation: Flow<Boolean> =
        dataStore.data.map { it[Keys.STUDY_PARTICIPATION] ?: false }

    /** "light" (the default), "dark", or "system" (follow the device setting). */
    val themeMode: Flow<String> =
        dataStore.data.map { it[Keys.THEME_MODE] ?: "light" }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    suspend fun setConsentAccepted(value: Boolean) {
        dataStore.edit { it[Keys.CONSENT_ACCEPTED] = value }
    }

    /** Saves consent + onboarding completion in a single DataStore transaction, so a
     * partial write (e.g. process death mid-write) can never leave a user stuck in a
     * "half onboarded" state that neither shows onboarding again nor reaches Home cleanly. */
    suspend fun completeOnboarding(participateInStudy: Boolean = false) {
        dataStore.edit {
            it[Keys.CONSENT_ACCEPTED] = true
            it[Keys.ONBOARDING_COMPLETE] = true
            it[Keys.STUDY_PARTICIPATION] = participateInStudy
        }
    }

    suspend fun setStudyParticipation(value: Boolean) {
        dataStore.edit { it[Keys.STUDY_PARTICIPATION] = value }
    }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setSessionLengthMinutes(value: Int) {
        dataStore.edit { it[Keys.SESSION_LENGTH_MINUTES] = value.coerceIn(15, 120) }
    }

    suspend fun setSchedulerArm(value: String) {
        dataStore.edit { it[Keys.SCHEDULER_ARM] = value }
    }

    suspend fun setReminderEnabled(value: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = value }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { it[Keys.THEME_MODE] = value }
    }
}
