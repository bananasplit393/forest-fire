package com.example.forestfire.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** [azimuth] is degrees from magnetic north in [0, 360), for a phone held flat in portrait. */
data class Heading(val azimuth: Float, val lowAccuracy: Boolean)

/** Which way the phone points, from the fused rotation-vector sensor. */
class HeadingSource(context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)

    /** False on devices without a rotation-vector sensor; [updates] then completes immediately. */
    val isAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    /** Completes immediately on devices without a rotation-vector sensor. */
    fun updates(): Flow<Heading> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                trySend(Heading(degrees, lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW))
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                accuracy = newAccuracy
            }
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
