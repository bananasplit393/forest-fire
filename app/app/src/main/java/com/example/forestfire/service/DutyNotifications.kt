package com.example.forestfire.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.forestfire.R
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.ui.compass.CompassActivity
import com.example.forestfire.ui.main.MainActivity

/**
 * The quiet "on duty" notification required by the foreground service, and the loud alarm.
 * Channel settings are fixed once created; the alarm channel id carries a version suffix for that reason.
 */
class DutyNotifications(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DUTY, "On duty", NotificationManager.IMPORTANCE_LOW),
        )
        // The buzzer (AlarmSound) owns the audio; the channel itself is silent so they never overlap.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ALARM)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Fire alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    fun duty(connection: ConnectionState, zone: String?): Notification =
        NotificationCompat.Builder(context, CHANNEL_DUTY)
            .setSmallIcon(R.drawable.ic_stat_fire)
            .setContentTitle(if (zone != null) "On duty — $zone" else "On duty")
            .setContentText(connection.label)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openActivity(MainActivity::class.java, REQUEST_MAIN))
            .build()

    fun updateDuty(connection: ConnectionState, zone: String?) {
        manager.notify(ID_DUTY, duty(connection, zone))
    }

    /** Silent, heads-up alarm notification; the buzzer is played by AlarmSound. */
    fun showAlarm(alert: FireAlert) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_fire)
            .setContentTitle("FIRE ALARM")
            .setContentText("Risk ${alert.riskPercent}% — tap to navigate to the fire")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openActivity(CompassActivity::class.java, REQUEST_ALARM))
            .addAction(
                0,
                "Silence",
                PendingIntent.getService(
                    context,
                    REQUEST_SILENCE,
                    DutyService.silenceIntent(context),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
        manager.notify(ID_ALARM, notification)
    }

    /** Shown when Android refused to restart the duty service in the background. Quiet: duty channel. */
    fun showDutyStopped() {
        val notification = NotificationCompat.Builder(context, CHANNEL_DUTY)
            .setSmallIcon(R.drawable.ic_stat_fire)
            .setContentTitle("Duty stopped")
            .setContentText("Tap to go back on duty")
            .setAutoCancel(true)
            .setContentIntent(openActivity(MainActivity::class.java, REQUEST_MAIN))
            .build()
        manager.notify(ID_DUTY_STOPPED, notification)
    }

    fun cancelDutyStopped() {
        manager.cancel(ID_DUTY_STOPPED)
    }

    fun cancelAlarm() {
        manager.cancel(ID_ALARM)
    }

    private fun openActivity(activity: Class<out Activity>, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ID_DUTY = 1
        const val ID_ALARM = 2
        const val ID_DUTY_STOPPED = 3
        private const val CHANNEL_DUTY = "duty"
        private const val CHANNEL_ALARM = "fire_alarm_v2"
        private const val LEGACY_CHANNEL_ALARM = "fire_alarm"
        private const val REQUEST_MAIN = 0
        private const val REQUEST_ALARM = 1
        private const val REQUEST_SILENCE = 2
    }
}
