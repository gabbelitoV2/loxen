package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats

private data class SendTiming(
    val timestamp: Long,
    val sequence: Long,
)

data class RtmpStreamStats(
    var rttMs: Double = 0.0,
    var packetsInFlight: UInt = 0u,
)

class RtmpStreamInfo {
    var bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats(speedChangeRate = 30uL))
    var stats: Atomic<RtmpStreamStats> = Atomic(RtmpStreamStats())
        private set
    private var sendTimings: ArrayDeque<SendTiming> = ArrayDeque()
    private var latestWrittenSequence: Long = 0
    private var latestAckedSequenceLow: UInt = 0u
    private var latestAckedSequenceHigh: Long = 0

    fun clear() {
        stats.mutate { it.value = RtmpStreamStats() }
        sendTimings.clear()
        latestWrittenSequence = 0
        latestAckedSequenceLow = 0u
        latestAckedSequenceHigh = 0
    }

    fun onTimeout() {
        bitrateStats.mutate { value ->
            value.value.update()
            value
        }
    }

    fun onWritten(sequence: Long) {
        latestWrittenSequence = sequence
        if (sendTimings.size < 500) {
            sendTimings.addLast(SendTiming(System.nanoTime(), sequence))
        }
        val packetsInFlight = packetsInFlight()
        stats.mutate { value ->
            value.value.packetsInFlight = packetsInFlight
            value
        }
    }

    fun onAck(sequence: UInt) {
        if (sequence < latestAckedSequenceLow) {
            if (latestAckedSequenceLow <= Int.MAX_VALUE.toUInt()) {
                latestAckedSequenceHigh += Int.MAX_VALUE.toLong() + 1
            } else {
                latestAckedSequenceHigh += UInt.MAX_VALUE.toLong() + 1
            }
        }
        latestAckedSequenceLow = sequence
        var ackedSendTiming: SendTiming? = null
        while (true) {
            val sendTiming = sendTimings.firstOrNull() ?: break
            if (latestAckedSequence() > sendTiming.sequence) {
                ackedSendTiming = sendTiming
                sendTimings.removeFirst()
            } else {
                break
            }
        }
        ackedSendTiming?.let { timing ->
            val rttMs = (System.nanoTime() - timing.timestamp) / 1_000_000.0
            val packetsInFlight = packetsInFlight()
            stats.mutate { value ->
                value.value.rttMs = rttMs
                value.value.packetsInFlight = packetsInFlight
                value
            }
        }
    }

    private fun latestAckedSequence(): Long {
        return latestAckedSequenceHigh + latestAckedSequenceLow.toLong()
    }

    private fun packetsInFlight(): UInt {
        return (
            (latestWrittenSequence - latestAckedSequence())
                .coerceIn(0L, UInt.MAX_VALUE.toLong()) / 1400
            ).toUInt()
    }
}
