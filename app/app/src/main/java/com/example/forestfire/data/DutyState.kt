package com.example.forestfire.data

import com.example.forestfire.data.model.FireAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ConnectionState {
    val label: String

    data object Disconnected : ConnectionState { override val label = "Disconnected" }
    data object Connecting : ConnectionState { override val label = "Connecting…" }
    data object Connected : ConnectionState { override val label = "Connected" }
    data object Reconnecting : ConnectionState { override val label = "Reconnecting…" }
    data class Error(val message: String) : ConnectionState { override val label get() = message }
}

/**
 * Shared between DutyService (writes) and the ViewModels (read), so the UI never binds to the
 * service. Lives as long as the process.
 */
object DutyState {
    private val _onDuty = MutableStateFlow(false)
    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _currentAlert = MutableStateFlow<FireAlert?>(null)

    private val _alarmSounding = MutableStateFlow(false)

    /** True while the emergency buzzer is playing (until the firefighter acknowledges). */
    val alarmSounding: StateFlow<Boolean> = _alarmSounding.asStateFlow()

    val onDuty: StateFlow<Boolean> = _onDuty.asStateFlow()
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()
    val currentAlert: StateFlow<FireAlert?> = _currentAlert.asStateFlow()

    fun setOnDuty(value: Boolean) { _onDuty.value = value }
    fun setConnection(value: ConnectionState) { _connection.value = value }
    fun setAlert(value: FireAlert?) { _currentAlert.value = value }

    fun setAlarmSounding(value: Boolean) { _alarmSounding.value = value }

    fun reset() {
        _onDuty.value = false
        _connection.value = ConnectionState.Disconnected
        _currentAlert.value = null
        _alarmSounding.value = false
    }
}
