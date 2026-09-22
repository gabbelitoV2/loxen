package com.moblin.android.media.haishinkit.rtmp

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RtmpStreamInfoSuite {
    @Test
    fun initialState() {
        val info = RtmpStreamInfo()
        assertEquals(0, info.bitrateStats.value.totalBytes.toInt())
        assertEquals(0, info.bitrateStats.value.latestSpeed.toInt())
        assertEquals(0, info.stats.value.rttMs.toInt())
        assertEquals(0, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun clearResetsState() {
        val info = RtmpStreamInfo()
        info.bitrateStats.value.add(bytesTransferred = 5000)
        info.onTimeout()
        info.onWritten(sequence = 1400)
        info.clear()
        assertEquals(5000, info.bitrateStats.value.totalBytes.toInt())
        assertEquals(1500, info.bitrateStats.value.latestSpeed.toInt())
        assertEquals(0, info.stats.value.rttMs.toInt())
        assertEquals(0, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun onTimeoutCalculatesBytesPerSecond() {
        val info = RtmpStreamInfo()
        info.bitrateStats.value.add(bytesTransferred = 1000)
        info.onTimeout()
        assertEquals(300, info.bitrateStats.value.latestSpeed.toInt())
    }

    @Test
    fun onTimeoutExponentialSmoothing() {
        val info = RtmpStreamInfo()
        info.bitrateStats.value.add(bytesTransferred = 1000)
        info.onTimeout()
        assertEquals(300, info.bitrateStats.value.latestSpeed.toInt())
        info.bitrateStats.value.add(bytesTransferred = 1000)
        info.onTimeout()
        assertEquals(510, info.bitrateStats.value.latestSpeed.toInt())
        info.onTimeout()
        assertEquals(357, info.bitrateStats.value.latestSpeed.toInt())
    }

    @Test
    fun onTimeoutNoNewBytes() {
        val info = RtmpStreamInfo()
        info.onTimeout()
        assertEquals(0, info.bitrateStats.value.latestSpeed.toInt())
        info.onTimeout()
        assertEquals(0, info.bitrateStats.value.latestSpeed.toInt())
    }

    @Test
    fun onWrittenUpdatesPacketsInFlight() {
        val info = RtmpStreamInfo()
        info.onWritten(sequence = 1400)
        assertEquals(1, info.stats.value.packetsInFlight.toInt())
        info.onWritten(sequence = 4200)
        assertEquals(3, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun onAckReducesPacketsInFlight() {
        val info = RtmpStreamInfo()
        info.onWritten(sequence = 1400)
        assertEquals(1, info.stats.value.packetsInFlight.toInt())
        info.onWritten(sequence = 2800)
        assertEquals(2, info.stats.value.packetsInFlight.toInt())
        info.onWritten(sequence = 4200)
        assertEquals(3, info.stats.value.packetsInFlight.toInt())
        info.onAck(sequence = 2800u)
        assertEquals(1, info.stats.value.packetsInFlight.toInt())
        info.onAck(sequence = 4201u)
        assertEquals(0, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun onAckUpdatesRtt() {
        runBlocking {
            val info = RtmpStreamInfo()
            info.onWritten(sequence = 1400)
            Thread.sleep(200)
            info.onAck(sequence = 1401u)
            assertTrue(info.stats.value.rttMs > 0)
        }
    }

    @Test
    fun onAckSequenceRolloverAtInt32Max() {
        val info = RtmpStreamInfo()
        info.onWritten(sequence = Int.MAX_VALUE.toLong() - 500)
        info.onWritten(sequence = Int.MAX_VALUE.toLong() + 5000)
        info.onAck(sequence = Int.MAX_VALUE.toUInt() - 600u)
        info.onAck(sequence = 1000u)
        assertEquals(2, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun onAckSequenceRolloverAtUInt32Max() {
        val info = RtmpStreamInfo()
        val aboveInt32Max = Int.MAX_VALUE.toUInt() + 1000u
        info.onWritten(sequence = aboveInt32Max.toLong() - 500)
        info.onWritten(sequence = UInt.MAX_VALUE.toLong() + 5000)
        info.onAck(sequence = aboveInt32Max - 600u)
        info.onAck(sequence = 1000u)
        assertEquals(2, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun packetsInFlightNeverNegative() {
        val info = RtmpStreamInfo()
        info.onAck(sequence = 5000u)
        info.onWritten(sequence = 1400)
        assertEquals(0, info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun multipleOnWrittenAccumulatesTimings() {
        val info = RtmpStreamInfo()
        for (i in 1..10) {
            info.onWritten(sequence = i.toLong() * 1400)
        }
        assertEquals(10, info.stats.value.packetsInFlight.toInt())
        info.onAck(sequence = 7000u)
        assertEquals(5, info.stats.value.packetsInFlight.toInt())
    }
}
