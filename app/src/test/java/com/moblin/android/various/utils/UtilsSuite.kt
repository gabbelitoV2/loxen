package com.moblin.android.various.utils

import com.moblin.android.common.various.add
import com.moblin.android.common.various.formatPace
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.formatWindAndGustSpeed
import com.moblin.android.common.various.formatWindSpeed
import com.moblin.android.common.various.getInt64Be
import com.moblin.android.common.various.removeAllWhitespaces
import com.moblin.android.common.various.truncate
import java.util.Locale
import java.util.UUID
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals

class UtilsSuite {
    @Test
    fun fullDuration() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        assertEquals("0 secs", formatShortDuration(seconds = 0))
        assertEquals("1 sec", formatShortDuration(seconds = 1))
        assertEquals("30 secs", formatShortDuration(seconds = 30))
        assertEquals("1 min", formatShortDuration(seconds = 60))
        assertEquals("2 min", formatShortDuration(seconds = 120))
        assertEquals("1 hr", formatShortDuration(seconds = 3600))
        assertEquals("1 day", formatShortDuration(seconds = 86400))
        assertEquals("7 days", formatShortDuration(seconds = 7 * 86400))
        assertEquals("30 days", formatShortDuration(seconds = 30 * 86400))
        assertEquals("90 days", formatShortDuration(seconds = 90 * 86400))
    }

    @Test
    fun pace() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        assertEquals("- min/km", formatPace(speed = 0.0))
        assertEquals("16:40 min/km", formatPace(speed = 1.0))
        assertEquals("5:33 min/km", formatPace(speed = 3.0))
        assertEquals("3:20 min/km", formatPace(speed = 5.0))
        assertEquals("1:40 min/km", formatPace(speed = 10.0))
        assertEquals("1:00 min/km", formatPace(speed = 16.66))
        assertEquals("0:59 min/km", formatPace(speed = 16.667))
    }

    @Test
    fun windSpeed() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        assertEquals("2 m/s",
            formatWindSpeed(speed = milesPerHourToMetersPerSecond(5.0), unit = null))
        assertEquals("4 m/s",
            formatWindSpeed(speed = milesPerHourToMetersPerSecond(10.0), unit = null))
    }

    @Test
    fun windSpeedAndGust() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        assertEquals("2 (4) m/s",
            formatWindAndGustSpeed(speed = milesPerHourToMetersPerSecond(5.0),
                gust = milesPerHourToMetersPerSecond(10.0),
                unit = null))
        assertEquals("6 (8) m/s",
            formatWindAndGustSpeed(speed = milesPerHourToMetersPerSecond(15.0),
                gust = milesPerHourToMetersPerSecond(20.0),
                unit = null))
    }

    @Test
    fun uuidAddEmpty() {
        val original = UUID.fromString("00000000-1111-2222-3333-000000000000")
        val extra = ByteArray(0)
        val result = UUID.fromString("00000000-1111-2222-3333-000000000000")
        assertEquals(result, original.add(data = extra))
    }

    @Test
    fun uuidAddShort() {
        val original = UUID.fromString("07080000-0000-0000-0000-010203040506")
        val extra = byteArrayOf(0x01, 0x23, 0x45, 0x67)
        val result = UUID.fromString("07080000-0000-0000-0000-010204274A6D")
        assertEquals(result, original.add(data = extra))
    }

    @Test
    fun uuidAddLong() {
        val original = UUID.fromString("07080000-0000-0000-0000-010203040506")
        val extra = byteArrayOf(0x01, 0x23, 0x45, 0x67) + ByteArray(15)
        val result = UUID.fromString("6E080000-0000-0000-0000-01020305284B")
        assertEquals(result, original.add(data = extra))
    }

    @Test
    fun uuidAddByteWrap() {
        val original = UUID.fromString("00010000-0000-0000-0000-FAFBFCFDFEFF")
        val extra = byteArrayOf(0x01, 0x23, 0x45, 0x67)
        val result = UUID.fromString("00010000-0000-0000-0000-FAFBFD204366")
        assertEquals(result, original.add(data = extra))
    }

    @Test
    fun arrayWithCPointers() {
        val data = listOf("1", "22", "333")
        val cPointers = data.map { it.toByteArray(Charsets.UTF_8) + byteArrayOf(0) }
        val first = cPointers[0]
        assertEquals(0x31.toByte(), first[0])
        assertEquals(0x0.toByte(), first[1])
        val second = cPointers[1]
        assertEquals(0x32.toByte(), second[0])
        assertEquals(0x32.toByte(), second[1])
        assertEquals(0x0.toByte(), second[2])
        val third = cPointers[2]
        assertEquals(0x33.toByte(), third[0])
        assertEquals(0x33.toByte(), third[1])
        assertEquals(0x33.toByte(), third[2])
        assertEquals(0x0.toByte(), third[3])
    }

    @Test
    fun getInt64() {
        val data = ByteArray(8) { 0xFF.toByte() }
        assertEquals(-1L, data.getInt64Be(offset = 0))
    }

    @Test
    fun removeAllWhitespaces() {
        assertEquals("", "".removeAllWhitespaces())
        assertEquals("jijiji", " ji ji ji ".removeAllWhitespaces())
        assertEquals("", "   ".removeAllWhitespaces())
        assertEquals("aa", "a     a".removeAllWhitespaces())
        assertEquals("fdsdfdf", "fds\ndf\ndf".removeAllWhitespaces())
    }

    @Test
    fun truncate() {
        assertEquals("", "".truncate(length = 5))
        assertEquals("hi", "hi".truncate(length = 5))
        assertEquals("hello", "hello".truncate(length = 5))
        assertEquals("he...", "hello!".truncate(length = 5))
        assertEquals("...", "hello!".truncate(length = 3))
        assertEquals("..", "hello!".truncate(length = 2))
        assertEquals("", "hello!".truncate(length = 0))
    }
}

private fun milesPerHourToMetersPerSecond(milesPerHour: Double): Double = milesPerHour * 0.44704
