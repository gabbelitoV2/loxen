package com.moblin.android.media.haishinkit.extension

enum class AVCaptureColorSpace(val rawValue: Int) {
    sRGB(1),
    P3_D65(2),
    HLG_BT2020(3),
    appleLog(4),
    ;

    override fun toString(): String = description

    companion object {
        fun fromRawValue(rawValue: Int): AVCaptureColorSpace? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

val AVCaptureColorSpace.description: String
    get() = when (this) {
        AVCaptureColorSpace.sRGB -> "SRGB"
        AVCaptureColorSpace.P3_D65 -> "P3_D65"
        AVCaptureColorSpace.HLG_BT2020 -> "HLG_BT2020"
        AVCaptureColorSpace.appleLog -> "Apple Log"
        else -> "Unknown"
    }
