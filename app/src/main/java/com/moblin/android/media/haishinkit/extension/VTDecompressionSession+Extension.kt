package com.moblin.android.media.haishinkit.extension

import com.moblin.android.media.MediaSample
import com.moblin.android.platform.videotoolbox.VTDecodeFrameFlags
import com.moblin.android.platform.videotoolbox.VTDecompressionOutputHandler
import com.moblin.android.platform.videotoolbox.VTDecompressionSession
import com.moblin.android.platform.videotoolbox.VTDecompressionSessionDecodeFrame
import com.moblin.android.platform.videotoolbox.VTDecompressionSessionInvalidate

val VTDecompressionSession.Companion.defaultDecodeFlags: Int
    get() = VTDecodeFrameFlags._EnableAsynchronousDecompression or VTDecodeFrameFlags._EnableTemporalProcessing

fun VTDecompressionSession.decodeFrame(
    sampleBuffer: MediaSample,
    outputHandler: VTDecompressionOutputHandler,
): Int {
    return VTDecompressionSessionDecodeFrame(
        this,
        sampleBuffer = sampleBuffer,
        flags = VTDecompressionSession.defaultDecodeFlags,
        outputHandler = outputHandler,
    )
}

fun VTDecompressionSession.invalidate() {
    VTDecompressionSessionInvalidate(this)
}
