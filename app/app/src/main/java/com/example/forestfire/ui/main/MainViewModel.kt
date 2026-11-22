package com.example.forestfire.ui.main

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.DutyState
import com.example.forestfire.data.LocationSource
import com.example.forestfire.data.SettingsRepository
import com.example.forestfire.data.isFresh
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.nav.Bearing
import com.example.forestfire.service.DutyService
import com.example.forestfire.ui.format.bearingLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val onDuty: StateFlow<Boolean> = DutyState.onDuty
    val connection: StateFlow<ConnectionState> = DutyState.connection
    val currentAlert: StateFlow<FireAlert?> = DutyState.currentAlert

    /** Null until the saved settings have loaded, so the screen never shows an empty zone. */
    val zone: StateFlow<String?> = SettingsRepository(application).settings
        .map<_, String?> { it.zone }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "1.3 km · NE" while an alarm is active and a fresh GPS fix exists; null otherwise. Location is only requested during an alarm. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val alertDirection: StateFlow<String?> = DutyState.currentAlert
        .flatMapLatest { alert ->
            if (alert == null) flowOf(null)
            else LocationSource(application).updates(5_000L)
                .map<Location, String?> { directionText(alert, it) }
                .onStart { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun directionText(alert: FireAlert, here: Location): String? {
        if (!here.isFresh()) return null
        val fire = Location("fire").apply { latitude = alert.lat; longitude = alert.lon }
        val point = bearingLabel(here.bearingTo(fire)).substringBefore(" ·")
        return "${Bearing.formatDistance(here.distanceTo(fire))} · $point"
    }

    val alarmSounding: StateFlow<Boolean> = DutyState.alarmSounding

    fun silence() {
        DutyService.silence(getApplication())
    }

    /** Set when the firefighter refused a permission needed to go on duty. */
    var permissionsDenied by mutableStateOf(false)

    fun goOnDuty() {
        permissionsDenied = false
        DutyService.start(getApplication())
    }

    fun goOffDuty() {
        DutyService.stop(getApplication())
    }
}
