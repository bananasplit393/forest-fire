package com.example.forestfire.data

import com.example.forestfire.data.model.AlertMessage
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.data.model.FirefighterLocation
import org.json.JSONException
import org.json.JSONObject

/** Converts MQTT payloads to and from the models. Never throws on bad input. */
object AlertJson {

    /** Returns null if the payload is not a valid alert; an empty payload means "cleared". */
    fun parse(payload: ByteArray): AlertMessage? {
        val text = payload.toString(Charsets.UTF_8).trim()
        if (text.isEmpty()) return AlertMessage.Cleared
        return try {
            val json = JSONObject(text)
            if (!json.getBoolean("active")) {
                AlertMessage.Cleared
            } else {
                val alert = FireAlert(
                    risk = json.getDouble("risk"),
                    lat = json.getDouble("lat"),
                    lon = json.getDouble("lon"),
                    ts = json.getLong("ts"),
                )
                if (alert.isInRange()) AlertMessage.Active(alert) else null
            }
        } catch (e: JSONException) {
            null
        }
    }

    fun locationToJson(location: FirefighterLocation): ByteArray =
        JSONObject()
            .put("id", location.id)
            .put("lat", location.lat)
            .put("lon", location.lon)
            .put("ts", location.ts)
            .toString()
            .toByteArray(Charsets.UTF_8)

    private fun FireAlert.isInRange(): Boolean =
        risk in 0.0..1.0 && lat in -90.0..90.0 && lon in -180.0..180.0
}
