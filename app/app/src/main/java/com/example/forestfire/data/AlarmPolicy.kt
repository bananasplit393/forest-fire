package com.example.forestfire.data

import com.example.forestfire.data.model.AlertMessage
import com.example.forestfire.data.model.FireAlert

/** What the duty service should do with an incoming alert message. */
sealed interface AlarmDecision {
    /** Sound the alarm and start publishing location. */
    data class NewAlarm(val alert: FireAlert) : AlarmDecision

    /** Same fire, new position: move the arrow, no new siren. */
    data class PositionUpdate(val alert: FireAlert) : AlarmDecision

    /** The backend ended the alarm. */
    data object Clear : AlarmDecision

    data object Ignore : AlarmDecision
}

object AlarmPolicy {

    /**
     * [current] is the active alarm (null if none). [dismissedTs] is the ts of the alert the
     * firefighter ended with "End response"; retained re-deliveries of it must not alarm again.
     * [message] is null when the payload was malformed.
     */
    fun decide(current: FireAlert?, dismissedTs: Long?, message: AlertMessage?): AlarmDecision =
        when (message) {
            null -> AlarmDecision.Ignore
            is AlertMessage.Cleared -> if (current != null) AlarmDecision.Clear else AlarmDecision.Ignore
            is AlertMessage.Active -> {
                val alert = message.alert
                when {
                    dismissedTs != null && alert.ts <= dismissedTs -> AlarmDecision.Ignore
                    current == null -> AlarmDecision.NewAlarm(alert)
                    alert.ts < current.ts -> AlarmDecision.Ignore
                    else -> AlarmDecision.PositionUpdate(alert)
                }
            }
        }
}
