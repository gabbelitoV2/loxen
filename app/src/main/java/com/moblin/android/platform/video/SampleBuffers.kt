package com.moblin.android.platform.video

import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.media.kCMTimeInvalidUs

class CMSampleTimingInfo(
    val duration: Long = kCMTimeInvalidUs,
    val presentationTimeStamp: Long = kCMTimeInvalidUs,
    val decodeTimeStamp: Long = kCMTimeInvalidUs,
)

fun CMSampleBufferCreateForImageBuffer(
    allocator: Any?,
    imageBuffer: CVPixelBuffer,
    dataReady: Boolean,
    makeDataReadyCallback: Any?,
    refcon: Any?,
    formatDescription: MediaFormat,
    sampleTiming: CMSampleTimingInfo,
): MediaSample? {
    return MediaSample(
        data = ByteArray(0),
        presentationTimeUs = sampleTiming.presentationTimeStamp,
        isKeyFrame = true,
        format = formatDescription,
        imageBuffer = imageBuffer,
        durationUs = sampleTiming.duration,
        decodeTimeStampUs = sampleTiming.decodeTimeStamp,
    )
}
