package com.moblin.android.media.haishinkit.extension

import com.moblin.android.media.haishinkit.codec.video.VTSessionProperty
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.video.CVImageBuffer
import com.moblin.android.platform.videotoolbox.VTCompressionOutputHandler
import com.moblin.android.platform.videotoolbox.VTCompressionSession
import com.moblin.android.platform.videotoolbox.VTCompressionSessionEncodeFrame
import com.moblin.android.platform.videotoolbox.VTCompressionSessionInvalidate
import com.moblin.android.platform.videotoolbox.VTCompressionSessionPrepareToEncodeFrames
import com.moblin.android.platform.videotoolbox.VTSessionSetProperties

fun VTCompressionSession.prepareToEncodeFrames(): Int {
    return VTCompressionSessionPrepareToEncodeFrames(this)
}

fun VTCompressionSession.encodeFrame(
    imageBuffer: CVImageBuffer,
    presentationTimeStamp: Long,
    duration: Long,
    outputHandler: VTCompressionOutputHandler,
): Int {
    return VTCompressionSessionEncodeFrame(
        this,
        imageBuffer = imageBuffer,
        presentationTimeStamp = presentationTimeStamp,
        duration = kCMTimeInvalidUs,
        frameProperties = null,
        outputHandler = outputHandler,
    )
}

fun VTCompressionSession.invalidate() {
    VTCompressionSessionInvalidate(this)
}

fun VTCompressionSession.setProperties(properties: List<VTSessionProperty>): Int {
    val dictionary: MutableMap<String, Any> = mutableMapOf()
    for (property in properties) {
        dictionary[property.key.value] = property.value
    }
    return VTSessionSetProperties(this, dictionary)
}
