package com.moblin.android.media.haishinkit.extension

import android.media.Image
import android.media.MediaFormat
import android.util.Log

object CMVideoFormatDescription {
    private const val tag = "CMVideoFormatDescription"

    fun create(imageBuffer: Image): MediaFormat? {
        val status: Int = TODO("no Android counterpart for CMVideoFormatDescriptionCreateForImageBuffer")
        if (status != 0) {
            Log.i(tag, "Failed to create video format description with error $status")
            return null
        }
        return TODO("no Android counterpart for CMVideoFormatDescription")
    }
}
