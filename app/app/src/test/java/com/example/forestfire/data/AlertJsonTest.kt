package com.example.forestfire.data

import com.example.forestfire.data.model.AlertMessage
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.data.model.FirefighterLocation
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertJsonTest {

    private fun parse(text: String) = AlertJson.parse(text.toByteArray())

    @Test
    fun parsesActiveAlert() {
        val message = parse("""{"active":true,"risk":0.87,"lat":56.1234,"lon":10.5678,"ts":1760870400}""")
        assertEquals(AlertMessage.Active(FireAlert(0.87, 56.1234, 10.5678, 1760870400)), message)
    }

    @Test
    fun parsesClearWithTimestamp() {
        assertEquals(AlertMessage.Cleared(1760871000), parse("""{"active":false,"ts":1760871000}"""))
    }

    @Test
    fun parsesClearWithoutTimestamp() {
        assertEquals(AlertMessage.Cleared(null), parse("""{"active":false}"""))
    }

    @Test
    fun emptyPayloadIsClear() {
        assertEquals(AlertMessage.Cleared(null), AlertJson.parse(ByteArray(0)))
        assertEquals(AlertMessage.Cleared(null), parse("  \n"))
    }

    @Test
    fun malformedJsonIsNull() {
        assertNull(parse("not json"))
        assertNull(parse("[1,2]"))
    }

    @Test
    fun missingFieldsAreNull() {
        assertNull(parse("""{"risk":0.5,"lat":56.1,"lon":10.2,"ts":1}"""))          // no "active"
        assertNull(parse("""{"active":true,"risk":0.5,"lat":56.1,"ts":1}"""))       // no "lon"
        assertNull(parse("""{"active":true,"risk":0.5,"lat":56.1,"lon":10.2}"""))   // no "ts"
    }

    @Test
    fun outOfRangeValuesAreNull() {
        assertNull(parse("""{"active":true,"risk":1.5,"lat":56.1,"lon":10.2,"ts":1}"""))
        assertNull(parse("""{"active":true,"risk":0.5,"lat":91.0,"lon":10.2,"ts":1}"""))
        assertNull(parse("""{"active":true,"risk":0.5,"lat":56.1,"lon":-181.0,"ts":1}"""))
    }

    @Test
    fun serializesLocation() {
        val bytes = AlertJson.locationToJson(FirefighterLocation("ff-1", 56.12, 10.55, 1760870410))
        val json = JSONObject(String(bytes))
        assertEquals("ff-1", json.getString("id"))
        assertEquals(56.12, json.getDouble("lat"), 1e-9)
        assertEquals(10.55, json.getDouble("lon"), 1e-9)
        assertEquals(1760870410L, json.getLong("ts"))
    }
}
