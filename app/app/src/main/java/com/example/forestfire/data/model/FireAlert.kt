package com.example.forestfire.data.model

import kotlin.math.roundToInt

/** The fire's estimated position and risk, as published by the backend. [ts] is Unix seconds. */
data class FireAlert(
    val risk: Double,
    val lat: Double,
    val lon: Double,
    val ts: Long,
) {
    val riskPercent: Int get() = (risk * 100).roundToInt()
}

/** One message on `forest/{zone}/alerts`. */
sealed interface AlertMessage {
    data class Active(val alert: FireAlert) : AlertMessage

    /** The backend ended the alarm. [ts] is null for an empty payload. */
    data class Cleared(val ts: Long?) : AlertMessage
}
