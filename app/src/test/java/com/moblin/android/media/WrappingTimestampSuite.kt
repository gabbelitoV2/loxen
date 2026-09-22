package com.moblin.android.media

import org.junit.Test
import kotlin.test.assertEquals

class WrappingTimestampSuite {
    private fun seconds(value: Long): Long = value * 1_000_000L

    @Test
    fun all() {
        val timestamp = WrappingTimestamp(name = "Test", maximumTimestamp = seconds(1024))
        assertEquals(seconds(30), timestamp.update(seconds(30)))
        assertEquals(seconds(-1), timestamp.update(seconds(1023)))
        assertEquals(seconds(500), timestamp.update(seconds(500)))
        assertEquals(seconds(700), timestamp.update(seconds(700)))
        assertEquals(seconds(1000), timestamp.update(seconds(1000)))
        assertEquals(seconds(1054), timestamp.update(seconds(30)))
        assertEquals(seconds(1000), timestamp.update(seconds(1000)))
        assertEquals(seconds(1524), timestamp.update(seconds(500)))
        assertEquals(seconds(2024), timestamp.update(seconds(1000)))
        assertEquals(seconds(2048), timestamp.update(seconds(0)))
        assertEquals(seconds(2046), timestamp.update(seconds(1022)))
        assertEquals(seconds(2046), timestamp.update(seconds(1022)))
    }
}
