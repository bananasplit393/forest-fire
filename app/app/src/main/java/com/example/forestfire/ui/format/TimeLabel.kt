package com.example.forestfire.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm")

/** "14:01" for an alert's Unix-seconds timestamp, in the phone's time zone. */
fun alertTimeLabel(tsSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    HOURS_MINUTES.format(Instant.ofEpochSecond(tsSeconds).atZone(zone))
