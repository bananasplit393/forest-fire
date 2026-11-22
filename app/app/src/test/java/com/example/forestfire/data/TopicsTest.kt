package com.example.forestfire.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicsTest {

    @Test
    fun alertsTopic() {
        assertEquals("forest/zone1/alerts", Topics.alerts("zone1"))
    }

    @Test
    fun firefighterTopic() {
        assertEquals("forest/zone1/firefighters/ff-1", Topics.firefighter("zone1", "ff-1"))
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
