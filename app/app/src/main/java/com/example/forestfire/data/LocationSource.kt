package com.example.forestfire.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The phone's GPS position as a Flow, via Google Play Services' fused location provider. */
class LocationSource(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /** Fixes roughly every [intervalMs]. Completes without values if location permission is missing. */
    @SuppressLint("MissingPermission") // callers only collect after the permission was granted
    fun updates(intervalMs: Long): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing", e)
            close()
            return@callbackFlow
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val TAG = "LocationSource"
    }
}

/** True if this fix is at most [maxAgeMs] old (monotonic clock, unaffected by wall-clock changes). */
fun Location.isFresh(maxAgeMs: Long = 30_000): Boolean =
    (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000 <= maxAgeMs
