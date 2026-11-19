package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.util.calculateMd5Base64
import java.net.URI
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RtmpHandshakeSuite {
    @Test
    fun c0c1() {
        val packet = RtmpHandshake.createC0C1Packet()
        assertEquals(RtmpHandshake.sigSize + 1, packet.size)
        assertContentEquals(byteArrayOf(0x03, 0, 0, 0, 0, 0, 0, 0, 0), packet.copyOfRange(0, 9))
    }

    @Test
    fun c2EchoesServerTimestampAndRandomData() {
        val s0s1 = ByteArray(RtmpHandshake.sigSize + 1) { index ->
            if (index == 0) 0x03.toByte() else ((index - 1) % 256).toByte()
        }
        val packet = RtmpHandshake.createC2Packet(s0s1)
        assertEquals(RtmpHandshake.sigSize, packet.size)
        assertContentEquals(s0s1.copyOfRange(1, 5), packet.copyOfRange(0, 4))
        assertContentEquals(s0s1.copyOfRange(9, s0s1.size), packet.copyOfRange(8, packet.size))
    }

    @Test
    fun c2ContainsCurrentTime() {
        val s0s1 = ByteArray(RtmpHandshake.sigSize + 1)
        val before = System.currentTimeMillis() / 1000
        val packet = RtmpHandshake.createC2Packet(s0s1)
        val after = System.currentTimeMillis() / 1000
        val time = ((packet[4].toLong() and 0xFF) shl 24) or
            ((packet[5].toLong() and 0xFF) shl 16) or
            ((packet[6].toLong() and 0xFF) shl 8) or
            (packet[7].toLong() and 0xFF)
        assertTrue(time >= before)
        assertTrue(time <= after)
    }
}

class RtmpSanJoseAuthSuite {
    private val url = URI("rtmp://user:pass@foo.com/app")

    @Test
    fun withOpaque() {
        val command = makeSanJoseAuthCommand(
            url = url,
            description = "[ AccessManager.Reject ] : [ authmod=adobe ] : ?reason=needauth&user=user&salt=abc" +
                "&challenge=def&opaque=ghi",
        )
        val match = assertNotNull(
            Regex(
                """rtmp://user:pass@foo\.com/app&opaque=ghi&challenge=([0-9a-f]{8})&response=(.+)"""
            ).matchEntire(command)
        )
        val challenge = match.groupValues[1]
        val expected = calculateMd5Base64(calculateMd5Base64("userabcpass") + "ghi" + challenge)
        assertEquals(expected, match.groupValues[2])
    }

    @Test
    fun withChallengeWithoutOpaque() {
        val command = makeSanJoseAuthCommand(
            url = url,
            description = "?reason=needauth&user=user&salt=abc&challenge=def",
        )
        val match = assertNotNull(
            Regex(
                """rtmp://user:pass@foo\.com/app&challenge=([0-9a-f]{8})&response=(.+)"""
            ).matchEntire(command)
        )
        val challenge = match.groupValues[1]
        val expected = calculateMd5Base64(calculateMd5Base64("userabcpass") + "def" + challenge)
        assertEquals(expected, match.groupValues[2])
    }

    @Test
    fun withoutQuery() {
        assertEquals(url.toString(), makeSanJoseAuthCommand(url = url, description = "reason=needauth"))
    }

    @Test
    fun withoutSalt() {
        assertEquals(url.toString(), makeSanJoseAuthCommand(url = url, description = "?reason=needauth&challenge=def"))
    }

    @Test
    fun withoutPassword() {
        val url = URI("rtmp://user@foo.com/app")
        assertEquals(url.toString(), makeSanJoseAuthCommand(url = url, description = "?reason=needauth&salt=abc"))
    }
}
