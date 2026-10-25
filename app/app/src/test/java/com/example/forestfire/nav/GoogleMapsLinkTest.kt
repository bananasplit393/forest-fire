package com.example.forestfire.nav

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleMapsLinkTest {

    private val original = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun buildsWalkingDirectionsUrl() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=56.172000,10.190000&travelmode=walking",
            GoogleMapsLink.walkingDirections(56.172, 10.19),
        )
    }

    @Test
    fun keepsNegativeCoordinates() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=-33.868800,151.209300&travelmode=walking",
            GoogleMapsLink.walkingDirections(-33.8688, 151.2093),
        )
    }

    @Test
    fun usesDotDecimalsWhateverTheDeviceLocale() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=56.172000,10.190000&travelmode=walking",
            GoogleMapsLink.walkingDirections(56.172, 10.19),
        )
    }
}
