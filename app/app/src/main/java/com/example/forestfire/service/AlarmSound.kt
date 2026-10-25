package com.example.forestfire.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.forestfire.R

/**
 * The emergency buzzer: loops on the alarm stream (audible in silent/vibrate mode) with a
 * repeating vibration until [stop]. Never throws; a playback failure is logged.
 */
class AlarmSound(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    private var vibrating = false

    val isPlaying: Boolean get() = player != null || vibrating

    /** Vibration and sound are independent: a failure of one must never leave the alarm silent. */
    fun start() {
        if (isPlaying) return
        try {
            vibrate()
            vibrating = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start the alarm vibration", e)
        }
        try {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
                .also { audioManager.requestAudioFocus(it) }
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_buzzer}"))
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Buzzer playback error $what/$extra")
                    releasePlayer()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start the alarm buzzer", e)
            releasePlayer()
        }
    }

    fun stop() {
        releasePlayer()
        vibrator?.cancel()
        vibrating = false
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun releasePlayer() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }

    // The AudioAttributes overload of vibrate() is deprecated on API 33+ but is the only one carrying the alarm usage on API 26-32.
    @Suppress("DEPRECATION")
    private fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION, 0), attributes)
    }

    private companion object {
        const val TAG = "AlarmSound"
        val VIBRATION = longArrayOf(0, 600, 400)
    }
}
