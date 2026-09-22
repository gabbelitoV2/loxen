package com.moblin.android.media.haishinkit.extension

import android.media.Image
import android.media.MediaCodec
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.video.VTSessionProperty

fun MediaCodec.prepareToEncodeFrames(): Int =
    TODO("VideoToolbox VTCompressionSessionPrepareToEncodeFrames has no MediaCodec equivalent; a MediaCodec must be configured then started with MediaCodec.start()")

fun MediaCodec.encodeFrame(
    imageBuffer: Image,
    presentationTimeStamp: Long,
    duration: Long,
    outputHandler: (status: Int, flags: Int, sampleBuffer: MediaSample?) -> Unit
): Int =
    TODO("VideoToolbox VTCompressionSessionEncodeFrame has no MediaCodec equivalent; queue the Image on the codec input buffer and dequeue output through the MediaCodec async callback, which must be registered at configure time")

fun MediaCodec.invalidate() {
    TODO("VideoToolbox VTCompressionSessionInvalidate has no MediaCodec equivalent; use MediaCodec.stop() followed by MediaCodec.release()")
}

fun MediaCodec.setProperties(properties: List<VTSessionProperty>): Int {
    val dictionary: MutableMap<String, Any?> = mutableMapOf()
    for (property in properties) {
        dictionary[property.key.value] = property.value
    }
    return TODO("VideoToolbox VTSessionSetProperties has no MediaCodec equivalent; apply $dictionary through MediaCodec.setParameters(Bundle)")
}
