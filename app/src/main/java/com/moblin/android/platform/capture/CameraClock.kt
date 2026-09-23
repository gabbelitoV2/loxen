package com.moblin.android.platform.capture

import android.hardware.camera2.CameraMetadata
import android.os.SystemClock
import android.util.Log
import com.moblin.android.platform.core.HostClock
import kotlin.math.abs

private const val maximumDistanceNs = 1_000_000_000L

internal class CameraClock(private val timestampSource: Int) {
    private var isRealtimeBase = timestampSource == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME

    val sourceName: String
        get() = if (timestampSource == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME) "REALTIME" else "UNKNOWN"

    fun toPresentationTimeUs(timestampNs: Long): Long {
        val nowNs = HostClock.nowNs()
        val realtimeOffsetNs = SystemClock.elapsedRealtimeNanos() - nowNs
        val hostNs = if (isRealtimeBase) timestampNs - realtimeOffsetNs else timestampNs
        if (abs(hostNs - nowNs) <= maximumDistanceNs) {
            return hostNs / 1000
        }
        val otherHostNs = if (isRealtimeBase) timestampNs else timestampNs - realtimeOffsetNs
        if (abs(otherHostNs - nowNs) <= maximumDistanceNs) {
            isRealtimeBase = !isRealtimeBase
            Log.i("MoblinCamera", "Camera timestamps are in the ${if (isRealtimeBase) "REALTIME" else "MONOTONIC"} base")
            return otherHostNs / 1000
        }
        return nowNs / 1000
    }
}
