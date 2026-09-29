package com.moblin.android.media.haishinkit.codec.video

import android.util.Size
import com.moblin.android.media.haishinkit.extension.hlgColorAttachments
import com.moblin.android.media.haishinkit.media.video.VideoUnit
import com.moblin.android.platform.video.kCVImageBufferColorPrimariesKey
import com.moblin.android.platform.video.kCVImageBufferTransferFunctionKey
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey

@Volatile
var videoEncoderDataRateLimitFactor: Double = 1.2

val videoEncoderAverageBitRateOnlySupported: Boolean = false

val videoEncoderVariableBitRateSupported: Boolean = true

const val kVTProfileLevel_H264_Baseline_3_1 = "H264_Baseline_3_1"

const val kVTHDRMetadataInsertionMode_Auto = "Auto"

const val kCVImageBufferColorPrimaries_ITU_R_2020 = "ITU_R_2020"

const val kCVImageBufferTransferFunction_ITU_R_2100_HLG = "ITU_R_2100_HLG"

const val kCVImageBufferYCbCrMatrix_ITU_R_2020 = "ITU_R_2020"

const val kVTH264EntropyMode_CABAC = "CABAC"

fun createDataRateLimits(bitRate: Int): List<Double> {
    var bitRate = bitRate.toDouble()
    if (bitRate < 1_000_000) {
        bitRate *= 1.2
    } else {
        bitRate *= videoEncoderDataRateLimitFactor
    }
    val bytesLimit = bitRate / 8
    val secondsLimit = 1.0
    return listOf(bytesLimit, secondsLimit)
}

fun createColorProperties(colorAttachments: Map<String, String>): List<VTSessionProperty> {
    val properties = mutableListOf<VTSessionProperty>()
    colorAttachments[kCVImageBufferColorPrimariesKey]?.let { primaries ->
        properties.add(VTSessionProperty(key = VTSessionPropertyKey.colorPrimaries, value = primaries))
    }
    colorAttachments[kCVImageBufferTransferFunctionKey]?.let { transferFunction ->
        properties.add(VTSessionProperty(key = VTSessionPropertyKey.transferFunction, value = transferFunction))
    }
    colorAttachments[kCVImageBufferYCbCrMatrixKey]?.let { yCbCrMatrix ->
        properties.add(VTSessionProperty(key = VTSessionPropertyKey.YCbCrMatrix, value = yCbCrMatrix))
    }
    return properties
}

class VideoEncoderSettings {
    enum class Format(val codecType: String) {
        h264("video/avc"),
        hevc("video/hevc"),
    }

    enum class RateControl {
        abr,
        cbr,
        vbr,
    }

    var videoSize: Size = Size(854, 480)
    var bitrate: Int = 640 * 1000
    var rateControl: RateControl = RateControl.abr
    var maxKeyFrameIntervalDuration: Int = 2
    var allowFrameReordering: Boolean = false
    var profileLevel: String = kVTProfileLevel_H264_Baseline_3_1
        set(value) {
            field = value
            if (value.contains("HEVC")) {
                format = Format.hevc
            } else {
                format = Format.h264
            }
        }

    var adaptiveResolution = false
    var adaptiveResolution160Threshold: Int = 0
    var adaptiveResolution360Threshold: Int = 0
    var adaptiveResolution480Threshold: Int = 0
    var adaptiveResolution720Threshold: Int = 0
    var adaptiveResolution1080Threshold: Int = 0

    var format: Format = Format.h264
        private set

    init {
        updateAdtaptiveResolutionThresholds(1.0)
    }

    fun updateAdtaptiveResolutionThresholds(factor: Double) {
        adaptiveResolution160Threshold = (100_000 * factor).toInt()
        adaptiveResolution360Threshold = (250_000 * factor).toInt()
        adaptiveResolution480Threshold = (500_000 * factor).toInt()
        adaptiveResolution720Threshold = (750_000 * factor).toInt()
        adaptiveResolution1080Threshold = (1_500_000 * factor).toInt()
    }

    fun shouldInvalidateSession(other: VideoEncoderSettings): Boolean {
        return !(videoSize == other.videoSize &&
            maxKeyFrameIntervalDuration == other.maxKeyFrameIntervalDuration &&
            allowFrameReordering == other.allowFrameReordering &&
            profileLevel == other.profileLevel &&
            rateControl == other.rateControl)
    }

    fun properties(): List<VTSessionProperty> {
        val isBaseline = profileLevel.contains("Baseline")
        val properties = mutableListOf(
            VTSessionProperty(key = VTSessionPropertyKey.realTime, value = true),
            VTSessionProperty(key = VTSessionPropertyKey.profileLevel, value = profileLevel),
            VTSessionProperty(key = VTSessionPropertyKey.expectedFrameRate, value = VideoUnit.defaultFrameRate),
            VTSessionProperty(key = VTSessionPropertyKey.maxKeyFrameIntervalDuration, value = maxKeyFrameIntervalDuration),
            VTSessionProperty(key = VTSessionPropertyKey.allowFrameReordering, value = allowFrameReordering),
            VTSessionProperty(key = VTSessionPropertyKey.pixelTransferProperties, value = mapOf("ScalingMode" to "Trim")),
        )
        properties += bitrateProperties(bitrate)
        if (profileLevel.contains("Main10")) {
            properties.add(VTSessionProperty(key = VTSessionPropertyKey.hdrMetadataInsertionMode, value = kVTHDRMetadataInsertionMode_Auto))
            properties += createColorProperties(hlgColorAttachments)
        }
        if (!isBaseline && profileLevel.contains("H264")) {
            properties.add(VTSessionProperty(key = VTSessionPropertyKey.h264EntropyMode, value = kVTH264EntropyMode_CABAC))
        }
        return properties
    }

    fun bitrateProperties(bitrate: Int): List<VTSessionProperty> {
        return when (rateControl) {
            RateControl.abr -> {
                if (videoEncoderAverageBitRateOnlySupported) {
                    listOf(VTSessionProperty(key = VTSessionPropertyKey.averageBitRate, value = bitrate))
                } else {
                    listOf(
                        VTSessionProperty(key = VTSessionPropertyKey.averageBitRate, value = bitrate),
                        VTSessionProperty(key = VTSessionPropertyKey.dataRateLimits, value = createDataRateLimits(bitRate = bitrate)),
                    )
                }
            }
            RateControl.cbr -> {
                listOf(VTSessionProperty(key = VTSessionPropertyKey.constantBitRate, value = bitrate))
            }
            RateControl.vbr -> {
                if (videoEncoderVariableBitRateSupported) {
                    listOf(VTSessionProperty(key = VTSessionPropertyKey.variableBitRate, value = bitrate))
                } else {
                    emptyList()
                }
            }
        }
    }
}
