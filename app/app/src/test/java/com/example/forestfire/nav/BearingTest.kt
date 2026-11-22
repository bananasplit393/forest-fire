package com.example.forestfire.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class BearingTest {

    @Test
    fun fireStraightAheadPointsUp() {
        assertEquals(0f, Bearing.arrowRotation(bearingToFire = 90f, azimuth = 90f), 0.001f)
    }

    @Test
    fun wrapsAroundNorth() {
        assertEquals(20f, Bearing.arrowRotation(bearingToFire = 10f, azimuth = 350f), 0.001f)
    }

    @Test
    fun handlesNegativeBearingFromLocationApi() {
        // Location.bearingTo() returns -180..180; -90 means west.
        assertEquals(270f, Bearing.arrowRotation(bearingToFire = -90f, azimuth = 0f), 0.001f)
    }

    @Test
    fun handlesAzimuthAbove360() {
        assertEquals(315f, Bearing.arrowRotation(bearingToFire = 0f, azimuth = 765f), 0.001f)
    }

    @Test
    fun formatsMetersBelowOneKilometre() {
        assertEquals("850 m", Bearing.formatDistance(849.6f))
    }

    @Test
    fun formatsKilometresWithOneDecimal() {
        assertEquals("1.0 km", Bearing.formatDistance(1000f))
        assertEquals("2.4 km", Bearing.formatDistance(2400f))
    }
}
