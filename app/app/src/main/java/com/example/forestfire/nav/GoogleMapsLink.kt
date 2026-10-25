package com.example.forestfire.nav

import java.util.Locale

/** Links that hand the fire's position to Google Maps (app if installed, browser otherwise). */
object GoogleMapsLink {

    fun walkingDirections(lat: Double, lon: Double): String =
        String.format(
            Locale.US,
            "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f&travelmode=walking",
            lat,
            lon,
        )
}
