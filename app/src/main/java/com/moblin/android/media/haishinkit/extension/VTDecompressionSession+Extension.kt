package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.media.MediaCodec
import com.moblin.android.media.MediaSample

@JvmInline
value class VTDecodeFrameFlags(val rawValue: Int) {
    operator fun plus(other: VTDecodeFrameFlags): VTDecodeFrameFlags =
        VTDecodeFrameFlags(rawValue or other.rawValue)

    operator fun contains(other: VTDecodeFrameFlags): Boolean =
        (rawValue and other.rawValue) == other.rawValue

    companion object {
        val _EnableAsynchronousDecompression: VTDecodeFrameFlags = VTDecodeFrameFlags(1 shl 0)
        val _EnableTemporalProcessing: VTDecodeFrameFlags = VTDecodeFrameFlags(1 shl 1)
    }
}

@JvmInline
value class VTDecodeInfoFlags(val rawValue: Int) {
    fun isEmpty(): Boolean = rawValue == 0

    companion object {
        val none: VTDecodeInfoFlags = VTDecodeInfoFlags(0)
    }
}

fun interface VTDecompressionOutputHandler {
    fun onDecodeOutput(
        status: Int,
        infoFlags: VTDecodeInfoFlags,
        imageBuffer: Image?,
        presentationTimeStampUs: Long,
        presentationDurationUs: Long
    )
}

interface VTDecompressionSession {
    val codec: MediaCodec?

    companion object {
        val defaultDecodeFlags: VTDecodeFrameFlags =
            VTDecodeFrameFlags._EnableAsynchronousDecompression + VTDecodeFrameFlags._EnableTemporalProcessing
    }
}

inline fun VTDecompressionSession.decodeFrame(
    sampleBuffer: MediaSample,
    outputHandler: VTDecompressionOutputHandler
): Int =
    TODO()
fun VTDecompressionSession.invalidate() {
    val codec = codec ?: return
    runCatching { codec.stop() }
    codec.release()
}
