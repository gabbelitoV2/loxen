package com.moblin.android.media.webrtc

import com.moblin.android.isEqual
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

private val ntpUnixEpochSeconds: ULong = 2_208_988_800uL

class WebrtcIngestClientSuite {
    @Test
    fun decodeNtpTimestampBeforeUnixEpoch() {
        for (value in listOf(
            0uL,
            1uL shl 32,
            ntpUnixEpochSeconds,
            (ntpUnixEpochSeconds - 1uL) shl 32 or 0xFFFF_FFFFuL,
        )) {
            assertNull(decodeNtpTimestamp(v = value))
        }
    }

    @Test
    fun decodeNtpTimestampAtUnixEpoch() {
        val timestamp = assertNotNull(decodeNtpTimestamp(v = ntpUnixEpochSeconds shl 32))
        assertEquals(0.0, timestamp)
    }

    @Test
    fun decodeNtpTimestampWithFraction() {
        val timestamp = assertNotNull(
            decodeNtpTimestamp(v = (ntpUnixEpochSeconds + 10uL) shl 32 or 0x8000_0000uL),
        )
        assertTrue(isEqual(timestamp, 10.5, epsilon = 1e-9))
    }
}
