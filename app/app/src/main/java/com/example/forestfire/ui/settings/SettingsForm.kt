package com.example.forestfire.ui.settings

import com.example.forestfire.data.Topics
import com.example.forestfire.data.model.Settings

/** The text fields on the settings screen (the port is text while being edited). */
data class SettingsForm(
    val zone: String = "",
    val firefighterId: String = "",
    val brokerHost: String = "",
    val brokerPort: String = "",
) {
    val zoneError: String?
        get() = if (Topics.isValidLevel(zone.trim())) null else "Required, and must not contain / + or #"

    val idError: String?
        get() = if (Topics.isValidLevel(firefighterId.trim())) null else "Required, and must not contain / + or #"

    val hostError: String?
        get() = if (brokerHost.isNotBlank()) null else "Required"

    val portError: String?
        get() = if (brokerPort.trim().toIntOrNull()?.let { it in 1..65535 } == true) null else "Port 1–65535"

    val isValid: Boolean
        get() = zoneError == null && idError == null && hostError == null && portError == null

    /** Only call when [isValid]. */
    fun toSettings(): Settings =
        Settings(zone.trim(), firefighterId.trim(), brokerHost.trim(), brokerPort.trim().toInt())

    companion object {
        fun from(settings: Settings) = SettingsForm(
            zone = settings.zone,
            firefighterId = settings.firefighterId,
            brokerHost = settings.brokerHost,
            brokerPort = settings.brokerPort.toString(),
        )
    }
}
