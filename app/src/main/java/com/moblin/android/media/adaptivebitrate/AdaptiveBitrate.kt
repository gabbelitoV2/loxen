package com.moblin.android.media.adaptivebitrate

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val adaptiveBitrateStart: Long = 1_000_000

const val adaptiveBitrateTransportMinimum: Long = adaptiveBitrateStart

interface AdaptiveBitrateDelegate {
    fun adaptiveBitrateSetVideoStreamBitrate(bitrate: Int)
}

data class StreamStats(
    val rttMs: Double,
    val packetsInFlight: Double,
    val transportBitrate: Long?,
    val latency: Int?,
    val mbpsSendRate: Double?,
    val relaxed: Boolean?,
) {
    fun limitByTransportBitrate(bitrate: Long): Long {
        val transportBitrate = transportBitrate ?: return bitrate
        val maximumBitrate = maxOf(
            transportBitrate + adaptiveBitrateTransportMinimum,
            (17 * transportBitrate) / 10,
        )
        return minOf(bitrate, maximumBitrate)
    }
}

data class AdaptiveBitrateSettings(
    var packetsInFlight: Long,
    var rttDiffHighFactor: Double,
    var rttDiffHighAllowedSpike: Double,
    var rttDiffHighMinDecrease: Long,
    var pifDiffIncreaseFactor: Long,
    var minimumBitrate: Long,
)

private class ActionTaken(val message: String) {
    val timestamp: Long = System.nanoTime()
}

open class AdaptiveBitrate(delegate: AdaptiveBitrateDelegate) {
    val delegate: AdaptiveBitrateDelegate? = delegate
    private val actionsTaken: ArrayDeque<ActionTaken> = ArrayDeque()
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    open fun setTargetBitrate(bitrate: Int) {}

    open fun setSettings(settings: AdaptiveBitrateSettings) {}

    open fun getCurrentBitrate(): Int = 0

    fun getCurrentBitrateInKbps(): Long = (getCurrentBitrate() / 1000).toLong()

    open fun getCurrentMaximumBitrateInKbps(): Long = 0

    open fun getFastPif(): Long = 0

    open fun getSmoothPif(): Long = 0

    open fun update(stats: StreamStats) {
        removeOldActionsTaken()
    }

    open fun getActionsTaken(): List<String> {
        return actionsTaken.map { it.message }
    }

    fun logAdaptiveAcion(actionTaken: String) {
        Log.d(TAG, "adaptive-bitrate: $actionTaken")
        val dateString = dateFormatter.format(Instant.now())
        actionsTaken.addLast(ActionTaken(dateString + " " + actionTaken))
        while (actionsTaken.size > 6) {
            actionsTaken.removeFirst()
        }
    }

    private fun removeOldActionsTaken() {
        val now = System.nanoTime()
        while (true) {
            val actionTaken = actionsTaken.firstOrNull() ?: break
            if (now - actionTaken.timestamp > 15_000_000_000L) {
                actionsTaken.removeFirst()
            } else {
                break
            }
        }
    }

    private companion object {
        const val TAG = "AdaptiveBitrate"
    }
}
