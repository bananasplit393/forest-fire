package com.example.forestfire.data.model

/** What the firefighter configures on the settings screen. */
data class Settings(
    val zone: String,
    val firefighterId: String,
    val brokerHost: String,
    val brokerPort: Int,
) {
    companion object {
        /** 10.0.2.2 is the development machine as seen from the Android emulator. */
        val DEFAULT = Settings(zone = "zone1", firefighterId = "ff-1", brokerHost = "10.0.2.2", brokerPort = 8883)
    }
}
