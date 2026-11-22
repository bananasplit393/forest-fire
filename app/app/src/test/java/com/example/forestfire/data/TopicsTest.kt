package com.example.forestfire.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicsTest {

    @Test
    fun alertsTopic() {
        assertEquals("alerts/rold-skov", Topics.alerts("rold-skov"))
    }

    @Test
    fun firefighterTopic() {
        assertEquals("firefighters/ff-1/location", Topics.firefighter("rold-skov", "ff-1"))
    }

    @Test
    fun acceptsPlainLevel() {
        assertTrue(Topics.isValidLevel("zone-7"))
    }

    @Test
    fun rejectsEmptyAndWildcardLevels() {
        listOf("", "   ", "a/b", "+", "zone#").forEach { level ->
            assertFalse("'$level' should be invalid", Topics.isValidLevel(level))
        }
    }
}
