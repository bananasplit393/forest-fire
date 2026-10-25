package com.example.forestfire.ui.format

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLabelTest {

    @Test
    fun formatsUnixSecondsAsHoursAndMinutes() {
        // 1760870400 = 2025-10-19T10:40:00Z
        assertEquals("10:40", alertTimeLabel(1760870400, ZoneOffset.UTC))
    }
}
