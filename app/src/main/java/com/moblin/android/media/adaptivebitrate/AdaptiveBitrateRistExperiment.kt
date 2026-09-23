package com.moblin.android.media.adaptivebitrate

import android.util.Log

val adaptiveBitrateRistFastSettings = AdaptiveBitrateSettings(
    packetsInFlight = 200L,
    rttDiffHighFactor = 0.9,
    rttDiffHighAllowedSpike = 50.0,
    rttDiffHighMinDecrease = 250_000L,
    pifDiffIncreaseFactor = 100_000L,
    minimumBitrate = 50_000L,
)

class AdaptiveBitrateRistExperiment(
    targetBitrate: Int,
    delegate: AdaptiveBitrateDelegate,
) : AdaptiveBitrate(delegate) {
    private var avgRtt: Double = 0.0
    private var fastRtt: Double = 0.0
    private var currentBitrate: Long
    private var previousBitrate: Long
    private var targetBitrate: Long
    private var currentMaximumBitrate: Long
    private var smoothPif: Double = 0.0
    private var fastPif: Double = 0.0
    private var settings: AdaptiveBitrateSettings = adaptiveBitrateFastSettings

    init {
        this.targetBitrate = targetBitrate.toLong()
        currentBitrate = adaptiveBitrateStart
        previousBitrate = adaptiveBitrateStart
        currentMaximumBitrate = adaptiveBitrateStart
    }

    override fun setTargetBitrate(bitrate: Int) {
        targetBitrate = bitrate.toLong()
    }

    override fun setSettings(settings: AdaptiveBitrateSettings) {
        Log.i(
            "AdaptiveBitrateRistExperiment",
            "adaptive-bitrate-rist-experiment: Using settings $settings",
        )
        this.settings = settings
    }

    override fun getCurrentBitrate(): Int {
        return currentBitrate.toInt()
    }

    override fun getCurrentMaximumBitrateInKbps(): Long {
        return currentMaximumBitrate / 1000
    }

    override fun getFastPif(): Long {
        return fastPif.toLong()
    }

    override fun getSmoothPif(): Long {
        return smoothPif.toLong()
    }

    override fun update(stats: StreamStats) {
        calcPifs(stats)
        calcRtts(stats)
        increaseCurrentMaxBitrate(stats, allowedRttJitter = 15.0, allowedPifJitter = 10.0)
        decreaseMaxRateIfPifIsHigh(factor = 0.9, pifMax = 100.0, minimumDecrease = 250_000L)
        decreaseMaxRateIfRttIsHigh(factor = 0.9, rttMax = 250.0, minimumDecrease = 250_000L)
        decreaseMaxRateIfRttDiffIsHigh(
            stats,
            factor = settings.rttDiffHighFactor,
            rttSpikeAllowed = settings.rttDiffHighAllowedSpike,
            minimumDecrease = settings.rttDiffHighMinDecrease,
        )
        calculateCurrentBitrate(stats)
        if (previousBitrate != currentBitrate) {
            delegate?.adaptiveBitrateSetVideoStreamBitrate(currentBitrate.toInt())
            previousBitrate = currentBitrate
        }
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
        if (avgRtt < 1) {
            avgRtt = stats.rttMs
        }
        if (avgRtt > stats.rttMs) {
            avgRtt *= 0.60
            avgRtt += stats.rttMs * 0.40
        } else {
            avgRtt *= 0.96
            if (stats.rttMs < 450) {
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
        if (avgRtt > 450) {
            avgRtt = 450.0
        }
    }

    private fun increaseCurrentMaxBitrate(
        stats: StreamStats,
        allowedRttJitter: Double,
        allowedPifJitter: Double,
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
                currentMaximumBitrate += (settings.pifDiffIncreaseFactor * pifDiffThing) /
                    settings.packetsInFlight
                if (currentMaximumBitrate > targetBitrate) {
                    currentMaximumBitrate = targetBitrate
                }
            }
        }
    }

    private fun decreaseMaxRateIfPifIsHigh(factor: Double, pifMax: Double, minimumDecrease: Long) {
        if (smoothPif <= pifMax) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        logAdaptiveAcion(
            "PIF: Decreasing bitrate by ${decrease / 1000}k, " +
                "smooth ${smoothPif.toInt()} > max ${pifMax.toInt()}",
        )
    }

    private fun decreaseMaxRateIfRttIsHigh(factor: Double, rttMax: Double, minimumDecrease: Long) {
        if (avgRtt <= rttMax) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        logAdaptiveAcion("RTT: Decrease bitrate by $decrease, avg $avgRtt > max $rttMax")
    }

    private fun decreaseMaxRateIfRttDiffIsHigh(
        stats: StreamStats,
        factor: Double,
        rttSpikeAllowed: Double,
        minimumDecrease: Long,
    ) {
        if (stats.rttMs <= avgRtt + rttSpikeAllowed) {
            return
        }
        val factorDecrease = (currentMaximumBitrate.toDouble() * (1 - factor)).toLong()
        val decrease = maxOf(factorDecrease, minimumDecrease)
        currentMaximumBitrate -= decrease
        logAdaptiveAcion(
            "RTT: Decreasing bitrate by ${decrease / 1000}k, " +
                "${stats.rttMs.toInt()} > avg + allow ${avgRtt.toInt()} + " +
                "${rttSpikeAllowed.toInt()}",
        )
    }

    private fun calculateCurrentBitrate(stats: StreamStats) {
        var pifSpikeDiff = fastPif.toLong() - smoothPif.toLong()
        if (pifSpikeDiff > settings.packetsInFlight) {
            logAdaptiveAcion("PIF: Lazy decrease diff $pifSpikeDiff > ${settings.packetsInFlight}")
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
            currentMaximumBitrate -= 500_000L
            logAdaptiveAcion("PIF: -500 dec diff $pifSpikeDiff == ${settings.packetsInFlight}")
        }
        val pifDiffThing = settings.packetsInFlight - pifSpikeDiff
        currentMaximumBitrate = stats.limitByTransportBitrate(currentMaximumBitrate)
        val minimumBitrate = maxOf(50_000L, settings.minimumBitrate)
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
        if ((fastPif - smoothPif).toLong() > settings.packetsInFlight * 2) {
            currentBitrate = settings.minimumBitrate
        }
    }
}
