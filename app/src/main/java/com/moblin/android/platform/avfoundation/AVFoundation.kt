package com.moblin.android.platform.avfoundation

import android.media.MediaFormat
import com.moblin.android.platform.core.HostClock

enum class AVMediaType {
    video,
    audio,
}

object AVCaptureVideoOrientation {
    const val portrait = 1
    const val portraitUpsideDown = 2
    const val landscapeRight = 3
    const val landscapeLeft = 4
}

object AVCaptureColorSpace {
    const val sRGB = MediaFormat.COLOR_STANDARD_BT709
    const val HLG_BT2020 = MediaFormat.COLOR_STANDARD_BT2020
    const val P3_D65 = 100
    const val appleLog = 101
}

object AVCaptureVideoStabilizationMode {
    const val off = 0
    const val standard = 1
    const val cinematic = 2
    const val cinematicExtended = 3
    const val previewOptimized = 4
    const val cinematicExtendedEnhanced = 5
    const val auto = -1
}

enum class AVCaptureAspectRatio {
    ratio16x9,
    ratio9x16,
    ratio4x3,
    ratio3x4,
    ratio1x1,
}

const val AVCaptureSessionRuntimeError = "AVCaptureSessionRuntimeError"
const val AVCaptureSessionWasInterrupted = "AVCaptureSessionWasInterrupted"
const val AVCaptureSessionInterruptionEnded = "AVCaptureSessionInterruptionEnded"
const val AVCaptureSessionErrorKey = "AVCaptureSessionErrorKey"
const val AVCaptureSessionInterruptionReasonKey = "AVCaptureSessionInterruptionReasonKey"

object AVCaptureSessionInterruptionReason {
    const val videoDeviceNotAvailableInBackground = 1
    const val audioDeviceInUseByAnotherClient = 2
    const val videoDeviceInUseByAnotherClient = 3
    const val videoDeviceNotAvailableWithMultipleForegroundApps = 4
    const val videoDeviceNotAvailableDueToSystemPressure = 5
}

class AVError(val code: Int, val localizedFailureReason: String?) : Exception(localizedFailureReason) {
    companion object {
        const val unknown = -11800
        const val deviceNotConnected = -11814
        const val sessionNotRunning = -11803
        const val applicationIsNotAuthorizedToUseDevice = -11852
    }
}

class AVFrameRateRange(val minFrameRate: Double, val maxFrameRate: Double) {
    val minFrameDuration: Long
        get() = if (maxFrameRate > 0) (1_000_000 / maxFrameRate).toLong() else -1L

    val maxFrameDuration: Long
        get() = if (minFrameRate > 0) (1_000_000 / minFrameRate).toLong() else -1L

    override fun toString(): String = "${formatRate(minFrameRate)}-${formatRate(maxFrameRate)}"

    private fun formatRate(rate: Double): String =
        if (rate == Math.floor(rate)) rate.toInt().toString() else "%.2f".format(rate)
}

class CMClock private constructor() {
    val time: Long
        get() = HostClock.nowUs()

    fun convertTime(time: Long, to: CMClock? = null): Long = time

    companion object {
        internal val hostTimeClock = CMClock()
    }
}

fun CMClockGetHostTimeClock(): CMClock = CMClock.hostTimeClock
