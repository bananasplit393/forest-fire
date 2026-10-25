package com.example.forestfire.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.forestfire.data.AlarmDecision
import com.example.forestfire.data.AlarmPolicy
import com.example.forestfire.data.AlertJson
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.DutyState
import com.example.forestfire.data.LocationSource
import com.example.forestfire.data.MqttClientFactory
import com.example.forestfire.data.SettingsRepository
import com.example.forestfire.data.Topics
import com.example.forestfire.data.isFresh
import com.example.forestfire.data.model.FirefighterLocation
import com.example.forestfire.data.model.Settings
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import java.security.cert.CertificateException
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

/**
 * Runs while the firefighter is on duty. Owns the MQTT connection, turns alerts into alarms,
 * and publishes the firefighter's location every 10 s while an alarm is active.
 * All state changes happen on the main thread; HiveMQ callbacks hop over via [scope] and are ignored
 * unless their session is still current. A closed session stops its client's automatic reconnecting
 * itself, because HiveMQ's disconnect() does nothing while the client is (re)connecting.
 */
class DutyService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationSource: LocationSource
    private lateinit var notifications: DutyNotifications
    private lateinit var alarmSound: AlarmSound

    private var sessionJob: Job? = null
    private var locationJob: Job? = null

    /** Client and settings of the current MQTT session. */
    private var client: Mqtt5AsyncClient? = null
    private var settings: Settings? = null
    private var sessionClosed: AtomicBoolean? = null

    /** ts of the alert ended with "End response"; retained re-deliveries of it are ignored. */
    private var dismissedTs: Long? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        locationSource = LocationSource(this)
        notifications = DutyNotifications(this)
        alarmSound = AlarmSound(this)
        notifications.createChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> goOnDuty()
            ACTION_STOP -> {
                goOffDuty()
                return START_NOT_STICKY
            }
            ACTION_END_RESPONSE -> if (sessionJob == null) stopSelf() else endResponse()
            ACTION_SILENCE -> if (sessionJob == null) stopSelf() else silence()
            null -> restartAfterProcessDeath()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        alarmSound.stop()
        sessionClosed?.set(true)
        scope.cancel()
        client?.disconnect()
        DutyState.reset()
        super.onDestroy()
    }

    private fun goOnDuty() {
        notifications.cancelDutyStopped()
        if (!startInForeground()) {
            stopSelf()
            return
        }
        if (sessionJob != null) return
        DutyState.setOnDuty(true)
        sessionJob = scope.launch {
            settingsRepository.setOnDuty(true)
            launch {
                DutyState.connection.collect { notifications.updateDuty(it, settings?.zone) }
            }
            // A settings change cancels the running session and starts a new one.
            settingsRepository.settings.distinctUntilChanged().collectLatest { runSession(it) }
        }
    }

    private fun goOffDuty() {
        clearAlarm()
        sessionJob?.cancel()
        sessionJob = null
        DutyState.reset()
        scope.launch {
            // A quick ACTION_START may have begun a new session; leave its flag and service alone.
            if (sessionJob != null) return@launch
            settingsRepository.setOnDuty(false)
            shutDown()
        }
    }

    /** START_STICKY restart: only resume if the firefighter did not go off duty. */
    private fun restartAfterProcessDeath() {
        if (!startInForeground()) {
            // Android 12+ may refuse a background start: reset the flag and ask the firefighter to come back.
            scope.launch {
                settingsRepository.setOnDuty(false)
                notifications.showDutyStopped()
                stopSelf()
            }
            return
        }
        scope.launch {
            if (settingsRepository.onDuty.first()) goOnDuty() else shutDown()
        }
    }

    private fun shutDown() {
        if (sessionJob != null) return // a new session started meanwhile
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            DutyNotifications.ID_DUTY,
            notifications.duty(DutyState.connection.value, settings?.zone),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        true
    } catch (e: RuntimeException) {
        // Android refuses e.g. when location permission was revoked or a background start is not allowed.
        Log.w(TAG, "Could not start foreground service", e)
        false
    }

    private suspend fun runSession(settings: Settings) {
        clearAlarm()
        dismissedTs = null
        this.settings = settings
        DutyState.setConnection(ConnectionState.Connecting)

        val closed = AtomicBoolean(false)
        sessionClosed = closed
        lateinit var mqtt: Mqtt5AsyncClient
        mqtt = MqttClientFactory.create(
            context = this,
            settings = settings,
            onConnected = { if (closed.get()) mqtt.disconnect() else onConnected(mqtt, settings) },
            onDisconnected = { onDisconnected(it, mqtt, closed) },
        )
        mqtt.publishes(MqttGlobalPublishFilter.SUBSCRIBED) { publish ->
            val payload = publish.payloadAsBytes
            scope.launch { if (client === mqtt) onAlertPayload(payload) }
        }
        client = mqtt
        try {
            try {
                mqtt.connect().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // onDisconnected has already set Reconnecting or Error; HiveMQ keeps retrying unless TLS failed.
                Log.w(TAG, "Initial MQTT connect failed", e)
            }
            awaitCancellation()
        } finally {
            closed.set(true)
            client = null
            mqtt.disconnect()
        }
    }

    /** Runs on a HiveMQ thread after every (re)connect. Subscribing each time also replays the retained alert. */
    private fun onConnected(mqtt: Mqtt5AsyncClient, settings: Settings) {
        scope.launch { if (client === mqtt) DutyState.setConnection(ConnectionState.Connected) }
        mqtt.subscribeWith()
            .topicFilter(Topics.alerts(settings.zone))
            .qos(MqttQos.AT_LEAST_ONCE)
            .send()
            .whenComplete { _, error -> if (error != null) Log.w(TAG, "Subscribe failed", error) }
    }

    /** Runs on a HiveMQ thread after a lost connection or a failed connect attempt. */
    private fun onDisconnected(context: MqttClientDisconnectedContext, mqtt: Mqtt5AsyncClient, closed: AtomicBoolean) {
        when {
            closed.get() -> context.reconnector.reconnect(false) // stale session: stop its retry loop
            context.source == MqttDisconnectSource.USER -> Unit // we disconnected on purpose
            context.cause.isTlsFailure() -> {
                context.reconnector.reconnect(false) // retrying cannot fix a certificate problem
                postConnection(mqtt, ConnectionState.Error("TLS handshake failed — check broker host and CA certificate"))
            }
            else -> postConnection(mqtt, ConnectionState.Reconnecting)
        }
    }

    private fun postConnection(mqtt: Mqtt5AsyncClient, state: ConnectionState) {
        scope.launch { if (client === mqtt) DutyState.setConnection(state) }
    }

    private fun onAlertPayload(payload: ByteArray) {
        val message = AlertJson.parse(payload)
        if (message == null) Log.w(TAG, "Ignoring malformed alert: ${String(payload)}")
        when (val decision = AlarmPolicy.decide(DutyState.currentAlert.value, dismissedTs, message)) {
            is AlarmDecision.NewAlarm -> {
                DutyState.setAlert(decision.alert)
                notifications.showAlarm(decision.alert)
                alarmSound.start()
                DutyState.setAlarmSounding(alarmSound.isPlaying)
                startLocationLoop()
            }
            is AlarmDecision.PositionUpdate -> {
                DutyState.setAlert(decision.alert)
                notifications.showAlarm(decision.alert)
            }
            AlarmDecision.Clear -> clearAlarm()
            AlarmDecision.Ignore -> Unit
        }
    }

    private fun endResponse() {
        dismissedTs = DutyState.currentAlert.value?.ts ?: dismissedTs
        clearAlarm()
    }

    /** Acknowledges the alarm: stops the buzzer, keeps the alarm itself active. */
    private fun silence() {
        alarmSound.stop()
        DutyState.setAlarmSounding(false)
    }

    private fun clearAlarm() {
        silence()
        DutyState.setAlert(null)
        notifications.cancelAlarm()
        locationJob?.cancel()
        locationJob = null
    }

    private fun startLocationLoop() {
        if (locationJob?.isActive == true) return
        locationJob = scope.launch {
            var latest: Location? = null
            launch { locationSource.updates(LOCATION_INTERVAL_MS).collect { latest = it } }
            while (true) {
                val fix = latest
                if (fix != null && fix.isFresh()) publishLocation(fix)
                else Log.d(TAG, "No fresh GPS fix; skipping location publish")
                delay(PUBLISH_INTERVAL_MS)
            }
        }
    }

    private fun publishLocation(fix: Location) {
        val mqtt = client ?: return
        val current = settings ?: return
        if (DutyState.connection.value != ConnectionState.Connected) return // no burst of stale fixes after reconnect
        val location = FirefighterLocation(
            id = current.firefighterId,
            lat = fix.latitude,
            lon = fix.longitude,
            ts = fix.time / 1000,
        )
        mqtt.publishWith()
            .topic(Topics.firefighter(current.zone, current.firefighterId))
            .qos(MqttQos.AT_MOST_ONCE)
            .payload(AlertJson.locationToJson(location))
            .send()
    }

    private fun Throwable.isTlsFailure(): Boolean =
        generateSequence(this) { it.cause }.any { it is SSLException || it is CertificateException }

    companion object {
        private const val TAG = "DutyService"
        private const val ACTION_START = "com.example.forestfire.action.START_DUTY"
        private const val ACTION_STOP = "com.example.forestfire.action.STOP_DUTY"
        private const val ACTION_END_RESPONSE = "com.example.forestfire.action.END_RESPONSE"
        private const val ACTION_SILENCE = "com.example.forestfire.action.SILENCE"
        private const val PUBLISH_INTERVAL_MS = 10_000L
        private const val LOCATION_INTERVAL_MS = 5_000L

        /** Call from a visible Activity, after location + notification permissions are granted. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        fun endResponse(context: Context) {
            context.startService(intent(context, ACTION_END_RESPONSE))
        }

        /** Stops the buzzer (compass opened, Silence tapped). No-op when nothing is sounding. */
        fun silence(context: Context) {
            if (DutyState.alarmSounding.value) context.startService(silenceIntent(context))
        }

        fun silenceIntent(context: Context): Intent = intent(context, ACTION_SILENCE)

        private fun intent(context: Context, action: String) =
            Intent(context, DutyService::class.java).setAction(action)
    }
}
