package dev.kumbuka.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val consentAccepted: Flow<Boolean> =
        dataStore.data.map { it[Keys.CONSENT_ACCEPTED] ?: false }

    val language: Flow<String> =
        dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    val isGuest: Flow<Boolean> =
        dataStore.data.map { it[Keys.IS_GUEST] ?: false }

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
}
