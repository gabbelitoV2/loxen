package com.moblin.android.media.adaptivebitrate

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.formatTwoDecimals
import com.moblin.android.various.settings.defaultSrtLatency

private val bitrateIncrMin: Long = (100 * 1000).toLong()
private val bitrateIncrInterval: Long = 400_000_000L
private val bitrateIncrScale: Long = 30L
private val bitrateDecrMin: Long = (100 * 1000).toLong()
private val bitrateDecrInterval: Long = 200_000_000L
private val bitrateDecrFastInterval: Long = 250_000_000L
private val bitrateDecrScale: Long = 10L

val adaptiveBitrateBelaboxSettings = AdaptiveBitrateSettings(
    packetsInFlight = 200,
    rttDiffHighFactor = 0.9,
    rttDiffHighAllowedSpike = 50.0,
    rttDiffHighMinDecrease = 250_000L,
    pifDiffIncreaseFactor = 100_000L,
    minimumBitrate = 250_000L,
)

class AdaptiveBitrateSrtBelabox(targetBitrate: Int, delegate: AdaptiveBitrateDelegate) : AdaptiveBitrate(delegate) {
    private var targetBitrate: Long = targetBitrate.toLong()
    private var settings = adaptiveBitrateBelaboxSettings
    private var sendBufferSizeAverage: Double = 0.0
    private var sendBufferSizeJitter: Double = 0.0
    private var prevSendBufferSize: Double = 0.0
    private var rttAverage: Double = 0.0
    private var rttAverageDelta: Double = 0.0
    private var prevRtt: Double = 300.0
    private var rttMin: Double = 200.0
    private var rttJitter: Double = 0.0
    private var throughput: Double = 0.0
    private var nextBitrateIncrTime: Long = System.nanoTime()
    private var nextBitrateDecrTime: Long = System.nanoTime()
    private var currentBitrate: Long = adaptiveBitrateStart.toLong()

    override fun setTargetBitrate(bitrate: Int) {
        targetBitrate = bitrate.toLong()
    }

    override fun setSettings(settings: AdaptiveBitrateSettings) {
        Log.i("AdaptiveBitrateSrtBelabox", "adaptive-bitrate: Using settings $settings")
        this.settings = settings
    }

    override fun getCurrentBitrate(): Int {
        return currentBitrate.toInt()
    }

    override fun getCurrentMaximumBitrateInKbps(): Long {
        return currentBitrate / 1000
    }

    private fun rttToSendBufferSize(rtt: Double, throughput: Double): Double {
        return (throughput / 8) * rtt / 1316
    }

    private fun updateSendBufferSizeAverage(sendBufferSize: Double) {
        sendBufferSizeAverage = sendBufferSizeAverage * 0.99 + sendBufferSize * 0.01
    }

    private fun updateSendBufferSizeJitter(sendBufferSize: Double) {
        sendBufferSizeJitter = 0.99 * sendBufferSizeJitter
        val deltaSendBufferSize = sendBufferSize - prevSendBufferSize
        if (deltaSendBufferSize > sendBufferSizeJitter) {
            sendBufferSizeJitter = deltaSendBufferSize
        }
        prevSendBufferSize = sendBufferSize
    }

    private fun updateRttAverage(rtt: Double) {
        if (rttAverage == 0.0) {
            rttAverage = rtt
        } else {
            rttAverage = rttAverage * 0.99 + 0.01 * rtt
        }
    }

    private fun updateAverageRttDelta(rtt: Double): Double {
        val deltaRtt = rtt - prevRtt
        rttAverageDelta = rttAverageDelta * 0.8 + deltaRtt * 0.2
        prevRtt = rtt
        return deltaRtt
    }

    private fun updateRttMin(rtt: Double) {
        rttMin *= 1.001
        if (rtt != 100.0 && rtt < rttMin && rttAverageDelta < 1.0) {
            rttMin = rtt
        }
    }

    private fun updateRttJitter(deltaRtt: Double) {
        rttJitter *= 0.99
        if (deltaRtt > rttJitter) {
            rttJitter = deltaRtt
        }
    }

    private fun updateThroughput(mbpsSendRate: Double) {
        throughput *= 0.97
        throughput += (mbpsSendRate * 1000.0 * 1000.0 / 1024.0) * 0.03
    }

    private fun updateBitrate(stats: StreamStats) {
        if (stats.rttMs == 0.0) {
            return
        }
        val sendBufferSize = stats.packetsInFlight
        updateSendBufferSizeAverage(sendBufferSize)
        updateSendBufferSizeJitter(sendBufferSize)
        val rtt = stats.rttMs
        updateRttAverage(rtt)
        val deltaRtt = updateAverageRttDelta(rtt)
        updateRttMin(rtt)
        updateRttJitter(deltaRtt)
        updateThroughput(stats.mbpsSendRate!!)
        val srtLatency = stats.latency?.toDouble() ?: defaultSrtLatency.toDouble()
        val currentTime = System.nanoTime()
        var bitrate = currentBitrate
        val sendBufferSizeTh3 = (sendBufferSizeAverage + sendBufferSizeJitter) * 4
        var sendBufferSizeTh2 = maxOf(
            50.0,
            sendBufferSizeAverage + maxOf(sendBufferSizeJitter * 3.0, sendBufferSizeAverage),
        )
        sendBufferSizeTh2 = minOf(
            sendBufferSizeTh2,
            rttToSendBufferSize(srtLatency / 2, throughput),
        )
        if (stats.relaxed ?: false) {
            sendBufferSizeTh2 *= 2
        }
        val sendBufferSizeTh1 = maxOf(50.0, sendBufferSizeAverage + sendBufferSizeJitter * 2.5)
        val rttThMax = rttAverage + maxOf(rttJitter * 4, rttAverage * 15 / 100)
        val rttThMin = rttMin + maxOf(1.0, rttJitter * 2)
        if (bitrate > settings.minimumBitrate &&
            (rtt >= (srtLatency / 3) || sendBufferSize > sendBufferSizeTh3)
        ) {
            bitrate = settings.minimumBitrate
            nextBitrateDecrTime = currentTime + bitrateDecrInterval
            logAdaptiveAcion(
                actionTaken =
                    "Set min: ${bitrate / 1000}, rtt: $rtt >= latency / 3: " +
                        "${srtLatency / 3} or bs: $sendBufferSize > bs_th3: " +
                        "${formatTwoDecimals(sendBufferSizeTh3)}",
            )
        } else if (currentTime > nextBitrateDecrTime &&
            (rtt > (srtLatency / 5) || sendBufferSize > sendBufferSizeTh2)
        ) {
            bitrate -= (bitrateDecrMin + bitrate / bitrateDecrScale)
            nextBitrateDecrTime = currentTime + bitrateDecrFastInterval
            logAdaptiveAcion(
                actionTaken =
                    "Fast decr: ${(bitrateDecrMin + bitrate / bitrateDecrScale) / 1000}, " +
                        "rtt: $rtt > latency / 5: ${srtLatency / 5} or bs: " +
                        "$sendBufferSize > bs_th2: ${formatTwoDecimals(sendBufferSizeTh2)}",
            )
        } else if (currentTime > nextBitrateDecrTime &&
            (rtt > rttThMax || sendBufferSize > sendBufferSizeTh1)
        ) {
            bitrate -= bitrateDecrMin
            nextBitrateDecrTime = currentTime + bitrateDecrInterval
            logAdaptiveAcion(
                actionTaken =
                    "Decr: ${bitrateDecrMin / 1000}, rtt: $rtt > rtt_th_max: " +
                        "${formatTwoDecimals(rttThMax)} or bs: $sendBufferSize > bs_th1: " +
                        "${formatTwoDecimals(sendBufferSizeTh1)}",
            )
        } else if (currentTime > nextBitrateIncrTime && rtt < rttThMin && rttAverageDelta < 0.01) {
            bitrate += bitrateIncrMin + bitrate / bitrateIncrScale
            nextBitrateIncrTime = currentTime + bitrateIncrInterval
        }
        bitrate = stats.limitByTransportBitrate(bitrate)
        bitrate = maxOf(minOf(bitrate, targetBitrate), settings.minimumBitrate)
        if (bitrate != currentBitrate) {
            currentBitrate = bitrate
            delegate?.adaptiveBitrateSetVideoStreamBitrate(bitrate.toInt())
        }
    }

    override fun update(stats: StreamStats) {
        updateBitrate(stats)
        super.update(stats)
    }
}
