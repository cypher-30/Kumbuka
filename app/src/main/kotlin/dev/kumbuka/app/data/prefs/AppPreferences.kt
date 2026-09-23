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
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val SESSION_LENGTH_MINUTES = intPreferencesKey("session_length_minutes")
        val SCHEDULER_ARM = stringPreferencesKey("scheduler_arm")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val consentAccepted: Flow<Boolean> =
        dataStore.data.map { it[Keys.CONSENT_ACCEPTED] ?: false }

    val language: Flow<String> =
        dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    val isGuest: Flow<Boolean> =
        dataStore.data.map { it[Keys.IS_GUEST] ?: false }

    val sessionLengthMinutes: Flow<Int> =
        dataStore.data.map { it[Keys.SESSION_LENGTH_MINUTES] ?: 60 }

    val schedulerArm: Flow<String> =
        dataStore.data.map { it[Keys.SCHEDULER_ARM] ?: "baseline" }

    val reminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }

    val reminderHour: Flow<Int> =
        dataStore.data.map { (it[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23) }

    val reminderMinute: Flow<Int> =
        dataStore.data.map { (it[Keys.REMINDER_MINUTE] ?: 0).coerceIn(0, 59) }

    /** "system" (follow device setting), "light", or "dark". */
    val themeMode: Flow<String> =
        dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    suspend fun setConsentAccepted(value: Boolean) {
        dataStore.edit { it[Keys.CONSENT_ACCEPTED] = value }
    }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setGuest(value: Boolean) {
        dataStore.edit { it[Keys.IS_GUEST] = value }
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
