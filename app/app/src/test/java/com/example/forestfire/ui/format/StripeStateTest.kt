package com.example.forestfire.ui.format

import com.example.forestfire.data.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class StripeStateTest {

    @Test
    fun offDutyIsAlwaysOff() {
        assertEquals(StripeState.Off, stripeStateFor(onDuty = false, connection = ConnectionState.Connected))
    }

    @Test
    fun connectedIsLit() {
        assertEquals(StripeState.Lit, stripeStateFor(true, ConnectionState.Connected))
    }

    @Test
    fun connectingStatesArePending() {
        assertEquals(StripeState.Pending, stripeStateFor(true, ConnectionState.Connecting))
        assertEquals(StripeState.Pending, stripeStateFor(true, ConnectionState.Reconnecting))
        assertEquals(StripeState.Pending, stripeStateFor(true, ConnectionState.Disconnected))
    }

    @Test
    fun errorIsFault() {
        assertEquals(StripeState.Fault, stripeStateFor(true, ConnectionState.Error("TLS handshake failed")))
    }
}
