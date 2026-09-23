package com.moblin.android.platform.videotoolbox

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecInfo.EncoderCapabilities
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.util.Range
import com.moblin.android.platform.video.GlRenderer
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val TAG = "MoblinEncoder"

internal class EncoderConfiguration(
    val format: MediaFormat,
    val fallbackFormat: MediaFormat,
    val allowFrameReordering: Boolean,
    val scalingMode: GlRenderer.ScalingMode,
    val bitrateRange: Range<Int>?,
    val summary: String,
)

private class LevelLimits(
    val level: Int,
    val maxFrameSize: Double,
    val maxRate: Double,
    val maxBitrateKbps: Double,
)

private val avcLevelLimits = listOf(
    LevelLimits(CodecProfileLevel.AVCLevel1, 99.0, 1485.0, 64.0),
    LevelLimits(CodecProfileLevel.AVCLevel11, 396.0, 3000.0, 192.0),
    LevelLimits(CodecProfileLevel.AVCLevel12, 396.0, 6000.0, 384.0),
    LevelLimits(CodecProfileLevel.AVCLevel13, 396.0, 11880.0, 768.0),
    LevelLimits(CodecProfileLevel.AVCLevel2, 396.0, 11880.0, 2000.0),
    LevelLimits(CodecProfileLevel.AVCLevel21, 792.0, 19800.0, 4000.0),
    LevelLimits(CodecProfileLevel.AVCLevel22, 1620.0, 20250.0, 4000.0),
    LevelLimits(CodecProfileLevel.AVCLevel3, 1620.0, 40500.0, 10000.0),
    LevelLimits(CodecProfileLevel.AVCLevel31, 3600.0, 108000.0, 14000.0),
    LevelLimits(CodecProfileLevel.AVCLevel32, 5120.0, 216000.0, 20000.0),
    LevelLimits(CodecProfileLevel.AVCLevel4, 8192.0, 245760.0, 20000.0),
    LevelLimits(CodecProfileLevel.AVCLevel41, 8192.0, 245760.0, 50000.0),
    LevelLimits(CodecProfileLevel.AVCLevel42, 8704.0, 522240.0, 50000.0),
    LevelLimits(CodecProfileLevel.AVCLevel5, 22080.0, 589824.0, 135000.0),
    LevelLimits(CodecProfileLevel.AVCLevel51, 36864.0, 983040.0, 240000.0),
    LevelLimits(CodecProfileLevel.AVCLevel52, 36864.0, 2073600.0, 240000.0),
    LevelLimits(CodecProfileLevel.AVCLevel6, 139264.0, 4177920.0, 240000.0),
    LevelLimits(CodecProfileLevel.AVCLevel61, 139264.0, 8355840.0, 480000.0),
    LevelLimits(CodecProfileLevel.AVCLevel62, 139264.0, 16711680.0, 800000.0),
)

private val hevcLevelLimits = listOf(
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel1, 36864.0, 552960.0, 128.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel2, 122880.0, 3686400.0, 1500.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel21, 245760.0, 7372800.0, 3000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel3, 552960.0, 16588800.0, 6000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel31, 983040.0, 33177600.0, 10000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel4, 2228224.0, 66846720.0, 12000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel41, 2228224.0, 133693440.0, 20000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel5, 8912896.0, 267386880.0, 25000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel51, 8912896.0, 534773760.0, 40000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel52, 8912896.0, 1069547520.0, 60000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel6, 35651584.0, 1069547520.0, 60000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel61, 35651584.0, 2139095040.0, 120000.0),
    LevelLimits(CodecProfileLevel.HEVCMainTierLevel62, 35651584.0, 4278190080.0, 240000.0),
)

private val avcLevelsByName = mapOf(
    "1_3" to CodecProfileLevel.AVCLevel13,
    "3_0" to CodecProfileLevel.AVCLevel3,
    "3_1" to CodecProfileLevel.AVCLevel31,
    "3_2" to CodecProfileLevel.AVCLevel32,
    "4_0" to CodecProfileLevel.AVCLevel4,
    "4_1" to CodecProfileLevel.AVCLevel41,
    "4_2" to CodecProfileLevel.AVCLevel42,
    "5_0" to CodecProfileLevel.AVCLevel5,
    "5_1" to CodecProfileLevel.AVCLevel51,
    "5_2" to CodecProfileLevel.AVCLevel52,
)

private val avcHighProfiles = setOf(CodecProfileLevel.AVCProfileHigh, CodecProfileLevel.AVCProfileConstrainedHigh)

private val avcBaselineProfiles = setOf(
    CodecProfileLevel.AVCProfileBaseline,
    CodecProfileLevel.AVCProfileConstrainedBaseline,
)

private val handledPropertyKeys = setOf(
    "RealTime",
    "ProfileLevel",
    "ExpectedFrameRate",
    "MaxKeyFrameIntervalDuration",
    "AllowFrameReordering",
    "PixelTransferProperties",
    "AverageBitRate",
    "ConstantBitRate",
    "VariableBitRate",
    "ColorPrimaries",
    "TransferFunction",
    "YCbCrMatrix",
)

private val ignoredPropertyKeys = setOf("DataRateLimits", "H264EntropyMode", "HDRMetadataInsertionMode")

private val loggedMessages: MutableSet<String> = Collections.synchronizedSet(HashSet())

private val encoderInfos: List<MediaCodecInfo> by lazy {
    try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
    } catch (e: Exception) {
        Log.i(TAG, "Failed to list codecs: $e")
        emptyList()
    }
}

internal fun logOnce(message: String) {
    if (loggedMessages.add(message)) {
        Log.i(TAG, message)
    }
}

internal fun normalizeVideoMimeType(codecType: String): String {
    return when (codecType.lowercase()) {
        "h264", "avc", "avc1", MediaFormat.MIMETYPE_VIDEO_AVC -> MediaFormat.MIMETYPE_VIDEO_AVC
        "hevc", "h265", "hvc1", "hev1", MediaFormat.MIMETYPE_VIDEO_HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
        else -> codecType
    }
}

internal fun selectVideoEncoders(mimeType: String, width: Int, height: Int): List<MediaCodecInfo> {
    val candidates = encoderInfos.filter { info ->
        !isAlias(info) &&
            info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } &&
            isSizeSupported(info, mimeType, width, height)
    }
    return candidates.sortedBy { if (isHardwareEncoder(it)) 0 else 1 }
}

internal fun selectVideoEncoder(mimeType: String, width: Int, height: Int): MediaCodecInfo? {
    return selectVideoEncoders(mimeType, width, height).firstOrNull()
}

internal fun profileFromProfileLevel(
    profileLevel: String,
    mimeType: String,
    capabilities: CodecCapabilities,
): Pair<Int, Int?>? {
    val supportedProfiles = capabilities.profileLevels.map { it.profile }.toSet()
    val profile = requestedProfiles(profileLevel).firstOrNull { it in supportedProfiles } ?: return null
    return Pair(profile, explicitLevel(profileLevel, mimeType))
}

internal fun makeEncoderConfiguration(
    codecInfo: MediaCodecInfo,
    mimeType: String,
    width: Int,
    height: Int,
    properties: Map<String, Any>,
): EncoderConfiguration? {
    val capabilities = codecInfo.getCapabilitiesForType(mimeType)
    val profileLevelName = properties["ProfileLevel"] as? String
    if (profileLevelName?.contains("Main10") == true || properties["TransferFunction"] == "ITU_R_2100_HLG") {
        val hdrEditing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            capabilities.isFeatureSupported(CodecCapabilities.FEATURE_HdrEditing)
        Log.i(
            TAG,
            "${codecInfo.name}: HEVC Main10 HLG needs FEATURE_HdrEditing (supported: $hdrEditing) and a " +
                "10-bit EGL config (not available), failing prepare",
        )
        return null
    }
    logUnhandledProperties(properties)
    val bitrateRange = capabilities.videoCapabilities?.bitrateRange
    val frameRate = (properties["ExpectedFrameRate"] as? Number)?.toDouble()?.takeIf { it > 0 } ?: 30.0
    val iFrameInterval = makeIFrameInterval(properties["MaxKeyFrameIntervalDuration"] as? Number)
    val constantBitRate = (properties["ConstantBitRate"] as? Number)?.toInt()
    val requestedBitrate = constantBitRate
        ?: (properties["AverageBitRate"] as? Number)?.toInt()
        ?: (properties["VariableBitRate"] as? Number)?.toInt()
        ?: (width * height * 4)
    val bitrate = bitrateRange?.clamp(requestedBitrate) ?: requestedBitrate
    val bitrateMode = selectBitrateMode(codecInfo, capabilities, constantBitRate != null)
    var profile: Int? = null
    var level: Int? = null
    if (profileLevelName != null) {
        val selected = profileFromProfileLevel(profileLevelName, mimeType, capabilities)
        if (selected == null) {
            Log.i(TAG, "${codecInfo.name}: Profile $profileLevelName not supported, using the encoder default")
        } else {
            profile = selected.first
            level = selectLevel(
                mimeType,
                selected.first,
                selected.second,
                capabilities,
                width,
                height,
                frameRate,
                bitrate,
            )
        }
    }
    val isBaseline = (profile != null && profile in avcBaselineProfiles) ||
        profileLevelName?.contains("Baseline") == true
    val allowFrameReordering = properties["AllowFrameReordering"] == true && !isBaseline
    val scalingMode = when ((properties["PixelTransferProperties"] as? Map<*, *>)?.get("ScalingMode")) {
        "Trim" -> GlRenderer.ScalingMode.trim
        "Letterbox" -> GlRenderer.ScalingMode.fit
        else -> GlRenderer.ScalingMode.stretch
    }
    val format = makeBaseFormat(mimeType, width, height, frameRate, iFrameInterval, bitrate)
    val fallbackFormat = makeBaseFormat(
        mimeType,
        width,
        height,
        frameRate,
        if (iFrameInterval.toDouble() < 0) 2 else iFrameInterval,
        bitrate,
    )
    if (bitrateMode != null) {
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, bitrateMode)
    }
    if (profile != null) {
        format.setInteger(MediaFormat.KEY_PROFILE, profile)
        if (level != null) {
            format.setInteger(MediaFormat.KEY_LEVEL, level)
        }
    }
    when (properties["RealTime"]) {
        true -> format.setInteger(MediaFormat.KEY_PRIORITY, 0)
        false -> format.setInteger(MediaFormat.KEY_PRIORITY, 1)
        else -> Unit
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        format.setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 0)
        format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, if (allowFrameReordering) 2 else 0)
    }
    format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
    format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
    format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
    val summary = "${codecInfo.name} ${width}x$height profile=${profileName(mimeType, profile)} " +
        "level=${level ?: "default"} bitrate=$bitrate mode=${bitrateModeName(bitrateMode)} " +
        "iframe=$iFrameInterval fps=${frameRate.roundToInt()} bframes=$allowFrameReordering scaling=$scalingMode"
    return EncoderConfiguration(
        format = format,
        fallbackFormat = fallbackFormat,
        allowFrameReordering = allowFrameReordering,
        scalingMode = scalingMode,
        bitrateRange = bitrateRange,
        summary = summary,
    )
}

private fun makeBaseFormat(
    mimeType: String,
    width: Int,
    height: Int,
    frameRate: Double,
    iFrameInterval: Number,
    bitrate: Int,
): MediaFormat {
    val format = MediaFormat.createVideoFormat(mimeType, width, height)
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatSurface)
    format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.roundToInt())
    if (iFrameInterval is Float) {
        format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
    } else {
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval.toInt())
    }
    format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
    return format
}

private fun makeIFrameInterval(value: Number?): Number {
    val seconds = value?.toDouble() ?: 0.0
    return when {
        seconds > 0 -> if (seconds == floor(seconds)) seconds.toInt() else seconds.toFloat()
        seconds < 0 -> -1
        else -> 2
    }
}

private fun selectBitrateMode(codecInfo: MediaCodecInfo, capabilities: CodecCapabilities, constant: Boolean): Int? {
    val encoderCapabilities = capabilities.encoderCapabilities ?: return null
    val preferred = if (constant) {
        listOf(EncoderCapabilities.BITRATE_MODE_CBR, EncoderCapabilities.BITRATE_MODE_VBR)
    } else {
        listOf(EncoderCapabilities.BITRATE_MODE_VBR, EncoderCapabilities.BITRATE_MODE_CBR)
    }
    val mode = preferred.firstOrNull { encoderCapabilities.isBitrateModeSupported(it) }
    if (mode != preferred.first()) {
        logOnce("${codecInfo.name}: Bitrate mode ${bitrateModeName(preferred.first())} not supported")
    }
    return mode
}

private fun requestedProfiles(profileLevel: String): List<Int> {
    return when {
        profileLevel.contains("HEVC_Main10") -> listOf(CodecProfileLevel.HEVCProfileMain10)
        profileLevel.contains("HEVC_Main") -> listOf(CodecProfileLevel.HEVCProfileMain)
        profileLevel.contains("H264_ConstrainedBaseline") || profileLevel.contains("H264_Baseline") ->
            listOf(CodecProfileLevel.AVCProfileConstrainedBaseline, CodecProfileLevel.AVCProfileBaseline)
        profileLevel.contains("H264_ConstrainedHigh") ->
            listOf(CodecProfileLevel.AVCProfileConstrainedHigh, CodecProfileLevel.AVCProfileHigh)
        profileLevel.contains("H264_Main") -> listOf(CodecProfileLevel.AVCProfileMain)
        profileLevel.contains("H264_High") -> listOf(CodecProfileLevel.AVCProfileHigh)
        else -> emptyList()
    }
}

private fun explicitLevel(profileLevel: String, mimeType: String): Int? {
    if (mimeType != MediaFormat.MIMETYPE_VIDEO_AVC) {
        return null
    }
    val match = Regex("_(\\d_\\d)$").find(profileLevel) ?: return null
    return avcLevelsByName[match.groupValues[1]]
}

private fun selectLevel(
    mimeType: String,
    profile: Int,
    requestedLevel: Int?,
    capabilities: CodecCapabilities,
    width: Int,
    height: Int,
    frameRate: Double,
    bitrate: Int,
): Int? {
    val maxLevel = capabilities.profileLevels.filter { it.profile == profile }.maxOfOrNull { it.level }
    if (maxLevel == null || maxLevel <= 0) {
        return requestedLevel
    }
    val level = requestedLevel ?: computeLevel(mimeType, profile, width, height, frameRate, bitrate) ?: return maxLevel
    if (level <= maxLevel) {
        return level
    }
    return if (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC && maxLevel.countTrailingZeroBits() % 2 == 1) {
        maxLevel ushr 1
    } else {
        maxLevel
    }
}

private fun computeLevel(
    mimeType: String,
    profile: Int,
    width: Int,
    height: Int,
    frameRate: Double,
    bitrate: Int,
): Int? {
    return if (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
        val frameSize = width.toDouble() * height.toDouble()
        val rate = frameSize * frameRate
        val kbps = bitrate / 1000.0
        hevcLevelLimits.firstOrNull {
            frameSize <= it.maxFrameSize && rate <= it.maxRate && kbps <= it.maxBitrateKbps
        }?.level
    } else {
        val frameSize = ceil(width / 16.0) * ceil(height / 16.0)
        val rate = frameSize * frameRate
        val kbps = bitrate / 1000.0 / (if (profile in avcHighProfiles) 1.25 else 1.0)
        avcLevelLimits.firstOrNull {
            frameSize <= it.maxFrameSize && rate <= it.maxRate && kbps <= it.maxBitrateKbps
        }?.level
    }
}

private fun profileName(mimeType: String, profile: Int?): String {
    if (profile == null) {
        return "default"
    }
    return if (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
        when (profile) {
            CodecProfileLevel.HEVCProfileMain -> "Main"
            CodecProfileLevel.HEVCProfileMain10 -> "Main10"
            else -> "$profile"
        }
    } else {
        when (profile) {
            CodecProfileLevel.AVCProfileConstrainedBaseline -> "ConstrainedBaseline"
            CodecProfileLevel.AVCProfileBaseline -> "Baseline"
            CodecProfileLevel.AVCProfileMain -> "Main"
            CodecProfileLevel.AVCProfileHigh -> "High"
            CodecProfileLevel.AVCProfileConstrainedHigh -> "ConstrainedHigh"
            else -> "$profile"
        }
    }
}

private fun bitrateModeName(mode: Int?): String {
    return when (mode) {
        EncoderCapabilities.BITRATE_MODE_VBR -> "VBR"
        EncoderCapabilities.BITRATE_MODE_CBR -> "CBR"
        EncoderCapabilities.BITRATE_MODE_CQ -> "CQ"
        null -> "default"
        else -> "$mode"
    }
}

private fun logUnhandledProperties(properties: Map<String, Any>) {
    for (key in properties.keys) {
        if (key in ignoredPropertyKeys) {
            logOnce("$key has no MediaCodec equivalent, ignored")
        } else if (key !in handledPropertyKeys) {
            logOnce("$key is not supported, ignored")
        }
    }
}

private fun isAlias(info: MediaCodecInfo): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isAlias
}

private fun isHardwareEncoder(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return info.isHardwareAccelerated
    }
    val name = info.name.lowercase()
    return !(name.startsWith("c2.android.") || name.startsWith("omx.google.") || name.contains(".sw."))
}

private fun isSizeSupported(info: MediaCodecInfo, mimeType: String, width: Int, height: Int): Boolean {
    return try {
        info.getCapabilitiesForType(mimeType).videoCapabilities?.isSizeSupported(width, height) == true
    } catch (e: Exception) {
        false
    }
}
