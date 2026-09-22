package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.common.various.sleep
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RtmpStreamInfoSuite {
    @Test
    fun initialState() {
        val info = RtmpStreamInfo()
        assertEquals(0, info.bitrateStats.value.totalBytes)
        assertEquals(0, info.bitrateStats.value.latestSpeed)
        assertEquals(0, info.stats.value.rttMs)
        assertEquals(0, info.stats.value.packetsInFlight)
    }

    @Test
    fun clearResetsState() {
        val info = RtmpStreamInfo()
        info.bitrateStats.mutate { it.add(bytesTransferred = 5000) }
        info.onTimeout()
        info.onWritten(sequence = 1400)
        info.clear()
        assertEquals(5000, info.bitrateStats.value.totalBytes)
        assertEquals(1500, info.bitrateStats.value.latestSpeed)
        assertEquals(0, info.stats.value.rttMs)
        assertEquals(0, info.stats.value.packetsInFlight)
    }

    @Test
    fun onTimeoutCalculatesBytesPerSecond() {
        val info = RtmpStreamInfo()
        info.bitrateStats.mutate { it.add(bytesTransferred = 1000) }
        info.onTimeout()
        assertEquals(300, info.bitrateStats.value.latestSpeed)
    }

    @Test
    fun onTimeoutExponentialSmoothing() {
        val info = RtmpStreamInfo()
        info.bitrateStats.mutate { it.add(bytesTransferred = 1000) }
        info.onTimeout()
        assertEquals(300, info.bitrateStats.value.latestSpeed)
        info.bitrateStats.mutate { it.add(bytesTransferred = 1000) }
        info.onTimeout()
        assertEquals(510, info.bitrateStats.value.latestSpeed)
        info.onTimeout()
        assertEquals(357, info.bitrateStats.value.latestSpeed)
    }

    @Test
    fun onTimeoutNoNewBytes() {
        val info = RtmpStreamInfo()
        info.onTimeout()
        assertEquals(0, info.bitrateStats.value.latestSpeed)
        info.onTimeout()
        assertEquals(0, info.bitrateStats.value.latestSpeed)
    }

    @Test
    fun onWrittenUpdatesPacketsInFlight() {
        val info = RtmpStreamInfo()
        info.onWritten(sequence = 1400)
        assertEquals(1, info.stats.value.packetsInFlight)
        info.onWritten(sequence = 4200)
        assertEquals(3, info.stats.value.packetsInFlight)
    }

    @Test
    fun onAckReducesPacketsInFlight() {
        val info = RtmpStreamInfo()
        info.onWritten(sequence = 1400)
        assertEquals(1, info.stats.value.packetsInFlight)
        info.onWritten(sequence = 2800)
        assertEquals(2, info.stats.value.packetsInFlight)
        info.onWritten(sequence = 4200)
        assertEquals(3, info.stats.value.packetsInFlight)
        info.onAck(sequence = 2800u)
        assertEquals(1, info.stats.value.packetsInFlight)
        info.onAck(sequence = 4201u)
        assertEquals(0, info.stats.value.packetsInFlight)
    }

    @Test
    fun onAckUpdatesRtt() {
        runBlocking {
            val info = RtmpStreamInfo()
            info.onWritten(sequence = 1400)
            sleep(200)
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
        assertEquals(2, info.stats.value.packetsInFlight)
    }

    @Test
    fun onAckSequenceRolloverAtUInt32Max() {
        val info = RtmpStreamInfo()
        val aboveInt32Max = Int.MAX_VALUE.toUInt() + 1000u
        info.onWritten(sequence = aboveInt32Max.toLong() - 500)
        info.onWritten(sequence = UInt.MAX_VALUE.toLong() + 5000)
        info.onAck(sequence = aboveInt32Max - 600u)
        info.onAck(sequence = 1000u)
        assertEquals(2, info.stats.value.packetsInFlight)
    }

    @Test
    fun packetsInFlightNeverNegative() {
        val info = RtmpStreamInfo()
        info.onAck(sequence = 5000u)
        info.onWritten(sequence = 1400)
        assertEquals(0, info.stats.value.packetsInFlight)
    }

    @Test
    fun multipleOnWrittenAccumulatesTimings() {
        val info = RtmpStreamInfo()
        for (i in 1..10) {
            info.onWritten(sequence = i.toLong() * 1400)
        }
        assertEquals(10, info.stats.value.packetsInFlight)
        info.onAck(sequence = 7000u)
        assertEquals(5, info.stats.value.packetsInFlight)
    }
}
