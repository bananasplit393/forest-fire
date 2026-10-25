package com.example.forestfire.data

import com.example.forestfire.data.AlarmDecision.Clear
import com.example.forestfire.data.AlarmDecision.Ignore
import com.example.forestfire.data.AlarmDecision.NewAlarm
import com.example.forestfire.data.AlarmDecision.PositionUpdate
import com.example.forestfire.data.model.AlertMessage.Active
import com.example.forestfire.data.model.AlertMessage.Cleared
import com.example.forestfire.data.model.FireAlert
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPolicyTest {

    private val alert = FireAlert(risk = 0.9, lat = 56.0, lon = 10.0, ts = 100)

    @Test
    fun firstActiveAlertIsNewAlarm() {
        assertEquals(NewAlarm(alert), AlarmPolicy.decide(current = null, dismissedTs = null, message = Active(alert)))
    }

    @Test
    fun activeAlertWhileAlarmActiveIsPositionUpdate() {
        val moved = alert.copy(lat = 56.01, ts = 110)
        assertEquals(PositionUpdate(moved), AlarmPolicy.decide(alert, null, Active(moved)))
    }

    @Test
    fun olderAlertWhileAlarmActiveIsIgnored() {
        assertEquals(Ignore, AlarmPolicy.decide(alert, null, Active(alert.copy(ts = 90))))
    }

    @Test
    fun redeliveredDismissedAlertIsIgnored() {
        assertEquals(Ignore, AlarmPolicy.decide(current = null, dismissedTs = 100, message = Active(alert)))
    }

    @Test
    fun newerAlertAfterDismissIsNewAlarm() {
        val newer = alert.copy(ts = 101)
        assertEquals(NewAlarm(newer), AlarmPolicy.decide(current = null, dismissedTs = 100, message = Active(newer)))
    }

    @Test
    fun clearWhileAlarmActiveIsClear() {
        assertEquals(Clear, AlarmPolicy.decide(alert, null, Cleared(120)))
        assertEquals(Clear, AlarmPolicy.decide(alert, null, Cleared(null)))
    }

    @Test
    fun clearWithoutActiveAlarmIsIgnored() {
        assertEquals(Ignore, AlarmPolicy.decide(null, null, Cleared(120)))
    }

    @Test
    fun malformedMessageIsIgnored() {
        assertEquals(Ignore, AlarmPolicy.decide(alert, null, null))
    }
}
