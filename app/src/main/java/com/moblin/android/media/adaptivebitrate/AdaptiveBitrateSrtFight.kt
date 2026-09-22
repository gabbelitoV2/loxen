package com.moblin.android.media.adaptivebitrate

import android.util.Log

val adaptiveBitrateFastSettings = AdaptiveBitrateSettings(
    packetsInFlight = 200L,
    rttDiffHighFactor = 0.9,
    rttDiffHighAllowedSpike = 50.0,
    rttDiffHighMinDecrease = 250_000L,
    pifDiffIncreaseFactor = 100_000L,
    minimumBitrate = 50000L
)

val adaptiveBitrateSlowSettings = AdaptiveBitrateSettings(
    packetsInFlight = 500L,
    rttDiffHighFactor = 0.95,
    rttDiffHighAllowedSpike = 100.0,
    rttDiffHighMinDecrease = 100_000L,
    pifDiffIncreaseFactor = 25000L,
    minimumBitrate = 50000L
)

class AdaptiveBitrateSrtFight(
    targetBitrate: Int,
    private val delegate: AdaptiveBitrateDelegate,
    private val rttMax: Double = 250.0,
    private val pifMax: Double = 100.0
) {
    private val base = AdaptiveBitrate(delegate)
    private var avgRtt: Double = 0.0
    private var fastRtt: Double = 0.0
    private var currentBitrate: Long = adaptiveBitrateStart
    private var previousBitrate: Long = adaptiveBitrateStart
    private var targetBitrate: Long = targetBitrate.toLong()
    private var currentMaximumBitrate: Long = adaptiveBitrateStart
    private var smoothPif: Double = 0.0
    private var fastPif: Double = 0.0
    private var settings: AdaptiveBitrateSettings = adaptiveBitrateFastSettings

    fun setTargetBitrate(bitrate: Int) {
        targetBitrate = bitrate.toLong()
    }

    fun setSettings(settings: AdaptiveBitrateSettings) {
        Log.i("AdaptiveBitrateSrtFight", "adaptive-bitrate: Using settings $settings")
        this.settings = settings
    }

    fun getCurrentBitrate(): Int {
        return currentBitrate.toInt()
    }

    fun getCurrentMaximumBitrateInKbps(): Long {
        return currentMaximumBitrate / 1000
    }

    fun getFastPif(): Long {
        return fastPif.toLong()
    }

    fun getSmoothPif(): Long {
        return smoothPif.toLong()
    }

    fun update(stats: StreamStats) {
        calcPifs(stats)
        calcRtts(stats)
        increaseCurrentMaxBitrate(stats, allowedRttJitter = 15.0, allowedPifJitter = 10.0)
        decreaseMaxRateIfPifIsHigh(factor = 0.9, minimumDecrease = 250_000L)
        decreaseMaxRateIfRttIsHigh(factor = 0.9, minimumDecrease = 250_000L)
        decreaseMaxRateIfRttDiffIsHigh(
            stats,
            factor = settings.rttDiffHighFactor,
            rttSpikeAllowed = settings.rttDiffHighAllowedSpike,
            minimumDecrease = settings.rttDiffHighMinDecrease
        )
        calculateCurrentBitrate(stats)
        if (previousBitrate != currentBitrate) {
            delegate?.adaptiveBitrateSetVideoStreamBitrate(bitrate = currentBitrate.toInt())
            previousBitrate = currentBitrate
        }
        base.update(stats)
    }

    private fun calcPifs(stats: StreamStats) {
        if (stats.packetsInFlight > smoothPif) {
            smoothPif *= 0.97
            smoothPif += stats.packetsInFlight * 0.03
        } else {
            smoothPif *= 0.9
            smoothPif += stats.packetsInFlight * 0.1
        }
        fastPif *= 0.67
        fastPif += stats.packetsInFlight * 0.33
    }

    private fun calcRtts(stats: StreamStats) {
        if (avgRtt < 1.0) {
            avgRtt = stats.rttMs
        }
        if (avgRtt > stats.rttMs) {
            avgRtt *= 0.60
            avgRtt += stats.rttMs * 0.40
        } else {
            avgRtt *= 0.96
            if (stats.rttMs < 450.0) {
                avgRtt += stats.rttMs * 0.04
            } else {
                avgRtt += 450 * 0.001
            }
        }
        if (fastRtt > stats.rttMs) {
            fastRtt *= 0.70
            fastRtt += stats.rttMs * 0.30
        } else {
            fastRtt *= 0.90
            fastRtt += stats.rttMs * 0.10
        }
        if (avgRtt > 450.0) {
            avgRtt = 450.0
        }
    }

    private fun increaseCurrentMaxBitrate(
        stats: StreamStats,
        allowedRttJitter: Double,
        allowedPifJitter: Double
    ) {
        var pifSpikeDiff = (stats.packetsInFlight - smoothPif).toLong()
        if (pifSpikeDiff < 0) {
            pifSpikeDiff = 0
        }
        if (pifSpikeDiff > settings.packetsInFlight) {
            pifSpikeDiff = settings.packetsInFlight
        }
        val pifDiffThing = settings.packetsInFlight - pifSpikeDiff
        if (smoothPif < settings.packetsInFlight.toDouble() && fastRtt <= avgRtt + allowedRttJitter) {
            if (stats.packetsInFlight - smoothPif < allowedPifJitter) {
                currentMaximumBitrate +=
                    (settings.pifDiffIncreaseFactor * pifDiffThing) / settings.packetsInFlight
                if (currentMaximumBitrate > targetBitrate) {
                    currentMaximumBitrate = targetBitrate
                }
            }
        }
    }

    private fun decreaseMaxRateIfPifIsHigh(factor: Double, minimumDecrease: Long) {
        if (smoothPif <= pifMax) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        base.logAdaptiveAcion(
            actionTaken = "PIF: Decreasing bitrate by ${decrease / 1000}k, " +
                "smooth ${smoothPif.toInt()} > max ${pifMax.toInt()}"
        )
    }

    private fun decreaseMaxRateIfRttIsHigh(factor: Double, minimumDecrease: Long) {
        if (avgRtt <= rttMax) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        base.logAdaptiveAcion(
            actionTaken = "RTT: Decrease bitrate by $decrease, avg $avgRtt > max $rttMax"
        )
    }

    private fun decreaseMaxRateIfRttDiffIsHigh(
        stats: StreamStats,
        factor: Double,
        rttSpikeAllowed: Double,
        minimumDecrease: Long
    ) {
        if (stats.rttMs <= avgRtt + rttSpikeAllowed) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        base.logAdaptiveAcion(
            actionTaken = "RTT: Decreasing bitrate by ${decrease / 1000}k, " +
                "${stats.rttMs.toInt()} > avg + allow ${avgRtt.toInt()} + ${rttSpikeAllowed.toInt()}"
        )
    }

    private fun calculateCurrentBitrate(stats: StreamStats) {
        var pifSpikeDiff = fastPif.toLong() - smoothPif.toLong()
        if (pifSpikeDiff > settings.packetsInFlight) {
            base.logAdaptiveAcion(
                actionTaken = "PIF: Lazy decrease diff $pifSpikeDiff > ${settings.packetsInFlight}"
            )
            currentMaximumBitrate = (currentMaximumBitrate.toDouble() * 0.95).toLong()
        }
        if (pifSpikeDiff <= settings.packetsInFlight / 5) {
            pifSpikeDiff = 0
        }
        if (pifSpikeDiff < 0) {
            pifSpikeDiff = 0
        }
        if (pifSpikeDiff > settings.packetsInFlight) {
            pifSpikeDiff = settings.packetsInFlight
        }
        if (pifSpikeDiff == settings.packetsInFlight) {
            currentMaximumBitrate -= 500_000
            base.logAdaptiveAcion(
                actionTaken = "PIF: -500 dec diff $pifSpikeDiff == ${settings.packetsInFlight}"
            )
        }
        val pifDiffThing = settings.packetsInFlight - pifSpikeDiff
        currentMaximumBitrate = stats.limitByTransportBitrate(bitrate = currentMaximumBitrate)
        val minimumBitrate = maxOf(50000L, settings.minimumBitrate)
        if (currentMaximumBitrate < minimumBitrate) {
            currentMaximumBitrate = minimumBitrate
        }
        var tempBitrate = currentMaximumBitrate
        tempBitrate *= pifDiffThing
        tempBitrate /= settings.packetsInFlight
        currentBitrate = tempBitrate
        if (currentBitrate < settings.minimumBitrate) {
            currentBitrate = settings.minimumBitrate
        }
        if ((fastPif - smoothPif).toInt().toLong() > settings.packetsInFlight * 2) {
            currentBitrate = settings.minimumBitrate
        }
    }
}
