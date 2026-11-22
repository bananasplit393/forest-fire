package com.example.forestfire.data

import android.content.Context
import com.example.forestfire.R
import com.example.forestfire.data.model.Settings
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManagerFactory

/** Builds a HiveMQ MQTT 5 client that only trusts the CA bundled in res/raw/ca.crt. */
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
            .sslConfig()
                .trustManagerFactory(teamCaTrust(context))
                .applySslConfig()
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener { onConnected() }
            .addDisconnectedListener { onDisconnected(it) }
            .buildAsync()

    private fun teamCaTrust(context: Context): TrustManagerFactory {
        val ca = context.resources.openRawResource(R.raw.ca).use { input ->
            CertificateFactory.getInstance("X.509").generateCertificate(input)
        }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("forestfire-ca", ca)
        }
        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }
    }
}
