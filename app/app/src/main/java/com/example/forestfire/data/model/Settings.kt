package com.example.forestfire.data.model

/** What the firefighter configures on the settings screen. */
data class Settings(
    val zone: String,
    val firefighterId: String,
    val brokerHost: String,
    val brokerPort: Int,
) {
    companion object {
        /** 192.168.137.1 is the laptop running the backend, seen from a phone on its Windows mobile hotspot. */
        val DEFAULT = Settings(zone = "rold-skov", firefighterId = "ff-1", brokerHost = "192.168.137.1", brokerPort = 1883)
    }
}
