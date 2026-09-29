package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.media.MediaFormat
import com.moblin.android.platform.log.Log

object CMVideoFormatDescription {
    private const val tag = "CMVideoFormatDescription"

    fun create(imageBuffer: Image): MediaFormat? {
        val status: Int = 0
        if (status != 0) {
            Log.i(tag, "Failed to create video format description with error $status")
            return null
        }
        return com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer(imageBuffer)
    }
}
