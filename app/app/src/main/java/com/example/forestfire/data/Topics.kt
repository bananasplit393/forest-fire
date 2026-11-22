package com.example.forestfire.data

/** MQTT topic names agreed with the team (see the spec, section 4). */
object Topics {

    fun alerts(zone: String): String = "forest/$zone/alerts"

    fun firefighter(zone: String, id: String): String = "forest/$zone/firefighters/$id"

    /** A zone or firefighter id must be one non-empty topic level without MQTT wildcards. */
    fun isValidLevel(level: String): Boolean =
        level.isNotBlank() && level.none { it == '/' || it == '+' || it == '#' }
}
