package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.video.CVBufferCopyAttachment
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.kCVImageBufferColorPrimariesKey
import com.moblin.android.platform.video.kCVImageBufferColorPrimaries_ITU_R_2020
import com.moblin.android.platform.video.kCVImageBufferColorPrimaries_ITU_R_709_2
import com.moblin.android.platform.video.kCVImageBufferTransferFunctionKey
import com.moblin.android.platform.video.kCVImageBufferTransferFunction_ITU_R_2100_HLG
import com.moblin.android.platform.video.kCVImageBufferTransferFunction_ITU_R_709_2
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrix_ITU_R_2020
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrix_ITU_R_601_4
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrix_ITU_R_709_2
import com.moblin.android.various.settings.SettingsStreamColorRange

val CVPixelBuffer.colorAttachments: Map<String, String>
    get() {
        val attachments = mutableMapOf<String, String>()
        for (key in listOf(
            kCVImageBufferColorPrimariesKey,
            kCVImageBufferTransferFunctionKey,
            kCVImageBufferYCbCrMatrixKey,
        )) {
            val value = CVBufferCopyAttachment(this, key) as? String
            if (value != null) {
                attachments[key] = value
            }
        }
        return attachments
    }

val defaultColorAttachments: Map<String, String> = mapOf(
    kCVImageBufferColorPrimariesKey to kCVImageBufferColorPrimaries_ITU_R_709_2,
    kCVImageBufferTransferFunctionKey to kCVImageBufferTransferFunction_ITU_R_709_2,
    kCVImageBufferYCbCrMatrixKey to kCVImageBufferYCbCrMatrix_ITU_R_601_4,
)

val rec709ColorAttachments: Map<String, String> = mapOf(
    kCVImageBufferColorPrimariesKey to kCVImageBufferColorPrimaries_ITU_R_709_2,
    kCVImageBufferTransferFunctionKey to kCVImageBufferTransferFunction_ITU_R_709_2,
    kCVImageBufferYCbCrMatrixKey to kCVImageBufferYCbCrMatrix_ITU_R_709_2,
)

fun sdrColorAttachments(colorRange: SettingsStreamColorRange): Map<String, String> {
    return when (colorRange) {
        SettingsStreamColorRange.full -> defaultColorAttachments
        SettingsStreamColorRange.limited -> rec709ColorAttachments
    }
}

fun sdrYCbCrMatrix(colorRange: SettingsStreamColorRange): String {
    return when (colorRange) {
        SettingsStreamColorRange.full -> kCVImageBufferYCbCrMatrix_ITU_R_601_4
        SettingsStreamColorRange.limited -> kCVImageBufferYCbCrMatrix_ITU_R_709_2
    }
}

val hlgColorAttachments: Map<String, String> = mapOf(
    kCVImageBufferColorPrimariesKey to kCVImageBufferColorPrimaries_ITU_R_2020,
    kCVImageBufferTransferFunctionKey to kCVImageBufferTransferFunction_ITU_R_2100_HLG,
    kCVImageBufferYCbCrMatrixKey to kCVImageBufferYCbCrMatrix_ITU_R_2020,
)
