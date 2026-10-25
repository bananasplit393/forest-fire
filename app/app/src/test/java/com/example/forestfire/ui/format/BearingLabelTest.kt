package com.example.forestfire.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class BearingLabelTest {

    @Test
    fun labelsCardinalAndIntercardinalPoints() {
        assertEquals("N · 0°", bearingLabel(0f))
        assertEquals("NE · 42°", bearingLabel(42f))
        assertEquals("S · 200°", bearingLabel(200f))
    }

    @Test
    fun normalizesNegativeAndWrappingBearings() {
        // Location.bearingTo() returns -180..180
        assertEquals("W · 270°", bearingLabel(-90f))
        assertEquals("N · 0°", bearingLabel(359.6f))
        assertEquals("E · 90°", bearingLabel(450f))
    }

    @Test
    fun spokenDirectionUsesFullWords() {
        assertEquals("north-east", bearingSpoken(42f))
        assertEquals("west", bearingSpoken(-90f))
    }
}
