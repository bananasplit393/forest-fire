package com.example.forestfire.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.forestfire.nav.GoogleMapsLink

/** Opens walking directions to the fire in Google Maps, or the browser when Maps isn't installed. */
fun Context.openWalkingDirections(lat: Double, lon: Double) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GoogleMapsLink.walkingDirections(lat, lon)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("MapsLauncher", "No app can open map links", e)
    }
}
