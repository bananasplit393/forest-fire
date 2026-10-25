package com.example.forestfire.ui.format

import kotlin.math.roundToInt

private val POINTS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private val SPOKEN = listOf(
    "north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west",
)

private fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

private fun pointIndex(degrees: Float): Int = ((normalize(degrees) + 22.5f) / 45f).toInt() % 8

/** "NE · 42°" for a bearing in degrees from north (any range, e.g. Location.bearingTo's -180..180). */
fun bearingLabel(degrees: Float): String =
    "${POINTS[pointIndex(degrees)]} · ${normalize(degrees).roundToInt() % 360}°"

/** "north-east": the same bearing for TalkBack. */
fun bearingSpoken(degrees: Float): String = SPOKEN[pointIndex(degrees)]
