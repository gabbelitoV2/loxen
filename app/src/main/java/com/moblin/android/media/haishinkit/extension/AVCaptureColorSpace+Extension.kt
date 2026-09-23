package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.avfoundation.AVCaptureColorSpace

fun AVCaptureColorSpace.description(colorSpace: Int): String {
    return when (colorSpace) {
        AVCaptureColorSpace.sRGB -> "SRGB"
        AVCaptureColorSpace.P3_D65 -> "P3_D65"
        AVCaptureColorSpace.HLG_BT2020 -> "HLG_BT2020"
        AVCaptureColorSpace.appleLog -> "Apple Log"
        else -> "Unknown"
    }
}
