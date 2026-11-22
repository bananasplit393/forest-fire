package com.example.forestfire.ui.format

import com.example.forestfire.data.ConnectionState

/** What the reflective stripe shows: it always means duty state, never decoration. */
enum class StripeState { Off, Lit, Pending, Fault }

fun stripeStateFor(onDuty: Boolean, connection: ConnectionState): StripeState = when {
    !onDuty -> StripeState.Off
    connection is ConnectionState.Connected -> StripeState.Lit
    connection is ConnectionState.Error -> StripeState.Fault
    else -> StripeState.Pending
}
