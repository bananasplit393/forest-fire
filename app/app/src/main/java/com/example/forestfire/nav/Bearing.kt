package com.example.forestfire.nav

import java.util.Locale
import kotlin.math.roundToInt

/** Arrow math for the compass screen. Inputs are plain numbers so this stays unit-testable. */
object Bearing {

    /**
     * Degrees to rotate an "up" arrow so it points at the fire.
     * [bearingToFire] comes from Location.bearingTo() (-180..180, true north);
     * [azimuth] is where the phone points (degrees from true north). Result is in [0, 360).
     */
    fun arrowRotation(bearingToFire: Float, azimuth: Float): Float {
        val rotation = (bearingToFire - azimuth) % 360f
        return if (rotation < 0f) rotation + 360f else rotation
    }

    fun formatDistance(meters: Float): String =
        if (meters < 1000f) "${meters.roundToInt()} m"
        else String.format(Locale.US, "%.1f km", meters / 1000f)
}
