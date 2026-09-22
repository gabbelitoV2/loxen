package com.moblin.android.media.adaptivebitrate

import com.moblin.android.common.various.sleep
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class Handler : AdaptiveBitrateDelegate {
    val bitrates: ArrayDeque<Int> = ArrayDeque()

    override fun adaptiveBitrateSetVideoStreamBitrate(bitrate: Int) {
        bitrates.addLast(bitrate)
    }
}

private fun makeStats(bitrate: Long): StreamStats {
    return StreamStats(
        rttMs = 30,
        packetsInFlight = 15,
        transportBitrate = bitrate,
        latency = 3000,
        mbpsSendRate = bitrate.toDouble(),
        relaxed = false,
    )
}

private suspend fun update(belabox: AdaptiveBitrateSrtBelabox, bitrate: Long) {
    sleep(milliSeconds = 20)
    belabox.update(stats = makeStats(bitrate = bitrate))
}

class AdaptiveBitrateSuite {
    @Test
    fun belaboxStartAtLowerThanTarget() = runTest {
        val handler = Handler()
        val belabox = AdaptiveBitrateSrtBelabox(targetBitrate = 5_000_000, delegate = handler)
        belabox.setSettings(settings = adaptiveBitrateBelaboxSettings)
        assertEquals(1_000_000, belabox.getCurrentBitrate())
        assertEquals(1000, belabox.getCurrentMaximumBitrateInKbps())
        assertTrue(handler.bitrates.isEmpty())
        update(belabox = belabox, bitrate = 5_000_000)
        assertEquals(1_133_333, belabox.getCurrentBitrate())
    }

    @Test
    fun belaboxTransportBitrateLimit() = runTest {
        val handler = Handler()
        val belabox = AdaptiveBitrateSrtBelabox(targetBitrate = 5_000_000, delegate = handler)
        belabox.setSettings(settings = adaptiveBitrateBelaboxSettings)
        assertEquals(1_000_000, belabox.getCurrentBitrate())
        assertEquals(1000, belabox.getCurrentMaximumBitrateInKbps())
        assertTrue(handler.bitrates.isEmpty())
        val transportBitrate5Mbps: Long = 5_000_000
        while (belabox.getCurrentBitrate() != 5_000_000) {
            update(belabox = belabox, bitrate = transportBitrate5Mbps)
        }
        val transportBitrate1Mbps: Long = 1_000_000
        update(belabox = belabox, bitrate = transportBitrate1Mbps)
        assertEquals(2_000_000, belabox.getCurrentBitrate())
        assertEquals(2_000_000, handler.bitrates.last())
    }
}
