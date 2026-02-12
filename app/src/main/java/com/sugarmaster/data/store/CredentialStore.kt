package com.sugarmaster.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sugarmaster_prefs")

class CredentialStore(private val context: Context) {

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_ACCOUNT_ID = stringPreferencesKey("account_id")
        private val KEY_PATIENT_ID = stringPreferencesKey("patient_id")
        private val KEY_REGION = stringPreferencesKey("region")
        private val KEY_VIBRATION_ALERTS = booleanPreferencesKey("vibration_alerts")
    }

    val email: Flow<String> = context.dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    val accountId: Flow<String> = context.dataStore.data.map { it[KEY_ACCOUNT_ID] ?: "" }
    val patientId: Flow<String> = context.dataStore.data.map { it[KEY_PATIENT_ID] ?: "" }
    val region: Flow<String> = context.dataStore.data.map { it[KEY_REGION] ?: "" }

    val vibrationAlerts: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_VIBRATION_ALERTS] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map {
        !it[KEY_TOKEN].isNullOrBlank()
    }

    suspend fun saveCredentials(email: String, password: String, region: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = email
            prefs[KEY_PASSWORD] = password
            prefs[KEY_REGION] = region
        }
    }

    suspend fun saveAuth(token: String, accountId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_ACCOUNT_ID] = accountId
        }
    }

    suspend fun savePatientId(patientId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PATIENT_ID] = patientId
        }
    }

    suspend fun getStoredCredentials(): Triple<String, String, String>? {
        val prefs = context.dataStore.data.first()
        val email = prefs[KEY_EMAIL] ?: ""
        val password = prefs[KEY_PASSWORD] ?: ""
        val region = prefs[KEY_REGION] ?: ""
        return if (email.isNotBlank() && password.isNotBlank()) {
            Triple(email, password, region)
        } else null
    }

    suspend fun setVibrationAlerts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIBRATION_ALERTS] = enabled
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
