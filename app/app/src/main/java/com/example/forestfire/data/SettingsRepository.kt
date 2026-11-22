package com.example.forestfire.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.forestfire.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore per process: the delegate must be top-level and used on the application context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists the settings screen values and whether the firefighter is on duty. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            zone = prefs[ZONE] ?: Settings.DEFAULT.zone,
            firefighterId = prefs[FIREFIGHTER_ID] ?: Settings.DEFAULT.firefighterId,
            brokerHost = prefs[BROKER_HOST] ?: Settings.DEFAULT.brokerHost,
            brokerPort = prefs[BROKER_PORT] ?: Settings.DEFAULT.brokerPort,
        )
    }

    /** Survives process death so DutyService can tell a system restart from an explicit "off duty". */
    val onDuty: Flow<Boolean> = dataStore.data.map { it[ON_DUTY] ?: false }

    suspend fun save(settings: Settings) {
        dataStore.edit { prefs ->
            prefs[ZONE] = settings.zone
            prefs[FIREFIGHTER_ID] = settings.firefighterId
            prefs[BROKER_HOST] = settings.brokerHost
            prefs[BROKER_PORT] = settings.brokerPort
        }
    }

    suspend fun setOnDuty(onDuty: Boolean) {
        dataStore.edit { it[ON_DUTY] = onDuty }
    }

    private companion object {
        val ZONE = stringPreferencesKey("zone")
        val FIREFIGHTER_ID = stringPreferencesKey("firefighter_id")
        val BROKER_HOST = stringPreferencesKey("broker_host")
        val BROKER_PORT = intPreferencesKey("broker_port")
        val ON_DUTY = booleanPreferencesKey("on_duty")
    }
}
