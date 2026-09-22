package com.moblin.android.media.haishinkit.extension

import android.media.Image
import android.media.MediaCodec
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.video.VTSessionProperty

fun MediaCodec.prepareToEncodeFrames(): Int =
    0
fun MediaCodec.encodeFrame(
    imageBuffer: Image,
    presentationTimeStamp: Long,
    duration: Long,
    outputHandler: (status: Int, flags: Int, sampleBuffer: MediaSample?) -> Unit
): Int =
    TODO()
fun MediaCodec.invalidate() {
    Unit
}

fun MediaCodec.setProperties(properties: List<VTSessionProperty>): Int {
    val dictionary: MutableMap<String, Any?> = mutableMapOf()
    for (property in properties) {
        dictionary[property.key.value] = property.value
    }
    return 0
}
