package com.example.forestfire.data

import android.content.Context
import com.example.forestfire.data.model.Settings
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient

/** Builds a HiveMQ MQTT 5 client that logs in to the team broker. */
object MqttClientFactory {

    fun create(
        context: Context,
        settings: Settings,
        onConnected: () -> Unit,
        onDisconnected: (MqttClientDisconnectedContext) -> Unit,
    ): Mqtt5AsyncClient =
        MqttClient.builder()
            .useMqttVersion5()
            .identifier("firefighter-${settings.firefighterId}")
            .serverHost(settings.brokerHost)
            .serverPort(settings.brokerPort)
            .simpleAuth()
                .username("forestfire")
                .password("mqtt-password".toByteArray())
                .applySimpleAuth()
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener { onConnected() }
            .addDisconnectedListener { onDisconnected(it) }
            .buildAsync()
}
