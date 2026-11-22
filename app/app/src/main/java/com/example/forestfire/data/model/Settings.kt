package com.example.forestfire.data.model

/** What the firefighter configures on the settings screen. */
data class Settings(
    val zone: String,
    val firefighterId: String,
    val brokerHost: String,
    val brokerPort: Int,
) {
    companion object {
        /** 192.168.0.137 is the PC running the backend on the home network. */
        val DEFAULT = Settings(zone = "rold-skov", firefighterId = "ff-1", brokerHost = "192.168.0.137", brokerPort = 1883)
    }
}
