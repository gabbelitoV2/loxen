package com.moblin.android.media.haishinkit.util

data class BitrateStatsInstant(
    var total: ULong,
    var speed: ULong,
)

class BitrateStats(
    private val speedChangeRate: ULong = 100uL,
) {
    var totalBytes: ULong = 0uL
        private set

    private var previousTotalBytes: ULong = 0uL

    var latestSpeed: ULong = 0uL
        private set

    fun add(bytesTransferred: Int) {
        totalBytes += bytesTransferred.toULong()
    }

    fun update(): BitrateStatsInstant {
        val speed = totalBytes - previousTotalBytes
        latestSpeed = (speedChangeRate * speed + (100uL - speedChangeRate) * latestSpeed) / 100uL
        previousTotalBytes = totalBytes
        return BitrateStatsInstant(total = totalBytes, speed = latestSpeed)
    }
}
