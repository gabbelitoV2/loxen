package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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

@Serializable(with = SettingsDebug.Serializer::class)
class SettingsDebug {
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

    val nativeLowLightBoost = MutableStateFlow(false)

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

    fun encode(): JsonObject = encodeContainer {
        encode("logLevel", logLevel)
        encode("logFilter", logFilter)
        encode("debugLogging", debugLogging)
        encode("debugLoggingMigrated", debugLoggingMigrated)
        encode("srtOverlay", debugOverlay)
        encode("cameraSwitchRemoveBlackish", cameraSwitchRemoveBlackish)
        encode("bluetoothOutputOnly", bluetoothOutputOnly)
        encode("maximumLogLines", maximumLogLines)
        encode("pixelFormat", pixelFormat)
        encode("beautyFilterSettings", faceToBeRemoved, SettingsFace.serializer())
        encode("allowVideoRangePixelFormat", allowVideoRangePixelFormat)
        encode("nativeLowLightBoost", nativeLowLightBoost)
        encode("blurSceneSwitch", blurSceneSwitch)
        encode("preferStereoMic", preferStereoMicToBeRemoved)
        encode("twitchRewards", twitchRewards)
        encode("tesla", tesla, SettingsTesla.serializer())
        encode("dnsLookupStrategy", dnsLookupStrategy)
        encode("dataRateLimitFactor", dataRateLimitFactor)
        encode("bitrateDropFix", bitrateDropFix)
        encode("relaxedBitrate", relaxedBitrate)
        encode("externalDisplayChat", externalDisplayChat)
        encode("videoSourceWidgetTrackFace", videoSourceWidgetTrackFace)
        encode("replay", replay)
        encode("recordSegmentLength", recordSegmentLength)
        encode("builtinAudioAndVideoDelay", builtinAudioAndVideoDelay)
        encode("builtinAudioAndVideoDelay70msMigrated", builtinAudioAndVideoDelay70msMigrated)
        encode("cameraManMoveVertically", cameraManMoveVertically)
        encode("cameraManSpeed", cameraManSpeed)
        encode("cameraManAlwaysMove", cameraManAlwaysMove)
        encode("enhancedMoblinSrt", enhancedMoblinSrt)
        encode("videoBitrateChangeEnabled", videoBitrateChange)
        encode("highQualityDownsampling", highQualityDownsamplingToBeRemoved)
        encode("httpProxy3", httpProxyToBeRemoved)
        encode("packetPadding", packetPadding)
    }

    companion object {
        const val builtinAudioAndVideoDelayDefault: Double = 0.07

        fun decode(container: JsonObject): SettingsDebug {
            val debug = SettingsDebug()
            debug.logLevel = container.decode("logLevel", SettingsLogLevel.error)
            debug.logFilter.value = container.decode("logFilter", "")
            debug.debugLogging.value = container.decode("debugLogging", false)
            debug.debugLoggingMigrated = container.decode("debugLoggingMigrated", false)
            if (!debug.debugLoggingMigrated) {
                debug.debugLogging.value = debug.logLevel == SettingsLogLevel.debug
                debug.debugLoggingMigrated = true
            }
            debug.debugOverlay.value = container.decode("srtOverlay", false)
            debug.cameraSwitchRemoveBlackish.value = container.decode("cameraSwitchRemoveBlackish", 0.3f)
            debug.bluetoothOutputOnly.value = container.decode("bluetoothOutputOnly", true)
            debug.maximumLogLines = container.decode("maximumLogLines", 500)
            debug.pixelFormat = container.decode("pixelFormat", pixelFormats[1])
            debug.faceToBeRemoved = container.decode("beautyFilterSettings", SettingsFace.serializer(), SettingsFace())
            debug.allowVideoRangePixelFormat.value = container.decode("allowVideoRangePixelFormat", false)
            debug.nativeLowLightBoost.value = container.decode("nativeLowLightBoost", false)
            debug.blurSceneSwitch = container.decode("blurSceneSwitch", true)
            debug.preferStereoMicToBeRemoved = container.decode("preferStereoMic", false)
            debug.twitchRewards.value = container.decode("twitchRewards", false)
            debug.tesla = container.decode("tesla", SettingsTesla.serializer(), SettingsTesla())
            debug.dnsLookupStrategy = container.decode("dnsLookupStrategy", SettingsDnsLookupStrategy.system)
            debug.dataRateLimitFactor.value = container.decode("dataRateLimitFactor", 2.0f)
            debug.bitrateDropFix.value = container.decode("bitrateDropFix", false)
            debug.relaxedBitrate.value = container.decode("relaxedBitrate", false)
            debug.externalDisplayChat = container.decode("externalDisplayChat", false)
            debug.videoSourceWidgetTrackFace = container.decode("videoSourceWidgetTrackFace", false)
            debug.replay = container.decode("replay", false)
            debug.recordSegmentLength = container.decode("recordSegmentLength", 5.0)
            debug.builtinAudioAndVideoDelay.value = container.decode(
                "builtinAudioAndVideoDelay",
                builtinAudioAndVideoDelayDefault,
            )
            debug.builtinAudioAndVideoDelay70msMigrated = container.decode(
                "builtinAudioAndVideoDelay70msMigrated",
                false,
            )
            if (!debug.builtinAudioAndVideoDelay70msMigrated && debug.builtinAudioAndVideoDelay.value == 0.0) {
                debug.builtinAudioAndVideoDelay.value = builtinAudioAndVideoDelayDefault
            }
            debug.builtinAudioAndVideoDelay70msMigrated = true
            debug.cameraManMoveVertically.value = container.decode("cameraManMoveVertically", false)
            debug.cameraManSpeed.value = container.decode("cameraManSpeed", 1.0)
            debug.cameraManAlwaysMove.value = container.decode("cameraManAlwaysMove", false)
            debug.enhancedMoblinSrt.value = container.decode("enhancedMoblinSrt", false)
            debug.videoBitrateChange.value = container.decode("videoBitrateChangeEnabled", false)
            debug.highQualityDownsamplingToBeRemoved = container.decode("highQualityDownsampling", false)
            debug.httpProxyToBeRemoved = container.decode("httpProxy3", false)
            debug.packetPadding.value = container.decode("packetPadding", false)
            return debug
        }
    }

    object Serializer : KSerializer<SettingsDebug> by JsonObjectSerializer(
        "SettingsDebug",
        { it.encode() },
        { decode(it) },
    )
}
