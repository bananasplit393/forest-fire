package com.example.forestfire.ui.settings

import com.example.forestfire.data.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormTest {

    private val valid = SettingsForm(zone = "zone1", firefighterId = "ff-1", brokerHost = "10.0.2.2", brokerPort = "8883")

    @Test
    fun roundTripsSettings() {
        assertEquals(Settings.DEFAULT, SettingsForm.from(Settings.DEFAULT).toSettings())
    }

    @Test
    fun trimsWhitespace() {
        val form = SettingsForm(" zone1 ", " ff-1 ", " broker.local ", "8883")
        assertEquals(Settings("zone1", "ff-1", "broker.local", 8883), form.toSettings())
    }

    @Test
    fun validFormIsValid() {
        assertTrue(valid.isValid)
    }

    @Test
    fun rejectsTopicWildcardsInZoneAndId() {
        assertFalse(valid.copy(zone = "zone/1").isValid)
        assertFalse(valid.copy(firefighterId = "#").isValid)
    }

    @Test
    fun rejectsBlankHostAndBadPort() {
        assertFalse(valid.copy(brokerHost = " ").isValid)
        assertFalse(valid.copy(brokerPort = "abc").isValid)
        assertFalse(valid.copy(brokerPort = "0").isValid)
        assertFalse(valid.copy(brokerPort = "70000").isValid)
    }

    @Test
    fun validFormHasNoFieldErrors() {
        assertNull(valid.zoneError)
        assertNull(valid.idError)
        assertNull(valid.hostError)
        assertNull(valid.portError)
    }

    @Test
    fun eachInvalidFieldReportsItsOwnError() {
        assertNotNull(valid.copy(zone = "zone/1").zoneError)
        assertNotNull(valid.copy(firefighterId = "").idError)
        assertNotNull(valid.copy(brokerHost = " ").hostError)
        assertNotNull(valid.copy(brokerPort = "70000").portError)
    }

    @Test
    fun portWithSurroundingSpacesIsValid() {
        val form = valid.copy(brokerPort = " 8883 ")
        assertTrue(form.isValid)
        assertEquals(8883, form.toSettings().brokerPort)
    }
}
