package com.example.forestfire.data

/** MQTT topic names used by the backend. The zone is the sensor node, e.g. rold-skov. */
object Topics {

    fun alerts(zone: String): String = "alerts/$zone"

    fun firefighter(zone: String, id: String): String = "firefighters/$id/location"

    /** A zone or firefighter id must be one non-empty topic level without MQTT wildcards. */
    fun isValidLevel(level: String): Boolean =
        level.isNotBlank() && level.none { it == '/' || it == '+' || it == '#' }
}
