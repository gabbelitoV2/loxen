package com.moblin.android.various.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable
enum class SettingsLogLevel(val rawValue: String) {
    @SerialName("Error")
    error("Error"),

    @SerialName("Info")
    info("Info"),

    @SerialName("Debug")
    debug("Debug");

    companion object {
        fun fromRawValue(rawValue: String): SettingsLogLevel? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

val pixelFormats: List<String> = listOf(
    "32BGRA",
    "420YpCbCr8BiPlanarFullRange",
    "420YpCbCr8BiPlanarVideoRange",
)

val pixelFormatTypes: List<Int> = listOf(
    0x42475241,
    0x34323066,
    0x34323076,
)

class SettingsDebug {
    companion object {
        const val builtinAudioAndVideoDelayDefault: Double = 0.07

        fun serializer(): KSerializer<SettingsDebug> = SettingsDebugSerializer
    }

    var logLevel: SettingsLogLevel = SettingsLogLevel.error

    val logFilter = MutableStateFlow("")

    val debugLogging = MutableStateFlow(false)

    var debugLoggingMigrated: Boolean = false

    val debugOverlay = MutableStateFlow(false)

    val cameraSwitchRemoveBlackish = MutableStateFlow(0.3f)

    val bluetoothOutputOnly = MutableStateFlow(true)

    var maximumLogLines: Int = 500
    var pixelFormat: String = pixelFormats[1]
    var faceToBeRemoved: SettingsFace = SettingsFace()

    val allowVideoRangePixelFormat = MutableStateFlow(false)

    var blurSceneSwitch: Boolean = true
    var preferStereoMicToBeRemoved: Boolean = false

    val twitchRewards = MutableStateFlow(false)

    var tesla: SettingsTesla = SettingsTesla()
    var dnsLookupStrategy: SettingsDnsLookupStrategy = SettingsDnsLookupStrategy.system

    val dataRateLimitFactor = MutableStateFlow(2.0f)

    val bitrateDropFix = MutableStateFlow(false)

    val relaxedBitrate = MutableStateFlow(false)

    var externalDisplayChat: Boolean = false
    var videoSourceWidgetTrackFace: Boolean = false
    var replay: Boolean = false
    var recordSegmentLength: Double = 5.0

    val builtinAudioAndVideoDelay = MutableStateFlow(builtinAudioAndVideoDelayDefault)

    var builtinAudioAndVideoDelay70msMigrated: Boolean = false

    val cameraManMoveVertically = MutableStateFlow(false)

    val cameraManSpeed = MutableStateFlow(1.0)

    val cameraManAlwaysMove = MutableStateFlow(false)

    val enhancedMoblinSrt = MutableStateFlow(false)

    val videoBitrateChange = MutableStateFlow(false)

    var highQualityDownsamplingToBeRemoved: Boolean = false
    var httpProxyToBeRemoved: Boolean = false

    val packetPadding = MutableStateFlow(false)

    object SettingsDebugSerializer : KSerializer<SettingsDebug> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsDebug") {
            element("logLevel", SettingsLogLevel.serializer().descriptor)
            element("logFilter", String.serializer().descriptor)
            element("debugLogging", Boolean.serializer().descriptor)
            element("debugLoggingMigrated", Boolean.serializer().descriptor)
            element("srtOverlay", Boolean.serializer().descriptor)
            element("cameraSwitchRemoveBlackish", Float.serializer().descriptor)
            element("bluetoothOutputOnly", Boolean.serializer().descriptor)
            element("maximumLogLines", Int.serializer().descriptor)
            element("pixelFormat", String.serializer().descriptor)
            element("beautyFilterSettings", SettingsFace.serializer().descriptor)
            element("allowVideoRangePixelFormat", Boolean.serializer().descriptor)
            element("blurSceneSwitch", Boolean.serializer().descriptor)
            element("preferStereoMic", Boolean.serializer().descriptor)
            element("twitchRewards", Boolean.serializer().descriptor)
            element("tesla", SettingsTesla.serializer().descriptor)
            element("dnsLookupStrategy", SettingsDnsLookupStrategy.serializer().descriptor)
            element("dataRateLimitFactor", Float.serializer().descriptor)
            element("bitrateDropFix", Boolean.serializer().descriptor)
            element("relaxedBitrate", Boolean.serializer().descriptor)
            element("externalDisplayChat", Boolean.serializer().descriptor)
            element("videoSourceWidgetTrackFace", Boolean.serializer().descriptor)
            element("replay", Boolean.serializer().descriptor)
            element("recordSegmentLength", Double.serializer().descriptor)
            element("builtinAudioAndVideoDelay", Double.serializer().descriptor)
            element("builtinAudioAndVideoDelay70msMigrated", Boolean.serializer().descriptor)
            element("cameraManMoveVertically", Boolean.serializer().descriptor)
            element("cameraManSpeed", Double.serializer().descriptor)
            element("cameraManAlwaysMove", Boolean.serializer().descriptor)
            element("enhancedMoblinSrt", Boolean.serializer().descriptor)
            element("videoBitrateChangeEnabled", Boolean.serializer().descriptor)
            element("highQualityDownsampling", Boolean.serializer().descriptor)
            element("httpProxy3", Boolean.serializer().descriptor)
            element("packetPadding", Boolean.serializer().descriptor)
        }

        override fun serialize(encoder: Encoder, value: SettingsDebug) {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(descriptor, 0, SettingsLogLevel.serializer(), value.logLevel)
                encodeStringElement(descriptor, 1, value.logFilter.value)
                encodeBooleanElement(descriptor, 2, value.debugLogging.value)
                encodeBooleanElement(descriptor, 3, value.debugLoggingMigrated)
                encodeBooleanElement(descriptor, 4, value.debugOverlay.value)
                encodeFloatElement(descriptor, 5, value.cameraSwitchRemoveBlackish.value)
                encodeBooleanElement(descriptor, 6, value.bluetoothOutputOnly.value)
                encodeIntElement(descriptor, 7, value.maximumLogLines)
                encodeStringElement(descriptor, 8, value.pixelFormat)
                encodeSerializableElement(descriptor, 9, SettingsFace.serializer(), value.faceToBeRemoved)
                encodeBooleanElement(descriptor, 10, value.allowVideoRangePixelFormat.value)
                encodeBooleanElement(descriptor, 11, value.blurSceneSwitch)
                encodeBooleanElement(descriptor, 12, value.preferStereoMicToBeRemoved)
                encodeBooleanElement(descriptor, 13, value.twitchRewards.value)
                encodeSerializableElement(descriptor, 14, SettingsTesla.serializer(), value.tesla)
                encodeSerializableElement(descriptor,
                    15,
                    SettingsDnsLookupStrategy.serializer(),
                    value.dnsLookupStrategy)
                encodeFloatElement(descriptor, 16, value.dataRateLimitFactor.value)
                encodeBooleanElement(descriptor, 17, value.bitrateDropFix.value)
                encodeBooleanElement(descriptor, 18, value.relaxedBitrate.value)
                encodeBooleanElement(descriptor, 19, value.externalDisplayChat)
                encodeBooleanElement(descriptor, 20, value.videoSourceWidgetTrackFace)
                encodeBooleanElement(descriptor, 21, value.replay)
                encodeDoubleElement(descriptor, 22, value.recordSegmentLength)
                encodeDoubleElement(descriptor, 23, value.builtinAudioAndVideoDelay.value)
                encodeBooleanElement(descriptor, 24, value.builtinAudioAndVideoDelay70msMigrated)
                encodeBooleanElement(descriptor, 25, value.cameraManMoveVertically.value)
                encodeDoubleElement(descriptor, 26, value.cameraManSpeed.value)
                encodeBooleanElement(descriptor, 27, value.cameraManAlwaysMove.value)
                encodeBooleanElement(descriptor, 28, value.enhancedMoblinSrt.value)
                encodeBooleanElement(descriptor, 29, value.videoBitrateChange.value)
                encodeBooleanElement(descriptor, 30, value.highQualityDownsamplingToBeRemoved)
                encodeBooleanElement(descriptor, 31, value.httpProxyToBeRemoved)
                encodeBooleanElement(descriptor, 32, value.packetPadding.value)
            }
        }

        override fun deserialize(decoder: Decoder): SettingsDebug {
            val result = SettingsDebug()
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break
                        0 -> result.logLevel = decodeSerializableElement(descriptor,
                            0,
                            SettingsLogLevel.serializer())
                        1 -> result.logFilter.value = decodeStringElement(descriptor, 1)
                        2 -> result.debugLogging.value = decodeBooleanElement(descriptor, 2)
                        3 -> result.debugLoggingMigrated = decodeBooleanElement(descriptor, 3)
                        4 -> result.debugOverlay.value = decodeBooleanElement(descriptor, 4)
                        5 -> result.cameraSwitchRemoveBlackish.value = decodeFloatElement(descriptor, 5)
                        6 -> result.bluetoothOutputOnly.value = decodeBooleanElement(descriptor, 6)
                        7 -> result.maximumLogLines = decodeIntElement(descriptor, 7)
                        8 -> result.pixelFormat = decodeStringElement(descriptor, 8)
                        9 -> result.faceToBeRemoved = decodeSerializableElement(descriptor,
                            9,
                            SettingsFace.serializer())
                        10 -> result.allowVideoRangePixelFormat.value = decodeBooleanElement(descriptor, 10)
                        11 -> result.blurSceneSwitch = decodeBooleanElement(descriptor, 11)
                        12 -> result.preferStereoMicToBeRemoved = decodeBooleanElement(descriptor, 12)
                        13 -> result.twitchRewards.value = decodeBooleanElement(descriptor, 13)
                        14 -> result.tesla = decodeSerializableElement(descriptor,
                            14,
                            SettingsTesla.serializer())
                        15 -> result.dnsLookupStrategy = decodeSerializableElement(descriptor,
                            15,
                            SettingsDnsLookupStrategy.serializer())
                        16 -> result.dataRateLimitFactor.value = decodeFloatElement(descriptor, 16)
                        17 -> result.bitrateDropFix.value = decodeBooleanElement(descriptor, 17)
                        18 -> result.relaxedBitrate.value = decodeBooleanElement(descriptor, 18)
                        19 -> result.externalDisplayChat = decodeBooleanElement(descriptor, 19)
                        20 -> result.videoSourceWidgetTrackFace = decodeBooleanElement(descriptor, 20)
                        21 -> result.replay = decodeBooleanElement(descriptor, 21)
                        22 -> result.recordSegmentLength = decodeDoubleElement(descriptor, 22)
                        23 -> result.builtinAudioAndVideoDelay.value = decodeDoubleElement(descriptor, 23)
                        24 -> result.builtinAudioAndVideoDelay70msMigrated = decodeBooleanElement(descriptor, 24)
                        25 -> result.cameraManMoveVertically.value = decodeBooleanElement(descriptor, 25)
                        26 -> result.cameraManSpeed.value = decodeDoubleElement(descriptor, 26)
                        27 -> result.cameraManAlwaysMove.value = decodeBooleanElement(descriptor, 27)
                        28 -> result.enhancedMoblinSrt.value = decodeBooleanElement(descriptor, 28)
                        29 -> result.videoBitrateChange.value = decodeBooleanElement(descriptor, 29)
                        30 -> result.highQualityDownsamplingToBeRemoved = decodeBooleanElement(descriptor, 30)
                        31 -> result.httpProxyToBeRemoved = decodeBooleanElement(descriptor, 31)
                        32 -> result.packetPadding.value = decodeBooleanElement(descriptor, 32)
                    }
                }
            }
            if (!result.debugLoggingMigrated) {
                result.debugLogging.value = result.logLevel == SettingsLogLevel.debug
                result.debugLoggingMigrated = true
            }
            if (!result.builtinAudioAndVideoDelay70msMigrated &&
                result.builtinAudioAndVideoDelay.value == 0.0
            ) {
                result.builtinAudioAndVideoDelay.value = SettingsDebug.builtinAudioAndVideoDelayDefault
            }
            result.builtinAudioAndVideoDelay70msMigrated = true
            return result
        }
    }
}
