package com.example.forestfire.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationTest {

    @Test
    fun turnsForwardAcrossNorth() {
        assertEquals(370f, shortestRotationTarget(current = 350f, target = 10f), 0.001f)
    }

    @Test
    fun turnsBackwardAcrossNorth() {
        assertEquals(-10f, shortestRotationTarget(current = 10f, target = 350f), 0.001f)
    }

    @Test
    fun keepsAccumulatedTurns() {
        assertEquals(720f, shortestRotationTarget(current = 730f, target = 0f), 0.001f)
        assertEquals(90f, shortestRotationTarget(current = 0f, target = 90f), 0.001f)
    }
}
