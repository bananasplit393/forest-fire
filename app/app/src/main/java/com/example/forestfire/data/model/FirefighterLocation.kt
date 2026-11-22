package com.example.forestfire.data.model

/** Published every 10 s while an alarm is active. [ts] is Unix seconds. */
data class FirefighterLocation(
    val id: String,
    val lat: Double,
    val lon: Double,
    val ts: Long,
)
