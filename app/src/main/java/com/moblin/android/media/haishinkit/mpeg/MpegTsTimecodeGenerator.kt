package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.platform.log.Log
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max

data class MpegTsTimecode(
    val clock: Instant,
    val frame: Int,
)

class MpegTsTimecodeGenerator {
    private var presentationTimeStampBase: Double? = null
    private var previousDecodeTimeStamp: Double? = null
    private var estimatedFrameDuration: Double = 0.033
    private var offsetingFrames: Boolean = false

    fun reset() {
        presentationTimeStampBase = null
        previousDecodeTimeStamp = null
    }

    fun hasReference(): Boolean {
        return presentationTimeStampBase != null
    }

    fun setReference(now: Double, presentationTimeStamp: Double) {
        val base = now - presentationTimeStamp
        presentationTimeStampBase = base
        Log.i(
            TAG,
            "timecode: Updated base time - NTP: $now PTS: $presentationTimeStamp BASE: $base",
        )
    }

    fun makeTimecode(presentationTimeStamp: Long, decodeTimeStamp: Long): MpegTsTimecode? {
        val base = presentationTimeStampBase ?: return null
        val pts = presentationTimeStamp / 1_000_000.0
        var dts = if (decodeTimeStamp == Long.MIN_VALUE) Double.NaN else decodeTimeStamp / 1_000_000.0
        if (dts.isNaN()) {
            dts = pts
        }
        previousDecodeTimeStamp?.let { previous ->
            estimatedFrameDuration = max(
                0.7 * estimatedFrameDuration + 0.3 * (dts - previous),
                0.001,
            )
        }
        previousDecodeTimeStamp = dts
        val seconds = base + pts + (if (offsetingFrames) estimatedFrameDuration / 2 else 0.0)
        val wholeSeconds = seconds.toLong()
        val now = Instant.ofEpochSecond(
            wholeSeconds,
            ((seconds - wholeSeconds) * 1_000_000_000.0).toLong(),
        )
        val offsetWithinSecond = (now.epochSecond + now.nano / 1_000_000_000.0) % 1.0
        val frame = offsetWithinSecond / estimatedFrameDuration
        val offsetFromFrame = offsetWithinSecond - floor(frame) * estimatedFrameDuration
        if (offsetFromFrame < estimatedFrameDuration / 6 ||
            offsetFromFrame > estimatedFrameDuration * 5 / 6
        ) {
            offsetingFrames = !offsetingFrames
        }
        return MpegTsTimecode(clock = now, frame = frame.toInt())
    }

    companion object {
        private const val TAG = "MpegTsTimecodeGenerator"
    }
}
